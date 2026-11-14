package com.zjx.travel.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjx.travel.ai.dto.AiChatRequest;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.ai.service.AiChatService;
import com.zjx.travel.util.ReturnVO;
import com.zjx.travel.util.SecurityUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 助手对话接口（悬浮窗）。
 *
 * <p>与 /ai/plan 的关系：对话是"聊"，行程生成仍是 /ai/plan/stream。
 * 在对话里生成的行程卡片，通过 {@link #appendPlan} 写回对话记录，
 * 这样重新打开窗口还能看到那张卡片。
 */
@RestController
@RequestMapping("ai/chat")
public class AiChatController {

    private static final long SSE_TIMEOUT_MILLIS = 90_000L;

    private final AiChatService chatService;
    private final ObjectMapper objectMapper;

    public AiChatController(AiChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitter.onTimeout(emitter::complete);
        chatService.chatStream(request, SecurityUtil.currentUserId(), emitter);
        return emitter;
    }

    @GetMapping("history")
    public ReturnVO history(@RequestParam(defaultValue = "30") int limit) {
        try {
            return ReturnVO.getSuccessDataReturnVO(chatService.history(SecurityUtil.currentUserId(), limit));
        } catch (AiPlanException e) {
            return new ReturnVO(0, e.getMessage());
        }
    }

    @DeleteMapping("history")
    public ReturnVO clear() {
        try {
            return ReturnVO.getSuccessDataReturnVO(Map.of("deleted", chatService.clear(SecurityUtil.currentUserId())));
        } catch (AiPlanException e) {
            return new ReturnVO(0, e.getMessage());
        }
    }

    /**
     * 把对话里生成的行程卡片写进对话历史。
     *
     * <p>请求体：{@code {"title": "...", "plan": {...}}}。
     * 前端生成成功后调用一次，失败也不影响展示（卡片已经在页面上）。
     */
    @PostMapping("plan")
    public ReturnVO appendPlan(@RequestBody Map<String, Object> body) {
        try {
            Integer userId = SecurityUtil.currentUserId();
            String title = body.get("title") == null ? "AI 行程" : String.valueOf(body.get("title"));
            Object plan = body.get("plan");
            String payload = plan == null ? null : objectMapper.writeValueAsString(plan);
            chatService.appendPlan(userId, title, payload);
            return ReturnVO.getSuccessDataReturnVO(Map.of("saved", true));
        } catch (AiPlanException e) {
            return new ReturnVO(0, e.getMessage());
        } catch (Exception e) {
            return new ReturnVO(0, "行程卡片保存失败");
        }
    }
}
