package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface ScenicMapper  extends BaseMapper<ScenicEntity> {

    ScenicEntity queryById(Integer id);

    public List<ScenicEntity> queryAllByLimit(@Param("scenic") ScenicEntity scenic, @Param("pageable") Pageable pageable);

    long count(@Param("scenic") ScenicEntity scenic);

    int insert(ScenicEntity scenic);

    int update(ScenicEntity scenic);

    int deleteById(Integer id);

    /**
     * 浏览量批量回写：view_count = view_count + delta
     *
     * <p>用数据库自身的加法而不是"先查后写"，避免并发回写时互相覆盖。
     *
     * @return 影响行数（0 表示景点不存在或已删除，调用方应保留 Redis 计数）
     */
    int increaseViewCount(@Param("id") Integer id, @Param("delta") long delta);

}