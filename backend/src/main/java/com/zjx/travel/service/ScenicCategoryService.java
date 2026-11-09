package com.zjx.travel.service;

import com.zjx.travel.domain.ScenicCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;

public interface ScenicCategoryService {
    ScenicCategoryEntity queryById(Integer id);

    Page<ScenicCategoryEntity> queryByPage(ScenicCategoryEntity scenicCategory, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(ScenicCategoryEntity scenicCategory);

    boolean update(ScenicCategoryEntity scenicCategory);

    boolean deleteById(Integer id);

}