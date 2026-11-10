<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">景点订单管理</h2>
        <p class="text-sm mt-0.5" style="color: #94A3B8;">管理所有景区门票订单</p>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="bg-white rounded-xl p-5 mb-5" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input v-model="searchKeyword" placeholder="输入用户昵称或景点名称..." clearable :prefix-icon="null" style="width: 100%;" @keyup.enter="handleSearch" />
        </div>
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 160px;" @change="handleSearch">
          <el-option label="全部状态" value="" />
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已完成" :value="3" />
        </el-select>
        <el-button @click="handleSearch" style="border-radius: 8px;"><i class="fa fa-search mr-1.5"></i>搜索</el-button>
        <el-button @click="resetSearch" style="border-radius: 8px;"><i class="fa fa-refresh mr-1.5"></i>重置</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-5">
      <div class="bg-white rounded-xl p-5 flex items-center gap-4" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
        <div class="w-11 h-11 bg-blue-50 rounded-xl flex items-center justify-center flex-shrink-0">
          <i class="fa fa-ticket text-blue-500 text-xl"></i>
        </div>
        <div>
          <p class="text-xs text-gray-500 uppercase tracking-wider">总订单</p>
          <p class="text-2xl font-bold text-gray-800">{{ totalElements }}</p>
        </div>
      </div>
      <div class="bg-white rounded-xl p-5 flex items-center gap-4" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
        <div class="w-11 h-11 bg-green-50 rounded-xl flex items-center justify-center flex-shrink-0">
          <i class="fa fa-check-circle text-green-500 text-xl"></i>
        </div>
        <div>
          <p class="text-xs text-gray-500 uppercase tracking-wider">已支付</p>
          <p class="text-2xl font-bold text-gray-800">{{ paidCount }}</p>
        </div>
      </div>
      <div class="bg-white rounded-xl p-5 flex items-center gap-4" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
        <div class="w-11 h-11 bg-orange-50 rounded-xl flex items-center justify-center flex-shrink-0">
          <i class="fa fa-clock-o text-orange-500 text-xl"></i>
        </div>
        <div>
          <p class="text-xs text-gray-500 uppercase tracking-wider">待支付</p>
          <p class="text-2xl font-bold text-gray-800">{{ unpaidCount }}</p>
        </div>
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
        <el-table-column prop="id" label="ID" width="55" align="center" />
        <el-table-column label="订单编号" width="230" align="center">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-700 bg-blue-50 px-2 py-1 rounded-md font-medium">{{ row.orderNo || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="100" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-avatar :size="24" :src="row.userId?.avatar" style="flex-shrink: 0;">
                <i class="fa fa-user text-xs"></i>
              </el-avatar>
              <span class="text-sm truncate max-w-[80px]">{{ row.userId?.nickname || '匿名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="景点名称" min-width="120" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-700">{{ row.scenicId?.name || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系人" width="120" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-600">{{ row.contactName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系电话" width="120" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-500 font-mono">{{ row.contactPhone || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="支付时间" width="170" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ formatTime(row.payTime) }}</span></template>
        </el-table-column>
        <el-table-column label="订单状态" width="120" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
              :style="orderStatusStyle(row)"
            >
              <i :class="orderStatusIcon(row)"></i>
              {{ orderStatusText(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
              <el-tooltip v-if="Number(row.status) === 0" content="取消订单" placement="top" :show-after="300">
                <button @click="handleCancel(row)" class="action-btn edit-btn" title="取消订单">
                  <i class="fa fa-ban"></i>
                </button>
              </el-tooltip>
              <el-tooltip v-else content="删除" placement="top" :show-after="300">
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
    <el-dialog v-model="detailVisible" title="订单详情" :width="640" top="5vh" destroy-on-close>
      <div v-if="detailData.id">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-5">
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">订单 ID</p>
            <p class="text-gray-800 font-medium">{{ detailData.id }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">订单编号</p>
            <p class="text-gray-800 font-mono font-medium text-sm bg-gray-100 px-2 py-1 rounded inline-block">{{ detailData.orderNo }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">用户账号</p>
            <p class="text-gray-700 text-sm">{{ detailData.userId?.account || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">用户昵称</p>
            <p class="text-gray-700 text-sm">{{ detailData.userId?.nickname || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">用户手机</p>
            <p class="text-gray-700 text-sm font-mono">{{ detailData.userId?.phone || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">景点名称</p>
            <p class="text-gray-800 font-medium text-sm">{{ detailData.scenicId?.name || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">景点地址</p>
            <p class="text-gray-700 text-sm">{{ detailData.scenicId?.address || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">联系人</p>
            <p class="text-gray-700 text-sm">{{ detailData.contactName || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">联系电话</p>
            <p class="text-gray-700 text-sm font-mono">{{ detailData.contactPhone || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">游玩日期</p>
            <p class="text-gray-700 text-sm">{{ detailData.playDate || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">票数</p>
            <p class="text-gray-800 font-semibold text-sm">{{ detailData.ticketCount ?? 0 }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">总金额</p>
            <p class="text-gray-800 font-bold text-sm">¥{{ (detailData.totalAmount ?? 0).toFixed(2) }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">支付方式</p>
            <p class="text-gray-700 text-sm">{{ payTypeText(detailData.payType) }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">订单状态</p>
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium mt-1"
              :style="orderStatusStyle(detailData)">
              <i :class="orderStatusIcon(detailData)"></i>{{ orderStatusText(detailData) }}
            </span>
          </div>
          <div v-if="detailData.cancelTime" class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">取消时间</p>
            <p class="text-gray-600 text-sm">{{ formatTime(detailData.cancelTime) }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">创建时间</p>
            <p class="text-gray-600 text-sm">{{ formatTime(detailData.createTime) }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">支付时间</p>
            <p class="text-gray-600 text-sm">{{ formatTime(detailData.payTime) }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrdersByPage, getOrderStatusStats, cancelOrder, deleteOrder } from '@/api/order'
import { orderStatusText, orderStatusIcon, orderStatusStyle } from '@/utils/format'
import { useAdminTable } from '@/composables/useAdminTable'

const searchKeyword = ref('')
const filterStatus = ref('')
const paidCount = ref(0)
const unpaidCount = ref(0)

// 服务端分页 + 服务端关键词/状态过滤
const { list, loading, currentPage, pageSize, totalElements, totalPages, fetchData } = useAdminTable(getOrdersByPage, {
  size: 10,
  extraParams: () => {
    const params = {}
    const kw = searchKeyword.value.trim()
    if (kw) params.keyword = kw
    if (filterStatus.value !== '') params.status = Number(filterStatus.value)
    return params
  }
})

// ---- 工具 ----
function formatTime(dt) {
  return dt ? String(dt).replace(/\.0$/, '').replace('T', ' ') : '--'
}

function payTypeText(v) {
  const map = { 1: '微信', 2: '支付宝', 3: '现金' }
  return map[v] || '未知'
}

// 状态统计跟随关键词搜索，不跟随状态筛选，保证两张卡片始终有意义
async function loadStatusStats() {
  try {
    const res = await getOrderStatusStats({ keyword: searchKeyword.value.trim() || undefined })
    if (res.code === 1) {
      paidCount.value = Number(res.content?.paidCount || 0)
      unpaidCount.value = Number(res.content?.unpaidCount || 0)
    }
  } catch (e) { /* 统计失败保留上一次值 */ }
}

function refresh() {
  fetchData()
  loadStatusStats()
}

// ---- 查询 ----
function handleSearch() {
  currentPage.value = 1
  refresh()
}

function resetSearch() {
  searchKeyword.value = ''
  filterStatus.value = ''
  currentPage.value = 1
  refresh()
}

function handleSizeChange() {
  currentPage.value = 1
  fetchData()
}

// ---- 取消（待支付）----
async function handleCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单「${row.orderNo}」吗？取消后余票会回补，订单记录保留。`,
      '取消确认',
      { confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning', icon: undefined }
    )
    const res = await cancelOrder(row.id)
    if (res.code === 1) {
      ElMessage.success('订单已取消，余票已回补')
      refresh()
    }
  } catch (err) { if (err !== 'cancel') console.error(err) }
}

// ---- 删除（待支付不可删）----
async function handleDelete(row) {
  if (Number(row.status) === 0) {
    ElMessage.warning('待支付订单请先取消，不能直接删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除订单「${row.orderNo}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined }
    )
    const res = await deleteOrder(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      refresh()
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

onMounted(refresh)
</script>

<style scoped>
.action-btn {
  width: 32px; height: 32px; border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid transparent; cursor: pointer;
  transition: all 0.2s ease; font-size: 14px;
}
.edit-btn {
  color: #3B82F6; background: #EFF6FF; border-color: #BFDBFE;
}
.edit-btn:hover {
  background: #DBEAFE; border-color: #93C5FD;
  transform: translateY(-1px); box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}
.delete-btn {
  color: #DC2626; background: #FEF2F2; border-color: #FECACA;
}
.delete-btn:hover {
  background: #FEE2E2; border-color: #FCA5A5;
  transform: translateY(-1px); box-shadow: 0 4px 12px rgba(220, 38, 38, 0.2);
}
:deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px #E2E8F0 inset; }
:deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #3B82F6 inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #3B82F6 inset; }
:deep(.relative .el-input__wrapper) { padding-left: 36px; }
:deep(.el-select .el-input__wrapper) { border-radius: 8px; }
:deep(.el-dialog__header) { margin-right: 0; padding: 20px 24px 16px; border-bottom: 1px solid #F1F5F9; }
:deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1E293B; }
:deep(.el-dialog__body) { padding: 24px; }
:deep(.el-dialog__footer) { padding: 16px 24px 20px; border-top: 1px solid #F1F5F9; }
:deep(.el-table__body tr:hover > td) { background-color: #F8FAFC !important; }
:deep(.el-table__cell) { padding: 6px 0; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) { background: #3B82F6; color: #fff; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active:hover) { background: #2563EB; }
:deep(.el-table__body tr) { cursor: pointer; }
</style>
