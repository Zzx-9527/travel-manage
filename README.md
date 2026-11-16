# 旅迹 Travel Manage

一个面向自由行用户的旅游平台：把「找景点 → 排路线 → 下单」串成一条线，并内置 AI 助手，用对话直接生成**可落库**的行程。

## 主要功能

### 用户端

| 模块 | 能力 |
|---|---|
| 景点 | 列表、分类筛选、关键词搜索；详情含影像集、门票、开放时间、坐标；按日库存下单（防超卖、5 秒防重复提交） |
| 路线 | 地图选点、驾车/步行/骑行路线规划、距离与时长估算、保存与二次编辑 |
| 攻略 | 发布、审核、详情、评论、收藏 |
| 个人中心 | 我的订单、收藏、发布、评论、消息，均按时间倒序 |

### AI 助手

- 右下角悬浮窗，支持拖动、八向缩放，宽屏自动两列布局
- 多轮对话；行程生成只从**在售景点**里挑选，不编造不存在的景点
- 结果以卡片呈现，可一键保存为路线或在地图上查看
- 每日配额：对话 60 条、生成 10 次（`travel.ai.chat-daily-limit` / `daily-limit` 可调），上游失败自动返还次数
- 对话记录与生成记录持久化，重开窗口可恢复

### 管理端

数据看板、景点与分类管理、订单管理、用户管理、攻略审核、评论/收藏/消息管理、AI 用量看板。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5、Java 17、MyBatis-Plus 3.5、Spring AI（DeepSeek） |
| 存储 | MySQL 8、Redis（Sa-Token 会话、Redisson） |
| 前端 | Vue 3、Vite、Element Plus、Pinia、ECharts、Tailwind |
| 地图 | 高德地图 JS API 2.0 |

## 目录结构

```
backend/                Spring Boot 服务
  src/main/java/        controller / service / mapper / domain
  src/main/resources/   application.yml、mapper XML
  sql/                  增量 SQL（AI 对话表、AI 生成记录、景点数据等）
  uploads/              上传文件与景点图片（/uploads/** 静态访问）
frontend/               Vue 3 前端
  src/views/user/       用户端页面
  src/views/            管理端页面
  docs/                 前端接口对接说明
docs/                   模块设计说明
```

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8.0
- Redis 5+

## 安装

### 1. 数据库

```bash
# 建库 + 建表（14 张表，脚本内已包含 CREATE DATABASE）
mysql -uroot -p < backend/sql/schema.sql

# 可选：导入种子数据（4 个分类 + 52 个景点）
mysql -uroot -p travel_management < backend/sql/seed_data.sql
```

> ⚠️ `schema.sql` 含 14 条 `DROP TABLE IF EXISTS`，**只用于全新初始化**；已有数据的库不要执行。

### 2. 后端

```bash
cd backend
cp .env.example .env       # 填入下面的配置
mvn spring-boot:run
```

`.env` 需要的变量：

| 变量 | 说明 | 必填 |
|---|---|---|
| `DEEPSEEK_API_KEY` | DeepSeek 的 API Key，AI 助手依赖 | 是 |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 数据库账号 | 是 |
| `SPRING_AI_OPENAI_BASE_URL` | 中转地址，默认 `https://api.deepseek.com` | 否 |
| `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL` | 模型名，默认 `deepseek-chat` | 否 |

启动后：

- 接口地址：`http://localhost:8081/travel`
- 接口文档：`http://localhost:8081/travel/swagger-ui/index.html`

### 3. 前端

```bash
cd frontend
cp .env.example .env.local     # 见下方说明
npm install
npm run dev
```

`.env.local` 需要的变量：

| 变量 | 说明 |
|---|---|
| `VITE_AMAP_KEY` | 高德 Web 端（JS API）Key |
| `VITE_AMAP_SECURITY_CODE` | 高德安全密钥 |
| `VITE_API_BASE_URL` | 后端地址，默认 `http://localhost:8081/travel` |
| `VITE_ASSET_BASE_URL` | 静态资源域名，默认 `http://localhost:8081` |

访问 `http://localhost:5173/travel/home`。

## 使用

### 用户端

1. 注册 / 登录
2. 景点列表筛选 → 详情页看门票与开放时间 → 选择日期和票数下单
3. 进入「路线规划」，在地图上添加景点，自动计算路线与耗时，保存后可继续编辑
4. 「我的」里查看订单、收藏、发布、评论和消息

### AI 助手

点击右下角悬浮球，直接说需求即可，例如：

- `西安两天，想看兵马俑和城墙，预算 1000`
- `成都三天，带孩子，节奏慢一点`
- `杭州一日游`

生成后可以「保存为路线」，或跳到地图查看点位顺序。

### 管理端

使用 `user_role = 1` 的账号登录，登录后自动进入 `/travel/dashboard`。

全新数据库里没有内置管理员，先在前端注册一个账号，再执行：

```sql
UPDATE travel_user SET user_role = 1 WHERE account = '你的账号';
```

## 已知限制

- 支付为站内模拟流程（待支付 → 已支付），未接入真实支付渠道
- 景点门票为参考价，淡旺季与线上线下价格可能有差异
- AI 只能选择 `scenic` 表中已收录的景点，未收录城市会提示无结果
- 部分景点的封面/影像集使用了公开网络图片，仅用于学习演示

## License

[MIT](LICENSE)
