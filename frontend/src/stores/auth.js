import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const TOKEN_KEY = 'travel_admin_token'
  const USER_KEY = 'travel_admin_user'

  // 记住我：勾选写 localStorage，不勾选写 sessionStorage；读取时两种都兼容
  function readStorage(key) {
    return localStorage.getItem(key) ?? sessionStorage.getItem(key)
  }

  function writeAuthStorage(key, value, remember) {
    const target = remember ? localStorage : sessionStorage
    const other = remember ? sessionStorage : localStorage
    target.setItem(key, value)
    other.removeItem(key)
  }

  function clearAuthStorage() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  }

  const token = ref(readStorage(TOKEN_KEY) || '')
  const userInfo = ref(readStoredUser())

  function readStoredUser() {
    const raw = readStorage(USER_KEY)
    try {
      return JSON.parse(raw || 'null')
    } catch {
      // 存储数据被污染时按未登录处理，避免启动白屏
      clearAuthStorage()
      return null
    }
  }

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.userRole === 1)
  const nickname = computed(() => userInfo.value?.nickname || '用户')
  const roleName = computed(() => (userInfo.value?.userRole === 1 ? '管理员' : '普通用户'))
  const avatar = computed(() => userInfo.value?.avatar || '')

  function saveAuth(t, user, remember) {
    // 资料更新等场景不会传 remember：沿用当前 token 所在存储，避免把会话登录意外变成持久登录
    const persist = remember === undefined ? localStorage.getItem(TOKEN_KEY) != null : remember
    // 后端返回的用户对象可能带 password（哈希）字段。它一旦进入存储，
    // 后续资料保存整包回传 travelUser/edit 时会被后端二次加密，等于改掉了密码。
    const { password, ...safeUser } = user || {}
    token.value = t
    userInfo.value = safeUser
    writeAuthStorage(TOKEN_KEY, t, persist)
    writeAuthStorage(USER_KEY, JSON.stringify(safeUser), persist)
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    clearAuthStorage()
  }

  async function login(account, password, remember = true) {
    const res = await loginApi({ account, password })
    if (res.code === 1) {
      const data = res.content
      let jwt = ''
      let user = data

      if (typeof data === 'string') {
        jwt = data
      } else if (data) {
        // tokenValue 是 Sa-Token getTokenInfo() 返回的令牌字段
        jwt = data.token || data.tokenValue || data.accessToken || data.jwt || ''
        user = data.userInfo || data.user || data
      }

      if (!jwt) {
        throw new Error('登录失败：未获取到有效的令牌')
      }

      saveAuth(jwt, user, remember)
      return user
    }
    throw new Error(res.msg || '登录失败')
  }

  // 登出（§8.4）：先同步清本地（/travel/login 是 noAuth 页，不清本地的话守卫会把
  // 已登出的用户弹回首页），再异步通知后端使 token 服务端失效。
  // 后端登出失败不阻塞前端退出——token 留在服务端最多等 24h 自然过期。
  async function logout() {
    const t = token.value
    clearAuth()
    if (!t) return
    try { await logoutApi(t) } catch { /* silent 请求，失败静默 */ }
  }

  // 注意：saveAuth 必须导出，否则组件内调用 authStore.saveAuth 会抛
  // "saveAuth is not a function"，且成功提示在它之后执行，永远不会弹出
  return { token, userInfo, isLoggedIn, isAdmin, nickname, roleName, avatar, login, logout, clearAuth, saveAuth }
})
