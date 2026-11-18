<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title"><i class="fa fa-pencil-square-o mr-2" style="color: #1CB89D;"></i>发布内容</h2>
      <p class="page-desc">分享你的旅行攻略，或规划一条新路线，提交后等待管理员审核</p>
    </div>

    <div class="publish-card">
      <el-tabs v-model="activeTab">
        <!-- 发攻略 -->
        <el-tab-pane label="发布攻略" name="guide">
          <el-form label-width="70px" class="pub-form">
            <el-form-item label="标题" required>
              <el-input v-model="guideForm.title" maxlength="60" placeholder="一句话吸引人的标题" />
            </el-form-item>
            <el-form-item label="封面">
              <div class="flex gap-2 w-full">
                <el-input v-model="guideForm.cover" placeholder="封面图片 URL（可空或点右侧上传）" />
                <el-upload :show-file-list="false" :before-upload="beforeCoverUpload" :http-request="doCoverUpload" accept=".jpg,.jpeg,.png,.gif,.webp">
                  <el-button :loading="uploadingCover">上传</el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item label="正文" required>
              <el-input
                v-model="guideForm.content"
                type="textarea" :rows="10" maxlength="5000" show-word-limit
                placeholder="分享你的旅行经验、路线安排、注意事项…"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitGuide" style="border-radius: 10px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
                <i class="fa fa-paper-plane mr-1"></i>提交攻略
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 发路线 -->
        <el-tab-pane label="发布路线" name="route">
          <el-form label-width="70px" class="pub-form">
            <el-form-item label="路线名" required>
              <el-input v-model="routeForm.name" maxlength="40" placeholder="例如：杭州经典两日游" />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="天数" required>
                <el-input-number v-model="routeForm.dayCount" :min="1" :max="30" />
              </el-form-item>
              <el-form-item label="出发城市">
                <el-input v-model="routeForm.startCity" maxlength="20" placeholder="杭州" />
              </el-form-item>
            </div>
            <div class="form-row">
              <el-form-item label="交通">
                <el-select v-model="routeForm.traffic" placeholder="选择交通方式" style="width: 160px;">
                  <el-option v-for="t in TRAFFIC_TYPES" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
              <el-form-item label="预算">
                <el-input-number v-model="routeForm.budget" :min="0" :max="100000" :step="100" />
              </el-form-item>
            </div>
            <el-form-item label="介绍">
              <el-input
                v-model="routeForm.intro"
                type="textarea" :rows="5" maxlength="500" show-word-limit
                placeholder="路线的亮点、节奏安排…"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitRoute" style="border-radius: 10px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
                <i class="fa fa-paper-plane mr-1"></i>提交路线
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { addStrategy } from '@/api/strategy'
import { addRoute } from '@/api/route'
import { uploadImage, imageFileError, resolveUploadUrl } from '@/api/file'
import { TRAFFIC_TYPES } from '@/utils/format'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('guide')
const submitting = ref(false)

const guideForm = ref({ title: '', cover: '', content: '' })
const routeForm = ref({ name: '', dayCount: 2, startCity: '', traffic: '高铁', budget: 1000, intro: '' })

// 封面上传（§10）：上传拿 url 填入输入框，随发布表单一起提交
const uploadingCover = ref(false)
function beforeCoverUpload(file) {
  const err = imageFileError(file)
  if (err) { ElMessage.warning(err); return false }
  return true
}
async function doCoverUpload({ file }) {
  uploadingCover.value = true
  try {
    const res = await uploadImage(file)
    if (res.code === 1) {
      guideForm.value.cover = resolveUploadUrl(res.content?.url)
      ElMessage.success('封面上传成功')
    }
  } catch (e) { /* 拦截器已提示 */ }
  finally { uploadingCover.value = false }
}

function currentUid() {
  const uid = authStore.userInfo?.id
  if (!uid) {
    ElMessage.warning('请先登录')
    router.push('/travel/login')
    return null
  }
  return uid
}

async function submitGuide() {
  const uid = currentUid()
  if (!uid) return
  if (!guideForm.value.title.trim() || !guideForm.value.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  submitting.value = true
  try {
    const res = await addStrategy({
      title: guideForm.value.title.trim(),
      cover: guideForm.value.cover?.trim() || undefined,
      content: guideForm.value.content.trim(),
      auditStatus: 0
    })
    if (res.code === 1) {
      ElMessage.success('攻略发布成功，等待审核')
      guideForm.value = { title: '', cover: '', content: '' }
    }
  } catch (e) { /* 拦截器已提示 */ }
  finally { submitting.value = false }
}

async function submitRoute() {
  const uid = currentUid()
  if (!uid) return
  if (!routeForm.value.name.trim()) {
    ElMessage.warning('请填写路线名')
    return
  }
  submitting.value = true
  try {
    const res = await addRoute({
      name: routeForm.value.name.trim(),
      dayCount: routeForm.value.dayCount || 1,
      startCity: routeForm.value.startCity?.trim() || undefined,
      traffic: routeForm.value.traffic,
      budget: routeForm.value.budget ?? 0,
      intro: routeForm.value.intro?.trim() || undefined,
      status: 0
    })
    if (res.code === 1) {
      const newRouteId = res.content?.id
      routeForm.value = { name: '', dayCount: 2, startCity: '', traffic: '高铁', budget: 1000, intro: '' }
      // 引导用户立即为这条路线配置行程（途经景点）
      try {
        await ElMessageBox.confirm(
          '路线已发布成功，正在等待管理员审核。现在为它配置每天的行程安排吗？',
          '发布成功',
          {
            confirmButtonText: '去配置行程',
            cancelButtonText: '稍后再说',
            type: 'success'
          }
        )
        router.push({
          path: '/travel/home/mine/publish/itinerary',
          query: { routeId: newRouteId }
        })
      } catch (e) { /* 用户选择稍后配置 */ }
    }
  } catch (e) { /* 拦截器已提示 */ }
  finally { submitting.value = false }
}
</script>

<style scoped>
.page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 56px;
  min-height: 60vh;
}

.page-head {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 14px;
  color: #94A3B8;
  margin: 0;
}

.publish-card {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.pub-form {
  margin-top: 8px;
}

.form-row {
  display: flex;
  gap: 24px;
}

.form-row .el-form-item {
  flex: 1;
}

@media (max-width: 600px) {
  .publish-card {
    padding: 18px 16px;
  }
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  .form-row .el-form-item {
    width: 100%;
  }
}
</style>
