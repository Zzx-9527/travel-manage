package com.zjx.travel.controller;

import com.zjx.travel.domain.CollectEntity;
import com.zjx.travel.service.CollectService;
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
@RequestMapping("collect")
public class CollectController {
    @Resource
    private CollectService collectService;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        CollectEntity collect = this.collectService.queryById(id);

        if(collect == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(collect.getUserId() == null ? null : collect.getUserId().getId());

        returnVO = ReturnVO.getSuccessDataReturnVO(collect);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(CollectEntity collect, Integer page, Integer size, String orderCol, String orderDirect) {

        // 越权防护：非管理员只能查自己的收藏，忽略前端传来的 collectorId / userId
        if (!SecurityUtil.isAdmin()) {
            collect.setCollectorId(SecurityUtil.currentUserId());
        }

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<CollectEntity> pageVO = 
            this.collectService.queryByPage(
                collect, 
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
    public ReturnVO add(@RequestBody CollectEntity collect) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 收藏人一律取当前登录人，忽略前端传来的 userId
        collect.setUserId(SecurityUtil.currentUserRef());

        //入库
        if(!this.collectService.insert(collect)){
            //入库失败
            return returnVO;
        }

        //入库成功
        returnVO.setCode(1);
        returnVO.setMsg("添加成功");

        return returnVO;
    }

    @PostMapping("edit")
    public ReturnVO edit(@RequestBody CollectEntity collect) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 归属校验：非管理员只能改自己的收藏记录
        if (collect.getId() != null) {
            CollectEntity old = this.collectService.queryById(collect.getId());
            if (old == null) {
                return returnVO;
            }
            SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());
        }

        //修改
        if(!this.collectService.update(collect)){
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

        CollectEntity old = this.collectService.queryById(id);
        if (old == null) {
            return returnVO;
        }
        SecurityUtil.requireSelfOrAdmin(old.getUserId() == null ? null : old.getUserId().getId());

        if(!this.collectService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

    /**
     * 判断当前用户是否已收藏目标
     * GET /travel/collect/checkCollected?targetType=2&targetId=9
     * targetType: 1攻略 2景点 3路线
     */
    @GetMapping("checkCollected")
    public ReturnVO checkCollected(Integer targetType, Integer targetId) {
        // 收藏人取当前登录人，忽略前端传的 userId
        boolean collected = this.collectService.checkCollected(
                SecurityUtil.currentUserId(), targetType, targetId);
        return ReturnVO.getSuccessDataReturnVO(collected);
    }

    /**
     * 按 用户 + 目标类型 + 目标ID 取消收藏
     * POST /travel/collect/deleteByUserAndTarget
     * body: { "targetType": 2, "targetId": 9 }
     */
    @PostMapping("deleteByUserAndTarget")
    public ReturnVO deleteByUserAndTarget(@RequestBody CollectEntity collect) {
        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 收藏人取当前登录人，忽略前端传的 userId
        Integer userId = SecurityUtil.currentUserId();
        if(!this.collectService.deleteByUserAndTarget(userId, collect.getTargetType(), collect.getTargetId())){
            returnVO.setMsg("取消收藏失败");
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("取消成功");

        return returnVO;
    }

}