package com.zjx.travel.domain;

import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 修改密码请求参数
 */
@Data
public class UpdatePasswordVO {
    // 用户ID
    private Integer id;
    // 原密码
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    // 新密码
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位")
    private String newPassword;
}
