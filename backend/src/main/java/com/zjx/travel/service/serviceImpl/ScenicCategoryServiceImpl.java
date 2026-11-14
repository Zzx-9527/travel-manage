package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.ScenicCategoryEntity;
import com.zjx.travel.mapper.ScenicCategoryMapper;
import com.zjx.travel.service.ScenicCategoryService;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ScenicCategoryServiceImpl implements ScenicCategoryService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private ScenicCategoryMapper scenicCategoryMapper;

    public ScenicCategoryEntity queryById(Integer id){
        return this.scenicCategoryMapper.queryById(id);
    }

    public Page<ScenicCategoryEntity> queryByPage(ScenicCategoryEntity scenicCategory, Integer page, Integer size, String orderCol, String orderDirect){

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

        long total = this.scenicCategoryMapper.count(scenicCategory);

        return new PageImpl<>(this.scenicCategoryMapper.queryAllByLimit(scenicCategory, pageRequest), pageRequest, total);
    }

    public boolean insert(ScenicCategoryEntity scenicCategory){
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        Date now = new Date();
        if (scenicCategory.getCreateTime() == null) {
            scenicCategory.setCreateTime(now);
        }
        if (scenicCategory.getUpdateTime() == null) {
            scenicCategory.setUpdateTime(now);
        }
        return this.scenicCategoryMapper.insert(scenicCategory) > 0;
    }

    public boolean update(ScenicCategoryEntity scenicCategory){
        return this.scenicCategoryMapper.update(scenicCategory) > 0;
    }

    public boolean deleteById(Integer id){
        return this.scenicCategoryMapper.deleteById(id) > 0;
    }

}