package com.zjx.travel.mapper;

import java.util.Map;

/**
 * 后台首页统计 Mapper
 */
public interface DashboardMapper {

    /**
     * 统计后台首页四个数字：用户数 / 景点数 / 攻略数 / 订单数
     * @return Map 包含 userCount、scenicCount、strategyCount、orderCount
     */
    Map<String, Object> stats();
}
