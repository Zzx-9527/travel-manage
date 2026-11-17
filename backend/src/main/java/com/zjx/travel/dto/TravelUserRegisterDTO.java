package com.zjx.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公开注册专用入参。
 *
 * <p>这里刻意不包含 {@code userRole}、{@code status} 等管理字段，
 * 避免匿名请求通过批量赋值创建管理员或绕过账号状态限制。
 */
@Data
public class TravelUserRegisterDTO {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度需在 4-20 位")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位")
    private String password;

    @Size(max = 50, message = "昵称不能超过 50 字")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 255, message = "头像地址不能超过 255 字")
    private String avatar;
}
