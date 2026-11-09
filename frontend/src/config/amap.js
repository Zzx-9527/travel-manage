// 高德地图配置
// key 与安全密钥通过环境变量注入（.env.local），不要把真实 key 提交进仓库
// 在 lbs.amap.com 控制台为 Key 配置域名白名单（含 localhost / 127.0.0.1）
// 参考 .env.example 复制一份 .env.local 并填入：
//   VITE_AMAP_KEY=xxx
//   VITE_AMAP_SECURITY_CODE=xxx
export const amapConfig = {
  key: import.meta.env.VITE_AMAP_KEY || '',
  securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || ''
}
