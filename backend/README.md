# 🏕️ 旅游管理平台 — 后端

基于 **Spring Boot 2.7.5** + **MyBatis-Plus** 构建的旅游业务管理 RESTful 后端服务，为前端（管理后台 / 用户端）提供统一的 API 接口支撑。

---

## 技术栈

| 技术 | 版本 / 说明 |
|------|------------|
| Java | 8 |
| Spring Boot | 2.7.5 |
| MyBatis-Plus | 3.5.9（ORM + 自动填充 + 分页） |
| MySQL | 5.7+，数据库 `travel_management` |
| Sa-Token | 1.44.0，会话存 Redis（24h 绝对过期 + 30min 无操作过期） |
| Spring Security Crypto | BCrypt 密码加密 |
| 接口文档 | Knife4j 4.5.0 + SpringDoc 1.7.0（OpenAPI 3） |
| 连接池 | HikariCP |
| Lombok | 1.18.24 |
| 部署 | 可执行 JAR，内嵌 Tomcat |

> 技术栈治理（springfox → Knife4j + SpringDoc、DBCP2 → HikariCP、WAR → jar、JWT → Sa-Token）已完成，Redis 缓存已接入；Redisson 防重与登录限流见 [接入规划](docs/redis-integration-plan.md) Phase 5。

---

## 文档

所有项目文档统一放在 [`docs/`](docs/README.md)，根目录只保留本 README。

| 你想要 | 看这里 |
|---|---|
| 接口怎么调（含鉴权契约） | [前端接口对接说明](docs/前端接口对接说明.md) |
| 技术栈改造与缓存/登录态规划 | [接入规划（执行基准）](docs/redis-integration-plan.md) |
| 消息、收藏、订单等模块细节 | [文档索引](docs/README.md) |

---

## 快速启动

### 1. 创建数据库

```sql
CREATE DATABASE travel_management DEFAULT CHARSET utf8mb4;
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/travel_management?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 3. 启动服务

```bash
mvn clean package -Dmaven.test.skip=true
mvn spring-boot:run
```

服务默认启动在：`http://localhost:8081/travel`

### 4. 访问接口文档

启动后访问：`http://localhost:8081/travel/doc.html`（Knife4j 界面；旧的 `/swagger-ui/index.html` 已随 springfox 一起移除）

---

## 模块结构

```
com.zjx.travel
├── controller/        # REST 控制器
├── service/           # 业务接口
│   └── serviceImpl/   # 业务实现
├── mapper/            # MyBatis-Plus Mapper
├── domain/            # 实体类
├── config/            # Spring 配置（CORS、Knife4j、文件上传、Sa-Token 拦截器）
├── handler/           # 全局异常处理（401/403）
└── util/              # 工具类（统一返回、Redis、锁、密码、文件、消息通知）
```

---

## API 概览

所有接口统一前缀：`/travel`

> 除登录、注册、接口文档外，所有接口都需要 `Authorization: Bearer <token>`。
> 标注「管理员」的接口要求 `userRole=1`，普通用户（`userRole=0`）返回 403。

### 用户模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/travelUser/login` | POST | 用户登录，返回 Sa-Token token |
| `/travelUser/logout` | POST | 退出登录，旧 token 立即失效 |
| `/travelUser/myProfile` | GET | 当前登录人信息（无参数，身份取自 token） |
| `/travelUser/updateProfile` | POST | 修改自己的昵称/手机号/头像（登录即可） |
| `/travelUser/online` | GET | 在线用户列表（管理员，含多端 tokenCount） |
| `/travelUser/kickout/{userId}` | POST | 强制某用户下线（管理员） |
| `/travelUser/queryById/{id}` | GET | 查询用户详情（管理员） |
| `/travelUser/queryByPage` | GET | 分页查询用户列表（管理员） |
| `/travelUser/add` | POST | 新增用户（注册，匿名可访问） |
| `/travelUser/edit` | POST | 修改用户信息（管理员） |
| `/travelUser/deleteById/{id}` | POST | 删除用户（管理员） |
| `/travelUser/updatePassword` | POST | 修改密码（身份取 token，body 不需要 id） |

### 景点模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/scenic/queryById/{id}` | GET | 查询景点详情 |
| `/scenic/queryByPage` | GET | 分页查询景点列表 |
| `/scenic/add` | POST | 新增景点 |
| `/scenic/edit` | POST | 编辑景点 |
| `/scenic/deleteById/{id}` | POST | 删除景点 |

### 景点分类

| 接口 | 方法 | 说明 |
|------|------|------|
| `/scenicCategory/queryByPage` | GET | 分页查询分类列表 |
| `/scenicCategory/add` | POST | 新增分类 |
| `/scenicCategory/edit` | POST | 编辑分类 |
| `/scenicCategory/deleteById/{id}` | POST | 删除分类 |

### 路线模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/route/queryById/{id}` | GET | 查询路线详情 |
| `/route/queryByPage` | GET | 分页查询路线列表 |
| `/route/add` | POST | 新增路线 |
| `/route/edit` | POST | 编辑路线（含审核状态变更 → 自动消息通知） |
| `/route/deleteById/{id}` | POST | 删除路线 |

### 路线详情（每日行程）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/routeDetail/queryByPage` | GET | 分页查询行程明细 |
| `/routeDetail/add` | POST | 新增行程明细 |
| `/routeDetail/edit` | POST | 编辑行程明细 |
| `/routeDetail/deleteByRouteId` | POST | 按路线删除行程明细 |

### 攻略模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/strategy/queryById/{id}` | GET | 查询攻略详情 |
| `/strategy/queryByPage` | GET | 分页查询攻略列表 |
| `/strategy/add` | POST | 新增攻略 |
| `/strategy/edit` | POST | 编辑攻略（含审核通知） |
| `/strategy/deleteById/{id}` | POST | 删除攻略 |

### 攻略评论

| 接口 | 方法 | 说明 |
|------|------|------|
| `/strategyComment/queryByPage` | GET | 分页查询评论 |
| `/strategyComment/add` | POST | 新增评论 |
| `/strategyComment/deleteById/{id}` | POST | 删除评论 |

### 门票订单

| 接口 | 方法 | 说明 |
|------|------|------|
| `/scenicOrders/queryById/{id}` | GET | 查询订单详情 |
| `/scenicOrders/queryByPage` | GET | 分页查询订单列表 |
| `/scenicOrders/add` | POST | 新增订单（自动生成订单号 + 消息通知） |
| `/scenicOrders/edit` | POST | 编辑订单 |
| `/scenicOrders/deleteById/{id}` | POST | 删除订单 |

> 订单的列表/详情/编辑/删除均做归属校验：普通用户只能操作自己的订单，越权返回 403；管理员可操作全部。
> 下单接口对同一用户有 5 秒防重复提交。

### 收藏模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/collect/queryByPage` | GET | 分页查询收藏列表（自动按当前用户过滤） |
| `/collect/checkCollected` | GET | 判断当前用户是否已收藏目标 |
| `/collect/add` | POST | 新增收藏（收藏人取当前用户） |
| `/collect/deleteByUserAndTarget` | POST | 按目标取消收藏 |
| `/collect/deleteById/{id}` | POST | 取消收藏（按收藏 id） |

### 消息通知

| 接口 | 方法 | 说明 |
|------|------|------|
| `/message/queryById/{id}` | GET | 查询消息详情 |
| `/message/queryByPage` | GET | 分页查询消息列表（自动按当前用户过滤） |
| `/message/unreadCount` | GET | 查询当前用户未读消息数量（无参数） |
| `/message/markRead` | POST | 标记消息为已读（支持单条 / 批量，只能标记自己的） |
| `/message/add` | POST | 发送消息（管理员） |
| `/message/edit` | POST | 修改消息（管理员） |
| `/message/deleteById/{id}` | POST | 删除消息（本人或管理员） |

### 数据面板

| 接口 | 方法 | 说明 |
|------|------|------|
| `/dashboard/stats` | GET | 后台首页统计（管理员） |

---

## 核心设计

### 统一响应格式

所有接口返回 `ReturnVO` 统一结构：

```json
{
  "code": 1,       
  "msg": "找到数据！",
  "content": {}    
}
```

### Sa-Token 鉴权

- 登录接口返回随机 token（`tik` 风格，非 JWT），会话存 Redis（key 前缀跟随 `sa-token.token-name`，本项目为 `Authorization:login:*`），应用重启不掉线
- 请求头携带 `Authorization: Bearer <token>` 访问受保护接口
- `SaInterceptor` 统一校验，白名单放行登录/注册/接口文档；鉴权异常统一返回 401/403
- 支持登出立即失效、30 分钟无操作自动过期；踢人下线与角色鉴权已接入
- 后台接口（`/dashboard/**`、用户管理、在线列表、踢人）仅 `userRole=1` 的管理员可访问，普通用户（`userRole=0`）返回 403
- 前端对接细节见 [前端接口对接说明](docs/前端接口对接说明.md) 第 8 节

### BCrypt 密码加密

- 用户密码使用 `BCryptPasswordEncoder` 哈希存储
- 密码字段序列化时自动忽略，不返回给前端

### 消息通知系统

内置 `MessageNotifier` 组件，在关键业务节点自动触发消息：

| 场景 | 消息类型 | 说明 |
|------|---------|------|
| 路线审核通过 / 驳回 | 审核通知 | 通知发布者 |
| 攻略审核通过 / 驳回 | 审核通知 | 通知发布者 |
| 门票下单成功 | 订单通知 | 通知下单用户 |

消息写入 `message` 表，前端可通过 API 查询未读消息并标记已读。

### MyBatis-Plus 自动填充

通过 `MyMetaObjectHandler` 自动填充：

- `createTime` → 插入时自动填充
- `updateTime` / `payTime` → 插入和更新时自动填充

### 文件上传

- 支持图片 / 视频等附件上传
- 已配置 multipart 上传限制：最大 10MB
- 生成唯一文件名（时间戳 + 序号），防重名

### CORS 跨域

已配置全局 CORS 过滤器，允许所有来源、请求头和方法，方便前端跨域调用。

---

## 项目结构

```
travel-0/
├── pom.xml
├── docs/                                  # 项目文档（索引见 docs/README.md）
└── src/main/
    ├── java/com/zjx/travel/
    │   ├── SpbYqApplication.java          # 启动类
    │   ├── controller/                    # 控制器层
    │   │   ├── TravelUserController.java  # 用户管理
    │   │   ├── ScenicController.java      # 景点管理
    │   │   ├── ScenicCategoryController.java
    │   │   ├── RouteController.java       # 路线管理
    │   │   ├── RouteDetailController.java # 路线详情
    │   │   ├── StrategyController.java    # 攻略管理
    │   │   ├── StrategyCommentController.java
    │   │   ├── ScenicOrdersController.java# 订单管理
    │   │   ├── CollectController.java     # 收藏管理
    │   │   ├── MessageController.java     # 消息通知
    │   │   └── DashboardController.java   # 数据面板
    │   ├── service/                       # 业务接口
    │   │   └── serviceImpl/               # 业务实现
    │   ├── mapper/                        # 数据访问层
    │   ├── domain/                        # 实体
    │   ├── config/                        # 配置（含 Sa-Token 拦截器）
    │   ├── handler/                       # 全局异常处理
    │   └── util/                          # 工具类（Redis、锁等）
    └── resources/
        ├── application.properties        # 主配置
        └── mapper/                       # MyBatis XML 映射
```