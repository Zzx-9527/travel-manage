package com.zjx.travel.ai.controller;

import com.zjx.travel.ai.config.AiProperties;
import com.zjx.travel.ai.service.AiQuotaService;
import com.zjx.travel.util.ReturnVO;
import com.zjx.travel.util.SecurityUtil;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模块自检接口。
 *
 * <p>用途：不真正调用模型，只回答"这个环境到底有没有把 AI 配通"，
 * 避免前端拿着一个空 Key 去请求，最后拿到一个看不懂的超时错误。
 *
 * <p>为什么用 {@link ObjectProvider} 拿 {@link ChatModel}：Key 没配时 Spring AI 的
 * 自动配置可能不创建这个 Bean，直接 {@code @Autowired} 会让整个应用起不来——
 * 一个可选的 AI 功能不该拖垮主业务。
 */
@RestController
@RequestMapping("ai")
public class AiHealthController {

    /** 与 application.yml 中的占位值保持一致 */
    private static final String PLACEHOLDER_KEY = "sk-not-configured";

    private final AiProperties properties;
    private final AiQuotaService quotaService;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public AiHealthController(AiProperties properties,
                              AiQuotaService quotaService,
                              ObjectProvider<ChatModel> chatModelProvider,
                              @Value("${spring.ai.openai.api-key:}") String apiKey,
                              @Value("${spring.ai.openai.base-url:}") String baseUrl,
                              @Value("${spring.ai.openai.chat.options.model:}") String model) {
        this.properties = properties;
        this.quotaService = quotaService;
        this.chatModelProvider = chatModelProvider;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @GetMapping("health")
    public ReturnVO health() {
        boolean keyConfigured = apiKey != null
                && !apiKey.isBlank()
                && !PLACEHOLDER_KEY.equals(apiKey);
        boolean chatModelReady = chatModelProvider.getIfAvailable() != null;
        boolean available = properties.isEnabled() && keyConfigured && chatModelReady;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", available);
        data.put("switchOn", properties.isEnabled());
        data.put("keyConfigured", keyConfigured);
        data.put("chatModelReady", chatModelReady);
        data.put("model", model);
        data.put("baseUrl", baseUrl);
        data.put("dailyLimit", properties.getDailyLimit());
        data.put("maxSpotsPerDay", properties.getMaxSpotsPerDay());
        data.put("remainingToday", quotaService.remaining(SecurityUtil.currentUserId()));
        data.put("message", available
                ? "AI 已就绪"
                : (properties.isEnabled()
                        ? "AI 未配置 API Key，请在环境变量 DEEPSEEK_API_KEY 中填入后重启"
                        : "AI 开关已关闭（travel.ai.enabled=false）"));

        return ReturnVO.getSuccessDataReturnVO(data);
    }
}
