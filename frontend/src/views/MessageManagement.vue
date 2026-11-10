<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">公告管理</h2>
        <p class="text-sm mt-0.5" style="color: #94A3B8;">向用户发布系统公告，公告会出现在用户的消息中心</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-5 gap-5 mb-5">
      <!-- 发布公告 -->
      <div class="lg:col-span-2 bg-white rounded-xl p-6" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
        <h3 class="text-base font-semibold mb-4" style="color: #1E293B;">
          <i class="fa fa-bullhorn mr-2" style="color: #1CB89D;"></i>发布公告
        </h3>
        <el-form label-position="top">
          <el-form-item v-if="scope === 'single'" label="消息类型">
            <el-select v-model="form.type" style="width: 100%;">
              <el-option label="系统公告" :value="0" />
              <el-option label="订单通知" :value="1" />
              <el-option label="审核通知" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="接收范围">
            <el-radio-group v-model="scope" @change="scope === 'single' && loadUsers()">
              <el-radio value="banner">全站横幅（所有用户可见）</el-radio>
              <el-radio value="single">指定用户（写入其消息中心）</el-radio>
            </el-radio-group>
          </el-form-item>
          <div v-if="scope === 'banner'" class="banner-tip">
            <i class="fa fa-info-circle"></i>
            全站横幅不写入任何用户的消息中心，而是在用户端页面顶部以横幅形式展示（用户可关闭），发布新横幅会自动替换旧的。
          </div>
          <el-form-item v-if="scope === 'single'" label="选择用户">
            <el-select v-model="form.userId" filterable placeholder="搜索昵称或账号..." style="width: 100%;">
              <el-option
                v-for="u in userOptions"
                :key="u.id"
                :label="`${u.nickname || u.account}（${u.account}）`"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="公告标题" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="公告内容（将展示在用户的消息中心）"
            />
          </el-form-item>
          <el-button
            type="primary"
            class="w-full"
            :loading="publishing"
            style="background: var(--brand-gradient); border: none; border-radius: 10px;"
            @click="handlePublish"
          >
            <i class="fa fa-send-o mr-2"></i>{{ publishBtnText }}
          </el-button>
        </el-form>
      </div>

      <!-- 发送历史 -->
      <div class="lg:col-span-3 bg-white rounded-xl overflow-hidden" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
        <div class="px-6 pt-2 flex items-end justify-between gap-4 flex-wrap">
          <el-tabs v-model="historyTab" class="history-tabs">
            <el-tab-pane name="banner">
              <template #label><i class="fa fa-bullhorn mr-1"></i>系统公告（{{ bannerCount }}）</template>
            </el-tab-pane>
            <el-tab-pane name="user">
              <template #label><i class="fa fa-users mr-1"></i>用户消息（{{ userMsgCount }}）</template>
            </el-tab-pane>
          </el-tabs>
          <div class="relative" style="width: 230px; margin-bottom: 6px;">
            <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
            <el-input
              v-model="historyKeyword"
              placeholder="输入用户昵称或标题..."
              clearable
              :prefix-icon="null"
              style="width: 100%;"
            />
          </div>
        </div>
        <div class="px-6 pb-3 text-xs" style="color: #94A3B8;">
          {{ historyTab === 'banner'
            ? '全站横幅公告：在用户端页面顶部展示（最新一条为当前生效横幅），不进入任何用户的消息中心'
            : '发送给指定用户的消息，以及系统内产生的订单/审核/系统通知' }}
        </div>
        <el-table
          :data="pagedList"
          v-loading="historyLoading"
          stripe
          style="width: 100%"
          :empty-text="historyTab === 'banner' ? '暂无横幅公告' : '暂无用户消息'"
          :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: 600 }"
        >
          <el-table-column v-if="historyTab === 'user'" prop="id" label="ID" width="60" align="center" />
          <el-table-column v-if="historyTab === 'user'" label="接收人" width="120" align="center">
            <template #default="{ row }">
              <div class="flex items-center justify-center gap-2">
                <el-avatar :size="22" :src="row.userId?.avatar" style="flex-shrink: 0;">
                  <i class="fa fa-user text-xs"></i>
                </el-avatar>
                <span class="text-sm truncate max-w-[70px]">{{ row.userId?.nickname || '未知' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="historyTab === 'user'" label="类型" width="80" align="center">
            <template #default="{ row }">
              <span class="type-badge" :style="typeStyle(row.type)">{{ typeText(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="标题 / 内容" min-width="200">
            <template #default="{ row }">
              <p class="text-sm font-medium" style="color: #1E293B; margin: 0;">{{ row.title }}</p>
              <p class="text-xs" style="color: #94A3B8; margin: 2px 0 0;">{{ truncate(row.content, 40) }}</p>
            </template>
          </el-table-column>
          <el-table-column label="发送时间" width="160" align="center">
            <template #default="{ row }"><span class="text-sm" style="color: #64748B;">{{ formatTime(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-tooltip content="删除" placement="top" :show-after="300">
                <button @click="handleDelete(row)" class="action-btn delete-btn" title="删除">
                  <i class="fa fa-trash-o"></i>
                </button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex items-center justify-between px-6 py-4 border-t border-gray-100">
          <span class="text-sm" style="color: #94A3B8;">第 {{ currentPage }}/{{ totalPages }} 页</span>
          <el-pagination
            v-model:current-page="currentPage" v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]" :total="totalElements" :pager-count="5"
            layout="sizes, prev, pager, next" background
            @current-change="loadHistory"
            @size-change="handleHistorySizeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addMessage, getMessagesByPage, deleteMessage, BANNER_MESSAGE_TYPE } from '@/api/message'
import { getUsersByPage } from '@/api/user'

// ---- 发布表单 ----
const form = ref({ type: 0, userId: null, title: '', content: '' })
const scope = ref('banner')
const publishing = ref(false)

// 用户选项（指定用户时才加载，分页拉全量）
const userOptions = ref([])
async function loadUsers() {
  if (userOptions.value.length) return
  try {
    const all = []
    let page = 1
    for (;;) {
      const res = await getUsersByPage({ page, size: 100 })
      if (res.code !== 1) break
      const rows = res.content?.content || []
      all.push(...rows)
      if (page >= (res.content?.totalPages ?? 1) || !rows.length) break
      page++
    }
    userOptions.value = all
  } catch (e) { console.error(e) }
}

const publishBtnText = computed(() =>
  publishing.value
    ? (scope.value === 'banner' ? '发布中...' : '发布中...')
    : (scope.value === 'banner' ? '发布全站横幅' : '发布给该用户')
)

async function handlePublish() {
  if (!form.value.title.trim()) { ElMessage.warning('请输入公告标题'); return }
  if (!form.value.content.trim()) { ElMessage.warning('请输入公告内容'); return }
  if (scope.value === 'single' && !form.value.userId) { ElMessage.warning('请选择接收用户'); return }

  const ok = await ElMessageBox.confirm(
    scope.value === 'banner'
      ? `确定发布全站横幅「${form.value.title}」吗？发布后立即在用户端顶部展示，并自动替换旧横幅。`
      : `确定向该用户发送公告「${form.value.title}」吗？`,
    '发送确认',
    { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info', icon: undefined }
  ).catch(() => false)
  if (!ok) return

  publishing.value = true
  try {
    if (scope.value === 'banner') {
      // 全站横幅：使用独立的公告类型，后端自动补当前管理员为接收人并置为已读
      const res = await addMessage({
        type: BANNER_MESSAGE_TYPE,
        title: form.value.title.trim(),
        content: form.value.content.trim()
      })
      if (res.code !== 1) { ElMessage.error(res.msg || '发布失败'); return }
      ElMessage.success('横幅已发布，用户端顶部将展示最新公告')
    } else {
      const res = await addMessage({
        userId: { id: form.value.userId },
        type: form.value.type,
        title: form.value.title.trim(),
        content: form.value.content.trim()
      })
      if (res.code !== 1) { ElMessage.error(res.msg || '发送失败'); return }
      ElMessage.success('公告已发送')
    }
    form.value.title = ''
    form.value.content = ''
    loadHistory()
  } finally { publishing.value = false }
}

// ---- 发送历史（服务端分页；系统公告与用户消息分开查询） ----
const bannerList = ref([])      // 系统公告：type=3 的公告消息，按 id 倒序
const userMsgList = ref([])     // 用户消息：排除 type=3 公告，按 id 升序
const historyLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const historyTab = ref('banner')
const historyKeyword = ref('')
const bannerTotal = ref(0)
const bannerTotalPages = ref(0)
const userMsgTotal = ref(0)
const userMsgTotalPages = ref(0)

async function loadHistory() {
  historyLoading.value = true
  try {
    if (historyTab.value === 'banner') {
      const res = await getMessagesByPage({
        page: currentPage.value,
        size: pageSize.value,
        type: BANNER_MESSAGE_TYPE,
        orderCol: 'id',
        orderDirect: 'DESC',
        keyword: historyKeyword.value.trim() || undefined
      })
      if (res.code === 1) {
        bannerList.value = res.content?.content || []
        bannerTotal.value = res.content?.totalElements ?? bannerList.value.length
        bannerTotalPages.value = res.content?.totalPages ?? Math.ceil(bannerTotal.value / pageSize.value)
      }
    } else {
      const res = await getMessagesByPage({
        page: currentPage.value,
        size: pageSize.value,
        typeNe: BANNER_MESSAGE_TYPE,
        keyword: historyKeyword.value.trim() || undefined
      })
      if (res.code === 1) {
        userMsgList.value = res.content?.content || []
        userMsgTotal.value = res.content?.totalElements ?? userMsgList.value.length
        userMsgTotalPages.value = res.content?.totalPages ?? Math.ceil(userMsgTotal.value / pageSize.value)
      }
    }
  } catch (e) { console.error(e) } finally { historyLoading.value = false }
}

const historyList = computed(() => {
  return historyTab.value === 'banner' ? bannerList.value : userMsgList.value
})
const bannerCount = computed(() => bannerTotal.value)
const userMsgCount = computed(() => userMsgTotal.value)

const totalElements = computed(() => historyTab.value === 'banner' ? bannerTotal.value : userMsgTotal.value)
const totalPages = computed(() => historyTab.value === 'banner' ? bannerTotalPages.value : userMsgTotalPages.value)
const pagedList = computed(() => historyList.value)
watch(historyTab, () => { currentPage.value = 1; loadHistory() })
watch(historyKeyword, () => { currentPage.value = 1; loadHistory() })
watch(totalPages, tp => { if (currentPage.value > tp) currentPage.value = tp })

function handleHistorySizeChange() {
  currentPage.value = 1
  loadHistory()
}

// ---- 工具 ----
function formatTime(dt) {
  return dt ? String(dt).replace(/\.0$/, '').replace('T', ' ') : '--'
}
function truncate(text, max) {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}
function typeText(t) {
  if (t === 3) return '公告'
  return t === 1 ? '订单' : t === 2 ? '审核' : '系统'
}
function typeStyle(t) {
  if (t === 1) return { background: '#EFF6FF', color: '#2563EB', border: '1px solid #BFDBFE' }
  if (t === 2) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  return { background: '#FFFBEB', color: '#D97706', border: '1px solid #FDE68A' }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除发给「${row.userId?.nickname || '未知'}」的消息「${row.title}」吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined }
    )
    const res = await deleteMessage(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      loadHistory()
    }
  } catch (err) { if (err !== 'cancel') console.error(err) }
}

onMounted(() => {
  loadHistory()
  loadUsers()
})
</script>

<style scoped>
.action-btn {
  width: 32px; height: 32px; border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid transparent; cursor: pointer;
  transition: all 0.2s ease; font-size: 14px;
}
.delete-btn {
  color: #DC2626; background: #FEF2F2; border-color: #FECACA;
}
.delete-btn:hover {
  background: #FEE2E2; border-color: #FCA5A5;
  transform: translateY(-1px); box-shadow: 0 4px 12px rgba(220, 38, 38, 0.2);
}
:deep(.el-table__cell) { padding: 6px 0; }
:deep(.el-select .el-input__wrapper) { border-radius: 8px; }
:deep(.el-input__wrapper) { border-radius: 8px; }
/* 搜索框：给左侧图标留出空间 */
:deep(.relative .el-input__wrapper) { padding-left: 36px; }
.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid transparent;
}

.banner-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--brand-50, #F0FDFA);
  border: 1px solid #99F6E4;
  color: #0F766E;
  font-size: 12px;
  line-height: 1.6;
  margin-bottom: 16px;
}

.banner-tip i { margin-top: 2px; }
</style>
