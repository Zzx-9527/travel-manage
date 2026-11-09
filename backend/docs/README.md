# 项目文档索引

> 所有项目文档统一放在 `docs/` 目录，根目录只保留 `README.md`。
> 新增文档请放入本目录，并在下表登记。

## 文档清单

| 文档 | 内容 | 状态 |
|---|---|---|
| [项目介绍.md](项目介绍.md) | 项目背景、功能范围与模块划分 | 现行 |
| [前端接口对接说明.md](前端接口对接说明.md) | 前端接口契约：通用约定、各模块接口、**第 8 节 Sa-Token 鉴权契约** | 现行，第 8 节待落地 |
| [新增API接口说明.md](新增API接口说明.md) | 后续新增接口的字段与用法 | 现行 |
| [collect接口说明.md](collect接口说明.md) | 收藏模块接口细节 | 现行 |
| [scenicOrders接口说明.md](scenicOrders接口说明.md) | 门票订单模块接口细节（库存版，含取消/模拟支付/库存查询） | 现行 |
| [plan-ticket-stock.md](plan-ticket-stock.md) | 门票库存、事务下单、取消回补、迁移与并发验收的执行规划 | 后端已实现，测试待隔离库执行 |
| [sql/2026-11-09_ticket_stock_precheck.sql](sql/2026-11-09_ticket_stock_precheck.sql) | 库存迁移前只读检查 | 已执行 |
| [sql/2026-11-09_ticket_stock_migration.sql](sql/2026-11-09_ticket_stock_migration.sql) | 库存表与历史订单状态迁移 | 已由用户执行并核验 |
| [消息模块后端实现方案.md](消息模块后端实现方案.md) | 消息中心的数据模型与实现方案 | 现行 |
| [订单与审核消息自动发送-后端实现说明.md](订单与审核消息自动发送-后端实现说明.md) | 业务事件触发消息通知的实现说明 | 现行 |
| [redis-integration-plan.md](redis-integration-plan.md) | **技术栈治理 + Redis / Redisson / Sa-Token 接入规划**（唯一执行基准） | 执行中 |
| [performance-report.md](performance-report.md) | 景点详情接口压测报告：热缓存 vs 直查库、QPS/P95/P99 对比、复现命令 | 已完成 |
| [HELP.md](HELP.md) | Spring Initializr 生成的原始说明 | 参考，可删 |

## 阅读顺序建议

1. 先看 [项目介绍.md](项目介绍.md) 了解业务范围；
2. 前端同学看 [前端接口对接说明.md](前端接口对接说明.md)，其中第 8 节是鉴权契约（Phase 3/4 落地后生效）；
3. 后端/维护者看 [redis-integration-plan.md](redis-integration-plan.md)，里面包含技术栈调整、阶段计划与验收标准。

## 文档维护约定

- 位置：所有说明类文档放 `docs/`，根目录只留 `README.md`。
- 命名：中文文档可用中文名；新增规划类文档建议 `plan-xxx.md`、接口类建议 `api-xxx.md`，避免出现 `新增`、`临时` 这类会过期的词。
- 状态：文档开头标注版本与最后更新日期；计划类文档需在文末维护「变更记录」。
- 交叉引用：文档间引用使用相对路径，例如 `[前端接口对接说明.md](前端接口对接说明.md)`。
- 同步：接口契约变更时，先改文档再改代码；代码合入后回填文档状态。
