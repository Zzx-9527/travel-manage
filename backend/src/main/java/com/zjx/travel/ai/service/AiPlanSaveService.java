package com.zjx.travel.ai.service;

import com.zjx.travel.ai.dto.AiPlanResult;
import com.zjx.travel.ai.dto.AiPlanSaveRequest;
import com.zjx.travel.ai.exception.AiPlanException;
import com.zjx.travel.domain.RouteDetailEntity;
import com.zjx.travel.domain.RouteEntity;
import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.mapper.RouteMapper;
import com.zjx.travel.mapper.ScenicMapper;
import com.zjx.travel.service.RouteDetailService;
import com.zjx.travel.service.RouteService;
import com.zjx.travel.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 把 AI 生成的行程落库成"我的路线"。
 *
 * <p>复用现有的 route + route_detail 两张表，状态置 0（待审核），
 * 和用户手动发布的路线走同一套审核流程，不新增表、不绕开审核。
 *
 * <p>两个安全点：
 * <ul>
 *   <li>归属人一律取当前登录用户，不接受前端传的 userId；</li>
 *   <li>景点 id 保存前再查一次 scenic 表，防止前端伪造出不存在的景点。</li>
 * </ul>
 */
@Service
public class AiPlanSaveService {

    private static final Logger log = LoggerFactory.getLogger(AiPlanSaveService.class);

    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_INTRO_LENGTH = 500;
    private static final int MAX_DAYS = 7;

    private final RouteService routeService;
    private final RouteDetailService routeDetailService;
    private final ScenicMapper scenicMapper;
    private final RouteMapper routeMapper;

    public AiPlanSaveService(RouteService routeService,
                             RouteDetailService routeDetailService,
                             ScenicMapper scenicMapper,
                             RouteMapper routeMapper) {
        this.routeService = routeService;
        this.routeDetailService = routeDetailService;
        this.scenicMapper = scenicMapper;
        this.routeMapper = routeMapper;
    }

    /**
     * @return 新建路线的 id
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer save(AiPlanSaveRequest request, Integer userId) {
        if (userId == null) {
            throw new AiPlanException("请先登录后再保存");
        }
        if (request == null || request.getDays() == null || request.getDays().isEmpty()) {
            throw new AiPlanException("行程为空，无法保存");
        }
        int dayCount = request.getDays() == null ? 0 : request.getDays().size();
        if (dayCount > MAX_DAYS) {
            throw new AiPlanException("行程天数超出上限");
        }

        Set<Integer> validScenicIds = scenicMapper.queryActiveList().stream()
                .map(ScenicEntity::getId)
                .collect(Collectors.toSet());

        String routeName = truncate(request.getTitle(), MAX_TITLE_LENGTH, "AI 生成的行程");
        String intro = buildIntro(request);
        // 预算允许前端不填；route.budget 为非空列，保存时统一按 0 处理
        double budget = request.getBudget() == null ? 0D : Math.max(0D, request.getBudget());
        // 幂等依据是“实际会落库的内容”，不是标题：同名但景点/天数不同的行程必须都能保存
        String fingerprint = fingerprint(request, routeName, intro, validScenicIds, budget);
        Integer existingId = routeMapper.findByUserAndFingerprint(userId, fingerprint);
        if (existingId != null) {
            log.info("同一份 AI 行程已保存过，返回已有路线：routeId={}，userId={}", existingId, userId);
            return existingId;
        }

        RouteEntity route = new RouteEntity();
        route.setUserId(SecurityUtil.userRef(userId));
        route.setName(routeName);
        route.setDayCount(dayCount);
        route.setStartCity(truncate(request.getDestination(), 30, null));
        route.setTraffic("其他");
        route.setBudget(budget);
        route.setIntro(intro);
        // 0 = 待审核，和用户手动发布的路线一致
        route.setStatus(0);
        route.setPlanFingerprint(fingerprint);

        try {
            if (!routeService.insert(route) || route.getId() == null) {
                throw new AiPlanException("保存失败，请稍后重试");
            }
        } catch (DuplicateKeyException e) {
            // 双击并发时数据库唯一键兜底，返回先写入的那条
            Integer concurrentId = routeMapper.findByUserAndFingerprint(userId, fingerprint);
            if (concurrentId != null) {
                return concurrentId;
            }
            throw new AiPlanException("保存失败，请稍后重试");
        }

        int savedSpots = 0;
        for (AiPlanResult.Day day : request.getDays()) {
            if (day == null || day.getSpots() == null) {
                continue;
            }
            int sort = 1;
            for (AiPlanResult.Spot spot : day.getSpots()) {
                if (spot == null || spot.getScenicId() == null
                        || !validScenicIds.contains(spot.getScenicId())) {
                    continue;
                }
                RouteDetailEntity detail = new RouteDetailEntity();
                detail.setRouteId(routeRef(route.getId()));
                detail.setScenicId(scenicRef(spot.getScenicId()));
                detail.setDayNo(day.getDayNo() == null ? 1 : day.getDayNo());
                detail.setSort(sort++);
                detail.setStayTime(spot.getStayTime());
                routeDetailService.insert(detail);
                savedSpots++;
            }
        }

        if (savedSpots == 0) {
            // 全部景点都无效：回滚整条路线，避免留下一条空行程
            throw new AiPlanException("行程中没有可用的景点，保存失败");
        }

        log.info("AI 行程已保存为路线：routeId={}，userId={}，天数={}，景点数={}",
                route.getId(), userId, dayCount, savedSpots);
        return route.getId();
    }

    /**
     * 对实际会落库的路线字段做稳定哈希。
     *
     * <p>仅包含有效景点，避免客户端塞入若干无效 id 后，同一份行程产生不同指纹。
     */
    private String fingerprint(AiPlanSaveRequest request, String title, String intro,
                               Set<Integer> validScenicIds, double budget) {
        StringBuilder canonical = new StringBuilder();
        appendPart(canonical, title);
        appendPart(canonical, truncate(request.getDestination(), 30, null));
        appendPart(canonical, budget);
        appendPart(canonical, intro);
        if (request.getDays() != null) {
            for (AiPlanResult.Day day : request.getDays()) {
                if (day == null) {
                    continue;
                }
                appendPart(canonical, day.getDayNo() == null ? 1 : day.getDayNo());
                if (day.getSpots() == null) {
                    continue;
                }
                int sort = 1;
                for (AiPlanResult.Spot spot : day.getSpots()) {
                    if (spot == null || spot.getScenicId() == null
                            || !validScenicIds.contains(spot.getScenicId())) {
                        continue;
                    }
                    appendPart(canonical, sort++);
                    appendPart(canonical, spot.getScenicId());
                    appendPart(canonical, spot.getStayTime());
                }
            }
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // JDK 17 必然支持 SHA-256，走到这里说明运行环境异常
            throw new IllegalStateException("无法计算行程指纹", e);
        }
    }

    private void appendPart(StringBuilder target, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        target.append(text.length()).append(':').append(text).append('|');
    }

    private String buildIntro(AiPlanSaveRequest request) {
        StringBuilder intro = new StringBuilder();
        if (request.getIntro() != null) {
            intro.append(request.getIntro().trim());
        }
        List<String> tips = request.getTips();
        if (tips != null && !tips.isEmpty()) {
            if (intro.length() > 0) {
                intro.append('\n');
            }
            intro.append("【提示】");
            intro.append(tips.stream().filter(t -> t != null && !t.isBlank())
                    .limit(5)
                    .collect(Collectors.joining("；")));
        }
        return truncate(intro.toString(), MAX_INTRO_LENGTH, null);
    }

    private RouteEntity routeRef(Integer id) {
        RouteEntity ref = new RouteEntity();
        ref.setId(id);
        return ref;
    }

    private ScenicEntity scenicRef(Integer id) {
        ScenicEntity ref = new ScenicEntity();
        ref.setId(id);
        return ref;
    }

    private String truncate(String value, int maxLength, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
