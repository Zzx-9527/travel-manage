package com.zjx.travel.service;

import com.zjx.travel.domain.ScenicOrdersEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface ScenicOrdersService {
    ScenicOrdersEntity queryById(Integer id);

    Page<ScenicOrdersEntity> queryByPage(ScenicOrdersEntity scenicOrders, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(ScenicOrdersEntity scenicOrders);

    boolean update(ScenicOrdersEntity scenicOrders);

    boolean deleteById(Integer id);

}