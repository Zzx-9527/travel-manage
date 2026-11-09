package com.zjx.travel.mapper;

import com.zjx.travel.domain.ScenicCapacityEntity;
import com.zjx.travel.domain.ScenicStockEntity;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;

public interface ScenicStockMapper {
    int insertCapacityIfAbsent(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("total") int total);
    int insertStockIfAbsent(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("ticketTypeId") int ticketTypeId, @Param("total") int total);
    int decrementCapacity(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("count") int count);
    int decrementStock(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("ticketTypeId") int ticketTypeId, @Param("count") int count);
    int incrementCapacity(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("count") int count);
    int incrementStock(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("ticketTypeId") int ticketTypeId, @Param("count") int count);
    ScenicCapacityEntity queryCapacity(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate);
    ScenicStockEntity queryStock(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("ticketTypeId") int ticketTypeId);
    int updateCapacityTotal(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("total") int total);
    int updateStockTotal(@Param("scenicId") Integer scenicId, @Param("playDate") LocalDate playDate, @Param("ticketTypeId") int ticketTypeId, @Param("total") int total);
}
