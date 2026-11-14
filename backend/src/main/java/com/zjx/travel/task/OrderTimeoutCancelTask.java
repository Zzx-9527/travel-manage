package com.zjx.travel.task;

import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.service.ScenicOrdersService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.Resource;
import java.util.List;

@Component
public class OrderTimeoutCancelTask {
    private static final Logger log=LoggerFactory.getLogger(OrderTimeoutCancelTask.class);
    @Resource private com.zjx.travel.mapper.ScenicOrdersMapper mapper;
    @Resource private ScenicOrdersService service;
    @Scheduled(fixedDelayString="${travel.order.timeout-interval-ms:60000}")
    public void cancelExpired(){
        int afterId = 0;
        try {
            for (;;) {
                List<ScenicOrdersEntity> batch = mapper.findTimeoutOrders(afterId, 100);
                if (batch.isEmpty()) break;
                for (ScenicOrdersEntity order : batch) {
                    afterId = order.getId();
                    try { service.timeoutCancel(order); }
                    catch (Exception e) { log.warn("自动取消失败 id={}，下轮重试", order.getId(), e); }
                }
                if (batch.size() < 100) break;
            }
        } catch (Exception e) { log.error("订单超时任务失败", e); }
    }
}
