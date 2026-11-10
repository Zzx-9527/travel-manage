package com.zjx.travel.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class MessageEntity {
    private Integer id;// 消息ID
    private TravelUserEntity userId;// 接收人
    private String title;// 消息标题
    private String content;// 消息内容
    private Integer type;// 消息类型 0系统 1订单 2审核
    private Integer isRead;// 是否已读 0未读 1已读
    // 用于批量已读（非数据库字段）
    @TableField(exist = false)
    private List<Integer> ids;
    // 查询用：排除指定消息类型（非数据库字段）
    @TableField(exist = false)
    private Integer typeNe;
    // 查询用：排除指定接收人（非数据库字段）
    @TableField(exist = false)
    private Integer userIdNe;
    // 查询用：标题/昵称/账号联合关键词（非数据库字段）
    @TableField(exist = false)
    private String keyword;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
