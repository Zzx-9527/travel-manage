<template>
  <div class="register-wrapper">
    <!-- 背景装饰 -->
    <div class="bg-decorations">
      <div class="deco-circle deco-circle-1"></div>
      <div class="deco-circle deco-circle-2"></div>
      <div class="deco-circle deco-circle-3"></div>
      <div class="deco-wave"></div>
    </div>

    <div class="register-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <img :src="brandScene" class="brand-scene" alt="" />
        <div class="brand-content">
          <div class="brand-icon">
            <i class="fa fa-globe"></i>
          </div>
          <h1 class="brand-title">旅迹 · 旅游信息管理平台</h1>
          <p class="brand-subtitle">探索世界之美，从这里开始</p>
        </div>
        <div class="brand-footer">
          <p>毕业设计 · 旅游信息管理平台</p>
        </div>
      </div>

      <!-- 右侧注册表单区 -->
      <div class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h2 class="form-title">创建账户</h2>
            <p class="form-desc">注册后开启你的旅程</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="0" status-icon @keyup.enter="handleRegister" size="large">
            <el-form-item prop="account">
              <div class="input-wrap">
                <i class="fa fa-user input-icon"></i>
                <el-input v-model="form.account" placeholder="请输入账号（3-20位）" clearable />
              </div>
            </el-form-item>
            <el-form-item prop="password">
              <div class="input-wrap">
                <i class="fa fa-lock input-icon"></i>
                <el-input v-model="form.password" type="password" placeholder="请输入密码（至少3位）" show-password />
              </div>
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <div class="input-wrap">
                <i class="fa fa-lock input-icon"></i>
                <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
              </div>
            </el-form-item>
            <el-form-item prop="nickname">
              <div class="input-wrap">
                <i class="fa fa-id-card input-icon"></i>
                <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
              </div>
            </el-form-item>
            <el-form-item prop="avatar">
              <div class="avatar-row">
                <el-avatar :size="48" :src="form.avatar" class="avatar-preview">
                  <i class="fa fa-user"></i>
                </el-avatar>
                <div class="input-wrap avatar-input">
                  <i class="fa fa-image input-icon"></i>
                  <el-input v-model="form.avatar" placeholder="头像图片 URL（选填，留空使用默认头像）" clearable />
                </div>
              </div>
            </el-form-item>
            <el-form-item prop="phone">
              <div class="input-wrap">
                <i class="fa fa-phone input-icon"></i>
                <el-input v-model="form.phone" placeholder="请输入手机号（选填）" maxlength="11" />
              </div>
            </el-form-item>

            <el-button
              class="register-btn"
              type="primary"
              :loading="loading"
              @click="handleRegister"
            >
              <i v-if="!loading" class="fa fa-user-plus mr-2"></i>{{ loading ? '注册中...' : '注 册' }}
            </el-button>
          </el-form>

          <div class="form-footer">
            <p>已有账户？<router-link to="/travel/login">去登录</router-link></p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { register } from '@/api/userCenter'
import brandScene from '@/assets/images/hero-scene.svg'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  avatar: '',
  phone: ''
})

const validateConfirm = (rule, value, callback) => {
  if (!value) callback(new Error('请再次输入密码'))
  else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, message: '密码长度不少于 3 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      account: form.account,
      password: form.password,
      nickname: form.nickname,
      phone: form.phone || undefined,
      // 头像选填：填了就带上，留空传 undefined → userCenter 用默认头像
      avatar: form.avatar.trim() || undefined
    })
    // 注册成功 → 尝试自动登录
    try {
      const user = await authStore.login(form.account, form.password)
      ElMessage.success(`欢迎加入，${user.nickname || form.nickname}！`)
      router.replace('/travel/home')
    } catch (err) {
      ElMessage.success('注册成功，请登录')
      router.replace('/travel/login')
    }
  } catch (err) {
    // 注册失败（如账号已存在）：拦截器/提示已展示，这里不重复
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-wrapper {
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
.register-container {
  display: flex;
  width: 920px;
  max-width: 94vw;
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
  font-size: 24px;
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

/* ===== 右侧注册表单区 ===== */
.form-section {
  flex: 1.2;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 40px;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 26px;
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

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__error) {
  padding-left: 36px;
}

.input-wrap {
  position: relative;
}

/* 头像：左侧预览 + 右侧 URL 输入 */
.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.avatar-preview {
  flex-shrink: 0;
  background: #F1F5F9;
  border: 2px solid #E2E8F0;
  color: #94A3B8;
  font-size: 20px;
}

.avatar-input {
  flex: 1;
  min-width: 0;
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

/* 注册按钮 */
.register-btn {
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
  margin-top: 4px;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(28, 184, 157, 0.4) !important;
}

.register-btn.is-loading {
  background: linear-gradient(135deg, #94A3B8, #64748B) !important;
  box-shadow: none !important;
}

/* 底部 */
.form-footer {
  margin-top: 22px;
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
  .register-container {
    flex-direction: column;
    min-height: auto;
    width: 96vw;
    border-radius: 16px;
  }

  .brand-section {
    padding: 28px 24px;
  }

  .form-section {
    padding: 32px 24px;
  }
}
</style>
