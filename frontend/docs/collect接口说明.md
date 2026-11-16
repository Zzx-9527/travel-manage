# 收藏模块接口契约（collect）

> 适用前端：AI 前端开发请严格按照本文档字段名与取值实现，勿使用已废弃的 `strategyId`。

## 本次重要变更

- **`strategy_id` 字段已从 `collect` 表删除**，禁止再传/再读 `strategyId`。
- 收藏目标统一用两个字段标识：

| 字段 | 含义 | 取值 |
| --- | --- | --- |
| `targetType` | 目标类型 | `1`=攻略，`2`=景点，`3`=路线 |
| `targetId` | 目标ID | 对应攻略/景点/路线表的 `id` |

- 收藏查询结果中**不再内嵌 `strategyId` 攻略对象**。攻略类型的收藏，前端须用 `targetId` 调攻略详情接口获取攻略信息。

## 公共信息

- **Base URL**：`http://<host>:<port>/travel/collect`（服务 context-path 为 `/travel`）
- **统一返回结构（ReturnVO）**：

```json
{
  "code": 1,          // 1=成功，0=失败
  "msg": "提示消息",
  "content": null     // 数据；按接口而定
}
```

---

## 1. 新增收藏

- **Method / URL**：`POST /travel/collect/add`
- **请求体**（JSON）：

```json
{
  "userId": { "id": 1 },
  "targetType": 2,
  "targetId": 9
}
```

- **参数说明**：
  - `userId.id`：必填，收藏者用户ID。
  - `targetType` / `targetId`：推荐必填。后端在 `targetType` 为 null 时有兼容推断：传 `scenicId:{id}` → type=2；传 `routeId:{id}` → type=3；两者都没有则默认 type=1，此时 `targetId` 必须由前端填。
- **响应**：成功 `{"code":1,"msg":"添加成功"}`；失败 `{"code":0,"msg":"没有找到数据！"}`。

---

## 2. 取消收藏（按用户+目标）

- **Method / URL**：`POST /travel/collect/deleteByUserAndTarget`
- **请求体**（JSON）：

```json
{
  "userId": { "id": 1 },
  "targetType": 2,
  "targetId": 9
}
```

- **响应**：成功 `{"code":1,"msg":"取消成功"}`；`userId`/`targetType`/`targetId` 任一为空或未删除到记录 `{"code":0,"msg":"取消收藏失败"}`。

---

## 3. 判断是否已收藏

- **Method / URL**：`GET /travel/collect/checkCollected`
- **Query 参数**：`userId`、`targetType`、`targetId`
- **示例**：`GET /travel/collect/checkCollected?userId=1&targetType=2&targetId=9`
- **响应**：

```json
{
  "code": 1,
  "msg": "找到数据！",
  "content": true
}
```

- `content` 为布尔值 `true`/`false`。

---

## 4. 分页查询收藏列表

- **Method / URL**：`GET /travel/collect/queryByPage`
- **Query 参数**：

| 参数 | 说明 |
| --- | --- |
| `collectorId` | 按收藏用户ID过滤（查某人的收藏列表时传，值为用户ID） |
| `targetType` | 按目标类型过滤（1/2/3） |
| `targetId` | 按目标ID过滤 |
| `createTime` | 按创建时间模糊过滤 |
| `page` | 页码，从 1 开始，默认 1 |
| `size` | 每页条数，默认 10 |
| `orderCol` | 排序字段（如 `createTime`） |
| `orderDirect` | `ASC` / `DESC` |

- **响应**（`content` 为 Spring Data 分页对象，关键字段）：

```json
{
  "code": 1,
  "msg": "找到数据！",
  "content": {
    "content": [
      {
        "id": 1,
        "userId": {
          "id": 1,
          "nickname": "张三",
          "avatar": "http://..."
        },
        "targetType": 2,
        "targetId": 9,
        "scenicId": {
          "id": 9,
          "name": "西湖",
          "cover": "http://..."
        },
        "routeId": null,
        "collectorId": null,
        "createTime": "2026-08-13 12:00:00"
      }
    ],
    "totalElements": 20,
    "totalPages": 2,
    "number": 0,
    "size": 10,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

- **列表项字段说明**：
  - `targetType=2`（景点）→ 返回 `scenicId` 对象（含 `categoryId` 子对象），`routeId` 为 null。
  - `targetType=3`（路线）→ 返回 `routeId` 对象，`scenicId` 为 null。
  - `targetType=1`（攻略）→ `scenicId`、`routeId` 均为 null，**仅提供 `targetType=1` 与 `targetId`，攻略详情需前端用 `targetId` 调攻略接口获取**。
  - `collectorId` 仅用于查询过滤，响应中通常为 null。
  - 无数据时返回 `{"code":0,"msg":"没有找到数据！"}`。

---

## 5. 按ID查询单条收藏

- **Method / URL**：`GET /travel/collect/queryById/{id}`
- **示例**：`GET /travel/collect/queryById/1`
- **响应**：`content` 为单条收藏对象，字段与第 4 节列表项一致。

---

## 6. 修改收藏

- **Method / URL**：`POST /travel/collect/edit`
- **请求体**（JSON，仅传需要修改的字段，`id` 必传）：

```json
{
  "id": 1,
  "userId": { "id": 1 },
  "targetType": 3,
  "targetId": 7
}
```

- **响应**：成功 `{"code":1,"msg":"修改成功"}`。

---

## 7. 按ID删除收藏

- **Method / URL**：`POST /travel/collect/deleteById/{id}`
- **示例**：`POST /travel/collect/deleteById/1`
- **响应**：成功 `{"code":1,"msg":"删除成功"}`。

---

## 前端实现清单（对照旧版改动）

1. 删除所有 `strategyId` 传参与读取。
2. 新增收藏攻略：`{ userId:{id}, targetType:1, targetId:攻略id }`。
3. 查询某人收藏列表：分页接口传 `collectorId=<用户id>`。
4. 攻略类型收藏的列表展示：用 `targetId` 另行请求攻略详情，不依赖收藏列表内嵌。
5. 取消收藏：`deleteByUserAndTarget` 三字段齐全。
