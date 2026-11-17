package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.RouteDetailEntity;
import com.zjx.travel.mapper.RouteDetailMapper;
import com.zjx.travel.service.RouteDetailService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class RouteDetailServiceImpl implements RouteDetailService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private RouteDetailMapper routeDetailMapper;

    public RouteDetailEntity queryById(Integer id){
        return this.routeDetailMapper.queryById(id);
    }

    public Page<RouteDetailEntity> queryByPage(RouteDetailEntity routeDetail, Integer routeId, Integer page, Integer size,
                                               String orderCol, String orderDirect, boolean onlyApproved){

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

        long total = this.routeDetailMapper.count(routeDetail, routeId, onlyApproved);

        return new PageImpl<>(this.routeDetailMapper.queryAllByLimit(routeDetail, routeId, onlyApproved, pageRequest),
                pageRequest, total);
    }

    public boolean insert(RouteDetailEntity routeDetail){
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        Date now = new Date();
        if (routeDetail.getCreateTime() == null) {
            routeDetail.setCreateTime(now);
        }
        if (routeDetail.getUpdateTime() == null) {
            routeDetail.setUpdateTime(now);
        }
        return this.routeDetailMapper.insert(routeDetail) > 0;
    }

    public boolean update(RouteDetailEntity routeDetail){
        return this.routeDetailMapper.update(routeDetail) > 0;
    }

    public boolean deleteById(Integer id){
        return this.routeDetailMapper.deleteById(id) > 0;
    }

}