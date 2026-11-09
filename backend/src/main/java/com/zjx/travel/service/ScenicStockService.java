package com.zjx.travel.service;

import com.zjx.travel.domain.ScenicCapacityEntity;
import com.zjx.travel.domain.ScenicStockEntity;
import java.time.LocalDate;

public interface ScenicStockService {
    void generateNext30Days();
    ScenicStockView query(Integer scenicId, LocalDate date);
    void adjust(Integer scenicId, LocalDate date, Integer capacity, Integer adultStock);
    class ScenicStockView { public ScenicCapacityEntity capacity; public ScenicStockEntity stock; public Integer remaining; }
}
