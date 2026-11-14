package com.zjx.travel.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.service.FileStorageService;
import com.zjx.travel.service.ScenicService;
import com.zjx.travel.util.*;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import com.zjx.travel.service.ScenicStockService;

@RestController
@RequestMapping("scenic")
public class ScenicController {
    @Resource
    private ScenicService scenicService;

    @Resource
    private FileStorageService fileStorageService;
    @Resource
    private ScenicStockService scenicStockService;

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

        if (scenic.getDefaultCapacity() != null && scenic.getDefaultCapacity() < 0
                || scenic.getDefaultAdultStock() != null && scenic.getDefaultAdultStock() < 0) {
            return new ReturnVO("长期默认库存不能为负数");
        }
        if (scenic.getDayTicketCutoffTime() != null && !scenic.getDayTicketCutoffTime().matches("^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$")) {
            return new ReturnVO("截止时间格式无效");
        }

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

    @GetMapping("stock")
    public ReturnVO stock(@RequestParam Integer scenicId, @RequestParam String playDate) {
        try {
            ScenicStockService.ScenicStockView view = scenicStockService.query(scenicId, LocalDate.parse(playDate));
            if (view == null) return new ReturnVO("暂未开放");
            return new ReturnVO(1, view.remaining != null && view.remaining == 0 ? "已售罄" : "找到数据！", view);
        } catch (DateTimeParseException e) { return new ReturnVO("日期格式无效"); }
    }

    /** 首次部署或补录时手动补齐未来 30 天库存；每日 0:05 任务只补缺失记录 */
    @PostMapping("stock/generate")
    @SaCheckRole("admin")
    public ReturnVO generateStock() {
        scenicStockService.generateNext30Days();
        return new ReturnVO(1, "库存已补齐");
    }

    @PostMapping("stock/adjust")
    @SaCheckRole("admin")
    public ReturnVO adjustStock(@RequestParam Integer scenicId, @RequestParam String playDate,
                                @RequestParam Integer capacity, @RequestParam Integer adultStock) {
        try {
            scenicStockService.adjust(scenicId, LocalDate.parse(playDate), capacity, adultStock);
            return ReturnVO.getSuccessDataReturnVO();
        } catch (DateTimeParseException e) { return new ReturnVO("日期格式无效"); }
        catch (IllegalArgumentException | IllegalStateException e) { return new ReturnVO(e.getMessage()); }
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