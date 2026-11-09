package com.zjx.travel.domain;

import java.io.Serializable;
import java.util.*;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class ScenicEntity{
    private Integer id;// primary key 
    private ScenicCategoryEntity categoryId;
    private String name;
    private String cover;
    private String images;
    private Double price;
    private String openTime;
    private String dayTicketCutoffTime;
    private Integer defaultCapacity;
    private Integer defaultAdultStock;
    private String address;
    private Double longitude;
    private Double latitude;
    private String intro;
    private Double score;
    private Integer viewCount;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}