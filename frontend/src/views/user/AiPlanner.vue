<template>
  <div class="ai-page">
    <header class="ai-hero">
      <div>
        <h1>AI 规划师</h1>
        <p>
          说清目的地、天数、预算和偏好，帮你排一份按天行程。
          只会从平台在售景点里选，价格、坐标、评分都取自真实数据。
        </p>
      </div>
      <div class="quota" :class="{ 'quota-empty': remaining <= 0 }">
        今日剩余 <b>{{ remaining }}</b> / {{ dailyLimit }} 次
      </div>
    </header>

    <el-alert
      v-if="!aiEnabled"
      class="ai-warn"
      type="warning"
      :closable="false"
      show-icon
      title="AI 当前不可用"
      :description="disabledReason"
    />

    <section class="ai-card">
      <div class="form-grid">
        <label class="field">
          <span class="field-label">目的地 <b>*</b></span>
          <el-input v-model="form.destination" placeholder="如：北京、西安" maxlength="30" clearable />
        </label>
        <label class="field">
          <span class="field-label">天数</span>
          <el-select v-model="form.days">
            <el-option v-for="d in 7" :key="d" :label="`${d} 天`" :value="d" />
          </el-select>
        </label>
        <label class="field">
          <span class="field-label">预算（元）</span>
          <el-input-number v-model="form.budget" :min="0" :step="500" controls-position="right" />
        </label>
        <label class="field">
          <span class="field-label">偏好</span>
          <el-select v-model="form.preference" clearable placeholder="可不选">
            <el-option v-for="p in preferences" :key="p" :label="p" :value="p" />
          </el-select>
        </label>
      </div>

      <label class="field field-full">
        <span class="field-label">补充说明</span>
        <el-input
          v-model="form.extra"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          placeholder="如：带老人不想太累、想多安排博物馆"
        />
      </label>

      <div class="form-actions">
        <el-button type="primary" size="large" :loading="loading" :disabled="!aiEnabled" @click="generate">
          {{ loading ? '正在规划…' : '生成行程' }}
        </el-button>
        <el-button v-if="loading" size="large" @click="cancel">停止</el-button>
      </div>
    </section>

    <el-alert v-if="error" class="ai-warn" type="error" :closable="false" show-icon :title="error" />

    <section v-if="loading || streamText" class="ai-stream">
      <div class="stream-head">
        <span class="dot" :class="{ pulsing: loading }"></span>
        <span>{{ loading ? 'AI 正在生成…' : '生成过程' }}</span>
        <span class="stream-count">{{ streamText.length }} 字符</span>
      </div>
      <pre ref="streamBox" class="stream-body">{{ streamText }}</pre>
    </section>

    <section v-if="plan" class="ai-result">
      <div class="result-head">
        <div>
          <h2>{{ plan.title }}</h2>
          <p>{{ plan.intro }}</p>
        </div>
        <el-button type="primary" plain :loading="saving" @click="save">保存为我的路线</el-button>
      </div>

      <div class="day-list">
        <article v-for="day in plan.days" :key="day.dayNo" class="day-card">
          <div class="day-head">
            <span class="day-no">D{{ day.dayNo }}</span>
            <span class="day-theme">{{ day.theme }}</span>
          </div>

          <div v-if="!day.spots || !day.spots.length" class="day-empty">自由活动 / 机动安排</div>
          <ul v-else class="spot-list">
            <li v-for="(spot, index) in day.spots" :key="spot.scenicId" class="spot-row">
              <div class="spot-cover">
                <img
                  v-if="spot.cover"
                  :src="spot.cover"
                  :alt="spot.name"
                  loading="lazy"
                  @error="hideBrokenImage"
                />
              </div>
              <span class="spot-index">{{ index + 1 }}</span>
              <div class="spot-main">
                <div class="spot-title">
                  <a class="spot-name" @click="goSpot(spot.scenicId)">{{ spot.name }}</a>
                  <span class="spot-meta">
                    <span v-if="spot.price != null">¥{{ spot.price }}</span>
                    <span v-if="spot.score">★ {{ spot.score }}</span>
                    <span>{{ spot.stayTime }}</span>
                  </span>
                </div>
                <p class="spot-reason">{{ spot.reason }}</p>
                <p class="spot-addr">{{ spot.address }}</p>
              </div>
            </li>
          </ul>
        </article>
      </div>

      <div v-if="plan.tips && plan.tips.length" class="tips">
        <h3>实用提示</h3>
        <ul>
          <li v-for="(tip, index) in plan.tips" :key="index">{{ tip }}</li>
        </ul>
      </div>

      <p class="meta-line">
        模型 {{ plan.model }} · 耗时 {{ plan.latencyMs }}ms
        <span v-if="plan.droppedSpots"> · 已过滤 {{ plan.droppedSpots }} 条无效景点</span>
      </p>
    </section>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAiHealth, planTripStream, savePlan } from '@/api/ai'

const router = useRouter()

const preferences = ['历史人文', '自然风光', '亲子', '美食', '拍照打卡', '轻松休闲']

const form = reactive({
  destination: '',
  days: 2,
  budget: 1500,
  preference: '',
  extra: ''
})

const loading = ref(false)
const saving = ref(false)
const streamText = ref('')
const streamBox = ref(null)
const plan = ref(null)
const error = ref('')
const remaining = ref(0)
const dailyLimit = ref(0)
const aiEnabled = ref(true)
const disabledReason = ref('')

let controller = null

onMounted(loadHealth)
onBeforeUnmount(() => controller?.abort())

async function loadHealth() {
  try {
    const res = await fetchAiHealth()
    const data = res.content || {}
    aiEnabled.value = Boolean(data.enabled)
    remaining.value = data.remainingToday ?? 0
    dailyLimit.value = data.dailyLimit ?? 0
    if (!data.enabled) disabledReason.value = data.message || 'AI 未启用'
  } catch {
    // 状态查询失败不阻塞页面，生成时再报具体错误
  }
}

function generate() {
  if (!form.destination.trim()) {
    ElMessage.warning('请先填写目的地')
    return
  }
  loading.value = true
  error.value = ''
  plan.value = null
  streamText.value = ''
  controller = new AbortController()

  planTripStream(
    { ...form },
    {
      signal: controller.signal,
      onDelta: (text) => {
        streamText.value += text
        nextTick(() => {
          if (streamBox.value) streamBox.value.scrollTop = streamBox.value.scrollHeight
        })
      },
      onPlan: (data) => {
        plan.value = data
      },
      onDone: () => {
        loading.value = false
        loadHealth()
      },
      onError: (msg) => {
        error.value = msg
        loading.value = false
      }
    }
  )
    .catch((e) => {
      if (e.name === 'AbortError') return
      error.value = e.message || '生成失败，请稍后重试'
    })
    .finally(() => {
      loading.value = false
    })
}

function cancel() {
  controller?.abort()
  loading.value = false
}

async function save() {
  if (!plan.value) return
  saving.value = true
  try {
    await savePlan({
      destination: form.destination,
      budget: form.budget,
      title: plan.value.title,
      intro: plan.value.intro,
      days: plan.value.days,
      tips: plan.value.tips
    })
    ElMessage.success('已保存到「我的发布」，等待审核')
    router.push('/travel/home/mine/publish')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function goSpot(id) {
  router.push(`/travel/home/spots/${id}`)
}

/** 封面加载失败时隐藏图片，避免出现破图图标；文字信息仍然完整 */
function hideBrokenImage(event) {
  event.target.style.display = 'none'
}
</script>

<style scoped>
.ai-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 20px 60px;
}

.ai-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.ai-hero h1 {
  font-size: 26px;
  font-weight: 700;
  color: var(--ink-900, #1f2937);
  margin: 0 0 8px;
}

.ai-hero p {
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-500, #6b7280);
  margin: 0;
  max-width: 640px;
}

.quota {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--ink-600, #4b5563);
  background: var(--ink-50, #f9fafb);
  border: 1px solid var(--ink-100, #f3f4f6);
  border-radius: 999px;
  padding: 8px 14px;
}

.quota b {
  color: var(--brand-600, #0f9481);
}

.quota-empty b {
  color: #e24b4a;
}

.ai-warn {
  margin-bottom: 16px;
}

.ai-card {
  background: #fff;
  border: 1px solid var(--ink-100, #f0f0f0);
  border-radius: 16px;
  padding: 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  color: var(--ink-500, #6b7280);
}

.field-label b {
  color: #e24b4a;
}

.field :deep(.el-select),
.field :deep(.el-input-number) {
  width: 100%;
}

.field-full {
  margin-top: 16px;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.ai-stream {
  background: #0f172a;
  border-radius: 14px;
  padding: 16px 18px;
  margin-bottom: 20px;
}

.stream-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #cbd5e1;
  margin-bottom: 10px;
}

.stream-count {
  margin-left: auto;
  color: #64748b;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22d3ee;
}

.dot.pulsing {
  animation: pulse 1.1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.35; }
  50% { opacity: 1; }
}

.stream-body {
  margin: 0;
  max-height: 220px;
  overflow-y: auto;
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
  white-space: pre-wrap;
  word-break: break-all;
}

.ai-result {
  background: #fff;
  border: 1px solid var(--ink-100, #f0f0f0);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px dashed var(--ink-100, #eee);
}

.result-head h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink-900, #1f2937);
  margin: 0 0 8px;
}

.result-head p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-500, #6b7280);
}

.day-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.day-card {
  border: 1px solid var(--ink-100, #f0f0f0);
  border-radius: 12px;
  padding: 14px 16px;
}

.day-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.day-no {
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: var(--brand-500, #1cb89d);
  border-radius: 6px;
  padding: 3px 8px;
}

.day-theme {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-800, #374151);
}

.day-empty {
  font-size: 13px;
  color: var(--ink-400, #9ca3af);
  padding: 6px 0 2px;
}

.spot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.spot-row {
  display: flex;
  gap: 12px;
}

.spot-cover {
  flex-shrink: 0;
  position: relative;
  width: 76px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  /* 外链图片加载失败或还在加载时露出的占位底：渐变 + 山水图标 */
  background: linear-gradient(135deg, #eef7f4, #e3f0ec);
  display: flex;
  align-items: center;
  justify-content: center;
}

.spot-cover::before {
  content: '🏞';
  font-size: 20px;
  opacity: 0.45;
}

.spot-cover img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spot-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--ink-50, #f3f4f6);
  color: var(--ink-500, #6b7280);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

.spot-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.spot-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900, #1f2937);
  cursor: pointer;
}

.spot-name:hover {
  color: var(--brand-600, #0f9481);
}

.spot-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--ink-400, #9ca3af);
}

.spot-reason {
  margin: 4px 0 2px;
  font-size: 12px;
  color: var(--ink-600, #4b5563);
}

.spot-addr {
  margin: 0;
  font-size: 12px;
  color: var(--ink-400, #9ca3af);
}

.tips {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed var(--ink-100, #eee);
}

.tips h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-800, #374151);
  margin: 0 0 8px;
}

.tips ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-600, #4b5563);
}

.meta-line {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--ink-400, #9ca3af);
}

@media (max-width: 860px) {
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .result-head {
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .ai-hero {
    flex-direction: column;
  }
}
</style>
