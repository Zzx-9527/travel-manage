package com.zjx.travel.controller;

import com.zjx.travel.domain.RouteEntity;
import com.zjx.travel.service.RouteService;
import com.zjx.travel.util.*;

import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

        if(route == null) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(route);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(RouteEntity route, Integer page, Integer size, String orderCol, String orderDirect, Boolean mine) {

        // “我的发布”场景：非管理员强制按当前登录人过滤，避免传 userId 查到别人的数据
        if (Boolean.TRUE.equals(mine) && !SecurityUtil.isAdmin()) {
            route.setUserId(SecurityUtil.currentUserRef());
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
    public ReturnVO add(@RequestBody RouteEntity route) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 发布人一律取当前登录人，忽略前端传来的 userId
        route.setUserId(SecurityUtil.currentUserRef());

        //入库
        if(!this.routeService.insert(route)){
            //入库失败
            return returnVO;
        }

        //入库成功
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");

        return returnVO;
    }

    @PostMapping("edit")
    public ReturnVO edit(@RequestBody RouteEntity route) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 归属校验：非管理员只能改自己的路线；同时拿到旧数据用于审核状态比对
        RouteEntity old = route.getId() == null ? null : this.routeService.queryById(route.getId());
        if (old != null) {
            SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());
        }

        // 检测审核状态变化：0(待审核) → 1(通过)/2(驳回) 才通知，普通编辑/重复审核不打扰用户
        boolean auditChanged = false;
        boolean auditPassed = false;
        if (old != null && route.getStatus() != null
                && (route.getStatus() == 1 || route.getStatus() == 2)) {
            auditChanged = old.getStatus() != null && old.getStatus() == 0;
            auditPassed = route.getStatus() == 1;
        }

        //修改
        if(!this.routeService.update(route)){
            //修改失败
            return returnVO;
        }

        // 审核状态发生变化后通知发布者
        if (auditChanged) {
            Integer receiverId = route.getUserId() != null ? route.getUserId().getId() : null;
            this.messageNotifier.sendAuditResult(receiverId, "路线", route.getName(), auditPassed);
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