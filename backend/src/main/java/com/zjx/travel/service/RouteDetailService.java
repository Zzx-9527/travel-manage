package com.zjx.travel.service;

import com.zjx.travel.domain.RouteDetailEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface RouteDetailService {
    RouteDetailEntity queryById(Integer id);

    Page<RouteDetailEntity> queryByPage(RouteDetailEntity routeDetail, Integer routeId, Integer page, Integer size,
                                       String orderCol, String orderDirect, boolean onlyApproved);

    boolean insert(RouteDetailEntity routeDetail);

    boolean update(RouteDetailEntity routeDetail);

    boolean deleteById(Integer id);

}