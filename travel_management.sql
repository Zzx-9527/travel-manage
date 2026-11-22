/*
 Navicat Premium Data Transfer

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : travel_management

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 22/11/2026 17:59:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'user / assistant',
  `msg_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'text' COMMENT 'text / plan',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文本内容；plan 存标题',
  `payload` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'plan 类型存完整行程 JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_plan_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_plan_record`;
CREATE TABLE `ai_plan_record`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户 id',
  `destination` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目的地',
  `days` tinyint NOT NULL COMMENT '计划天数',
  `budget` decimal(10, 2) NULL DEFAULT NULL COMMENT '预算，可空',
  `preference` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '偏好，可空',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '补充说明，可空',
  `status` tinyint NOT NULL COMMENT '1=成功 0=失败',
  `error_msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `scenic_count` smallint NOT NULL DEFAULT 0 COMMENT '最终有效景点数',
  `dropped_spots` smallint NOT NULL DEFAULT 0 COMMENT '被白名单过滤的条目数',
  `latency_ms` int NOT NULL DEFAULT 0 COMMENT '端到端耗时(毫秒)',
  `model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名',
  `result_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成结果快照，便于排查',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_destination`(`destination` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 行程生成记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for collect
-- ----------------------------
DROP TABLE IF EXISTS `collect`;
CREATE TABLE `collect`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '收藏ID，自增主键',
  `user_id` int NOT NULL COMMENT '用户ID，外键关联travel_user.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `target_type` int NOT NULL DEFAULT 1 COMMENT '收藏目标类型 1攻略 2景点 3路线',
  `target_id` int NOT NULL COMMENT '收藏目标ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_collect_user_target`(`user_id` ASC, `target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_collect_user` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` int NULL DEFAULT NULL COMMENT '接收人ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
  `type` int NULL DEFAULT 0 COMMENT '消息类型 0系统 1订单 2审核',
  `is_read` int NULL DEFAULT 0 COMMENT '是否已读 0未读 1已读',
  `del_mark` int NULL DEFAULT 1 COMMENT '逻辑删除 1正常 0删除',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for route
-- ----------------------------
DROP TABLE IF EXISTS `route`;
CREATE TABLE `route`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '路线ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路线名称',
  `day_count` int NOT NULL DEFAULT 1 COMMENT '游玩天数',
  `start_city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出发城市',
  `traffic` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交通方式',
  `budget` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '预算',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '路线说明',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态 0待审核 1已发布2已驳回',
  `plan_fingerprint` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AI行程内容指纹，手动路线为NULL',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_route_user_plan_fingerprint`(`user_id` ASC, `plan_fingerprint` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_route_user` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '旅游路线表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for route_detail
-- ----------------------------
DROP TABLE IF EXISTS `route_detail`;
CREATE TABLE `route_detail`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '详情ID',
  `route_id` int NOT NULL COMMENT '路线ID',
  `scenic_id` int NOT NULL COMMENT '景点ID',
  `day_no` int NOT NULL COMMENT '第几天',
  `sort` int NOT NULL COMMENT '游玩顺序',
  `stay_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '建议停留时长',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_route_id`(`route_id` ASC) USING BTREE,
  INDEX `idx_scenic_id`(`scenic_id` ASC) USING BTREE,
  CONSTRAINT `route_detail_ibfk_1` FOREIGN KEY (`route_id`) REFERENCES `route` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `route_detail_ibfk_2` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '路线景点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scenic
-- ----------------------------
DROP TABLE IF EXISTS `scenic`;
CREATE TABLE `scenic`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '景点ID',
  `category_id` int NOT NULL COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '景点名称',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图',
  `images` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片集(逗号分隔)',
  `price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '门票价格',
  `open_time` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开放时间',
  `day_ticket_cutoff_time` time NOT NULL DEFAULT '17:00:00' COMMENT '当日门票停止下单时间（Asia/Shanghai）',
  `default_capacity` int NULL DEFAULT NULL COMMENT '未来日期默认场地日容量，NULL表示全站默认100',
  `default_adult_stock` int NULL DEFAULT NULL COMMENT '未来日期成人票默认库存，NULL表示全站默认100',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '详细地址',
  `longitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 7) NULL DEFAULT NULL COMMENT '纬度',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '景点介绍',
  `score` decimal(2, 1) NOT NULL DEFAULT 5.0 COMMENT '综合评分',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0下架 1上架',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  CONSTRAINT `scenic_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `scenic_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scenic_capacity
-- ----------------------------
DROP TABLE IF EXISTS `scenic_capacity`;
CREATE TABLE `scenic_capacity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` int NOT NULL COMMENT '景区ID',
  `play_date` date NOT NULL COMMENT '游玩日期（Asia/Shanghai）',
  `total` int NOT NULL COMMENT '当日场地总容量',
  `remaining` int NOT NULL COMMENT '当日剩余容量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scenic_capacity_day`(`scenic_id` ASC, `play_date` ASC) USING BTREE,
  CONSTRAINT `fk_capacity_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_capacity_valid` CHECK ((`total` >= 0) and (`remaining` >= 0) and (`remaining` <= `total`))
) ENGINE = InnoDB AUTO_INCREMENT = 2632 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景区每日场地承载容量' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scenic_category
-- ----------------------------
DROP TABLE IF EXISTS `scenic_category`;
CREATE TABLE `scenic_category`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景点分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scenic_orders
-- ----------------------------
DROP TABLE IF EXISTS `scenic_orders`;
CREATE TABLE `scenic_orders`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单ID，自增主键',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号，唯一',
  `user_id` int NOT NULL COMMENT '用户ID，外键关联travel_user.id',
  `scenic_id` int NOT NULL COMMENT '景点ID，外键关联scenic.id',
  `contact_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `play_date` date NOT NULL COMMENT '游玩日期',
  `ticket_count` int NOT NULL COMMENT '购票人数（张数）',
  `ticket_type_id` int NOT NULL DEFAULT 1 COMMENT '票种ID，本期仅成人票1',
  `stock_reserved` tinyint NOT NULL DEFAULT 0 COMMENT '新库存预留：1是，历史订单0否',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单总金额',
  `unit_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '下单时单价快照；存量未知则为NULL',
  `pay_type` tinyint NULL DEFAULT NULL COMMENT '支付方式：1微信，2支付宝，3现金',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付,2免费',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付 1已支付 2已取消 3已完成',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间（旧单为迁移时间）',
  `cancel_reason` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MANUAL/TIMEOUT/LEGACY_UNPAID/LEGACY_REFUNDED',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记：0已删除，1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_scenic_id`(`scenic_id` ASC) USING BTREE,
  INDEX `idx_pay_status`(`pay_status` ASC) USING BTREE,
  INDEX `idx_play_date`(`play_date` ASC) USING BTREE,
  INDEX `idx_order_timeout`(`status` ASC, `stock_reserved` ASC, `create_time` ASC, `id` ASC) USING BTREE,
  CONSTRAINT `fk_order_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 363 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景点门票订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for scenic_stock
-- ----------------------------
DROP TABLE IF EXISTS `scenic_stock`;
CREATE TABLE `scenic_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` int NOT NULL COMMENT '景区ID',
  `play_date` date NOT NULL COMMENT '游玩日期（Asia/Shanghai）',
  `ticket_type_id` int NOT NULL DEFAULT 1 COMMENT '票种ID，本期仅1=成人票',
  `total` int NOT NULL COMMENT '票种总库存',
  `remaining` int NOT NULL COMMENT '票种剩余库存',
  `version` int NOT NULL DEFAULT 0 COMMENT '库存变更计数，当前不用作乐观锁条件',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scenic_stock_day_type`(`scenic_id` ASC, `play_date` ASC, `ticket_type_id` ASC) USING BTREE,
  CONSTRAINT `fk_stock_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_stock_ticket_type` CHECK (`ticket_type_id` > 0),
  CONSTRAINT `ck_stock_valid` CHECK ((`total` >= 0) and (`remaining` >= 0) and (`remaining` <= `total`))
) ENGINE = InnoDB AUTO_INCREMENT = 2630 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '景区票种每日库存' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strategy
-- ----------------------------
DROP TABLE IF EXISTS `strategy`;
CREATE TABLE `strategy`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '攻略ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '攻略标题',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文内容',
  `collect_count` int NOT NULL DEFAULT 0 COMMENT '收藏数',
  `read_count` int NOT NULL DEFAULT 0 COMMENT '阅读量',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态 0待审核 1通过 2驳回',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `strategy_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '旅游攻略表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for strategy_comment
-- ----------------------------
DROP TABLE IF EXISTS `strategy_comment`;
CREATE TABLE `strategy_comment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `strategy_id` int NOT NULL COMMENT '攻略ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `audit_status` tinyint NOT NULL COMMENT '审核状态 0待审核 1通过 2驳回',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_strategy_id`(`strategy_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `strategy_comment_ibfk_1` FOREIGN KEY (`strategy_id`) REFERENCES `strategy` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strategy_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for travel_user
-- ----------------------------
DROP TABLE IF EXISTS `travel_user`;
CREATE TABLE `travel_user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(MD5加密)',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `user_role` tinyint NOT NULL DEFAULT 0 COMMENT '角色 0普通用户 1管理员',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
  `del_mark` tinyint NOT NULL DEFAULT 1 COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `account`(`account` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
