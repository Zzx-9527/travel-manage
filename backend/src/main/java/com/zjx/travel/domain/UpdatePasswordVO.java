package com.zjx.travel.domain;

import lombok.Data;

/**
 * 修改密码请求参数
 */
@Data
public class UpdatePasswordVO {
    // 用户ID
    private Integer id;
    // 原密码
    private String oldPassword;
    // 新密码
    private String newPassword;
}
