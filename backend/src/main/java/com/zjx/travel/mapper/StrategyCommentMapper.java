package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.StrategyCommentEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface StrategyCommentMapper  extends BaseMapper<StrategyCommentEntity> {

    StrategyCommentEntity queryById(Integer id);

    public List<StrategyCommentEntity> queryAllByLimit(@Param("strategyComment") StrategyCommentEntity strategyComment, @Param("pageable") Pageable pageable);

    long count(@Param("strategyComment") StrategyCommentEntity strategyComment);

    int insert(StrategyCommentEntity strategyComment);

    int update(StrategyCommentEntity strategyComment);

    int deleteById(Integer id);

}