package com.zjx.travel.config;

import cn.dev33.satoken.stp.StpInterface;
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
 * 禁用账号不返回任何角色。
 *
 * <p>角色以数据库为唯一权威来源，每次鉴权都实时读取，不信任旧会话中的角色快照。
 * 这样管理员被降权后，即使旧 token 还没有过期，也会立即失去后台权限。
 *
 * <p>权限点暂时为空：本项目只做接口级角色控制，没有细到按钮/资源的权限设计。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final Logger log = LoggerFactory.getLogger(StpInterfaceImpl.class);

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";

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
     * 实时回源数据库读取角色。
     * 查库失败时返回 null（等同于“没有任何角色”），表现为 403，而不是 500。
     */
    private Integer readUserRole(Object loginId) {
        try {
            Integer userId = Integer.valueOf(String.valueOf(loginId));
            TravelUserEntity user = travelUserService.queryById(userId);
            if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
                return null;
            }
            return user.getUserRole();
        } catch (Exception e) {
            log.warn("查询用户角色失败，按无角色处理，loginId={}, cause={}", loginId, e.toString());
            return null;
        }
    }
}
