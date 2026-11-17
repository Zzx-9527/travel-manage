package com.zjx.travel.controller;

import jakarta.validation.Valid;

import com.zjx.travel.domain.StrategyCommentEntity;
import com.zjx.travel.domain.StrategyEntity;
import com.zjx.travel.service.StrategyCommentService;
import com.zjx.travel.service.StrategyService;
import com.zjx.travel.util.*;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.*;

@RestController
@RequestMapping("strategyComment")
public class StrategyCommentController {
    @Resource
    private StrategyCommentService strategyCommentService;
    @Resource
    private StrategyService strategyService;
    @Resource
    private MessageNotifier messageNotifier;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        StrategyCommentEntity strategyComment = this.strategyCommentService.queryById(id);

        if (strategyComment == null) {
            return returnVO;
        }
        // 待审核/已驳回评论只对评论者本人和管理员可见；
        // 评论联查里带出了所属攻略正文，因此父攻略也必须对当前用户可见。
        Integer ownerId = strategyComment.getUserId() == null ? null : strategyComment.getUserId().getId();
        if (!Integer.valueOf(1).equals(strategyComment.getStatus()) && !SecurityUtil.isSelfOrAdmin(ownerId)) {
            return returnVO;
        }
        if (!canViewParentStrategy(strategyComment.getStrategyId())) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(strategyComment);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(StrategyCommentEntity strategyComment, Integer page, Integer size, String orderCol, String orderDirect, Boolean mine) {

        boolean admin = SecurityUtil.isAdmin();
        if (!admin) {
            if (Boolean.TRUE.equals(mine)) {
                // “我的评论”场景：非管理员强制按当前登录人过滤，避免传 userId 查到别人的数据
                strategyComment.setUserId(SecurityUtil.currentUserRef());
            } else {
                // 公开评论列表只返回审核通过的评论
                strategyComment.setStatus(1);
            }
            // 评论结果会联查攻略正文，非管理员的任何列表都只允许访问已通过审核的攻略
            if (strategyComment.getStrategyId() == null) {
                strategyComment.setStrategyId(new StrategyEntity());
            }
            strategyComment.getStrategyId().setAuditStatus(1);
        }

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<StrategyCommentEntity> pageVO = 
            this.strategyCommentService.queryByPage(
                strategyComment, 
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
    public ReturnVO add(@Valid @RequestBody StrategyCommentEntity strategyComment) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Integer strategyId = strategyComment.getStrategyId() == null ? null : strategyComment.getStrategyId().getId();
        StrategyEntity strategy = strategyId == null ? null : this.strategyService.queryById(strategyId);
        if (!canViewParentStrategy(strategy)) {
            returnVO.setMsg("攻略不存在或尚未通过审核");
            return returnVO;
        }
        // 使用数据库中的完整攻略引用，避免信任客户端传入的嵌套字段
        strategyComment.setStrategyId(strategy);
        // 评论人一律取当前登录人，忽略前端传来的 userId
        strategyComment.setUserId(SecurityUtil.currentUserRef());
        // 普通用户评论必须待审核；管理员后台新增允许指定状态
        if (!SecurityUtil.isAdmin()) {
            strategyComment.setStatus(0);
            strategyComment.setCreateTime(null);
        }

        //入库
        if(!this.strategyCommentService.insert(strategyComment)){
            //入库失败
            return returnVO;
        }

        //入库成功
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");

        return returnVO;
    }

    @PostMapping("edit")
    public ReturnVO edit(@Valid @RequestBody StrategyCommentEntity strategyComment) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        if (strategyComment.getId() == null) {
            return returnVO;
        }

        StrategyCommentEntity old = this.strategyCommentService.queryById(strategyComment.getId());
        if (old == null) {
            return returnVO;
        }

        // 归属校验：非管理员只能改自己的评论
        Integer ownerId = old.getUserId() == null ? null : old.getUserId().getId();
        SecurityUtil.requireSelfOrAdmin(ownerId);

        boolean admin = SecurityUtil.isAdmin();
        // 归属人和所属攻略不能被编辑请求转移
        strategyComment.setUserId(SecurityUtil.userRef(ownerId));
        strategyComment.setStrategyId(old.getStrategyId());

        boolean auditChanged = false;
        boolean auditPassed = false;
        if (admin) {
            // 仅管理员可以变更审核状态，且只通知 0 → 1/2 的有效状态变化
            if (strategyComment.getStatus() != null
                    && (strategyComment.getStatus() == 1 || strategyComment.getStatus() == 2)) {
                auditChanged = old.getStatus() != null && old.getStatus() == 0;
                auditPassed = strategyComment.getStatus() == 1;
            }
        } else {
            // 普通用户修改评论后重新进入待审核，不能自行审核通过
            strategyComment.setStatus(0);
            strategyComment.setCreateTime(null);
        }

        //修改
        if(!this.strategyCommentService.update(strategyComment)){
            //修改失败
            return returnVO;
        }

        // 审核状态发生变化后通知评论者
        if (auditChanged) {
            this.messageNotifier.sendAuditResult(ownerId, "评论", "你的评论", auditPassed);
        }

        //修改成功
        returnVO.setCode(1);
        returnVO.setMsg("修改成功");

        return returnVO;
    }

    @PostMapping("deleteById/{id}")
    public ReturnVO deleteById(@PathVariable("id") Integer id) {        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        StrategyCommentEntity old = this.strategyCommentService.queryById(id);
        if (old == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());

        if(!this.strategyCommentService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

    private boolean canViewParentStrategy(StrategyEntity strategy) {
        if (strategy == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(strategy.getAuditStatus())) {
            return true;
        }
        Integer ownerId = strategy.getUserId() == null ? null : strategy.getUserId().getId();
        return SecurityUtil.isSelfOrAdmin(ownerId);
    }

}
