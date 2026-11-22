<template>
  <div class="auth-page">
    <div class="auth-scene" :style="{ backgroundImage: `url(${brandScene})` }"></div>
    <div class="auth-veil"></div>

    <!-- 左侧品牌主张 -->
    <aside class="auth-side auth-side-left">
      <p class="auth-side-title">旅迹</p>
      <span class="auth-side-rule"></span>
      <p class="auth-side-platform">旅游信息管理平台</p>
      <p class="auth-side-tagline">探索世界之美，从这里开始</p>
    </aside>

    <!-- 右侧能力清单 -->
    <aside class="auth-side auth-side-right">
      <ul class="auth-side-list">
        <li><b>景点</b><span>山湖与城市，按主题浏览</span></li>
        <li><b>路线</b><span>把想去的地方连成一条线</span></li>
        <li><b>攻略</b><span>出发前，先看看别人怎么走</span></li>
        <li><b>收藏</b><span>想去的先存着，随时回看</span></li>
      </ul>
    </aside>

    <!-- 居中纸面 -->
    <main class="auth-sheet">
      <div class="auth-brand">
        <span class="auth-mark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 21s6.5-5.9 6.5-10.5a6.5 6.5 0 1 0-13 0C5.5 15.1 12 21 12 21Z" />
            <circle cx="12" cy="10.4" r="2.4" />
          </svg>
        </span>
        <span class="auth-brand-name">旅迹</span>
        <span class="auth-brand-divider"></span>
        <span class="auth-brand-sub">旅游信息管理平台</span>
      </div>

      <div class="auth-sheet-body">
        <div class="auth-head">
          <h1>欢迎回来</h1>
          <p>登录后继续你的旅程，收藏的景点与路线都还在。</p>
        </div>

        <el-form
          ref="formRef"
          class="auth-form"
          :model="form"
          :rules="rules"
          @keyup.enter="handleLogin"
          @submit.prevent
        >
          <el-form-item prop="account">
            <div class="auth-field-head">
              <label class="auth-label" for="login-account">账号</label>
            </div>
            <el-input
              id="login-account"
              v-model="form.account"
              placeholder="请输入账号"
              autocomplete="username"
              clearable
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="8" r="3.6" />
                  <path d="M4.8 20.2c1.2-3.6 4-5.4 7.2-5.4s6 1.8 7.2 5.4" />
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <div class="auth-field-head">
              <label class="auth-label" for="login-password">密码</label>
            </div>
            <el-input
              id="login-password"
              v-model="form.password"
              :type="pwdVisible ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="4.6" y="10.4" width="14.8" height="9.8" rx="2.4" />
                  <path d="M8.2 10.4V8a3.8 3.8 0 0 1 7.6 0v2.4" />
                </svg>
              </template>
              <template #suffix>
                <button
                  type="button"
                  class="auth-peek"
                  :aria-label="pwdVisible ? '隐藏密码' : '显示密码'"
                  :aria-pressed="pwdVisible"
                  @click="pwdVisible = !pwdVisible"
                >
                  <svg v-if="!pwdVisible" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2.8 12S6.4 6.6 12 6.6 21.2 12 21.2 12 17.6 17.4 12 17.4 2.8 12 2.8 12Z" />
                    <circle cx="12" cy="12" r="2.7" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4.2 8.4C3.4 9.6 2.8 12 2.8 12s3.6 5.4 9.2 5.4c1.5 0 2.8-.4 3.9-1" />
                    <path d="M19.2 15.2c1.3-1.3 2-3.2 2-3.2s-3.6-5.4-9.2-5.4c-.8 0-1.5.1-2.2.3" />
                    <path d="M4 4l16 16" />
                  </svg>
                </button>
              </template>
            </el-input>
          </el-form-item>

          <div class="auth-options">
            <label class="auth-check">
              <input v-model="remember" type="checkbox" />
              <span class="auth-box">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5 12.5l4.5 4.5L19 7.5" />
                </svg>
              </span>
              记住我
            </label>
            <button type="button" class="auth-link" @click="handleForgot">忘记密码？</button>
          </div>

          <button type="button" class="auth-cta" :disabled="loading" @click="handleLogin">
            <span v-if="loading" class="auth-spinner"></span>
            {{ loading ? '登录中…' : '登 录' }}
          </button>
        </el-form>

        <div class="auth-switch">
          还没有账户？
          <router-link to="/travel/register">立即注册</router-link>
        </div>

        <div class="auth-divider"></div>
        <div class="auth-foot-note">
          <span>© 2026 旅迹 · 旅游信息管理平台</span>
          <a href="#">遇到问题？</a>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import brandScene from '@/assets/images/hero-scene.svg'
import '@/assets/css/auth.css'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const remember = ref(true)
const pwdVisible = ref(false)

const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value || loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const user = await authStore.login(form.account, form.password, remember.value)
    ElMessage.success(`欢迎回来，${user?.nickname || form.account}`)
    // 管理员进后台，普通用户进用户端（与路由守卫的判定保持一致）
    router.replace(authStore.isAdmin ? '/travel' : '/travel/home')
  } catch (err) {
    // 接口层的报错已由 request 拦截器统一提示，这里只补本地校验类的错误
    if (err?.message === '登录失败：未获取到有效的令牌') {
      ElMessage.error('登录失败：未获取到有效凭证，请联系管理员')
    }
  } finally {
    loading.value = false
  }
}

function handleForgot() {
  ElMessage.info('当前版本未开放自助找回，请联系管理员重置密码')
}
</script>
