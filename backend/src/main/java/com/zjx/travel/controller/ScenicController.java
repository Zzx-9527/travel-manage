package com.zjx.travel.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.service.FileStorageService;
import com.zjx.travel.service.ScenicService;
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
@RequestMapping("scenic")
public class ScenicController {
    @Resource
    private ScenicService scenicService;

    @Resource
    private FileStorageService fileStorageService;

    @GetMapping("queryById/{id}")
    public ReturnVO queryById(@PathVariable("id") Integer id) {
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();
        ScenicEntity scenic = this.scenicService.queryById(id);

        if(scenic == null) {
            return returnVO;
        }

        // 只有查到景点才算一次浏览；计数写 Redis，由定时任务批量回写数据库
        this.scenicService.recordView(id);

        returnVO = ReturnVO.getSuccessDataReturnVO(scenic);

        return returnVO;
    }

    @GetMapping("queryByPage")
    public ReturnVO queryByPage(ScenicEntity scenic, Integer page, Integer size, String orderCol, String orderDirect) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        Page<ScenicEntity> pageVO = 
            this.scenicService.queryByPage(
                scenic, 
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
    public ReturnVO add(@RequestBody ScenicEntity scenic) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        //入库
        if(!this.scenicService.insert(scenic)){
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
    public ReturnVO edit(@RequestBody ScenicEntity scenic) {

        //定义失败的返回对象
        ReturnVO returnVO = ReturnVO.getNodataFoundReturnVO();

        // 单图覆盖：先取旧记录，更新成功后再删旧图
        ScenicEntity old = scenic.getId() == null ? null : this.scenicService.queryById(scenic.getId());

        //修改
        if(!this.scenicService.update(scenic)){
            //修改失败
            return returnVO;
        }

        if (old != null && scenic.getCover() != null && !scenic.getCover().equals(old.getCover())) {
            this.fileStorageService.deleteByUrl(old.getCover());
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
        if(!this.scenicService.deleteById(id)){
            //删除失败
            return returnVO;
        }

        returnVO.setCode(1);
        returnVO.setMsg("删除成功");

        return returnVO;
    }

}