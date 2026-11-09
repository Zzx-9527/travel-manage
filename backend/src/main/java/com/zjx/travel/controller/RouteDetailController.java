package com.zjx.travel.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.zjx.travel.domain.RouteDetailEntity;
import com.zjx.travel.service.RouteDetailService;
import com.zjx.travel.util.*;

import javax.annotation.Resource;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@RestController
@RequestMapping("routeDetail")
public class RouteDetailController {
    @Resource
    private RouteDetailService routeDetailService;

    /**
     * routeId 作为独立的查询参数接收（Integer），
     * 防止它被 Spring 绑定到实体上的 routeId(RouteEntity) 对象而导致类型转换 400
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("routeId");
    }

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        RouteDetailEntity routeDetail = this.routeDetailService.queryById(id);

        if(routeDetail == null) {
            return returnVO;
        }

        returnVO = ReturnVO.getSuccessDataReturnVO(routeDetail);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(RouteDetailEntity routeDetail, Integer routeId, Integer page, Integer size, String orderCol, String orderDirect) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<RouteDetailEntity> pageVO =
            this.routeDetailService.queryByPage(
                routeDetail,
                routeId,
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
    @SaCheckRole("admin")
    public ReturnVO add(@RequestBody RouteDetailEntity routeDetail) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        //入库
        if(!this.routeDetailService.insert(routeDetail)){
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
    public ReturnVO edit(@RequestBody RouteDetailEntity routeDetail) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        //修改
        if(!this.routeDetailService.update(routeDetail)){
            //修改失败
            return returnVO;
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
        if(!this.routeDetailService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

}