<template>
  <div class="login-wrapper">
    <!-- 背景装饰 -->
    <div class="bg-decorations">
      <div class="deco-circle deco-circle-1"></div>
      <div class="deco-circle deco-circle-2"></div>
      <div class="deco-circle deco-circle-3"></div>
      <div class="deco-wave"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <img :src="brandScene" class="brand-scene" alt="" />
        <div class="brand-content">
          <div class="brand-icon">
            <i class="fa fa-globe"></i>
          </div>
          <h1 class="brand-title">旅游信息管理平台</h1>
          <p class="brand-subtitle">探索世界之美，从这里开始</p>
        </div>
        <div class="brand-footer">
          <p>毕业设计 · 旅游信息管理平台</p>
        </div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-desc">请登录您的账户以继续</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="0" status-icon @keyup.enter="handleLogin" size="large">
            <el-form-item prop="account">
              <div class="input-wrap">
                <i class="fa fa-user input-icon"></i>
                <el-input v-model="form.account" placeholder="请输入账号" clearable :prefix-icon="null" />
              </div>
            </el-form-item>
            <el-form-item prop="password">
              <div class="input-wrap">
                <i class="fa fa-lock input-icon"></i>
                <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="null" />
              </div>
            </el-form-item>

            <div class="form-options">
              <label class="remember-me">
                <input type="checkbox" v-model="remember" />
                <span class="checkmark"></span>
                <span class="label-text">记住我</span>
              </label>
            </div>

            <el-button
              class="login-btn"
              type="primary"
              :loading="loading"
              @click="handleLogin"
            >
              <i v-if="!loading" class="fa fa-sign-in mr-2"></i>{{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form>

          <div class="form-footer">
            <p>还没有账户？<router-link to="/travel/register">立即注册</router-link></p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import brandScene from '@/assets/images/hero-scene.svg'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const remember = ref(true)

const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, message: '密码长度不少于 3 位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const user = await authStore.login(form.account, form.password)
    ElMessage.success(`欢迎回来，${user.nickname || '用户'}！`)
    // 401 跳转登录时会带上原页面地址，登录后回跳；仅允许站内路径
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/travel')) {
      await router.replace(redirect)
    } else if (user.userRole === 1) {
      await router.replace('/travel')
    } else {
      await router.replace('/travel/home')
    }
  } catch (err) {
    ElMessage.error(err.message || '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (authStore.isLoggedIn) {
    router.replace(authStore.isAdmin ? '/travel' : '/travel/home')
  }
})
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0D1B2A 0%, #1A2D42 30%, #132D46 60%, #0B1E2F 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 背景装饰 ===== */
.bg-decorations {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.deco-circle-1 {
  width: 600px; height: 600px;
  background: radial-gradient(circle, #1CB89D, transparent);
  top: -200px; right: -150px;
  animation: float 20s ease-in-out infinite;
}

.deco-circle-2 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, #3B82F6, transparent);
  bottom: -100px; left: -100px;
  animation: float 25s ease-in-out infinite reverse;
}

.deco-circle-3 {
  width: 200px; height: 200px;
  background: radial-gradient(circle, #1CB89D, transparent);
  top: 40%; left: 20%;
  animation: float 15s ease-in-out infinite 5s;
}

.deco-wave {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 120px;
  background: linear-gradient(180deg, transparent, rgba(28, 184, 157, 0.04));
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

/* ===== 主容器 ===== */
.login-container {
  display: flex;
  width: 920px;
  max-width: 94vw;
  min-height: 580px;
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  box-shadow:
    0 25px 60px rgba(0, 0, 0, 0.3),
    0 8px 20px rgba(0, 0, 0, 0.15);
}

/* ===== 左侧品牌区 ===== */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #0D9488 0%, #1CB89D 40%, #0D1B2A 100%);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

/* 品牌区底部山水插画 */
.brand-scene {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  width: 100%;
  height: 46%;
  object-fit: cover;
  object-position: center bottom;
  opacity: 0.9;
  pointer-events: none;
}

.brand-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.5;
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-icon {
  width: 64px; height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  font-size: 28px;
  color: #fff;
  margin-bottom: 24px;
}

.brand-title {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  line-height: 1.3;
}

.brand-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

.brand-footer {
  position: relative;
  z-index: 1;
}

.brand-footer p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
}

/* ===== 右侧登录表单区 ===== */
.form-section {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #1E293B;
  margin: 0 0 6px;
}

.form-desc {
  font-size: 14px;
  color: #94A3B8;
  margin: 0;
}

/* 输入框容器 */
:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__error) {
  padding-left: 36px;
}

.input-wrap {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #94A3B8;
  font-size: 15px;
  z-index: 2;
  pointer-events: none;
}

:deep(.el-input__wrapper) {
  padding-left: 40px !important;
  height: 48px;
  border-radius: 12px !important;
  box-shadow: 0 0 0 1px #E2E8F0 inset !important;
  background: #F8FAFC;
  transition: all 0.25s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #1CB89D inset !important;
  background: #fff;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(28, 184, 157, 0.25) inset !important;
  background: #fff;
}

:deep(.el-input__inner) {
  font-size: 14px;
  color: #1E293B;
}

:deep(.el-input__inner::placeholder) {
  color: #CBD5E1;
}

/* 选项 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.remember-me input {
  display: none;
}

.checkmark {
  width: 18px; height: 18px;
  border-radius: 4px;
  border: 2px solid #CBD5E1;
  position: relative;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.remember-me input:checked + .checkmark {
  background: #1CB89D;
  border-color: #1CB89D;
}

.remember-me input:checked + .checkmark::after {
  content: '';
  position: absolute;
  left: 4px; top: 1px;
  width: 6px; height: 10px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.label-text {
  font-size: 13px;
  color: #64748B;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none !important;
  background: linear-gradient(135deg, #1CB89D, #0D9488) !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 15px rgba(28, 184, 157, 0.3) !important;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(28, 184, 157, 0.4) !important;
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn.is-loading {
  background: linear-gradient(135deg, #94A3B8, #64748B) !important;
  box-shadow: none !important;
}

/* 底部 */
.form-footer {
  margin-top: 24px;
  text-align: center;
}

.form-footer p {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

.form-footer a {
  color: #1CB89D;
  text-decoration: none;
  font-weight: 500;
}

.form-footer a:hover {
  color: #0D9488;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    min-height: auto;
    width: 96vw;
    border-radius: 16px;
  }

  .brand-section {
    padding: 32px 28px;
  }

  .form-section {
    padding: 36px 28px;
  }
}
</style>
