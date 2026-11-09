<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">评论管理</h2>
        <p class="text-sm mt-0.5" style="color: #94A3B8;">管理用户对景点、攻略等内容的评论</p>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="bg-white rounded-xl p-5 mb-5" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input v-model="searchKeyword" placeholder="搜索攻略标题..." clearable :prefix-icon="null" style="width: 100%;" @keyup.enter="handleSearch" />
        </div>
        <el-select v-model="filterStatus" placeholder="审核状态" clearable style="width: 150px;" @change="handleSearch">
          <el-option label="全部状态" :value="null" />
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button @click="handleSearch" style="border-radius: 8px;"><i class="fa fa-search mr-1.5"></i>搜索</el-button>
        <el-button @click="resetSearch" style="border-radius: 8px;"><i class="fa fa-refresh mr-1.5"></i>重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="bg-white rounded-xl overflow-hidden" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <el-table
        :data="list"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: 600 }"
        @row-click="openDetailDialog"
      >
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="评论用户" width="130" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-avatar :size="26" :src="row.userId?.avatar" style="flex-shrink: 0;">
                <i class="fa fa-user text-xs"></i>
              </el-avatar>
              <span class="text-sm truncate max-w-[80px]">{{ row.userId?.nickname || '匿名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="攻略标题" min-width="160" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-700 font-medium" :title="row.strategyId?.title">{{ row.strategyId?.title || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="220" align="center">
          <template #default="{ row }">
            <div class="max-w-xs mx-auto">
              <p class="text-sm text-gray-700 truncate leading-relaxed" :title="row.content">{{ row.content || '--' }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
              :style="auditStyle(row.status)"
            >
              <i :class="auditIcon(row.status)"></i>
              {{ auditText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="165" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
              <el-tooltip content="审核" placement="top" :show-after="300">
                <button @click="openAuditDialog(row)" class="action-btn audit-btn" title="审核">
                  <i class="fa fa-check-circle-o"></i>
                </button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top" :show-after="300">
                <button @click="handleDelete(row)" class="action-btn delete-btn" title="删除">
                  <i class="fa fa-trash-o"></i>
                </button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="flex items-center justify-between px-6 py-4 border-t border-gray-100">
        <span class="text-sm" style="color: #94A3B8;">
          共 <span style="color: #475569;">{{ totalElements }}</span> 条记录，第 {{ currentPage }}/{{ totalPages }} 页
        </span>
        <el-pagination
          v-model:current-page="currentPage" v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]" :total="totalElements" :pager-count="5"
          layout="sizes, prev, pager, next, jumper" background
          @size-change="handleSizeChange" @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 审核状态对话框 -->
    <el-dialog v-model="auditVisible" title="评论审核" :width="460" :close-on-click-modal="false" top="25vh">
      <div class="flex gap-4 mb-5 p-4 rounded-xl" style="background: #F8FAFC;">
        <div class="w-12 h-12 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-lg font-bold flex-shrink-0">
          <i class="fa fa-commenting"></i>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-xs text-gray-400 mb-1">评论用户</p>
          <p class="font-semibold text-gray-800 text-sm">{{ auditRow?.userId?.nickname || '匿名' }}</p>
          <p class="text-xs text-gray-400 mt-2 truncate" :title="auditRow?.content">内容：{{ auditRow?.content }}</p>
        </div>
      </div>
      <el-form label-width="0">
        <el-form-item>
          <el-select v-model="auditStatusValue" style="width: 100%;" size="large">
            <el-option :value="0"><span><i class="fa fa-clock-o text-amber-500 mr-2"></i>待审核</span></el-option>
            <el-option :value="1"><span><i class="fa fa-check-circle text-emerald-500 mr-2"></i>通过</span></el-option>
            <el-option :value="2"><span><i class="fa fa-times-circle text-red-500 mr-2"></i>驳回</span></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="auditVisible = false" style="border-radius: 8px;">取 消</el-button>
          <el-button type="primary" @click="handleAudit" :loading="auditSubmitting"
            style="border-radius: 8px; background: linear-gradient(135deg, #3B82F6, #2563EB); border: none;">
            <i class="fa fa-check mr-1"></i>保存修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="评论详情" :width="640" top="5vh" destroy-on-close>
      <div v-if="detailData.id">
        <div class="flex items-center gap-4 mb-5 p-4 rounded-xl" style="background: #F8FAFC;">
          <el-avatar :size="48" :src="detailData.userId?.avatar">
            <i class="fa fa-user"></i>
          </el-avatar>
          <div>
            <p class="font-semibold text-gray-800">{{ detailData.userId?.nickname || '匿名用户' }}</p>
            <p class="text-xs text-gray-400">用户ID: {{ detailData.userId?.id || '--' }}</p>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-5">
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">评论 ID</p>
            <p class="text-gray-800 font-medium">{{ detailData.id }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">审核状态</p>
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium mt-1"
              :style="auditStyle(detailData.status)">
              <i :class="auditIcon(detailData.status)"></i>{{ auditText(detailData.status) }}
            </span>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">评分</p>
            <div class="inline-flex items-center gap-1">
              <span v-for="i in 5" :key="i">
                <i :class="i <= (detailData.rating || 0) ? 'fa fa-star' : 'fa fa-star-o'" style="color: #F59E0B; font-size: 16px;"></i>
              </span>
              <span class="ml-1 text-sm text-gray-600">({{ detailData.rating || 0 }}分)</span>
            </div>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">评论对象</p>
            <p class="text-gray-800 font-medium">{{ detailData.scenicId?.name || detailData.targetType || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">创建时间</p>
            <p class="text-gray-600 text-sm">{{ formatTime(detailData.createTime) }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">修改时间</p>
            <p class="text-gray-600 text-sm">{{ formatTime(detailData.updateTime) }}</p>
          </div>
        </div>

        <div>
          <p class="text-xs text-gray-400 uppercase tracking-wide mb-2">评论内容</p>
          <div class="p-4 rounded-xl text-sm leading-relaxed whitespace-pre-wrap" style="background: #F8FAFC; color: #374151; max-height: 200px; overflow-y: auto;">
            {{ detailData.content || '暂无内容' }}
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCommentsByPage, editComment, deleteComment } from '@/api/comment'
import { useAdminTable } from '@/composables/useAdminTable'

const searchKeyword = ref('')
const filterStatus = ref(null)

const { list, loading, currentPage, pageSize, totalElements, totalPages, fetchData } = useAdminTable(getCommentsByPage, {
  extraParams: () => {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    return params
  }
})

// ---- 工具 ----
function formatTime(dt) {
  return dt ? String(dt).replace(/\.0$/, '').replace('T', ' ') : '--'
}
function auditText(s) {
  return s === 0 ? '待审核' : s === 1 ? '通过' : s === 2 ? '驳回' : '未知'
}
function auditIcon(s) {
  return s === 0 ? 'fa fa-clock-o' : s === 1 ? 'fa fa-check-circle' : s === 2 ? 'fa fa-times-circle' : 'fa fa-question-circle'
}
function auditStyle(s) {
  if (s === 0) return { background: '#FFFBEB', color: '#D97706', border: '1px solid #FDE68A' }
  if (s === 1) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  if (s === 2) return { background: '#FEF2F2', color: '#DC2626', border: '1px solid #FECACA' }
  return { background: '#F1F5F9', color: '#64748B', border: '1px solid #E2E8F0' }
}

// ---- 查询 ----
function handleSearch() { currentPage.value = 1; fetchData() }
function resetSearch() { searchKeyword.value = ''; filterStatus.value = null; currentPage.value = 1; fetchData() }
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; fetchData() }
function handlePageChange(p) { currentPage.value = p; fetchData() }

// ---- 审核 ----
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const auditRow = ref(null)
const auditStatusValue = ref(0)

function openAuditDialog(row) {
  auditRow.value = row
  auditStatusValue.value = row.status
  auditVisible.value = true
}

async function handleAudit() {
  if (!auditRow.value) return
  auditSubmitting.value = true
  try {
    const r = auditRow.value
    const payload = {
      id: r.id,
      userId: r.userId,
      scenicId: r.scenicId || null,
      content: r.content,
      rating: r.rating,
      status: auditStatusValue.value
    }
    const res = await editComment(payload)
    if (res.code === 1) {
      ElMessage.success('审核状态已更新')
      auditVisible.value = false
      fetchData()
    }
  } catch (err) { console.error(err)
  } finally { auditSubmitting.value = false }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除该评论吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined }
    )
    const res = await deleteComment(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (err) { if (err !== 'cancel') console.error(err) }
}

// ---- 详情 ----
const detailVisible = ref(false)
const detailData = ref({})
function openDetailDialog(row) {
  detailData.value = row
  detailVisible.value = true
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.action-btn {
  width: 32px; height: 32px; border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid transparent; cursor: pointer;
  transition: all 0.2s ease; font-size: 14px;
}
.audit-btn {
  color: #0D9488; background: #F0FDFA; border-color: #CCFBF1;
}
.audit-btn:hover {
  background: #CCFBF1; border-color: #5EEAD4;
  transform: translateY(-1px); box-shadow: 0 4px 12px rgba(13, 148, 136, 0.2);
}
.delete-btn {
  color: #DC2626; background: #FEF2F2; border-color: #FECACA;
}
.delete-btn:hover {
  background: #FEE2E2; border-color: #FCA5A5;
  transform: translateY(-1px); box-shadow: 0 4px 12px rgba(220, 38, 38, 0.2);
}
:deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px #E2E8F0 inset; }
:deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #1CB89D inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #1CB89D inset; }
:deep(.relative .el-input__wrapper) { padding-left: 36px; }
:deep(.el-select .el-input__wrapper) { border-radius: 8px; }
:deep(.el-dialog__header) { margin-right: 0; padding: 20px 24px 16px; border-bottom: 1px solid #F1F5F9; }
:deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1E293B; }
:deep(.el-dialog__body) { padding: 24px; }
:deep(.el-dialog__footer) { padding: 16px 24px 20px; border-top: 1px solid #F1F5F9; }
:deep(.el-table__body tr:hover > td) { background-color: #F8FAFC !important; }
:deep(.el-table__cell) { padding: 6px 0; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) { background: #1CB89D; color: #fff; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active:hover) { background: #0D9488; }
:deep(.el-table__body tr) { cursor: pointer; }
</style>
