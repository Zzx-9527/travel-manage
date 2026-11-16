package com.zjx.travel.domain;

import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.*;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class RouteEntity{
    private Integer id;// primary key 
    private TravelUserEntity userId;
    @NotBlank(message = "路线名称不能为空")
    @Size(max = 100, message = "路线名称不能超过 100 字")
    private String name;
    @NotNull(message = "游玩天数不能为空")
    @Min(value = 1, message = "游玩天数至少 1 天")
    @Max(value = 15, message = "游玩天数最多 15 天")
    private Integer dayCount;
    private String startCity;
    private String traffic;
    @DecimalMin(value = "0", message = "预算不能为负数")
    private Double budget;
    private String intro;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}