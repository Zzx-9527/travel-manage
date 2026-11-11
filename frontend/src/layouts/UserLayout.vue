<template>
  <div class="user-layout">
    <!-- ====== 顶部导航栏 ====== -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <router-link to="/travel/home" class="logo">
          <div class="logo-icon"><i class="fa fa-globe"></i></div>
          <span class="logo-text">旅迹</span>
        </router-link>

        <nav class="nav-links">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >{{ item.label }}</router-link>
        </nav>

        <div class="top-bar-right">
          <div class="header-search">
            <i class="fa fa-search"></i>
            <select v-model="searchType" class="header-search-type" title="搜索类型" aria-label="搜索类型">
              <option value="spot">景点</option>
              <option value="guide">攻略</option>
              <option value="route">路线</option>
            </select>
            <input
              v-model="keyword"
              class="header-search-input"
              placeholder="输入关键词..."
              aria-label="搜索关键词"
              @keyup.enter="doSearch"
            />
          </div>
          <router-link to="/travel/home/publish" class="publish-btn"><i class="fa fa-pencil mr-1"></i>发布</router-link>
          <button class="icon-btn" title="消息通知" aria-label="消息通知" @click="router.push('/travel/home/messages')">
            <i class="fa fa-bell-o"></i>
            <span v-if="unreadCount > 0" class="msg-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </button>

          <!-- 已登录：用户下拉 -->
          <el-dropdown v-if="authStore.isLoggedIn" trigger="click" placement="bottom-end" @command="handleCommand">
            <div class="user-avatar-wrap">
              <el-avatar :size="36" :src="authStore.avatar">
                <i class="fa fa-user"></i>
              </el-avatar>
              <span class="user-name">{{ authStore.nickname }}</span>
              <i class="fa fa-chevron-down text-xs" style="color: var(--ink-400);"></i>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><i class="fa fa-user-circle mr-2"></i>个人中心</el-dropdown-item>
                <el-dropdown-item command="logout"><i class="fa fa-sign-out mr-2" style="color: #FF6B6B;"></i><span style="color: #FF6B6B;">退出登录</span></el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 未登录：登录/注册按钮（防御性，守卫已兜底） -->
          <div v-else class="auth-btns">
            <router-link to="/travel/login" class="auth-link">登录</router-link>
            <router-link to="/travel/register" class="auth-register">注册</router-link>
          </div>

          <!-- 移动端菜单按钮 -->
          <button
            class="menu-toggle"
            title="菜单"
            aria-label="打开菜单"
            :aria-expanded="mobileOpen"
            aria-controls="mobile-drawer"
            @click="openMobileDrawer"
          >
            <i class="fa fa-bars"></i>
          </button>
        </div>
      </div>
    </header>

    <!-- ====== 主体内容 ====== -->
    <main class="main-content">
      <!-- 全站公告横幅（公告管理发布，用户可关闭） -->
      <AnnouncementBanner />
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- ====== 底部 ====== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <div class="footer-logo"><i class="fa fa-globe mr-2"></i>旅迹 · 旅游信息管理平台</div>
          <p class="footer-desc">毕业设计作品 · 探索世界之美</p>
        </div>
        <div class="footer-links">
          <router-link :to="{ path: '/travel/home/about', query: { s: 'about' } }">关于我们</router-link>
          <router-link :to="{ path: '/travel/home/about', query: { s: 'help' } }">帮助中心</router-link>
          <router-link :to="{ path: '/travel/home/about', query: { s: 'privacy' } }">隐私政策</router-link>
          <router-link :to="{ path: '/travel/home/about', query: { s: 'terms' } }">用户协议</router-link>
        </div>
        <div class="footer-copy">&copy; 2026 旅游信息管理平台 · 毕业设计</div>
      </div>
    </footer>

    <!-- ====== 移动端抽屉导航 ====== -->
    <transition name="mask">
      <div v-if="mobileOpen" class="drawer-mask" @click="closeMobileDrawer"></div>
    </transition>
    <transition name="slide">
      <aside
        v-if="mobileOpen"
        id="mobile-drawer"
        class="mobile-drawer"
        role="dialog"
        aria-modal="true"
        aria-label="移动端导航"
        @keydown.esc="closeMobileDrawer"
      >
        <div class="drawer-head">
          <div class="logo">
            <div class="logo-icon"><i class="fa fa-globe"></i></div>
            <span class="logo-text">旅迹</span>
          </div>
          <button
            ref="drawerCloseRef"
            class="icon-btn"
            title="关闭"
            aria-label="关闭导航"
            @click="closeMobileDrawer"
          >
            <i class="fa fa-times"></i>
          </button>
        </div>

        <div class="drawer-search">
          <select v-model="searchType" class="drawer-search-type" aria-label="搜索类型">
            <option value="spot">景点</option>
            <option value="guide">攻略</option>
            <option value="route">路线</option>
          </select>
          <input v-model="keyword" class="drawer-search-input" placeholder="输入关键词..." aria-label="搜索关键词" @keyup.enter="doSearch" />
          <button class="drawer-search-btn" aria-label="搜索" @click="doSearch"><i class="fa fa-search"></i></button>
        </div>

        <nav class="drawer-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="drawer-link"
            :class="{ active: isActive(item.path) }"
          >
            <span>{{ item.label }}</span>
            <i class="fa fa-angle-right"></i>
          </router-link>
        </nav>

        <div class="drawer-actions">
          <router-link to="/travel/home/publish" class="publish-btn drawer-publish"><i class="fa fa-pencil mr-1"></i>发布内容</router-link>
          <button class="drawer-msg" @click="router.push('/travel/home/messages'); closeMobileDrawer()">
            <i class="fa fa-bell-o"></i>消息通知
            <span v-if="unreadCount > 0" class="msg-badge-static">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </button>
        </div>

        <div class="drawer-user">
          <template v-if="authStore.isLoggedIn">
            <div class="drawer-user-row" @click="handleCommand('profile')">
              <el-avatar :size="34" :src="authStore.avatar"><i class="fa fa-user"></i></el-avatar>
              <div class="drawer-user-info">
                <span class="drawer-user-name">{{ authStore.nickname }}</span>
                <span class="drawer-user-sub">个人中心</span>
              </div>
              <i class="fa fa-angle-right"></i>
            </div>
            <button class="drawer-logout" @click="handleCommand('logout')"><i class="fa fa-sign-out mr-2"></i>退出登录</button>
          </template>
          <template v-else>
            <div class="drawer-auth">
              <router-link to="/travel/login" class="drawer-login">登录</router-link>
              <router-link to="/travel/register" class="publish-btn drawer-register">注册</router-link>
            </div>
          </template>
        </div>
      </aside>
    </transition>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getMessageUnreadCount } from '@/api/message'
import AnnouncementBanner from '@/components/user/AnnouncementBanner.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 消息未读数（铃铛徽标）
const unreadCount = ref(0)

// 移动端抽屉
const mobileOpen = ref(false)
const drawerCloseRef = ref(null)
let mobileTrigger = null

function openMobileDrawer() {
  mobileTrigger = document.activeElement
  mobileOpen.value = true
}

function closeMobileDrawer() {
  mobileOpen.value = false
}

// 打开时锁定页面滚动并聚焦关闭按钮；关闭时恢复焦点和滚动
watch(mobileOpen, async (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
  if (open) {
    await nextTick()
    drawerCloseRef.value?.focus()
  } else if (mobileTrigger && typeof mobileTrigger.focus === 'function') {
    mobileTrigger.focus()
    mobileTrigger = null
  }
})

const navItems = [
  { path: '/travel/home', label: '首页' },
  { path: '/travel/home/spots', label: '景点' },
  { path: '/travel/home/guides', label: '攻略' },
  { path: '/travel/home/routes', label: '路线' },
  { path: '/travel/home/planner', label: '路线规划' }
]

async function loadUnread() {
  const uid = authStore.userInfo?.id
  if (!uid) return
  try {
    const res = await getMessageUnreadCount()
    if (res.code === 1) unreadCount.value = Number(res.content || 0)
  } catch (e) { /* 忽略 */ }
}

onMounted(() => {
  loadUnread()
  // 从消息中心返回时刷新；路由切换时收起移动端抽屉
  watch(() => route.path, (p) => {
    mobileOpen.value = false
    if (!p.startsWith('/travel/home/messages')) loadUnread()
  })
  // 消息中心标记已读后通知刷新徽标（停留在消息页时路由不变）
  window.addEventListener('unread-refresh', loadUnread)
})

onBeforeUnmount(() => {
  window.removeEventListener('unread-refresh', loadUnread)
  document.body.style.overflow = ''
})

const keyword = ref('')
const searchType = ref('spot')

const searchPaths = {
  spot: '/travel/home/spots',
  guide: '/travel/home/guides',
  route: '/travel/home/routes'
}

function isActive(path) {
  if (path === '/travel/home') return route.path === '/travel/home'
  return route.path.startsWith(path)
}

function doSearch() {
  const path = searchPaths[searchType.value] || searchPaths.spot
  const kw = keyword.value.trim()
  router.push(kw ? { path, query: { keyword: kw } } : { path })
}

function handleCommand(cmd) {
  if (cmd === 'profile') {
    mobileOpen.value = false
    router.push('/travel/home/mine')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    }).then(() => {
      authStore.logout()
      router.push('/travel/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  background: var(--ink-50);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部导航 ===== */
.top-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  height: 64px;
}

.top-bar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: var(--brand-gradient);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 18px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.5px;
}

.nav-links {
  display: flex;
  gap: 32px;
  flex-shrink: 0;
}

.nav-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-500);
  text-decoration: none;
  transition: color 0.2s;
  position: relative;
}

.nav-link:hover { color: var(--brand-600); }
.nav-link.active { color: var(--ink-900); font-weight: 600; }
.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: -22px; left: 50%;
  transform: translateX(-50%);
  width: 20px; height: 3px;
  border-radius: 2px;
  background: var(--brand-500);
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

/* 顶部搜索框（迷你版 hero 搜索） */
.header-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--ink-100);
  border: 1px solid var(--ink-200);
  border-radius: 12px;
  padding: 0 12px;
  height: 38px;
  width: 240px;
  color: var(--ink-400);
  transition: all 0.25s;
}

.header-search:focus-within {
  border-color: rgba(28, 184, 157, 0.5);
  background: #fff;
}

.header-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--ink-800);
  min-width: 0;
}

.header-search-type {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-600);
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 4px;
}

.header-search-type:focus-visible {
  outline: none;
}

.header-search-input::placeholder { color: var(--ink-300); }

.icon-btn {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: transparent;
  border: none;
  color: var(--ink-400);
  font-size: 16px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.icon-btn:hover { background: var(--ink-100); color: var(--ink-600); }

.msg-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #FF6B6B;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
  box-sizing: border-box;
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 10px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
}

.publish-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--brand-shadow);
}

.user-avatar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 24px;
  transition: background 0.2s;
}

.user-avatar-wrap:hover { background: var(--ink-100); }

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-800);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 未登录按钮 */
.auth-btns {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-500);
  text-decoration: none;
  transition: color 0.2s;
}

.auth-link:hover { color: var(--brand-600); }

.auth-register {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  padding: 8px 18px;
  border-radius: 10px;
  background: var(--brand-gradient);
  transition: all 0.2s;
}

.auth-register:hover {
  transform: translateY(-1px);
  box-shadow: var(--brand-shadow);
}

/* 移动端菜单按钮（≤900px 显示） */
.menu-toggle {
  display: none;
  width: 38px; height: 38px;
  border-radius: 10px;
  background: transparent;
  border: none;
  color: var(--ink-600);
  font-size: 18px;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.menu-toggle:hover { background: var(--ink-100); }

/* ===== 主体 ===== */
.main-content {
  flex: 1;
  padding-top: 64px;
}

/* ===== Footer ===== */
.footer {
  background: var(--ink-900);
  padding: 48px 24px 32px;
  margin-top: 0;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-brand {
  margin-bottom: 20px;
}

.footer-logo {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6px;
}

.footer-desc {
  font-size: 13px;
  color: rgba(255,255,255,0.4);
  margin: 0;
}

.footer-links {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.footer-links a {
  font-size: 13px;
  color: rgba(255,255,255,0.5);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-links a:hover { color: rgba(255,255,255,0.8); }

.footer-copy {
  font-size: 12px;
  color: rgba(255,255,255,0.3);
  border-top: 1px solid rgba(255,255,255,0.06);
  padding-top: 20px;
}

/* ===== 移动端抽屉 ===== */
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(13, 27, 42, 0.45);
  backdrop-filter: blur(2px);
  z-index: 200;
}

.mobile-drawer {
  position: fixed;
  top: 0; right: 0; bottom: 0;
  width: 300px;
  max-width: 86vw;
  background: #fff;
  z-index: 201;
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
  overflow-y: auto;
  box-shadow: -12px 0 40px rgba(13, 27, 42, 0.15);
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.drawer-search {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--ink-100);
  border: 1px solid var(--ink-200);
  border-radius: 12px;
  padding: 0 10px;
  height: 42px;
  margin-bottom: 16px;
}

.drawer-search-type {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-600);
  flex-shrink: 0;
}

.drawer-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--ink-800);
}

.drawer-search-btn {
  width: 30px; height: 30px;
  border: none;
  border-radius: 8px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
}

.drawer-nav {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}

.drawer-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 12px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  color: var(--ink-600);
  text-decoration: none;
  transition: all 0.2s;
}

.drawer-link i { color: var(--ink-300); font-size: 14px; }

.drawer-link:hover { background: var(--ink-50); }

.drawer-link.active {
  background: var(--brand-50);
  color: var(--brand-600);
  font-weight: 600;
}

.drawer-link.active i { color: var(--brand-600); }

.drawer-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--ink-100);
  margin-bottom: 16px;
}

.drawer-publish { width: 100%; padding: 11px 0; }

.drawer-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 14px;
  border-radius: 10px;
  border: 1px solid var(--ink-200);
  background: #fff;
  color: var(--ink-600);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.msg-badge-static {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #FF6B6B;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.drawer-user { margin-top: auto; }

.drawer-user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.drawer-user-row:hover { background: var(--ink-50); }

.drawer-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.drawer-user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-user-sub {
  font-size: 12px;
  color: var(--ink-400);
}

.drawer-user-row > i { color: var(--ink-300); }

.drawer-logout {
  width: 100%;
  padding: 11px 0;
  border-radius: 10px;
  border: 1px solid rgba(255, 107, 107, 0.35);
  background: #FFF5F5;
  color: #FF6B6B;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}

.drawer-auth {
  display: flex;
  gap: 10px;
}

.drawer-login {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 11px 0;
  border-radius: 10px;
  border: 1px solid var(--ink-200);
  color: var(--ink-600);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}

.drawer-register {
  flex: 1;
  padding: 11px 0;
  border-radius: 10px;
  font-size: 14px;
}

/* 抽屉动画 */
.mask-enter-active, .mask-leave-active { transition: opacity 0.25s ease; }
.mask-enter-from, .mask-leave-to { opacity: 0; }

.slide-enter-active, .slide-leave-active { transition: transform 0.28s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .header-search { display: none; }
  .menu-toggle { display: flex; }
}

@media (max-width: 600px) {
  .nav-links { display: none; }
  /* 只隐藏顶栏发布按钮，抽屉里的“发布内容/注册”仍需保留 */
  .top-bar-right .publish-btn { display: none; }
  /* 320px 下收窄用户区，避免菜单按钮被挤出视口 */
  .top-bar-right { gap: 8px; }
  .user-name { display: none; }
  .user-avatar-wrap { padding: 4px; }
}
</style>
