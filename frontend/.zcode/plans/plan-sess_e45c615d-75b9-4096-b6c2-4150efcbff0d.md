## 修复计划（travel-platform 前端）

### P0 — 崩溃类 bug 修复（纯前端）
1. `src/stores/auth.js:7`、`src/router/index.js:220`：JSON.parse 加 try/catch 兜底，异常时清空并按未登录处理
2. `GuideList.vue` / `RouteList.vue` / `SpotList.vue`：加 `watch(() => route.query.keyword)` 修复页内搜索失效
3. `ScenicMap.vue`：为信息窗体的"查看详情"按钮补上点击逻辑（跳转到对应景点详情页），并修复 `getContent()` 返回字符串导致 `querySelector` 报错的问题
4. `RoutePlanner.vue:177`：onBeforeUnmount 移除 resize 监听并加 debounce
5. `Home.vue` getCategoryStyle 空分类名兜底；`GuideDetail.vue` 缓存对象缺 content 字段时强制请求详情
6. MyOrders/MyComments/MyPublish/GuideDetail 的 id 比较统一 `Number()` 转换

### P1 — 安全加固（仅两项）
1. `src/config/amap.js`：key/securityJsCode 改 `import.meta.env.VITE_AMAP_*`，新建 `.env.example`，`.gitignore` 补 `.env`
2. `src/api/request.js`：401 拦截重写——同时清 Pinia store 与 localStorage、排除登录接口、携带 redirect 回跳参数

### P2 — 体验与代码质量
1. 抽取 `useAdminTable` 组合式函数，8 个管理页接入：加请求竞态守卫、code=0 清空旧列表、删除后页码 clamp
2. MessageCenter"全部已读"改真实全量；OrderManagement 统计卡片标注"本页"
3. 加 404 捕获路由；Element Plus 配 zh-cn locale；图标统一到 @element-plus/icons-vue，移除 font-awesome

### P3 — 新功能路线图（不在本次执行，供挑选）
搜索增强（防抖+搜索历史+热门搜索词）、消息未读数 store 化+定时轮询、订单支付流程完善（支付页、订单状态展示、退款入口）、骨架屏与图片懒加载、深色模式

完成后运行 `npm run build` 验证构建通过，并汇总修改清单。