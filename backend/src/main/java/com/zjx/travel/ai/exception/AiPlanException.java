package com.zjx.travel.ai.exception;

/**
 * AI 行程规划的业务异常。
 *
 * <p>消息会原样返回给前端（"请先填写目的地""AI 服务暂时不可用"这类可读提示），
 * 因此这里只放"用户看得懂"的话；堆栈和上游原始错误由 Service 记录到日志，
 * 不往外抛，避免泄露内部细节。
 */
public class AiPlanException extends RuntimeException {

    public AiPlanException(String message) {
        super(message);
    }
}
