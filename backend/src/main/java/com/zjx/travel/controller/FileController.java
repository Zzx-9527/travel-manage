package com.zjx.travel.controller;

import com.zjx.travel.service.FileStorageService;
import com.zjx.travel.util.ReturnVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件上传入口。
 *
 * <p>权限：只要求登录（Sa-Token 拦截器统一校验），普通用户可以上传头像/攻略配图，
 * 不需要管理员角色。
 *
 * <p>返回结构保持简洁：
 * <pre>{@code
 * { "code": 1, "msg": "上传成功",
 *   "content": { "url": "/travel/uploads/2026/10/31/xxx.png", "name": "原文件名.png", "size": 12345 } }
 * }</pre>
 */
@RestController
@RequestMapping("file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Resource
    private FileStorageService fileStorageService;

    /**
     * 上传单张图片（multipart 字段名固定为 {@code file}）。
     */
    @PostMapping("upload")
    public ReturnVO upload(@RequestParam("file") MultipartFile file) {
        try {
            FileStorageService.StoredFile stored = fileStorageService.store(file);
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("url", stored.getUrl());
            content.put("name", stored.getOriginalName());
            content.put("size", stored.getSize());
            ReturnVO returnVO = ReturnVO.getSuccessDataReturnVO(content);
            returnVO.setMsg("上传成功");
            return returnVO;
        } catch (IllegalArgumentException e) {
            // 文件为空、类型不支持、路径非法等，属于用户输入问题，直接返回原因
            return new ReturnVO(0, e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return new ReturnVO(0, "上传失败，请稍后重试");
        }
    }
}
