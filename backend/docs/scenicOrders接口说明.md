# 订单模块接口说明（scenicOrders）

> 适用前端：AI 前端开发请严格按照本文档字段名与取值实现。

## 本次重要变更

- **订单号 `orderNo` 改为后端自动生成**（需重启后端服务后生效），规则：`ORDER` + 年月日时分秒毫秒 + 3位随机数，例如 `ORDER20260906143102588123`。
- 新增订单时**前端不要传 `orderNo`**，传了也会被后端覆盖；数据库 `order_no` 为唯一索引。
- `createTime` 由后端自动填充，前端不传；`payTime` 初始为 null，支付完成后通过修改接口更新。

## 公共信息

- **Base URL**：`http://<host>:8081/travel/scenicOrders`（端口 8081，服务 context-path 为 `/travel`）
- **统一返回结构（ReturnVO）**：

```json
{
  "code": 1,          // 1=成功，0=失败
  "msg": "提示消息",
  "content": null     // 数据；按接口而定
}
```

- 时间字段格式：`createTime`/`payTime` 为 `yyyy-MM-dd HH:mm:ss`，`playDate` 为日期 `yyyy-MM-dd`。
- 订单对象内嵌 `userId`（下单用户对象）和 `scenicId`（景点对象，内含 `categoryId` 分类对象）。

---

## 1. 新增订单

- **Method / URL**：`POST /travel/scenicOrders/add`
- **请求体**（JSON，必填字段如下）：

```json
{
  "userId": { "id": 1 },
  "scenicId": { "id": 1 },
  "contactName": "张三",
  "contactPhone": "13800138000",
  "playDate": "2026-09-10",
  "ticketCount": 2,
  "totalAmount": 380.0,
  "payStatus": 0,
  "payType": 1
}
```

- **参数说明**：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `userId.id` | 是 | 下单用户ID |
| `scenicId.id` | 是 | 景点ID |
| `contactName` | 是 | 联系人姓名，最长 20 字 |
| `contactPhone` | 是 | 联系电话，最长 20 字 |
| `playDate` | 是 | 游玩日期，`yyyy-MM-dd` |
| `ticketCount` | 是 | 票数，正整数 |
| `totalAmount` | 是 | 订单总金额，保留 2 位小数；后端**不校验**是否等于 单价×票数，由前端计算后传入 |
| `payStatus` | 是 | 支付状态编码，新增订单传 `0`（待支付）；现有数据取值 0/1/2，常见约定 0=待支付、1=已支付、2=已退款 |
| `payType` | 否 | 支付方式编码，现有数据取值 1/2 |
| `orderNo` | 传了无效 | 后端自动生成并覆盖 |
| `createTime` / `payTime` | 传了无效 | 后端处理 |

- **响应**：成功 `{"code":1,"msg":"添加成功"}`；失败（如必填字段缺失）`{"code":0,"msg":"没有找到数据！"}`。

---

## 2. 修改订单

- **Method / URL**：`POST /travel/scenicOrders/edit`
- **请求体**（JSON，`id` 必传，仅传需要修改的字段，未传的字段不会被更新）：

```json
{
  "id": 1,
  "payStatus": 1,
  "payTime": "2026-09-06 14:40:00"
}
```

- **典型场景**：支付成功 → 传 `payStatus: 1` 和 `payTime`；申请退款 → 传 `payStatus: 2`。
- **响应**：成功 `{"code":1,"msg":"修改成功"}`；失败 `{"code":0,"msg":"没有找到数据！"}`。

---

## 3. 按ID查询单条订单

- **Method / URL**：`GET /travel/scenicOrders/queryById/{id}`
- **示例**：`GET /travel/scenicOrders/queryById/1`
- **响应**：`content` 为单条订单对象，字段与第 4 节列表项一致；无数据 `{"code":0,"msg":"没有找到数据！"}`。

---

## 4. 分页查询订单列表

- **Method / URL**：`GET /travel/scenicOrders/queryByPage`
- **Query 参数**：

| 参数 | 说明 |
| --- | --- |
| `page` | 页码，从 1 开始，默认 1 |
| `size` | 每页条数，默认 10 |
| `orderCol` | 排序字段，如 `createTime`、`id`、`totalAmount` |
| `orderDirect` | `ASC` / `DESC` |
| `orderNo` | 按订单号**模糊**过滤 |
| `contactName` | 按联系人**模糊**过滤 |
| `contactPhone` | 按电话**模糊**过滤 |
| `playDate` | 按游玩日期**模糊**过滤（如 `2026-09`） |
| `ticketCount` | 精确匹配 |
| `totalAmount` | 精确匹配 |
| `payType` / `payStatus` | 精确匹配 |
| `id` | 按订单ID精确匹配 |

- **示例**：`GET /travel/scenicOrders/queryByPage?page=1&size=10&orderCol=createTime&orderDirect=DESC&payStatus=0`
- **响应**（`content` 为 Spring Data 分页对象，关键字段）：

```json
{
  "code": 1,
  "msg": "找到数据！",
  "content": {
    "content": [
      {
        "id": 1,
        "orderNo": "ORDER2025001",
        "userId": {
          "id": 1,
          "account": "user001",
          "nickname": "张三",
          "avatar": "https://..."
        },
        "scenicId": {
          "id": 1,
          "name": "黄山风景区",
          "cover": "https://...",
          "price": 190.0,
          "openTime": "06:00-18:00",
          "address": "安徽省黄山市黄山区汤口镇",
          "categoryId": { "id": 1, "name": "自然景观" }
        },
        "contactName": "张三",
        "contactPhone": "13800138000",
        "playDate": "2026-09-10",
        "ticketCount": 2,
        "totalAmount": 380.0,
        "payType": 1,
        "payStatus": 0,
        "createTime": "2026-09-06 14:31:02",
        "payTime": null
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
  - `userId` 内嵌完整用户对象（含 `password` 密文字段，前端忽略即可，勿展示）。
  - `scenicId` 内嵌景点对象与 `categoryId` 分类对象，可直接用于订单列表展示，无需再查景点详情。
  - 无数据时返回 `{"code":0,"msg":"没有找到数据！"}`。

### ⚠️ 已知限制（前端规避）

1. **不支持按用户查订单**：本接口没有 `userId` 过滤参数，"我的订单"功能当前无法实现，需后端在查询条件中补充 `user_id` 过滤后再对接。
2. **禁止传 `scenicId.*` 过滤参数**（如 `scenicId.id=1`）：当前实现会触发后端 500 错误，按景点筛选需后端修复后再用。
3. `orderCol` 的值会直接作为排序列名，请只使用上文列出的字段名。

---

## 5. 删除订单（软删除）

- **Method / URL**：`POST /travel/scenicOrders/deleteById/{id}`
- **示例**：`POST /travel/scenicOrders/deleteById/1`
- **说明**：实际是把记录的 `del_mark` 置 0，之后所有查询接口均不再返回该订单。
- **响应**：成功 `{"code":1,"msg":"删除成功"}`；失败 `{"code":0,"msg":"没有找到数据！"}`。

---

## 前端实现清单

1. 新增订单：必传 `userId.id`、`scenicId.id`、`contactName`、`contactPhone`、`playDate`、`ticketCount`、`totalAmount`、`payStatus`；**不要传 `orderNo`**。
2. 下单成功后如需展示订单号，重新调 `queryById` 获取后端生成的 `orderNo`。
3. 支付/退款流程：调 `edit` 更新 `payStatus`（和 `payTime`），不要重复下单。
4. 订单列表：`queryByPage` 分页参数 + 过滤参数，过滤参数只使用第 4 节列出的项。
5. 删除订单走 `deleteById/{id}`，为软删除，无需二次确认数据库状态。
