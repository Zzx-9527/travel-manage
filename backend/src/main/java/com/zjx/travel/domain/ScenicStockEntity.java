package com.zjx.travel.domain;

import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

@Data
public class ScenicStockEntity {
    private Long id;
    private Integer scenicId;
    private LocalDate playDate;
    private Integer ticketTypeId = 1;
    private Integer total;
    private Integer remaining;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
