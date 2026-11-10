package com.zjx.travel.domain;

import java.io.Serializable;
import java.util.*;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class CollectEntity{
    private Integer id;// primary key
    private TravelUserEntity userId;
    // 收藏目标类型：1攻略 2景点 3路线（数据库字段）
    private Integer targetType;
    // 收藏目标ID（数据库字段）
    private Integer targetId;
    // 收藏的景点（非数据库字段，仅用于查询返回）
    @TableField(exist = false)
    private ScenicEntity scenicId;
    // 收藏的路线（非数据库字段，仅用于查询返回）
    @TableField(exist = false)
    private RouteEntity routeId;
    // 收藏的攻略（非数据库字段，仅用于查询返回）
    @TableField(exist = false)
    private StrategyEntity strategyId;
    // 用于按收藏用户ID过滤（非数据库字段）
    @TableField(exist = false)
    private Integer collectorId;
    // 管理端联合搜索：收藏者昵称 / 景点名 / 路线名 / 攻略标题（非数据库字段）
    @TableField(exist = false)
    private String keyword;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
