-- ============================================================
-- message 表初始数据（消息通知中心）：10 个用户，每人 3 条
-- 注意：user_id 默认 1~10，请改成你库里真实存在的用户ID
--       （可先查 SELECT id, account FROM travel_user;）
-- 重复执行会重复插入，如需重置可先 TRUNCATE TABLE message;
-- ============================================================

INSERT INTO `message` (`user_id`, `title`, `content`, `type`, `is_read`, `del_mark`, `create_time`, `update_time`) VALUES
-- ============ 用户1 ============
(1, '欢迎加入旅游平台', '亲爱的用户，欢迎注册！完善个人资料可以获得更多推荐。', 0, 0, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, '订单支付成功', '您预订的「都江堰景区」门票订单已支付成功，凭订单号入园。', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, '攻略审核通过', '您发布的攻略《成都三日游》已审核通过，快去看看吧。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- ============ 用户2 ============
(2, '平台活动上线', '五一限时活动已开启，景点门票最高立减50元！', 0, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, '取票提醒', '您明天在「青城山景区」游玩，请提前安排好出行时间。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(2, '路线审核未通过', '您发布的路线《川西环线》因交通信息不完整未通过审核，请修改后重新提交。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),

-- ============ 用户3 ============
(3, '账号安全提醒', '您的账号于新设备登录，若非本人操作请及时修改密码。', 0, 0, 1, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(3, '退款成功', '您的「欢乐谷」订单已退款成功，金额将在1-3个工作日内到账。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(3, '攻略审核通过', '您发布的攻略《九寨沟赏秋全攻略》已审核通过。', 2, 1, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- ============ 用户4 ============
(4, '欢迎加入旅游平台', '亲爱的用户，欢迎注册！完善个人资料可以获得更多推荐。', 0, 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(4, '订单待支付', '您有1笔「峨眉山景区」订单待支付，请在24小时内完成付款。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(4, '攻略审核未通过', '您发布的攻略《西安美食地图》图片不足，请补充后重新提交。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- ============ 用户5 ============
(5, '平台公告', '平台将于本周五凌晨2:00-4:00进行系统升级维护。', 0, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, '订单支付成功', '您预订的「三星堆博物馆」门票订单已支付成功。', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, '路线审核通过', '您发布的路线《稻城亚丁七日游》已审核通过。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR)),

-- ============ 用户6 ============
(6, '账号安全提醒', '您的账号密码于昨日修改成功。', 0, 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, '取票提醒', '您明天在「宽窄巷子」游玩，可提前在线取票避免排队。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(6, '攻略审核通过', '您发布的攻略《大理慢生活》已审核通过。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 9 HOUR), DATE_SUB(NOW(), INTERVAL 9 HOUR)),

-- ============ 用户7 ============
(7, '欢迎加入旅游平台', '亲爱的用户，欢迎注册！完善个人资料可以获得更多推荐。', 0, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, '订单支付成功', '您预订的「张家界国家森林公园」门票订单已支付成功。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(7, '路线审核未通过', '您发布的路线《环青海湖骑行》预算字段缺失，请补充后重新提交。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- ============ 用户8 ============
(8, '平台活动上线', '周末特惠：成都周边景区门票 9.9 元起！', 0, 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(8, '退款成功', '您的「泸沽湖景区」订单已退款成功。', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, '攻略审核通过', '您发布的攻略《重庆洪崖洞夜游》已审核通过。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- ============ 用户9 ============
(9, '账号安全提醒', '检测到您的账号存在异地登录风险，已临时锁定，请尽快改密。', 0, 0, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(9, '订单待支付', '您有1笔「黄龙风景区」订单待支付，请在24小时内完成付款。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, '路线审核通过', '您发布的路线《桂林山水四日游》已审核通过。', 2, 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- ============ 用户10 ============
(10, '欢迎加入旅游平台', '亲爱的用户，欢迎注册！完善个人资料可以获得更多推荐。', 0, 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(10, '订单支付成功', '您预订的「故宫博物院」门票订单已支付成功。', 1, 0, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10, '攻略审核未通过', '您发布的攻略《上海迪士尼一日游》标题过长，请修改后重新提交。', 2, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));
