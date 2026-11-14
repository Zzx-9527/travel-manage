package com.zjx.travel.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 生成的结构化行程。
 *
 * <p>字段分两类，边界要清楚：
 * <ul>
 *   <li><b>模型返回的</b>：title / intro / days / tips，其中 spots 只允许带
 *       scenicId、stayTime、reason——模型的职责是"选哪个、为什么"，不是"这个景点多少钱"；</li>
 *   <li><b>服务端补的</b>：spots 里的 name / price / score / address / 经纬度 / 封面，
 *       以及 model、latencyMs、droppedSpots。价格、坐标这类事实数据一律以库为准，
 *       不采信模型输出，避免"AI 说门票 60 块"这种线上事故。</li>
 * </ul>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPlanResult {

    private String title;

    private String intro;

    private List<Day> days;

    private List<String> tips;

    /** 实际使用的模型名（服务端填写） */
    private String model;

    /** 本次调用耗时（毫秒，服务端填写） */
    private Long latencyMs;

    /** 被服务端白名单过滤掉的"幻觉景点"数量（服务端填写，用于观察模型稳定性） */
    private Integer droppedSpots;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Day {

        private Integer dayNo;

        /** 当天主题，如"古城墙与钟鼓楼" */
        private String theme;

        private List<Spot> spots;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Spot {

        /** 景点 id，必须命中候选清单，否则整条被丢弃 */
        private Integer scenicId;

        /** 时段：上午 / 下午 / 晚上 */
        private String timeSlot;

        /** 建议停留时长，如"2小时" */
        private String stayTime;

        /** 从上一站到这里的交通方式与耗时（第一站从住处出发） */
        private String traffic;

        /** 景点介绍：看点与背景，60-100 字 */
        private String intro;

        /** 注意事项：预约、闭馆、排队、穿着等 */
        private String notice;

        /** 推荐理由，模型生成 */
        private String reason;

        // ===== 以下由服务端从数据库补全，模型给的值会被覆盖 =====
        private String name;
        private Double price;
        private Double score;
        private String address;
        private String openTime;
        private Double longitude;
        private Double latitude;
        private String cover;
    }
}
