package com.zjx.travel.util;

import com.zjx.travel.domain.MessageEntity;
import com.zjx.travel.domain.TravelUserEntity;
import com.zjx.travel.service.MessageService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class MessageNotifier {

    public static final int TYPE_SYSTEM = 0;
    public static final int TYPE_ORDER = 1;
    public static final int TYPE_AUDIT = 2;

    @Resource
    private MessageService messageService;

    /** 发送一条消息（receiverUserId 为接收人用户ID） */
    public void send(Integer receiverUserId, int type, String title, String content) {
        try {
            if (receiverUserId == null) return;
            MessageEntity m = new MessageEntity();
            TravelUserEntity u = new TravelUserEntity();
            u.setId(receiverUserId);
            m.setUserId(u);
            m.setType(type);
            m.setTitle(title);
            m.setContent(content);
            messageService.insert(m);   // insert 已自动填 isRead=0、createTime/updateTime
        } catch (Exception e) {
            e.printStackTrace();        // 消息失败只记日志，绝不影响主业务
        }
    }

    /** 审核结果通知（kind：攻略/路线/评论） */
    public void sendAuditResult(Integer receiverUserId, String kind, String name, boolean passed) {
        String content = passed
                ? "你发布的" + kind + "《" + name + "》已通过审核"
                : "你发布的" + kind + "《" + name + "》未通过审核，请修改后重新提交";
        send(receiverUserId, TYPE_AUDIT, kind + "审核结果", content);
    }

    /** 订单事件通知 */
    public void sendOrder(Integer receiverUserId, String title, String content) {
        send(receiverUserId, TYPE_ORDER, title, content);
    }
}
