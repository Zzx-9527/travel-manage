package com.zjx.travel.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjx.travel.ai.config.AiProperties;
import com.zjx.travel.ai.dto.AiPlanRequest;
import com.zjx.travel.ai.dto.AiPlanResult;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.ai.prompt.PlanPromptBuilder;
import com.zjx.travel.ai.support.AiErrors;
import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.mapper.ScenicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * AI 行程规划的核心编排。
 *
 * <p>一次完整链路：参数归一化 → 取候选景点 → 组装 Prompt → 调模型 → 解析 JSON
 * → <b>白名单校验并补全事实数据</b> → 返回。
 *
 * <p>校验这一步是整条链路的核心价值，防的是模型的三类典型问题：
 * <ul>
 *   <li><b>幻觉景点</b>：scenicId 不在候选清单里 → 整条丢弃；</li>
 *   <li><b>重复景点</b>：同一个景点被安排进多天 → 只保留第一次；</li>
 *   <li><b>越界与超量</b>：dayNo 超出天数、每天景点数超上限 → 丢弃多余部分。</li>
 * </ul>
 * 丢弃数量会记进 {@code droppedSpots}，可以当作"模型幻觉率"的观测指标。
 */
@Service
public class AiPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiPlanService.class);

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 7;
    private static final int DEFAULT_DAYS = 2;

    /** 候选景点上限：控制 Prompt 体积，40 个约 1.2k tokens */
    private static final int MAX_CANDIDATES = 40;

    /** 时段顺序，用于把模型给的景点按 上午→下午→晚上 排好 */
    private static final List<String> SLOT_ORDER = List.of("上午", "下午", "晚上");

    /** 单个景点的文本上限，防止模型写超长把卡片撑爆 */
    private static final int MAX_TRAFFIC_LENGTH = 120;
    private static final int MAX_INTRO_LENGTH = 220;
    private static final int MAX_NOTICE_LENGTH = 160;

    private final ChatClient chatClient;
    private final ScenicMapper scenicMapper;
    private final PlanPromptBuilder promptBuilder;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final String model;
    private final AiQuotaService quotaService;
    private final AiPlanRecordService recordService;
    private final ExecutorService streamExecutor;

    public AiPlanService(ChatClient.Builder chatClientBuilder,
                         ScenicMapper scenicMapper,
                         PlanPromptBuilder promptBuilder,
                         AiProperties properties,
                         ObjectMapper objectMapper,
                         AiQuotaService quotaService,
                         AiPlanRecordService recordService,
                         @Qualifier("aiStreamExecutor") ExecutorService streamExecutor,
                         @Value("${spring.ai.openai.chat.options.model:unknown}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.scenicMapper = scenicMapper;
        this.promptBuilder = promptBuilder;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.quotaService = quotaService;
        this.recordService = recordService;
        this.streamExecutor = streamExecutor;
        this.model = model;
    }

    public AiPlanResult plan(AiPlanRequest request, Integer userId) {
        if (!quotaService.tryAcquireGeneration(userId)) {
            throw new AiPlanException("上一条行程还在生成中，请稍候再试");
        }
        long totalStart = System.currentTimeMillis();
        AiPlanRequest req = null;
        try {
            req = normalize(request);
            // 先确认目的地有景点可排，再扣配额：目的地写错不该消耗今天的次数
            List<ScenicEntity> candidates = loadCandidates(req.getDestination());
            quotaService.checkAndConsume(userId);

            // 扣了配额之后的任何失败都要返还：用户没拿到结果，不该损失今天的次数
            try {
                long start = System.currentTimeMillis();
                String raw;
                try {
                    raw = chatClient.prompt()
                            .system(promptBuilder.systemPrompt())
                            .user(promptBuilder.userPrompt(req, candidates))
                            .call()
                            .content();
                } catch (Exception e) {
                    // 上游错误（超时、401、限流）只记日志，不把原始报文返回给前端
                    log.error("调用大模型失败：目的地={}，天数={}", req.getDestination(), req.getDays(), e);
                    throw new AiPlanException(friendlyLlmError(e));
                }
                long latency = System.currentTimeMillis() - start;

                AiPlanResult result = parse(raw);
                int dropped = validateAndEnrich(result, req, candidates);

                result.setModel(model);
                result.setLatencyMs(latency);
                result.setDroppedSpots(dropped);

                log.info("AI 行程生成完成：目的地={}，天数={}，有效景点={}，丢弃={}，耗时={}ms",
                        req.getDestination(), req.getDays(), countSpots(result), dropped, latency);
                recordService.success(userId, req, result, System.currentTimeMillis() - totalStart);
                return result;
            } catch (RuntimeException e) {
                quotaService.refund(userId);
                recordService.failure(userId, req, e.getMessage(), System.currentTimeMillis() - totalStart);
                throw e;
            }
        } finally {
            quotaService.releaseGeneration(userId);
        }
    }

    /** 上游异常翻译，规则见 {@link AiErrors}（对话与规划共用一份，避免两处口径不一致） */
    private String friendlyLlmError(Throwable error) {
        return AiErrors.friendly(error);
    }

    /**
     * 流式生成：边收边推 {@code delta}（前端做打字机），收完再做校验并推最终结构。
     *
     * <p>为什么 delta 推的是原始 JSON 文本，而不是漂亮的人话：模型的职责就是产出结构化数据，
     * 额外让它先叙旧会让 Prompt 更复杂、更容易跑偏。前端把 delta 渲染成"生成中"的过程内容，
     * 收到 plan 事件后再整体替换成行程卡片。
     *
     * <p>注意：userId 必须在调用线程取好再传进来——Sa-Token 的登录态存在 ThreadLocal 里，
     * 换到线程池里再取是拿不到的。
     */
    public void planStream(AiPlanRequest request, Integer userId, SseEmitter emitter) {
        streamExecutor.execute(() -> {
            long start = System.currentTimeMillis();
            if (!quotaService.tryAcquireGeneration(userId)) {
                sendEvent(emitter, "error", Map.of("msg", "上一条行程还在生成中，请稍候再试"));
                safeComplete(emitter);
                return;
            }
            boolean quotaCounted = false;
            AiPlanRequest req = null;
            try {
                req = normalize(request);
                // 先确认目的地有景点可排，再扣配额：目的地写错不该消耗今天的次数
                List<ScenicEntity> candidates = loadCandidates(req.getDestination());
                quotaService.checkAndConsume(userId);
                quotaCounted = true;

                Flux<String> flux = chatClient.prompt()
                        .system(promptBuilder.systemPrompt())
                        .user(promptBuilder.userPrompt(req, candidates))
                        .stream()
                        .content();

                StringBuilder buffer = new StringBuilder();
                for (String chunk : flux.toIterable()) {
                    buffer.append(chunk);
                    if (!sendEvent(emitter, "delta", Map.of("text", chunk))) {
                        // 前端已断开：停止读取上游流，避免继续烧 token
                        return;
                    }
                }

                long latency = System.currentTimeMillis() - start;
                AiPlanResult result = parse(buffer.toString());
                int dropped = validateAndEnrich(result, req, candidates);
                result.setModel(model);
                result.setLatencyMs(latency);
                result.setDroppedSpots(dropped);

                sendEvent(emitter, "plan", result);
                sendEvent(emitter, "done", Map.of("latencyMs", latency, "droppedSpots", dropped));
                emitter.complete();

                log.info("AI 行程流式生成完成：目的地={}，天数={}，景点={}，丢弃={}，耗时={}ms",
                        req.getDestination(), req.getDays(), countSpots(result), dropped, latency);
                recordService.success(userId, req, result, latency);
            } catch (AiPlanException e) {
                if (quotaCounted) {
                    quotaService.refund(userId);
                }
                recordService.failure(userId, req, e.getMessage(), System.currentTimeMillis() - start);
                sendEvent(emitter, "error", Map.of("msg", e.getMessage()));
                safeComplete(emitter);
            } catch (Exception e) {
                log.error("AI 流式生成失败", e);
                if (quotaCounted) {
                    quotaService.refund(userId);
                }
                String message = friendlyLlmError(e);
                recordService.failure(userId, req, message, System.currentTimeMillis() - start);
                sendEvent(emitter, "error", Map.of("msg", message));
                safeComplete(emitter);
            } finally {
                quotaService.releaseGeneration(userId);
            }
        });
    }

    /** 推送一个 SSE 事件；连接已断开时返回 false，调用方据此停止后续处理 */
    private boolean sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 推送失败（客户端可能已断开）：event={}, cause={}", event, e.toString());
            return false;
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 客户端已断开时 complete 会抛 IllegalStateException，忽略即可
        }
    }

    // ==================== 参数归一化 ====================

    private AiPlanRequest normalize(AiPlanRequest request) {
        if (request == null) {
            throw new AiPlanException("请求不能为空");
        }
        String destination = request.getDestination() == null ? "" : request.getDestination().trim();
        if (destination.isEmpty()) {
            throw new AiPlanException("请先填写目的地");
        }

        AiPlanRequest req = new AiPlanRequest();
        req.setDestination(truncate(destination, 30));
        int days = request.getDays() == null ? DEFAULT_DAYS : request.getDays();
        req.setDays(Math.max(MIN_DAYS, Math.min(MAX_DAYS, days)));
        if (request.getBudget() != null && request.getBudget() > 0) {
            req.setBudget(request.getBudget());
        }
        req.setPreference(truncate(request.getPreference(), 20));
        req.setExtra(truncate(request.getExtra(), 200));
        return req;
    }

    // ==================== 候选景点 ====================

    /**
     * 取候选景点：只保留名称或地址命中目的地的景点。
     *
     * <p><b>不做"没匹配上就退回全量"的兜底。</b>曾经这么做过，结果是
     * 用户输入"丽江"（库里没有），模型从全量候选里挑了乌镇、西湖、张家界、黄山，
     * 生成了一份看起来像模像样、实际完全错误的行程——这比直接说"没有"更糟。
     *
     * <p>宁可返回一个明确的"未收录"，也不产出一份误导性结果。
     */
    private List<ScenicEntity> loadCandidates(String destination) {
        List<ScenicEntity> all = scenicMapper.queryActiveList();
        if (all == null || all.isEmpty()) {
            throw new AiPlanException("景点库暂无可上架景点，无法生成行程");
        }
        List<String> keywords = matchKeywords(destination);
        List<ScenicEntity> matched = all.stream()
                .filter(s -> keywords.stream()
                        .anyMatch(k -> contains(s.getName(), k) || contains(s.getAddress(), k)))
                .sorted(Comparator.<ScenicEntity, Double>comparing(
                        ScenicEntity::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_CANDIDATES)
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            throw new AiPlanException(notCoveredMessage(destination, all));
        }
        return matched;
    }

    /**
     * 生成用于匹配的候选关键词。
     *
     * <p>用户输入的是"目的地"，未必和库里的写法一致：库里地址是"浙江省杭州市西湖区"，
     * 用户填"杭州西湖"时整串匹配不上，但它确实存在。所以整串匹配失败后再退化到前两个字
     * （"杭州"）试一次——这类"城市 + 具体景点"的输入很常见。
     *
     * <p>不用模糊包含或编辑距离：那会让"南京"命中"北京"之类的误匹配，
     * 宁可拒绝也不能给错行程。
     */
    private List<String> matchKeywords(String destination) {
        List<String> keywords = new ArrayList<>(2);
        keywords.add(destination);
        if (destination.length() > 2) {
            keywords.add(destination.substring(0, 2));
        }
        return keywords;
    }

    /** 目的地未收录时的提示：告诉用户现在能规划哪些地方，而不是只说"没有" */
    private String notCoveredMessage(String destination, List<ScenicEntity> all) {
        String samples = all.stream()
                .sorted(Comparator.<ScenicEntity, Double>comparing(
                        ScenicEntity::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(ScenicEntity::getName)
                .collect(Collectors.joining("、"));
        return "平台暂未收录「" + destination + "」的景点，无法规划该目的地。"
                + "当前收录的景点有：" + samples + " 等，换个目的地试试。";
    }

    // ==================== 解析 ====================

    private AiPlanResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AiPlanException("AI 没有返回内容，请重试");
        }
        String json = stripCodeFence(raw.trim());
        try {
            return objectMapper.readValue(json, AiPlanResult.class);
        } catch (Exception e) {
            log.warn("AI 返回内容不是合法 JSON，原始内容前 500 字：{}", abbreviate(json));
            throw new AiPlanException("AI 返回格式异常，请重试");
        }
    }

    /** 有些模型会给 JSON 套一层 ```json 代码块，这里剥掉 */
    private String stripCodeFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        if (firstLineEnd < 0) {
            return text;
        }
        String body = text.substring(firstLineEnd + 1);
        int fenceEnd = body.lastIndexOf("```");
        return (fenceEnd >= 0 ? body.substring(0, fenceEnd) : body).trim();
    }

    // ==================== 校验与补全 ====================

    /**
     * @return 被丢弃的景点条目数（含幻觉 id、重复、越界、超量）
     */
    private int validateAndEnrich(AiPlanResult result, AiPlanRequest req, List<ScenicEntity> candidates) {
        Map<Integer, ScenicEntity> byId = new HashMap<>();
        for (ScenicEntity scenic : candidates) {
            byId.put(scenic.getId(), scenic);
        }

        int dropped = 0;
        Set<Integer> usedScenicIds = new HashSet<>();
        Map<Integer, AiPlanResult.Day> byDayNo = new TreeMap<>();

        List<AiPlanResult.Day> modelDays = result.getDays() == null ? List.of() : result.getDays();
        for (AiPlanResult.Day day : modelDays) {
            if (day == null) {
                continue;
            }
            int dayNo = day.getDayNo() == null ? -1 : day.getDayNo();
            if (dayNo < MIN_DAYS || dayNo > req.getDays() || byDayNo.containsKey(dayNo)) {
                dropped += sizeOf(day.getSpots());
                continue;
            }

            List<AiPlanResult.Spot> spots = new ArrayList<>();
            for (AiPlanResult.Spot spot : day.getSpots() == null
                    ? List.<AiPlanResult.Spot>of() : day.getSpots()) {
                if (spot == null || spot.getScenicId() == null) {
                    dropped++;
                    continue;
                }
                ScenicEntity scenic = byId.get(spot.getScenicId());
                if (scenic == null) {
                    // 幻觉景点：id 不在候选清单里
                    dropped++;
                    continue;
                }
                if (!usedScenicIds.add(spot.getScenicId())) {
                    dropped++;
                    continue;
                }
                if (spots.size() >= properties.getMaxSpotsPerDay()) {
                    dropped++;
                    continue;
                }
                enrich(spot, scenic);
                spots.add(spot);
            }

            // 模型偶尔会把"晚上"排在"上午"前面，这里按固定时段顺序稳定排序
            spots.sort(Comparator.comparingInt(spot -> slotRank(spot.getTimeSlot())));
            day.setDayNo(dayNo);
            day.setSpots(spots);
            if (spots.isEmpty()) {
                // 当天没有可安排的景点（候选不足或被校验清空）：统一显示"自由活动"，
                // 避免出现"主题是园林休闲、但一个景点都没有"的误导性展示
                day.setTheme("自由活动");
            } else if (day.getTheme() == null || day.getTheme().isBlank()) {
                day.setTheme("第 " + dayNo + " 天");
            }
            byDayNo.put(dayNo, day);
        }

        // 模型漏掉的天补齐，避免"要 3 天只给 2 天"
        for (int i = MIN_DAYS; i <= req.getDays(); i++) {
            final int dayNo = i;
            byDayNo.computeIfAbsent(dayNo, key -> {
                AiPlanResult.Day empty = new AiPlanResult.Day();
                empty.setDayNo(dayNo);
                empty.setTheme("自由活动");
                empty.setSpots(new ArrayList<>());
                return empty;
            });
        }

        result.setDays(new ArrayList<>(byDayNo.values()));
        if (result.getTips() == null) {
            result.setTips(List.of());
        }
        if (countSpots(result) == 0) {
            throw new AiPlanException("AI 没能从现有景点中选出合适行程，请换个描述再试");
        }
        return dropped;
    }

    /** 事实数据一律以库为准，模型给的名称/价格/坐标全部覆盖 */
    private void enrich(AiPlanResult.Spot spot, ScenicEntity scenic) {
        spot.setName(scenic.getName());
        spot.setPrice(scenic.getPrice());
        spot.setScore(scenic.getScore());
        spot.setAddress(scenic.getAddress());
        spot.setOpenTime(scenic.getOpenTime());
        spot.setLongitude(scenic.getLongitude());
        spot.setLatitude(scenic.getLatitude());
        spot.setCover(scenic.getCover());
        if (spot.getStayTime() == null || spot.getStayTime().isBlank()) {
            spot.setStayTime("2小时");
        }
        if (spot.getReason() == null) {
            spot.setReason("");
        }
        // 模型偶尔会写长，截断防止卡片被撑爆
        spot.setTraffic(truncate(spot.getTraffic(), MAX_TRAFFIC_LENGTH));
        spot.setIntro(truncate(spot.getIntro(), MAX_INTRO_LENGTH));
        spot.setNotice(truncate(spot.getNotice(), MAX_NOTICE_LENGTH));
    }

    // ==================== 小工具 ====================

    /** 时段排序权重；未知时段的排在最后，保持原顺序 */
    private int slotRank(String timeSlot) {
        if (timeSlot == null) {
            return SLOT_ORDER.size();
        }
        int index = SLOT_ORDER.indexOf(timeSlot.trim());
        return index < 0 ? SLOT_ORDER.size() : index;
    }

    private int countSpots(AiPlanResult result) {        return result.getDays() == null ? 0 : result.getDays().stream()
                .mapToInt(day -> day.getSpots() == null ? 0 : day.getSpots().size())
                .sum();
    }

    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private boolean contains(String text, String keyword) {
        return text != null && keyword != null && text.contains(keyword);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String abbreviate(String text) {
        return text.length() <= 500 ? text : text.substring(0, 500) + "...";
    }
}
