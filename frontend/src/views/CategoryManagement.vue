<template>
  <div class="page-container">
    <!-- 页面标题与操作栏 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">景点分类管理</h2>
        <p class="text-sm mt-1" style="color: #94A3B8;">管理景点分类信息，用于对景点进行分类归档</p>
      </div>
      <el-button
        type="primary"
        @click="openAddDialog"
        style="background: linear-gradient(135deg, #1CB89D, #0D9488); border: none; border-radius: 10px; padding: 10px 22px;"
      >
        <i class="fa fa-plus mr-2"></i>新增分类
      </el-button>
    </div>

    <!-- 搜索与筛选 -->
    <div
      class="bg-white rounded-xl p-5 mb-5"
      style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);"
    >
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索分类名称..."
            clearable
            :prefix-icon="null"
            style="width: 100%;"
            @keyup.enter="handleSearch"
          />
        </div>
        <el-button @click="handleSearch" style="border-radius: 8px;">
          <i class="fa fa-search mr-1.5"></i>查询
        </el-button>
        <el-button @click="resetSearch" style="border-radius: 8px;">
          <i class="fa fa-refresh mr-1.5"></i>重置
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div
      class="bg-white rounded-xl overflow-hidden"
      style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);"
    >
      <el-table
        :data="categoryList"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: 600 }"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="分类名称" min-width="200" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1.5">
              <i class="fa fa-tag" style="color: #10B981;"></i>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            <span style="color: #64748B;">{{ row.createTime?.replace(/\.0$/, '').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="修改时间" width="180" align="center">
          <template #default="{ row }">
            <span style="color: #64748B;">{{ row.updateTime?.replace(/\.0$/, '').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-tooltip content="编辑分类" placement="top" :show-after="300">
                <button @click="openEditDialog(row)" class="action-btn edit-btn" title="编辑">
                  <i class="fa fa-pencil-square-o"></i>
                </button>
              </el-tooltip>
              <el-tooltip content="删除分类" placement="top" :show-after="300">
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
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="totalElements"
          :pager-count="5"
          layout="sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 添加/编辑分类对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      :width="460"
      :close-on-click-modal="false"
      :before-close="handleCloseDialog"
      top="25vh"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
        label-position="left"
        status-icon
      >
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入景点分类名称"
            clearable
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="handleCloseDialog" style="border-radius: 8px;">取 消</el-button>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="submitting"
            style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;"
          >
            {{ isEdit ? '保存修改' : '确认添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoriesByPage, addCategory, editCategory, deleteCategory } from '@/api/category'
import { useAdminTable } from '@/composables/useAdminTable'

// 搜索
const searchKeyword = ref('')

// 对话框状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const submitting = ref(false)

const defaultForm = { name: '' }
const formData = reactive({ ...defaultForm })

const formRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '名称不能超过 50 个字符', trigger: 'blur' }
  ]
}

const { list: categoryList, loading, currentPage, pageSize, totalElements, totalPages, fetchData: fetchCategories } = useAdminTable(getCategoriesByPage, {
  extraParams: () => {
    const params = {}
    if (searchKeyword.value) params.name = searchKeyword.value
    return params
  }
})

function handleSearch() {
  currentPage.value = 1
  fetchCategories()
}

function resetSearch() {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchCategories()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchCategories()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchCategories()
}

function openAddDialog() {
  isEdit.value = false
  editingId.value = null
  formData.name = ''
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  formData.name = row.name
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
    if (isEdit.value) {
      const res = await editCategory({ id: editingId.value, name: formData.name })
      if (res.code === 1) {
        ElMessage.success('修改成功')
        handleCloseDialog()
        fetchCategories()
      }
    } else {
      const res = await addCategory({ name: formData.name })
      if (res.code === 1) {
        ElMessage.success('添加成功')
        handleCloseDialog()
        fetchCategories()
      }
    }
  } catch (err) {
    console.error('提交失败:', err)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${row.name}」吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        icon: undefined
      }
    )
    const res = await deleteCategory(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchCategories()
    }
  } catch (err) {
    if (err !== 'cancel') console.error('删除失败:', err)
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}
.edit-btn {
  color: #0D9488;
  background: #F0FDFA;
  border-color: #CCFBF1;
}
.edit-btn:hover {
  background: #CCFBF1;
  border-color: #5EEAD4;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.2);
}
.delete-btn {
  color: #DC2626;
  background: #FEF2F2;
  border-color: #FECACA;
}
.delete-btn:hover {
  background: #FEE2E2;
  border-color: #FCA5A5;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.2);
}
:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #E2E8F0 inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #1CB89D inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1CB89D inset;
}
:deep(.el-dialog__header) {
  margin-right: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #F1F5F9;
}
:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}
:deep(.el-dialog__body) {
  padding: 24px;
}
:deep(.el-dialog__footer) {
  padding: 16px 24px 20px;
  border-top: 1px solid #F1F5F9;
}
:deep(.el-table__body tr:hover > td) {
  background-color: #F8FAFC !important;
}
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background: #1CB89D;
  color: #fff;
}
:deep(.el-pagination.is-background .el-pager li:not(.disabled).active:hover) {
  background: #0D9488;
}
:deep(.relative .el-input__wrapper) {
  padding-left: 36px;            /* 为图标留出空间，与图标 left-3.5 对齐 */
}
</style>
