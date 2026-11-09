package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface ScenicOrdersMapper  extends BaseMapper<ScenicOrdersEntity> {

    ScenicOrdersEntity queryById(Integer id);

    public List<ScenicOrdersEntity> queryAllByLimit(@Param("scenicOrders") ScenicOrdersEntity scenicOrders, @Param("pageable") Pageable pageable);

    long count(@Param("scenicOrders") ScenicOrdersEntity scenicOrders);

    int insert(ScenicOrdersEntity scenicOrders);

    int update(ScenicOrdersEntity scenicOrders);

    int deleteById(Integer id);

}