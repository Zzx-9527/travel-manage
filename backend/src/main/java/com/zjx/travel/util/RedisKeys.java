package com.zjx.travel.util;

/**
 * Redis key 统一管理。
 *
 * <p>命名规范：{@code travel:v1:{模块}:{用途}:{标识}}，全小写、冒号分隔。
 * <ul>
 *   <li>前缀带版本号 {@code v1}：实体结构变更后换版本位等于换命名空间，比逐条清理旧缓存安全；</li>
 *   <li>业务 key 一律带 TTL，禁止永久 key（计数类除外）；</li>
 *   <li>{@code satoken:} 开头的 key 由 Sa-Token 维护，本类不涉及，也不允许人工删除。</li>
 * </ul>
 */
public final class RedisKeys {

    /** 业务 key 统一前缀 */
    public static final String PREFIX = "travel:v1:";

    /** 锁 key 的中间段 */
    private static final String LOCK_SEGMENT = "lock:";

    private RedisKeys() {
    }

    /** 景点详情：travel:v1:scenic:detail:{id} */
    public static String scenicDetail(Integer scenicId) {
        return PREFIX + "scenic:detail:" + scenicId;
    }

    /** 景点浏览量计数：travel:v1:scenic:view:{id} */
    public static String scenicViewCount(Integer scenicId) {
        return PREFIX + "scenic:view:" + scenicId;
    }

    /** 景点详情重建锁：travel:v1:lock:scenic:detail:{id} */
    public static String scenicDetailLock(Integer scenicId) {
        return lockOf(scenicDetail(scenicId));
    }

    /** 登录限流：travel:v1:ratelimit:login:{ip} */
    public static String loginRateLimit(String ip) {
        String safeIp = ip == null ? "unknown" : ip.replace(':', '-');
        return PREFIX + "ratelimit:login:" + safeIp;
    }

    /** 订单防重复提交：travel:v1:submit:order:{userId} */
    public static String orderSubmit(Integer userId) {
        return PREFIX + "submit:order:" + userId;
    }

    /**
     * 由业务 key 推导锁 key：{@code travel:v1:scenic:detail:12} → {@code travel:v1:lock:scenic:detail:12}。
     * 非本前缀的 key 会整体挂到 lock: 段下，保证锁 key 仍在统一命名空间内。
     */
    public static String lockOf(String businessKey) {
        if (businessKey == null || businessKey.isEmpty()) {
            throw new IllegalArgumentException("businessKey 不能为空");
        }
        String suffix = businessKey.startsWith(PREFIX) ? businessKey.substring(PREFIX.length()) : businessKey;
        return PREFIX + LOCK_SEGMENT + suffix;
    }

    /** 是否为本项目的业务 key（用于清理、统计时排除 satoken: 等框架 key） */
    public static boolean isBusinessKey(String key) {
        return key != null && key.startsWith(PREFIX);
    }
}
