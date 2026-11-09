<template>
  <div class="flex h-screen overflow-hidden" style="background-color: #F8FAFC">
    <!-- 窄屏侧栏遮罩 -->
    <div v-if="sidebarOpen" class="sidebar-mask" @click="sidebarOpen = false"></div>

    <!-- 侧边栏 -->
    <aside
      class="admin-aside flex-shrink-0 flex flex-col transition-all duration-300 relative z-20"
      :class="{ open: sidebarOpen }"
      style="width: 240px; background: linear-gradient(180deg, #0D1B2A 0%, #132D46 100%);"
    >
      <!-- Logo 区域 -->
      <div class="h-16 flex items-center px-5 border-b border-white/10 flex-shrink-0">
        <div class="w-9 h-9 rounded-lg flex items-center justify-center text-lg" style="background: linear-gradient(135deg, #1CB89D, #0D9488);">
          <i class="fa fa-globe text-white text-base"></i>
        </div>
        <span class="ml-3 text-white font-bold tracking-wide text-base">旅游信息管理平台</span>
      </div>

      <!-- 菜单区域 -->
      <nav class="flex-1 overflow-y-auto py-3 px-3">
        <div class="mb-2 px-3 text-xs font-medium tracking-wider uppercase" style="color: #5A738E;">主菜单</div>
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg mb-0.5 transition-all duration-200 group"
          :class="isActive(item.path) ? 'menu-item-active' : 'menu-item-default'"
        >
          <i
            class="fa text-base w-5 text-center transition-all duration-200"
            :class="[item.icon, isActive(item.path) ? 'text-white' : '']"
            :style="isActive(item.path) ? {} : { color: '#5A738E' }"
          ></i>
          <span class="text-sm font-medium transition-all duration-200">{{ item.label }}</span>
          <div
            v-if="item.badge"
            class="ml-auto text-xs px-2 py-0.5 rounded-full"
            style="background: rgba(28, 184, 157, 0.2); color: #1CB89D;"
          >
            {{ item.badge }}
          </div>
        </router-link>
      </nav>

      <!-- 底部版本信息 -->
      <div class="px-5 py-3 border-t border-white/10 flex-shrink-0">
        <p class="text-xs" style="color: #5A738E;">v1.0.0 · 毕业设计</p>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 顶部导航栏 -->
      <header class="h-16 flex-shrink-0 flex items-center justify-between px-6 bg-white shadow-sm border-b border-gray-100">
        <div class="flex items-center gap-3">
          <!-- 窄屏菜单按钮 -->
          <button
            class="w-9 h-9 rounded-lg flex items-center justify-center hover:bg-gray-100 transition-colors lg:hidden"
            style="color: #475569;"
            title="菜单"
            @click="sidebarOpen = !sidebarOpen"
          >
            <i class="fa fa-bars text-base"></i>
          </button>
          <h2 class="text-lg font-semibold" style="color: #1E293B;">{{ currentTitle }}</h2>
        </div>

        <div class="flex items-center gap-4">
          <!-- 全屏按钮 -->
          <button
            class="w-9 h-9 rounded-lg flex items-center justify-center hover:bg-gray-100 transition-colors"
            style="color: #94A3B8;"
            :title="isFullscreen ? '退出全屏' : '全屏'"
            @click="toggleFullscreen"
          >
            <i :class="['fa text-sm', isFullscreen ? 'fa-compress' : 'fa-expand']"></i>
          </button>

          <!-- 用户下拉菜单 -->
          <el-dropdown trigger="click" placement="bottom-end" @command="handleUserCommand">
            <div class="flex items-center gap-2.5 cursor-pointer px-2 py-1 rounded-lg hover:bg-gray-50 transition-colors">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center text-white text-sm font-medium overflow-hidden"
                style="background: linear-gradient(135deg, #1CB89D, #0D9488);"
              >
                <el-avatar v-if="authStore.avatar" :size="32" :src="authStore.avatar" style="flex-shrink: 0;" />
                <i v-else class="fa fa-user"></i>
              </div>
              <div class="flex flex-col leading-tight">
                <span class="text-sm font-medium" style="color: #1E293B;">{{ authStore.nickname }}</span>
                <span class="text-xs" style="color: #94A3B8;">{{ authStore.roleName }}</span>
              </div>
              <i class="fa fa-chevron-down text-xs ml-1" style="color: #94A3B8;"></i>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <i class="fa fa-user-circle mr-2"></i>用户信息
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <i class="fa fa-key mr-2"></i>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <i class="fa fa-sign-out mr-2" style="color: #FF6B6B;"></i>
                  <span style="color: #FF6B6B;">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区域 -->
      <main class="flex-1 overflow-y-auto">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 用户信息对话框 -->
    <el-dialog v-model="profileVisible" title="用户信息" :width="460" top="12vh">
      <el-form label-width="72px">
        <el-form-item label="账号">
          <el-input :model-value="profileForm.account" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" maxlength="20" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" maxlength="11" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="profileForm.avatar" placeholder="头像图片 URL（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileSaving" @click="saveProfile" style="border-radius: 8px; background: var(--brand-gradient); border: none;">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="pwdVisible" title="修改密码" :width="420" top="14vh">
      <el-form label-width="72px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="savePassword" style="border-radius: 8px; background: var(--brand-gradient); border: none;">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getMyProfile, updateProfile, updatePassword } from '@/api/userCenter'
import { getMessageUnreadCount } from '@/api/message'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentTitle = computed(() => route.meta?.title || '系统概览')

// 窄屏（≤1024px）侧栏抽屉开关；路由切换时自动收起
const sidebarOpen = ref(false)
watch(() => route.path, () => { sidebarOpen.value = false })

// 全屏切换（纯前端，不依赖后端）
const isFullscreen = ref(false)

function syncFullscreen() {
  isFullscreen.value = !!document.fullscreenElement
}

function toggleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen?.().catch(() => {})
  } else {
    document.documentElement.requestFullscreen?.().catch(() => {})
  }
}

onMounted(() => {
  document.addEventListener('fullscreenchange', syncFullscreen)
  loadUnread()
})

onUnmounted(() => document.removeEventListener('fullscreenchange', syncFullscreen))

// ---- 消息未读数（铃铛徽标）----
const unreadCount = ref(0)

async function loadUnread() {
  const uid = authStore.userInfo?.id
  if (!uid) return
  try {
    const res = await getMessageUnreadCount()
    if (res.code === 1) unreadCount.value = Number(res.content || 0)
  } catch (e) { /* 忽略，保持上次值 */ }
}

// 从消息中心返回时刷新未读数
watch(() => route.path, (p) => {
  if (!p.startsWith('/travel/messages')) loadUnread()
})

const menuItems = ref([
  { path: '/travel/dashboard', icon: 'fa-tachometer', label: '系统概览' },
  { path: '/travel/users', icon: 'fa-users', label: '用户管理' },
  { path: '/travel/categories', icon: 'fa-tags', label: '景点分类管理' },
  { path: '/travel/spots', icon: 'fa-map-marker', label: '景点管理' },
  { path: '/travel/guides', icon: 'fa-book', label: '旅游攻略管理' },
  { path: '/travel/routes', icon: 'fa-road', label: '旅游路线管理' },
  { path: '/travel/route-details', icon: 'fa-flag-checkered', label: '路线途经景点管理' },
  { path: '/travel/comments', icon: 'fa-comments', label: '评论管理' },
  { path: '/travel/favorites', icon: 'fa-heart', label: '收藏管理' },
  { path: '/travel/orders', icon: 'fa-ticket', label: '景点门票订单管理' },
  { path: '/travel/announcements', icon: 'fa-bullhorn', label: '公告管理' },
])

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

function handleUserCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      authStore.logout()
      router.push('/travel/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    openProfile()
  } else if (command === 'password') {
    pwdVisible.value = true
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  }
}

// ---- 用户信息 ----
const profileVisible = ref(false)
const profileSaving = ref(false)
const profileForm = ref({ account: '', nickname: '', phone: '', avatar: '' })

async function openProfile() {
  profileForm.value = {
    account: authStore.userInfo?.account || '',
    nickname: authStore.nickname,
    phone: authStore.userInfo?.phone || '',
    avatar: authStore.avatar
  }
  profileVisible.value = true
  try {
    const res = await getMyProfile()
    if (res.code === 1 && res.content) {
      profileForm.value = {
        account: res.content.account || profileForm.value.account,
        nickname: res.content.nickname || profileForm.value.nickname,
        phone: res.content.phone || '',
        avatar: res.content.avatar || ''
      }
    }
  } catch (e) { /* 用本地登录信息回填 */ }
}

async function saveProfile() {
  if (!profileForm.value.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  profileSaving.value = true
  try {
    // 只提交白名单字段（§2.1）：updateProfile 身份取 token，password/account 等字段一律不传
    const res = await updateProfile({
      nickname: profileForm.value.nickname.trim(),
      phone: profileForm.value.phone?.trim() || '',
      avatar: profileForm.value.avatar?.trim() || ''
    })
    if (res?.code === 1) {
      authStore.saveAuth(authStore.token, {
        ...authStore.userInfo,
        nickname: profileForm.value.nickname.trim(),
        phone: profileForm.value.phone?.trim() || '',
        avatar: profileForm.value.avatar?.trim() || ''
      })
      ElMessage.success('资料已保存')
      profileVisible.value = false
    } else {
      ElMessage.error(res?.msg || `保存失败（code=${res?.code ?? '无响应码'}）`)
    }
  } catch (e) {
    ElMessage.error(e?.response?.status ? `保存失败（HTTP ${e.response.status}）` : (e?.message || '保存失败，请检查网络'))
  }
  finally { profileSaving.value = false }
}

// ---- 修改密码 ----
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function savePassword() {
  const f = pwdForm.value
  if (!f.oldPassword || !f.newPassword) {
    ElMessage.warning('请填写完整的密码')
    return
  }
  if (f.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (f.newPassword !== f.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    // §3：updatePassword 自带原密码校验；改密成功不踢已有会话，保持当前登录即可
    await updatePassword({
      oldPassword: f.oldPassword,
      newPassword: f.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    // 失败提示由拦截器统一弹出（原密码不正确 / 网络错误），这里不再重复弹
  }
  finally { pwdSaving.value = false }
}
</script>

<style scoped>
/* 窄屏侧栏抽屉化 */
.sidebar-mask {
  display: none;
}

@media (max-width: 1024px) {
  .admin-aside {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 60;
    transform: translateX(-100%);
    box-shadow: none;
  }

  .admin-aside.open {
    transform: translateX(0);
    box-shadow: 0 0 48px rgba(0, 0, 0, 0.35);
  }

  .sidebar-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(13, 27, 42, 0.45);
    z-index: 55;
  }
}

.msg-badge {
  position: absolute;
  top: 3px;
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

.menu-item-default {
  color: #94A3B8;
  transition: all 0.2s ease;
}
.menu-item-default:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #E2E8F0;
}
.menu-item-default:hover i {
  color: #94A3B8 !important;
}

.menu-item-active {
  background: linear-gradient(135deg, rgba(28, 184, 157, 0.15) 0%, rgba(28, 184, 157, 0.05) 100%);
  color: #FFFFFF;
  box-shadow: inset 3px 0 0 0 #1CB89D;
}
.menu-item-active i {
  color: #1CB89D !important;
}
</style>
