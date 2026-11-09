# 门票订单接口说明（scenicOrders）

> 版本：v2.0；最后更新：2026-11-09。数据库迁移已完成，**后端库存版功能正在实现/验收**；新接口只在部署新版后端后生效，前端需同步替换旧的编辑支付、删除取消逻辑。具体实施状态见 [库存规划](plan-ticket-stock.md)。

## 通用约定

- Base URL：`http://localhost:8081/travel`；除登录/注册白名单外需 `Authorization: Bearer <token>`。
- 返回仍为 `ReturnVO {"code":1,"msg":"...","content":...}`。业务失败 HTTP 200 + `code=0`；未登录 HTTP 401，越权 HTTP 403。
- 订单 `status`：0 待支付、1 已支付、2 已取消、3 已完成（完成入口本期不开放）。`payStatus` 是兼容旧数据的字段：0 未支付、1 已支付、**2 仅代表历史退款**，不是“免费订单”或新的取消状态。请以 `status` 为 UI 主状态。
- `cancelReason`：`MANUAL` 手动、`TIMEOUT` 超时、`LEGACY_UNPAID` 历史未支付取消、`LEGACY_REFUNDED` 历史退款；`cancelTime` 记录取消时间。`unitPrice` 是订单单价快照；历史订单可能为 null（不能用当前景区价冒充历史价）。
- 仅允许购买 **Asia/Shanghai 当天到当天 +29 天**共 30 个自然日；当天到景区 `dayTicketCutoffTime`（默认 17:00）即停止售票；只能购买系统已生成库存的日期。下单时占用库存，未支付满 30 分钟自动取消并回补；已取消订单保留可见，不自动逻辑删除。

## 1. 下单 `POST /scenicOrders/add`

```json
{
  "scenicId": {"id": 1},
  "playDate": "2026-11-10",
  "ticketCount": 2,
  "contactName": "张三",
  "contactPhone": "13800138000",
  "payType": 1
}
```

必填景区、合法日期、正数票数、联系人姓名/电话；当前仅成人票 `ticketTypeId=1`。无需且不得依赖客户端提供的 `userId`、`totalAmount`、`unitPrice`、`payStatus`、`status`、`orderNo`：身份取 token，价格取景区当前价，金额服务端计算；新单状态均为 0，支付时间为 null。成功返回 `content: {"id": 123, "orderNo": "ORDER..."}`；库存不存在提示“暂未开放”，余票不足提示“已售罄/库存不足”，均不生成订单。每用户 5 秒防重复提交，防超卖由 MySQL 条件扣减与事务保证，不依赖 Redis 防重。

## 2. 取消 `POST /scenicOrders/cancel/{id}`

本人或管理员可取消**待支付**订单；订单状态原子改为 `status=2`，库存仅回补一次，历史订单仍可在订单列表查看。已支付、已取消等不可再次取消；跨用户返回 HTTP 403。历史订单在迁移前未占用新库存，不因历史取消而回补。

## 3. 模拟支付 `POST /scenicOrders/pay/{id}`

仅订单本人；订单仍处于待支付、创建未满 30 分钟才可支付（不接真实网关）。成功后 `status=1`、`payStatus=1`、`payTime` 有值；不扣减第二次库存。已到期时即使超时任务尚未来得及执行也不能支付。**不要通过 `/scenicOrders/edit` 修改 `payStatus` 或 `payTime`**。

## 4. 查询列表和详情

- `GET /scenicOrders/queryByPage?page=1&size=10`：普通用户仅看本人订单；管理员可查全部。`content.content` 是订单列表；可按 `status`、`orderNo`、`playDate` 等字段筛选（以实际接口映射为准）。
- `GET /scenicOrders/queryById/{id}`：查询单条订单；非本人/非管理员返回 HTTP 403。订单对象保留 `userId`、`scenicId` 关联结构，同时新增 `status`、`cancelTime`、`cancelReason`、`unitPrice`、`ticketTypeId`。
- 自动取消原因是 `TIMEOUT` 时展示“已超时取消”；历史 `LEGACY_REFUNDED` 展示“历史退款（已取消）”。订单软删标记为 0 的迁移前旧记录仍维持原先不可见状态。

## 5. 编辑与删除边界

- `POST /scenicOrders/edit`：仅能修改本人**待支付**订单的联系人信息，例如 `{"id":123,"contactName":"李四","contactPhone":"13800138000"}`。不可修改用户、景区、日期、数量、金额、订单号、支付或业务状态；多余字段不会成为修改权限。
- `POST /scenicOrders/deleteById/{id}`：待支付订单不能直接删除，应调用取消接口；取消历史需保留以供用户查询。删除不能被用作库存回补或逃避支付状态约束的入口。

## 6. 库存查询与后台配置

- `GET /scenic/stock?scenicId=1&playDate=2026-11-10`：可售余票取场地容量与成人票库存 `remaining` 的较小值；无库存记录为“暂未开放”。读值仅展示，下单时仍可能被其他用户抢完。
- 管理员 `POST /scenic/stock/adjust?scenicId=1&playDate=2026-11-10&capacity=100&adultStock=100`：安全调整单日总量，不得低于已占用数量；已售余量不被重置。
- 管理员可在景区编辑接口配置 `dayTicketCutoffTime`（`HH:mm` 或 `HH:mm:ss`，默认 17:00）、长期覆盖 `defaultCapacity` 与 `defaultAdultStock`（不配置则均为 100）。定时任务每天补齐今天至 +29 天，仅新增缺失记录，不覆盖旧库存；下架景区不生成新记录。

## 与旧接口文档相比必须修改的前端行为

1. 不再在下单时信任客户端金额、用户 ID、支付状态；成功后直接取响应中的订单 ID/订单号。
2. 不再用编辑接口“支付/退款”，模拟支付改 `/pay/{id}`；不再用逻辑删除代替取消，改 `/cancel/{id}`。
3. 不再把 `payStatus=2` 解释为免费；订单页显示新 `status` 和 `cancelReason`；待支付倒计时 30 分钟，但以后端判断为准。
