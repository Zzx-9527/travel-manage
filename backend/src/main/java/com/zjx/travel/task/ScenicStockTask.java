package com.zjx.travel.task;

import com.zjx.travel.service.ScenicStockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;

@Component
public class ScenicStockTask {
    @Resource private ScenicStockService scenicStockService;
    /** Daily rolling generation; INSERT IGNORE never resets existing inventory. */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    public void generate() { scenicStockService.generateNext30Days(); }
}
