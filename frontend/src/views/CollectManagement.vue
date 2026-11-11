<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">收藏管理</h2>
        <p class="text-sm mt-0.5" style="color: #94A3B8;">管理所有用户的攻略收藏记录</p>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="bg-white rounded-xl p-5 mb-5" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input
            v-model="searchKeyword"
            placeholder="输入收藏者昵称或内容关键词..."
            clearable
            :prefix-icon="null"
            style="width: 100%;"
            @keyup.enter="handleSearch"
          />
        </div>
        <el-select v-model="filterType" placeholder="收藏类型" clearable style="width: 150px;" @change="handleSearch">
          <el-option label="全部类型" :value="null" />
          <el-option label="攻略" :value="1" />
          <el-option label="景点" :value="2" />
          <el-option label="路线" :value="3" />
        </el-select>
        <el-button @click="handleSearch" style="border-radius: 8px;">
          <i class="fa fa-search mr-1.5"></i>查询
        </el-button>
        <el-button @click="resetSearch" style="border-radius: 8px;">
          <i class="fa fa-refresh mr-1.5"></i>重置
        </el-button>
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
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="收藏者" width="140" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-avatar :size="24" :src="row.userId?.avatar" style="flex-shrink: 0;">
                <i class="fa fa-user text-xs"></i>
              </el-avatar>
              <span class="text-sm truncate max-w-[80px]">{{ row.userId?.nickname || '匿名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <span class="type-badge" :style="collectTypeStyle(collectTarget(row).type)">{{ collectTypeText(collectTarget(row).type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收藏ID" width="100" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-600">{{ collectTarget(row).id ?? '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收藏内容" min-width="200" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-700 font-medium line-clamp-2" :title="collectName(row)">{{ collectName(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收藏时间" width="170" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
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
          @size-change="handleSizeChange"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="收藏详情" :width="560" top="10vh" destroy-on-close>
      <div v-if="detailData.id">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-5">
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">收藏 ID</p>
            <p class="text-gray-800 font-medium">{{ detailData.id }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">收藏时间</p>
            <p class="text-gray-600 text-sm">{{ formatTime(detailData.createTime) }}</p>
          </div>
        </div>

        <div class="rounded-xl p-4 mb-4" style="background: #F8FAFC;">
          <p class="text-xs text-gray-400 uppercase tracking-wide mb-2">收藏内容</p>
          <div class="flex items-start gap-3">
            <el-image
              v-if="collectTarget(detailData).cover"
              :src="collectTarget(detailData).cover"
              fit="cover"
              lazy
              style="width: 64px; height: 48px; border-radius: 6px; flex-shrink: 0;"
            >
              <template #error>
                <div class="w-16 h-12 bg-gray-200 rounded-lg flex items-center justify-center">
                  <i class="fa fa-image text-gray-400"></i>
                </div>
              </template>
            </el-image>
            <div v-else class="w-16 h-12 bg-gray-100 rounded-lg flex items-center justify-center">
              <i class="fa fa-road text-gray-400"></i>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-semibold text-gray-800 text-sm">{{ collectName(detailData) }}</p>
              <p class="text-xs text-gray-400 mt-1">
                类型：{{ collectTypeText(collectTarget(detailData).type) }}
              </p>
            </div>
          </div>
        </div>

        <div class="rounded-xl p-4" style="background: #F8FAFC;">
          <p class="text-xs text-gray-400 uppercase tracking-wide mb-2">收藏用户</p>
          <div class="flex items-center gap-3">
            <el-avatar :size="36" :src="detailData.userId?.avatar">
              <i class="fa fa-user"></i>
            </el-avatar>
            <div>
              <p class="font-semibold text-gray-800 text-sm">{{ detailData.userId?.nickname || '匿名用户' }}</p>
              <p class="text-xs text-gray-400">用户ID: {{ detailData.userId?.id || '--' }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { collectTarget, collectTypeText, collectTypeStyle, formatTime } from '@/utils/format'
import { getCollectsByPage, deleteCollect } from '@/api/collect'
import { useAdminTable } from '@/composables/useAdminTable'

const searchKeyword = ref('')
const filterType = ref(null)

// 服务端分页 + 服务端关键词/类型过滤
const { list, loading, currentPage, pageSize, totalElements, totalPages, fetchData } = useAdminTable(getCollectsByPage, {
  size: 10,
  extraParams: () => {
    const params = { orderCol: 'id', orderDirect: 'ASC' }
    const kw = searchKeyword.value.trim()
    if (kw) params.keyword = kw
    if (filterType.value) params.targetType = filterType.value
    return params
  }
})

// ---- 工具 ----
function collectName(row) {
  const t = collectTarget(row)
  if (t.name) return t.name
  return t.type === 1 ? `攻略 #${t.id ?? '--'}` : '未知内容'
}

// ---- 查询 ----
function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function resetSearch() {
  searchKeyword.value = ''
  filterType.value = null
  currentPage.value = 1
  fetchData()
}

function handleSizeChange() {
  currentPage.value = 1
  fetchData()
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除收藏「${collectName(row)}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined }
    )
    const res = await deleteCollect(row.id)
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

onMounted(fetchData)
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
:deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px #E2E8F0 inset; }
:deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #1CB89D inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #1CB89D inset; }
:deep(.relative .el-input__wrapper) { padding-left: 36px; }
:deep(.el-dialog__header) { margin-right: 0; padding: 20px 24px 16px; border-bottom: 1px solid #F1F5F9; }
:deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1E293B; }
:deep(.el-dialog__body) { padding: 24px; }
:deep(.el-dialog__footer) { padding: 16px 24px 20px; border-top: 1px solid #F1F5F9; }
:deep(.el-table__body tr:hover > td) { background-color: #F8FAFC !important; }
:deep(.el-table__cell) { padding: 6px 0; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) { background: #1CB89D; color: #fff; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active:hover) { background: #0D9488; }
:deep(.el-table__body tr) { cursor: pointer; }
.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid transparent;
}
</style>
