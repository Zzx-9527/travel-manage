package com.zjx.travel.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * "把 AI 行程保存为我的路线"的请求体。
 *
 * <p>为什么前端要把整个行程回传，而不是后端缓存后再按 id 取：
 * 这样后端是无状态的，不需要为临时结果维护缓存和过期策略；
 * 代价是请求体稍大（几 KB），可以接受。
 *
 * <p>服务端不会盲信前端传来的景点：保存前会再用 scenic 表校验一遍 id。
 */
@Data
public class AiPlanSaveRequest {

    /** 出发城市 / 目的地 */
    private String destination;

    /** 出发地；为空时不设置 */
    private String startCity;

    /** 路线交通方式；为空或非法时保存为「其他」 */
    private String traffic;

    /** 预算 */
    private Double budget;

    /** 行程标题 */
    private String title;

    /** 行程简介 */
    private String intro;

    /** 按天的行程（结构复用生成结果） */
    private List<AiPlanResult.Day> days;

    /** 实用提示，拼进路线简介 */
    private List<String> tips;
}
