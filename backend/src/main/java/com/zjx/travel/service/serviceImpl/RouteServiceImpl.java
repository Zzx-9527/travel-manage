package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.RouteEntity;
import com.zjx.travel.mapper.RouteMapper;
import com.zjx.travel.service.RouteService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class RouteServiceImpl implements RouteService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private RouteMapper routeMapper;

    public RouteEntity queryById(Integer id){
        return this.routeMapper.queryById(id);
    }

    public Page<RouteEntity> queryByPage(RouteEntity route, Integer page, Integer size, String orderCol, String orderDirect){

        if(page == null || page <= 0){
            page = PAGE_DEFAULT;
        }
        if(size == null || size <= 0){
            size = SIZE_DEFAULT;
        }

        Sort sort = null;
        if(orderCol != null) {
            Sort.Order order = new Sort.Order(("DESC".equals(orderDirect) ? Sort.Direction.DESC : Sort.Direction.ASC), orderCol);
            sort = Sort.by(order);
        }

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if(sort != null){
            pageRequest = PageRequest.of(page - 1, size, sort);
        }

        long total = this.routeMapper.count(route);

        return new PageImpl<>(this.routeMapper.queryAllByLimit(route, pageRequest), pageRequest, total);
    }

    public boolean insert(RouteEntity route){
        // 用户端发布路线默认进入“待审核”状态（0待审核 1已通过 2已驳回）
        if (route.getStatus() == null) {
            route.setStatus(0);
        }
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        Date now = new Date();
        if (route.getCreateTime() == null) {
            route.setCreateTime(now);
        }
        if (route.getUpdateTime() == null) {
            route.setUpdateTime(now);
        }
        return this.routeMapper.insert(route) > 0;
    }

    public boolean update(RouteEntity route){
        return this.routeMapper.update(route) > 0;
    }

    public boolean deleteById(Integer id){
        return this.routeMapper.deleteById(id) > 0;
    }

}