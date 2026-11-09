-- 仅查询，不修改数据。先运行并保存结果；目标库：travel_management (MySQL 8.0 / InnoDB)。
USE `travel_management`;
SELECT VERSION() AS mysql_version, DATABASE() AS db_name, NOW() AS db_now,
       @@session.time_zone AS session_time_zone;
SHOW CREATE TABLE `scenic`;
SHOW CREATE TABLE `scenic_orders`;
SHOW INDEX FROM `scenic_orders` WHERE Key_name = 'uk_order_no';
SELECT `pay_status`, `del_mark`, COUNT(*) AS orders_count
FROM `scenic_orders` GROUP BY `pay_status`, `del_mark` ORDER BY `pay_status`, `del_mark`;
SELECT `order_no`, COUNT(*) AS duplicate_count FROM `scenic_orders`
GROUP BY `order_no` HAVING COUNT(*) > 1;
SELECT COUNT(*) AS invalid_orders FROM `scenic_orders`
WHERE `pay_status` IS NULL OR `pay_status` NOT IN (0,1,2)
   OR `ticket_count` <= 0 OR `total_amount` < 0;
SELECT `status`, `del_mark`, COUNT(*) AS scenic_count
FROM `scenic` GROUP BY `status`, `del_mark`;
SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND (
  (TABLE_NAME='scenic' AND COLUMN_NAME IN ('day_ticket_cutoff_time','default_capacity','default_adult_stock'))
  OR (TABLE_NAME='scenic_orders' AND COLUMN_NAME IN ('status','cancel_time','cancel_reason','unit_price','ticket_type_id','stock_reserved'))
) ORDER BY TABLE_NAME, COLUMN_NAME;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('scenic_capacity','scenic_stock');
-- 核对要求：invalid_orders=0，无重复订单号；以上新列及两张新表尚不存在。
-- 发现异常先停止并反馈结果；不要重复执行后续迁移。
