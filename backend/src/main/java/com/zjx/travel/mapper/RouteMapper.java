package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.RouteEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface RouteMapper  extends BaseMapper<RouteEntity> {

    RouteEntity queryById(Integer id);

    public List<RouteEntity> queryAllByLimit(@Param("route") RouteEntity route, @Param("pageable") Pageable pageable);

    long count(@Param("route") RouteEntity route);

    int insert(RouteEntity route);

    int update(RouteEntity route);

    int deleteById(Integer id);

}