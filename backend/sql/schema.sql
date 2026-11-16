
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `travel_management` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `travel_management`;
DROP TABLE IF EXISTS `ai_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `role` varchar(16) NOT NULL COMMENT 'user / assistant',
  `msg_type` varchar(16) NOT NULL DEFAULT 'text' COMMENT 'text / plan',
  `content` text NOT NULL COMMENT '文本内容；plan 存标题',
  `payload` mediumtext COMMENT 'plan 类型存完整行程 JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 对话消息';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_plan_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_plan_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户 id',
  `destination` varchar(64) NOT NULL COMMENT '目的地',
  `days` tinyint NOT NULL COMMENT '计划天数',
  `budget` decimal(10,2) DEFAULT NULL COMMENT '预算，可空',
  `preference` varchar(64) DEFAULT NULL COMMENT '偏好，可空',
  `extra` varchar(255) DEFAULT NULL COMMENT '补充说明，可空',
  `status` tinyint NOT NULL COMMENT '1=成功 0=失败',
  `error_msg` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `scenic_count` smallint NOT NULL DEFAULT '0' COMMENT '最终有效景点数',
  `dropped_spots` smallint NOT NULL DEFAULT '0' COMMENT '被白名单过滤的条目数',
  `latency_ms` int NOT NULL DEFAULT '0' COMMENT '端到端耗时(毫秒)',
  `model` varchar(64) DEFAULT NULL COMMENT '模型名',
  `result_json` mediumtext COMMENT '生成结果快照，便于排查',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_destination` (`destination`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 行程生成记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collect` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '收藏ID，自增主键',
  `user_id` int NOT NULL COMMENT '用户ID，外键关联travel_user.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `target_type` int NOT NULL DEFAULT '1' COMMENT '收藏目标类型 1攻略 2景点 3路线',
  `target_id` int NOT NULL COMMENT '收藏目标ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) USING BTREE,
  CONSTRAINT `fk_collect_user` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id` int DEFAULT NULL COMMENT '接收人ID',
  `title` varchar(255) DEFAULT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `type` int DEFAULT '0' COMMENT '消息类型 0系统 1订单 2审核',
  `is_read` int DEFAULT '0' COMMENT '是否已读 0未读 1已读',
  `del_mark` int DEFAULT '1' COMMENT '逻辑删除 1正常 0删除',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通知';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '路线ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `name` varchar(100) DEFAULT NULL COMMENT '路线名称',
  `day_count` int NOT NULL DEFAULT '1' COMMENT '游玩天数',
  `start_city` varchar(50) DEFAULT NULL COMMENT '出发城市',
  `traffic` varchar(50) DEFAULT NULL COMMENT '交通方式',
  `budget` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '预算',
  `intro` text COMMENT '路线说明',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1已发布2已驳回',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_route_user` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='旅游路线表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `route_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `route_detail` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '详情ID',
  `route_id` int NOT NULL COMMENT '路线ID',
  `scenic_id` int NOT NULL COMMENT '景点ID',
  `day_no` int NOT NULL COMMENT '第几天',
  `sort` int NOT NULL COMMENT '游玩顺序',
  `stay_time` varchar(50) DEFAULT NULL COMMENT '建议停留时长',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_route_id` (`route_id`),
  KEY `idx_scenic_id` (`scenic_id`),
  CONSTRAINT `route_detail_ibfk_1` FOREIGN KEY (`route_id`) REFERENCES `route` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `route_detail_ibfk_2` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线景点表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenic` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '景点ID',
  `category_id` int NOT NULL COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '景点名称',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `images` varchar(500) DEFAULT NULL COMMENT '图片集(逗号分隔)',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '门票价格',
  `open_time` varchar(100) DEFAULT NULL COMMENT '开放时间',
  `day_ticket_cutoff_time` time NOT NULL DEFAULT '17:00:00' COMMENT '当日门票停止下单时间（Asia/Shanghai）',
  `default_capacity` int DEFAULT NULL COMMENT '未来日期默认场地日容量，NULL表示全站默认100',
  `default_adult_stock` int DEFAULT NULL COMMENT '未来日期成人票默认库存，NULL表示全站默认100',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `intro` text COMMENT '景点介绍',
  `score` decimal(2,1) NOT NULL DEFAULT '5.0' COMMENT '综合评分',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0下架 1上架',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  CONSTRAINT `scenic_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `scenic_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景点表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenic_capacity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenic_capacity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` int NOT NULL COMMENT '景区ID',
  `play_date` date NOT NULL COMMENT '游玩日期（Asia/Shanghai）',
  `total` int NOT NULL COMMENT '当日场地总容量',
  `remaining` int NOT NULL COMMENT '当日剩余容量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scenic_capacity_day` (`scenic_id`,`play_date`),
  CONSTRAINT `fk_capacity_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`),
  CONSTRAINT `ck_capacity_valid` CHECK (((`total` >= 0) and (`remaining` >= 0) and (`remaining` <= `total`)))
) ENGINE=InnoDB AUTO_INCREMENT=2632 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景区每日场地承载容量';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenic_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenic_category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(30) NOT NULL COMMENT '分类名称',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景点分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenic_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenic_orders` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单ID，自增主键',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号，唯一',
  `user_id` int NOT NULL COMMENT '用户ID，外键关联travel_user.id',
  `scenic_id` int NOT NULL COMMENT '景点ID，外键关联scenic.id',
  `contact_name` varchar(20) NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) NOT NULL COMMENT '联系电话',
  `play_date` date NOT NULL COMMENT '游玩日期',
  `ticket_count` int NOT NULL COMMENT '购票人数（张数）',
  `ticket_type_id` int NOT NULL DEFAULT '1' COMMENT '票种ID，本期仅成人票1',
  `stock_reserved` tinyint NOT NULL DEFAULT '0' COMMENT '新库存预留：1是，历史订单0否',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '下单时单价快照；存量未知则为NULL',
  `pay_type` tinyint DEFAULT NULL COMMENT '支付方式：1微信，2支付宝，3现金',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态：0未支付，1已支付,2免费',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0待支付 1已支付 2已取消 3已完成',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间（旧单为迁移时间）',
  `cancel_reason` varchar(24) DEFAULT NULL COMMENT 'MANUAL/TIMEOUT/LEGACY_UNPAID/LEGACY_REFUNDED',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记：0已删除，1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_scenic_id` (`scenic_id`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_play_date` (`play_date`),
  KEY `idx_order_timeout` (`status`,`stock_reserved`,`create_time`,`id`),
  CONSTRAINT `fk_order_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=363 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景点门票订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenic_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenic_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scenic_id` int NOT NULL COMMENT '景区ID',
  `play_date` date NOT NULL COMMENT '游玩日期（Asia/Shanghai）',
  `ticket_type_id` int NOT NULL DEFAULT '1' COMMENT '票种ID，本期仅1=成人票',
  `total` int NOT NULL COMMENT '票种总库存',
  `remaining` int NOT NULL COMMENT '票种剩余库存',
  `version` int NOT NULL DEFAULT '0' COMMENT '库存变更计数，当前不用作乐观锁条件',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scenic_stock_day_type` (`scenic_id`,`play_date`,`ticket_type_id`),
  CONSTRAINT `fk_stock_scenic` FOREIGN KEY (`scenic_id`) REFERENCES `scenic` (`id`),
  CONSTRAINT `ck_stock_ticket_type` CHECK ((`ticket_type_id` > 0)),
  CONSTRAINT `ck_stock_valid` CHECK (((`total` >= 0) and (`remaining` >= 0) and (`remaining` <= `total`)))
) ENGINE=InnoDB AUTO_INCREMENT=2630 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='景区票种每日库存';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `strategy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `strategy` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '攻略ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `title` varchar(100) NOT NULL COMMENT '攻略标题',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `content` text COMMENT '正文内容',
  `collect_count` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `read_count` int NOT NULL DEFAULT '0' COMMENT '阅读量',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2驳回',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `strategy_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='旅游攻略表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `strategy_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `strategy_comment` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `strategy_id` int NOT NULL COMMENT '攻略ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `audit_status` tinyint NOT NULL COMMENT '审核状态 0待审核 1通过 2驳回',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `strategy_comment_ibfk_1` FOREIGN KEY (`strategy_id`) REFERENCES `strategy` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `strategy_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `travel_user` (`id`) ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `travel_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `account` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(100) NOT NULL COMMENT '密码(MD5加密)',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `user_role` tinyint NOT NULL DEFAULT '0' COMMENT '角色 0普通用户 1管理员',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0禁用 1正常',
  `del_mark` tinyint NOT NULL DEFAULT '1' COMMENT '删除标记 0已删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `account` (`account`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

