package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.MessageEntity;
import com.zjx.travel.mapper.MessageMapper;
import com.zjx.travel.service.MessageService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

@Service
public class MessageServiceImpl implements MessageService {
    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    @Resource
    private MessageMapper messageMapper;

    public MessageEntity queryById(Integer id){
        return this.messageMapper.queryById(id);
    }

    public Page<MessageEntity> queryByPage(MessageEntity message, Integer page, Integer size, String orderCol, String orderDirect){

        if(page == null || page <= 0){
            page = PAGE_DEFAULT;
        }
        if(size == null || size <= 0){
            size = SIZE_DEFAULT;
        }

        Sort sort = null;
        if(orderCol != null) {
            Sort.Order order = new Sort.Order(("DESC".equals(orderDirect) ? Sort.Direction.DESC : Sort.Direction.ASC), orderCol);
            sort = Sort.by(order);
        }

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if(sort != null){
            pageRequest = PageRequest.of(page - 1, size, sort);
        }

        long total = this.messageMapper.count(message);

        return new PageImpl<>(this.messageMapper.queryAllByLimit(message, pageRequest), pageRequest, total);
    }

    public boolean insert(MessageEntity message){
        if (message.getIsRead() == null) {
            message.setIsRead(0);
        }
        if (message.getType() == null) {
            message.setType(0);
        }
        // 自定义 XML 插入不会触发 MyBatis-Plus 自动填充，这里手动赋值
        if (message.getCreateTime() == null) {
            message.setCreateTime(new Date());
        }
        if (message.getUpdateTime() == null) {
            message.setUpdateTime(new Date());
        }
        return this.messageMapper.insert(message) > 0;
    }

    public boolean update(MessageEntity message){
        if (message.getUpdateTime() == null) {
            message.setUpdateTime(new Date());
        }
        return this.messageMapper.update(message) > 0;
    }

    public boolean deleteById(Integer id){
        return this.messageMapper.deleteById(id) > 0;
    }

    public int markRead(Integer id, List<Integer> ids, Integer userId){
        if (id == null && (ids == null || ids.isEmpty())) {
            return -1;
        }
        if (userId == null) {
            return -1;
        }
        return this.messageMapper.markRead(id, ids, userId);
    }

    public long unreadCount(Integer userId){
        if (userId == null) {
            return 0;
        }
        return this.messageMapper.unreadCount(userId);
    }

}
