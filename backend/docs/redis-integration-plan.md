# 技术栈治理与 Redis / Redisson / Sa-Token 接入规划

> 版本：v1.18 ｜ 最后更新：2026-10-31 ｜ 状态：Phase 0.5–6 完成；安全加固 + 1.3 完成；新增自助改资料、改密身份修复
> 本文件是本项目技术栈调整、缓存、分布式锁、登录态的**唯一同步基准**。变更需在文末「变更记录」追加一行。
>
> **版本演进**
> - v1.0：Redis 选型与缓存规划
> - v1.1：引入 Redisson、Sa-Token（替代 JWT）
> - v1.2：新增 Phase 0.5 技术栈清理；锁策略改为"手写优先 + Redisson 兜底"
> - v1.3：Q1–Q7 拍板；新增 Phase 4（会话管理与角色权限）；分页缓存移出范围；压测转必做
> - v1.4：Phase 1 完成（RedisKeys / LockUtil / RedisUtil / 8 用例）；Redisson 后移至 Phase 5
> - v1.5：Phase 2 完成（景点详情缓存接入，6 个用例）；全量测试 19/19 通过
> - v1.6：Phase 0.5 完成（Knife4j + SpringDoc、Logback、HikariCP、jar 打包、删除 log4j/fileupload/ServletInitializer），jar 启动与读写全链路实测通过
> - v1.7：Phase 3 完成：Sa-Token 替换 JWT（登录/登出/拦截器/401-403 异常处理），删除 `JwtUtil`、`JwtInterceptor` 与 jjwt 依赖；实测会话在 Redis 中跨重启有效
> - v1.8：Phase 4 完成：在线用户列表（SCAN）、踢人下线、`StpInterface` + `@SaCheckRole("admin")` 角色鉴权；修正 Sa-Token key 前缀认知（`token-name` 决定前缀，不是固定 `satoken:`）
> - v1.9：Phase 5 完成：Redisson 3.27.2（核心包）+ Netty 统一 4.1.107、`RedissonConfig`、登录限流、订单防重；修正本地 Redis 无密码的配置错误；修复订单查询越权；补事故复盘（§13）
> - v1.10：登录限流由 Redisson `RRateLimiter` 改为**手写 Lua 固定窗口**——原实现每个 IP 产生 3 个无 TTL 的 key（配置 hash + `{name}:value` + `{name}:permits`），属慢性泄漏；现为 1 个 key、60 秒自动过期
> - **v1.11（本版）**：Phase 6 完成：新增 `travel.cache.enabled` 开关与 `tools/LoadTest.java`，完成 50/100/200 并发、热缓存 vs 无缓存对比，产出 `docs/performance-report.md`

---

## 0. 文档使用约定

- 每个 Phase 开始时状态改为 `进行中`，结束时改为 `已完成` 并补「验收记录」。
- 新增需求先写进本文档再动代码。
- 验收标准必须可执行、可观察（命令、现象、数字）。
- **替换/删除旧技术时，先跑通新链路再删旧代码**，每个 Phase 结束保证项目可启动。

---

## 1. 背景与目标

### 1.1 项目现状

| 项 | 内容 |
|---|---|
| 项目 | 旅游管理平台（景点 / 攻略 / 路线 / 订单 / 收藏 / 消息 / 后台统计） |
| 框架 | Spring Boot 2.7.5，Java 8，Maven，**war** 打包 |
| 持久层 | MyBatis-Plus 3.5.9 + MySQL 驱动 5.1.47 + **dbcp2** |
| 文档 | **springfox 3.0.0**（靠 `ANT_PATH_MATCHER` 打补丁兼容） |
| 日志 | **log4j 1.2.17**（代码零引用，仅有一个无人读取的 `log4j.properties`） |
| 鉴权 | JWT（jjwt 0.11.5），`JwtInterceptor` 已写好但**未启用** |
| Redis | 已接入 Lettuce + 自定义 `RedisTemplate`，连通性测试通过 |
| 端口/路径 | 8081，context-path = `/travel` |

**鉴权相关现状（决定迁移成本）**

- `TravelUserController.login` 调 `JwtUtil.generateToken`，返回 `{user, token}`。
- `TravelUserController.myProfile` 自己解析 `Authorization`，失败回退 `?id=`。
- 全项目**没有任何 Controller 读 `request.getAttribute("userId")`** → 换鉴权不引发大面积改动。
- `TravelUserEntity.userRole`：**0 = 普通用户，1 = 管理员**（列默认值 0）。
- 前端把 `token` 放 `Authorization` 头，带 `Bearer ` 前缀。

### 1.2 本轮已确认的技术调整

| # | 现状 | 调整为 | 判断依据 |
|---|---|---|---|
| 1 | springfox 3.0.0 | **Knife4j 4.5.0 + SpringDoc** | springfox 2020 年停更，与 Boot 2.6+ 存在 PathPattern 冲突，需靠配置补丁；springdoc 已是事实标准 |
| 2 | log4j 1.2.17 | **删除，改用 SLF4J + Logback** | log4j 1.x 2015 年 EOL，多个 CVE；且代码零引用，属纯死依赖 |
| 3 | commons-fileupload 1.3.1 | **删除** | 代码只用 Spring 的 `MultipartFile`，该依赖从未被使用；1.3.1 有 CVE-2023-24998 |
| 4 | war 打包 | **jar 打包** | 内嵌 Tomcat、单文件部署，是当前 Spring Boot 默认形态 |
| 5 | dbcp2 | **HikariCP** | Boot 默认池，性能与可观测性更好；原 `default-auto-commit=false` 本身是隐患 |

> **口径**：保留某项技术的唯一正当理由是「改造成本 > 收益」，且必须写明成本；不以"存量老项目也这样"作为理由。

### 1.3 目标（可验收）

| 编号 | 目标 | 验收口径 |
|---|---|---|
| G1 | 文档链路升级 | `/travel/doc.html` 打开 Knife4j，接口分组正常 |
| G2 | 连接池替换 | 启动日志出现 HikariPool，池上限 20 |
| G3 | 可执行 jar | `java -jar` 直接启动，无需外置 Tomcat |
| G4 | 热点读走缓存 | 景点详情二次请求不产生 SQL |
| G5 | 缓存故障可降级 | Redis 停机后读接口仍返回数据 |
| G6 | 登录态可失效 | 登出/踢人后旧 token 立即 401 |
| G7 | 简单锁不引入重框架 | 缓存击穿用手写 `setIfAbsent`，约 15 行 |
| G8 | 复杂并发有方案 | 登录限流用手写固定窗口（第 6 次被拒），防重走 Redisson |
| G9 | 在线用户可查可踢 | 管理员能列出在线用户并强制下线，被踢者立即 401 |
| G10 | 后台接口按角色鉴权 | 普通用户访问 `/dashboard/**` 返回 403 |
| G11 | 有可量化数据 | 详情接口冷/热 P95 与缓存命中率写入文档 |

### 1.4 非目标（明确不做）

| 不做 | 原因 |
|---|---|
| 升级 Spring Boot 3 / JDK 17 | 需动 30 处 `javax→jakarta`、SpringDoc jakarta 版、Sa-Token boot3 starter、`spring.data.redis.*`，约 1.5 天；**Q6 已确认不做** |
| **分页列表缓存** | **Q4 已确认不做**：会改列表返回结构（`PageImpl` → DTO），前端解析成本大于收益 |
| Redis 集群/哨兵 | 单机演示，生产方案写在文档即可 |
| Redisson 接管 `RedisConnectionFactory` | 会让已验证的 Lettuce 链路重新回归 |
| 布隆过滤器 | 数据量小，空值缓存已够（可选加分项） |
| 多级缓存、OAuth2、SSO、RBAC 管理界面 | 与项目规模不匹配 |

---

## 2. 技术选型

### 2.1 依赖清单（Phase 0.5 + 3 完成后的最终态）

| 坐标 | 版本 | 用途 | 状态 |
|---|---|---|---|
| `spring-boot-starter-web` | 2.7.5 | Web + 内嵌 Tomcat | 保留 |
| `spring-boot-starter-jdbc` | 2.7.5 | 数据源（自带 HikariCP） | 保留 |
| `mybatis-plus-boot-starter` | 3.5.9 | ORM | 保留 |
| `mysql:mysql-connector-java` | 5.1.47 | 驱动 | 保留（不动） |
| `spring-boot-starter-data-redis` | 2.7.5 | Lettuce | 保留 |
| `com.github.xiaoymin:knife4j-openapi3-spring-boot-starter` | **4.5.0** | 接口文档（SpringDoc + Knife4j UI） | **新增** |
| `cn.dev33:sa-token-spring-boot-starter` | **1.44.0** | 登录鉴权（Boot 2 专用） | **新增** |
| `cn.dev33:sa-token-redis-jackson` | **1.44.0** | 会话持久化到 Redis | **新增** |
| `org.redisson:redisson` | **3.27.2** | 分布式锁 / 防重（核心包） | **已引入（Phase 5）**，同时把 `netty.version` 统一到 4.1.107 |
| `spring-boot-starter-test` | 2.7.5 | 测试 | 保留 |
| `lombok` | 1.18.24 | 简化实体 | 保留 |
| `spring-security-crypto` | 2.7.5 | BCrypt | 保留 |
| `commons-io` | 2.11.0 | 文件工具 | 保留 |
| `spring-boot-devtools` | 2.7.5 | 热重启 | 保留 |
| ~~`springfox-boot-starter`~~ | 3.0.0 | — | **删除** |
| ~~`log4j:log4j`~~ | 1.2.17 | — | **删除** |
| ~~`commons-fileupload`~~ | 1.3.1 | — | **删除** |
| ~~`commons-dbcp2`~~ | — | — | **删除** |
| ~~`jjwt-api/impl/jackson`~~ | 0.11.5 | — | **Phase 3 删除** |

> HikariCP **不需要显式声明**：`spring-boot-starter-jdbc` 已内置，删掉 `spring.datasource.type` 后自动生效。

### 2.2 技术栈调整决策表

| 项 | 动作 | 成本 | 风险 | 收益 |
|---|---|---|---|---|
| springfox → Knife4j + SpringDoc | 换依赖 + 重写 `SwaggerConfig`（只有一个 `Docket`，无注解迁移） | 0.3 天 | 与 Boot 2.7.5 兼容性需实测 | 文档 UI 现代化、去掉补丁配置 |
| log4j 1.x → Logback | 删依赖、删 `log4j.properties` | 10 分钟 | 无（代码零引用） | 消除 CVE 与追问 |
| commons-fileupload 删除 | 删依赖 | 5 分钟 | 无（未被使用） | 减少 CVE 面 |
| war → jar | 改 `packaging`、删 `ServletInitializer` | 20 分钟 | 部署方式变化 | 单命令启动 |
| dbcp2 → HikariCP | 删依赖与旧配置，加 `hikari.*` | 20 分钟 | 原 `auto-commit=false` 语义变化，需回归写接口 | 默认池、指标更全 |

### 2.3 保留不动的技术（附理由）

| 技术 | 保留理由 | 备注 |
|---|---|---|
| Spring Boot 2.7.5 | 升 3.x 需连带迁移，约 1.5 天 | 2.7.5 的 OSS 支持已于 2023-11 结束，属已知技术债；Q6 确认不升 |
| JDK 8 | 与 Boot 2.7 + Lombok 1.18.24 组合稳定 | 同上 |
| MySQL 驱动 5.1.47 | 当前库连接正常，换 8.x 需重调时区/SSL/认证插件 | 若换 MySQL 8 服务器必须同步升级 |
| MyBatis-Plus 3.5.9 | 当前主线版本 | — |
| 手写分页（count + limit + `PageImpl`） | 可跑，且分页缓存已取消（Q4） | 无改造动机 |

### 2.4 关键决策说明（面试可直接用）

- **为什么 springfox 换 Knife4j + SpringDoc**：springfox 3.0.0 自 2020 年停更，与 Boot 2.6+ 的 `PathPatternParser` 冲突，只能降级匹配策略兼容；SpringDoc 基于 `HandlerMethod` 扫描，是当前标准实现，Knife4j 在其上提供分组与调试。项目无历史 swagger 注解，迁移只需换依赖 + 一个 `OpenAPI` Bean。
- **为什么删 log4j 1.x 而不是升 log4j2**：代码零处 `org.apache.log4j` 引用，`log4j.properties` 也不会被 Logback 读取——纯死依赖，直接删比桥接更干净。
- **为什么 war 改 jar**：jar + 内嵌 Tomcat 是 Boot 默认形态，部署只需 `java -jar`；war 仅在必须外置容器的场景有价值。
- **为什么 dbcp2 换 HikariCP**：Boot 默认池、性能更好、自带池指标；原 `default-auto-commit=false` 把连接默认设为手动提交，事务边界依赖 MyBatis-Spring 隐式提交，是易踩坑的配置。
- **为什么 JWT 换 Sa-Token**：JWT 无状态、签发后不可撤销，登出只是前端删 token；也无法踢人、无法"30 分钟无操作过期"。单体项目不需要跨服务无状态，改存 Redis 会话换取可撤销性，代价是每次鉴权一次 Redis 查询。
- **为什么锁分两套**：手写 `setIfAbsent` 适合"单 key、临界区短、操作幂等"的缓存重建；Redisson 适合"可重入、等待队列、看门狗续期、跨多资源"的限流与防重。

---

## 3. 现状基线（Phase 0 已完成）

| 交付物 | 文件 | 状态 |
|---|---|---|
| 依赖 | [pom.xml](pom.xml) 的 `spring-boot-starter-data-redis` | ✅ |
| 配置 | [application.properties](src/main/resources/application.properties) 的 `spring.redis.*` | ✅ |
| 配置类 | [RedisConfigConfiguration.java](src/main/java/com/zjx/travel/config/RedisConfigConfiguration.java) | ✅ |
| 验证测试 | [RedisConfigTest.java](src/test/java/com/zjx/travel/RedisConfigTest.java)（5 用例） | ✅ 通过 |

**已知约束（后续设计必须遵守）**

1. `PageImpl` 不可缓存（无无参构造器）——本版已取消分页缓存，该约束仅记录。
2. 实体需保留可访问的无参构造器。
3. 日期字段 `@JsonFormat` 存字符串，**丢毫秒**。
4. Redis 懒连接：服务未启动时应用能启动，故障首次调用才暴露 → 降级须覆盖首次调用。

---

## 4. 总体设计

### 4.1 分层

```
Controller            参数校验、返回封装；鉴权/角色拦截在此层生效
   ↓
Service(Impl)         业务语义：何时读缓存、何时失效、何时加锁
   ↓
RedisUtil / LockUtil  通用能力：缓存模板、手写锁、Redisson 锁、限流；统一异常兜底
   ↓
Lettuce(RedisTemplate)      Redisson(RedissonClient)      Sa-Token(SaTokenDao)
   缓存读写                     复杂锁 / 限流 / 防重            会话读写
```

**约束**
- `RedisTemplate` 只出现在 `RedisUtil`；`RedissonClient` 只出现在 `LockUtil`。
- 业务层不出现 `RedisTemplate` / `RedissonClient`；`StpUtil` 允许在 Controller 与拦截器使用。

### 4.2 Key 命名规范

**业务 key（`travel:v1:` 前缀）**

| 用途 | 示例 | 备注 |
|---|---|---|
| 景点详情 | `travel:v1:scenic:detail:12` | — |
| 详情空值哨兵 | 同 key，值存哨兵标记 | TTL 60s |
| 详情重建锁 | `travel:v1:lock:scenic:detail:12` | 手写锁，值为 UUID |
| 登录限流 | `travel:v1:ratelimit:login:{ip}` | 手写固定窗口（Lua `INCR`+`EXPIRE`），1 个 key、TTL 60s |
| 防重复提交 | `travel:v1:submit:order:{userId}` | Redisson `RBucket.trySet` / `RLock` |
| 浏览量 | `travel:v1:scenic:view:12` | `INCR` |

> 分页 key（`travel:v1:scenic:page:*`）随 Q4 取消，不再使用。

**框架 key（禁止手写、禁止清理）**

| 来源 | 前缀 | 说明 |
|---|---|---|
| Sa-Token | `{token-name}:login:*`（本项目为 `Authorization:login:*`） | **前缀跟随 `sa-token.token-name`，不是固定的 `satoken:`**；`redis-cli` 可观察，禁人工 `DEL`（会强制全员下线） |
| Redisson | 直接使用业务锁名 | 内部以 Hash 存储 |

**规范**：全小写、冒号分隔、无中文；单 key < 128 字节；业务 key 必须带 TTL。

### 4.3 缓存策略矩阵

| 数据 | 读路径 | TTL | 失效时机 | 一致性 |
|---|---|---|---|---|
| 景点详情 | Cache-Aside | 30min ± 10% | update/delete 后 `DEL` | 最终一致 |
| 详情空值 | 哨兵值 | 60s | 新增后 `DEL` | 允许 60s 内"不存在" |
| 登录态 | 登录写入，拦截器读取 | `timeout=24h` + `active-timeout=30min` | 登出/踢人 `DEL` | **强一致** |
| 浏览量（可选） | `INCR` | 长期 | 回写 DB 后 `DEL` | 可少量丢失 |

**统一原则**：写操作**先更库、后删缓存**；删除失败重试 1 次 + ERROR 日志，TTL 兜底。

### 4.4 三大缓存问题与锁选型

| 问题 | 方案 | 用的锁 |
|---|---|---|
| 穿透 | 参数校验 + **空值缓存 60s** | 不需要 |
| 击穿 | **互斥重建**：`setIfAbsent` 成功后回源，失败者短暂自旋后重读 | **手写锁** |
| 雪崩 | TTL 随机抖动 ±10% + 降级回源 | 不需要 |

**锁选型标准（本项目执行口径）**

| 判据 | 手写 `setIfAbsent` | Redisson `RLock` |
|---|---|---|
| 临界区操作是否幂等 | 是（缓存重建/查询） | 否（扣减/下单） |
| 是否需要可重入 | 否 | 是 |
| 是否需要公平锁/等待队列 | 否 | 是 |
| 是否跨多个资源 | 否 | 是 |
| 业务耗时是否可预估（< TTL） | 是 | 否，需要看门狗续期 |
| 结论 | 缓存击穿、短任务、固定窗口限流 | 可重入/公平锁、防重、订单类 |

**手写锁的最小正确实现**：
1. `setIfAbsent(key, uuid, 30s)` —— 加锁与过期一步完成，避免 `setnx` + `expire` 的非原子窗口。
2. 值为 UUID，释放前比对，**用 Lua 脚本 compare-and-delete**，避免删掉别人的锁。
3. 释放放 `finally`，捕获异常不掩盖业务异常。
4. TTL ≥ 临界区最长耗时的 3 倍。

**为什么缓存击穿不用 Redisson**：临界区只有"查库 + 回写"，幂等、耗时 < 100ms、单 key，15 行足够；引入 Redisson 只是多一条连接。Redisson 留给需要续期、可重入、等待队列的场景（§4.8）。

### 4.5 一致性与降级

**一致性**
- Cache-Aside + 先更库后删缓存 + TTL 兜底 → 最终一致，脏窗口 ≤ TTL。
- `update`/`delete` 必须删除详情 key（分页缓存已取消，无需版本号）。
- 登录态强一致：先删会话，再返回登出成功。

**降级（分四类）**

| 依赖 | 故障影响 | 策略 |
|---|---|---|
| 缓存 | RT 上升 | `RedisUtil` 全 `try/catch`，异常返回 null → 回源，接口正常 |
| 手写锁 | 无法加锁 | 降级为直接查库（放弃互斥，接受重复回源） |
| Redisson 锁/防重 | 无法加锁/防重 | 捕获异常后放行（限流失效好过业务不可用），打 WARN 并计数 |
| Sa-Token 会话 | **无法登录/鉴权** | 不降级，返回 401/500，单独告警 |

### 4.6 序列化约束

1. value 用 `new GenericJackson2JsonRedisSerializer()` 无参构造，不要自建 `ObjectMapper`。
2. `INCR` 目标必须写 `Long`，写 `"1"` 会变成带引号的 `"1"`。
3. 实体删字段/改名需升 key 版本号。
4. Sa-Token 的序列化由 `sa-token-redis-jackson` 自管，与业务 `RedisTemplate` 隔离。

### 4.7 Sa-Token 会话设计

| 项 | 取值 | 理由 |
|---|---|---|
| token 名称 | `Authorization` | 与前端现状一致 |
| token 前缀 | `Bearer` | 自动剥离，前端无需改 |
| token 风格 | `tik` | 短、不连续，降低遍历风险 |
| 有效期 | `timeout=86400` | 与原 JWT 一致 |
| 活跃过期 | `active-timeout=1800` | 新增能力，JWT 做不到 |
| 多端登录 | `is-concurrent=true`、`is-share=false` | 每端独立 token，可分别踢 |
| Cookie | `is-read-cookie=false` | 前后端分离，只认 Header |
| 权限 | `StpInterface` 读 `userRole` | 1=管理员 → `admin`；0=普通用户 → `user` |

**登录态改造点（Phase 3）**

| 位置 | 现状 | 改造后 |
|---|---|---|
| `TravelUserController.login` | `JwtUtil.generateToken` | `StpUtil.login(userId)`，返回 `{user, token, tokenName, expire}` |
| `TravelUserController.myProfile` | 手写解析 Header | `StpUtil.getLoginIdAsInt()` |
| 登出接口 | 无 | `StpUtil.logout()` |
| `JwtInterceptor` | 校验 JWT | 删除，改 `SaInterceptor` |
| `WebConfig` | 拦截器整段注释 | 注册 `SaInterceptor` + 白名单（登录/注册/`/doc.html`/`/v3/api-docs/**`/`/webjars/**`/OPTIONS） |
| 全局异常 | 无 | `@RestControllerAdvice`：`NotLoginException`→401、`NotRoleException`→403 |
| `JwtUtil` | 静态工具类 | Phase 3 末尾删除 |

**会话管理接口（Phase 4）**

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/travelUser/logout` | POST | 登录 | 删除当前 token 对应会话 |
| `/travelUser/online` | GET | 管理员 | 在线用户分页列表（userId/account/nickname/tokenCount/lastActiveTime） |
| `/travelUser/kickout/{userId}` | POST | 管理员 | `StpUtil.kickout(userId)` 强制下线 |

**在线列表实现要点**：登录时把 `account`、`nickname`、`userRole` 写入 `StpUtil.getSession()`；列表用 `StpUtil.searchTokenValue` 遍历有效 token，`getLoginIdByToken` 聚合 userID，`tokenCount` 即该用户 token 数；演示规模下内存分页即可（不做 Redis 侧分页，避免过度设计）。

**角色鉴权（Phase 4）**：`StpInterface.getRoleList` 返回 `["admin"]` 或 `["user"]`；`DashboardController`、在线列表、踢人、用户管理（注册接口除外）加 `@SaCheckRole("admin")`。

### 4.8 Redisson 使用边界

| 场景 | 对象 | 位置 | Phase |
|---|---|---|---|
| 登录限流 | **手写固定窗口**（`LockUtil.tryAcquireFixedWindow`，Lua） | 登录接口前置 | 5 |
| 订单/收藏防重复提交 | `RBucket.trySet` / `RLock` | 下单、收藏接口 | 5 |
| 高并发下替换手写锁（A/B 对比，可选） | `RLock` | `RedisUtil.getOrLoad` | 可选 |
| 景点 id 防穿透 | `RBloomFilter` | 详情前置 | 可选 |

**配置约定**：单机模式、复用 `spring.redis.*`、`connectionPoolSize=8`、`connectionMinimumIdleSize=2`、`destroyMethod="shutdown"`；与 Lettuce 共享 Redis 实例但不共享连接。

---

## 5. 实施计划

### 里程碑总览

| Phase | 内容 | 工时 | 累计 |
|---|---|---|---|
| 0 | Redis 基础设施 | 已完成 | — |
| 0.5 | 技术栈清理（Knife4j / Logback / Hikari / jar） | 0.5 天 | 0.5 |
| 1 | 基础封装（RedisUtil / RedissonClient / LockUtil） | 1 天 | 1.5 |
| 2 | 景点详情缓存（手写锁） | 1 天 | 2.5 |
| 3 | Sa-Token 替换 JWT | 1.5 天 | 4 |
| 4 | 会话管理与角色权限（在线列表 / 踢人 / 角色） | 1 天 | 5 |
| 5 | Redisson 防重 + 手写固定窗口限流 | 0.5–1 天 | 5.5–6 |
| 6 | 压测与验收 | 0.5 天 | 6–6.5 |

**最小可交付**：Phase 0.5 + 1 + 2 + 3（4 天）——技术栈无硬伤，缓存与可撤销登录态可用。
**完整交付**：0.5 → 6（6–6.5 天）。

---

### Phase 0　Redis 基础设施（已完成）

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成 |
| 交付物 | `application.yml`、`RedisConfigConfiguration.java`、`RedisConfigTest.java` |
| 验收记录 | 2026-10-31 5 个用例通过 |

### Phase 0.5　技术栈清理

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31） |
| 目标 | 去掉停更/死依赖，统一连接池与打包方式 |
| 验收记录 | `mvn clean package` 成功（19/19 测试通过）；`java -jar` 启动 4.3s；`/travel/doc.html` 200、`/travel/v3/api-docs` 200；日志出现 `TravelHikariPool - Start completed`；新增→分页查询→详情→删除全链路验证通过（删除后详情返回"没有找到数据"） |
| 交付物 | 改 `pom.xml`、`application.yml`、`SwaggerConfig.java`；删 `ServletInitializer.java`、`log4j.properties` |
| 具体改动 | ① `packaging` 改 `jar`，删 `ServletInitializer`；② 删 `springfox-boot-starter`，加 `knife4j-openapi3-spring-boot-starter:4.5.0`；③ 删 `log4j:1.2.17` 与 `log4j.properties`；④ 删 `commons-fileupload`；⑤ 删 `commons-dbcp2` 与 `spring.datasource.type`、`spring.datasource.dbcp2.*`，加 `spring.datasource.hikari.*`（`maximum-pool-size=20`、`minimum-idle=2`、`connection-timeout=60000`、`idle-timeout=600000`、`max-lifetime=1800000`）；⑥ 删 `spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER`；⑦ 加 `knife4j.enable=true`、`knife4j.setting.language=zh_cn`；⑧ `SwaggerConfig` 改为 `OpenAPI` Bean（标题/版本/描述），删除 `Docket` |
| 实施顺序 | 先 pom → 再配置 → 再 Swagger → 最后删文件，每步都能启动 |
| 验收标准 | ① `mvn clean package` 产出可执行 jar；② `java -jar target/spbYq-0.0.1-SNAPSHOT.jar` 启动成功；③ `http://localhost:8081/travel/doc.html` 可打开并调试接口；④ 登录/景点列表/详情三个接口回归正常；⑤ 启动日志出现 HikariPool、无 springfox/log4j 输出；⑥ 新增一条景点数据能落库 |
| 预计工时 | 0.5 天 |
| 风险 | Knife4j 与 Boot 2.7.5 兼容性（R11）；Hikari 事务语义变化（R12） |

### Phase 1　基础封装（新增 5 文件）

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31，`mvn -Dtest=RedisUtilTest,RedisUtilDegradeTest test` 8/8 通过） |
| 目标 | 缓存与手写锁能力收口（Redisson 在 Phase 5） |
| 交付物 | `util/RedisKeys.java`、`util/LockUtil.java`（仅手写锁）、`util/RedisUtil.java`、`test/RedisUtilTest.java`、`test/RedisUtilDegradeTest.java` |
| 实现要点 | ① `getOrLoad(key, ttl, Supplier)`：命中/回源/回写/异常降级；② 空值哨兵（60s）；③ TTL ±10% 抖动；④ 手写锁 `setIfAbsent` + UUID + Lua 原子释放；⑤ 未抢到锁时自旋约 200ms，仍未就绪则直接回源；⑥ 异常统一 WARN 不上抛。**本阶段不引入任何新依赖**，Redisson 后移至 Phase 5（避免在还没用到的阶段承担 Netty 版本风险） |
| 验收标准 | 单测覆盖「命中 / 未命中 / 空值缓存 / 异常降级 / 锁互斥 / 锁过期 / 10 并发仅回源 1 次」全绿；业务代码零改动；`mvn test` 通过 |
| 预计工时 | 1 天 |
| 风险 | Redisson 与 Lettuce 的 Netty 版本冲突（R3） |

### Phase 2　景点详情缓存

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31，`ScenicServiceCacheTest` 6/6 通过） |
| 目标 | 详情走缓存；并发只有一个请求回源；写后立即失效 |
| 交付物 | 改 [ScenicServiceImpl.java](src/main/java/com/zjx/travel/service/serviceImpl/ScenicServiceImpl.java)；新增 `test/service/ScenicServiceCacheTest.java` |
| 实现要点 | `queryById` 走 `getOrLoad` + **手写锁**互斥；空值缓存；`insert/update/deleteById` 后删 key |
| 验收标准 | ① 连续两次请求只出现 1 条 SELECT；② 20 并发同 key 只出现 1 条 SELECT；③ `TTL` 在 (0,1800] 且每次不同；④ `edit` 后立即读到新值；⑤ Redis 停机后接口仍 200 |
| 预计工时 | 1 天 |
| 风险 | 并发验证需要工具，见 §7 |

### Phase 3　Sa-Token 替换 JWT

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31） |
| 目标 | 登录态可撤销、可续期 |
| 验收记录 | 无 token → 401「未登录，请先登录」；登录返回 `token/tokenName/expire=86400`；带 token 访问 200；登出后旧 token 401；**应用重启后旧 token 仍可用**（证明会话在 Redis 而非内存）；`/doc.html` 白名单 200 |
| 交付物 | 改 `pom.xml`（加 2 个 Sa-Token 依赖）、`application.yml`（`sa-token.*`）、`TravelUserController`、`WebConfig`；新增 `handler/GlobalExceptionHandler.java`；删 `JwtUtil`、`JwtInterceptor`、jjwt 依赖 |
| 实现要点 | ① `token-name=Authorization`、`token-prefix=Bearer`、`token-style=tik`；② 白名单含 `/doc.html`、`/v3/api-docs/**`、`/webjars/**`、OPTIONS；③ `NotLoginException` 按 type 映射 401 文案（未登录/过期/被踢/被顶）；④ 登录响应保留 `token` 字段并追加 `tokenName`、`expire`；⑤ 先新旧并存，回归通过再删 JWT |
| 验收标准 | ① 登录返回 token；② 无 token 访问受保护接口 401；③ 带 token 200；④ 登出后旧 token 401；⑤ Redis 中可见 `Authorization:login:token:*` 且 TTL≈86400；⑥ 前端主流程不受影响 |
| 预计工时 | 1.5 天 |
| 风险 | 拦截器全站生效（R5）；前端联调（Q1 已确认可配合） |

### Phase 4　会话管理与角色权限

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31） |
| 目标 | 后台能看在线用户、能踢人；接口按角色控制 |
| 验收记录 | 在线列表返回 2 人（含 userRole 与 tokenCount）；普通用户访问 `/dashboard/stats` → 403；管理员 → 200；踢人后该用户 → 401「账号被强制下线」且从在线列表消失；踢自己被拒；删除账号会连带踢掉其全部 token |
| 交付物 | 改 `TravelUserController`（`/online`、`/kickout/{userId}`）、`DashboardController`（加 `@SaCheckRole("admin")`）；新增 `config/StpInterfaceImpl.java`、`domain/OnlineUserVO.java` |
| 实现要点 | ① 登录时 `StpUtil.getSession().set(...)` 写 account/nickname/userRole；② 在线列表用 `StpUtil.searchTokenValue` 聚合，按 userId 去重、统计 tokenCount；③ 踢人 `StpUtil.kickout(userId)`；④ `StpInterface` 返回 `admin`/`user`；⑤ 注册接口 `/travelUser/add` 必须保持匿名 |
| 验收标准 | ① 管理员能分页看到在线用户；② 踢人后该用户下一次请求 401 且 msg 为"账号被强制下线"；③ 普通用户访问 `/dashboard/stats` 返回 403；④ 管理员访问正常；⑤ 注册/登录接口不受影响 |
| 预计工时 | 1 天 |
| 风险 | 在线列表遍历全量 token，用户量大时性能差（本项目可接受，文档注明） |

### Phase 5　Redisson 防重与登录限流

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31） |
| 目标 | 展示 Redisson 在复杂并发场景的价值，与手写锁形成对照 |
| 验收记录 | 24/24 测试通过（Redisson 跨线程互斥、防重仅成功一次；固定窗口放行 5 次、第 6 次拒绝、**计数 key TTL 在 (0,60]**）；HTTP 实测同一 IP 第 6 次登录返回"操作过于频繁"，5 秒内重复下单返回"请勿重复提交"；限流改为手写实现后仅剩 1 个 key/IP，旧 Redisson 残留已清理 |
| 交付物 | 新增 `config/RedissonConfig.java`；`util/LockUtil.java` 增加 `tryLockWithRedisson` / `unlockRedisson` / `trySetOnce` / `tryAcquireFixedWindow`；`pom.xml` 加 `redisson` 依赖并统一 `<netty.version>4.1.107.Final`；改 `TravelUserController`（登录 IP 限流）、`ScenicOrdersController`（防重复提交）；新增 `test/util/LockUtilRedissonTest.java`、`test/util/LoginRateLimitTest.java` |
| 实现要点 | ① 限流：Lua 脚本原子 `INCR + EXPIRE(60s)`，1 个 key/IP，第 6 次被拒；② 防重：`RBucket.trySet(true, 5s)`，key 含 userId + 业务；③ 阈值与窗口做成常量；④ 限流组件异常一律放行，避免"Redis 抖动导致谁都登录不了" |
| 验收标准 | ① 登录连续 6 次，第 6 次返回"操作过于频繁"；② 同一用户 5 秒内重复下单/收藏被拒；③ 换 IP / 等窗口后恢复 |
| 预计工时 | 0.5–1 天 |
| 风险 | 阈值过严影响演示 → 配置化 |

### Phase 6　压测与验收

| 项 | 内容 |
|---|---|
| 状态 | ✅ 已完成（2026-10-31） |
| 目标 | 拿到可写进简历的量化数据 |
| 交付物 | [`docs/performance-report.md`](performance-report.md)（完整报告）+ `tools/LoadTest.java`（压测器）+ `travel.cache.enabled` 开关 |
| 场景 | 景点详情接口 HTTP 压测：50 / 100 / 200 并发 × 200 次，热缓存 vs `--travel.cache.enabled=false`（直查库） |
| 结果 | QPS 提升 12.5%–16.6%，P95 下降 15.6%–20.7%，P99 下降 25.6%–34.0%；缓存命中 100%，数据库查询 10,000 → 0 次 |
| 工具 | 自研 `tools/LoadTest.java`（JDK 8 单文件，`HttpURLConnection` + 线程池），避免 PowerShell 客户端开销 |
| 验收记录 | 报告含环境、方法、复现命令、三档并发数据、分析与局限；§9.1 简历数字已回填 |
| 预计工时 | 0.5 天 |

---

## 6. 非功能要求

| 维度 | 要求 |
|---|---|
| 可用性 | 缓存/锁故障可降级；**鉴权不可降级**，故障时明确 401/500 并告警 |
| 一致性 | 业务缓存最终一致（窗口 ≤ TTL）；登录态强一致 |
| 安全 | 密码不入缓存；token 不落日志；`Authorization:login:*` 禁人工删除；`tik` 风格降遍历风险；后台接口按角色鉴权 |
| 容量 | 单 value < 10KB；Redis 总占用 < 100MB；无永久业务 key |
| 性能 | 详情热缓存 P95 ≤ 未缓存 1/3（Phase 6 实测）；鉴权新增 Redis 查询 < 1ms（本地） |
| 可维护 | `RedisTemplate`/`RedissonClient` 只在工具类；key 统一常量；降级有 WARN 日志 |

---

## 7. 测试方案

| 层级 | 内容 | 手段 |
|---|---|---|
| 单元测试 | `RedisUtil` 命中/未命中/降级；手写锁互斥与过期；TTL 抖动 | `@SpringBootTest(NONE)` + JUnit 5 |
| 集成验证 | 详情缓存命中、写后失效 | 手工 + `redis-cli monitor` + MyBatis SQL 日志 |
| 鉴权验证 | 登录/未登录/登出/踢人/角色 403/超时 | curl + `redis-cli` 观察 `Authorization:login:*` |
| 并发验证 | 20 线程同 key 只回源 1 次 | JMeter 或 `curl` 循环 + 日志计数 |
| 限流验证 | 第 6 次被拒 | `curl` 循环 |
| 故障演练 | Redis 停机 / 端口错误 / 密码错误 | 改配置启动，验证降级与鉴权失败表现 |
| 回归 | Phase 0.5 后接口全量回归 | 登录/列表/详情/新增/编辑/删除 |

**手工验证脚本（Phase 2 起复用）**
1. `redis-cli -a *** monitor` 开窗口。
2. 请求详情两次 → 第一次 `SET`，第二次仅 `GET`。
3. `TTL <key>` > 0。
4. 调 `edit` → 出现 `DEL`，再请求 → 新值 + 新 `SET`。
5. 停 Redis → 读接口仍 200，登录返回 500。

---

## 8. 风险登记

| 编号 | 风险 | 影响 | 应对 |
|---|---|---|---|
| R1 | Redis 宕机拖慢接口 | RT 上升 | 3s 超时 + 全链路降级 |
| R2 | 缓存与库不一致 | 脏读 | 先更库后删缓存 + TTL |
| R3 | Redisson 与 Lettuce 的 Netty 版本冲突 | 启动 `NoSuchMethodError` | pom 统一 `<netty.version>`；Phase 1 启动即验证 |
| R4 | Sa-Token 1.44 与 Boot 2.7 自动配置不兼容 | 启动失败/会话不生效 | 回退 1.38.0 |
| R5 | 拦截器全站生效 | 前端 401 | 白名单 + 前端契约（已出文档） |
| R6 | 手写锁误删他人的锁 | 并发失控 | Lua compare-and-delete + UUID |
| R7 | 锁 TTL 小于临界区耗时 | 提前失效、重复回源 | TTL ≥ 3 倍最大耗时 |
| R8 | **在线列表遍历全量 token** | 用户多时慢 | 演示规模可接受；文档注明，后续可分页扫描 |
| R9 | 误删 `Authorization:login:*` | 全员掉线 | 文档红线 + 禁止 `KEYS`/批量 `DEL` |
| R10 | Redis 单点，鉴权不可用 | 无法登录 | 明确接受；生产升级主从 |
| R11 | Knife4j 4.5 与 Boot 2.7.5 不兼容 | 文档打不开/启动失败 | Phase 0.5 首步验证；失败退回 `springdoc-openapi-ui:1.7.0` |
| R12 | Hikari 替换后写操作不落库 | 数据丢失 | 不设 `auto-commit=false`；验收含"新增一条数据" |
| R13 | jar 打包后上传/静态资源路径变化 | 附件访问异常 | 回归上传下载 |
| R14 | 踢人后前端未处理 401 | 页面白屏/死循环跳转 | 前端契约已写明"401 清 token 跳登录"，且踢人提示单独文案 |
| R15 | **订单查询越权**：`queryByPage` 条件里没有 `user_id`，任何人传 `userId` 都能查全站订单 | 隐私泄露 | **已修复**：mapper 补 `user_id` 条件；Controller 对非管理员强制覆盖为当前登录人；`queryById/edit/deleteById` 增加归属校验（403） |
| R16 | **批量清理测试数据误伤真实数据** | 数据被误删/误改 | 见 §13 复盘：禁止"列表查询 + 批量删"，必须按创建时返回的 id 或唯一业务键精确删除 |
| R17 | **禁用账号仍可登录**：登录只校验账号密码，未校验 `status`；且禁用后已登录会话不受影响 | 风控失效 | **已修复**：登录校验 `status=0` 直接拒绝；管理员改禁用时 `StpUtil.kickout` 立即踢下线 |
| R18 | **业务接口信任前端传的 `userId`**：收藏/消息/攻略/路线/评论均可传他人 id 越权 | 越权读写 | **第 1 轮已修复**：新增 `SecurityUtil`，写入与"我的列表"一律取 token 身份，单条操作校验归属（403） |
| R19 | **自动填充失效**：自定义 XML 插入不触发 `MyMetaObjectHandler`，不传 `createTime` 会插入失败 | 接口报错 | **第 1 轮已修复**：各 Service 的 insert 兜底补 `createTime/updateTime` |
| R20 | **后台写接口无角色校验**：景点/分类/行程/消息等后台接口任何登录用户都能调 | 越权改数据 | **第 2 轮已修复**：上述接口补 `@SaCheckRole("admin")`；用户端发文/收藏/下单保持登录即可 |

---

## 9. 简历呈现与面试问答

### 9.1 简历条目（数字待 Phase 6 填入）

- 完成项目技术栈治理：以 **Knife4j + SpringDoc** 替换停更的 springfox，移除 log4j 1.x 与未使用的 commons-fileupload 等死依赖，**dbcp2 → HikariCP**，**war → jar** 内嵌部署。
- 基于 Spring Data Redis（Lettuce）构建缓存层，自定义 `RedisTemplate` 序列化（String key + JSON value），解决 JDK 序列化导致的 key 不可读与类型丢失。
- 缓存击穿采用**手写分布式锁**（`setIfAbsent` + UUID + Lua 原子释放）；登录限流用 **Lua 固定窗口**（原子 `INCR + EXPIRE`，单 key、60 秒自动过期）；订单防重复提交使用 **Redisson `RBucket.trySet`**——按场景选择实现，而非一律上框架。
- 使用 **Sa-Token** 替换 JWT：登录态存 Redis，支持登出即时失效、30 分钟无操作自动过期、**在线用户列表与踢人下线**，并按 `userRole` 实现接口级角色鉴权，修复 JWT 签发后无法作废的安全缺陷。
- 缓存层统一降级：Redis 异常自动回源，接口可用性不受影响；压测显示详情接口 **QPS 提升 12.5%–16.6%、P95 下降 15.6%–20.7%、P99 下降 25.6%–34.0%**（1–4 万请求，50/100/200 并发），数据库查询量降为 0。

### 9.2 高频追问与答法

| 问题 | 回答要点 |
|---|---|
| 为什么 JWT 换 Sa-Token？ | JWT 无状态、无法主动失效，登出只是前端删 token；Sa-Token 会话存 Redis，能登出、踢人、续期、查在线列表。代价是每次鉴权一次 Redis 查询，单体可接受 |
| 为什么锁分两套？ | 缓存重建是"单 key、短临界区、幂等"，手写 15 行足够；可重入/等待队列/续期这类复杂锁交给 Redisson。限流用 Lua 固定窗口——比 `RRateLimiter` 少 2 个无 TTL 的 key，且登录只需"每分钟 N 次"，用不到令牌桶 |
| 为什么不用 Redisson 的限流器？ | 实测它的一个限流器会写 3 个 key（配置 hash + `{name}:value` + `{name}:permits`）且**都不带 TTL**，每个 IP 永久留 3 个 key，属于慢性泄漏；固定窗口用 1 个 key、60 秒自动过期，行为可预测 |
| 手写锁怎么保证不误删？ | 值为 UUID，Lua 脚本比对后再删，保证"判断 + 删除"原子；TTL 兜底防死锁 |
| 在线用户列表怎么实现？ | 登录时把用户信息写入 Sa-Token Session；列表用 **SCAN** 扫 `{token-name}:login:token:*`（本项目 `Authorization:...`），反查 loginId 后按 userId 聚合 tokenCount；**不用 KEYS**，量大时应改为在线 ZSet 或独立在线表 |
| 踢人是怎么生效的？ | `StpUtil.kickout(userId)` 删除该账号所有 token 记录，后续请求校验失败抛 `NotLoginException(KICK_OUT)`，统一异常处理返回 401 |
| 角色鉴权怎么做的？ | 实现 `StpInterface.getRoleList`，把 `userRole` 映射为 `admin`/`user`，后台接口加 `@SaCheckRole("admin")` |
| 为什么换 Knife4j + SpringDoc？ | springfox 2020 停更且与 Boot 2.6+ 的 PathPattern 冲突，只能降级匹配策略；SpringDoc 是当前标准，Knife4j 提供中文 UI |
| 为什么 war 改 jar？ | 内嵌 Tomcat、单文件部署；war 只在外置容器场景有价值 |
| 为什么换 HikariCP？ | Boot 默认池，性能与监控更好；原来的 `default-auto-commit=false` 是隐患 |
| 缓存一致性怎么保证？ | 先更库后删缓存 + TTL 兜底；接受最终一致，不做双写 |
| Redis 挂了会怎样？ | 缓存降级回源；锁降级放行；**登录鉴权不可用**（不能猜身份） |

---

## 10. 交付物与目录变更

| 类型 | 路径 | 阶段 |
|---|---|---|
| 改 | `pom.xml` | 0.5（清理 + Knife4j）、1（Redisson）、3（Sa-Token，删 jjwt） |
| 改 | `../src/main/resources/application.yml` | 0（Redis）、0.5（Hikari/Knife4j）、3（sa-token） |
| 改 | `config/SwaggerConfig.java` → SpringDoc `OpenAPI` Bean | 0.5 |
| 删 | `config/ServletInitializer.java`、`resources/log4j.properties` | 0.5 |
| 增 | `util/RedisKeys.java`、`util/LockUtil.java`（手写锁）、`util/RedisUtil.java` | 1 ✅ 已完成 |
| 增 | `test/RedisUtilTest.java`、`test/RedisUtilDegradeTest.java` | 1 ✅ 已完成 |
| 增 | `config/RedissonConfig.java`、`LockUtil` 的 Redisson 分支 | 5 |
| 改 | `service/serviceImpl/ScenicServiceImpl.java` | 2 |
| 增 | `handler/GlobalExceptionHandler.java` | 3 |
| 改 | `controller/TravelUserController.java`、`config/WebConfig.java` | 3 |
| 删 | `util/JwtUtil.java`、`interceptor/JwtInterceptor.java` | 3 末尾 |
| 增 | `config/StpInterfaceImpl.java`、`domain/OnlineUserVO.java` | 4 |
| 改 | `controller/DashboardController.java`、`controller/ScenicOrdersController.java` | 4、5 |
| 改 | `resources/mapper/ScenicOrdersMapper.xml`（补 `user_id` 条件） | 越权修复 |
| 增 | `docs/performance-report.md` | 6 |
| 改 | `docs/前端接口对接说明.md`（新增第 8 节鉴权契约） | 契约先行 ✅ 已写 |

---

## 11. 已确认决策（Q1–Q7）

| 编号 | 问题 | 结论 | 对文档的影响 |
|---|---|---|---|
| Q1 | 前端能否配合 `Authorization: Bearer {token}` | **可以配合** | 契约已写入 `docs/前端接口对接说明.md` 第 8 节；Phase 3 无需保留旧 JWT 兼容 |
| Q2 | 是否需要踢人 / 在线用户列表 | **需要** | 新增 Phase 4，新增 2 个接口 |
| Q3 | 是否需要接口级角色鉴权 | **需要** | Phase 4 增加 `StpInterface` + `@SaCheckRole` |
| Q4 | 分页缓存是否要做 | **不做** | 移入非目标；key 规范与策略矩阵已删除分页项 |
| Q5 | 是否要压测 | **需要** | Phase 6 由可选改为必做，产出 `docs/performance-report.md` |
| Q6 | 是否升 Boot 3 | **不升** | 技术债记录在案，不进主线 |
| Q7 | `/doc.html` 是否冲突 | **不冲突** | 白名单直接加 `/doc.html`、`/v3/api-docs/**`、`/webjars/**` |
| Q8 | 本地 Redis 是否设密码 | **未设**（`acl list`：`user default on nopass`） | `application.yml` 的 `spring.redis.password` 已注释；生产用 `${REDIS_PASSWORD:}` 注入 |

**无剩余待确认项**，可从 Phase 0.5 直接开工。

---

## 12. 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|---|---|---|---|
| v1.0 | 2026-10-31 | 初版：Redis 选型、Key 规范、缓存策略、Phase 0–5 | — |
| v1.1 | 2026-10-31 | 引入 Redisson、Sa-Token（替代 JWT）；阶段重排 | — |
| v1.2 | 2026-10-31 | Phase 0.5 技术栈清理；锁策略改为"手写优先 + Redisson 兜底"；删除"以国企存量项目为参照"的口径 | — |
| v1.3 | 2026-10-31 | Q1–Q7 拍板：新增 Phase 4（在线列表/踢人/角色鉴权）、取消分页缓存、压测转必做、Boot 3 明确不做；补齐前端鉴权契约并同步 `前端接口对接说明.md` | — |
| v1.4 | 2026-10-31 | Phase 1 完成：新增 `RedisKeys`、`LockUtil`（手写锁）、`RedisUtil`（Cache-Aside 模板）与 8 个测试用例（6 个集成 + 2 个降级），全部通过；Redisson 从 Phase 1 后移至 Phase 5，Phase 1 零新增依赖 | — |
| v1.5 | 2026-10-31 | Phase 2 完成：`ScenicServiceImpl` 接入 Cache-Aside（参数校验 + 空值哨兵 + 手写锁 + TTL 抖动），insert/update/delete 后删缓存；新增 6 个用例；全量测试 19/19 通过 | — |
| v1.6 | 2026-10-31 | Phase 0.5 完成：springfox→Knife4j 4.5.0+SpringDoc 1.7.0、删 log4j 1.x 改用 Logback、删 commons-fileupload、dbcp2→HikariCP、war→jar（删 `ServletInitializer`）；jar 启动、`/doc.html`、Hikari 读写全链路实测通过；修正 `.gitignore` 误忽略 `pom.xml` 与 `src/test/` | — |
| v1.7 | 2026-10-31 | Phase 3 完成：引入 Sa-Token 1.44.0 + `sa-token-redis-jackson`，`SaInterceptor` 替换 JWT 拦截器，新增 `/travelUser/logout`、`GlobalExceptionHandler`（401/403），删除 `JwtUtil`/`JwtInterceptor` 与 jjwt 依赖；实测登出即时失效、会话跨应用重启有效 | — |
| v1.8 | 2026-10-31 | Phase 4 完成：新增 `OnlineUserVO`、`StpInterfaceImpl`、`/travelUser/online`（SCAN 聚合）、`/travelUser/kickout/{userId}`；`@SaCheckRole("admin")` 覆盖 dashboard 与用户管理接口；删除账号时连带踢出登录态；修正 Sa-Token key 前缀说明 | — |
| v1.9 | 2026-10-31 | Phase 5 完成：引入 Redisson 3.27.2 核心包并把 `netty.version` 统一到 4.1.107；新增 `RedissonConfig`、`LockUtil` 的 `RLock`/`RRateLimiter`/`RBucket` 能力、登录 IP 限流、订单防重复提交；修正 `spring.redis.password`（本地 Redis 实为无密码）；修复订单查询越权；新增 §13 事故复盘 | — |
| v1.10 | 2026-10-31 | 登录限流改为手写 Lua 固定窗口（原子 `INCR+EXPIRE`，1 key/IP、TTL 60s），移除 Redisson `RRateLimiter` 与遗留的 22 个无 TTL key；新增 `LoginRateLimitTest`（含 TTL 断言）；测试 24/24 通过；同步 §2/§4.2/§4.8/§9 的表述 | — |
| v1.11 | 2026-10-31 | Phase 6 完成：新增 `travel.cache.enabled` 缓存开关（排障/压测基线）与 `tools/LoadTest.java`；完成 50/100/200 并发热缓存 vs 直查库对比，产出 `docs/performance-report.md`；简历数字回填 | — |
| v1.12 | 2026-10-31 | 修复禁用账号仍可登录：登录校验 `status=0` 直接拒绝（提示"账号已被禁用"），管理员改禁用时 `StpUtil.kickout` 立即下线；实测旧 token 401、恢复启用后可正常登录 | — |
| v1.13 | 2026-10-31 | 安全加固第 1 轮：删除 `PrjConstant`（他项目残留）；新增 `SecurityUtil`，收藏/消息/攻略/路线/评论的身份一律取自 token，单条操作校验归属；各 Service insert 兜底补时间，修复自动填充失效 | — |
| v1.14 | 2026-10-31 | 安全加固第 2 轮：景点/分类/行程/消息（add、edit）及 `travelUser/queryById` 补 `@SaCheckRole("admin")`；实测普通用户 5 个接口全部 403、管理员正常、用户端发文与收藏不受影响 | — |
| v1.15 | 2026-10-31 | 安全加固第 3、4 轮：`myProfile` 去掉 `?id=` 死分支，身份只认 token；修正 `user_role` 约定为 0 普通/1 管理员并在 Service 加角色白名单；README 校正 `unreadCount`、Sa-Token key 前缀、缺失的收藏/消息接口与管理员标注 | — |
| v1.16 | 2026-10-31 | 浏览量功能：详情接口 `INCR`，`@Scheduled` 每 60 秒用 SCAN 批量回写；**先落库再 DECRBY**（失败保留计数重试），回写后删详情缓存；Redisson 锁防多实例重复回写；新增 3 个测试，实测 3 次浏览 → 5 秒后 DB=3 且 key 清理 | — |
| v1.17 | 2026-10-31 | 文件上传：`FileStorageService` + `POST /file/upload`（本地 `./uploads`、`/uploads/**` 静态访问、UUID 防重名、后缀 + Content-Type 白名单、路径穿越校验）；景点封面 / 攻略封面 / 用户头像更新成功后自动删旧图；前端对接说明补 §10–§13 | — |
| v1.18 | 2026-10-31 | 新增 `POST /travelUser/updateProfile`（普通用户改昵称/手机/头像，身份取 token，`userRole/status` 传了也忽略）；`updatePassword` 改为 token 身份，堵住"带别人 id 改别人密码"；实测提权字段无效、A 改密不影响 B | — |

---

## 13. 附录：事故复盘（2026-10-31）

### 13.1 事件

Phase 5 收尾验证时，为清理测试订单，脚本执行了：

```
GET /scenicOrders/queryByPage?userId.id=32&page=1&size=20
→ 对返回的 20 条记录逐条调用 POST /scenicOrders/deleteById/{id}
```

结果这 20 条**并非**该测试用户的订单，而是全站订单的前 20 条。

### 13.2 影响

- `ScenicOrdersMapper.xml` 的 `deleteById` 是**软删除**（`del_mark 1→0`），**没有物理删除数据**。
- 20 条订单（id 1–11、13–21）被错误标记为已删除，前端列表不可见。
- 通过 MySQL binlog（`ROW` 格式）解析出准确的 20 个 id，用户手动执行 `UPDATE ... SET del_mark = 1` 完成恢复。

### 13.3 根因

1. **直接原因**：`ScenicOrdersMapper.xml` 的 `condition` 片段**没有 `user_id` 条件**，`userId.id` 参数被静默忽略，查询退化为"全表前 20 条"。
2. **流程原因**：清理脚本用"列表查询 + 批量删"来清理测试数据，把"查到的"等同于"我创建的"，没有做二次校验。
3. **放大因素**：该接口本身存在越权缺陷（见 R15），任何用户都能查到全站订单，所以"查到的不是自己的"在业务上也完全可能发生。

### 13.4 修复

- **越权（R15）**：mapper 补 `user_id` 过滤；Controller 对非管理员强制用当前登录人覆盖查询条件；`queryById` / `edit` / `deleteById` 增加归属校验，跨用户访问抛 `NotPermissionException` → 403。
- **验证**：B 用户查列表返回 0 条；B 查/删 A 的订单返回 403；管理员仍可查全部；下单后的精确 id 删除可正常完成。

### 13.5 后续规则（写入团队约定）

1. 清理测试数据**禁止**"列表查询 + 批量删"，必须按创建时拿到的 id 或唯一业务键（如 `order_no`）精确删除。
2. 任何批量的写操作前，先把目标 id 集合打印出来人工确认。
3. 清理脚本执行前确认接口的过滤条件真的生效（先用两个账号交叉验证）。
4. 生产库操作前先备份；本地也应确认软删除/物理删除语义。

### 13.6 补充：限流 key 泄漏（2026-10-31 晚）

- **现象**：Redis 中出现 `travel:v1:ratelimit:login:{ip}`（hash）与 `{travel:v1:ratelimit:login:{ip}}:value` / `:permits` 三组 key，TTL 全为 -1。
- **原因**：Redisson 的 `RRateLimiter` 把一个限流器拆成"配置 + 令牌数 + 时间戳"三个 key，且都不设过期；每个 IP 永久占 3 个 key。
- **修复**：登录限流改为手写 Lua 固定窗口（`INCR` + `EXPIRE` 原子执行），1 个 key/IP、TTL 60 秒；移除 `RRateLimiter` 及 22 个残留 key。
- **教训**：引入第三方组件时，必须验证它在 Redis 里**实际写了哪些 key、带不带 TTL**，不能只看 API 语义。

---

## 14. 浏览量方案（已实现）

| 环节 | 实现 | 说明 |
|---|---|---|
| 计数 | 详情接口成功后 `INCR travel:v1:scenic:view:{id}` | 只写 Redis，不在请求线程里 `UPDATE` |
| 回写 | `@Scheduled` 每 60 秒（`travel.view.flush-interval-ms`） | 初始延迟一个周期，避开启动抖动 |
| 遍历 | `SCAN MATCH travel:v1:scenic:view:* COUNT 500` | 不用 `KEYS`，单次上限 1 万 key |
| 单条处理顺序 | `GET` → `UPDATE scenic SET view_count = view_count + delta` → 成功才 `DECRBY/DEL` | **先落库、后扣 Redis**；顺序反了会丢数据 |
| 扣减方式 | Lua：剩余 ≤ 增量则 `DEL`，否则 `DECRBY` | 回写期间新增的浏览不会被吞 |
| 失败处理 | 更新影响行数为 0（景点已删）或异常 → 保留 Redis 计数 | 下一轮重试 |
| 展示 | 只读 DB 值；回写成功后 `DEL` 详情缓存 | 不额外查 Redis，避免读路径多一次往返 |
| 多实例 | Redisson 锁 `travel:v1:lock:scenic:view:flush` | 防止两个节点重复累加 |

**一致性**：最终一致，最大滞后 ≈ 1 个回写周期；崩溃窗口内可能**多计一次**（at-least-once），不会少计。

**未做**：同一用户/UV 去重（当前每次请求都计数）；Redis 持久化建议（生产建议开 AOF，否则重启会丢未回写的增量）。

---

## 15. 文件上传方案（已实现，HTTP 冒烟待补）

| 项 | 实现 |
|---|---|
| 存储 | 项目内 `./uploads`（`travel.upload.dir`），按 `yyyy/MM/dd/{uuid}.{ext}` 分目录 |
| 访问 | `WebConfig` 把 `/uploads/**` 映射到该目录，并加入 Sa-Token 白名单（`<img>` 无法带 token） |
| 接口 | `POST /file/upload`，multipart 字段 `file`，登录即可（不要求管理员） |
| 防重名 | 文件名只用 UUID，原始文件名不参与路径 |
| 防穿越 | 后缀白名单 + `image/*` 校验；落盘路径归一化后必须 `startsWith(root)`；删除同样校验 |
| 限制 | 单文件 10MB（`spring.servlet.multipart.*` + `MultipartConfig`） |
| 单图覆盖 | 景点 `cover`、攻略 `cover`、用户 `avatar` 在 `edit` 成功且值变化时，调用 `deleteByUrl` 删除旧文件；更新失败不删 |
| 返回 | `{ url, name, size }`，`url` 可直接访问 |

**验证状态**：单元测试 5/5（存取删、后缀拒绝、伪装 Content-Type 拒绝、穿越删除拒绝、恶意文件名不逃逸）；HTTP 冒烟因测试工具限制未完成，需在 Knife4j 或浏览器里手动确认「上传 → 静态访问 → 覆盖后旧图消失」。
