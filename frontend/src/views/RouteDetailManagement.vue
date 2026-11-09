<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">路线途经景点管理</h2>
        <p class="text-sm mt-0.5" style="color: #94A3B8;">管理每条路线包含的景点及游玩顺序</p>
      </div>
      <el-button
        type="primary"
        @click="openAddDialog"
        style="background: linear-gradient(135deg, #1CB89D, #0D9488); border: none; border-radius: 10px; padding: 10px 22px;"
      >
        <i class="fa fa-plus mr-2"></i>新增途经景点
      </el-button>
    </div>

    <!-- 搜索 -->
    <div class="bg-white rounded-xl p-5 mb-5" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input v-model="searchKeyword" placeholder="搜索路线名称..." clearable :prefix-icon="null" style="width: 100%;" @keyup.enter="handleSearch" />
        </div>
        <el-button @click="handleSearch" style="border-radius: 8px;"><i class="fa fa-search mr-1.5"></i>搜索</el-button>
        <el-button @click="resetSearch" style="border-radius: 8px;"><i class="fa fa-refresh mr-1.5"></i>刷新</el-button>
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
        <el-table-column label="路线名称" min-width="150" align="center">
          <template #default="{ row }">
            <span class="font-semibold text-gray-800 text-sm truncate max-w-[140px] inline-block" :title="row.routeId?.name">{{ row.routeId?.name || '未知路线' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="景点名称" min-width="150" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-700">{{ row.scenicId?.name || '未知景点' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="第几天" width="80" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-orange-50 text-orange-700 font-semibold text-sm">
              {{ row.dayNo }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="65" align="center">
          <template #default="{ row }">
            <span class="text-gray-700 font-mono">{{ row.sort }}</span>
          </template>
        </el-table-column>
        <el-table-column label="停留时间" width="110" align="center">
          <template #default="{ row }">
            <span class="text-gray-700">{{ row.stayTime || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="165" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ formatTime(row.createTime) }}</span></template>
        </el-table-column>
        <el-table-column label="修改时间" width="165" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ formatTime(row.updateTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
              <el-tooltip content="编辑" placement="top" :show-after="300">
                <button @click="openEditDialog(row)" class="action-btn edit-btn" title="编辑">
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
          @size-change="handleSizeChange" @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑途经景点' : '新增途经景点'"
      :width="500"
      :close-on-click-modal="false"
      :before-close="handleCloseDialog"
      top="10vh"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="95px" label-position="left" status-icon>
        <el-form-item label="路线" prop="routeId">
          <el-select v-model="formData.routeId" placeholder="请选择路线" filterable style="width: 100%;">
            <el-option v-for="r in routeOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="景点" prop="scenicId">
          <el-select v-model="formData.scenicId" placeholder="请选择景点" filterable style="width: 100%;">
            <el-option v-for="s in scenicOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="第几天" prop="dayNo">
            <el-input-number v-model="formData.dayNo" :min="1" :max="30" style="width: 100%;" placeholder="如：1" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" :min="1" :max="999" style="width: 100%;" placeholder="如：1" />
          </el-form-item>
        </div>
        <el-form-item label="停留时间" prop="stayTime">
          <el-input v-model="formData.stayTime" placeholder="如：4小时、全天" clearable maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="handleCloseDialog" style="border-radius: 8px;">取 消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting"
            style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
            {{ isEdit ? '保存修改' : '确认添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="途经景点详情" :width="560" top="10vh" destroy-on-close>
      <div v-if="detailData.id">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-5">
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">ID</p>
            <p class="text-gray-800 font-medium">{{ detailData.id }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">所属路线</p>
            <p class="text-gray-800 font-medium">{{ detailData.routeId?.name || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">景点名称</p>
            <p class="text-gray-800 font-medium">{{ detailData.scenicId?.name || '--' }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">第几天</p>
            <p class="text-gray-800 font-medium">第{{ detailData.dayNo }}天</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">排序</p>
            <p class="text-gray-800 font-medium">{{ detailData.sort }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">停留时间</p>
            <p class="text-gray-700">{{ detailData.stayTime || '--' }}</p>
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
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRouteDetailsByPage, addRouteDetail, editRouteDetail, deleteRouteDetail } from '@/api/routeDetail'
import { getRoutesByPage } from '@/api/route'
import { getSpotsByPage } from '@/api/scenic'
import { useAdminTable } from '@/composables/useAdminTable'

const submitting = ref(false)
const searchKeyword = ref('')

const { list, loading, currentPage, pageSize, totalElements, totalPages, fetchData } = useAdminTable(getRouteDetailsByPage, {
  extraParams: () => {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    return params
  }
})

// 下拉选项
const routeOptions = ref([])
const scenicOptions = ref([])

async function loadRouteOptions() {
  try {
    const res = await getRoutesByPage({ page: 1, size: 200 })
    if (res.code === 1) routeOptions.value = res.content.content || []
  } catch (err) { console.error('加载路线列表失败:', err) }
}

async function loadScenicOptions() {
  try {
    const res = await getSpotsByPage({ page: 1, size: 200 })
    if (res.code === 1) scenicOptions.value = res.content.content || []
  } catch (err) { console.error('加载景点列表失败:', err) }
}

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const defaultForm = {
  routeId: null,
  scenicId: null,
  dayNo: 1,
  sort: 1,
  stayTime: ''
}

const formData = reactive({ ...defaultForm })

const formRules = {
  routeId: [{ required: true, message: '请选择路线', trigger: 'change' }],
  scenicId: [{ required: true, message: '请选择景点', trigger: 'change' }],
  dayNo: [{ required: true, message: '请输入第几天', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
  stayTime: [{ required: true, message: '请输入停留时间', trigger: 'blur' }]
}

// 详情
const detailVisible = ref(false)
const detailData = ref({})

function formatTime(dt) {
  return dt ? String(dt).replace(/\.0$/, '').replace('T', ' ') : '--'
}

// ---- 查询 ----
function handleSearch() { currentPage.value = 1; fetchData() }
function resetSearch() { searchKeyword.value = ''; currentPage.value = 1; fetchData() }

function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; fetchData() }
function handlePageChange(p) { currentPage.value = p; fetchData() }

// ---- 表单 ----
function openAddDialog() {
  isEdit.value = false
  editingId.value = null
  Object.assign(formData, defaultForm)
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  formData.routeId = row.routeId?.id || null
  formData.scenicId = row.scenicId?.id || null
  formData.dayNo = row.dayNo ?? 1
  formData.sort = row.sort ?? 1
  formData.stayTime = row.stayTime || ''
  dialogVisible.value = true
}

function handleCloseDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      routeId: { id: formData.routeId },
      scenicId: { id: formData.scenicId },
      dayNo: formData.dayNo,
      sort: formData.sort,
      stayTime: formData.stayTime
    }

    if (isEdit.value) {
      payload.id = editingId.value
      const res = await editRouteDetail(payload)
      if (res.code === 1) {
        ElMessage.success('修改成功')
        handleCloseDialog()
        fetchData()
      }
    } else {
      const res = await addRouteDetail(payload)
      if (res.code === 1) {
        ElMessage.success('添加成功')
        handleCloseDialog()
        fetchData()
      }
    }
  } catch (err) { console.error('提交失败:', err)
  } finally { submitting.value = false }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除路线「${row.routeId?.name}」中的景点「${row.scenicId?.name}」吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined }
    )
    const res = await deleteRouteDetail(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (err) { if (err !== 'cancel') console.error(err) }
}

// ---- 详情 ----
function openDetailDialog(row) {
  detailData.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadRouteOptions()
  loadScenicOptions()
  fetchData()
})
</script>

<style scoped>
.action-btn {
  width: 32px; height: 32px; border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid transparent; cursor: pointer;
  transition: all 0.2s ease; font-size: 14px;
}
.edit-btn {
  color: #0D9488; background: #F0FDFA; border-color: #CCFBF1;
}
.edit-btn:hover {
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
:deep(.el-select .el-input__wrapper) { border-radius: 8px; }
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
</style>
