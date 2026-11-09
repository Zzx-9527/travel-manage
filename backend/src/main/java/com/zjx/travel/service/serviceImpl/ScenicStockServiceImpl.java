package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.*;
import com.zjx.travel.mapper.*;
import com.zjx.travel.service.ScenicStockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.time.*;
import java.util.*;

@Service
public class ScenicStockServiceImpl implements ScenicStockService {
    public static final int DEFAULT_TOTAL = 100;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    @Resource private ScenicMapper scenicMapper;
    @Resource private ScenicStockMapper stockMapper;

    @Override
    public void generateNext30Days() {
        LocalDate today = LocalDate.now(ZONE);
        for (ScenicEntity scenic : scenicMapper.queryActiveForStock()) {
            int capacity = scenic.getDefaultCapacity() == null ? DEFAULT_TOTAL : scenic.getDefaultCapacity();
            int stock = scenic.getDefaultAdultStock() == null ? DEFAULT_TOTAL : scenic.getDefaultAdultStock();
            if (capacity < 0 || stock < 0) continue;
            for (int i=0; i<30; i++) {
                LocalDate date = today.plusDays(i);
                stockMapper.insertCapacityIfAbsent(scenic.getId(), date, capacity);
                stockMapper.insertStockIfAbsent(scenic.getId(), date, 1, stock);
            }
        }
    }

    @Override
    public ScenicStockView query(Integer scenicId, LocalDate date) {
        if (scenicId == null || date == null) return null;
        LocalDate today = LocalDate.now(ZONE);
        if (date.isBefore(today) || date.isAfter(today.plusDays(29))) return null;
        ScenicCapacityEntity c = stockMapper.queryCapacity(scenicId, date);
        ScenicStockEntity s = stockMapper.queryStock(scenicId, date, 1);
        if (c == null || s == null) return null;
        ScenicStockView view = new ScenicStockView(); view.capacity=c; view.stock=s;
        view.remaining = Math.min(c.getRemaining(), s.getRemaining());
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Integer scenicId, LocalDate date, Integer capacity, Integer adultStock) {
        LocalDate today = LocalDate.now(ZONE);
        if (scenicId == null || date == null || date.isBefore(today) || date.isAfter(today.plusDays(29)) || capacity == null || adultStock == null || capacity < 0 || adultStock < 0)
            throw new IllegalArgumentException("库存参数无效");
        // lock/update capacity first, then ticket stock; each SQL preserves occupied quantity.
        if (stockMapper.updateCapacityTotal(scenicId, date, capacity) != 1) throw new IllegalStateException("容量不存在或低于已占用数量");
        if (stockMapper.updateStockTotal(scenicId, date, 1, adultStock) != 1) throw new IllegalStateException("票库存不存在或低于已占用数量");
    }
}
