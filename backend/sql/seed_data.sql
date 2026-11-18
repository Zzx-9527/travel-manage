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

LOCK TABLES `scenic_category` WRITE;
/*!40000 ALTER TABLE `scenic_category` DISABLE KEYS */;
INSERT INTO `scenic_category` (`id`, `name`, `del_mark`, `create_time`, `update_time`) VALUES (1,'自然景观',1,'2026-03-01 08:12:35','2026-03-01 08:25:11'),(2,'人文古迹',1,'2026-03-02 09:05:18','2026-03-02 09:18:42'),(3,'主题乐园',1,'2026-03-03 10:22:47','2026-03-03 10:35:06'),(4,'城市休闲',1,'2026-03-04 11:33:09','2026-03-04 11:48:33');
/*!40000 ALTER TABLE `scenic_category` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


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

LOCK TABLES `scenic` WRITE;
/*!40000 ALTER TABLE `scenic` DISABLE KEYS */;
INSERT INTO `scenic` (`id`, `category_id`, `name`, `cover`, `images`, `price`, `open_time`, `day_ticket_cutoff_time`, `default_capacity`, `default_adult_stock`, `address`, `longitude`, `latitude`, `intro`, `score`, `view_count`, `status`, `del_mark`, `create_time`, `update_time`) VALUES (1,1,'黄山风景区','/travel/uploads/scenic/1.jpg','/travel/uploads/scenic/1_1.jpg,/travel/uploads/scenic/1_2.jpg,/travel/uploads/scenic/1_3.jpg',190.00,'06:00-18:00','17:00:00',NULL,NULL,'安徽省黄山市黄山区汤口镇',118.1766660,30.1400000,'五岳归来不看山，黄山归来不看岳',4.9,12602,1,1,'2026-04-13 19:37:49','2026-11-16 01:18:15'),(2,2,'故宫博物院','/travel/uploads/scenic/2.jpg','/travel/uploads/scenic/2_1.jpg,/travel/uploads/scenic/2_2.jpg,/travel/uploads/scenic/2_3.jpg',60.00,'08:30-17:00','17:00:00',NULL,NULL,'北京市东城区景山前街4号',116.3974800,39.9172800,'中国明清两代的皇家宫殿',4.8,9866,1,1,'2026-04-13 19:37:49','2026-11-16 00:19:17'),(3,3,'上海迪士尼乐园','/travel/uploads/scenic/3.jpg','/travel/uploads/scenic/3_2.jpg,/travel/uploads/scenic/3_3.jpg',599.00,'08:00-22:00','17:00:00',NULL,NULL,'上海市浦东新区川沙镇黄赵路310号',121.7806100,31.1433300,'中国内地首座迪士尼主题乐园',4.7,15620,1,1,'2026-04-13 19:37:49','2026-11-16 00:28:13'),(4,4,'北京颐和园','/travel/uploads/scenic/4.jpg','',30.00,'06:00-20:00','17:00:00',NULL,NULL,'北京市海淀区新建宫门路19号',116.2728300,39.9913800,'保存最完整的皇家园林',4.6,7531,1,1,'2026-04-13 19:37:49','2026-11-16 00:31:15'),(5,2,'陕西历史博物馆','/travel/uploads/scenic/5.jpg','/travel/uploads/scenic/5_1.jpg,/travel/uploads/scenic/5_2.jpg,/travel/uploads/scenic/5_3.jpg',0.00,'08:30-18:00','17:00:00',NULL,NULL,'陕西省西安市雁塔区小寨东路91号',108.9523000,34.2343000,'中国第一座大型现代化国家级博物馆',4.9,6210,1,1,'2026-04-13 19:37:49','2026-11-16 00:19:19'),(6,1,'泰山风景区','https://picsum.photos/800/450?random=111','',115.00,'全天开放','17:00:00',NULL,NULL,'山东省泰安市泰山区红门路',117.1136000,36.2036000,'五岳之首，天下第一山',4.8,8950,0,1,'2026-04-13 19:37:49','2026-11-16 00:01:51'),(7,1,'西湖风景区','/travel/uploads/scenic/7.jpg','/travel/uploads/scenic/7_1.jpg,/travel/uploads/scenic/7_2.jpg',0.00,'全天开放','17:00:00',NULL,NULL,'浙江省杭州市西湖区',120.1542000,30.2732000,'人间天堂，杭州西湖',4.9,11230,1,1,'2026-04-13 19:37:49','2026-11-16 00:28:13'),(8,2,'乌镇古镇','/travel/uploads/scenic/8.jpg','/travel/uploads/scenic/8_2.jpg,/travel/uploads/scenic/8_3.jpg',150.00,'全天开放','17:00:00',NULL,NULL,'浙江省嘉兴市桐乡市乌镇镇',120.4896000,30.7400000,'中国最后的枕水人家',4.7,7840,1,1,'2026-04-13 19:37:49','2026-11-16 00:28:13'),(9,1,'三亚亚龙湾海滩','/travel/uploads/scenic/9.jpg','',0.00,'全天开放','17:00:00',NULL,NULL,'海南省三亚市吉阳区亚龙湾',109.5147000,18.2177000,'天下第一湾',4.8,9650,1,1,'2026-04-13 19:37:49','2026-11-16 00:28:13'),(10,1,'普陀山','/travel/uploads/scenic/10.jpg','/travel/uploads/scenic/10_1.jpg,/travel/uploads/scenic/10_2.jpg,/travel/uploads/scenic/10_3.jpg',160.00,'06:00-18:00','17:00:00',NULL,NULL,'浙江省舟山市普陀区',122.3758000,29.9903000,'中国佛教四大名山之一',4.6,5420,1,1,'2026-04-13 19:37:49','2026-11-16 00:19:24'),(11,1,'张家界国家森林公园','https://picsum.photos/seed/scenic11/800/450','',225.00,'07:00-18:00','17:00:00',NULL,NULL,'湖南省张家界市武陵源区',110.4880000,29.1166000,'阿凡达取景地，奇峰三千',4.9,10211,1,1,'2026-04-13 19:37:49','2026-11-16 00:02:48'),(75,2,'八达岭长城','/travel/uploads/scenic/75.jpg','/travel/uploads/scenic/75_1.jpg,/travel/uploads/scenic/75_2.jpg',40.00,'06:30-16:30','14:30:00',8000,6000,'北京市延庆区G6京藏高速58号出口',116.0167800,40.3565200,'明长城中保存最完整、最具代表性的一段，城墙依山脊起伏，登高可望群山连绵。',4.7,2,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(76,2,'天坛公园','/travel/uploads/scenic/76.jpg','/travel/uploads/scenic/76_1.jpg,/travel/uploads/scenic/76_2.jpg,/travel/uploads/scenic/76_3.jpg',15.00,'06:00-21:00','17:00:00',5000,4000,'北京市东城区天坛内东里7号',116.4102400,39.8819600,'明清帝王祭天祈谷之所，祈年殿三重檐圆形建筑是中国古建的标志性符号。',4.7,6,1,1,'2026-11-15 22:41:59','2026-11-16 00:19:33'),(77,3,'北京环球度假区','/travel/uploads/scenic/77.jpg','',418.00,'09:00-21:00','19:00:00',20000,16000,'北京市通州区环球大道1号',116.6726800,39.8550800,'七大主题景区加哈利波特魔法世界，适合安排一整天，演出与过山车项目密集。',4.6,2,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(78,4,'外滩','/travel/uploads/scenic/78.jpg','/travel/uploads/scenic/78_1.jpg,/travel/uploads/scenic/78_2.jpg,/travel/uploads/scenic/78_3.jpg',0.00,'全天','23:00:00',10000,8000,'上海市黄浦区中山东一路',121.4900000,31.2400000,'黄浦江西岸的万国建筑博览群，与对岸陆家嘴天际线隔江相望，夜景最佳。',4.8,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:19:57'),(79,4,'东方明珠广播电视塔','/travel/uploads/scenic/79.jpg','/travel/uploads/scenic/79_2.jpg',199.00,'09:00-21:30','20:00:00',10000,8000,'上海市浦东新区世纪大道1号',121.4997100,31.2397000,'陆家嘴地标，259米全透明观光廊可俯瞰外滩全景，塔内还有城市历史陈列馆。',4.7,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(80,2,'灵隐寺','/travel/uploads/scenic/80.jpg','/travel/uploads/scenic/80_2.jpg,/travel/uploads/scenic/80_3.jpg',45.00,'07:00-18:00','16:30:00',5000,4000,'杭州市西湖区法云弄1号',120.1010000,30.2400000,'江南著名古刹，飞来峰造像与大殿释迦牟尼像为看点，需先购飞来峰门票再入寺。',4.7,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(81,2,'拙政园','/travel/uploads/scenic/81.jpg','/travel/uploads/scenic/81_1.jpg,/travel/uploads/scenic/81_2.jpg,/travel/uploads/scenic/81_3.jpg',90.00,'07:30-17:30','16:00:00',3000,2400,'苏州市姑苏区东北街178号',120.6290000,31.3240000,'苏州最大古典园林，以水为中心分东中西三部分，借景与漏窗手法是造园范本。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:04'),(82,2,'中山陵','/travel/uploads/scenic/82.jpg','/travel/uploads/scenic/82_3.jpg',0.00,'08:30-17:00，周一闭馆','16:30:00',8000,6000,'南京市玄武区石象路7号',118.8480000,32.0600000,'孙中山先生陵寝，392级台阶沿中轴线直上，蓝琉璃顶与苍松翠柏相映。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(83,4,'夫子庙秦淮风光带','/travel/uploads/scenic/83.jpg','/travel/uploads/scenic/83_3.jpg',0.00,'全天','23:00:00',20000,16000,'南京市秦淮区贡院街152号',118.7880000,32.0230000,'十里秦淮核心段，夜游画舫与灯会是特色，周边小吃集中适合傍晚来。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(84,2,'秦始皇兵马俑博物馆','https://picsum.photos/seed/scenic84/800/450','/travel/uploads/scenic/84_1.jpg,/travel/uploads/scenic/84_2.jpg',120.00,'08:30-18:00','16:30:00',8000,6000,'西安市临潼区秦陵北路',109.2780000,34.3840000,'世界第八大奇迹，一、二、三号坑按军阵排列，建议请讲解或租语音导览。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:10'),(85,2,'华清宫','https://picsum.photos/seed/scenic85/800/450','',120.00,'07:00-18:00','16:00:00',5000,4000,'西安市临潼区华清路38号',109.2160000,34.3660000,'唐代皇家温泉行宫，骊山脚下，唐玄宗与杨贵妃故事发生地，晚间有实景演出。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:02:48'),(86,1,'华山','/travel/uploads/scenic/86.jpg','/travel/uploads/scenic/86_1.jpg,/travel/uploads/scenic/86_2.jpg,/travel/uploads/scenic/86_3.jpg',160.00,'07:00-19:00','16:00:00',8000,6000,'渭南市华阴市集灵路中段',110.0870000,34.4840000,'五岳之西，以险著称，长空栈道与鹞子翻身需另收费，建议索道上下留足一天。',4.5,2,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:11'),(87,1,'成都大熊猫繁育研究基地','https://picsum.photos/seed/scenic87/800/450','/travel/uploads/scenic/87_3.jpg',55.00,'07:30-18:00','16:00:00',5000,4000,'成都市成华区熊猫大道1375号',104.1470000,30.7340000,'全球最大圈养大熊猫繁育机构，早晨活跃度最高，月亮产房可看幼崽。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(88,2,'都江堰','/travel/uploads/scenic/88.jpg','/travel/uploads/scenic/88_1.jpg,/travel/uploads/scenic/88_2.jpg,/travel/uploads/scenic/88_3.jpg',80.00,'08:00-18:00','16:30:00',8000,6000,'成都市都江堰市公园路',103.6180000,31.0030000,'战国李冰主持修建的无坝引水工程，鱼嘴、飞沙堰、宝瓶口三大主体沿用至今。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:15'),(89,1,'九寨沟','https://picsum.photos/seed/scenic89/800/450','/travel/uploads/scenic/89_1.jpg,/travel/uploads/scenic/89_2.jpg',169.00,'08:30-18:00','14:00:00',8000,6000,'阿坝州九寨沟县漳扎镇',103.9180000,33.2600000,'以高山湖泊与瀑布群闻名，五花海、诺日朗瀑布为精华，秋季彩林最盛。',4.8,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(90,1,'峨眉山','/travel/uploads/scenic/90.jpg','/travel/uploads/scenic/90_1.jpg,/travel/uploads/scenic/90_2.jpg,/travel/uploads/scenic/90_3.jpg',160.00,'06:00-18:00','16:00:00',8000,6000,'乐山市峨眉山市名山南路',103.3360000,29.5590000,'佛教四大名山之一，金顶日出云海与十方普贤像为看点，可乘索道减少爬升。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:18'),(91,2,'乐山大佛','https://picsum.photos/seed/scenic91/800/450','/travel/uploads/scenic/91_1.jpg',80.00,'07:30-18:30','16:30:00',5000,4000,'乐山市市中区凌云路2435号',103.7710000,29.5470000,'唐代依山凿成的弥勒坐佛，通高71米，九曲栈道可近观，也可乘船远眺全貌。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(92,4,'洪崖洞','/travel/uploads/scenic/92.jpg','',0.00,'全天（灯光19:00-23:00）','22:00:00',20000,16000,'重庆市渝中区嘉陵江滨江路88号',106.5830000,29.5620000,'依山而建的吊脚楼群，11层错落直下江边，夜间灯光与千厮门大桥同框。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:01:51'),(93,1,'天门山','/travel/uploads/scenic/93.jpg','/travel/uploads/scenic/93_1.jpg,/travel/uploads/scenic/93_2.jpg,/travel/uploads/scenic/93_3.jpg',288.00,'08:00-19:00','16:00:00',5000,4000,'张家界市永定区大庸路',110.4790000,29.0510000,'世界最长高山客运索道直达山顶，玻璃栈道与天门洞是必体验项。',4.6,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:20'),(94,2,'凤凰古城','https://picsum.photos/seed/scenic94/800/450','/travel/uploads/scenic/94_1.jpg,/travel/uploads/scenic/94_2.jpg,/travel/uploads/scenic/94_3.jpg',0.00,'全天','22:00:00',8000,6000,'湘西州凤凰县沱江镇',109.5990000,27.9480000,'沱江穿城而过的苗族聚居古城，吊脚楼与虹桥夜景出名，九景联票另计。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:22'),(95,4,'橘子洲','https://picsum.photos/seed/scenic95/800/450','',0.00,'全天','22:00:00',20000,16000,'长沙市岳麓区橘子洲头2号',112.9620000,28.1900000,'湘江中的长条形沙洲，青年毛泽东艺术雕塑与焰火表演是主要看点。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:02:48'),(96,1,'桂林漓江','/travel/uploads/scenic/96.jpg','/travel/uploads/scenic/96_2.jpg,/travel/uploads/scenic/96_3.jpg',215.00,'08:00-17:00','15:00:00',8000,6000,'桂林市灵川县磨盘山码头',110.2900000,25.1000000,'喀斯特峰林夹江而立，竹筏与游船两种玩法，兴坪段二十元人民币背景最经典。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(97,2,'鼓浪屿','/travel/uploads/scenic/97.jpg','',100.00,'全天（轮渡07:10-18:30）','17:00:00',5000,4000,'厦门市思明区鼓浪屿',118.0670000,24.4470000,'万国建筑博览与钢琴之岛，日光岩、菽庄花园为必看，上岛需提前订船票。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(98,1,'武夷山','https://picsum.photos/seed/scenic98/800/450','/travel/uploads/scenic/98_1.jpg,/travel/uploads/scenic/98_2.jpg,/travel/uploads/scenic/98_3.jpg',140.00,'08:00-17:00','15:30:00',8000,6000,'南平市武夷山市武夷宫',117.7000000,27.7300000,'九曲溪竹筏漂流看丹霞地貌，天游峰可俯瞰群峰，岩茶产区遍布。',4.5,2,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:26'),(99,1,'蜈支洲岛','https://picsum.photos/seed/scenic99/800/450','/travel/uploads/scenic/99_1.jpg,/travel/uploads/scenic/99_2.jpg',144.00,'08:00-17:30','15:00:00',5000,4000,'三亚市海棠区海棠湾镇',109.7640000,18.3120000,'潜水与水上项目集中地，能见度高，环岛路与情人桥适合拍照。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(100,2,'丽江古城','/travel/uploads/scenic/100.jpg','/travel/uploads/scenic/100_1.jpg,/travel/uploads/scenic/100_3.jpg',50.00,'全天','22:00:00',8000,6000,'丽江市古城区七一街',100.2340000,26.8720000,'纳西族聚居的高原古城，四方街与水系纵横，需缴古城维护费。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(101,1,'玉龙雪山','https://picsum.photos/seed/scenic101/800/450','/travel/uploads/scenic/101_1.jpg,/travel/uploads/scenic/101_2.jpg,/travel/uploads/scenic/101_3.jpg',100.00,'07:00-18:00','15:00:00',5000,4000,'丽江市玉龙县甘海子',100.1800000,27.1000000,'北半球最南的现代海洋性冰川，大索道直上海拔4506米，蓝月谷在山脚。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:31'),(102,1,'洱海','/travel/uploads/scenic/102.jpg','/travel/uploads/scenic/102_1.jpg,/travel/uploads/scenic/102_3.jpg',0.00,'全天','23:00:00',20000,16000,'大理州大理市洱海生态廊道',100.1900000,25.7700000,'高原淡水湖，环湖130公里，喜洲、双廊、才村是主要停留点，骑行最佳。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(103,1,'石林','/travel/uploads/scenic/103.jpg','/travel/uploads/scenic/103_2.jpg,/travel/uploads/scenic/103_3.jpg',130.00,'08:00-18:00','16:00:00',8000,6000,'昆明市石林彝族自治县石林大道',103.3240000,24.8140000,'喀斯特石芽地貌博物馆，大石林、小石林与乃古石林片区风格各异。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(104,1,'黄果树瀑布','https://picsum.photos/seed/scenic104/800/450','',180.00,'07:00-18:00','16:00:00',8000,6000,'安顺市镇宁县黄果树镇',105.6670000,25.9830000,'亚洲最大瀑布群，主瀑高77.8米，水帘洞可从瀑布内侧穿行。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:02:48'),(105,1,'荔波小七孔','https://picsum.photos/seed/scenic105/800/450','/travel/uploads/scenic/105_1.jpg,/travel/uploads/scenic/105_2.jpg,/travel/uploads/scenic/105_3.jpg',110.00,'08:00-17:00','15:30:00',5000,4000,'黔南州荔波县王蒙镇',107.7330000,25.2500000,'响水河上的七孔古桥与层层跌水，鸳鸯湖可划船，水色翠绿。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:36'),(106,2,'西江千户苗寨','/travel/uploads/scenic/106.jpg','/travel/uploads/scenic/106_1.jpg,/travel/uploads/scenic/106_2.jpg,/travel/uploads/scenic/106_3.jpg',110.00,'全天','22:00:00',8000,6000,'黔东南州雷山县西江镇',108.1730000,26.4940000,'世界最大苗族聚居村寨，层层吊脚楼依山而建，夜间万家灯火。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:37'),(107,2,'黄鹤楼','/travel/uploads/scenic/107.jpg','/travel/uploads/scenic/107_1.jpg,/travel/uploads/scenic/107_2.jpg,/travel/uploads/scenic/107_3.jpg',70.00,'08:00-18:00','17:00:00',5000,4000,'武汉市武昌区蛇山西山坡特1号',114.3020000,30.5450000,'江南三大名楼之一，现楼为1985年重建，顶层可望长江大桥与三镇。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:41'),(108,4,'广州塔','/travel/uploads/scenic/108.jpg','/travel/uploads/scenic/108_2.jpg,/travel/uploads/scenic/108_3.jpg',150.00,'09:30-22:30','21:00:00',10000,8000,'广州市海珠区阅江西路222号',113.3240000,23.1060000,'600米高电视塔，俗称小蛮腰，488米观景台与摩天轮可俯瞰珠江新城。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(109,3,'长隆野生动物世界','https://picsum.photos/seed/scenic109/800/450','/travel/uploads/scenic/109_2.jpg,/travel/uploads/scenic/109_3.jpg',300.00,'09:30-18:00','16:30:00',20000,16000,'广州市番禺区大石镇迎宾路',113.3260000,23.0030000,'亚洲较大野生动物主题公园，自驾区与小火车看放养种群，熊猫三胞胎在此。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(110,2,'曲阜三孔','/travel/uploads/scenic/110.jpg','/travel/uploads/scenic/110_1.jpg,/travel/uploads/scenic/110_2.jpg,/travel/uploads/scenic/110_3.jpg',150.00,'08:00-17:00','16:00:00',5000,4000,'济宁市曲阜市南马道西街',116.9860000,35.5960000,'孔庙、孔府、孔林合称三孔，中国现存规模最大的祭祀建筑群。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:49'),(111,2,'莫高窟','/travel/uploads/scenic/111.jpg','/travel/uploads/scenic/111_1.jpg,/travel/uploads/scenic/111_2.jpg,/travel/uploads/scenic/111_3.jpg',238.00,'08:00-18:00','15:00:00',3000,2400,'敦煌市东南25公里莫高窟',94.8090000,40.0420000,'千年佛教艺术宝库，现存洞窟735个，需提前预约，先看数字电影再进窟。',4.4,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:50'),(112,1,'鸣沙山月牙泉','/travel/uploads/scenic/112.jpg','/travel/uploads/scenic/112_1.jpg,/travel/uploads/scenic/112_2.jpg,/travel/uploads/scenic/112_3.jpg',120.00,'05:30-21:00','19:00:00',8000,6000,'敦煌市鸣沙山月牙泉景区',94.6670000,40.0870000,'沙山环抱的月牙形泉眼，千年不涸，骑骆驼与滑沙是常规体验。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:20:51'),(113,2,'布达拉宫','https://picsum.photos/seed/scenic113/800/450','',200.00,'09:00-16:00','14:00:00',3000,2400,'拉萨市城关区北京中路35号',91.1170000,29.6580000,'依红山而建的宫堡式建筑群，海拔3700米，需提前预约并限时参观。',4.5,6,1,1,'2026-11-15 22:41:59','2026-11-16 00:30:15'),(114,1,'青海湖','/travel/uploads/scenic/114.jpg','/travel/uploads/scenic/114_1.jpg',100.00,'全天','18:00:00',8000,6000,'海南州共和县二郎剑景区',100.5000000,36.7500000,'中国最大内陆咸水湖，二郎剑与黑马河看日出，七八月油菜花最盛。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13'),(115,2,'平遥古城','/travel/uploads/scenic/115.jpg','/travel/uploads/scenic/115_1.jpg',125.00,'08:00-18:00','16:30:00',8000,6000,'晋中市平遥县古城内',112.1760000,37.1890000,'保存完整的明清县城，城墙、票号、镖局构成晋商文化现场。',4.5,0,1,1,'2026-11-15 22:41:59','2026-11-16 00:28:13');
/*!40000 ALTER TABLE `scenic` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ============================================================
-- 演示数据（仅用于全新数据库，重复执行会产生重复数据）
-- 账号：admin / zhangsan / lisi，密码均为 123456
-- ============================================================

-- ---------- 用户 ----------
INSERT INTO `travel_user` (`id`,`account`,`password`,`nickname`,`phone`,`avatar`,`user_role`,`status`,`del_mark`,`create_time`,`update_time`) VALUES
(1,'admin','$2a$10$atcNTHe9alBRH6eZ7B/3nOO9FMI.mGThkr4PFwr0ZMAZ8rnNSMEU6','管理员','13800000000',NULL,1,1,1,NOW(),NOW()),
(2,'zhangsan','$2a$10$atcNTHe9alBRH6eZ7B/3nOO9FMI.mGThkr4PFwr0ZMAZ8rnNSMEU6','张三','13800000001',NULL,0,1,1,NOW(),NOW()),
(3,'lisi','$2a$10$atcNTHe9alBRH6eZ7B/3nOO9FMI.mGThkr4PFwr0ZMAZ8rnNSMEU6','李四','13800000002',NULL,0,1,1,NOW(),NOW());

-- ---------- 路线 ----------
INSERT INTO `route` (`id`,`user_id`,`name`,`day_count`,`start_city`,`traffic`,`budget`,`intro`,`status`,`del_mark`,`create_time`,`update_time`) VALUES
(1,2,'北京经典三日游',3,'北京','自驾',2600.00,'故宫、长城、颐和园三大地标，节奏适中，适合第一次来北京。',1,1,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2,2,'成都休闲两日游',2,'成都','自驾',1200.00,'上午看熊猫，下午逛都江堰，适合周末短途。',1,1,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3,3,'西安历史文化两日游',2,'西安','自驾',1500.00,'兵马俑与华清宫一线，把秦汉唐一次看全。',1,1,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO `route_detail` (`route_id`,`scenic_id`,`day_no`,`sort`,`stay_time`,`del_mark`,`create_time`) VALUES
(1,2,1,1,'4 小时',1,NOW()),
(1,75,2,1,'5 小时',1,NOW()),
(1,76,2,2,'2 小时',1,NOW()),
(1,4,3,1,'3 小时',1,NOW()),
(2,87,1,1,'3 小时',1,NOW()),
(2,88,2,1,'4 小时',1,NOW()),
(3,84,1,1,'4 小时',1,NOW()),
(3,85,2,1,'3 小时',1,NOW());

-- ---------- 攻略 ----------
INSERT INTO `strategy` (`id`,`user_id`,`title`,`cover`,`content`,`collect_count`,`read_count`,`audit_status`,`del_mark`,`create_time`,`update_time`) VALUES
(1,2,'北京三日游避坑指南','/travel/uploads/scenic/2.jpg','故宫一定要提前 7 天在官方小程序抢票，早上 8 点半入场人最少。长城建议坐缆车上、走下来，省一半体力。颐和园从新建宫门进，直接到昆明湖。',12,860,1,1,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2,2,'成都大熊猫基地怎么玩最值','/travel/uploads/scenic/87_3.jpg','熊猫上午最活跃，建议 7 点半就进园，先冲月亮产房看幼崽。园区很大，观光车 10 元很值。回程可以顺路去都江堰，一天两个点刚刚好。',8,540,1,1,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3,3,'西安兵马俑一日路线','/travel/uploads/scenic/84_1.jpg','兵马俑按一号坑、三号坑、二号坑的顺序看，请个讲解差别很大。下午去华清宫，骊山脚下，晚上有实景演出。',5,320,1,1,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4,3,'黄山看日出住宿怎么选','/travel/uploads/scenic/1.jpg','山上酒店贵但省事，光明顶附近看日出走路十分钟。预算有限可以住山下汤口镇，凌晨坐索道上去，就是比较赶。',3,210,1,1,DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------- 攻略评论 ----------
INSERT INTO `strategy_comment` (`strategy_id`,`user_id`,`content`,`del_mark`,`create_time`,`audit_status`) VALUES
(1,3,'抢票这点太真实了，我上次就是没抢到。',1,DATE_SUB(NOW(), INTERVAL 4 DAY),1),
(1,2,'补充一句：珍宝馆要单独预约，别忘了。',1,DATE_SUB(NOW(), INTERVAL 3 DAY),1),
(2,3,'月亮产房几点排队比较合适？',1,DATE_SUB(NOW(), INTERVAL 3 DAY),1),
(3,2,'讲解确实值，不然就是看三个大坑。',1,DATE_SUB(NOW(), INTERVAL 2 DAY),1),
(4,2,'住山上会不会太贵？',1,DATE_SUB(NOW(), INTERVAL 1 DAY),1),
(4,3,'旺季一千多，但省了半夜爬山的折腾。',1,DATE_SUB(NOW(), INTERVAL 1 DAY),1);

-- ---------- 收藏（1 攻略 2 景点 3 路线） ----------
INSERT INTO `collect` (`user_id`,`target_type`,`target_id`,`create_time`) VALUES
(2,2,2,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2,2,75,DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2,3,1,DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2,1,1,DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3,2,1,DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ---------- 消息 ----------
INSERT INTO `message` (`user_id`,`title`,`content`,`type`,`is_read`,`del_mark`,`create_time`,`update_time`) VALUES
(2,'欢迎加入旅迹','完善个人资料可以获得更准确的行程推荐。',0,0,1,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2,'订单支付成功','您预订的「故宫博物院」门票已支付成功，凭订单号入园。',1,1,1,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2,'攻略审核通过','您发布的攻略《北京三日游避坑指南》已审核通过。',2,0,1,DATE_SUB(NOW(), INTERVAL 4 DAY),DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3,'订单支付成功','您预订的「成都大熊猫繁育研究基地」门票已支付成功。',1,0,1,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3,'订单已取消','您的「秦始皇兵马俑博物馆」订单已取消，库存已释放。',1,0,1,DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3,'欢迎加入旅迹','完善个人资料可以获得更准确的行程推荐。',0,1,1,DATE_SUB(NOW(), INTERVAL 6 DAY),DATE_SUB(NOW(), INTERVAL 6 DAY));

-- ---------- 订单（0 待支付 1 已支付 2 已取消） ----------
INSERT INTO `scenic_orders` (`order_no`,`user_id`,`scenic_id`,`contact_name`,`contact_phone`,`play_date`,`ticket_count`,`ticket_type_id`,`stock_reserved`,`total_amount`,`unit_price`,`pay_type`,`pay_status`,`status`,`cancel_time`,`cancel_reason`,`del_mark`,`create_time`,`pay_time`) VALUES
('DEMO202611160001',2,2,'张三','13800000001',DATE_ADD(CURDATE(), INTERVAL 3 DAY),2,1,0,120.00,60.00,1,1,1,NULL,NULL,1,DATE_SUB(NOW(), INTERVAL 3 DAY),DATE_SUB(NOW(), INTERVAL 3 DAY)),
('DEMO202611160002',2,75,'张三','13800000001',DATE_ADD(CURDATE(), INTERVAL 5 DAY),1,1,0,40.00,40.00,NULL,0,0,NULL,NULL,1,DATE_SUB(NOW(), INTERVAL 1 DAY),NULL),
('DEMO202611160003',3,87,'李四','13800000002',DATE_ADD(CURDATE(), INTERVAL 2 DAY),2,1,0,110.00,55.00,1,1,1,NULL,NULL,1,DATE_SUB(NOW(), INTERVAL 2 DAY),DATE_SUB(NOW(), INTERVAL 2 DAY)),
('DEMO202611160004',3,84,'李四','13800000002',DATE_ADD(CURDATE(), INTERVAL 6 DAY),1,1,0,120.00,120.00,NULL,2,2,DATE_SUB(NOW(), INTERVAL 1 DAY),'MANUAL',1,DATE_SUB(NOW(), INTERVAL 2 DAY),NULL);

-- ---------- 未来 30 天库存与场地容量（仅补缺失的日期） ----------
INSERT INTO `scenic_stock` (`scenic_id`,`play_date`,`ticket_type_id`,`total`,`remaining`,`version`)
WITH RECURSIVE d(n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM d WHERE n < 29)
SELECT s.id, DATE_ADD(CURDATE(), INTERVAL d.n DAY), 1, 2000, 2000, 0
FROM `scenic` s CROSS JOIN d
WHERE s.del_mark = 1 AND s.status = 1
  AND NOT EXISTS (SELECT 1 FROM `scenic_stock` st WHERE st.scenic_id = s.id AND st.play_date = DATE_ADD(CURDATE(), INTERVAL d.n DAY));

INSERT INTO `scenic_capacity` (`scenic_id`,`play_date`,`total`,`remaining`)
WITH RECURSIVE d2(n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM d2 WHERE n < 29)
SELECT s.id, DATE_ADD(CURDATE(), INTERVAL d2.n DAY), 5000, 5000
FROM `scenic` s CROSS JOIN d2
WHERE s.del_mark = 1 AND s.status = 1
  AND NOT EXISTS (SELECT 1 FROM `scenic_capacity` c WHERE c.scenic_id = s.id AND c.play_date = DATE_ADD(CURDATE(), INTERVAL d2.n DAY));
