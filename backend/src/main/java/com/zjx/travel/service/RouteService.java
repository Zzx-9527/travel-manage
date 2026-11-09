package com.zjx.travel.service;

import com.zjx.travel.domain.RouteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface RouteService {
    RouteEntity queryById(Integer id);

    Page<RouteEntity> queryByPage(RouteEntity route, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(RouteEntity route);

    boolean update(RouteEntity route);

    boolean deleteById(Integer id);

}