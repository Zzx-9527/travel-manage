package com.zjx.travel.domain;

import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

@Data
public class ScenicCapacityEntity {
    private Long id;
    private Integer scenicId;
    private LocalDate playDate;
    private Integer total;
    private Integer remaining;
    private Date createTime;
    private Date updateTime;
}
