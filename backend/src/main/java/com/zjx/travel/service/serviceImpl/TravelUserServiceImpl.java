package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.mapper.TravelUserMapper;
import com.zjx.travel.service.TravelUserService;
import com.zjx.travel.util.PasswordUtil;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class TravelUserServiceImpl implements TravelUserService {
    private static final Logger log = LoggerFactory.getLogger(TravelUserServiceImpl.class);
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private TravelUserMapper travelUserMapper;

    public TravelUserEntity queryById(Integer id){
        return this.travelUserMapper.queryById(id);
    }

    public Page<TravelUserEntity> queryByPage(TravelUserEntity travelUser, Integer page, Integer size, String orderCol, String orderDirect){

        if(page == null || page <= 0){
            page = PAGE_DEFAULT;
        }
        if(size == null || size <= 0){
            size = SIZE_DEFAULT;
        }

        Sort sort = null;
        if(orderCol != null) {
            Sort.Order order = new Sort.Order(("DESC".equals(orderDirect) ? Sort.Direction.DESC : Sort.Direction.ASC), orderCol);
            sort = Sort.by(order);
        }

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if(sort != null){
            pageRequest = PageRequest.of(page - 1, size, sort);
        }

        long total = this.travelUserMapper.count(travelUser);

        return new PageImpl<>(this.travelUserMapper.queryAllByLimit(travelUser, pageRequest), pageRequest, total);
    }

    public boolean insert(TravelUserEntity travelUser){
        // 对密码进行 BCrypt 加密
        if (travelUser.getPassword() != null && !travelUser.getPassword().isEmpty()) {
            travelUser.setPassword(PasswordUtil.encode(travelUser.getPassword()));
        }
        // 角色只允许 0（普通用户）/ 1（管理员）：空值和脏值一律按普通用户落库，避免误发管理员权限
        if (travelUser.getUserRole() == null
                || (travelUser.getUserRole() != 0 && travelUser.getUserRole() != 1)) {
            travelUser.setUserRole(0);
        }
        // 注册接口不传 status，插入语句显式写入会导致非空约束失败，这里默认启用
        if (travelUser.getStatus() == null) {
            travelUser.setStatus(1);
        }
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        Date now = new Date();
        if (travelUser.getCreateTime() == null) {
            travelUser.setCreateTime(now);
        }
        if (travelUser.getUpdateTime() == null) {
            travelUser.setUpdateTime(now);
        }
        return this.travelUserMapper.insert(travelUser) > 0;
    }

    public boolean register(TravelUserEntity travelUser) {
        if (travelUser == null || travelUser.getAccount() == null) {
            return false;
        }
        // 公开注册不接受任何角色/状态输入，服务端再次强制兜底
        travelUser.setId(null);
        travelUser.setUserRole(0);
        travelUser.setStatus(1);
        travelUser.setAccount(travelUser.getAccount().trim());
        if (this.travelUserMapper.queryByAccount(travelUser.getAccount()) != null) {
            return false;
        }
        try {
            return this.insert(travelUser);
        } catch (DuplicateKeyException e) {
            // 并发注册时由数据库唯一索引兜底
            return false;
        }
    }

    public boolean update(TravelUserEntity travelUser){
        TravelUserEntity old = travelUser.getId() == null ? null : this.travelUserMapper.queryById(travelUser.getId());
        boolean roleChanged = old != null && travelUser.getUserRole() != null
                && !Objects.equals(old.getUserRole(), travelUser.getUserRole());
        boolean statusChanged = old != null && travelUser.getStatus() != null
                && !Objects.equals(old.getStatus(), travelUser.getStatus());

        // 对密码进行 BCrypt 加密
        if (travelUser.getPassword() != null && !travelUser.getPassword().isEmpty()) {
            travelUser.setPassword(PasswordUtil.encode(travelUser.getPassword()));
        }
        // 管理员改角色时同样只接受 0/1，脏值按普通用户处理
        if (travelUser.getUserRole() != null
                && travelUser.getUserRole() != 0 && travelUser.getUserRole() != 1) {
            travelUser.setUserRole(0);
        }
        boolean updated = this.travelUserMapper.update(travelUser) > 0;
        if (updated && (roleChanged || statusChanged)) {
            // 角色或启用状态变化后旧会话必须失效，不能等 24 小时自然过期
            invalidateSessions(travelUser.getId());
        }
        return updated;
    }

    public boolean deleteById(Integer id){
        boolean deleted = this.travelUserMapper.deleteById(id) > 0;
        if (deleted) {
            invalidateSessions(id);
        }
        return deleted;
    }

    private void invalidateSessions(Integer userId) {
        if (userId == null) {
            return;
        }
        try {
            StpUtil.kickout(userId);
        } catch (Exception e) {
            // 数据库修改已经成功，不能因为会话清理异常把成功响应改成失败
            log.warn("清理用户登录态失败，userId={}, cause={}", userId, e.toString());
        }
    }

    public TravelUserEntity login(TravelUserEntity loginUser){
        // 先根据账号查询用户
        TravelUserEntity user = this.travelUserMapper.queryByAccount(loginUser.getAccount());
        if (user != null && PasswordUtil.matches(loginUser.getPassword(), user.getPassword())) {
            return user;
        }
        return null;
    }

    public TravelUserEntity myProfile(Integer id){
        TravelUserEntity user = this.travelUserMapper.queryById(id);
        if (user != null) {
            // 不把密码返回给前端
            user.setPassword(null);
        }
        return user;
    }

    public boolean updatePassword(Integer id, String oldPassword, String newPassword){
        if (id == null || oldPassword == null || newPassword == null) {
            return false;
        }
        TravelUserEntity user = this.travelUserMapper.queryById(id);
        if (user == null) {
            return false;
        }
        // 校验原密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            return false;
        }
        // 更新为新密码（BCrypt 加密）
        TravelUserEntity update = new TravelUserEntity();
        update.setId(id);
        update.setPassword(PasswordUtil.encode(newPassword));
        update.setUpdateTime(new Date());
        return this.travelUserMapper.update(update) > 0;
    }
}