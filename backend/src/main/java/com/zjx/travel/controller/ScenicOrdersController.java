package com.zjx.travel.controller;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.service.ScenicOrdersService;
import com.zjx.travel.service.ScenicService;
import com.zjx.travel.util.*;

import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("scenicOrders")
public class ScenicOrdersController {
    @Resource
    private ScenicOrdersService scenicOrdersService;
    @Resource
    private ScenicService scenicService;
    @Resource
    private MessageNotifier messageNotifier;
    @Resource
    private LockUtil lockUtil;

    /** 查库解析景区名称用于消息文案（提交体里 scenicId 可能只带 id，查不到时用兜底文案） */
    private String resolveScenicName(ScenicOrdersEntity scenicOrders) {
        if (scenicOrders.getScenicId() != null && scenicOrders.getScenicId().getId() != null) {
            ScenicEntity scenic = this.scenicService.queryById(scenicOrders.getScenicId().getId());
            if (scenic != null && scenic.getName() != null) {
                return scenic.getName();
            }
        }
        return "景区";
    }

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

    @PostMapping("add")
    public ReturnVO add(@RequestBody ScenicOrdersEntity scenicOrders) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 防重复提交：同一用户 5 秒内只允许提交一次。
        // 用 RBucket.trySet（SETNX 语义）而不是"先查再插"，并发下不会两个请求都认为自己是第一次。
        Integer orderUserId = scenicOrders.getUserId() == null ? null : scenicOrders.getUserId().getId();
        if (orderUserId != null
                && !lockUtil.trySetOnce(RedisKeys.orderSubmit(orderUserId), Duration.ofSeconds(5))) {
            returnVO.setMsg("请勿重复提交，请 5 秒后再试");
            return returnVO;
        }

        //入库
        if(!this.scenicOrdersService.insert(scenicOrders)){
            //入库失败
            return returnVO;
        }

        // 下单成功通知（订单号由 insert 生成并回写；景区名查库解析，不依赖前端提交体）
        if (scenicOrders.getUserId() != null && scenicOrders.getUserId().getId() != null) {
            this.messageNotifier.sendOrder(scenicOrders.getUserId().getId(), "下单成功",
                    this.resolveScenicName(scenicOrders) + "门票生成订单 " + scenicOrders.getOrderNo());
        }

        //入库成功
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");

        return returnVO;
    }

    @PostMapping("edit")
    public ReturnVO edit(@RequestBody ScenicOrdersEntity scenicOrders) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 越权防护：普通用户不能修改别人的订单
        if (scenicOrders.getId() != null) {
            requireSelfOrAdmin(this.scenicOrdersService.queryById(scenicOrders.getId()));
        }

        // 检测支付状态变化：0(未支付) → 1(已支付) 才通知，避免重复支付/免费单重复打扰用户
        boolean payChanged = false;
        if (scenicOrders.getId() != null && scenicOrders.getPayStatus() != null && scenicOrders.getPayStatus() == 1) {
            ScenicOrdersEntity old = this.scenicOrdersService.queryById(scenicOrders.getId());
            payChanged = old != null && old.getPayStatus() != null && old.getPayStatus() == 0;
        }

        //修改
        if(!this.scenicOrdersService.update(scenicOrders)){
            //修改失败
            return returnVO;
        }

        // 支付成功通知（景区名同样查库解析）
        if (payChanged && scenicOrders.getUserId() != null && scenicOrders.getUserId().getId() != null) {
            this.messageNotifier.sendOrder(scenicOrders.getUserId().getId(), "订单支付成功",
                    "您预订的「" + this.resolveScenicName(scenicOrders)
                            + "」门票订单已支付成功，凭订单号入园。");
        }

        //修改成功
        returnVO.setCode(1);
        returnVO.setMsg("修改成功");

        return returnVO;
    }

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