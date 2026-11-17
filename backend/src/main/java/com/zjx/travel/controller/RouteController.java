package com.zjx.travel.controller;

import jakarta.validation.Valid;

import com.zjx.travel.domain.RouteEntity;
import com.zjx.travel.service.RouteService;
import com.zjx.travel.util.*;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.*;

@RestController
@RequestMapping("route")
public class RouteController {
    @Resource
    private RouteService routeService;
    @Resource
    private MessageNotifier messageNotifier;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        RouteEntity route = this.routeService.queryById(id);

        if (route == null) {
            return returnVO;
        }
        // 待审核/已驳回内容只对作者本人和管理员可见，不能靠猜 ID 读取
        Integer ownerId = route.getUserId() == null ? null : route.getUserId().getId();
        if (!Integer.valueOf(1).equals(route.getStatus()) && !SecurityUtil.isSelfOrAdmin(ownerId)) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(route);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(RouteEntity route, Integer page, Integer size, String orderCol, String orderDirect, Boolean mine) {

        boolean admin = SecurityUtil.isAdmin();
        if (Boolean.TRUE.equals(mine)) {
            // “我的发布”场景：非管理员强制按当前登录人过滤，避免传 userId 查到别人的数据
            if (!admin) {
                route.setUserId(SecurityUtil.currentUserRef());
            }
        } else if (!admin) {
            // 公开列表只返回审核通过的路线，忽略前端传入的审核状态
            route.setStatus(1);
        }

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<RouteEntity> pageVO = 
            this.routeService.queryByPage(
                route, 
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
    public ReturnVO add(@Valid @RequestBody RouteEntity route) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 发布人一律取当前登录人，忽略前端传来的 userId
        route.setUserId(SecurityUtil.currentUserRef());
        // 普通用户发布必须待审核；管理员后台新增允许直接指定审核状态
        if (!SecurityUtil.isAdmin()) {
            route.setStatus(0);
            route.setCreateTime(null);
            route.setUpdateTime(null);
        }

        //入库
        if(!this.routeService.insert(route) || route.getId() == null){
            //入库失败
            returnVO.setMsg("路线创建失败，请稍后重试");
            return returnVO;
        }

        //入库成功：返回新路线 ID，前端可立即跳转配置行程
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");
        returnVO.setContent(Map.of("id", route.getId()));

        return returnVO;
    }

    @PostMapping("edit")
    public ReturnVO edit(@Valid @RequestBody RouteEntity route) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        if (route.getId() == null) {
            return returnVO;
        }

        RouteEntity old = this.routeService.queryById(route.getId());
        if (old == null) {
            return returnVO;
        }

        // 归属校验：非管理员只能改自己的路线
        Integer ownerId = old.getUserId() == null ? null : old.getUserId().getId();
        SecurityUtil.requireSelfOrAdmin(ownerId);

        boolean admin = SecurityUtil.isAdmin();
        // 归属人不能被编辑请求转移
        route.setUserId(SecurityUtil.userRef(ownerId));

        boolean auditChanged = false;
        boolean auditPassed = false;
        if (admin) {
            // 仅管理员可以变更审核状态，且只通知 0 → 1/2 的有效状态变化
            if (route.getStatus() != null && (route.getStatus() == 1 || route.getStatus() == 2)) {
                auditChanged = old.getStatus() != null && old.getStatus() == 0;
                auditPassed = route.getStatus() == 1;
            }
        } else {
            // 普通用户修改内容后重新进入待审核，防止先发布再改内容绕过审核
            route.setStatus(0);
            route.setCreateTime(null);
            route.setUpdateTime(null);
        }

        //修改
        if(!this.routeService.update(route)){
            //修改失败
            return returnVO;
        }

        // 审核状态发生变化后通知发布者
        if (auditChanged) {
            String title = route.getName() != null ? route.getName() : old.getName();
            this.messageNotifier.sendAuditResult(ownerId, "路线", title, auditPassed);
        }

        //修改成功
        returnVO.setCode(1);
        returnVO.setMsg("修改成功");

        return returnVO;
    }

    @PostMapping("deleteById/{id}")
    public ReturnVO deleteById(@PathVariable("id") Integer id) {        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        RouteEntity old = this.routeService.queryById(id);
        if (old == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());

        if(!this.routeService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

}
