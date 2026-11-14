package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.RouteEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface RouteMapper  extends BaseMapper<RouteEntity> {

    RouteEntity queryById(Integer id);

    public List<RouteEntity> queryAllByLimit(@Param("route") RouteEntity route, @Param("pageable") Pageable pageable);

    long count(@Param("route") RouteEntity route);

    int insert(RouteEntity route);

    int update(RouteEntity route);

    int deleteById(Integer id);

    /**
     * 查最近 5 分钟内、同用户、同名的待审核路线 id。
     *
     * <p>用于 AI 行程保存去重：用户连点两次"保存"不该产生两条一样的路线。
     * 返回 null 表示没有重复。
     */
    Integer findRecentSameName(@Param("userId") Integer userId, @Param("name") String name);

}