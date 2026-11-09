package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.ScenicOrdersEntity;
import com.zjx.travel.mapper.ScenicOrdersMapper;
import com.zjx.travel.service.ScenicOrdersService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ScenicOrdersServiceImpl implements ScenicOrdersService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private ScenicOrdersMapper scenicOrdersMapper;

    public ScenicOrdersEntity queryById(Integer id){
        return this.scenicOrdersMapper.queryById(id);
    }

    public Page<ScenicOrdersEntity> queryByPage(ScenicOrdersEntity scenicOrders, Integer page, Integer size, String orderCol, String orderDirect){

        if(page == null || page <= 0){
            page = PAGE_DEFAULT;
        }
        if(size == null || size <= 0){
            size = SIZE_DEFAULT;
        }

        Sort sort = null;
        if(orderCol != null) {
            Sort.Order order = new Sort.Order(("DESC".equals(orderDirect) ? Sort.Direction.DESC : Sort.Direction.ASC), orderCol);
            sort = Sort.by(order);
        }

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if(sort != null){
            pageRequest = PageRequest.of(page - 1, size, sort);
        }

        long total = this.scenicOrdersMapper.count(scenicOrders);

        return new PageImpl<>(this.scenicOrdersMapper.queryAllByLimit(scenicOrders, pageRequest), pageRequest, total);
    }

    public boolean insert(ScenicOrdersEntity scenicOrders){
        //订单号由后端生成：ORDER + 年月日时分秒毫秒 + 3位随机数，如 ORDER20260906143052588123
        String orderNo = "ORDER" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + (int)(Math.random() * 900 + 100);
        scenicOrders.setOrderNo(orderNo);
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补下单时间
        if (scenicOrders.getCreateTime() == null) {
            scenicOrders.setCreateTime(new Date());
        }
        return this.scenicOrdersMapper.insert(scenicOrders) > 0;
    }

    public boolean update(ScenicOrdersEntity scenicOrders){
        return this.scenicOrdersMapper.update(scenicOrders) > 0;
    }

    public boolean deleteById(Integer id){
        return this.scenicOrdersMapper.deleteById(id) > 0;
    }

}