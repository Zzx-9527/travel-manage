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
          <h1>创建账户</h1>
          <p>注册后开启你的旅程，收藏、规划、发布都用这个账号。</p>
        </div>

        <el-form
          ref="formRef"
          class="auth-form"
          :model="form"
          :rules="rules"
          @keyup.enter="handleRegister"
          @submit.prevent
        >
          <div class="auth-group">账号信息</div>

          <el-form-item prop="account">
            <div class="auth-field-head">
              <label class="auth-label" for="reg-account">账号</label>
              <span class="auth-optional">4–20 位</span>
            </div>
            <el-input
              id="reg-account"
              v-model="form.account"
              placeholder="设置登录账号"
              autocomplete="username"
              maxlength="20"
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
              <label class="auth-label" for="reg-password">密码</label>
              <span class="auth-optional">6–20 位</span>
            </div>
            <el-input
              id="reg-password"
              v-model="form.password"
              :type="pwdVisible ? 'text' : 'password'"
              placeholder="设置密码"
              autocomplete="new-password"
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
            <div class="auth-strength" :data-level="strength">
              <i></i><i></i><i></i><b>{{ strengthText }}</b>
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <div class="auth-field-head">
              <label class="auth-label" for="reg-confirm">确认密码</label>
            </div>
            <el-input
              id="reg-confirm"
              v-model="form.confirmPassword"
              :type="confirmVisible ? 'text' : 'password'"
              placeholder="再次输入密码"
              autocomplete="new-password"
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="4.6" y="10.4" width="14.8" height="9.8" rx="2.4" />
                  <path d="M8.2 10.4V8a3.8 3.8 0 0 1 7.6 0v2.4" />
                  <path d="M10 15.2l1.6 1.6 3-3" />
                </svg>
              </template>
              <template #suffix>
                <button
                  type="button"
                  class="auth-peek"
                  :aria-label="confirmVisible ? '隐藏密码' : '显示密码'"
                  :aria-pressed="confirmVisible"
                  @click="confirmVisible = !confirmVisible"
                >
                  <svg v-if="!confirmVisible" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
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

          <div class="auth-group">个人资料</div>

          <el-form-item prop="nickname">
            <div class="auth-field-head">
              <label class="auth-label" for="reg-nickname">昵称</label>
            </div>
            <el-input
              id="reg-nickname"
              v-model="form.nickname"
              placeholder="给自己起个名字"
              maxlength="20"
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3.4" y="5.6" width="17.2" height="12.8" rx="2.4" />
                  <circle cx="9" cy="11" r="1.9" />
                  <path d="M6.2 15.7c.6-1.5 1.6-2.3 2.8-2.3s2.2.8 2.8 2.3M14.6 10.4h3.4M14.6 13.2h2.4" />
                </svg>
              </template>
              <template #suffix>
                <span class="auth-avatar-chip" :style="{ background: avatarColor }" aria-hidden="true">
                  {{ avatarInitial }}
                </span>
              </template>
            </el-input>
            <p class="auth-hint">头像先按昵称首字预览，注册后可在个人中心上传自己的照片。</p>
          </el-form-item>

          <el-form-item prop="phone">
            <div class="auth-field-head">
              <label class="auth-label" for="reg-phone">手机号</label>
              <span class="auth-optional">选填</span>
            </div>
            <el-input
              id="reg-phone"
              v-model="form.phone"
              type="tel"
              placeholder="用于找回账号"
              maxlength="11"
            >
              <template #prefix>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M7 3.8h2.1l1.4 3.5-1.8 1.3a12.4 12.4 0 0 0 5.7 5.7l1.3-1.8 3.5 1.4V16a1.6 1.6 0 0 1-1.7 1.6A15.7 15.7 0 0 1 5.4 5.5 1.6 1.6 0 0 1 7 3.8Z" />
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <div style="height: 2px"></div>

          <button type="button" class="auth-cta" :disabled="loading" @click="handleRegister">
            <span v-if="loading" class="auth-spinner"></span>
            {{ loading ? '注册中…' : '注 册' }}
          </button>
        </el-form>

        <div class="auth-switch">
          已有账户？
          <router-link to="/travel/login">去登录</router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { register } from '@/api/userCenter'
import brandScene from '@/assets/images/hero-scene.svg'
import '@/assets/css/auth.css'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const pwdVisible = ref(false)
const confirmVisible = ref(false)

const form = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  nickname: '',
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
    { min: 4, max: 20, message: '账号长度为 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

// 昵称首字头像：颜色由字符码决定，同一个昵称每次颜色一致
const AVATAR_COLORS = ['#0F766E', '#0B7C72', '#15726B', '#1B5E7A', '#2A5D80', '#0B4A6F']
const avatarInitial = computed(() => {
  const v = form.nickname.trim()
  return v ? Array.from(v)[0] : '旅'
})
const avatarColor = computed(() => {
  const code = avatarInitial.value.codePointAt(0) || 0
  return AVATAR_COLORS[code % AVATAR_COLORS.length]
})

const strength = computed(() => {
  const v = form.password
  if (v.length < 6) return 0
  if (v.length >= 12 && /[a-zA-Z]/.test(v) && /\d/.test(v) && /[^a-zA-Z0-9]/.test(v)) return 3
  if (v.length >= 10 && /[a-zA-Z]/.test(v) && /\d/.test(v)) return 2
  return 1
})
const strengthText = computed(() => ['强度', '偏弱', '中等', '很强'][strength.value])

async function handleRegister() {
  if (!formRef.value || loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 头像不在这里填：留空即使用后端默认头像，用户可在个人中心上传
    await register({
      account: form.account,
      password: form.password,
      nickname: form.nickname,
      phone: form.phone || undefined
    })

    try {
      const user = await authStore.login(form.account, form.password)
      ElMessage.success(`欢迎加入，${user?.nickname || form.nickname}！`)
      router.replace('/travel/home')
    } catch {
      ElMessage.success('注册成功，请登录')
      router.replace('/travel/login')
    }
  } catch {
    // 注册失败（如账号已存在）：拦截器已提示，这里不重复
  } finally {
    loading.value = false
  }
}
</script>
