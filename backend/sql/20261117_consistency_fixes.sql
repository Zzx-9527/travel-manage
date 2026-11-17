-- 2026-11-17 第二批修复：AI 行程幂等与收藏一致性
-- 适用：已经初始化过 schema.sql 的现有数据库；只执行一次。
-- 全新初始化不需要执行本文件，schema.sql 已包含相同结构。

-- 1. AI 行程保存幂等：同用户 + 同内容指纹唯一，手动路线该列为 NULL 不受影响
ALTER TABLE `route`
  ADD COLUMN `plan_fingerprint` varchar(64) DEFAULT NULL COMMENT 'AI行程内容指纹，手动路线为NULL' AFTER `status`,
  ADD UNIQUE KEY `uk_route_user_plan_fingerprint` (`user_id`, `plan_fingerprint`);

-- 2. 清理历史重复收藏，只保留最早的一条
DELETE c1
FROM `collect` c1
JOIN `collect` c2
  ON c1.user_id = c2.user_id
 AND c1.target_type = c2.target_type
 AND c1.target_id = c2.target_id
 AND c1.id > c2.id;

-- 3. 同一用户对同一目标只能收藏一次，从数据库层阻止并发重复写入
ALTER TABLE `collect`
  ADD UNIQUE KEY `uk_collect_user_target` (`user_id`, `target_type`, `target_id`);

-- 4. 以 collect 表为准重建攻略收藏数，修复历史上只在前端本地加减造成的偏差
UPDATE `strategy` s
LEFT JOIN (
  SELECT target_id, COUNT(*) AS total
  FROM `collect`
  WHERE target_type = 1
  GROUP BY target_id
) c ON c.target_id = s.id
SET s.collect_count = COALESCE(c.total, 0);
