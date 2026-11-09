# 后端新增接口使用说明

> 项目基础地址：`http://localhost:8081/travel`
> 统一返回格式：`{ "code": 1, "msg": "...", "content": ... }`，`code = 1` 表示成功，`0` 表示失败。
> 分页接口返回 `content` 为 Spring Data 的 `Page` 结构：`{ "content": [...], "pageable": {...}, "totalElements": 总数, ... }`。

---

## 一、需要先执行的 SQL（重要）

以下 SQL 只需执行一次，请在你的数据库 `travel_management` 中执行：

```sql
-- ========== 1. 消息通知中心：新建 message 表 ==========
CREATE TABLE `message` (
    `id`          INT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     INT          DEFAULT NULL COMMENT '接收人ID',
    `title`       VARCHAR(255) DEFAULT NULL COMMENT '消息标题',
    `content`     TEXT COMMENT '消息内容',
    `type`        INT          DEFAULT 0 COMMENT '消息类型 0系统 1订单 2审核',
    `is_read`     INT          DEFAULT 0 COMMENT '是否已读 0未读 1已读',
    `del_mark`    INT          DEFAULT 1 COMMENT '逻辑删除 1正常 0删除',
    `create_time` DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '消息通知';

-- ========== 2. 收藏扩展：给 collect 表增加 target_type / target_id ==========
-- target_type：1攻略 2景点 3路线；target_id：对应目标的ID
ALTER TABLE `collect`
    ADD COLUMN `target_type` INT DEFAULT 1 COMMENT '收藏目标类型 1攻略 2景点 3路线',
    ADD COLUMN `target_id`   INT DEFAULT NULL COMMENT '收藏目标ID';

-- 关键：strategy_id 原本 NOT NULL，必须改成允许为空（否则收藏景点/路线会报
-- "Column 'strategy_id' cannot be null"，因为景点/路线收藏没有 strategy_id）
ALTER TABLE `collect` MODIFY COLUMN `strategy_id` INT NULL COMMENT '收藏的攻略ID(仅攻略类型有值)';

-- 历史数据回填：把原来的 strategy_id 迁移到 target_type/target_id
UPDATE `collect` SET `target_type` = 1, `target_id` = `strategy_id` WHERE `target_type` IS NULL;
```

> 说明：`strategy_id` 列保留不动，新收藏同时会写 `target_id`（兼容旧逻辑），老数据回填后查询结果不变。

---

## 二、各接口详细说明

### 1. 后台首页统计卡片 `dashboard/stats`

| 项 | 内容 |
|---|---|
| 请求方式 | `GET` |
| 地址 | `/travel/dashboard/stats` |
| 参数 | 无 |

**返回示例：**
```json
{
  "code": 1,
  "msg": "找到数据！",
  "content": {
    "userCount": 12,      // 用户数
    "scenicCount": 48,    // 景点数
    "strategyCount": 36,  // 攻略数
    "orderCount": 89      // 订单数
  }
}
```

前端把原来的硬编码 `125/48/36/89` 换成该接口返回值即可。

---

### 2. 后台「用户信息」菜单 `myProfile`

复用当前用户的登录信息（不返回密码）。

| 项 | 内容 |
|---|---|
| 请求方式 | `GET` |
| 地址 | `/travel/travelUser/myProfile` |
| 参数 | 无。用户身份从请求头 `Authorization: Bearer <token>` 解析（未登录返回 401） |

**返回示例：**
```json
{
  "code": 1,
  "msg": "找到数据！",
  "content": {
    "id": 1,
    "account": "admin",
    "nickname": "管理员",
    "phone": "13800000000",
    "avatar": "/upload/xxx.png",
    "userRole": 1,
    "status": 1,
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-01 10:00:00"
  }
}
```

> 注意：`password` 字段已置空，不会返回。

---

### 3. 后台「修改密码」菜单 `updatePassword`

| 项 | 内容 |
|---|---|
| 请求方式 | `POST` |
| 地址 | `/travel/travelUser/updatePassword` |
| 请求头 | `Content-Type: application/json` |
| Body | `{ "oldPassword": "旧密码", "newPassword": "新密码" }`（`id` 已废弃，身份取 token） |

**成功返回：**
```json
{ "code": 1, "msg": "修改成功", "content": null }
```

**失败返回（原密码错误等）：**
```json
{ "code": 0, "msg": "原密码不正确或用户不存在", "content": null }
```

> 后端会校验原密码是否正确，新密码以 BCrypt 加密后入库。

---

### 4. 消息通知中心 `message/*`

#### 4.1 分页查询消息 `GET /travel/message/queryByPage`

| 参数 | 类型 | 说明 |
|---|---|---|
| userId.id | Integer | 接收人用户ID（必传，用于查某人的消息，GET 参数写 `?userId.id=1`） |
| type | Integer | 消息类型：0系统 1订单 2审核（可选） |
| isRead | Integer | 0未读 1已读（可选） |
| title | String | 标题模糊搜索（可选） |
| page / size | Integer | 页码从1开始 / 每页条数，默认 1/10 |
| orderCol / orderDirect | String | 排序字段 / ASC/DESC（可选） |

**返回示例：**
```json
{
  "code": 1,
  "msg": "找到数据！",
  "content": {
    "content": [
      {
        "id": 1,
        "userId": { "id": 1, "nickname": "admin" },
        "title": "订单提醒",
        "content": "您的景点门票订单已支付成功",
        "type": 1,
        "isRead": 0,
        "createTime": "2024-01-01 10:00:00"
      }
    ],
    "totalElements": 5
  }
}
```

#### 4.2 标记已读 `POST /travel/message/markRead`

支持单条 / 批量：

```json
// 单条
{ "id": 1 }
// 批量
{ "ids": [1, 2, 3] }
```

返回 `{ "code": 1, "msg": "标记成功", "content": 3 }`，`content` 为本次标记条数。

#### 4.3 未读数量 `GET /travel/message/unreadCount?userId=1`

返回：
```json
{ "code": 1, "msg": "找到数据！", "content": 3 }
```
`content` 即未读数量，用于消息中心小红点。

#### 4.4 新增消息（后台发送通知） `POST /travel/message/add`

```json
{
  "userId": { "id": 1 },
  "title": "审核通知",
  "content": "您的攻略已审核通过",
  "type": 2
}
```

#### 4.5 其它

- `GET /travel/message/queryById/{id}`
- `POST /travel/message/edit`（修改消息）
- `POST /travel/message/deleteById/{id}`（逻辑删除）

---

### 5. 用户发布攻略 / 路线（复用 strategy/add、route/add）

这两个接口本来就有，本次做了两点增强，用户端发布后**默认进入「待审核」状态**：

- `strategy/add`：不传 `auditStatus` 时默认 `0`（待审核，1已通过 2已驳回），并自动补 `collectCount=0`、`readCount=0`。
- `route/add`：不传 `status` 时默认 `0`（待审核，1已通过 2已驳回）。

用户端发布攻略：
```json
POST /travel/strategy/add
{
  "userId": { "id": 1 },
  "title": "我的第一篇攻略",
  "cover": "/upload/cover.jpg",
  "content": "攻略正文..."
}
```

用户端发布路线：
```json
POST /travel/route/add
{
  "userId": { "id": 1 },
  "name": "成都三日游",
  "dayCount": 3,
  "startCity": "成都",
  "traffic": "高铁+公交",
  "budget": 1500,
  "intro": "路线介绍"
}
```

> 后台发布时继续传 `auditStatus: 1` 或 `status: 1` 即为已通过，不受影响。前端「我的发布」列表用 `queryByPage` 按 `userId`（travelUser.id）过滤即可看到待审核内容。

---

### 6. 景点 / 路线收藏（collect 扩展 targetType）

`collect` 表新增 `targetType`（1攻略 2景点 3路线）和 `targetId` 字段，同一套接口支持攻略/景点/路线收藏。

#### 6.1 收藏 `POST /travel/collect/add`

三种目标类型写法（推荐直接传 targetType + targetId）：

```json
// 收藏景点
{ "userId": { "id": 1 }, "targetType": 2, "targetId": 9 }

// 收藏路线
{ "userId": { "id": 1 }, "targetType": 3, "targetId": 5 }

// 收藏攻略（兼容旧写法）
{ "userId": { "id": 1 }, "strategyId": { "id": 3 } }
```

#### 6.2 查询收藏列表 `GET /travel/collect/queryByPage`

| 参数 | 说明 |
|---|---|
| collectorId | 收藏用户ID（**新参数**，用于查「我的收藏」） |
| targetType | 1攻略 2景点 3路线（按类型过滤） |
| targetId | 目标ID（可选） |
| title | 攻略标题模糊搜索（可选） |

返回内容里根据 `targetType` 会带出对应对象：
- 攻略：`strategyId`（含作者信息）
- 景点：`scenicId`（含分类 `categoryId`）
- 路线：`routeId`

#### 6.3 判断是否已收藏 `GET /travel/collect/checkCollected`

```
/travel/collect/checkCollected?userId=1&targetType=2&targetId=9
```
返回 `content: true / false`，用于收藏按钮回显。

#### 6.4 取消收藏 `POST /travel/collect/deleteByUserAndTarget`

```json
{ "userId": { "id": 1 }, "targetType": 2, "targetId": 9 }
```

> 旧的 `deleteById/{id}` 仍然可用。

---

### 7. 路线途经景点按路线过滤（routeDetail/queryByPage 增加 routeId）

| 项 | 内容 |
|---|---|
| 请求方式 | `GET` |
| 地址 | `/travel/routeDetail/queryByPage?routeId=5&page=1&size=100` |
| 新增参数 | `routeId`：路线ID，传入后只返回该路线的途经景点 |

之前前端是拉全量 100 条再客户端过滤，现在直接由后端按 `routeId` 过滤分页。不传 `routeId` 时行为与原来一致。

---

## 三、前端对接注意事项

1. **收藏类型约定**：`targetType` 统一为 `1=攻略，2=景点，3=路线`，前后端保持一致。
2. **消息过滤**：消息中心查询不再需要传 `userId`（后端按 token 取当前用户）；管理员可传 `userId.id`（嵌套写法，如 `?userId.id=1`），直接传 `userId=1` 会因类型转换失败报 400。
3. **我的收藏 / 我的发布**：`collectorId`、`userId` 已由后端按 token 自动填充，前端不必再传（传了也会被覆盖）。
4. **token**：所有接口都需要 `Authorization: Bearer <token>`（登录、注册、接口文档除外）；`myProfile` **不再接受 `?id=`**，一律返回当前登录人信息。
5. **新表/新列**：上线前先执行第一节的 SQL。
