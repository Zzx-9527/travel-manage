package com.zjx.travel.service;

import com.zjx.travel.domain.StrategyCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface StrategyCommentService {
    StrategyCommentEntity queryById(Integer id);

    Page<StrategyCommentEntity> queryByPage(StrategyCommentEntity strategyComment, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(StrategyCommentEntity strategyComment);

    boolean update(StrategyCommentEntity strategyComment);

    boolean deleteById(Integer id);

}