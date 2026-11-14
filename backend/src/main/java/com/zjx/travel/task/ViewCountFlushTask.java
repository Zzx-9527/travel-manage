package com.zjx.travel.task;

import com.zjx.travel.service.ScenicService;
import com.zjx.travel.util.LockUtil;
import com.zjx.travel.util.RedisKeys;
import com.zjx.travel.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 景点浏览量回写任务。
 *
 * <p>详情接口只做 {@code INCR}（不写库），本任务每 60 秒把 Redis 里累积的增量批量写回 MySQL。
 *
 * <p><b>顺序很重要</b>：先 {@code UPDATE} 成功，再扣减 Redis。反过来（先删 Redis 再写库）
 * 一旦写库失败或进程崩溃，这部分浏览量就永久丢了。现在的顺序最坏情况是"崩在扣减之前 → 下一轮多算一次"，
 * 也就是 at-least-once：宁可多计，不可少计。
 *
 * <p>扣减用 {@code DECRBY} 而不是直接 {@code DEL}：回写期间可能又有新浏览把计数加上去了，
 * 直接删会把新增的部分一起吃掉。
 *
 * <p>多实例部署时用 Redisson 锁互斥，避免两个节点同时回写导致重复累加。
 */
@Component
public class ViewCountFlushTask {

    private static final Logger log = LoggerFactory.getLogger(ViewCountFlushTask.class);

    /** 浏览量计数器 key 前缀：travel:v1:scenic:view:{id} */
    private static final String VIEW_KEY_PREFIX = RedisKeys.PREFIX + "scenic:view:";

    /** 回写互斥锁：travel:v1:lock:scenic:view:flush */
    private static final String FLUSH_LOCK_KEY = RedisKeys.PREFIX + "lock:scenic:view:flush";

    /** 锁租约：回写是分批扫 key + 逐条更新，给 60 秒足够 */
    private static final long LOCK_LEASE_SECONDS = 60L;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private LockUtil lockUtil;

    @Resource
    private ScenicService scenicService;

    /**
     * 定时回写。间隔由 {@code travel.view.flush-interval-ms} 控制（默认 60 秒）。
     *
     * <p>初始延迟设为一个周期，避免应用刚启动、数据库连接池还没热起来就开跑。
     */
    @Scheduled(initialDelayString = "${travel.view.flush-interval-ms:60000}",
            fixedDelayString = "${travel.view.flush-interval-ms:60000}")
    public void flush() {
        if (!lockUtil.tryLockWithRedisson(FLUSH_LOCK_KEY, 0, LOCK_LEASE_SECONDS)) {
            log.debug("另一实例正在回写浏览量，本次跳过");
            return;
        }

        int flushed = 0;
        try {
            // 必须用 SCAN：KEYS 在大 key 量下会阻塞 Redis 单线程
            List<String> keys = redisUtil.scanKeys(VIEW_KEY_PREFIX + "*");
            for (String key : keys) {
                Integer scenicId = parseScenicId(key);
                if (scenicId == null) {
                    continue;
                }

                Number pending = redisUtil.get(key);
                long delta = pending == null ? 0L : pending.longValue();
                if (delta <= 0) {
                    continue;
                }

                // 1) 先落库；失败就保留 Redis 计数，下一轮重试
                if (!scenicService.increaseViewCount(scenicId, delta)) {
                    log.warn("浏览量回写未成功，保留计数等待下轮重试：scenicId={}, delta={}", scenicId, delta);
                    continue;
                }

                // 2) 落库成功后再扣减（扣到 0 自动删 key）
                redisUtil.decrementOrDelete(key, delta);

                // 3) 详情缓存里是旧的 viewCount，删掉让下次读取新值
                redisUtil.delete(RedisKeys.scenicDetail(scenicId));

                flushed++;
            }
            if (flushed > 0) {
                log.info("浏览量回写完成，共 {} 个景点", flushed);
            }
        } catch (Exception e) {
            // 定时任务不能把异常抛给调度线程
            log.error("浏览量回写任务异常", e);
        } finally {
            lockUtil.unlockRedisson(FLUSH_LOCK_KEY);
        }
    }

    private Integer parseScenicId(String key) {
        try {
            return Integer.valueOf(key.substring(VIEW_KEY_PREFIX.length()));
        } catch (Exception e) {
            log.warn("无法解析浏览量计数 key，已跳过：{}", key);
            return null;
        }
    }
}
