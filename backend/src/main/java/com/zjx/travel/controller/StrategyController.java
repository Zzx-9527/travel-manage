package com.zjx.travel.controller;

import com.zjx.travel.domain.StrategyEntity;
import com.zjx.travel.service.FileStorageService;
import com.zjx.travel.service.StrategyService;
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

        if(strategy == null) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(strategy);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(StrategyEntity strategy, Integer page, Integer size, String orderCol, String orderDirect, Boolean mine) {

        // “我的发布”场景：非管理员强制按当前登录人过滤，避免传 userId 查到别人的数据
        if (Boolean.TRUE.equals(mine) && !SecurityUtil.isAdmin()) {
            strategy.setUserId(SecurityUtil.currentUserRef());
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
    public ReturnVO add(@RequestBody StrategyEntity strategy) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 发布人一律取当前登录人，忽略前端传来的 userId
        strategy.setUserId(SecurityUtil.currentUserRef());

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
    public ReturnVO edit(@RequestBody StrategyEntity strategy) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 归属校验：非管理员只能改自己的攻略；同时拿到旧数据用于审核状态比对
        StrategyEntity old = strategy.getId() == null ? null : this.strategyService.queryById(strategy.getId());
        if (old != null) {
            SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());
        }

        // 检测审核状态变化：0(待审核) → 1(通过)/2(驳回) 才通知，普通编辑/重复审核不打扰用户
        boolean auditChanged = false;
        boolean auditPassed = false;
        if (old != null && strategy.getAuditStatus() != null
                && (strategy.getAuditStatus() == 1 || strategy.getAuditStatus() == 2)) {
            auditChanged = old.getAuditStatus() != null && old.getAuditStatus() == 0;
            auditPassed = strategy.getAuditStatus() == 1;
        }

        //修改
        if(!this.strategyService.update(strategy)){
            //修改失败
            return returnVO;
        }

        // 单图覆盖：封面换了且更新成功，删掉磁盘上的旧图
        if (old != null && strategy.getCover() != null && !strategy.getCover().equals(old.getCover())) {
            this.fileStorageService.deleteByUrl(old.getCover());
        }

        // 审核状态发生变化后通知发布者
        if (auditChanged) {
            Integer receiverId = strategy.getUserId() != null ? strategy.getUserId().getId() : null;
            this.messageNotifier.sendAuditResult(receiverId, "攻略", strategy.getTitle(), auditPassed);
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