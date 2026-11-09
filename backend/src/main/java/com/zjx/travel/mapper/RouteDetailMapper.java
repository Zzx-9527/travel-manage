package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.RouteDetailEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface RouteDetailMapper  extends BaseMapper<RouteDetailEntity> {

    RouteDetailEntity queryById(Integer id);

    public List<RouteDetailEntity> queryAllByLimit(@Param("routeDetail") RouteDetailEntity routeDetail, @Param("routeId") Integer routeId, @Param("pageable") Pageable pageable);

    long count(@Param("routeDetail") RouteDetailEntity routeDetail, @Param("routeId") Integer routeId);

    int insert(RouteDetailEntity routeDetail);

    int update(RouteDetailEntity routeDetail);

    int deleteById(Integer id);

}