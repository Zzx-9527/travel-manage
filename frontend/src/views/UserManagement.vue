<template>
  <div class="page-container">
    <!-- 页面标题与操作栏 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">用户管理</h2>
        <p class="text-sm mt-1" style="color: #94A3B8;">管理系统中的所有注册用户</p>
      </div>
      <el-button
        type="primary"
        @click="openAddDialog"
        style="background: linear-gradient(135deg, #1CB89D, #0D9488); border: none; border-radius: 10px; padding: 10px 22px;"
      >
        <i class="fa fa-plus mr-2"></i>添加用户
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
            placeholder="搜索账号、昵称..."
            clearable
            :prefix-icon="null"
            style="width: 100%;"
            @keyup.enter="handleSearch"
          />
        </div>
        <el-select v-model="searchForm.userRole" placeholder="用户角色" clearable style="width: 150px;">
          <el-option label="全部角色" :value="null" />
          <el-option label="普通用户" :value="0" />
          <el-option label="管理员" :value="1" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="用户状态" clearable style="width: 150px;">
          <el-option label="全部状态" :value="null" />
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
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
    <div
      class="bg-white rounded-xl overflow-hidden"
      style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);"
    >
      <el-table
        :data="userList"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: 600 }"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar">
              <i class="fa fa-user"></i>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="account" label="账号" min-width="120" align="center" />
        <el-table-column prop="nickname" label="昵称" min-width="120" align="center" />
        <el-table-column prop="phone" label="手机号" width="140" align="center" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              effect="plain"
              size="small"
              :style="{
                borderRadius: '6px',
                border: 'none',
                padding: '2px 12px',
                background: row.userRole === 1 ? '#FEF2F2' : '#F1F5F9'
              }"
              disable-transitions
            >
              <span :style="{ color: row.userRole === 1 ? '#DC2626' : '#64748B' }">
                <i :class="row.userRole === 1 ? 'fa fa-crown mr-1' : 'fa fa-user mr-1'"></i>
                {{ row.userRole === 1 ? '管理员' : '普通用户' }}
              </span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <span
              class="inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full"
              :style="{
                background: row.status === 1 ? '#F0FDF4' : '#FEF2F2',
                color: row.status === 1 ? '#16A34A' : '#DC2626'
              }"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                :style="{ background: row.status === 1 ? '#22C55E' : '#EF4444' }"
              ></span>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            <span style="color: #64748B;">{{ row.createTime?.replace(/\.0$/, '').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="修改时间" width="170" align="center">
          <template #default="{ row }">
            <span style="color: #64748B;">{{ row.updateTime?.replace(/\.0$/, '').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-tooltip content="编辑用户" placement="top" :show-after="300">
                <button
                  @click="openEditDialog(row)"
                  class="action-btn edit-btn"
                  title="编辑"
                >
                  <i class="fa fa-pencil-square-o"></i>
                </button>
              </el-tooltip>
              <el-tooltip content="删除用户" placement="top" :show-after="300">
                <button
                  @click="handleDelete(row)"
                  class="action-btn delete-btn"
                  title="删除"
                >
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

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      :width="520"
      :close-on-click-modal="false"
      :before-close="handleCloseDialog"
      top="8vh"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
        label-position="left"
        status-icon
      >
        <el-form-item label="账号" prop="account">
          <el-input v-model="formData.account" placeholder="请输入账号" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
            clearable
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="用户角色">
          <el-radio-group v-model="formData.userRole">
            <el-radio :value="0" style="margin-right: 20px;">
              <span><i class="fa fa-user mr-1"></i>普通用户</span>
            </el-radio>
            <el-radio :value="1">
              <span><i class="fa fa-crown mr-1" style="color: #F59E0B;"></i>管理员</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="formData.status"
            :active-value="1"
            :inactive-value="0"
            active-color="#1CB89D"
            inactive-color="#CBD5E1"
            inline-prompt
            :active-icon="Check"
            :inactive-icon="Close"
          />
          <span class="ml-2 text-sm" :style="{ color: formData.status === 1 ? '#16A34A' : '#94A3B8' }">
            {{ formData.status === 1 ? '启用' : '禁用' }}
          </span>
        </el-form-item>
        <el-form-item label="头像链接">
          <el-input v-model="formData.avatar" placeholder="请输入头像URL" clearable />
        </el-form-item>
        <el-form-item label="头像预览" v-if="formData.avatar">
          <el-avatar :size="48" :src="formData.avatar">
            <i class="fa fa-user"></i>
          </el-avatar>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { getUsersByPage, addUser, editUser, deleteUser } from '@/api/user'
import { useAdminTable } from '@/composables/useAdminTable'

// 搜索（单输入框模糊搜索）
const searchKeyword = ref('')
const searchForm = reactive({
  account: '',
  nickname: '',
  userRole: null,
  status: null
})

const submitting = ref(false)
const { list: userList, loading, currentPage, pageSize, totalElements, totalPages, fetchData: fetchUsers } = useAdminTable(getUsersByPage, {
  extraParams: () => {
    const params = {}
    if (searchKeyword.value) params.account = searchKeyword.value
    if (searchForm.userRole !== null && searchForm.userRole !== undefined) params.userRole = searchForm.userRole
    if (searchForm.status !== null && searchForm.status !== undefined) params.status = searchForm.status
    return params
  }
})

// 对话框状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const defaultForm = {
  account: '',
  password: '',
  nickname: '',
  phone: '',
  avatar: '',
  userRole: 0,
  status: 1
}

const formData = reactive({ ...defaultForm })

// 表单校验规则（编辑时密码非必填）
const formRules = computed(() => {
  const rules = {
    account: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    nickname: [
      { required: true, message: '请输入昵称', trigger: 'blur' }
    ],
    phone: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ]
  }
  if (!isEdit.value) {
    rules.password = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 5, max: 20, message: '密码长度在 5 到 20 个字符', trigger: 'blur' }
    ]
  } else {
    rules.password = [
      { min: 5, max: 20, message: '密码长度在 5 到 20 个字符', trigger: 'blur' }
    ]
  }
  return rules
})

// 查询用户列表（page 从 1 开始，与后端匹配）
function handleSearch() {
  currentPage.value = 1
  fetchUsers()
}

function resetSearch() {
  searchKeyword.value = ''
  searchForm.userRole = null
  searchForm.status = null
  currentPage.value = 1
  fetchUsers()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchUsers()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchUsers()
}

// 打开添加对话框
function openAddDialog() {
  isEdit.value = false
  editingId.value = null
  Object.assign(formData, defaultForm)
  dialogVisible.value = true
}

// 打开编辑对话框
function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  formData.account = row.account
  formData.password = ''
  formData.nickname = row.nickname
  formData.phone = row.phone
  formData.avatar = row.avatar || ''
  formData.userRole = row.userRole
  formData.status = row.status
  dialogVisible.value = true
}

function handleCloseDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
    const payload = {
      account: formData.account,
      nickname: formData.nickname,
      phone: formData.phone,
      avatar: formData.avatar || `https://picsum.photos/200/200?random=${Date.now()}`,
      userRole: formData.userRole,
      status: formData.status,
      createTime: now,
      updateTime: now
    }

    if (isEdit.value) {
      payload.id = editingId.value
      if (formData.password) {
        payload.password = formData.password
      }
      const res = await editUser(payload)
      if (res.code === 1) {
        ElMessage.success('修改成功')
        handleCloseDialog()
        fetchUsers()
      }
    } else {
      payload.password = formData.password
      const res = await addUser(payload)
      if (res.code === 1) {
        ElMessage.success('添加成功')
        handleCloseDialog()
        fetchUsers()
      }
    }
  } catch (err) {
    console.error('提交失败:', err)
  } finally {
    submitting.value = false
  }
}

// 删除用户
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.nickname}」吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        icon: undefined
      }
    )
    const res = await deleteUser(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchUsers()
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败:', err)
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
/* 操作按钮统一样式 */
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

/* Element Plus 微调 */
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
:deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
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
:deep(.el-table__cell) {
  padding: 8px 0;
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
