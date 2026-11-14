package com.zjx.travel.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块的业务侧配置（模型连接参数由 Spring AI 自己管，见 {@code spring.ai.openai.*}）。
 *
 * <p>这里只放"我们自己要控制的旋钮"：总开关、配额、每天景点上限、读超时。
 * 与 {@code spring.ai.*} 分开的好处是：换模型厂商只改 Spring AI 那段，
 * 业务规则（配额、上限）不用跟着动。
 */
@Data
@Component
@ConfigurationProperties(prefix = "travel.ai")
public class AiProperties {

    /** 总开关：false 时 /ai/** 直接返回"AI 未启用"，不影响其他功能 */
    private boolean enabled = true;

    /** 每人每天最多生成次数，Redis 按自然日计数 */
    private int dailyLimit = 10;

    /** 每天最多安排几个景点，服务端会截断模型给的多余项 */
    private int maxSpotsPerDay = 5;
}
