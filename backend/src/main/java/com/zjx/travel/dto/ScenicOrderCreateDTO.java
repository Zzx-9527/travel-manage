package com.zjx.travel.dto;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.Valid;

import lombok.Data;

@Data
public class ScenicOrderCreateDTO {
    @NotNull(message = "请选择景点")
    @Valid
    private ScenicRef scenicId;

    @Data
    public static class ScenicRef {
        @NotNull(message = "景点不能为空")
        private Integer id;
    }
    @NotBlank(message = "联系人不能为空")
    @Size(max = 20, message = "联系人姓名过长")
    private String contactName;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;
    @NotBlank(message = "游玩日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "游玩日期格式应为 yyyy-MM-dd")
    private String playDate;
    @NotNull(message = "票数不能为空")
    @Min(value = 1, message = "至少购买 1 张")
    @Max(value = 10, message = "单笔最多购买 10 张")
    private Integer ticketCount;
    private Integer payType;
}
