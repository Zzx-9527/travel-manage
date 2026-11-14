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

    public AiQuotaService(RedisUtil redisUtil, AiProperties properties) {
        this.redisUtil = redisUtil;
        this.properties = properties;
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

    /** 到明天零点再多留一分钟，避免边界时刻 key 提前消失导致配额重置 */
    private Duration ttlUntilTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT).plusMinutes(1);
        return Duration.between(now, tomorrow);
    }
}
