package com.zjx.travel.util;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁与限流工具，提供两套实现，按场景选择。
 *
 * <table border="1">
 *   <caption>选型标准</caption>
 *   <tr><th>场景</th><th>用哪个</th><th>原因</th></tr>
 *   <tr><td>缓存击穿回源（单 key、短临界区、幂等读）</td><td>{@link #tryLock}（手写）</td>
 *       <td>15 行代码解决，失败可重试，重复回源无害</td></tr>
 *   <tr><td>限流、防重复提交、不可重入的业务</td><td>{@link #tryAcquire} / {@link #trySetOnce} / {@link #tryLockWithRedisson}</td>
 *       <td>需要窗口计数、可重入、租约、看门狗</td></tr>
 * </table>
 *
 * <p><b>手写锁为什么用 {@link StringRedisTemplate}</b>：锁值是纯字符串（UUID），Lua 里
 * {@code redis.call('get')} 拿到的是原始字节；若走 JSON 序列化器，值会变成带引号的
 * {@code "uuid"}，比对容易踩坑。锁是基础设施，用纯字符串最直观。
 *
 * <p><b>释放为什么必须用 Lua</b>：{@code get} 再 {@code del} 是两步，锁可能在这两步之间过期
 * 并被别人拿走，直接 del 会删掉别人的锁。脚本把"比较 + 删除"做成原子操作。
 *
 * <p><b>降级约定</b>：Redis 不可用时锁与限流一律"放行"（返回 true），并打 WARN。
 * 此时本来就没有互斥/限流能力，让请求继续，比让接口报错更划算。
 */
@Component
public class LockUtil {

    private static final Logger log = LoggerFactory.getLogger(LockUtil.class);

    /** KEYS[1]=锁 key，ARGV[1]=持有者 token；值相等才删除，保证不误删 */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    /**
     * 固定窗口限流：KEYS[1]=计数 key，ARGV[1]=窗口秒数，返回窗口内第几次请求。
     *
     * <p>INCR 和 EXPIRE 放在同一个脚本里，保证"计数"和"过期"要么都发生、要么都不发生，
     * 不会出现"计了数但没设过期"导致 key 永久堆积（这正是 Redisson RRateLimiter 的问题：
     * 每个 key 会拆成配置 + value + permits 三个且都不带 TTL）。
     *
     * <p>额外兜底：如果 key 因历史原因没有 TTL（ttl &lt; 0），补一次过期。
     */
    private static final RedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) "
                    + "if current == 1 then "
                    + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
                    + "elseif redis.call('TTL', KEYS[1]) < 0 then "
                    + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
                    + "end "
                    + "return current",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    public LockUtil(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    // ==================== 手写锁：简单互斥场景 ====================

    /**
     * 尝试加锁，不等待（简单互斥）。
     *
     * <p>加锁与设置过期由 {@code SET key value NX PX} 一条命令完成，避免
     * {@code setnx} 之后再 {@code expire} 的非原子窗口。
     *
     * @param lockKey 锁 key，建议用 {@link RedisKeys#lockOf(String)} 生成
     * @param token   持有者标识，释放时必须传同一个值
     * @param ttl     锁存活时间，必须大于临界区最长耗时（建议 ≥ 3 倍）
     * @return true 表示加锁成功（或 Redis 不可用时的降级放行）
     */
    public boolean tryLock(String lockKey, String token, Duration ttl) {
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, ttl);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis 不可用，锁降级为放行，lockKey={}, cause={}", lockKey, e.toString());
            return true;
        }
    }

    /**
     * 释放锁：只有值等于 {@code token} 时才删除。
     *
     * @return true 表示确实删除了自己的锁；false 表示锁已过期、已属于别人，或 Redis 异常
     */
    public boolean unlock(String lockKey, String token) {
        try {
            Long deleted = stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
            return deleted != null && deleted > 0;
        } catch (Exception e) {
            log.warn("释放锁失败（可能是锁已过期），lockKey={}, cause={}", lockKey, e.toString());
            return false;
        }
    }

    /** 判断锁当前是否被持有，仅用于排查，不参与业务判断 */
    public boolean isLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.warn("查询锁状态失败，lockKey={}, cause={}", lockKey, e.toString());
            return false;
        }
    }

    /** 剩余存活时间（秒），-2 表示 key 不存在，-1 表示未设置过期；异常返回 null */
    public Long getLockTtlSeconds(String lockKey) {
        try {
            return stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("查询锁 TTL 失败，lockKey={}, cause={}", lockKey, e.toString());
            return null;
        }
    }

    // ==================== Redisson 锁：复杂并发场景 ====================

    /**
     * Redisson 可重入锁。
     *
     * <p>相比手写锁多了三件事：可重入、等待队列、租约到期自动释放（不传 leaseTime 时还有看门狗续期）。
     * 这里显式传 {@code leaseTime}，让锁在业务异常时也能自动释放，避免"锁住了没人开"。
     *
     * <p><b>注意</b>：Redisson 锁与线程绑定，必须在<b>同一个线程</b>里加锁和释放。
     *
     * @param waitSeconds  最多等待多久（0 表示不等待，立即返回）
     * @param leaseSeconds 租约时长，到期自动释放
     */
    public boolean tryLockWithRedisson(String lockKey, long waitSeconds, long leaseSeconds) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Redisson 加锁被中断，lockKey={}", lockKey);
            return false;
        } catch (Exception e) {
            log.warn("Redisson 加锁异常，降级放行，lockKey={}, cause={}", lockKey, e.toString());
            return true;
        }
    }

    /**
     * 释放 Redisson 锁：只有当前线程持有才释放，避免误释放别人的锁。
     *
     * @return true 表示确实释放了；false 表示锁不属于当前线程或已过期
     */
    public boolean unlockRedisson(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (!lock.isHeldByCurrentThread()) {
                return false;
            }
            lock.unlock();
            return true;
        } catch (Exception e) {
            log.warn("释放 Redisson 锁失败，lockKey={}, cause={}", lockKey, e.toString());
            return false;
        }
    }

    // ==================== 限流与防重复提交 ====================

    /**
     * 固定窗口限流：{@code windowSeconds} 秒内最多放行 {@code limit} 次。
     *
     * <p>为什么不用 Redisson 的 {@code RRateLimiter}：
     * <ol>
     *   <li>它一个限流器会写成 3 个 key（配置 hash + {@code {}:value} + <code>{}:permits</code>），
     *       而且都不带 TTL，IP 一多就是 3 倍速的 key 泄漏；</li>
     *   <li>登录限流只需"每分钟 N 次"，固定窗口足够，不需要令牌桶的平滑能力；</li>
     *   <li>手写 Lua 只有 1 个 key、60 秒自动过期，行为可预测，面试也好讲。</li>
     * </ol>
     *
     * <p>代价是窗口边界存在"双倍突发"（59 秒和 61 秒各打满一次），对登录防撞库完全够用；
     * 真要平滑限流再上令牌桶。
     *
     * @return true 表示放行；Redis 异常时也返回 true（限流组件故障不应该变成"谁都登录不了"）
     */
    public boolean tryAcquireFixedWindow(String key, int limit, int windowSeconds) {
        try {
            Long current = stringRedisTemplate.execute(FIXED_WINDOW_SCRIPT,
                    Collections.singletonList(key), String.valueOf(windowSeconds));
            if (current == null) {
                return true;
            }
            return current <= limit;
        } catch (Exception e) {
            log.warn("限流组件异常，本次放行，key={}, cause={}", key, e.toString());
            return true;
        }
    }

    /**
     * 防重复提交：在 ttl 内同一个 key 只允许成功写入一次。
     *
     * <p>用 {@code RBucket.trySet} 而不是"先 get 再 set"，是因为 SETNX 语义天然原子，
     * 并发下不会出现两个请求都判断为"第一次"。
     *
     * @return true 表示是第一次（放行）；false 表示 ttl 内已经提交过
     */
    public boolean trySetOnce(String key, Duration ttl) {
        try {
            RBucket<Boolean> bucket = redissonClient.getBucket(key);
            return bucket.trySet(Boolean.TRUE, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("防重复提交组件异常，本次放行，key={}, cause={}", key, e.toString());
            return true;
        }
    }
}
