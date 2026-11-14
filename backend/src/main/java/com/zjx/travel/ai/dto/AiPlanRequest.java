package com.zjx.travel.ai.dto;

import lombok.Data;

/**
 * AI 行程规划请求。
 *
 * <p>刻意做成"结构化表单 + 一句自由补充"，而不是纯自然语言：
 * 天数、预算、偏好这些字段直接进 Prompt，比让模型从一句话里猜要稳得多。
 */
@Data
public class AiPlanRequest {

    /** 目的地，必填，如"西安" */
    private String destination;

    /** 天数，1-7，缺省 2 */
    private Integer days;

    /** 预算（元），可空 */
    private Double budget;

    /** 偏好，如"历史""自然""亲子""美食"，可空 */
    private String preference;

    /** 自由补充，如"不想太累""带老人"，可空 */
    private String extra;
}
