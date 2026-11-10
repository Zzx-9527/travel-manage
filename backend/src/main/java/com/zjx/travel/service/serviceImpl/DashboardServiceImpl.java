package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.mapper.DashboardMapper;
import com.zjx.travel.service.DashboardService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private DashboardMapper dashboardMapper;

    public Map<String, Object> stats() {
        return this.dashboardMapper.stats();
    }

    public Map<String, Object> charts() {
        Map<String, Object> charts = new HashMap<>();
        charts.put("categoryStats", this.dashboardMapper.categoryStats());
        charts.put("orderStatusStats", this.dashboardMapper.orderStatusStats());
        return charts;
    }
}
