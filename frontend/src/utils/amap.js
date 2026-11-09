// 高德地图 JS API 加载器（动态注入 <script>，无需 npm 依赖）
// 提供：配置判断、按需加载。景点坐标直接取自景点数据的 longitude/latitude，不依赖地理编码。
import { amapConfig } from '@/config/amap'

let loadPromise = null
let loaded = false

// 是否已配置 key（未配置时地图视图降级提示，不影响其他功能）
export function isAmapConfigured() {
  return !!(amapConfig.key && amapConfig.securityJsCode)
}

// 动态加载高德 JS API v2.0，返回 window.AMap
export function loadAMap() {
  if (!isAmapConfigured()) {
    return Promise.reject(new Error('未配置高德地图 Key'))
  }
  if (loaded && window.AMap) return Promise.resolve(window.AMap)
  if (loadPromise) return loadPromise

  // v2.0 需在脚本加载前设置安全密钥
  if (!window._AMapSecurityConfig && amapConfig.securityJsCode) {
    window._AMapSecurityConfig = { securityJsCode: amapConfig.securityJsCode }
  }

  loadPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-amap]')
    if (existing) {
      existing.addEventListener('load', () => resolve(window.AMap))
      existing.addEventListener('error', () => reject(new Error('高德地图脚本加载失败')))
      return
    }
    const script = document.createElement('script')
    script.setAttribute('data-amap', '')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${amapConfig.key}`
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('高德地图脚本加载失败'))
    document.head.appendChild(script)
  })
    .then((AMap) => {
      loaded = true
      return AMap
    })
    .catch((err) => {
      loadPromise = null
      throw err
    })

  return loadPromise
}
