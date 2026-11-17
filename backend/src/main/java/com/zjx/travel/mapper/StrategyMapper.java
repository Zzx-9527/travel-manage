package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface StrategyMapper extends BaseMapper<StrategyEntity> {

    StrategyEntity queryById(Integer id);

    public List<StrategyEntity> queryAllByLimit(@Param("strategy") StrategyEntity strategy, @Param("pageable") Pageable pageable);

    long count(@Param("strategy") StrategyEntity strategy);

    int update(StrategyEntity strategy);

    int deleteById(Integer id);

    /** 原子调整攻略收藏数，结果不会小于 0。 */
    int updateCollectCount(@Param("id") Integer id, @Param("delta") int delta);

}