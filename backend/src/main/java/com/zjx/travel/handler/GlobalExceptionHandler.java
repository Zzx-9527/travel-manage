package com.zjx.travel.handler;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.validation.FieldError;

import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolationException;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.zjx.travel.util.ReturnVO;
import com.zjx.travel.exception.OrderBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把 Sa-Token 的鉴权异常翻译成前端约定的 JSON + HTTP 状态码。
 *
 * <p>约定（详见 {@code docs/前端接口对接说明.md} 第 8 节）：
 * <ul>
 *   <li>401：未登录 / token 失效 / 被踢下线 / 在别处登录；</li>
 *   <li>403：已登录但角色不足。</li>
 * </ul>
 * 业务失败仍然是 HTTP 200 + {@code code=0}，不受这里影响。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderBusinessException.class)
    public ReturnVO handleOrderBusiness(OrderBusinessException e) { return new ReturnVO(0, e.getMessage()); }

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ReturnVO handleNotLogin(NotLoginException e) {
        String message = loginMessage(e.getType());
        log.debug("未登录访问被拦截：type={}, message={}", e.getType(), message);
        return new ReturnVO(0, message);
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ReturnVO handleNoPermission(Exception e) {
        log.warn("权限不足：{}", e.getMessage());
        return new ReturnVO(0, "无权限访问");
    }

    /**
     * 把 Sa-Token 的异常类型翻译成前端可读的文案。
     * 前端只需要按 HTTP 401 统一跳登录，文案用于 toast 提示。
     */
    private String loginMessage(String type) {
        switch (type) {
            case NotLoginException.NOT_TOKEN:
                return "未登录，请先登录";
            case NotLoginException.INVALID_TOKEN:
                return "token 无效或已过期";
            case NotLoginException.TOKEN_TIMEOUT:
                return "登录已过期，请重新登录";
            case NotLoginException.BE_REPLACED:
                return "账号在别处登录";
            case NotLoginException.KICK_OUT:
                return "账号被强制下线";
            case NotLoginException.TOKEN_FREEZE:
                return "账号已被封禁";
            case NotLoginException.NO_PREFIX:
                return "token 缺少 Bearer 前缀";
            default:
                return "未登录，请先登录";
        }
    }

    /**
     * 请求体参数校验失败（@Valid 触发）。
     * 与业务失败保持一致：HTTP 200 + code=0，前端直接展示 msg。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ReturnVO handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isEmpty())
                .findFirst()
                .orElse("请求参数不合法");
        log.debug("参数校验失败：{}", message);
        return new ReturnVO(0, message);
    }

    /** 方法参数上的约束校验失败（如 @RequestParam 加注解时） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ReturnVO handleConstraint(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("请求参数不合法");
        log.debug("参数校验失败：{}", message);
        return new ReturnVO(0, message);
    }

    /** 请求体不是合法 JSON 或字段类型不匹配 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ReturnVO handleUnreadable(HttpMessageNotReadableException e) {
        log.debug("请求体解析失败：{}", e.getMessage());
        return new ReturnVO(0, "请求参数格式不正确");
    }
}
