package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.CollectEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface CollectMapper  extends BaseMapper<CollectEntity> {

    CollectEntity queryById(Integer id);

    public List<CollectEntity> queryAllByLimit(@Param("collect") CollectEntity collect, @Param("pageable") Pageable pageable);

    long count(@Param("collect") CollectEntity collect);

    int insert(CollectEntity collect);

    int update(CollectEntity collect);

    int deleteById(Integer id);

    /**
     * 判断是否已收藏
     */
    int checkCollected(@Param("userId") Integer userId, @Param("targetType") Integer targetType, @Param("targetId") Integer targetId);

    /**
     * 按 用户 + 目标类型 + 目标ID 取消收藏
     */
    int deleteByUserAndTarget(@Param("userId") Integer userId, @Param("targetType") Integer targetType, @Param("targetId") Integer targetId);

}
