# 后端 API 清单

> 面向"旅游信息管理平台"前端（`src/api/*.js`）的全部后端接口需求。
> 前端已跑通：构建通过、页面可正常渲染（无后端时优雅降级）。本清单是后端实现时参照的契约。

---

## 0. 统一约定

| 项 | 约定 |
|---|---|
| Base URL | `http://localhost:8081/travel/` |
| 认证 | Sa-Token，`Authorization: Bearer <token>`（token 为服务端随机串，非 JWT，前端只存取不解析）；本地存有 token 即附带；HTTP 401=未登录/失效、403=无权限 |
| 返回体 | `{ code: 1, msg: "ok", content: ... }`，`code !== 1` 视为失败 |
| 分页返回 | `content: { content: [], totalElements: N, totalPages: N }` |
| 分页参数 | `page`（从 1 开始）、`size` |
| 删除 | 一律**逻辑删除**（`deleteById`，POST） |
| 审核/上架字段 | 攻略 `auditStatus`、路线 `status`、景点 `status`：`0=禁用/未通过`、`1=启用/通过` |
| 订单状态 | `status`：`0=待支付`、`1=已支付`、`2=已取消`、`3=已完成`；`cancelReason=TIMEOUT` 表示超时取消。`payStatus` 仅保留历史兼容（2=历史退款，不再代表“免费”） |
| 静默降级 | 标注 🔕 的接口历史上前端加过 `silent: true`；**2026-11 起前端已无任何 silent 标记**（仅 `logout` 保留一处 UX 性质的使用，与 🔕 无关），当前 🔕 仅表示「后端未实现的旧规划路径」 |

---

## 一、认证 & 用户

### 1.1 `POST travelUser/login` 🔓
- Body：`{ account, password }`
- 返回 `content`：`{ token, userInfo: { id, account, nickname, avatar, phone, userRole, status } }`（`userRole`: 1=管理员 / 0=普通用户）

### 1.2 `POST travelUser/register`
- Body：`{ account, password, nickname, phone? }`
- 重复账号需返回明确错误（前端未设 silent，会弹全局错误）

### 1.3 `GET travelUser/myProfile`
- 需登录；返回当前登录用户信息（`content`：userInfo 结构）
- 用于"我的资料"页回填 + 刷新本地登录态

### 1.4 `POST travelUser/updateProfile`
- Body：`{ nickname, phone?, avatar? }`；需登录

### 1.5 `POST travelUser/updatePassword`
- Body：`{ oldPassword, newPassword }`；需登录；旧密码错误需返回明确错误

### 1.6 管理端用户 CRUD（后台）
| 接口 | 方法 | 参数 |
|---|---|---|
| `travelUser/register` | POST | 公开注册（account/password/nickname/phone/avatar，匿名） |
| `travelUser/queryByPage` | GET | `page, size, account?, userRole?, status?` |
| `travelUser/add` | POST | 后台新增用户（管理员；account/password/nickname/phone/userRole/status） |
| `travelUser/edit` | POST | 用户字段（含 id） |
| `travelUser/deleteById/{id}` | POST | 逻辑删除 |

---

## 二、景点分类 scenicCategory

| 接口 | 方法 | 参数 |
|---|---|---|
| `scenicCategory/queryByPage` | GET | `page, size, name?` |
| `scenicCategory/add` | POST | `{ name, ... }` |
| `scenicCategory/edit` | POST | `{ id, name, ... }` |
| `scenicCategory/deleteById/{id}` | POST | 逻辑删除 |

---

## 三、景点 scenic

| 接口 | 方法 | 参数 |
|---|---|---|
| `scenic/queryByPage` | GET | `page, size, name?, categoryId?, status?`（用户端传 `status:1`） |
| `scenic/detail/{id}` | GET 🔕 | 返回景点详情（含 `images` 逗号分隔、`categoryId`、`price/score/address/openTime/intro`） |
| `scenic/add` | POST | 景点字段 |
| `scenic/edit` | POST | 景点字段（含 id） |
| `scenic/deleteById/{id}` | POST | 逻辑删除 |

---

## 四、攻略 strategy

| 接口 | 方法 | 参数 |
|---|---|---|
| `strategy/queryByPage` | GET | `page, size, title?, auditStatus?`（用户端传 `auditStatus:1`） |
| `strategy/detail/{id}` | GET 🔕 | 返回攻略详情（含 `content` 正文、`userId`、`auditStatus`、`collectCount/readCount`） |
| `strategy/add` | POST | 攻略字段（含 title/cover/content，建议记录 userId 并落 `auditStatus=0` 待审核） |
| `strategy/edit` | POST | 攻略字段（含 id；审核状态变更走此接口） |
| `strategy/deleteById/{id}` | POST | 逻辑删除 |

---

## 五、攻略评论 strategyComment

| 接口 | 方法 | 参数 |
|---|---|---|
| `strategyComment/queryByPage` | GET | `page, size, keyword?, status?`（管理端） |
| `strategyComment/byStrategy` | GET 🔕 | `strategyId, page, size`（攻略详情页评论列表） |
| `strategyComment/add` | POST | `{ strategyId, content, rating }`；建议落 `status=0` 待审核 |
| `strategyComment/myComments` | GET 🔕 | `page, size`（我的评论，需登录） |
| `strategyComment/deleteMy/{id}` | POST 🔕 | 用户删除自己的评论（需登录，校验归属） |
| `strategyComment/edit` | POST | 含 id；审核状态变更走此接口 |
| `strategyComment/deleteById/{id}` | POST | 逻辑删除 |

---

## 六、收藏 collect（当前针对攻略）

| 接口 | 方法 | 参数 |
|---|---|---|
| `collect/queryByPage` | GET | `page, size, title?`（管理端） |
| `collect/add` | POST | `{ strategyId }`；需登录，校验不重复收藏 |
| `collect/myFavorites` | GET 🔕 | `page, size`；返回条目需带攻略信息（title/cover/strategyId）供卡片渲染 |
| `collect/deleteMy/{id}` | POST 🔕 | 取消收藏（传收藏记录 id） |
| `collect/check` | GET 🔕 | `strategyId`；返回 `content: { collected: true/false }` |
| `collect/deleteById/{id}` | POST | 逻辑删除 |

---

## 七、路线 route

| 接口 | 方法 | 参数 |
|---|---|---|
| `route/queryByPage` | GET | `page, size, name?, status?`（用户端传 `status:1`） |
| `route/detail/{id}` | GET 🔕 | 返回路线详情（含 name/intro/dayCount/startCity/traffic/budget/userId） |
| `route/add` | POST | 路线字段（服务端取当前用户并落 `status=0` 待审核，返回 `content.id`） |
| `route/edit` | POST | 路线字段（含 id） |
| `route/deleteById/{id}` | POST | 逻辑删除 |

---

## 八、路线明细 routeDetail（路线途经景点）

| 接口 | 方法 | 参数 |
|---|---|---|
| `routeDetail/queryByPage` | GET | `page, size`（**建议增加 `routeId` 过滤参数**，见下方优化项） |
| `routeDetail/add` | POST | `{ routeId, scenicId, dayNo, sort, stayTime }` |
| `routeDetail/edit` | POST | 含 id |
| `routeDetail/deleteById/{id}` | POST | 逻辑删除 |

---

## 九、订单 scenicOrders

| 接口 | 方法 | 参数 |
|---|---|---|
| `scenicOrders/queryByPage` | GET | `page, size, orderNo?, status?`；普通用户后端只返回本人订单，管理员可查全部 |
| `scenicOrders/add` | POST | `{ scenicId: { id }, playDate, ticketCount, contactName, contactPhone }`；身份取 token，金额/状态/订单号由后端决定；返回 `{ id, orderNo }` |
| `scenicOrders/pay/{id}` | POST | 模拟支付：仅本人、待支付且创建未满 30 分钟，状态 → 已支付 |
| `scenicOrders/cancel/{id}` | POST | 本人或管理员取消待支付订单，回补库存，订单保留 |
| `scenicOrders/edit` | POST | 仅允许修改本人待支付订单的联系人 |
| `scenicOrders/deleteById/{id}` | POST | 逻辑删除；待支付订单不可删除，需先取消 |
| `scenic/stock` | GET | `scenicId, playDate`：查询可售余票，仅展示 |

---

## 十、需新增的接口（前端当前缺失/占位，需前后端一起补）

| # | 场景 | 建议接口 | 说明 |
|---|---|---|---|
| 1 | **后台首页统计卡片** | `GET dashboard/stats` | 返回 `{ userCount, scenicCount, strategyCount, orderCount }`（当前 Dashboard 是硬编码假数据 `125/48/36/89`） |
| 2 | **后台"用户信息"菜单** | `GET travelUser/myProfile`（复用 1.3） | 当前 MainLayout 点了弹"功能开发中" |
| 3 | **后台"修改密码"菜单** | `POST travelUser/updatePassword`（复用 1.5） | 同上，需增加后台入口 UI |
| 4 | **消息通知中心** | 新建 `message/*`：`queryByPage`、`markRead/{id}`、`unreadCount` | 用户端/后台顶栏铃铛目前只弹"即将上线"；需新页面 + 接口 |
| 5 | **用户发布攻略/路线** | 复用 `strategy/add`、`route/add` | 用户端目前**无发布入口**；后端需支持 userId + 待审核状态，前端再加发布表单 |
| 6 | **景点/路线收藏** | 扩展 `collect` 加 `targetType`（strategy/scenic/route） | 目前 collect 仅攻略收藏；如需景点/路线收藏需扩展 |
| 7 | **路线途经景点按路线过滤** | `routeDetail/queryByPage` 增加 `routeId` 参数 | 当前 RouteDetail.vue 拉 100 条再客户端过滤（`RouteDetail.vue:139`），数据量大时不可行 |

---

## 附：前端"静默降级"标记状态

前端现已**不存在任何 🔕 相关的 `silent: true`**（唯一保留在 `src/api/auth.js` 的 `logout` 调用上，属"登出失败不该报错"的 UX 设计，与 🔕 无关），原清理点映射表随之作废。

- 已实现并摘除 🔕：`myProfile`、`updateProfile`、`updatePassword`、`strategyComment/add`、`collect/add`
- 仍带 🔕 的行（`scenic/strategy/route 的 detail/{id}`、`strategyComment/byStrategy|myComments|deleteMy`、`collect/myFavorites|deleteMy|check`、`scenicOrders/create|myOrders|pay|cancel`）均为后端未实现的旧规划路径；实际调用早已走通用 CRUD + token 识别身份（契约见《前端接口对接说明.md》），后续实现或废弃时再清理对应行
