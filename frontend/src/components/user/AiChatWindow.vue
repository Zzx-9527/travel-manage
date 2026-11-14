<template>
  <teleport to="body">
    <transition name="chat-pop">
      <section
        v-if="open"
        ref="winRef"
        class="chat-win"
        :class="{ 'is-mobile': isMobile, 'is-dragging': dragging }"
        :style="winStyle"
      >
        <header class="chat-head" @mousedown="startDrag" @touchstart.prevent="startDrag">
          <div class="chat-brand">
            <span class="chat-avatar"><i class="fa fa-magic"></i></span>
            <div class="chat-brand-text">
              <strong>AI 助手</strong>
              <em>行程规划 · 出行问答</em>
            </div>
          </div>
          <div class="chat-tools">
            <button title="重置位置" aria-label="重置位置" @click.stop="resetPos">
              <i class="fa fa-compress"></i>
            </button>
            <button title="清空对话" aria-label="清空对话" @click.stop="clearAll">
              <i class="fa fa-trash-o"></i>
            </button>
            <button title="关闭" aria-label="关闭" @click.stop="open = false">
              <i class="fa fa-times"></i>
            </button>
          </div>
        </header>

        <div ref="bodyRef" class="chat-body">
          <div v-if="!messages.length && !loadingHistory" class="chat-hello">
            <h2>嗨，{{ nickname || '旅行者' }}</h2>
            <h2>请问有什么可以帮您？</h2>
            <button v-for="q in suggestions" :key="q" class="suggest" @click="sendText(q)">{{ q }}</button>
          </div>

          <div v-if="loadingHistory" class="chat-loading">正在载入上次对话…</div>

          <template v-for="msg in messages" :key="msg.key">
            <div v-if="msg.type === 'text'" class="row" :class="msg.role">
              <div class="bubble" :class="{ 'bubble-error': msg.error }">{{ msg.content }}</div>
            </div>

            <AiPlanFormCard
              v-else-if="msg.type === 'form'"
              :initial="msg.request"
              @submit="onFormSubmit(msg, $event)"
              @cancel="removeMsg(msg.key)"
            />

            <AiPlanResultCard
              v-else-if="msg.type === 'plan'"
              :plan="msg.plan"
              :busy="sending"
              @save="savePlanMsg(msg)"
              @to-map="toMap(msg)"
              @replan="replan(msg)"
              @spot="goSpot"
            />
          </template>
        </div>

        <footer class="chat-foot">
          <button class="plan-entry" @click="openPlanForm">
            <i class="fa fa-magic"></i>帮我规划行程
          </button>
          <div class="chat-input">
            <textarea
              v-model="draft"
              rows="1"
              maxlength="500"
              placeholder="请告诉我您遇到的问题"
              @keydown.enter.exact.prevent="send"
            ></textarea>
            <button class="send" :disabled="!draft.trim() || sending" aria-label="发送" @click="send">
              <i class="fa fa-arrow-up"></i>
            </button>
          </div>
        </footer>
      </section>
    </transition>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  appendChatPlan,
  chatStream,
  clearChatHistory,
  fetchChatHistory,
  planTripStream,
  savePlan
} from '@/api/ai'
import AiPlanFormCard from './AiPlanFormCard.vue'
import AiPlanResultCard from './AiPlanResultCard.vue'

const open = defineModel('open', { type: Boolean, default: false })

const router = useRouter()
const authStore = useAuthStore()

const nickname = computed(() => authStore.nickname || '')
const suggestions = ['北京三天怎么玩', '带老人去杭州要注意什么', '帮我规划成都两日游']

const messages = ref([])
const draft = ref('')
const sending = ref(false)
const loadingHistory = ref(false)
const loaded = ref(false)

const bodyRef = ref(null)
const winRef = ref(null)
const dragging = ref(false)
const pos = ref({ x: null, y: null })
const isMobile = ref(false)

let keySeq = 0
let dragOffset = { x: 0, y: 0 }

const winStyle = computed(() => {
  if (isMobile.value || pos.value.x == null) {
    return {}
  }
  return {
    left: `${pos.value.x}px`,
    top: `${pos.value.y}px`,
    right: 'auto',
    bottom: 'auto'
  }
})

watch(open, (value) => {
  if (value) {
    loadHistory()
    scrollToBottom()
  }
})

onMounted(() => {
  syncMobile()
  restorePos()
  window.addEventListener('resize', syncMobile)
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('touchend', endDrag)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncMobile)
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)
})

// ==================== 会话 ====================

async function loadHistory() {
  if (loaded.value) {
    return
  }
  loadingHistory.value = true
  try {
    const res = await fetchChatHistory(30)
    messages.value = (res.content || []).map(toMessage).filter(Boolean)
    loaded.value = true
  } catch {
    loaded.value = true
  } finally {
    loadingHistory.value = false
    scrollToBottom()
  }
}

/** plan 类型的记录恢复成卡片；解析失败的直接跳过，不阻塞其它消息 */
function toMessage(item) {
  if (item.type === 'plan') {
    if (!item.payload) {
      return null
    }
    try {
      return { key: ++keySeq, role: 'assistant', type: 'plan', plan: JSON.parse(item.payload) }
    } catch {
      return null
    }
  }
  return { key: ++keySeq, role: item.role, type: 'text', content: item.content }
}

function send() {
  const text = draft.value.trim()
  if (!text || sending.value) {
    return
  }
  draft.value = ''
  sendText(text)
}

async function sendText(text) {
  messages.value.push({ key: ++keySeq, role: 'user', type: 'text', content: text })
  messages.value.push({ key: ++keySeq, role: 'assistant', type: 'text', content: '' })
  const reply = messages.value[messages.value.length - 1]
  sending.value = true
  scrollToBottom()

  try {
    await chatStream(text, {
      onDelta: (chunk) => {
        reply.content += chunk
        scrollToBottom()
      },
      onError: (msg) => {
        reply.content = msg
        reply.error = true
      }
    })
  } catch (e) {
    if (e.name !== 'AbortError') {
      reply.content = e.message || '连接失败，请重试'
      reply.error = true
    }
  } finally {
    sending.value = false
    // 空回复（比如中途断流）不留在界面上
    if (!reply.content) {
      removeMsg(reply.key)
    }
    scrollToBottom()
  }
}

function openPlanForm() {
  messages.value.push({ key: ++keySeq, role: 'assistant', type: 'form' })
  scrollToBottom()
}

function replan(msg) {
  messages.value.push({ key: ++keySeq, role: 'assistant', type: 'form', request: msg.request || null })
  scrollToBottom()
}

/**
 * 表单提交：把"正在生成"的气泡换成行程卡片。
 * 期间不展示模型吐出的原始 JSON——对聊天窗口来说那是噪音。
 */
async function onFormSubmit(formMsg, payload) {
  removeMsg(formMsg.key)
  messages.value.push({ key: ++keySeq, role: 'assistant', type: 'text', content: '正在为你规划行程，约需 3 秒…' })
  const progress = messages.value[messages.value.length - 1]
  sending.value = true
  scrollToBottom()

  let plan = null
  let errorMsg = ''
  try {
    await planTripStream(payload, {
      onPlan: (result) => {
        plan = result
      },
      onError: (msg) => {
        errorMsg = msg
      }
    })
  } catch (e) {
    if (e.name !== 'AbortError') {
      errorMsg = e.message || '生成失败，请重试'
    }
  } finally {
    sending.value = false
    removeMsg(progress.key)
  }

  if (plan) {
    messages.value.push({ key: ++keySeq, role: 'assistant', type: 'plan', plan, request: payload })
    // 落库失败不影响展示，卡片已经在页面上了
    appendChatPlan(plan.title, plan).catch(() => {})
  } else {
    messages.value.push({
      key: ++keySeq,
      role: 'assistant',
      type: 'text',
      content: errorMsg || '没拿到行程，请重试',
      error: true
    })
  }
  scrollToBottom()
}

async function savePlanMsg(msg) {
  try {
    await savePlan({
      destination: msg.request?.destination || '',
      budget: msg.request?.budget,
      title: msg.plan.title,
      intro: msg.plan.intro,
      days: msg.plan.days,
      tips: msg.plan.tips
    })
    ElMessage.success('已保存到「我的发布」，等待审核')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

/** 与"最近生成"里的逻辑一致：按天顺序去重后带进规划页 */
function toMap(msg) {
  const spots = []
  const seen = new Set()
  for (const day of msg.plan?.days || []) {
    for (const spot of day.spots || []) {
      if (!spot.scenicId || seen.has(spot.scenicId)) continue
      seen.add(spot.scenicId)
      spots.push({
        id: spot.scenicId,
        name: spot.name,
        address: spot.address,
        longitude: spot.longitude,
        latitude: spot.latitude,
        price: spot.price,
        score: spot.score,
        cover: spot.cover,
        openTime: spot.openTime
      })
    }
  }
  if (!spots.length) {
    ElMessage.warning('这份行程里没有可用的景点')
    return
  }
  sessionStorage.setItem('travel:ai:planner', JSON.stringify({ spots }))
  open.value = false
  router.push({ path: '/travel/home/planner', query: { from: 'ai' } })
}

function goSpot(id) {
  open.value = false
  router.push(`/travel/home/spots/${id}`)
}

async function clearAll() {
  try {
    await ElMessageBox.confirm('清空后这段对话就找不回来了，确定吗？', '清空对话', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await clearChatHistory()
    messages.value = []
    ElMessage.success('已清空')
  } catch (e) {
    ElMessage.error(e.message || '清空失败')
  }
}

function removeMsg(key) {
  messages.value = messages.value.filter((msg) => msg.key !== key)
}

function scrollToBottom() {
  nextTick(() => {
    const el = bodyRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

// ==================== 拖动 ====================

function syncMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    pos.value = { x: null, y: null }
  }
}

function restorePos() {
  try {
    const raw = localStorage.getItem('travel:ai:chat:pos')
    if (raw) {
      const saved = JSON.parse(raw)
      if (typeof saved.x === 'number' && typeof saved.y === 'number') {
        pos.value = clampPos(saved.x, saved.y)
      }
    }
  } catch {
    // 位置信息损坏就回默认位置
  }
}

function startDrag(event) {
  if (isMobile.value || !winRef.value) {
    return
  }
  const rect = winRef.value.getBoundingClientRect()
  const point = event.touches ? event.touches[0] : event
  dragOffset = { x: point.clientX - rect.left, y: point.clientY - rect.top }
  dragging.value = true
}

function onDrag(event) {
  if (!dragging.value || !winRef.value) {
    return
  }
  const point = event.touches ? event.touches[0] : event
  const next = clampPos(point.clientX - dragOffset.x, point.clientY - dragOffset.y)
  pos.value = next
  if (event.cancelable) {
    event.preventDefault()
  }
}

function endDrag() {
  if (!dragging.value) {
    return
  }
  dragging.value = false
  if (!isMobile.value && pos.value.x != null) {
    localStorage.setItem('travel:ai:chat:pos', JSON.stringify(pos.value))
  }
}

function clampPos(x, y) {
  const width = winRef.value?.offsetWidth || 400
  const height = winRef.value?.offsetHeight || 620
  const maxX = Math.max(8, window.innerWidth - width - 8)
  const maxY = Math.max(8, window.innerHeight - height - 8)
  return {
    x: Math.min(Math.max(8, x), maxX),
    y: Math.min(Math.max(8, y), maxY)
  }
}

function resetPos() {
  pos.value = { x: null, y: null }
  localStorage.removeItem('travel:ai:chat:pos')
}
</script>

<style scoped>
.chat-win {
  position: fixed;
  right: 26px;
  bottom: 96px;
  z-index: 2100;
  width: 400px;
  height: 620px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18), 0 4px 12px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.chat-win.is-mobile {
  inset: 0;
  width: 100%;
  height: 100%;
  max-height: none;
  border-radius: 0;
}

.chat-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  cursor: grab;
  user-select: none;
  background: #fff;
}

.chat-win.is-dragging .chat-head {
  cursor: grabbing;
}

.chat-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  background: linear-gradient(135deg, #1cb89d, #0f9481);
}

.chat-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.chat-brand-text strong {
  font-size: 14px;
  color: #0f172a;
}

.chat-brand-text em {
  font-size: 11px;
  font-style: normal;
  color: #94a3b8;
}

.chat-tools {
  display: flex;
  gap: 4px;
}

.chat-tools button {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 13px;
}

.chat-tools button:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f6fbfd 60%, #eef7fb 100%);
}

.chat-hello {
  padding: 6px 2px;
}

.chat-hello h2 {
  margin: 0 0 4px;
  font-size: 19px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.chat-hello h2 + h2 {
  margin-bottom: 16px;
}

.suggest {
  display: block;
  width: 100%;
  text-align: left;
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
}

.suggest:hover {
  border-color: #b9e8de;
  box-shadow: 0 4px 12px rgba(28, 184, 157, 0.1);
}

.chat-loading {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 12px 0;
}

.row {
  display: flex;
  margin-bottom: 10px;
}

.row.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 84%;
  padding: 9px 12px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.row.assistant .bubble {
  background: #fff;
  color: #1f2937;
  border: 1px solid #eef2f7;
  border-top-left-radius: 4px;
}

.row.user .bubble {
  background: #1cb89d;
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble-error {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.chat-foot {
  flex-shrink: 0;
  padding: 10px 12px 12px;
  border-top: 1px solid #f1f5f9;
  background: #fff;
}

.plan-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 6px 12px;
  border: 1px solid #d7f0ea;
  border-radius: 999px;
  background: #f0fbf8;
  color: #0f9481;
  font-size: 12px;
  cursor: pointer;
}

.plan-entry:hover {
  background: #e2f7f2;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 8px 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
}

.chat-input textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 13px;
  line-height: 1.6;
  max-height: 96px;
  min-height: 22px;
  padding: 4px 0;
  font-family: inherit;
  color: #1f2937;
  background: transparent;
}

.send {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #1cb89d;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.send:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.chat-pop-enter-active,
.chat-pop-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.chat-pop-enter-from,
.chat-pop-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}
</style>
