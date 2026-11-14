package com.zjx.travel.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.service.TravelUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色数据源：把业务里的 {@code userRole} 映射成框架认识的角色标识。
 *
 * <p>映射规则（与数据库约定一致）：{@code userRole == 1} → {@code admin}，
 * {@code userRole == 0}（普通用户）→ {@code user}。其他脏值一律按普通用户处理。
 * 注意：列默认值就是 0，注册时不需要显式指定角色。
 * 这样接口上就可以直接写 {@code @SaCheckRole("admin")}，而不是在每个方法里判断数字。
 *
 * <p>读取顺序：优先读登录时写进 Sa-Session 的 {@code userRole}（一次 Redis 读，不查库）；
 * 会话里没有时（例如历史会话、或登录后改过角色）再回源数据库并回填会话。
 *
 * <p>权限点暂时为空：本项目只做接口级角色控制，没有细到按钮/资源的权限设计。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final Logger log = LoggerFactory.getLogger(StpInterfaceImpl.class);

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";
    private static final String SESSION_KEY_USER_ROLE = "userRole";

    @Resource
    private TravelUserService travelUserService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Integer userRole = readUserRole(loginId);
        if (userRole == null) {
            return Collections.emptyList();
        }
        return Integer.valueOf(1).equals(userRole)
                ? Collections.singletonList(ROLE_ADMIN)
                : Collections.singletonList(ROLE_USER);
    }

    /**
     * 先读会话，读不到再查库并回填会话。
     * 查库失败时返回 null（等同于"没有任何角色"），表现为 403，而不是 500。
     */
    private Integer readUserRole(Object loginId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(loginId, false);
            if (session != null) {
                Object cached = session.get(SESSION_KEY_USER_ROLE);
                if (cached instanceof Integer) {
                    return (Integer) cached;
                }
                if (cached instanceof Number) {
                    return ((Number) cached).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("读取会话角色失败，将回源数据库，loginId={}, cause={}", loginId, e.toString());
        }

        try {
            Integer userId = Integer.valueOf(String.valueOf(loginId));
            TravelUserEntity user = travelUserService.queryById(userId);
            if (user == null) {
                return null;
            }
            Integer role = user.getUserRole();
            SaSession session = StpUtil.getSessionByLoginId(loginId, true);
            if (session != null && role != null) {
                session.set(SESSION_KEY_USER_ROLE, role);
            }
            return role;
        } catch (Exception e) {
            log.warn("查询用户角色失败，按无角色处理，loginId={}, cause={}", loginId, e.toString());
            return null;
        }
    }
}
