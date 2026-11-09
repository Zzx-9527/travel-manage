package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.ScenicCategoryEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface ScenicCategoryMapper  extends BaseMapper<ScenicCategoryEntity> {

    ScenicCategoryEntity queryById(Integer id);

    public List<ScenicCategoryEntity> queryAllByLimit(@Param("scenicCategory") ScenicCategoryEntity scenicCategory, @Param("pageable") Pageable pageable);

    long count(@Param("scenicCategory") ScenicCategoryEntity scenicCategory);

    int insert(ScenicCategoryEntity scenicCategory);

    int update(ScenicCategoryEntity scenicCategory);

    int deleteById(Integer id);

}