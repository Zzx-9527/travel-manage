package com.zjx.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjx.travel.domain.MessageEntity;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface MessageMapper extends BaseMapper<MessageEntity> {

    MessageEntity queryById(Integer id);

    List<MessageEntity> queryAllByLimit(@Param("message") MessageEntity message, @Param("pageable") Pageable pageable);

    long count(@Param("message") MessageEntity message);

    int insert(MessageEntity message);

    int update(MessageEntity message);

    int deleteById(Integer id);

    /**
     * 标记已读：按 id 或 ids 批量，只能操作自己的消息（userId 必传）
     */
    int markRead(@Param("id") Integer id, @Param("ids") List<Integer> ids, @Param("userId") Integer userId);

    /**
     * 未读消息数量
     */
    long unreadCount(@Param("userId") Integer userId);
}
