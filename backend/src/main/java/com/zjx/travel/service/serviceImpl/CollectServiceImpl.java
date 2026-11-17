package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.CollectEntity;
import com.zjx.travel.mapper.CollectMapper;
import com.zjx.travel.mapper.StrategyMapper;
import com.zjx.travel.service.CollectService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CollectServiceImpl implements CollectService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private CollectMapper collectMapper;
    @Resource
    private StrategyMapper strategyMapper;

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

    @Transactional(rollbackFor = Exception.class)
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
        if (collect.getUserId() == null || collect.getUserId().getId() == null) {
            return false;
        }
        // 已存在直接按成功处理：收藏接口天然幂等
        if (checkCollected(collect.getUserId().getId(), collect.getTargetType(), collect.getTargetId())) {
            return true;
        }
        // 自定义 XML 插入不会触发自动填充，这里手动赋值
        if (collect.getCreateTime() == null) {
            collect.setCreateTime(new Date());
        }
        int inserted = this.collectMapper.insertIgnore(collect);
        if (inserted > 0) {
            adjustStrategyCount(collect.getTargetType(), collect.getTargetId(), 1);
            return true;
        }
        // 并发请求被唯一键拦下时，目标收藏已经存在，也视为成功
        return checkCollected(collect.getUserId().getId(), collect.getTargetType(), collect.getTargetId());
    }

    public boolean update(CollectEntity collect){
        CollectEntity old = collect.getId() == null ? null : this.collectMapper.queryById(collect.getId());
        if (old == null) {
            return false;
        }
        // 收藏目标不可通过编辑接口迁移，避免收藏数与目标记录失配
        collect.setTargetType(old.getTargetType());
        collect.setTargetId(old.getTargetId());
        return this.collectMapper.update(collect) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id){
        CollectEntity old = this.collectMapper.queryById(id);
        if (old == null) {
            return false;
        }
        boolean deleted = this.collectMapper.deleteById(id) > 0;
        if (deleted) {
            adjustStrategyCount(old.getTargetType(), old.getTargetId(), -1);
        }
        return deleted;
    }

    public boolean checkCollected(Integer userId, Integer targetType, Integer targetId){
        if (userId == null || targetType == null || targetId == null) {
            return false;
        }
        return this.collectMapper.checkCollected(userId, targetType, targetId) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByUserAndTarget(Integer userId, Integer targetType, Integer targetId){
        if (userId == null || targetType == null || targetId == null) {
            return false;
        }
        boolean deleted = this.collectMapper.deleteByUserAndTarget(userId, targetType, targetId) > 0;
        if (deleted) {
            adjustStrategyCount(targetType, targetId, -1);
        }
        return deleted;
    }

    private void adjustStrategyCount(Integer targetType, Integer targetId, int delta) {
        if (Integer.valueOf(1).equals(targetType) && targetId != null) {
            this.strategyMapper.updateCollectCount(targetId, delta);
        }
    }

}
