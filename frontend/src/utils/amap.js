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

// 获取当前所在城市：浏览器/高德定位 → 逆地理编码。
// 浏览器定位必须由用户授权，失败时抛出可读错误，由调用方决定是否回退到手动输入。
export async function locateCurrentCity() {
  const AMap = await loadAMap()

  const position = await new Promise((resolve, reject) => {
    AMap.plugin('AMap.Geolocation', () => {
      const geo = new AMap.Geolocation({ enableHighAccuracy: true, timeout: 8000 })
      geo.getCurrentPosition((status, result) => {
        if (status === 'complete' && result?.position) {
          resolve(result.position)
        } else {
          reject(new Error('定位失败，请检查浏览器定位权限'))
        }
      })
    })
  })

  return new Promise((resolve, reject) => {
    AMap.plugin('AMap.Geocoder', () => {
      const geocoder = new AMap.Geocoder()
      geocoder.getAddress([position.getLng(), position.getLat()], (status, result) => {
        if (status !== 'complete') {
          reject(new Error('地址解析失败，请手动填写出发地'))
          return
        }
        const component = result?.regeocode?.addressComponent || {}
        const raw = component.city || component.province
        const city = Array.isArray(raw) ? raw[0] : raw
        if (!city) {
          reject(new Error('未获取到当前城市，请手动填写出发地'))
          return
        }
        resolve(city)
      })
    })
  })
}
