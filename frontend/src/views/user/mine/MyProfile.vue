<template>
  <div>
    <h2 class="page-title">个人资料</h2>

    <!-- 资料表单 -->
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="账号">
        <el-input :model-value="account" disabled style="max-width: 360px;" />
      </el-form-item>
      <el-form-item label="头像">
        <div class="avatar-field">
          <el-avatar :size="72" :src="form.avatar" style="flex-shrink: 0;">
            <i class="fa fa-user text-2xl"></i>
          </el-avatar>
          <el-input v-model="form.avatar" placeholder="头像图片 URL（选填）" clearable style="max-width: 360px;" />
          <el-upload :show-file-list="false" :before-upload="beforeAvatarUpload" :http-request="doAvatarUpload" accept=".jpg,.jpeg,.png,.gif,.webp">
            <el-button :loading="uploadingAvatar">上传</el-button>
          </el-upload>
        </div>
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" style="max-width: 360px;" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" style="max-width: 360px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave" style="border-radius: 10px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
          <i class="fa fa-save mr-1"></i>保存资料
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 修改密码 -->
    <div class="pwd-card">
      <h3 class="pwd-title"><i class="fa fa-lock mr-2"></i>修改密码</h3>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" style="max-width: 360px;" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 3 位" style="max-width: 360px;" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" style="max-width: 360px;" />
        </el-form-item>
        <el-form-item>
          <el-button :loading="pwdSaving" @click="handleChangePassword" style="border-radius: 10px;">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getMyProfile, updateProfile, updatePassword } from '@/api/userCenter'
import { uploadImage, imageFileError, resolveUploadUrl } from '@/api/file'

const authStore = useAuthStore()

const formRef = ref(null)
const saving = ref(false)

const account = computed(() => authStore.userInfo?.account || '--')

const form = reactive({
  nickname: authStore.userInfo?.nickname || '',
  phone: authStore.userInfo?.phone || '',
  avatar: authStore.userInfo?.avatar || ''
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const pwdFormRef = ref(null)
const pwdSaving = ref(false)

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (rule, value, callback) => {
  if (!value) callback(new Error('请再次输入新密码'))
  else if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 3, message: '密码长度不少于 3 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

async function loadProfile() {
  // 后端未实现时静默降级，仍使用本地登录信息
  try {
    const res = await getMyProfile()
    if (res.code === 1 && res.content) {
      form.nickname = res.content.nickname || form.nickname
      form.phone = res.content.phone || ''
      form.avatar = res.content.avatar || ''
      authStore.saveAuth(authStore.token, { ...authStore.userInfo, ...res.content })
    }
  } catch (err) { /* silent */ }
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    // 只提交白名单字段（§2.1）：updateProfile 身份取 token，password/account 等字段一律不传
    const res = await updateProfile({
      nickname: form.nickname,
      phone: form.phone || '',
      avatar: form.avatar || ''
    })
    if (res?.code === 1) {
      authStore.saveAuth(authStore.token, { ...authStore.userInfo, nickname: form.nickname, phone: form.phone, avatar: form.avatar })
      ElMessage.success('资料已保存')
    } else {
      // 兜底提示：无论后端返回什么形态（含格式异常），保存操作必须给用户反馈
      ElMessage.error(res?.msg || `保存失败（code=${res?.code ?? '无响应码'}）`)
    }
  } catch (err) {
    // 拦截器已弹过错的场景会再弹一次具体原因，这里兜底保证必有反馈
    ElMessage.error(err?.response?.status ? `保存失败（HTTP ${err.response.status}）` : (err?.message || '保存失败，请检查网络'))
  } finally {
    saving.value = false
  }
}

// 头像上传（§10）：上传拿 url 填入输入框，保存资料时随 edit 提交；
// 入库成功后后端自动删除旧头像文件（单图覆盖），外部手填 URL 不在此管理内
const uploadingAvatar = ref(false)
function beforeAvatarUpload(file) {
  const err = imageFileError(file)
  if (err) { ElMessage.warning(err); return false }
  return true
}
async function doAvatarUpload({ file }) {
  uploadingAvatar.value = true
  try {
    const res = await uploadImage(file)
    if (res.code === 1) {
      form.avatar = resolveUploadUrl(res.content?.url)
      ElMessage.success('头像已上传，保存资料后生效')
    }
  } catch (e) { /* 拦截器已提示 */ }
  finally { uploadingAvatar.value = false }
}

async function handleChangePassword() {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdSaving.value = true
  try {
    // §3：updatePassword 自带原密码校验；改密成功不踢已有会话，保持当前登录即可
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = pwdForm.newPassword = pwdForm.confirmPassword = ''
  } catch (err) {
    // 失败提示由拦截器统一弹出（原密码不正确 / 网络错误），这里不再重复弹
  } finally {
    pwdSaving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 24px;
}

.avatar-field {
  display: flex;
  align-items: center;
  gap: 16px;
}

@media (max-width: 600px) {
  .avatar-field {
    flex-wrap: wrap;
    align-items: flex-start;
  }
}

.pwd-card {
  margin-top: 32px;
  border-top: 1px solid #F1F5F9;
  padding-top: 24px;
}

.pwd-title {
  font-size: 16px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 20px;
}
</style>
