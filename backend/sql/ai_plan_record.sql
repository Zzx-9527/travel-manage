-- AI 行程生成记录表
-- 用途：管理端用量看板（次数、成功率、平均耗时、幻觉过滤数、热门目的地）+ 失败排查
-- 执行：在 travel_management 库执行一次即可；代码对"表不存在"做了容错，不会影响生成功能

CREATE TABLE IF NOT EXISTS `ai_plan_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       INT             NOT NULL                COMMENT '用户 id',
  `destination`   VARCHAR(64)     NOT NULL                COMMENT '目的地',
  `days`          TINYINT         NOT NULL                COMMENT '计划天数',
  `budget`        DECIMAL(10,2)   DEFAULT NULL            COMMENT '预算，可空',
  `preference`    VARCHAR(64)     DEFAULT NULL            COMMENT '偏好，可空',
  `extra`         VARCHAR(255)    DEFAULT NULL            COMMENT '补充说明，可空',
  `status`        TINYINT         NOT NULL                COMMENT '1=成功 0=失败',
  `error_msg`     VARCHAR(255)    DEFAULT NULL            COMMENT '失败原因',
  `scenic_count`  SMALLINT        NOT NULL DEFAULT 0      COMMENT '最终有效景点数',
  `dropped_spots` SMALLINT        NOT NULL DEFAULT 0      COMMENT '被白名单过滤的条目数',
  `latency_ms`    INT             NOT NULL DEFAULT 0      COMMENT '端到端耗时(毫秒)',
  `model`         VARCHAR(64)     DEFAULT NULL            COMMENT '模型名',
  `result_json`   MEDIUMTEXT                              COMMENT '生成结果快照，便于排查',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_destination` (`destination`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 行程生成记录';
