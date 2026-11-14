package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.CollectEntity;
import com.zjx.travel.mapper.CollectMapper;
import com.zjx.travel.service.CollectService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class CollectServiceImpl implements CollectService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private CollectMapper collectMapper;

    public CollectEntity queryById(Integer id){
        return this.collectMapper.queryById(id);
    }

    public Page<CollectEntity> queryByPage(CollectEntity collect, Integer page, Integer size, String orderCol, String orderDirect){

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

        long total = this.collectMapper.count(collect);

        return new PageImpl<>(this.collectMapper.queryAllByLimit(collect, pageRequest), pageRequest, total);
    }

    public boolean insert(CollectEntity collect){
        // 兼容三种收藏传参：targetType+targetId / scenicId / routeId
        if (collect.getTargetType() == null) {
            if (collect.getScenicId() != null && collect.getScenicId().getId() != null) {
                collect.setTargetType(2);
                collect.setTargetId(collect.getScenicId().getId());
            } else if (collect.getRouteId() != null && collect.getRouteId().getId() != null) {
                collect.setTargetType(3);
                collect.setTargetId(collect.getRouteId().getId());
            }
        }
        if (collect.getTargetType() == null) {
            collect.setTargetType(1);
        }
        // 自定义 XML 插入不会触发自动填充，这里手动赋值
        if (collect.getCreateTime() == null) {
            collect.setCreateTime(new Date());
        }
        return this.collectMapper.insert(collect) > 0;
    }

    public boolean update(CollectEntity collect){
        return this.collectMapper.update(collect) > 0;
    }

    public boolean deleteById(Integer id){
        return this.collectMapper.deleteById(id) > 0;
    }

    public boolean checkCollected(Integer userId, Integer targetType, Integer targetId){
        if (userId == null || targetType == null || targetId == null) {
            return false;
        }
        return this.collectMapper.checkCollected(userId, targetType, targetId) > 0;
    }

    public boolean deleteByUserAndTarget(Integer userId, Integer targetType, Integer targetId){
        if (userId == null || targetType == null || targetId == null) {
            return false;
        }
        return this.collectMapper.deleteByUserAndTarget(userId, targetType, targetId) > 0;
    }

}
