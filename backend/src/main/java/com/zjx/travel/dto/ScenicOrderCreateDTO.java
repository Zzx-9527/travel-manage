package com.zjx.travel.dto;

import lombok.Data;

@Data
public class ScenicOrderCreateDTO {
    private ScenicRef scenicId;

    @Data
    public static class ScenicRef {
        private Integer id;
    }
    private String contactName;
    private String contactPhone;
    private String playDate;
    private Integer ticketCount;
    private Integer payType;
}
