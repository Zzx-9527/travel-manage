package com.zjx.travel.domain;

import java.io.Serializable;
import java.util.*;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class StrategyCommentEntity{
    private Integer id;// primary key 
    private StrategyEntity strategyId;
    private TravelUserEntity userId;
    private String content;
    private Integer status;
    // 用于接收搜索参数（非数据库字段）
    @TableField(exist = false)
    private String keyword;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}