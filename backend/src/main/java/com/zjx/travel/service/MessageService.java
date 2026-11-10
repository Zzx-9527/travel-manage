package com.zjx.travel.service;

import com.zjx.travel.domain.MessageEntity;
import org.springframework.data.domain.Page;

import java.util.*;

public interface MessageService {
    MessageEntity queryById(Integer id);

    Page<MessageEntity> queryByPage(MessageEntity message, Integer page, Integer size, String orderCol, String orderDirect);

    boolean insert(MessageEntity message);

    boolean update(MessageEntity message);

    boolean deleteById(Integer id);

    // 标记已读：返回成功标记条数（-1 表示参数错误）；只能标记自己的消息
    int markRead(Integer id, List<Integer> ids, Integer userId);

    // 将当前用户全部未读消息标记为已读
    int markAllRead(Integer userId);

    // 未读消息数量
    long unreadCount(Integer userId);
}
