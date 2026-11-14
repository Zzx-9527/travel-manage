package com.zjx.travel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Redis 通用工具与 Cache-Aside 模板。
 *
 * <p>业务代码只通过本类访问缓存，不直接注入 {@code RedisTemplate}，目的是把三件事收口：
 * <ol>
 *   <li><b>降级</b>：所有 Redis 调用都 try/catch，异常时按"未命中"处理，接口继续回源查库；</li>
 *   <li><b>缓存三大问题</b>：空值哨兵防穿透、TTL 随机抖动防雪崩、手写锁 + 双重检查防击穿；</li>
 *   <li><b>序列化细节</b>：空值统一用哨兵字符串表示，避免把 null 写进缓存后无法区分"未缓存"和"缓存了空"。</li>
 * </ol>
 *
 * <p>典型用法（Service 层）：
 * <pre>{@code
 * return redisUtil.getOrLoad(RedisKeys.scenicDetail(id), Duration.ofMinutes(30),
 *         () -> scenicMapper.queryById(id));
 * }</pre>
 */
@Component
public class RedisUtil {

    private static final Logger log = LoggerFactory.getLogger(RedisUtil.class);

    /**
     * 空值哨兵。查库结果为 null 时写入该值，读缓存时再翻译回 null，
     * 从而把"库里确实没有"与"缓存里没这条记录"区分开，防止同一个不存在的 id 反复打库。
     */
    static final String NULL_VALUE = "@NULL@";

    /** 空值缓存默认存活时间，短 TTL 避免数据新增后长时间读到"不存在" */
    private static final long NULL_VALUE_TTL_SECONDS = 60L;

    /** TTL 抖动比例：±10%，打散同一批 key 的过期时间，避免雪崩 */
    private static final double TTL_JITTER_RATIO = 0.1D;

    /** 重建锁的存活时间：临界区是"查库 + 回写"，10 秒足够，且远大于正常耗时 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    /** 未抢到锁时的自旋次数与间隔：总共约 200ms，超过就放弃等待直接回源 */
    private static final int LOCK_RETRY_TIMES = 5;
    private static final long LOCK_RETRY_INTERVAL_MILLIS = 40L;

    /** SCAN 每批数量与单次扫描上限，防止误扫全库把应用拖死 */
    private static final int SCAN_BATCH_SIZE = 500;
    private static final int SCAN_MAX_KEYS = 10000;

    /**
     * 计数器扣减脚本：剩余量 ≤ 本次扣减量时删除 key，否则 DECRBY。
     *
     * <p>浏览量回写专用：数据库累加成功后才调用，扣减过程中新来的浏览不会被误删，
     * 也不会出现"先删 Redis 再写库、写库失败就丢数据"的问题。
     */
    private static final RedisScript<Long> DECREMENT_OR_DELETE_SCRIPT = new DefaultRedisScript<>(
            "local current = tonumber(redis.call('get', KEYS[1]) or '0') "
                    + "local delta = tonumber(ARGV[1]) "
                    + "if current <= delta then "
                    + "  redis.call('del', KEYS[1]) "
                    + "else "
                    + "  redis.call('decrby', KEYS[1], delta) "
                    + "end "
                    + "return current",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final LockUtil lockUtil;

    /**
     * 缓存总开关（{@code travel.cache.enabled}，默认 true）。
     *
     * <p>用途：① 线上排障时一键绕过缓存看真实库表现；② 压测时作为"无缓存"基线。
     * 关闭后 {@link #getOrLoad} 直接回源，不读也不写缓存。
     */
    private final boolean cacheEnabled;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate, LockUtil lockUtil,
                     @Value("${travel.cache.enabled:true}") boolean cacheEnabled) {
        this.redisTemplate = redisTemplate;
        this.lockUtil = lockUtil;
        this.cacheEnabled = cacheEnabled;
    }

    // ==================== 基础读写 ====================

    /** 写入并设置过期时间；失败只打日志，不影响主流程 */
    public void set(String key, Object value, Duration ttl) {
        writeQuietly(key, value, ttl);
    }

    /** 读取；未命中、空值哨兵、Redis 异常都返回 null */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return NULL_VALUE.equals(value) ? null : (T) value;
        } catch (Exception e) {
            log.warn("读取缓存失败，按未命中处理，key={}, cause={}", key, e.toString());
            return null;
        }
    }

    /** 删除；返回是否真的删掉了（false 也可能是 Redis 异常） */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.warn("删除缓存失败，key={}, cause={}", key, e.toString());
            return false;
        }
    }

    /** 是否存在（不算空值哨兵） */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("判断缓存是否存在失败，key={}, cause={}", key, e.toString());
            return false;
        }
    }

    /** 剩余存活时间（秒）：-2 不存在，-1 未设置过期；异常返回 null */
    public Long getExpireSeconds(String key) {
        try {
            return redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("查询缓存 TTL 失败，key={}, cause={}", key, e.toString());
            return null;
        }
    }

    /** 原子自增；失败返回 null。注意值必须是数字（写字符串会带引号，INCR 会报错） */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.warn("自增失败，key={}, cause={}", key, e.toString());
            return null;
        }
    }

    /**
     * 设置 key 的过期时间（秒级）。
     *
     * <p>用于"先 INCR 再补 TTL"的计数场景：自增和设置过期无法用单条命令完成，
     * 这里把过期失败降级为"本次不过期"，由调用方决定是否容忍。
     *
     * @return true 表示设置成功；false 表示 key 不存在或 Redis 异常
     */
    public boolean expire(String key, Duration ttl) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
        } catch (Exception e) {
            log.warn("设置过期时间失败，key={}, cause={}", key, e.toString());
            return false;
        }
    }

    /**
     * 扣减计数器：数据库累加成功之后再调用（顺序不能反）。
     *
     * @param delta 本次已经写入数据库的增量
     * @return true 表示扣减成功；false 表示 Redis 异常（调用方记日志，下一轮重试会多计一次）
     */
    public boolean decrementOrDelete(String key, long delta) {
        try {
            redisTemplate.execute(DECREMENT_OR_DELETE_SCRIPT, Collections.singletonList(key), delta);
            return true;
        } catch (Exception e) {
            log.warn("扣减计数器失败，key={}, delta={}, cause={}", key, delta, e.toString());
            return false;
        }
    }

    // ==================== Cache-Aside 模板 ====================

    /**
     * 读缓存 → 未命中则加锁回源 → 回写缓存。
     *
     * <p>空结果按 {@link #NULL_VALUE_TTL_SECONDS} 秒缓存；正常结果按传入 ttl 加 ±10% 抖动。
     * 任何 Redis 异常都会退化为直接调用 loader，保证接口可用。
     *
     * @param key    缓存 key，建议由 {@link RedisKeys} 生成
     * @param ttl    正常数据的存活时间
     * @param loader 回源逻辑（通常是查库）
     */
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        return getOrLoad(key, ttl, Duration.ofSeconds(NULL_VALUE_TTL_SECONDS), loader);
    }

    /**
     * 同上，可自定义空值的存活时间。
     *
     * @param nullValueTtl 查库结果为空时的缓存时间，传 {@code Duration.ZERO} 表示不缓存空值
     */
    public <T> T getOrLoad(String key, Duration ttl, Duration nullValueTtl, Supplier<T> loader) {
        Objects.requireNonNull(key, "key 不能为空");
        Objects.requireNonNull(ttl, "ttl 不能为空");
        Objects.requireNonNull(loader, "loader 不能为空");

        // 缓存开关关闭：直接回源，不读不写（排障 / 压测基线用）
        if (!cacheEnabled) {
            return loader.get();
        }

        Object cached = readQuietly(key);
        if (cached != null) {
            return unwrap(cached);
        }

        String lockKey = RedisKeys.lockOf(key);
        String token = UUID.randomUUID().toString();

        if (!lockUtil.tryLock(lockKey, token, LOCK_TTL)) {
            // 没抢到锁：持锁者正在回源，短暂自旋等它回写；等不到就自己回源（宁可多查一次，也不让请求堆积）
            for (int i = 0; i < LOCK_RETRY_TIMES; i++) {
                sleepQuietly(LOCK_RETRY_INTERVAL_MILLIS);
                Object retry = readQuietly(key);
                if (retry != null) {
                    return unwrap(retry);
                }
            }
            log.warn("未抢到缓存重建锁且缓存仍未就绪，降级直接回源，key={}", key);
            return loader.get();
        }

        try {
            // 双重检查：等锁期间别人可能已经回写完成
            Object doubleCheck = readQuietly(key);
            if (doubleCheck != null) {
                return unwrap(doubleCheck);
            }

            T value = loader.get();
            if (value == null) {
                if (nullValueTtl != null && !nullValueTtl.isZero() && !nullValueTtl.isNegative()) {
                    writeQuietly(key, NULL_VALUE, nullValueTtl);
                }
            } else {
                writeQuietly(key, value, jitter(ttl));
            }
            return value;
        } finally {
            lockUtil.unlock(lockKey, token);
        }
    }

    /**
     * 按 pattern 扫描 key（SCAN 实现，**不用 KEYS**）。
     *
     * <p>KEYS 会阻塞 Redis 单线程，生产禁用；SCAN 是游标式增量遍历，
     * 只会在单次迭代上花少量时间，适合"在线用户"这类管理端低频查询。
     *
     * <p>加了单次上限（{@value #SCAN_MAX_KEYS} 个 key），避免 pattern 写错时把应用拖死。
     *
     * @param pattern 例如 {@code satoken:login:token:*} 或 {@code travel:v1:scenic:detail:*}
     * @return 匹配到的 key；Redis 异常时返回空列表（调用方按"没有数据"处理）
     */
    public List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(SCAN_BATCH_SIZE)
                        .build();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext() && keys.size() < SCAN_MAX_KEYS) {
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("扫描 key 失败，pattern={}, cause={}", pattern, e.toString());
        }
        return keys;
    }

    // ==================== 内部方法 ====================

    /** 读原始值，异常时返回 null（表示"当作没缓存"） */
    private Object readQuietly(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取缓存失败，按未命中处理，key={}, cause={}", key, e.toString());
            return null;
        }
    }

    /** 写缓存，异常只打日志 */
    private void writeQuietly(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("写入缓存失败，key={}, cause={}", key, e.toString());
        }
    }

    /** 把空值哨兵翻译回 null */
    @SuppressWarnings("unchecked")
    private <T> T unwrap(Object cached) {
        if (NULL_VALUE.equals(cached)) {
            return null;
        }
        return (T) cached;
    }

    /**
     * TTL 加 ±10% 随机抖动。
     *
     * <p>如果一批 key 在同一时刻批量写入且 TTL 完全相同，它们会同时过期、同时回源，
     * 数据库瞬间被打穿——这就是雪崩。抖动把过期时间打散到 54~66 分钟（以 1 小时为例）。
     */
    private Duration jitter(Duration ttl) {
        long baseMillis = Math.max(1000L, ttl.toMillis());
        long delta = (long) (baseMillis * TTL_JITTER_RATIO);
        long offset = delta <= 0 ? 0L : ThreadLocalRandom.current().nextLong(-delta, delta + 1);
        return Duration.ofMillis(baseMillis + offset);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待缓存重建时被中断，key 自旋提前结束");
        }
    }
}
