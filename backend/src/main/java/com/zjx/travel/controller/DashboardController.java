package com.zjx.travel.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.zjx.travel.service.DashboardService;
import com.zjx.travel.util.ReturnVO;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 后台首页统计卡片：返回 用户数 / 景点数 / 攻略数 / 订单数
     * GET /travel/dashboard/stats
     *
     * <p>后台专属接口，仅管理员可访问；普通用户返回 403。
     */
    @GetMapping("stats")
    @SaCheckRole("admin")
    public ReturnVO stats() {
        Map<String, Object> stats = this.dashboardService.stats();
        return ReturnVO.getSuccessDataReturnVO(stats);
    }

    /**
     * 后台首页饼图数据：景点分类分布 + 订单状态分布
     * GET /travel/dashboard/charts
     */
    @GetMapping("charts")
    @SaCheckRole("admin")
    public ReturnVO charts() {
        Map<String, Object> charts = this.dashboardService.charts();
        return ReturnVO.getSuccessDataReturnVO(charts);
    }
}
