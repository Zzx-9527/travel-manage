package com.zjx.travel.ai.service;

import com.zjx.travel.ai.config.AiProperties;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.util.RedisKeys;
import com.zjx.travel.util.RedisUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * AI 每日配额。
 *
 * <p>实现是"Redis 自增 + key 按自然日分桶"：key 里带 {@code yyyyMMdd}，
 * 第一次自增时把 TTL 补到当天 24 点，第二天自然落到新 key，不需要定时任务清理。
 *
 * <p>两个刻意的降级：
 * <ul>
 *   <li>Redis 不可用时 <b>放行</b>而不是拦截——配额是成本控制手段，不该反过来拖垮功能；</li>
 *   <li>计数在调用模型之前 +1，失败的请求也计入，避免"失败重试刷次数"。</li>
 * </ul>
 */
@Service
public class AiQuotaService {

    private final RedisUtil redisUtil;
    private final AiProperties properties;

    /** 生成中标记的存活时间，需大于 SSE 连接上限（120s），防止长任务被误判为结束 */
    private static final Duration GENERATION_LOCK_TTL = Duration.ofSeconds(150);

    public AiQuotaService(RedisUtil redisUtil, AiProperties properties) {
        this.redisUtil = redisUtil;
        this.properties = properties;
    }

    /**
     * 并发闸：同一用户同时只允许一条生成任务。
     *
     * <p>为什么需要它：每日配额只限制"总量"，拦不住用户连点五次导致的并发调用——
     * 那五次会同时打到模型，既浪费钱，返回顺序也会乱。
     *
     * <p>Redis 异常时放行（{@code setIfAbsent} 返回 null），不让缓存故障把功能锁死。
     *
     * @return true 表示可以继续；false 表示已有任务在跑
     */
    public boolean tryAcquireGeneration(Integer userId) {
        if (userId == null) {
            return true;
        }
        Boolean locked = redisUtil.setIfAbsent(RedisKeys.aiRunning(userId), "1", GENERATION_LOCK_TTL);
        return !Boolean.FALSE.equals(locked);
    }

    /** 释放并发标记，放在 finally 里调用 */
    public void releaseGeneration(Integer userId) {
        if (userId == null) {
            return;
        }
        redisUtil.delete(RedisKeys.aiRunning(userId));
    }

    /**
     * 校验并占用一次配额，超限抛 {@link AiPlanException}。
     */
    public void checkAndConsume(Integer userId) {
        if (userId == null) {
            throw new AiPlanException("请先登录后再使用 AI 规划");
        }
        int limit = properties.getDailyLimit();
        if (limit <= 0) {
            return;
        }

        String key = RedisKeys.aiQuota(userId, LocalDate.now());
        Long used = redisUtil.increment(key, 1);
        if (used == null) {
            // Redis 故障：直接放行，只记日志（RedisUtil 内部已记录原因）
            return;
        }
        if (used == 1L) {
            redisUtil.expire(key, ttlUntilTomorrow());
        }
        if (used > limit) {
            throw new AiPlanException("今天的 AI 规划次数已用完（每天 " + limit + " 次），明天再来吧");
        }
    }

    /** 今日剩余次数，用于前端展示；拿不到时返回上限（不阻塞用户） */
    public int remaining(Integer userId) {
        int limit = properties.getDailyLimit();
        if (userId == null || limit <= 0) {
            return limit;
        }
        Object value = redisUtil.get(RedisKeys.aiQuota(userId, LocalDate.now()));
        int used = 0;
        if (value instanceof Number number) {
            used = number.intValue();
        } else if (value instanceof String text) {
            try {
                used = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                // 值损坏按 0 处理
            }
        }
        return Math.max(0, limit - used);
    }

    /**
     * 对话配额：与"排行程"分开计数。
     *
     * <p>为什么分开：一次行程生成的成本大约是单条对话的十倍，
     * 放一起会让"多聊两句"挤掉"生成行程"的名额，用户会觉得额度莫名其妙就没了。
     */
    public void checkAndConsumeChat(Integer userId) {
        if (userId == null) {
            throw new AiPlanException("请先登录后再使用 AI 助手");
        }
        int limit = properties.getChatDailyLimit();
        if (limit <= 0) {
            return;
        }
        String key = RedisKeys.aiChatQuota(userId, LocalDate.now());
        Long used = redisUtil.increment(key, 1);
        if (used == null) {
            return;
        }
        if (used == 1L) {
            redisUtil.expire(key, ttlUntilTomorrow());
        }
        if (used > limit) {
            throw new AiPlanException("今天的 AI 对话次数已用完（每天 " + limit + " 条），明天再来吧");
        }
    }

    /** 到明天零点再多留一分钟，避免边界时刻 key 提前消失导致配额重置 */
    private Duration ttlUntilTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT).plusMinutes(1);
        return Duration.between(now, tomorrow);
    }

    /**
     * 返还一次配额：生成失败时调用，避免用户"一次结果没拿到，次数却没了"。
     *
     * <p>三个保护：
     * <ul>
     *   <li>只在 key 还存在时返还——调用跨过午夜、key 已按新的一天重建时，
     *       不能去扣今天的额度；</li>
     *   <li>减到 0 就删掉 key，不留一个 0 值且无 TTL 的脏 key；</li>
     *   <li>Redis 异常时静默失败，返还属于补偿动作，不该影响主流程返回的错误信息。</li>
     * </ul>
     */
    public void refund(Integer userId) {
        refundKey(userId == null ? null : RedisKeys.aiQuota(userId, LocalDate.now()));
    }

    /** 对话失败时返还，逻辑同上 */
    public void refundChat(Integer userId) {
        refundKey(userId == null ? null : RedisKeys.aiChatQuota(userId, LocalDate.now()));
    }

    private void refundKey(String key) {
        if (key == null) {
            return;
        }
        if (!redisUtil.hasKey(key)) {
            return;
        }
        Long value = redisUtil.increment(key, -1);
        if (value != null && value <= 0) {
            redisUtil.delete(key);
        }
    }
}
