package com.zjx.travel.mapper;

import com.zjx.travel.domain.AiPlanRecordEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AI 生成记录（表 ai_plan_record）。
 *
 * <p>聚合查询直接返回 Map，避免为看板单独建一堆 VO；字段名由 SQL 的别名决定。
 */
public interface AiPlanRecordMapper {

    int insert(AiPlanRecordEntity record);

    /** 今日汇总：总数、成功、失败、平均耗时、过滤条目数 */
    Map<String, Object> todaySummary();

    /** 近 N 天每日生成量（含成功数） */
    List<Map<String, Object>> dailyTrend(@Param("days") int days);

    /** 热门目的地 TOP N */
    List<Map<String, Object>> topDestinations(@Param("limit") int limit);

    /** 最近若干条记录 */
    List<AiPlanRecordEntity> recent(@Param("limit") int limit);
}
