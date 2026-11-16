package com.zjx.travel.ai.service;

import com.zjx.travel.ai.dto.AiChatRequest;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.ai.support.AiErrors;
import com.zjx.travel.domain.AiChatMessageEntity;
import com.zjx.travel.mapper.AiChatMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 助手对话（悬浮窗里的聊天）。
 *
 * <p>与"排行程"的区别：这里不要 JSON，就要人话，所以不做结构化校验，
 * 但同样受配额、上游错误翻译、SSE 断流即停的约束。
 *
 * <p>上下文策略：每次取最近 10 条**文本**消息一起发给模型。
 * 不把行程卡片（plan 类型）塞进上下文——那是结构化数据，模型读它没有收益，只会白烧 token。
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    /** 一次带多少条历史消息，约等于 5 轮对话 */
    private static final int CONTEXT_LIMIT = 10;

    /** 单条消息上限，与前端保持一致 */
    private static final int MAX_MESSAGE_LENGTH = 500;

    /** 落库时 assistant 回复的截断长度，防止异常长回复把 TEXT 撑爆 */
    private static final int MAX_STORE_LENGTH = 4000;

    private static final String SYSTEM_PROMPT = """
            你是「旅迹」平台的旅游助手，帮用户解决出行相关的问题。

            回答要求：
            1. 用简体中文，简洁、口语化，一般不超过 200 字；需要列举时用短横线，最多 5 条。
            2. 只聊出行相关：目的地选择、行程安排、交通、住宿、美食、门票、季节与注意事项。
              与旅游无关的问题，礼貌说明你只负责出行，并把话题引回旅游。
            3. 平台内的景点、门票、路线以平台数据为准。不要编造具体价格、库存、营业时间；
               不确定就说"以详情页为准"。
            4. 用户想让你直接排一份完整行程时，告诉他：点输入框上方的「帮我规划行程」，
               填目的地和天数，系统会按平台在售景点生成，并且能保存成路线。
            5. 不要输出 markdown 标题、代码块或表格，纯文本即可。
            """;

    private final ChatClient chatClient;
    private final AiChatMessageMapper messageMapper;
    private final AiQuotaService quotaService;
    private final ExecutorService streamExecutor;

    /** 表不存在时只告警一次 */
    private final AtomicBoolean tableWarned = new AtomicBoolean(false);

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         AiChatMessageMapper messageMapper,
                         AiQuotaService quotaService,
                         @Qualifier("aiStreamExecutor") ExecutorService streamExecutor) {
        this.chatClient = chatClientBuilder.build();
        this.messageMapper = messageMapper;
        this.quotaService = quotaService;
        this.streamExecutor = streamExecutor;
    }

    /**
     * 流式对话。事件：{@code delta} 增量文本 / {@code done} 结束 / {@code error} 可读错误。
     */
    public void chatStream(AiChatRequest request, Integer userId, SseEmitter emitter) {
        streamExecutor.execute(() -> {
            String text = request == null || request.getMessage() == null ? "" : request.getMessage().trim();
            if (text.isEmpty()) {
                sendEvent(emitter, "error", Map.of("msg", "说点什么吧"));
                safeComplete(emitter);
                return;
            }
            if (text.length() > MAX_MESSAGE_LENGTH) {
                text = text.substring(0, MAX_MESSAGE_LENGTH);
            }

            boolean counted = false;
            try {
                quotaService.checkAndConsumeChat(userId);
                counted = true;

                // 先取历史再存当前这句，避免把自己重复放进上下文
                List<AiChatMessageEntity> history = loadRecent(userId, CONTEXT_LIMIT);
                save(userId, "user", "text", text, null);

                List<Message> messages = new ArrayList<>(history.size() + 2);
                messages.add(new SystemMessage(SYSTEM_PROMPT));
                for (AiChatMessageEntity item : history) {
                    if (!"text".equals(item.getMsgType()) || item.getContent() == null) {
                        continue;
                    }
                    if ("user".equals(item.getRole())) {
                        messages.add(new UserMessage(item.getContent()));
                    } else {
                        messages.add(new AssistantMessage(item.getContent()));
                    }
                }
                messages.add(new UserMessage(text));

                StringBuilder answer = new StringBuilder();
                for (String chunk : chatClient.prompt().messages(messages).stream().content().toIterable()) {
                    answer.append(chunk);
                    if (!sendEvent(emitter, "delta", Map.of("text", chunk))) {
                        // 客户端断开：不保存半截回复
                        return;
                    }
                }

                String reply = answer.toString().trim();
                if (!reply.isEmpty()) {
                    save(userId, "assistant", "text", truncate(reply, MAX_STORE_LENGTH), null);
                }
                sendEvent(emitter, "done", Map.of("length", reply.length()));
                emitter.complete();
                log.info("AI 对话完成：userId={}，问={}字，答={}字", userId, text.length(), reply.length());
            } catch (AiPlanException e) {
                if (counted) {
                    quotaService.refundChat(userId);
                }
                sendEvent(emitter, "error", Map.of("msg", e.getMessage()));
                safeComplete(emitter);
            } catch (Exception e) {
                log.error("AI 对话失败", e);
                if (counted) {
                    quotaService.refundChat(userId);
                }
                sendEvent(emitter, "error", Map.of("msg", AiErrors.friendly(e)));
                safeComplete(emitter);
            }
        });
    }

    /** 历史消息，按时间正序返回 */
    public List<Map<String, Object>> history(Integer userId, int limit) {
        if (userId == null) {
            throw new AiPlanException("请先登录");
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return loadRecent(userId, safeLimit).stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("role", item.getRole());
            map.put("type", item.getMsgType());
            map.put("content", item.getContent());
            map.put("payload", item.getPayload());
            map.put("createTime", item.getCreateTime());
            return map;
        }).toList();
    }

    public int clear(Integer userId) {
        if (userId == null) {
            throw new AiPlanException("请先登录");
        }
        try {
            return messageMapper.deleteByUser(userId);
        } catch (Exception e) {
            warnOnce("清空对话失败", e);
            return 0;
        }
    }

    /** 把对话里生成的行程卡片写进历史，重新打开窗口还能看到 */
    public void appendPlan(Integer userId, String title, String payloadJson) {
        save(userId, "assistant", "plan", truncate(title, 200), payloadJson);
    }

    /** 倒序取 N 条后反转成对话顺序；表不存在时降级为空上下文 */
    private List<AiChatMessageEntity> loadRecent(Integer userId, int limit) {
        try {
            List<AiChatMessageEntity> rows = messageMapper.recentByUser(userId, limit);
            List<AiChatMessageEntity> ordered = new ArrayList<>(rows);
            java.util.Collections.reverse(ordered);
            return ordered;
        } catch (Exception e) {
            warnOnce("读取对话历史失败", e);
            return List.of();
        }
    }

    private void save(Integer userId, String role, String type, String content, String payload) {
        AiChatMessageEntity message = new AiChatMessageEntity();
        message.setUserId(userId);
        message.setRole(role);
        message.setMsgType(type);
        message.setContent(content);
        message.setPayload(payload);
        try {
            messageMapper.insert(message);
        } catch (Exception e) {
            warnOnce("写入对话记录失败", e);
        }
    }

    private void warnOnce(String action, Exception e) {
        if (tableWarned.compareAndSet(false, true)) {
            log.warn("{}（若尚未建表，请执行 backend/sql/schema.sql）：{}", action, e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 推送失败（客户端可能已断开）：event={}, cause={}", event, e.toString());
            return false;
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 客户端已断开时 complete 会抛异常，忽略
        }
    }
}
