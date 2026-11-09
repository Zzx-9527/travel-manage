package com.zjx.travel.domain;

import lombok.Data;

import java.util.Date;

/**
 * 在线用户展示对象（仅用于后台在线列表，不对应数据库表）。
 *
 * <p>一个用户可能有多端登录（多 token），这里按 userId 聚合，用 {@code tokenCount} 表示端数。
 */
@Data
public class OnlineUserVO {

    private Integer userId;

    private String account;

    private String nickname;

    /** 0=普通用户，1=管理员（与 travel_user.user_role 一致） */
    private Integer userRole;

    /** 当前有效 token 数，多端登录时大于 1 */
    private Integer tokenCount;

    /** 会话创建时间（该用户最早一次登录的时间） */
    private Date loginTime;
}
