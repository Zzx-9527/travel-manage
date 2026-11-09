package com.zjx.travel.service;

import java.util.Map;

public interface DashboardService {

    /**
     * 统计后台首页四个数字：用户数 / 景点数 / 攻略数 / 订单数
     */
    Map<String, Object> stats();
}
