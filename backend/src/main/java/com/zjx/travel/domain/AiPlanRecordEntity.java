package com.zjx.travel.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 行程生成记录（对应表 ai_plan_record）。
 *
 * <p>只做"写一条 + 管理端聚合查询"，不参与业务逻辑：
 * 记录失败不影响生成结果，因此所有写库操作在 Service 里都被 try/catch 包住。
 */
@Data
public class AiPlanRecordEntity {

    private Long id;

    private Integer userId;

    private String destination;

    private Integer days;

    private BigDecimal budget;

    private String preference;

    private String extra;

    /** 1=成功 0=失败 */
    private Integer status;

    private String errorMsg;

    /** 最终有效景点数 */
    private Integer scenicCount;

    /** 被白名单过滤掉的条目数，可近似看作模型幻觉量 */
    private Integer droppedSpots;

    private Integer latencyMs;

    private String model;

    /** 生成结果快照（失败时为空） */
    private String resultJson;

    private String createTime;
}
