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
        <el-select v-model="filterPayStatus" placeholder="全部状态" clearable style="width: 160px;" @change="handleSearch">
          <el-option label="全部状态" value="" />
          <el-option label="未支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="免费" :value="2" />
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
          <p class="text-xs text-gray-500 uppercase tracking-wider">未支付</p>
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
        <el-table-column label="支付状态" width="100" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
              :style="payStatusStyle(row)"
            >
              <i :class="payStatusIcon(row)"></i>
              {{ payStatusText(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
              <el-tooltip content="修改状态" placement="top" :show-after="300">
                <button @click="openEditDialog(row)" class="action-btn edit-btn" title="修改状态">
                  <i class="fa fa-pencil-square-o"></i>
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
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 编辑对话框（修改支付状态） -->
    <el-dialog v-model="editVisible" title="修改支付状态" :width="480" :close-on-click-modal="false" top="15vh">
      <div v-if="editRow">
        <div class="flex gap-4 mb-5 p-4 rounded-xl" style="background: #F8FAFC;">
          <div class="w-12 h-12 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-lg font-bold flex-shrink-0">
            <i class="fa fa-ticket"></i>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-xs text-gray-400 mb-1">订单编号</p>
            <p class="font-mono font-semibold text-gray-800 text-sm">{{ editRow.orderNo }}</p>
            <p class="text-xs text-gray-400 mt-1.5">景点：{{ editRow.scenicId?.name || '--' }}</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4 mb-4">
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 mb-1">联系人</p>
            <p class="text-gray-800 font-medium text-sm">{{ editRow.contactName || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 mb-1">总金额</p>
            <p class="text-gray-800 font-semibold text-sm">¥{{ (editRow.totalAmount ?? 0).toFixed(2) }}</p>
          </div>
        </div>
        <el-form label-width="0">
          <el-form-item>
            <el-select v-model="editPayStatusValue" style="width: 100%;" size="large" :disabled="editRow.payStatus === 2">
              <el-option :value="1"><span><i class="fa fa-check-circle text-emerald-500 mr-2"></i>已支付</span></el-option>
              <el-option :value="0"><span><i class="fa fa-clock-o text-amber-500 mr-2"></i>未支付</span></el-option>
            </el-select>
          </el-form-item>
        </el-form>
        <div v-if="editRow.payStatus === 2" class="flex items-start gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg">
          <i class="fa fa-info-circle text-amber-500 mt-0.5 flex-shrink-0"></i>
          <p class="text-xs text-amber-700">免费订单不可修改支付状态。</p>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="editVisible = false" style="border-radius: 8px;">取 消</el-button>
          <el-button type="primary" @click="handleEdit" :loading="editSubmitting"
            style="border-radius: 8px; background: linear-gradient(135deg, #3B82F6, #2563EB); border: none;">
            <i class="fa fa-check mr-1"></i>确认修改
          </el-button>
        </div>
      </template>
    </el-dialog>

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
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">支付状态</p>
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium mt-1"
              :style="payStatusStyle(detailData)">
              <i :class="payStatusIcon(detailData)"></i>{{ payStatusText(detailData) }}
            </span>
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
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrdersByPage, editOrder, deleteOrder } from '@/api/order'

const searchKeyword = ref('')
const filterPayStatus = ref('')

// 后端不支持按用户昵称/景点名称模糊搜索，这里拉全量后前端过滤 + 本地分页
const allRows = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

async function loadAll() {
  loading.value = true
  try {
    const all = []
    let page = 1
    for (;;) {
      const res = await getOrdersByPage({ page, size: 100 })
      if (res.code !== 1) break
      const rows = res.content?.content || []
      all.push(...rows)
      const tp = res.content?.totalPages ?? 1
      if (page >= tp || !rows.length) break
      page++
    }
    allRows.value = all
  } catch (e) { console.error(e) } finally { loading.value = false }
}

// ---- 工具 ----
function formatTime(dt) {
  return dt ? String(dt).replace(/\.0$/, '').replace('T', ' ') : '--'
}

function payStatusText(row) {
  if (row.payStatus === 2) return '免费'
  return row.payStatus === 1 ? '已支付' : '未支付'
}
function payStatusIcon(row) {
  if (row.payStatus === 2) return 'fa fa-gift'
  return row.payStatus === 1 ? 'fa fa-check-circle' : 'fa fa-clock-o'
}
function payStatusStyle(row) {
  if (row.payStatus === 2) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  if (row.payStatus === 1) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  return { background: '#FEF2F2', color: '#DC2626', border: '1px solid #FECACA' }
}

function payTypeText(v) {
  const map = { 1: '微信', 2: '支付宝', 3: '现金' }
  return map[v] || '未知'
}

// 前端过滤：匹配用户昵称或景点名称 + 支付状态
const filteredList = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  const st = filterPayStatus.value
  return allRows.value.filter(o => {
    if (st === 0 || st === 1 || st === 2) {
      if (Number(o.payStatus) !== st) return false
    }
    if (!kw) return true
    const nick = o.userId?.nickname || ''
    const scenic = o.scenicId?.name || ''
    return nick.toLowerCase().includes(kw) || scenic.toLowerCase().includes(kw)
  })
})

// 统计（基于当前筛选结果）
const paidCount = computed(() => filteredList.value.filter(o => o.payStatus === 1).length)
const unpaidCount = computed(() => filteredList.value.filter(o => o.payStatus === 0).length)

// 本地分页
const totalElements = computed(() => filteredList.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / pageSize.value)))
const list = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// 筛选结果变少时把页码拉回有效范围
watch(totalPages, tp => { if (currentPage.value > tp) currentPage.value = tp })

// ---- 查询 ----
function handleSearch() { currentPage.value = 1 }
function resetSearch() { searchKeyword.value = ''; filterPayStatus.value = ''; currentPage.value = 1 }
function handleSizeChange() { currentPage.value = 1 }

// ---- 编辑 ----
const editVisible = ref(false)
const editSubmitting = ref(false)
const editRow = ref(null)
const editPayStatusValue = ref(1)

function openEditDialog(row) {
  if (row.payStatus === 2) {
    ElMessage.warning('免费订单无需修改支付状态')
    return
  }
  editRow.value = row
  editPayStatusValue.value = row.payStatus === 1 ? 1 : 0
  editVisible.value = true
}

async function handleEdit() {
  if (!editRow.value) return
  editSubmitting.value = true
  try {
    const r = editRow.value
    const payload = {
      id: r.id,
      orderNo: r.orderNo,
      userId: r.userId ? { id: r.userId.id } : null,
      scenicId: r.scenicId ? { id: r.scenicId.id } : null,
      contactName: r.contactName,
      contactPhone: r.contactPhone,
      playDate: r.playDate,
      ticketCount: r.ticketCount,
      totalAmount: r.totalAmount,
      payType: r.payType,
      payStatus: editPayStatusValue.value
    }
    const res = await editOrder(payload)
    if (res.code === 1) {
      ElMessage.success('支付状态已更新')
      editVisible.value = false
      loadAll()
    }
  } catch (err) { console.error(err)
  } finally { editSubmitting.value = false }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除订单「${row.orderNo}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined }
    )
    const res = await deleteOrder(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      loadAll()
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

onMounted(() => { loadAll() })
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
