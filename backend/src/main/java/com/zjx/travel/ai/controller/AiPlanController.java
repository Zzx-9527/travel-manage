package com.zjx.travel.ai.controller;

import com.zjx.travel.ai.dto.AiPlanRequest;
import com.zjx.travel.ai.dto.AiPlanSaveRequest;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.ai.service.AiPlanSaveService;
import com.zjx.travel.ai.service.AiPlanService;
import com.zjx.travel.util.ReturnVO;
import com.zjx.travel.util.SecurityUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * AI 行程规划接口。
 *
 * <p>两个版本并存：
 * <ul>
 *   <li>{@code POST /ai/plan}：同步返回，前端一个 axios 请求就能拿到完整结果，实现最简单；</li>
 *   <li>{@code POST /ai/plan/stream}：SSE 流式，边生成边推，首字延迟从秒级降到几百毫秒。</li>
 * </ul>
 * 业务失败一律 {@code code=0} + 可读提示；流式版本用 {@code event: error} 事件传递，
 * 因为 SSE 一旦开始输出，HTTP 状态码就改不了了。
 *
 * <p>注意：登录用户 id 必须在这里取好再传给 Service——Sa-Token 的上下文是
 * ThreadLocal，进了异步线程就拿不到了。
 */
@RestController
@RequestMapping("ai")
public class AiPlanController {

    /** SSE 连接最长存活时间：单次生成通常 3-10 秒，给到 2 分钟足够 */
    private static final long SSE_TIMEOUT_MILLIS = 120_000L;

    private final AiPlanService aiPlanService;
    private final AiPlanSaveService aiPlanSaveService;

    public AiPlanController(AiPlanService aiPlanService, AiPlanSaveService aiPlanSaveService) {
        this.aiPlanService = aiPlanService;
        this.aiPlanSaveService = aiPlanSaveService;
    }

    /**
     * 同步生成。
     *
     * <p>业务失败（未填目的地、模型不可用、格式异常）统一返回 {@code code=0}
     * + 可读提示，与平台其他接口的约定保持一致，不走 HTTP 错误码。
     */
    @PostMapping("plan")
    public ReturnVO plan(@RequestBody AiPlanRequest request) {
        try {
            return ReturnVO.getSuccessDataReturnVO(aiPlanService.plan(request, SecurityUtil.currentUserId()));
        } catch (AiPlanException e) {
            return new ReturnVO(0, e.getMessage());
        }
    }

    /**
     * 流式生成。
     *
     * <p>前端不能用 {@code EventSource}（它不支持自定义请求头，带不了 Authorization），
     * 需要走 {@code fetch} + {@code ReadableStream} 手动解析 SSE 帧。
     */
    @PostMapping(value = "plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter planStream(@RequestBody AiPlanRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitter.onTimeout(emitter::complete);
        aiPlanService.planStream(request, SecurityUtil.currentUserId(), emitter);
        return emitter;
    }

    /**
     * 把生成结果保存为"我的路线"（route + route_detail，状态待审核）。
     */
    @PostMapping("plan/save")
    public ReturnVO save(@RequestBody AiPlanSaveRequest request) {
        try {
            Integer routeId = aiPlanSaveService.save(request, SecurityUtil.currentUserId());
            return ReturnVO.getSuccessDataReturnVO(Map.of("routeId", routeId));
        } catch (AiPlanException e) {
            return new ReturnVO(0, e.getMessage());
        }
    }
}
