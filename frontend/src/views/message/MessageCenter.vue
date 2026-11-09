<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title"><i class="fa fa-bell-o mr-2" style="color: #1CB89D;"></i>消息中心</h2>
      <p class="page-desc">系统通知、订单与审核消息都汇聚在这里</p>
    </div>

    <!-- 筛选 -->
    <div class="filter-card">
      <el-radio-group v-model="typeFilter" @change="handleFilter">
        <el-radio-button :value="-1">全部类型</el-radio-button>
        <el-radio-button :value="0">系统</el-radio-button>
        <el-radio-button :value="1">订单</el-radio-button>
        <el-radio-button :value="2">审核</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="readFilter" @change="handleFilter" style="margin-left: 12px;">
        <el-radio-button :value="-1">全部</el-radio-button>
        <el-radio-button :value="0">未读</el-radio-button>
        <el-radio-button :value="1">已读</el-radio-button>
      </el-radio-group>
      <el-button class="read-all-btn" size="small" :disabled="!hasUnread" @click="markAllRead">
        <i class="fa fa-check-double mr-1"></i>全部已读
      </el-button>
    </div>

    <!-- 列表 -->
    <div v-if="loading" class="flex justify-center py-16">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
    </div>

    <el-empty v-else-if="!list.length" description="暂无消息" :image-size="110" />

    <div v-else class="msg-list">
      <div v-for="m in list" :key="m.id" class="msg-item" :class="{ unread: m.isRead === 0 }" @click="openMessage(m)">
        <div class="msg-icon" :style="typeStyle(m)"><i :class="typeIcon(m)"></i></div>
        <div class="msg-body">
          <div class="msg-head">
            <span class="msg-title">{{ m.title || '未命名消息' }}</span>
            <span class="msg-type" :style="typeStyle(m)">{{ typeText(m) }}</span>
            <span class="msg-time">{{ formatTime(m.createTime) }}</span>
          </div>
          <p class="msg-content">{{ m.content }}</p>
        </div>
        <button class="msg-del" title="删除" @click.stop="removeMessage(m)">
          <i class="fa fa-trash-o"></i>
        </button>
      </div>

      <div v-if="list.length" class="pager">
        <span class="pager-text">共 <b>{{ total }}</b> 条记录，第 {{ page }}/{{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[8, 16, 32]" :total="total" :pager-count="5"
          layout="sizes, prev, pager, next, jumper" background
          @size-change="setSize" @current-change="setPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { usePageList } from '@/composables/usePageList'
import { formatTime } from '@/utils/format'
import { getMessagesByPage, markMessageRead, deleteMessage } from '@/api/message'

const authStore = useAuthStore()

const { list, total, totalPages, page, size, loading, load, setPage, setSize } = usePageList(getMessagesByPage, { size: 8 })

const typeFilter = ref(-1)
const readFilter = ref(-1)

const hasUnread = computed(() => list.value.some(m => m.isRead === 0))

function fetchList() {
  const uid = authStore.userInfo?.id
  if (!uid) return
  // 统一用 userId 传接收人
  const params = { userId: uid }
  if (typeFilter.value !== -1) params.type = typeFilter.value
  if (readFilter.value !== -1) params.isRead = readFilter.value
  load(params)
}

function handleFilter() {
  page.value = 1
  fetchList()
}

// 审核类消息：内容含"未通过/驳回"即为审核驳回，徽章显示红色
function isRejectedAudit(m) {
  return m.type === 2 && /未通过|驳回/.test((m.title || '') + (m.content || ''))
}

function typeText(m) {
  if (isRejectedAudit(m)) return '审核驳回'
  return m.type === 1 ? '订单' : m.type === 2 ? '审核' : '系统'
}

function typeIcon(m) {
  if (isRejectedAudit(m)) return 'fa fa-times-circle'
  return m.type === 1 ? 'fa fa-ticket' : m.type === 2 ? 'fa fa-check-circle' : 'fa fa-bullhorn'
}

function typeStyle(m) {
  if (isRejectedAudit(m)) return { background: '#FEF2F2', color: '#DC2626', border: '1px solid #FECACA' }
  if (m.type === 1) return { background: '#EFF6FF', color: '#2563EB', border: '1px solid #BFDBFE' }
  if (m.type === 2) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  return { background: '#FFFBEB', color: '#D97706', border: '1px solid #FDE68A' }
}

async function openMessage(m) {
  if (m.isRead === 0) {
    try {
      await markMessageRead({ id: m.id })
      m.isRead = 1
      // 通知顶栏刷新未读徽标
      window.dispatchEvent(new Event('unread-refresh'))
    } catch (e) { /* 忽略 */ }
  }
}

async function markAllRead() {
  const uid = authStore.userInfo?.id
  if (!uid) return
  try {
    // 逐页取齐该用户全部未读消息 id 再批量标记，避免只处理当前页
    const ids = []
    let p = 1
    while (p <= 50) {
      const res = await getMessagesByPage({ page: p, size: 200, userId: uid, isRead: 0 })
      if (res.code !== 1) break
      const rows = res.content?.content || res.content || []
      ids.push(...rows.map(m => m.id))
      if (rows.length < 200) break
      p++
    }
    if (!ids.length) {
      ElMessage.info('没有未读消息')
      return
    }
    const res = await markMessageRead({ ids })
    if (res.code === 1) {
      ElMessage.success('已全部标记为已读')
      fetchList()
      // 通知顶栏刷新未读徽标
      window.dispatchEvent(new Event('unread-refresh'))
    }
  } catch (e) { /* 拦截器已提示 */ }
}

async function removeMessage(m) {
  try {
    const res = await deleteMessage(m.id)
    if (res.code === 1) {
      ElMessage.success('消息已删除')
      fetchList()
    }
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(fetchList)
</script>

<style scoped>
.page {
  max-width: 900px;
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

.filter-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 14px 18px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 20px;
}

.read-all-btn {
  margin-left: auto;
  border-radius: 8px;
}

.msg-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.msg-item:hover {
  border-color: rgba(28, 184, 157, 0.4);
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
}

.msg-item.unread {
  background: #F0FDF9;
  border-color: #99F6E4;
}

.msg-icon {
  width: 40px; height: 40px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  border: 1px solid transparent;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
}

.msg-item.unread .msg-title { color: #0D9488; }

.msg-type {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid transparent;
}

.msg-time {
  font-size: 12px;
  color: #94A3B8;
  margin-left: auto;
}

.msg-content {
  font-size: 13px;
  color: #64748B;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-del {
  flex-shrink: 0;
  width: 32px; height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94A3B8;
  cursor: pointer;
  transition: all 0.2s;
}

.msg-del:hover { background: #FEE2E2; color: #DC2626; }

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.pager-text {
  font-size: 13px;
  color: #94A3B8;
}

.pager-text b { color: #475569; }
</style>
