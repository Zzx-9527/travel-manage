package com.zjx.travel.service;

import com.zjx.travel.domain.TravelUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface TravelUserService {
    TravelUserEntity queryById(Integer id);

    Page<TravelUserEntity> queryByPage(TravelUserEntity travelUser, Integer page, Integer size, String orderCol, String orderDirect);

    /** 管理员新增用户（可指定角色/状态）。 */
    boolean insert(TravelUserEntity travelUser);

    /** 匿名注册：服务端强制普通用户角色和启用状态。 */
    boolean register(TravelUserEntity travelUser);

    boolean update(TravelUserEntity travelUser);

    boolean deleteById(Integer id);

    //登录
    TravelUserEntity login(TravelUserEntity travelUser);

    // 查询当前用户信息（不返回密码）
    TravelUserEntity myProfile(Integer id);

    // 修改密码（校验原密码）
    boolean updatePassword(Integer id, String oldPassword, String newPassword);
}