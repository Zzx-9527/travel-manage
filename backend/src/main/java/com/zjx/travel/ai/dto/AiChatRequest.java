package com.zjx.travel.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 对话请求：只带当前这一句，历史上下文由服务端按 user_id 从库里取，
 * 避免前端伪造历史或把上下文无限放大。
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(max = 500, message = "单条消息不要超过 500 字")
    private String message;
}
