package com.zjx.travel.ai.support;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把上游异常翻译成用户看得懂的话（规划与对话共用）。
 *
 * <p>Spring AI 的 OpenAI 客户端在收到 4xx/5xx 时抛的是
 * {@code NonTransientAiException}，<b>消息形如 {@code "HTTP 401 - {...}"}，而且没有 cause</b>，
 * 所以不能只沿着 cause 链找 {@code RestClientResponseException}——那样会全部落到兜底文案。
 * 这里两条路都走：先看异常类型，再从消息里抠状态码。
 *
 * <p>分流的目的是让用户分清"该等等"（429/5xx/超时）和"该找人"（401/403）。
 */
public final class AiErrors {

    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP (\\d{3})");

    private AiErrors() {
    }

    public static String friendly(Throwable error) {
        Throwable cause = error;
        int depth = 0;
        while (cause != null && depth++ < 8) {
            if (cause instanceof RestClientResponseException httpError) {
                return messageByStatus(httpError.getStatusCode().value());
            }
            if (cause instanceof ResourceAccessException || cause instanceof SocketTimeoutException) {
                return "AI 服务连接失败或响应超时，请稍后重试";
            }
            Integer status = extractHttpStatus(cause.getMessage());
            if (status != null) {
                return messageByStatus(status);
            }
            cause = cause.getCause();
        }
        return "AI 服务暂时不可用，请稍后重试";
    }

    private static String messageByStatus(int status) {
        if (status == 401 || status == 403) {
            return "AI Key 无效或无权限，请联系管理员";
        }
        if (status == 429) {
            return "AI 服务当前请求过多（限流），请稍后再试";
        }
        if (status >= 500) {
            return "AI 服务暂时不可用，请稍后重试";
        }
        return "AI 调用失败（HTTP " + status + "），请稍后重试";
    }

    private static Integer extractHttpStatus(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(message);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
