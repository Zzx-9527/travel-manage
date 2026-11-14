package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.StrategyCommentEntity;
import com.zjx.travel.mapper.StrategyCommentMapper;
import com.zjx.travel.service.StrategyCommentService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class StrategyCommentServiceImpl implements StrategyCommentService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private StrategyCommentMapper strategyCommentMapper;

    public StrategyCommentEntity queryById(Integer id){
        return this.strategyCommentMapper.queryById(id);
    }

    public Page<StrategyCommentEntity> queryByPage(StrategyCommentEntity strategyComment, Integer page, Integer size, String orderCol, String orderDirect){

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

        long total = this.strategyCommentMapper.count(strategyComment);

        return new PageImpl<>(this.strategyCommentMapper.queryAllByLimit(strategyComment, pageRequest), pageRequest, total);
    }

    public boolean insert(StrategyCommentEntity strategyComment){
        // 评论默认待审核状态（0待审核 1已通过 2已驳回）
        if (strategyComment.getStatus() == null) {
            strategyComment.setStatus(0);
        }
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        if (strategyComment.getCreateTime() == null) {
            strategyComment.setCreateTime(new Date());
        }
        return this.strategyCommentMapper.insert(strategyComment) > 0;
    }

    public boolean update(StrategyCommentEntity strategyComment){
        return this.strategyCommentMapper.update(strategyComment) > 0;
    }

    public boolean deleteById(Integer id){
        return this.strategyCommentMapper.deleteById(id) > 0;
    }

}