package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.mapper.TravelUserMapper;
import com.zjx.travel.service.TravelUserService;
import com.zjx.travel.util.PasswordUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class TravelUserServiceImpl implements TravelUserService {
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

    public boolean update(TravelUserEntity travelUser){
        // 对密码进行 BCrypt 加密
        if (travelUser.getPassword() != null && !travelUser.getPassword().isEmpty()) {
            travelUser.setPassword(PasswordUtil.encode(travelUser.getPassword()));
        }
        // 管理员改角色时同样只接受 0/1，脏值按普通用户处理
        if (travelUser.getUserRole() != null
                && travelUser.getUserRole() != 0 && travelUser.getUserRole() != 1) {
            travelUser.setUserRole(0);
        }
        return this.travelUserMapper.update(travelUser) > 0;
    }

    public boolean deleteById(Integer id){
        return this.travelUserMapper.deleteById(id) > 0;
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