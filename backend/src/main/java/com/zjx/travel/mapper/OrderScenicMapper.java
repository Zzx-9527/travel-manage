package com.zjx.travel.mapper;

import com.zjx.travel.domain.ScenicEntity;
import org.apache.ibatis.annotations.Select;

public interface OrderScenicMapper {
    @Select("SELECT id, name, price, status, open_time AS openTime, "
            + "day_ticket_cutoff_time AS dayTicketCutoffTime "
            + "FROM scenic WHERE id=#{id} AND del_mark=1")
    ScenicEntity findForOrder(Integer id);
}
