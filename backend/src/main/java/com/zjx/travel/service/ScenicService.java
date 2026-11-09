package com.zjx.travel.service;

import com.zjx.travel.domain.ScenicEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface ScenicService {
    ScenicEntity queryById(Integer id);

    Page<ScenicEntity> queryByPage(ScenicEntity scenic, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(ScenicEntity scenic);

    boolean update(ScenicEntity scenic);

    boolean deleteById(Integer id);

    /**
     * 记录一次浏览：Redis 计数 +1，不直接写库（由定时任务批量回写）。
     * Redis 异常时静默失败，不能影响详情接口。
     */
    void recordView(Integer scenicId);

    /**
     * 把 Redis 中累积的浏览量增量加到数据库。
     *
     * @return false 表示景点不存在/已删除或更新失败，调用方应保留 Redis 计数等待重试
     */
    boolean increaseViewCount(Integer scenicId, long delta);

}