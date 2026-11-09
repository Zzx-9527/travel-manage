package com.zjx.travel.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.zjx.travel.domain.OnlineUserVO;
import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.domain.UpdatePasswordVO;
import com.zjx.travel.service.FileStorageService;
import com.zjx.travel.service.TravelUserService;
import com.zjx.travel.util.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@RestController
@RequestMapping("travelUser")
public class TravelUserController {

    private static final Logger log = LoggerFactory.getLogger(TravelUserController.class);

    /** 登录限流：同一 IP 60 秒内最多 5 次 */
    private static final int LOGIN_RATE_LIMIT_TIMES = 5;
    private static final int LOGIN_RATE_LIMIT_WINDOW_SECONDS = 60;

    /**
     * Sa-Token 在 Redis 中的 key 前缀。
     *
     * <p>注意：1.44 的 key 前缀**不是**固定写死的 {@code satoken:}，而是跟随 {@code sa-token.token-name}，
     * 本项目配了 {@code token-name=Authorization}，所以实际 key 形如
     * {@code Authorization:login:token:xxx}。这里从配置动态取，避免以后改 token-name 后扫不到。
     */
    private static String loginTokenPrefix() {
        return SaManager.getConfig().getTokenName() + ":login:token:";
    }

    @Resource
    private TravelUserService travelUserService;

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private LockUtil lockUtil;

    @GetMapping("queryById/{id}")
    @SaCheckRole("admin")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        TravelUserEntity travelUser = this.travelUserService.queryById(id);

        if(travelUser == null) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(travelUser);

        return returnVO;
    }

    @GetMapping("queryByPage")
    @SaCheckRole("admin")
    public ReturnVO queryByPage(TravelUserEntity travelUser, Integer page, Integer size, String orderCol, String orderDirect) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<TravelUserEntity> pageVO = 
            this.travelUserService.queryByPage(
                travelUser, 
                page, size,
                orderCol, orderDirect);

        //没有数据
        if(pageVO.getContent().isEmpty()){
            return returnVO;
        }

        //查询成功
        returnVO = ReturnVO.getSuccessDataReturnVO(pageVO);

        return returnVO;
    }

    @PostMapping("add")
    public ReturnVO add(@RequestBody TravelUserEntity travelUser) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        //入库
        if(!this.travelUserService.insert(travelUser)){
            //入库失败
            return returnVO;
        }

        //入库成功
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");

        return returnVO;
    }

    @PostMapping("edit")
    @SaCheckRole("admin")
    public ReturnVO edit(@RequestBody TravelUserEntity travelUser) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 单图覆盖：先取旧记录，更新成功后再删旧头像
        TravelUserEntity old = travelUser.getId() == null ? null : this.travelUserService.queryById(travelUser.getId());

        //修改
        if(!this.travelUserService.update(travelUser)){
            //修改失败
            return returnVO;
        }

        if (old != null && travelUser.getAvatar() != null && !travelUser.getAvatar().equals(old.getAvatar())) {
            this.fileStorageService.deleteByUrl(old.getAvatar());
        }

        // 改成禁用（status=0）时立即踢下线，否则对方已持有的 token 在 24 小时内仍然可用
        if (travelUser.getStatus() != null && travelUser.getStatus() == 0 && travelUser.getId() != null) {
            StpUtil.kickout(travelUser.getId());
            log.info("用户被禁用并强制下线：userId={}", travelUser.getId());
        }

        //修改成功
        returnVO.setCode(1);
        returnVO.setMsg("修改成功");

        return returnVO;
    }

    @PostMapping("deleteById/{id}")
    @SaCheckRole("admin")
    public ReturnVO deleteById(@PathVariable("id") Integer id) {        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        if(!this.travelUserService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        // 账号已删除，立即踢掉其全部登录态，避免旧 token 在有效期内继续可用
        StpUtil.kickout(id);

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

    /**
     * 登录：校验账号密码后，由 Sa-Token 生成 token 并把会话写入 Redis。
     *
     * <p>返回结构保持与旧版一致（{@code token} 字段名不变），额外补了 {@code tokenName}
     * 与 {@code expire}，前端无需改代码即可继续用。
     */
    @PostMapping("login")
    public ReturnVO login(@RequestBody TravelUserEntity userinfo, HttpServletRequest request) {
        //定义一个失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 登录限流：按 IP 计数，防暴力撞库。限流组件异常时 LockUtil 内部降级放行，
        // 不会因为 Redis 抖动导致谁都登录不了。
        String clientIp = resolveClientIp(request);
        if (!lockUtil.tryAcquireFixedWindow(RedisKeys.loginRateLimit(clientIp),
                LOGIN_RATE_LIMIT_TIMES, LOGIN_RATE_LIMIT_WINDOW_SECONDS)) {
            log.warn("登录请求过于频繁，ip={}", clientIp);
            returnVO.setMsg("操作过于频繁，请 1 分钟后再试");
            return returnVO;
        }

        TravelUserEntity loginUser = this.travelUserService.login(userinfo);
        if (loginUser == null) {
            returnVO.setMsg("用户名或密码错误");
            return returnVO;
        }

        // 账号被禁用（status=0）时禁止登录：密码正确也不放行
        if (loginUser.getStatus() != null && loginUser.getStatus() == 0) {
            log.warn("账号已被禁用，拒绝登录：account={}", loginUser.getAccount());
            returnVO.setMsg("账号已被禁用，请联系管理员");
            return returnVO;
        }

        // Sa-Token 登录：会话落 Redis，token 由框架生成（默认 tik 风格，非 JWT）
        StpUtil.login(loginUser.getId());
        // 会话里冗余用户基础信息，在线列表 / 角色判断可直接读，不必每次查库
        StpUtil.getSession().set("account", loginUser.getAccount());
        StpUtil.getSession().set("nickname", loginUser.getNickname());
        StpUtil.getSession().set("userRole", loginUser.getUserRole());

        // 将 Token 放入返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("user", loginUser);
        result.put("token", StpUtil.getTokenValue());
        result.put("tokenName", StpUtil.getTokenName());
        result.put("expire", SaManager.getConfig().getTimeout());

        returnVO = ReturnVO.getSuccessDataReturnVO(result);
        returnVO.setMsg("登录成功");
        returnVO.setCode(1);
        return returnVO;
    }

    /**
     * 登出：删除当前 token 对应的会话，旧 token 立即失效（这是替换 JWT 的核心收益）。
     */
    @PostMapping("logout")
    public ReturnVO logout() {
        StpUtil.logout();
        ReturnVO returnVO = ReturnVO.getSuccessDataReturnVO();
        returnVO.setMsg("已退出登录");
        return returnVO;
    }

    /**
     * 当前用户信息（不返回密码）。
     *
     * <p>用户身份只认 token：拦截器已保证登录，这里直接取会话里的 userId。
     * 不再接受 {@code ?id=} 参数——未登录的请求在拦截器就被 401 挡掉了，那条分支本来就走不到。
     * GET /travel/travelUser/myProfile
     */
    @GetMapping("myProfile")
    public ReturnVO myProfile() {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Integer userId = StpUtil.getLoginIdAsInt();

        TravelUserEntity user = this.travelUserService.myProfile(userId);
        if (user == null) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(user);
        return returnVO;
    }

    /**
     * 在线用户列表（仅管理员）。
     *
     * <p>实现方式：遍历 Redis 中 {@code satoken:login:token:*} 的有效 token，
     * 通过 {@code getLoginIdByToken} 反查 userId，再按 userId 聚合出 token 数。
     * 用户信息优先从 Sa-Session 读取（登录时已写入），避免逐个查库。
     *
     * <p>注意：这是"全量扫描 + 内存分页"，演示规模（几十人）没问题；
     * 用户量大时应改为维护在线 ZSet 或独立在线表，见规划文档 R8。
     */
    @GetMapping("online")
    @SaCheckRole("admin")
    public ReturnVO online(@RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size) {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 10;
        }

        // 用 SCAN 扫出所有有效 token 的 key，再剥掉前缀拿到 token 值；
        // 不用 StpUtil.searchTokenValue，是因为不同版本的 Redis DAO 对返回内容
        // 是否包含前缀处理不一致，直接扫 key 行为最确定。
        String tokenPrefix = loginTokenPrefix();
        List<String> tokenKeys = redisUtil.scanKeys(tokenPrefix + "*");
        Map<Integer, OnlineUserVO> merged = new LinkedHashMap<>();
        for (String tokenKey : tokenKeys) {
            if (!tokenKey.startsWith(tokenPrefix)) {
                continue;
            }
            String token = tokenKey.substring(tokenPrefix.length());
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                continue;
            }
            Integer userId = Integer.valueOf(String.valueOf(loginId));
            OnlineUserVO vo = merged.get(userId);
            if (vo == null) {
                vo = new OnlineUserVO();
                vo.setUserId(userId);
                vo.setTokenCount(0);
                SaSession session = StpUtil.getSessionByLoginId(userId, false);
                if (session != null) {
                    vo.setAccount(asString(session.get("account")));
                    vo.setNickname(asString(session.get("nickname")));
                    Object role = session.get("userRole");
                    vo.setUserRole(role instanceof Number ? ((Number) role).intValue() : null);
                    vo.setLoginTime(new Date(session.getCreateTime()));
                }
                merged.put(userId, vo);
            }
            vo.setTokenCount(vo.getTokenCount() + 1);
        }

        List<OnlineUserVO> all = new ArrayList<>(merged.values());
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("list", new ArrayList<>(all.subList(from, to)));

        ReturnVO returnVO = ReturnVO.getSuccessDataReturnVO(data);
        returnVO.setMsg("在线用户 " + total + " 人");
        return returnVO;
    }

    /**
     * 强制下线（仅管理员）：踢掉该账号的全部 token，被踢者下一次请求收到 401「账号被强制下线」。
     */
    @PostMapping("kickout/{userId}")
    @SaCheckRole("admin")
    public ReturnVO kickout(@PathVariable("userId") Integer userId) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        if (userId == null) {
            returnVO.setMsg("缺少用户ID");
            return returnVO;
        }
        if (userId.equals(StpUtil.getLoginIdAsInt())) {
            returnVO.setMsg("不能踢自己下线");
            return returnVO;
        }
        StpUtil.kickout(userId);
        returnVO.setCode(1);
        returnVO.setMsg("已强制下线");
        return returnVO;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 取客户端 IP：优先 X-Forwarded-For（有反向代理时），否则用 remoteAddr */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 修改当前登录人的资料（昵称 / 手机号 / 头像）。
     *
     * <p>不需要管理员权限：身份取自 token，只允许改这三个字段，
     * 角色、状态等字段即使传了也会被忽略（防止用户给自己提权）。
     * 头像遵循"单图覆盖"：更新成功后删除磁盘上的旧头像。
     */
    @PostMapping("updateProfile")
    public ReturnVO updateProfile(@RequestBody TravelUserEntity profile) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        Integer userId = SecurityUtil.currentUserId();

        boolean nothingToUpdate = profile.getNickname() == null
                && profile.getPhone() == null
                && profile.getAvatar() == null;
        if (nothingToUpdate) {
            returnVO.setMsg("没有需要修改的内容");
            return returnVO;
        }

        TravelUserEntity old = this.travelUserService.queryById(userId);
        if (old == null) {
            returnVO.setMsg("用户不存在");
            return returnVO;
        }

        // 只复制允许修改的字段，避免前端传 userRole/status 把自己改成管理员
        TravelUserEntity update = new TravelUserEntity();
        update.setId(userId);
        update.setNickname(profile.getNickname());
        update.setPhone(profile.getPhone());
        update.setAvatar(profile.getAvatar());
        this.travelUserService.update(update);

        // 单图覆盖：头像换了才删旧图（外部 URL 会被 deleteByUrl 自动忽略）
        if (profile.getAvatar() != null && !profile.getAvatar().equals(old.getAvatar())) {
            this.fileStorageService.deleteByUrl(old.getAvatar());
        }

        returnVO.setCode(1);
        returnVO.setMsg("修改成功");
        return returnVO;
    }

    /**
     * 修改密码（需校验原密码）
     * POST /travel/travelUser/updatePassword
     * body: { "oldPassword": "123456", "newPassword": "abc123" }
     *
     * <p>身份取自 token，body 里的 {@code id} 不再生效（防止改他人密码）。
     */
    @PostMapping("updatePassword")
    public ReturnVO updatePassword(@RequestBody UpdatePasswordVO vo) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        if (vo.getOldPassword() == null || vo.getNewPassword() == null
                || vo.getOldPassword().isEmpty() || vo.getNewPassword().isEmpty()) {
            returnVO.setMsg("参数不完整");
            return returnVO;
        }

        Integer userId = SecurityUtil.currentUserId();
        if (!this.travelUserService.updatePassword(userId, vo.getOldPassword(), vo.getNewPassword())) {
            returnVO.setMsg("原密码不正确或用户不存在");
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("修改成功");
        return returnVO;
    }

}