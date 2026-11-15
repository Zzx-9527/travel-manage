<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">旅游攻略管理</h2>
        <p class="text-sm mt-0.5" style="color: #94A3B8;">管理平台所有旅游攻略内容</p>
      </div>
      <el-button
        type="primary"
        @click="openAddDialog"
        style="background: linear-gradient(135deg, #1CB89D, #0D9488); border: none; border-radius: 10px; padding: 10px 22px;"
      >
        <i class="fa fa-plus mr-2"></i>添加攻略
      </el-button>
    </div>

    <!-- 搜索 -->
    <div class="bg-white rounded-xl p-5 mb-5" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input v-model="searchKeyword" placeholder="搜索标题..." clearable :prefix-icon="null" style="width: 100%;" @keyup.enter="handleSearch" />
        </div>
        <el-select v-model="filterStatus" placeholder="审核状态" clearable style="width: 150px;" @change="handleSearch">
          <el-option label="全部状态" :value="null" />
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button @click="handleSearch" style="border-radius: 8px;"><i class="fa fa-search mr-1.5"></i>查询</el-button>
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
        <el-table-column prop="id" label="ID" width="65" align="center" />
        <el-table-column label="封面" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.cover" :src="resolveUploadUrl(row.cover)" style="width: 56px; height: 38px; border-radius: 6px; cursor: pointer;" fit="cover" lazy
              :preview-src-list="[resolveUploadUrl(row.cover)]" preview-teleported @click.stop
            >
              <template #error><div class="w-[56px] h-[38px] bg-gray-100 rounded flex items-center justify-center text-gray-400 text-xs"><i class="fa fa-image"></i></div></template>
            </el-image>
            <div v-else class="w-[56px] h-[38px] bg-gray-100 rounded flex items-center justify-center text-gray-400 text-xs mx-auto"><i class="fa fa-image"></i></div>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="180" align="center">
          <template #default="{ row }">
            <div class="max-w-xs mx-auto">
              <p class="font-semibold text-gray-800 text-sm truncate" :title="row.title">{{ row.title }}</p>
              <p class="text-xs text-gray-400 mt-1 truncate">{{ truncateText(row.content, 50) }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="130" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-avatar :size="24" :src="row.userId?.avatar" style="flex-shrink: 0;">
                <i class="fa fa-user text-xs"></i>
              </el-avatar>
              <span class="text-sm truncate max-w-[80px]">{{ row.userId?.nickname || '未知' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
              :style="auditStyle(row.auditStatus)"
            >
              <i :class="auditIcon(row.auditStatus)"></i>
              {{ auditText(row.auditStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="165" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ row.createTime?.replace(/\.0$/, '').replace('T', ' ') }}</span></template>
        </el-table-column>
        <el-table-column label="修改时间" width="165" align="center">
          <template #default="{ row }"><span style="color: #64748B;">{{ row.updateTime?.replace(/\.0$/, '').replace('T', ' ') }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
              <el-tooltip content="修改审核状态" placement="top" :show-after="300">
                <button @click="openAuditDialog(row)" class="action-btn audit-btn" title="审核">
                  <i class="fa fa-check-circle-o"></i>
                </button>
              </el-tooltip>
              <el-tooltip content="删除攻略" placement="top" :show-after="300">
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

    <!-- 添加攻略对话框 -->
    <el-dialog v-model="addVisible" title="添加攻略" :width="560" :close-on-click-modal="false" :before-close="() => { addVisible = false; addFormRef?.resetFields() }" top="5vh">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px" label-position="left" status-icon>
        <el-form-item label="作者ID">
          <el-input :model-value="10" disabled style="color: #94A3B8;">
            <template #prefix><i class="fa fa-lock text-gray-400"></i></template>
          </el-input>
          <p class="text-xs text-gray-400 mt-1">管理后台默认以管理员身份发布</p>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="addForm.title" placeholder="请输入攻略标题" clearable maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="addForm.cover" placeholder="https://..." clearable />
          <div v-if="addForm.cover" class="mt-2">
            <el-image :src="addForm.cover" style="width: 120px; height: 72px; border-radius: 8px;" fit="cover">
              <template #error><div class="w-[120px] h-[72px] bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-xs">加载失败</div></template>
            </el-image>
          </div>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="addForm.content" type="textarea" :rows="5" placeholder="请输入攻略详细内容..." maxlength="5000" show-word-limit />
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="收藏数">
            <el-input-number v-model="addForm.collectCount" :min="0" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="阅读数">
            <el-input-number v-model="addForm.readCount" :min="0" style="width: 100%;" />
          </el-form-item>
        </div>
        <el-form-item label="审核状态">
          <el-select v-model="addForm.auditStatus" style="width: 100%;">
            <el-option :value="0"><span><i class="fa fa-clock-o text-amber-500 mr-1"></i>待审核</span></el-option>
            <el-option :value="1"><span><i class="fa fa-check-circle text-emerald-500 mr-1"></i>已通过</span></el-option>
            <el-option :value="2"><span><i class="fa fa-times-circle text-red-500 mr-1"></i>已拒绝</span></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="addVisible = false" style="border-radius: 8px;">取 消</el-button>
          <el-button type="primary" @click="handleAdd" :loading="addSubmitting"
            style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
            <i class="fa fa-plus mr-1"></i>确认添加
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审核状态对话框 -->
    <el-dialog v-model="auditVisible" title="修改审核状态" :width="460" :close-on-click-modal="false" top="25vh">
      <div class="flex gap-4 mb-5 p-4 rounded-xl" style="background: #F8FAFC;">
        <el-image v-if="auditRow?.cover" :src="resolveUploadUrl(auditRow.cover)" style="width: 96px; height: 64px; border-radius: 8px; flex-shrink: 0;" fit="cover">
          <template #error><div class="w-24 h-16 bg-gray-200 rounded-lg flex items-center justify-center text-gray-400 text-xs"><i class="fa fa-image"></i></div></template>
        </el-image>
        <div class="flex-1 min-w-0">
          <p class="text-xs text-gray-400 mb-1">攻略标题</p>
          <p class="font-semibold text-gray-800 truncate text-sm">{{ auditRow?.title }}</p>
          <p class="text-xs text-gray-400 mt-2">作者：<span class="text-gray-600">{{ auditRow?.userId?.nickname || '未知' }}</span></p>
        </div>
      </div>
      <el-form label-width="0">
        <el-form-item>
          <el-select v-model="auditStatusValue" style="width: 100%;" size="large">
            <el-option :value="0"><span><i class="fa fa-clock-o text-amber-500 mr-2"></i>待审核</span></el-option>
            <el-option :value="1"><span><i class="fa fa-check-circle text-emerald-500 mr-2"></i>已通过</span></el-option>
            <el-option :value="2"><span><i class="fa fa-times-circle text-red-500 mr-2"></i>已拒绝</span></el-option>
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
    <el-dialog v-model="detailVisible" :title="detailData.title || '攻略详情'" :width="680" top="5vh" destroy-on-close>
      <div v-if="detailData.id">
        <div v-if="detailData.cover" class="mb-5">
          <el-image :src="resolveUploadUrl(detailData.cover)" style="width: 100%; height: 240px; border-radius: 12px;" fit="cover" :preview-src-list="[detailData.cover]" preview-teleported>
            <template #error>
              <div class="w-full h-48 bg-gray-100 rounded-xl flex items-center justify-center text-gray-400"><i class="fa fa-image text-3xl mr-2"></i>暂无封面</div>
            </template>
          </el-image>
        </div>
        <div v-else class="w-full h-48 bg-gray-100 rounded-xl flex items-center justify-center text-gray-400 mb-5">
          <i class="fa fa-image text-3xl mr-2"></i>暂无封面图片
        </div>
        <h2 class="text-xl font-bold text-gray-800 mb-4 text-center">{{ detailData.title }}</h2>

        <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-5">
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide">攻略 ID</p>
            <p class="text-gray-800 font-medium">{{ detailData.id }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide">审核状态</p>
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium mt-1"
              :style="auditStyle(detailData.auditStatus)">
              <i :class="auditIcon(detailData.auditStatus)"></i>{{ auditText(detailData.auditStatus) }}
            </span>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide">收藏数</p>
            <p class="text-gray-800 font-medium"><i class="fa fa-heart text-red-400 mr-1"></i>{{ detailData.collectCount || 0 }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide">阅读数</p>
            <p class="text-gray-800 font-medium"><i class="fa fa-eye text-blue-400 mr-1"></i>{{ detailData.readCount || 0 }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide">创建时间</p>
            <p class="text-gray-600 text-sm">{{ detailData.createTime?.replace(/\.0$/, '').replace('T', ' ') }}</p>
          </div>
          <div class="rounded-lg p-3" style="background: #F8FAFC;">
            <p class="text-xs text-gray-400 uppercase tracking-wide">修改时间</p>
            <p class="text-gray-600 text-sm">{{ detailData.updateTime?.replace(/\.0$/, '').replace('T', ' ') }}</p>
          </div>
        </div>

        <div class="rounded-lg p-4 mb-5 flex items-center gap-3" style="background: #F8FAFC;">
          <el-avatar :size="40" :src="detailData.userId?.avatar"><i class="fa fa-user"></i></el-avatar>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide">作者</p>
            <p class="font-semibold text-gray-800">{{ detailData.userId?.nickname || '未知用户' }}</p>
            <p class="text-xs text-gray-400">账号：{{ detailData.userId?.account || '--' }}</p>
          </div>
        </div>

        <div>
          <p class="text-xs text-gray-400 uppercase tracking-wide mb-2">攻略内容</p>
          <div class="p-4 rounded-xl leading-relaxed text-sm whitespace-pre-wrap" style="background: #F8FAFC; color: #374151; max-height: 300px; overflow-y: auto;">
            {{ detailData.content || '暂无内容' }}
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { resolveUploadUrl } from '@/api/file'
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStrategiesByPage, addStrategy, editStrategy, deleteStrategy } from '@/api/strategy'
import { useAdminTable } from '@/composables/useAdminTable'

const searchKeyword = ref('')
const filterStatus = ref(null)

const { list, loading, currentPage, pageSize, totalElements, totalPages, fetchData } = useAdminTable(getStrategiesByPage, {
  extraParams: () => {
    const params = {}
    if (searchKeyword.value) params.title = searchKeyword.value
    if (filterStatus.value !== null) params.auditStatus = filterStatus.value
    return params
  }
})

// ---- 工具 ----
function truncateText(text, max = 50) {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}
function auditText(s) {
  return s === 0 ? '待审核' : s === 1 ? '已通过' : s === 2 ? '已拒绝' : '未知'
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

// ---- 添加 ----
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref(null)
const addForm = reactive({
  title: '', cover: '', content: '', collectCount: 0, readCount: 0, auditStatus: 0
})
const addRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}
function openAddDialog() {
  addForm.title = ''; addForm.cover = ''; addForm.content = ''
  addForm.collectCount = 0; addForm.readCount = 0; addForm.auditStatus = 0
  addVisible.value = true
}
async function handleAdd() {
  if (!addFormRef.value) return
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return
  addSubmitting.value = true
  try {
    const payload = {
      title: addForm.title, cover: addForm.cover, content: addForm.content,
      collectCount: addForm.collectCount, readCount: addForm.readCount, auditStatus: addForm.auditStatus
    }
    const res = await addStrategy(payload)
    if (res.code === 1) {
      ElMessage.success('添加成功')
      addVisible.value = false
      currentPage.value = 1; fetchData()
    }
  } catch (err) { console.error(err)
  } finally { addSubmitting.value = false }
}

// ---- 审核 ----
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const auditRow = ref(null)
const auditStatusValue = ref(0)
function openAuditDialog(row) {
  auditRow.value = row
  auditStatusValue.value = row.auditStatus
  auditVisible.value = true
}
async function handleAudit() {
  if (!auditRow.value) return
  auditSubmitting.value = true
  try {
    const s = auditRow.value
    const payload = {
      id: s.id,
      userId: { id: s.userId?.id },
      title: s.title, cover: s.cover, content: s.content,
      collectCount: s.collectCount, readCount: s.readCount,
      auditStatus: auditStatusValue.value
    }
    const res = await editStrategy(payload)
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
    await ElMessageBox.confirm(`确定要删除攻略「${row.title}」吗？此操作不可撤销。`, '删除确认', {
      confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', icon: undefined
    })
    const res = await deleteStrategy(row.id)
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
:deep(.el-select .el-input__wrapper) { border-radius: 8px; }
:deep(.el-dialog__header) { margin-right: 0; padding: 20px 24px 16px; border-bottom: 1px solid #F1F5F9; }
:deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1E293B; }
:deep(.el-dialog__body) { padding: 24px; }
:deep(.el-dialog__footer) { padding: 16px 24px 20px; border-top: 1px solid #F1F5F9; }
:deep(.el-table__body tr:hover > td) { background-color: #F8FAFC !important; }
:deep(.el-table__cell) { padding: 6px 0; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) { background: #1CB89D; color: #fff; }
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active:hover) { background: #0D9488; }
:deep(.el-table__body tr) {
  cursor: pointer;               /* 鼠标变成手型，提示可点击 */
}
:deep(.relative .el-input__wrapper) {
  padding-left: 36px;            /* 为图标留出空间，与图标 left-3.5 对齐 */
}
</style>
