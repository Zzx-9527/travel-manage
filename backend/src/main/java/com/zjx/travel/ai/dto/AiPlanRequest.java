package com.zjx.travel.ai.dto;

import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;

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
    @NotBlank(message = "目的地不能为空")
    @Size(max = 50, message = "目的地名称过长")
    private String destination;

    /** 出发地，可空；为空时不设置 */
    @Size(max = 30, message = "出发地名称过长")
    private String startCity;

    /** 交通方式，可空；保存路线时为空则使用「其他」 */
    @Pattern(regexp = "^(地铁|高铁|飞机|大巴|自驾|其他)?$", message = "交通方式只能是：地铁、高铁、飞机、大巴、自驾、其他")
    private String traffic;

    /** 天数，1-7，缺省 2 */
    @NotNull(message = "天数不能为空")
    @Min(value = 1, message = "天数至少 1 天")
    @Max(value = 15, message = "天数最多 15 天")
    private Integer days;

    /** 预算（元），可空 */
    @DecimalMin(value = "0", message = "预算不能为负数")
    private Double budget;

    /** 偏好，如"历史""自然""亲子""美食"，可空 */
    private String preference;

    /** 自由补充，如"不想太累""带老人"，可空 */
    private String extra;
}
