package com.zjx.travel.ai.controller;

import com.zjx.travel.ai.dto.AiPlanRequest;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.ai.service.AiPlanService;
import com.zjx.travel.util.ReturnVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 行程规划接口。
 *
 * <p>当前是同步版本：等模型生成完一次性返回。响应体结构与平台其他接口一致
 * （{@code {code,msg,content}}），因此前端可以复用现有的 axios 封装。
 * 流式版本（SSE）放在下一步，接口名会区分开，避免和这个同步接口混淆。
 */
@RestController
@RequestMapping("ai")
public class AiPlanController {

    private final AiPlanService aiPlanService;

    public AiPlanController(AiPlanService aiPlanService) {
        this.aiPlanService = aiPlanService;
    }

    /**
     * 生成行程。
     *
     * <p>业务失败（未填目的地、模型不可用、格式异常）统一返回 {@code code=0}
     * + 可读提示，与平台其他接口的约定保持一致，不走 HTTP 错误码。
     */
    @PostMapping("plan")
    public ReturnVO plan(@RequestBody AiPlanRequest request) {
        try {
            return ReturnVO.getSuccessDataReturnVO(aiPlanService.plan(request));
        } catch (AiPlanException e) {
            return new ReturnVO(0, e.getMessage());
        }
    }
}
