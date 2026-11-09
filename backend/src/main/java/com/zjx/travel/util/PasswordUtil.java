package com.zjx.travel.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 对密码进行加密
     */
    public static String encode(String pwd) {
        return encoder.encode(pwd);
    }

    /**
     * 验证密码是否匹配
     */
    public static boolean matches(String rawPwd, String encodedPwd) {
        return encoder.matches(rawPwd, encodedPwd);
    }
}
