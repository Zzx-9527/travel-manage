package com.zjx.travel.service;

import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.dto.ScenicOrderCreateDTO;
import com.zjx.travel.dto.ScenicOrderEditDTO;
import org.springframework.data.domain.Page;

public interface ScenicOrdersService {
    ScenicOrdersEntity queryById(Integer id);
    Page<ScenicOrdersEntity> queryByPage(ScenicOrdersEntity query, Integer page, Integer size, String orderCol, String orderDirect);
    boolean deleteById(Integer id);
    ScenicOrdersEntity createOrder(ScenicOrderCreateDTO request, Integer userId);
    void cancel(Integer id, Integer userId, boolean admin, String reason);
    void pay(Integer id, Integer userId);
    boolean editContact(ScenicOrderEditDTO request, Integer userId);
    void timeoutCancel(ScenicOrdersEntity order);
}
