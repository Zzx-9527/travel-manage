# AI 行程规划模块说明

> 适用：面试讲解、后续维护、以及"这块为什么要这么写"的追溯。
> 代码位置：`backend/src/main/java/com/zjx/travel/ai/`
> 前端页面：`frontend/src/views/user/AiPlanner.vue`（路由 `/travel/home/ai`）

---

## 1. 一句话

用户用"表单 + 一句补充"描述需求，模型在**平台已上架的景点**里挑出按天行程，
服务端二次校验后落库成"我的路线"，全程支持流式输出与每日配额。

---

## 2. 能力边界

**做**：按天排景点、给推荐理由与停留时长、流式展示生成过程、保存为待审核路线。

**不做**（且刻意不做）：

| 不做 | 原因 |
|---|---|
| 让模型编造库外景点 | 编出来的景点点不开、买不了票，是虚假功能 |
| 让模型给价格/坐标 | 事实数据以数据库为准，否则会出现"故宫门票 10 块" |
| 多轮对话澄清 | 表单字段已经足够，多轮会引入会话状态，收益不匹配成本 |
| 向量检索 / RAG | 本次范围是"排行程"，候选集就是全量在售景点，用不上向量库 |

---

## 3. 端到端链路

```
前端表单
  │  destination / days / budget / preference / extra
  ▼
POST /ai/plan/stream  ──►  SecurityUtil.currentUserId()   ← 必须在这里取，进线程池就拿不到了
  ▼
ai-stream 线程池（4 线程）
  │
  ├─ 1. 参数归一化      天数夹到 1-7，目的地必填，文本截断
  ├─ 2. 并发闸          Redis SETNX ai:running:{userId}，防同一用户连点
  ├─ 3. 配额闸          Redis INCR ai:quota:{userId}:{yyyyMMdd}，超限直接错误事件
  ├─ 4. 取候选景点      scenic WHERE del_mark=1 AND status=1，只保留名称/地址命中目的地的
  │                     没有命中就直接报"未收录"，**不回退全量候选**
  ├─ 5. 组装 Prompt     system 固定规则 + user 需求与候选清单
  ├─ 6. 调模型          ChatClient.stream()，逐块推 SSE delta
  ├─ 7. 解析           剥 ``` 围栏 → Jackson → AiPlanResult
  ├─ 8. 校验           幻觉 id / 重复景点 / 越界天数 / 单日超量
  ├─ 9. 补全事实        name/price/score/address/经纬度/封面 全部按 id 回填
  └─ 10. 推 plan + done
  ▼
前端渲染按天卡片  ──►  POST /ai/plan/save  ──►  route + route_detail（status=0 待审核）
```

同步版 `POST /ai/plan` 走同一套编排，只是不推 delta，一次性返回。

---

## 4. 关键设计

### 4.1 为什么是 Spring AI + OpenAI starter，而不是 DeepSeek 专用 SDK

- 项目在 2026-11 从 Boot 2.7 / Java 8 升到 **Boot 3.5.13 / Java 17**，这是 Spring AI 1.x 的前置条件（1.1.x ↔ Boot 3.5.x）。
- DeepSeek 的接口**兼容 OpenAI 协议**，所以只引 `spring-ai-starter-model-openai`，把 `base-url` 指到 `https://api.deepseek.com` 即可。换通义、Kimi、智谱、本地 vLLM 都只改配置，不动代码。
- 没有引厂商专用 starter，避免多一层绑定。

### 4.2 Prompt：把候选景点做成"封闭世界"

`PlanPromptBuilder` 干两件事：

1. **候选清单进 user，固定规则进 system**。DeepSeek 有自动上下文缓存，system 部分逐字节不变才能命中前缀缓存，长期调用能省一笔钱。
2. **清单每行只有 5 个字段**：`id | 名称 | 地址 | 票价 | 评分`。字段多了 Prompt 变长、模型注意力被稀释；地址保留是因为它决定"顺不顺路"。

规则里最关键的一句是"**只能使用候选清单中出现的景点**"，配合服务端校验形成两道闸。

### 4.3 防幻觉三道闸

| 闸 | 位置 | 拦什么 |
|---|---|---|
| ① 封闭候选集 | Prompt | 从源头把可选范围压到库内景点 |
| ② ID 白名单 | `AiPlanService.validateAndEnrich` | 模型仍然可能编 id：不在候选 Map 里的整条丢弃，计入 `droppedSpots` |
| ③ 事实数据回填 | 同上 | 名称、价格、评分、坐标、封面一律用库里的值覆盖模型输出 |

`droppedSpots` 不只是个数字，它是**模型幻觉率的观测指标**：大于 0 说明这次生成有编造，
持续偏高就该调整 Prompt 或候选集。

其他校验：`dayNo` 越界丢弃、同景点全程去重、单日超 `max-spots-per-day` 截断、
缺失的天补成"自由活动"、全部为空则直接报错。

### 4.4 流式：SseEmitter 的三个必要细节

1. **独立线程池**（`AiExecutorConfig`，4 线程、daemon）。`SseEmitter` 要求 Controller 先返回、
   再在别的线程写数据；如果直接在 Tomcat 工作线程里阻塞读模型流，几个并发就把请求线程占满。
2. **断开即停**。`sendEvent` 返回 false（客户端断开）时直接 return，停止读取上游流，
   否则用户关了页面，后台还在烧 token。
3. **不用 EventSource**。它不支持自定义请求头，带不了 `Authorization: Bearer`；
   把 token 放 query 会进访问日志。所以前端用 `fetch` + `ReadableStream` 手解 SSE 帧。

事件类型：`delta`（增量文本）/ `plan`（校验后的结构）/ `done`（耗时）/ `error`（可读错误）。

### 4.5 配额：按自然日分桶，故障放行

- key：`travel:v1:ai:quota:{userId}:{yyyyMMdd}`，首次 `INCR` 后把 TTL 补到次日 0 点。
  日期在 key 里，所以不需要定时任务判断跨天。
- **Redis 挂了直接放行**。配额是成本控制手段，不该反过来把功能拖死；失败只记日志。
- 计数在调用模型**之前** +1，失败的请求也计入，避免"失败重试刷次数"。
- 并发保护另用 `SETNX ai:running:{userId}`（TTL 120 秒），同一用户同时只允许一条在跑，
  进程崩溃时靠 TTL 自动释放。

### 4.6 保存：复用现有表，不绕审核

- 写入 `route`（`status=0` 待审核）+ `route_detail`（天、顺序、停留时长）。
- 一个 `@Transactional` 方法内完成；景点全部无效时抛异常回滚，不留空行程。
- 归属人取当前登录用户，**不接受前端传的 userId**。
- 保存前再用 `scenic` 表校验一次 id，防止前端伪造。
- `route_detail` 没有存"推荐理由"的字段，理由只在生成结果里展示，落库时丢失——
  这是已知取舍，要保留就得加列，本次按"不动现有表"的约束放弃。

---

## 5. 数据契约

请求 `POST /ai/plan`、`/ai/plan/stream`：

```json
{ "destination": "北京", "days": 2, "budget": 1500, "preference": "历史", "extra": "不想太累" }
```

响应 `content`：

```json
{
  "title": "北京历史轻松两日游",
  "intro": "精选北京核心历史景点，行程宽松不累。",
  "days": [
    { "dayNo": 1, "theme": "皇城历史",
      "spots": [
        { "scenicId": 2, "stayTime": "3小时", "reason": "明清皇宫，历史底蕴深厚",
          "name": "故宫博物院", "price": 60.0, "score": 4.8,
          "address": "北京市东城区景山前街4号", "longitude": 116.39748, "latitude": 39.91728,
          "cover": "https://..." }
      ] },
    { "dayNo": 2, "theme": "自由活动", "spots": [] }
  ],
  "tips": ["故宫需提前预约门票"],
  "model": "deepseek-chat", "latencyMs": 1815, "droppedSpots": 0
}
```

模型只被要求返回 `scenicId / stayTime / reason`，其余字段由服务端补。

---

## 6. 失败与降级矩阵

| 情况 | 用户看到 | 服务端行为 |
|---|---|---|
| 未配 Key | `AI 未配置 API Key…`（`/ai/health` 的 message） | 应用照常启动，`enabled=false` |
| 目的地为空 | 请先填写目的地 | 归一化阶段拦截 |
| 目的地未收录 | 平台暂未收录「丽江」的景点…当前收录：… | `loadCandidates` 直接抛业务异常；不扣配额、不调模型 |
| 每日配额超限 | 今天的 AI 规划次数已用完（每天 N 次） | 同步返回 `code=0`，流式回 `error` 事件 |
| 同一用户并发 | 上一条行程还在生成中，请稍候 | `SETNX` 抢锁失败 |
| Key 无效 / 403 | AI Key 无效或无权限，请联系管理员 | 解析 401/403 |
| 上游限流 | AI 服务当前限流，请稍后再试 | 解析 429 |
| 上游超时 | AI 服务响应超时，请稍后重试 | `ResourceAccessException` / `SocketTimeoutException` |
| 返回不是 JSON | AI 返回格式异常，请重试 | 记原始内容前 500 字，便于排查 |
| 全部景点被过滤 | AI 没能从现有景点中选出合适行程 | `droppedSpots` 记录，抛业务异常 |

所有用户可见错误都是**人话**；堆栈、上游报文只进日志。

---

## 7. 踩坑记录

**① Knife4j 4.5 在 Boot 3.4+ 上必崩**
它内置 springdoc 2.3.0，调用了 Spring 6.2 已删除的 `ControllerAdviceBean(Object)`；
强升 springdoc 又会撞 `SpringDocConfigProperties.getGroupConfigs()` 改名。
最终换官方 `springdoc-openapi-starter-webmvc-ui`，文档地址从 `/doc.html` 变为 `/swagger-ui/index.html`。

**② 404 错误页会让拦截器二次鉴权**
错误页转发（`DispatcherType.ERROR`）会再次进入 `SaInterceptor`，此时 Sa-Token 的
ThreadLocal 上下文已清理，抛 `SaTokenContextException`，把 404 变成 500。

**③ SSE 结束时同样有二次派发**
`SseEmitter` 完成后 Tomcat 会做一次 `DispatcherType.ASYNC` 派发，症状和 ② 一样，
只是每个流式请求收尾都会刷一屏堆栈。

②③ 同一个根因：**一个请求会多次进入拦截器，只有首次 REQUEST 派发该鉴权**。
修法是在 `WebConfig` 里对 ERROR / ASYNC 直接放行（Spring Security 的过滤器默认也是这么做的）。

---

## 8. 实测数据

| 场景 | 结果 |
|---|---|
| 同步生成（北京 2 天） | 1.80s，故宫 + 颐和园，`droppedSpots=2` → 调整提示词后降到 0 |
| 流式生成 | 180-211 个 `delta` 帧 + plan + done，端到端 2.0-2.15s |
| 配额限流 | 上限设 1 时，第二次请求被拦，同步/流式均正确 |
| 保存落库 | `route`（status=0）+ 2 条 `route_detail`，字段与预期一致 |
| 回归 | 44 个用例，0 失败（1 个真实调用用例默认跳过） |

---

## 9. 面试问答预案

**Q：为什么要做服务端校验，模型输出的 JSON 不能直接用吗？**
> DeepSeek 只支持 `json_object` 模式，能保证是合法 JSON，但**不保证字段结构**，
> 更不保证 id 真实存在。所以必须有第二道闸：id 白名单 + 事实数据回填。

**Q：流式为什么不用 WebSocket？**
> 需求是单向推送，SSE 基于 HTTP、天然穿透代理、断线可重连；WebSocket 的双向能力用不上，
> 还要额外处理握手和心跳。

**Q：为什么不用 EventSource？**
> 它不能带自定义请求头，`Authorization` 塞 query 会进日志。所以用 `fetch` + `ReadableStream`
> 自己解析 `event:` / `data:` 帧。

**Q：模型超时或限流了怎么办？**
> 先按异常类型翻译成人话（429/超时/Key 无效/5xx），再靠每日配额和并发闸控制总量。
> 没有做自动重试：生成类请求重试会重复计费，让用户自己决定更合理。

**Q：为什么不用 RAG？**
> 候选集就是全部在售景点（当前 11 条，生产也就几百条），直接放进 Prompt 比建向量库简单得多。
> 景点上千条时再考虑"按城市预筛 + 向量召回"。

**Q：怎么控制成本？**
> 四层：每日配额、同一用户并发闸、候选集上限 40 条（控制 Prompt 体积）、
> system 固定以命中 DeepSeek 上下文缓存。

**Q：这套东西如果换成别的模型要改多少？**
> 只改 `spring.ai.openai.base-url` 和 `model` 两行配置。代码里只有 `ChatClient`，
> 没有任何厂商 SDK 的痕迹。

**Q：你觉得这块最大的不足是什么？**
> 没有 token 用量与失败原因的持久化（`ai_call_log`），成本只能靠日志估算；
> 也没有对生成质量做评估。这两点是我下一步要补的。

---

## 10. 已知限制与下一步

| 限制 | 影响 | 下一步 |
|---|---|---|
| 景点数据只有 11 条 | 用户输入的大部分城市会直接提示"未收录"；库里有的城市若景点少，某天会自由活动 | 补数据——这是唯一根治办法，不要靠"回退全量"糊弄 |
| 没有 token 用量落库 | 成本只能估算 | 加 `ai_call_log`（表结构已设计，未落） |
| 生成质量无评估 | 无法量化好坏 | 抽样人工打分，或引入 LLM-as-judge |
| 推荐理由不落库 | 保存后详情页看不到理由 | 给 `route_detail` 加列（需改现有表） |
| 无自动重试 | 偶发失败需用户手动重试 | 若加，必须区分"可重试"与"已计费" |
