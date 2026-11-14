package com.zjx.travel.domain;

import lombok.Data;

/**
 * AI 对话消息（表 ai_chat_message）。
 *
 * <p>一个用户只有一条会话线：不建 conversation 表，也不分多会话。
 * 产品上这是"随身助手"，用户不会同时开两个话题；真要做多会话再拆表。
 */
@Data
public class AiChatMessageEntity {

    private Long id;

    private Integer userId;

    /** user / assistant */
    private String role;

    /** text=普通消息 plan=行程卡片 */
    private String msgType;

    /** 文本内容；plan 类型存标题 */
    private String content;

    /** 结构化消息体，plan 类型存完整行程 JSON */
    private String payload;

    private String createTime;
}
