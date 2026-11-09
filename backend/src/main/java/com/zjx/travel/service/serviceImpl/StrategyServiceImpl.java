package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.StrategyEntity;
import com.zjx.travel.mapper.StrategyMapper;
import com.zjx.travel.service.StrategyService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class StrategyServiceImpl implements StrategyService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private StrategyMapper strategyMapper;

    public StrategyEntity queryById(Integer id){
        return this.strategyMapper.queryById(id);
    }

    public Page<StrategyEntity> queryByPage(StrategyEntity strategy, Integer page, Integer size, String orderCol, String orderDirect){

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

        long total = this.strategyMapper.count(strategy);

        return new PageImpl<>(this.strategyMapper.queryAllByLimit(strategy, pageRequest), pageRequest, total);
    }

    public boolean insert(StrategyEntity strategy){
        // 用户端发布攻略默认进入“待审核”状态（0待审核 1已通过 2已驳回）
        if (strategy.getAuditStatus() == null) {
            strategy.setAuditStatus(0);
        }
        if (strategy.getCollectCount() == null) {
            strategy.setCollectCount(0);
        }
        if (strategy.getReadCount() == null) {
            strategy.setReadCount(0);
        }
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        Date now = new Date();
        if (strategy.getCreateTime() == null) {
            strategy.setCreateTime(now);
        }
        if (strategy.getUpdateTime() == null) {
            strategy.setUpdateTime(now);
        }
        return this.strategyMapper.insert(strategy) > 0;
    }

    public boolean update(StrategyEntity strategy){
        return this.strategyMapper.update(strategy) > 0;
    }

    public boolean deleteById(Integer id){
        return this.strategyMapper.deleteById(id) > 0;
    }

}