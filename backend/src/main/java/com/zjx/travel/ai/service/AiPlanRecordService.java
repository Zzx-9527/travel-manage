package com.zjx.travel.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjx.travel.ai.dto.AiPlanRequest;
import com.zjx.travel.ai.dto.AiPlanResult;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.domain.AiPlanRecordEntity;
import com.zjx.travel.mapper.AiPlanRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 生成记录与用量统计。
 *
 * <p>设计原则：<b>记录是旁路，绝不拖累主流程</b>。
 * 表还没建、库抖动、结果序列化失败，都只记日志，不影响用户拿到行程。
 * 因此所有写库操作都包在 try/catch 里，且"表不存在"的告警只打一次，避免刷屏。
 */
@Service
public class AiPlanRecordService {

    private static final Logger log = LoggerFactory.getLogger(AiPlanRecordService.class);

    private static final int MAX_ERROR_LENGTH = 255;

    private final AiPlanRecordMapper recordMapper;
    private final ObjectMapper objectMapper;

    /** 表不存在时只告警一次 */
    private final AtomicBoolean writeFailureWarned = new AtomicBoolean(false);

    public AiPlanRecordService(AiPlanRecordMapper recordMapper, ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
    }

    public void success(Integer userId, AiPlanRequest request, AiPlanResult result, long latencyMs) {
        AiPlanRecordEntity record = baseRecord(userId, request, latencyMs);
        record.setStatus(1);
        record.setScenicCount(countSpots(result));
        record.setDroppedSpots(result.getDroppedSpots() == null ? 0 : result.getDroppedSpots());
        record.setModel(result.getModel());
        try {
            record.setResultJson(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("AI 结果序列化失败，记录里不存快照：{}", e.getMessage());
        }
        save(record);
    }

    public void failure(Integer userId, AiPlanRequest request, String error, long latencyMs) {
        AiPlanRecordEntity record = baseRecord(userId, request, latencyMs);
        record.setStatus(0);
        record.setScenicCount(0);
        record.setDroppedSpots(0);
        record.setErrorMsg(truncate(error));
        save(record);
    }

    /**
     * 管理端看板数据。表不存在时返回 {@code tableReady=false}，
     * 前端据此提示"请先执行建表 SQL"，而不是甩一个 500。
     */
    public Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            Map<String, Object> summary = recordMapper.todaySummary();
            data.put("tableReady", true);
            data.put("summary", summary == null ? Map.of() : summary);
            data.put("trend", recordMapper.dailyTrend(6));
            data.put("topDestinations", recordMapper.topDestinations(5));
            data.put("recent", recordMapper.recent(10));
        } catch (Exception e) {
            log.warn("AI 用量统计查询失败（若尚未建表，请执行 backend/sql/ai_plan_record.sql）：{}", e.getMessage());
            data.put("tableReady", false);
            data.put("message", "记录表尚未创建，请先执行 backend/sql/ai_plan_record.sql");
        }
        return data;
    }

    /**
     * 当前用户的历史生成记录（分页）。
     *
     * <p>只返回"够渲染列表"的字段，标题从结果快照里解析；
     * 失败记录也返回，前端可以显示"上次失败"，但不可恢复。
     */
    public Map<String, Object> history(Integer userId, int page, int size) {
        if (userId == null) {
            throw new AiPlanException("请先登录");
        }
        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 1);
        long total = recordMapper.countByUser(userId);
        List<AiPlanRecordEntity> rows = recordMapper.pageByUser(userId, (safePage - 1) * safeSize, safeSize);

        List<Map<String, Object>> items = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("destination", row.getDestination());
            item.put("days", row.getDays());
            item.put("budget", row.getBudget());
            item.put("preference", row.getPreference());
            item.put("extra", row.getExtra());
            item.put("status", row.getStatus());
            item.put("scenicCount", row.getScenicCount());
            item.put("latencyMs", row.getLatencyMs());
            item.put("createTime", row.getCreateTime());
            item.put("title", extractTitle(row));
            return item;
        }).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("items", items);
        return data;
    }

    /** 按 id 取回完整行程快照；只能取自己的记录 */
    public Map<String, Object> detail(Long id, Integer userId) {
        if (userId == null) {
            throw new AiPlanException("请先登录");
        }
        AiPlanRecordEntity row = recordMapper.findByIdAndUser(id, userId);
        if (row == null) {
            throw new AiPlanException("记录不存在或无权访问");
        }
        if (row.getStatus() == null || row.getStatus() != 1 || row.getResultJson() == null) {
            throw new AiPlanException("这条记录没有可恢复的行程");
        }
        try {
            return objectMapper.readValue(row.getResultJson(), Map.class);
        } catch (Exception e) {
            log.warn("历史行程快照解析失败，recordId={}, cause={}", id, e.getMessage());
            throw new AiPlanException("行程快照损坏，无法恢复");
        }
    }

    /** 从结果快照里取标题；失败记录或解析异常返回 null */
    private String extractTitle(AiPlanRecordEntity row) {
        if (row.getStatus() == null || row.getStatus() != 1 || row.getResultJson() == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(row.getResultJson());
            String title = node.path("title").asText(null);
            return title == null || title.isBlank() ? null : title;
        } catch (Exception e) {
            return null;
        }
    }

    private AiPlanRecordEntity baseRecord(Integer userId, AiPlanRequest request, long latencyMs) {        AiPlanRecordEntity record = new AiPlanRecordEntity();
        record.setUserId(userId);
        if (request != null) {
            record.setDestination(truncateValue(request.getDestination(), 64));
            record.setDays(request.getDays() == null ? 0 : request.getDays());
            record.setBudget(request.getBudget() == null
                    ? null : java.math.BigDecimal.valueOf(request.getBudget()));
            record.setPreference(truncateValue(request.getPreference(), 64));
            record.setExtra(truncateValue(request.getExtra(), 255));
        }
        record.setLatencyMs((int) Math.min(Integer.MAX_VALUE, latencyMs));
        return record;
    }

    private void save(AiPlanRecordEntity record) {
        try {
            recordMapper.insert(record);
        } catch (Exception e) {
            if (writeFailureWarned.compareAndSet(false, true)) {
                log.warn("AI 生成记录写库失败（若尚未建表，请执行 backend/sql/ai_plan_record.sql）：{}",
                        e.getMessage());
            }
        }
    }

    private int countSpots(AiPlanResult result) {
        if (result == null || result.getDays() == null) {
            return 0;
        }
        return result.getDays().stream()
                .mapToInt(day -> day.getSpots() == null ? 0 : day.getSpots().size())
                .sum();
    }

    private String truncate(String value) {
        return truncateValue(value, MAX_ERROR_LENGTH);
    }

    private String truncateValue(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
