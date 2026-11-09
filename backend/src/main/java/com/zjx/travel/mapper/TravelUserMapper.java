package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.TravelUserEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface TravelUserMapper extends BaseMapper<TravelUserEntity> {

    TravelUserEntity queryById(Integer id);

    public List<TravelUserEntity> queryAllByLimit(@Param("travelUser") TravelUserEntity travelUser, @Param("pageable") Pageable pageable);

    long count(@Param("travelUser") TravelUserEntity travelUser);

    int update(TravelUserEntity travelUser);

    int deleteById(Integer id);

    TravelUserEntity queryByAccount(String account);
}