import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: 'http://localhost:8081/travel/',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 — 自动附加令牌（后端已切换 Sa-Token，token 为非 JWT 的随机串，
// 不能再按 eyJ 前缀过滤；要求后端配置 token-name: Authorization、token-prefix: Bearer）
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('travel_admin_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 1) {
      // 查询无数据属正常空态：后端统一返回「没有找到数据」，放行不弹错，由页面渲染空列表
      if (res.msg && String(res.msg).includes('没有找到数据')) {
        return res
      }
      // silent：后端接口尚未实现时静默降级，由页面自行渲染空态
      if (!response.config.silent) ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    const isLoginApi = String(error.config?.url || '').includes('login')
    if (error.response?.status === 401 && !isLoginApi) {
      // 登录态失效：本地状态与 Pinia store 一并清理，避免双份状态源不一致
      // 提示优先用后端 msg（「token 无效或已过期」「账号被强制下线」等，见对接说明 §8.3）
      try { useAuthStore().clearAuth() } catch { /* store 未就绪时仅清 localStorage */ }
      localStorage.removeItem('travel_admin_token')
      localStorage.removeItem('travel_admin_user')
      const current = router.currentRoute.value
      if (!error.config?.silent) ElMessage.error(error.response?.data?.msg || '登录已过期，请重新登录')
      if (current.path !== '/travel/login') {
        router.replace({ path: '/travel/login', query: { redirect: current.fullPath } })
      }
    } else if (error.response?.status === 403) {
      // 已登录但角色不足 / 越权（§8.3）：只提示无权限，不清 token、不跳登录页
      if (!error.config?.silent) ElMessage.error(error.response?.data?.msg || '无权限访问')
    } else {
      if (!error.config?.silent) ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
