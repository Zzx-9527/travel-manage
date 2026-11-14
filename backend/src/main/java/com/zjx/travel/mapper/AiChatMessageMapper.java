package com.zjx.travel.mapper;

import com.zjx.travel.domain.AiChatMessageEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 对话消息（表 ai_chat_message）。
 */
public interface AiChatMessageMapper {

    int insert(AiChatMessageEntity message);

    /** 取某个用户最近 N 条消息，按时间倒序，调用方再反转成对话顺序 */
    List<AiChatMessageEntity> recentByUser(@Param("userId") Integer userId, @Param("limit") int limit);

    /** 清空某个用户的对话 */
    int deleteByUser(@Param("userId") Integer userId);
}
