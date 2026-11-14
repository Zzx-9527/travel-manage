package com.zjx.travel.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.zjx.travel.domain.MessageEntity;
import com.zjx.travel.service.MessageService;
import com.zjx.travel.util.*;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.*;

@RestController
@RequestMapping("message")
public class MessageController {
    @Resource
    private MessageService messageService;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        MessageEntity message = this.messageService.queryById(id);

        if(message == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(message.getUserId() == null ? null : message.getUserId().getId());

        returnVO = ReturnVO.getSuccessDataReturnVO(message);

        return returnVO;
    }

    /**
     * 分页查询消息（可按 type / isRead / title 过滤）
     * GET /travel/message/queryByPage?type=0&isRead=0&page=1&size=10
     */
    @GetMapping("queryByPage")
    public ReturnVO queryByPage(MessageEntity message, Integer page, Integer size, String orderCol, String orderDirect) {

        // 越权防护：非管理员只能查自己的消息，忽略前端传来的 userId
        if (!SecurityUtil.isAdmin()) {
            message.setUserId(SecurityUtil.currentUserRef());
        }

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<MessageEntity> pageVO =
            this.messageService.queryByPage(
                message,
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

    /**
     * 新增消息（后台发送通知）
     * POST /travel/message/add
     * body: { "userId": {"id": 1}, "title": "标题", "content": "内容", "type": 0 }
     * type=3 表示全站横幅，此时 userId 可省略，后端自动补当前管理员并置为已读。
     */
    @PostMapping("add")
    @SaCheckRole("admin")
    public ReturnVO add(@RequestBody MessageEntity message) {

        // 全站横幅：接收人固定为当前管理员，并直接置为已读，避免个人消息中心多出未读横幅
        if (Integer.valueOf(3).equals(message.getType())) {
            message.setUserId(SecurityUtil.currentUserRef());
            message.setIsRead(1);
        }

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        //入库
        if(!this.messageService.insert(message)){
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
    public ReturnVO edit(@RequestBody MessageEntity message) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 归属校验：非管理员只能改自己的消息
        if (message.getId() != null) {
            MessageEntity old = this.messageService.queryById(message.getId());
            if (old == null) {
                return returnVO;
            }
            SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());
        }

        //修改
        if(!this.messageService.update(message)){
            //修改失败
            return returnVO;
        }

        //修改成功
        returnVO.setCode(1);
        returnVO.setMsg("修改成功");

        return returnVO;
    }

    @PostMapping("deleteById/{id}")
    public ReturnVO deleteById(@PathVariable("id") Integer id) {        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        MessageEntity old = this.messageService.queryById(id);
        if (old == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());

        if(!this.messageService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

    /**
     * 标记已读（支持单条/批量）
     * POST /travel/message/markRead
     * body: { "id": 1 }  或  { "ids": [1,2,3] }
     */
    @PostMapping("markRead")
    public ReturnVO markRead(@RequestBody MessageEntity message) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        int count = this.messageService.markRead(message.getId(), message.getIds(), SecurityUtil.currentUserId());
        if (count < 0) {
            returnVO.setMsg("参数不完整");
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("标记成功");
        returnVO.setContent(count);
        return returnVO;
    }

    /**
     * 全部标记已读（当前登录人）
     * POST /travel/message/markAllRead
     */
    @PostMapping("markAllRead")
    public ReturnVO markAllRead() {
        int count = this.messageService.markAllRead(SecurityUtil.currentUserId());
        if (count < 0) {
            return new ReturnVO(0, "参数不完整");
        }
        ReturnVO returnVO = new ReturnVO(1, "标记成功");
        returnVO.setContent(count);
        return returnVO;
    }

    /**
     * 未读消息数量（消息中心小红点）
     * GET /travel/message/unreadCount
     */
    @GetMapping("unreadCount")
    public ReturnVO unreadCount() {
        // 只统计当前登录人的未读，忽略前端传的 userId
        long count = this.messageService.unreadCount(SecurityUtil.currentUserId());
        return ReturnVO.getSuccessDataReturnVO(count);
    }

}
