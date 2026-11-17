package com.zjx.travel.controller;

import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.dto.TravelUserRegisterDTO;
import com.zjx.travel.service.TravelUserService;
import com.zjx.travel.util.ReturnVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 匿名注册入口。
 *
 * <p>注册与后台“新增用户”拆成两个接口：本接口只接收公开资料，
 * 服务层还会再次强制角色为普通用户，双重避免匿名提权。
 */
@RestController
@RequestMapping("travelUser")
public class TravelUserRegisterController {

    @Resource
    private TravelUserService travelUserService;

    @PostMapping("register")
    public ReturnVO register(@Valid @RequestBody TravelUserRegisterDTO dto) {
        TravelUserEntity user = new TravelUserEntity();
        user.setAccount(dto.getAccount().trim());
        user.setPassword(dto.getPassword());
        user.setNickname(trimToNull(dto.getNickname()));
        user.setPhone(trimToNull(dto.getPhone()));
        user.setAvatar(trimToNull(dto.getAvatar()));

        if (!this.travelUserService.register(user)) {
            return new ReturnVO("注册失败，账号已存在");
        }

        ReturnVO returnVO = ReturnVO.getSuccessDataReturnVO();
        returnVO.setMsg("注册成功");
        return returnVO;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
