package com.zjx.travel.domain;

import java.io.Serializable;
import java.util.*;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class ScenicOrdersEntity{
    private Integer id;// primary key 
    private String orderNo;
    private TravelUserEntity userId;
    private ScenicEntity scenicId;
    private String contactName;
    private String contactPhone;
    private String playDate;
    private Integer ticketCount;
    private Double totalAmount;
    private Integer payType;
    private Integer payStatus;
    private Integer status;
    private Date cancelTime;
    private String cancelReason;
    private Double unitPrice;
    private Integer ticketTypeId = 1;
    private Integer stockReserved;
    // 管理端联合搜索：下单人昵称 / 景点名称（非数据库字段）
    @TableField(exist = false)
    private String keyword;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date payTime;
}