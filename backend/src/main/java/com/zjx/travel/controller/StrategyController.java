package com.zjx.travel.controller;

import jakarta.validation.Valid;

import com.zjx.travel.domain.StrategyEntity;
import com.zjx.travel.service.FileStorageService;
import com.zjx.travel.service.StrategyService;
import com.zjx.travel.util.*;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.*;

@RestController
@RequestMapping("strategy")
public class StrategyController {
    @Resource
    private StrategyService strategyService;
    @Resource
    private MessageNotifier messageNotifier;
    @Resource
    private FileStorageService fileStorageService;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        StrategyEntity strategy = this.strategyService.queryById(id);

        if (strategy == null) {
            return returnVO;
        }
        // 待审核/已驳回攻略只对作者本人和管理员可见
        Integer ownerId = strategy.getUserId() == null ? null : strategy.getUserId().getId();
        if (!Integer.valueOf(1).equals(strategy.getAuditStatus()) && !SecurityUtil.isSelfOrAdmin(ownerId)) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(strategy);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(StrategyEntity strategy, Integer page, Integer size, String orderCol, String orderDirect, Boolean mine) {

        boolean admin = SecurityUtil.isAdmin();
        if (Boolean.TRUE.equals(mine)) {
            // “我的发布”场景：非管理员强制按当前登录人过滤，避免传 userId 查到别人的数据
            if (!admin) {
                strategy.setUserId(SecurityUtil.currentUserRef());
            }
        } else if (!admin) {
            // 公开列表只返回审核通过的攻略，忽略前端传入的审核状态
            strategy.setAuditStatus(1);
        }

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<StrategyEntity> pageVO = 
            this.strategyService.queryByPage(
                strategy, 
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
    public ReturnVO add(@Valid @RequestBody StrategyEntity strategy) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 发布人一律取当前登录人，忽略前端传来的 userId
        strategy.setUserId(SecurityUtil.currentUserRef());
        // 普通用户发布必须待审核，收藏/阅读数只能从 0 开始；管理员后台新增可按管理字段落库
        if (!SecurityUtil.isAdmin()) {
            strategy.setAuditStatus(0);
            strategy.setCollectCount(0);
            strategy.setReadCount(0);
            strategy.setCreateTime(null);
            strategy.setUpdateTime(null);
        }

        //入库
        if(!this.strategyService.insert(strategy)){
            //入库失败
            return returnVO;
        }

        //入库成功
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");

        return returnVO;
    }

    @PostMapping("edit")
    public ReturnVO edit(@Valid @RequestBody StrategyEntity strategy) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        if (strategy.getId() == null) {
            return returnVO;
        }

        StrategyEntity old = this.strategyService.queryById(strategy.getId());
        if (old == null) {
            return returnVO;
        }

        // 归属校验：非管理员只能改自己的攻略
        Integer ownerId = old.getUserId() == null ? null : old.getUserId().getId();
        SecurityUtil.requireSelfOrAdmin(ownerId);

        boolean admin = SecurityUtil.isAdmin();
        // 归属人不能被编辑请求转移
        strategy.setUserId(SecurityUtil.userRef(ownerId));

        boolean auditChanged = false;
        boolean auditPassed = false;
        if (admin) {
            // 仅管理员可以变更审核状态，且只通知 0 → 1/2 的有效状态变化
            if (strategy.getAuditStatus() != null
                    && (strategy.getAuditStatus() == 1 || strategy.getAuditStatus() == 2)) {
                auditChanged = old.getAuditStatus() != null && old.getAuditStatus() == 0;
                auditPassed = strategy.getAuditStatus() == 1;
            }
        } else {
            // 普通用户不得改审核状态和统计字段；修改内容后重新进入待审核
            strategy.setAuditStatus(0);
            strategy.setCollectCount(null);
            strategy.setReadCount(null);
            strategy.setCreateTime(null);
            strategy.setUpdateTime(null);
        }

        //修改
        if(!this.strategyService.update(strategy)){
            //修改失败
            return returnVO;
        }

        // 单图覆盖：封面换了且更新成功，删掉磁盘上的旧图
        if (strategy.getCover() != null && !strategy.getCover().equals(old.getCover())) {
            this.fileStorageService.deleteByUrl(old.getCover());
        }

        // 审核状态发生变化后通知发布者
        if (auditChanged) {
            String title = strategy.getTitle() != null ? strategy.getTitle() : old.getTitle();
            this.messageNotifier.sendAuditResult(ownerId, "攻略", title, auditPassed);
        }

        //修改成功
        returnVO.setCode(1);
        returnVO.setMsg("修改成功");

        return returnVO;
    }

    @PostMapping("deleteById/{id}")
    public ReturnVO deleteById(@PathVariable("id") Integer id) {        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        StrategyEntity old = this.strategyService.queryById(id);
        if (old == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());

        if(!this.strategyService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

}
