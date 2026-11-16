package com.zjx.travel.controller;

import jakarta.validation.Valid;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.service.ScenicOrdersService;
import com.zjx.travel.dto.*;
import com.zjx.travel.util.SecurityUtil;
import com.zjx.travel.util.LockUtil;
import com.zjx.travel.util.MessageNotifier;
import com.zjx.travel.util.RedisKeys;
import com.zjx.travel.util.ReturnVO;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("scenicOrders")
public class ScenicOrdersController {
    @Resource
    private ScenicOrdersService scenicOrdersService;
    @Resource
    private MessageNotifier messageNotifier;
    @Resource
    private LockUtil lockUtil;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        ScenicOrdersEntity scenicOrders = this.scenicOrdersService.queryById(id);

        if(scenicOrders == null) {
            return returnVO;
        }

        // 越权防护：普通用户只能查看自己的订单
        requireSelfOrAdmin(scenicOrders);

        returnVO = ReturnVO.getSuccessDataReturnVO(scenicOrders);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(ScenicOrdersEntity scenicOrders, Integer page, Integer size, String orderCol, String orderDirect) {
        if (scenicOrders == null) scenicOrders = new ScenicOrdersEntity();

        // 越权防护：非管理员强制按当前登录人过滤，忽略前端传来的 userId，避免"改个参数查全站订单"
        applyOwnerScope(scenicOrders);

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<ScenicOrdersEntity> pageVO = 
            this.scenicOrdersService.queryByPage(
                scenicOrders, 
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
     * 管理端订单状态统计：total / paidCount / unpaidCount
     * GET /travel/scenicOrders/statusStats?keyword=...
     */
    @GetMapping("statusStats")
    @SaCheckRole("admin")
    public ReturnVO statusStats(ScenicOrdersEntity scenicOrders) {
        if (scenicOrders == null) scenicOrders = new ScenicOrdersEntity();
        return ReturnVO.getSuccessDataReturnVO(this.scenicOrdersService.statusStats(scenicOrders));
    }

    @PostMapping("add")
    public ReturnVO add(@Valid @RequestBody ScenicOrderCreateDTO request) {
        Integer uid = SecurityUtil.currentUserId();
        if (!lockUtil.trySetOnce(RedisKeys.orderSubmit(uid), Duration.ofSeconds(5)))
            return new ReturnVO(0, "请勿重复提交，请 5 秒后再试");
        ScenicOrdersEntity created = scenicOrdersService.createOrder(request, uid);
        // 事务已提交，再发消息；消息失败只记日志，不影响已生成的订单
        messageNotifier.sendOrder(uid, "下单成功", "门票订单已创建：" + created.getOrderNo());
        Map<String, Object> content = new HashMap<>();
        content.put("id", created.getId());
        content.put("orderNo", created.getOrderNo());
        return new ReturnVO(1, "添加成功", content);
    }

    @PostMapping("cancel/{id}")
    public ReturnVO cancel(@PathVariable Integer id){
        Integer uid = SecurityUtil.currentUserId();
        ScenicOrdersEntity order = scenicOrdersService.queryById(id);
        scenicOrdersService.cancel(id, uid, SecurityUtil.isAdmin(), "MANUAL");
        if (order != null && order.getUserId() != null)
            messageNotifier.sendOrder(order.getUserId().getId(), "订单已取消", "门票订单已取消，余票已回补");
        return new ReturnVO(1,"取消成功");
    }

    @PostMapping("pay/{id}")
    public ReturnVO pay(@PathVariable Integer id){
        Integer uid = SecurityUtil.currentUserId();
        scenicOrdersService.pay(id, uid);
        messageNotifier.sendOrder(uid, "订单支付成功", "门票订单已模拟支付成功");
        return new ReturnVO(1,"支付成功");
    }

    @PostMapping("edit")
    public ReturnVO edit(@RequestBody ScenicOrderEditDTO request) { if(!scenicOrdersService.editContact(request,SecurityUtil.currentUserId())) return new ReturnVO(0,"订单不可编辑"); return new ReturnVO(1,"修改成功"); }

    @PostMapping("deleteById/{id}")
    public ReturnVO deleteById(@PathVariable("id") Integer id) {        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 越权防护：普通用户不能删除别人的订单
        requireSelfOrAdmin(this.scenicOrdersService.queryById(id));

        if(!this.scenicOrdersService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

    // ==================== 越权防护 ====================

    /** 非管理员只能按自己过滤，前端传什么 userId 都会被覆盖 */
    private void applyOwnerScope(ScenicOrdersEntity query) {
        if (StpUtil.hasRole("admin")) {
            return;
        }
        TravelUserEntity owner = new TravelUserEntity();
        owner.setId(StpUtil.getLoginIdAsInt());
        query.setUserId(owner);
    }

    /**
     * 订单只能本人或管理员操作。
     *
     * <p>注意：这里不判断"订单是否存在"——不存在时交给上层返回"没有找到数据"，
     * 避免通过 404/403 的差异探测出订单 id 是否存在。
     */
    private void requireSelfOrAdmin(ScenicOrdersEntity order) {
        if (order == null || StpUtil.hasRole("admin")) {
            return;
        }
        Integer ownerId = order.getUserId() == null ? null : order.getUserId().getId();
        if (ownerId == null || !String.valueOf(ownerId).equals(String.valueOf(StpUtil.getLoginId()))) {
            throw new NotPermissionException("order:access:other");
        }
    }

}