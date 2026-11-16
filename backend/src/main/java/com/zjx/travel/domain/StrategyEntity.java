package com.zjx.travel.domain;

import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class StrategyEntity{
    private Integer id;
    private TravelUserEntity userId;
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;
    private String cover;
    @NotBlank(message = "正文不能为空")
    private String content;
    private Integer collectCount;
    private Integer readCount;
    private Integer auditStatus;
    
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}