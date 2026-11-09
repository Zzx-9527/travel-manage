# 旅游信息管理平台（前端）

基于 Vue 3 + Vite 构建的旅游信息管理平台前端，包含**用户端**与**管理端**双端：普通用户可浏览景点、攻略、路线并进行智能路线规划、下单、收藏与互动；管理员可对用户、景点、攻略、路线、订单、评论等内容进行统一管理与审核。

## 功能特性

### 用户端（`/travel/home`）

- **首页**：景点、攻略、路线聚合展示，公告横幅提醒
- **景点浏览**：景点列表 / 详情，内置高德地图查看景点位置
- **攻略中心**：攻略列表 / 详情，支持收藏、评论
- **路线中心**：路线列表 / 详情，按天查看途经景点行程
- **智能路线规划**：基于 Haversine 球面距离 + 最近邻贪心算法自动排定景点访问顺序，结合高德驾车规划展示行程，纯前端实现、无需额外后端支持
- **内容发布**：用户可发布攻略 / 路线，提交后默认进入待审核状态
- **门票订单**：景点门票下单与支付流程
- **消息中心**：系统 / 订单 / 审核三类消息，未读小红点、关键词搜索、单条与批量已读
- **个人中心**：个人资料、我的订单、我的收藏、我的评论、我的发布、路线行程配置

### 管理端（`/travel`）

- **系统概览**：用户 / 景点 / 攻略 / 订单统计卡片 + ECharts 数据图表
- **内容管理**：景点分类、景点、旅游攻略、旅游路线、路线途经景点
- **审核管理**：攻略 / 路线审核（待审核 / 通过 / 驳回），审核结果自动发送消息通知
- **互动管理**：评论管理、收藏管理
- **用户与订单**：用户管理、景点门票订单管理
- **消息与公告**：后台发通知的消息中心、面向全站的公告管理

### 通用能力

- JWT 登录鉴权，请求拦截器自动附加 Token，401 自动清理登录态并跳转登录页
- 路由守卫按角色（管理员 / 普通用户）控制访问范围
- 统一响应拦截与错误提示，接口未就绪时支持 `silent` 静默降级渲染空态
- 高德地图 Key 通过环境变量注入，未配置时地图功能优雅降级
- 骨架屏、空状态等加载体验组件

## 技术栈

| 分类 | 技术 |
|---|---|
| 核心框架 | Vue 3（Composition API） |
| 构建工具 | Vite 5 |
| UI 组件库 | Element Plus（中文语言包） |
| CSS 方案 | Tailwind CSS 3 + 自定义主题 |
| 状态管理 | Pinia |
| 路由 | Vue Router 4（History 模式） |
| HTTP 请求 | Axios（拦截器统一处理） |
| 图表 | ECharts 6 |
| 地图 | 高德地图 JS API 2.0（WebGL） |
| 图标 | Font Awesome + @element-plus/icons-vue |

## 项目结构

```
├── docs/                        # 后端 API 清单、消息模块方案等文档
├── public/                      # 静态资源
├── src/
│   ├── api/                     # 接口封装（auth/scenic/strategy/route/order/message/...）
│   ├── assets/                  # 样式与图片资源
│   ├── components/user/         # 用户端通用组件（卡片、支付/下单弹窗、地图、骨架屏等）
│   ├── composables/             # 组合式函数（分页列表、后台表格）
│   ├── config/                  # 高德地图等外部服务配置
│   ├── layouts/                 # 布局（管理端 MainLayout / 用户端 UserLayout）
│   ├── router/                  # 路由与导航守卫
│   ├── stores/                  # Pinia 状态（登录态）
│   ├── utils/                   # 工具（路线规划算法、地图加载、格式化、降级）
│   ├── views/                   # 管理端页面
│   │   └── user/                # 用户端页面（含 mine/ 个人中心子模块）
│   ├── App.vue
│   └── main.js
├── .env.example                 # 环境变量模板
├── smoke-test.mjs               # 用户端冒烟测试（Puppeteer 截图遍历）
├── vite.config.js               # Vite 配置（代理、别名）
└── package.json
```

## 快速开始

### 环境要求

- Node.js 16+
- 可运行的后端服务（默认 `http://localhost:8081/travel`，接口约定见 [docs/后端API清单.md](docs/后端API清单.md)）
- 高德开放平台账号（用于地图功能，[lbs.amap.com](https://lbs.amap.com) 创建 Web端 JS API Key）

### 安装与启动

```bash
# 1. 安装依赖
npm install

# 2. 配置环境变量（高德地图 Key）
cp .env.example .env.local
# 编辑 .env.local，填入：
#   VITE_AMAP_KEY=你的高德Key
#   VITE_AMAP_SECURITY_CODE=配套的安全密钥

# 3. 启动开发服务器（默认 http://localhost:5173）
npm run dev
```

> 地图 Key 未配置时应用仍可运行，仅地图相关功能降级展示。

### 构建与预览

```bash
npm run build     # 打包到 dist/
npm run preview   # 本地预览构建产物
```

### 冒烟测试

开发服务器运行中时，可执行基于 Puppeteer 的用户端冒烟测试（拦截 API 返回模拟数据，遍历关键页面截图并收集控制台报错）：

```bash
node smoke-test.mjs
```

## 后端对接约定

- 基础地址：`http://localhost:8081/travel`
- 统一响应结构：`{ "code": 1, "msg": "提示信息", "content": "业务数据" }`，`code = 1` 成功
- 分页数据统一从 `content.content`（列表）与 `content.totalElements`（总数）取值
- 登录后请求头携带 `Authorization: Bearer <token>`
- 常用枚举：
  - `targetType`（收藏对象）：`1=攻略，2=景点，3=路线`
  - `auditStatus` / `status`（审核状态）：`0=待审核，1=已通过，2=已驳回`
  - 消息 `type`：`0=系统，1=订单，2=审核`；`isRead`：`0=未读，1=已读`

详细接口说明见 [docs/后端API清单.md](docs/后端API清单.md) 与 [前端接口对接说明.md](前端接口对接说明.md)。
