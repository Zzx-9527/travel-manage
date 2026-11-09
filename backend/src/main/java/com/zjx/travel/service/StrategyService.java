package com.zjx.travel.service;

import com.zjx.travel.domain.StrategyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface StrategyService {
    StrategyEntity queryById(Integer id);

    Page<StrategyEntity> queryByPage(StrategyEntity strategy, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(StrategyEntity strategy);

    boolean update(StrategyEntity strategy);

    boolean deleteById(Integer id);

}