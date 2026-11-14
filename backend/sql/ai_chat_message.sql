-- AI 对话消息表（悬浮窗聊天记录）
-- 作用：关闭窗口 / 刷新页面后再打开，能看到上次聊到哪、生成过哪几份行程
-- 执行：在 travel_management 库执行一次；代码对"表不存在"做了容错，不影响对话本身

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     INT             NOT NULL                COMMENT '用户 id',
  `role`        VARCHAR(16)     NOT NULL                COMMENT 'user / assistant',
  `msg_type`    VARCHAR(16)     NOT NULL DEFAULT 'text' COMMENT 'text=普通消息 plan=行程卡片',
  `content`     TEXT            NOT NULL                COMMENT '文本内容；plan 类型存标题',
  `payload`     MEDIUMTEXT                              COMMENT '结构化消息体，plan 类型存完整行程 JSON',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';
