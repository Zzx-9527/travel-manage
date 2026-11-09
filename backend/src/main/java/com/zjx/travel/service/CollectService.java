package com.zjx.travel.service;

import com.zjx.travel.domain.CollectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface CollectService {
    CollectEntity queryById(Integer id);

    Page<CollectEntity> queryByPage(CollectEntity collect, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(CollectEntity collect);

    boolean update(CollectEntity collect);

    boolean deleteById(Integer id);

    // 判断是否已收藏
    boolean checkCollected(Integer userId, Integer targetType, Integer targetId);

    // 按 用户 + 目标类型 + 目标ID 取消收藏
    boolean deleteByUserAndTarget(Integer userId, Integer targetType, Integer targetId);

}