package com.zjx.travel.util;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.zjx.travel.domain.TravelUserEntity;

/**
 * 当前登录人的统一入口。
 *
 * <p>背景：项目在接入 Sa-Token 之前，所有接口的"操作人"都来自前端传的 {@code userId}，
 * 任何人改个参数就能操作别人的数据。这里把取当前用户的逻辑收口，业务层一律从这里拿身份，
 * 前端传的 userId 只作为"查询条件"参考，写入和鉴权一律以 token 为准。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 当前登录用户 id（未登录会抛 NotLoginException，由全局异常处理转 401） */
    public static Integer currentUserId() {
        return StpUtil.getLoginIdAsInt();
    }

    public static boolean isAdmin() {
        return StpUtil.hasRole("admin");
    }

    /** 构造一个只带 id 的用户引用，用于实体的 userId 字段 */
    public static TravelUserEntity currentUserRef() {
        return userRef(currentUserId());
    }

    public static TravelUserEntity userRef(Integer userId) {
        TravelUserEntity user = new TravelUserEntity();
        user.setId(userId);
        return user;
    }

    /**
     * 只有本人或管理员可以继续，否则抛 403。
     *
     * @param ownerId 资源归属人 id，允许为空（为空时非管理员一律拒绝）
     */
    public static void requireSelfOrAdmin(Integer ownerId) {
        if (!isSelfOrAdmin(ownerId)) {
            throw new NotPermissionException("resource:access:other");
        }
    }

    /** 当前登录人是否为资源归属人或管理员，用于审核内容的读权限判断。 */
    public static boolean isSelfOrAdmin(Integer ownerId) {
        return isAdmin() || (ownerId != null && ownerId.equals(currentUserId()));
    }
}
