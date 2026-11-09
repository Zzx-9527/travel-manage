-- 门票库存 schema + 历史订单迁移（MySQL 8.0，针对已核查的 travel_management）。
-- 请先执行同目录 *_precheck.sql 并确认 invalid_orders=0、无重复订单号、目标列/表不存在。
-- 已知基线：pay_status=0:4条（含已软删1条），=1:26条，=2:7条；order_no 已存在 UNIQUE uk_order_no；
-- scenic.price 与 scenic_orders.total_amount 已是 DECIMAL(10,2)，无需重复修改。
-- 重要：先停旧版后端（旧下单不扣库存），做数据库备份；迁移后须上线新版后端，不能继续运行旧版下单。
-- MySQL DDL 会隐式提交，整个脚本不能靠 ROLLBACK 撤销；失败时不要重跑，依备份和实际执行位置处理。
-- 本脚本只建表/加字段/映射历史订单，不预生成未来30天库存；新版服务上线前由生成任务补齐。
USE `travel_management`;

-- 景区长期覆盖值 NULL=使用应用系统全站默认值100；后台可配置0以上的覆盖值。
-- 今天票截止时间按 Asia/Shanghai 解释，到达该时刻不可再下单。
ALTER TABLE `scenic`
  ADD COLUMN `day_ticket_cutoff_time` TIME NOT NULL DEFAULT '17:00:00' COMMENT '当日门票停止下单时间（Asia/Shanghai）' AFTER `open_time`,
  ADD COLUMN `default_capacity` INT NULL DEFAULT NULL COMMENT '未来日期默认场地日容量，NULL表示全站默认100' AFTER `day_ticket_cutoff_time`,
  ADD COLUMN `default_adult_stock` INT NULL DEFAULT NULL COMMENT '未来日期成人票默认库存，NULL表示全站默认100' AFTER `default_capacity`;

CREATE TABLE `scenic_capacity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` INT NOT NULL COMMENT '景区ID',
  `play_date` DATE NOT NULL COMMENT '游玩日期（Asia/Shanghai）',
  `total` INT NOT NULL COMMENT '当日场地总容量',
  `remaining` INT NOT NULL COMMENT '当日剩余容量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scenic_capacity_day` (`scenic_id`, `play_date`),
  CONSTRAINT `fk_capacity_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`),
  CONSTRAINT `ck_capacity_valid` CHECK (`total` >= 0 AND `remaining` >= 0 AND `remaining` <= `total`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景区每日场地承载容量';

CREATE TABLE `scenic_stock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` INT NOT NULL COMMENT '景区ID',
  `play_date` DATE NOT NULL COMMENT '游玩日期（Asia/Shanghai）',
  `ticket_type_id` INT NOT NULL DEFAULT 1 COMMENT '票种ID，本期仅1=成人票',
  `total` INT NOT NULL COMMENT '票种总库存',
  `remaining` INT NOT NULL COMMENT '票种剩余库存',
  `version` INT NOT NULL DEFAULT 0 COMMENT '库存变更计数，当前不用作乐观锁条件',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scenic_stock_day_type` (`scenic_id`, `play_date`, `ticket_type_id`),
  CONSTRAINT `fk_stock_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`),
  CONSTRAINT `ck_stock_valid` CHECK (`total` >= 0 AND `remaining` >= 0 AND `remaining` <= `total`),
  CONSTRAINT `ck_stock_ticket_type` CHECK (`ticket_type_id` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景区票种每日库存';

-- 先将状态列设为可空，以确保所有旧订单都经过显式映射后才能设为非空。
-- stock_reserved 历史订单一律为0；新版下单必须显式插入1，避免旧单取消时错误回补。
ALTER TABLE `scenic_orders`
  ADD COLUMN `status` TINYINT NULL DEFAULT NULL COMMENT '订单状态：0待支付 1已支付 2已取消 3已完成' AFTER `pay_status`,
  ADD COLUMN `cancel_time` DATETIME NULL DEFAULT NULL COMMENT '取消时间（旧单为迁移时间）' AFTER `status`,
  ADD COLUMN `cancel_reason` VARCHAR(24) NULL DEFAULT NULL COMMENT 'MANUAL/TIMEOUT/LEGACY_UNPAID/LEGACY_REFUNDED' AFTER `cancel_time`,
  ADD COLUMN `unit_price` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '下单时单价快照；存量未知则为NULL' AFTER `total_amount`,
  ADD COLUMN `ticket_type_id` INT NOT NULL DEFAULT 1 COMMENT '票种ID，本期仅成人票1' AFTER `ticket_count`,
  ADD COLUMN `stock_reserved` TINYINT NOT NULL DEFAULT 0 COMMENT '新库存预留：1是，历史订单0否' AFTER `ticket_type_id`;

-- 已支付旧单保留；未支付旧单改取消、保持 del_mark 原值（未软删的仍可查看）；
-- 旧 pay_status=2（退款）映射为已取消，保留 pay_status=2 与取消原因，绝不回补新库存。
-- 旧单没有准确票价快照，不可用当前景区价伪造历史单价；unit_price 留 NULL。
UPDATE `scenic_orders`
SET `status` = CASE `pay_status` WHEN 1 THEN 1 ELSE 2 END,
    `cancel_time` = CASE WHEN `pay_status` IN (0,2) THEN NOW() ELSE NULL END,
    `cancel_reason` = CASE `pay_status`
      WHEN 0 THEN 'LEGACY_UNPAID'
      WHEN 2 THEN 'LEGACY_REFUNDED'
      ELSE NULL END
WHERE `status` IS NULL AND `pay_status` IN (0,1,2);

-- 预检查明确只存在0/1/2时才运行以下两条；若上方更新后仍有NULL，ALTER NOT NULL会报错，先停止调查。
ALTER TABLE `scenic_orders`
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付 1已支付 2已取消 3已完成';
CREATE INDEX `idx_order_timeout` ON `scenic_orders` (`status`, `stock_reserved`, `create_time`, `id`);
-- 不再 ADD UNIQUE(order_no)：实际表已有 UNIQUE KEY uk_order_no。

-- 只读验收：status=2 的历史订单不可逻辑删除、不能回补新库存；旧已软删1条仍保持原状。
SELECT `pay_status`, `status`, `cancel_reason`, `stock_reserved`, `del_mark`, COUNT(*) AS count_orders
FROM `scenic_orders` GROUP BY `pay_status`, `status`, `cancel_reason`, `stock_reserved`, `del_mark`
ORDER BY `pay_status`, `del_mark`;
SELECT COUNT(*) AS unmapped_orders FROM `scenic_orders` WHERE `status` IS NULL OR `stock_reserved` <> 0;
SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('scenic_capacity','scenic_stock');
