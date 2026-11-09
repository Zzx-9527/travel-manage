package com.zjx.travel.mapper;

import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.domain.StrategyEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface ScenicOrdersMapper {

    ScenicOrdersEntity queryById(Integer id);

    public List<ScenicOrdersEntity> queryAllByLimit(@Param("scenicOrders") ScenicOrdersEntity scenicOrders, @Param("pageable") Pageable pageable);

    long count(@Param("scenicOrders") ScenicOrdersEntity scenicOrders);

    int insert(ScenicOrdersEntity scenicOrders);


    int deleteById(Integer id);

    int updateContact(@Param("id") Integer id, @Param("userId") Integer userId, @Param("contactName") String contactName, @Param("contactPhone") String contactPhone);
    int transitionToCancelled(@Param("id") Integer id, @Param("reason") String reason);
    int transitionToTimeout(@Param("id") Integer id);
    int transitionToPaid(@Param("id") Integer id, @Param("userId") Integer userId);
    java.util.List<ScenicOrdersEntity> findTimeoutOrders(@Param("afterId") int afterId, @Param("limit") int limit);

}