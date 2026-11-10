<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
      <div>
        <h2 class="text-xl font-bold" style="color: #1E293B;">景点管理</h2>
        <p class="text-sm mt-1" style="color: #94A3B8;">管理所有景点信息，包括分类、价格、评分等</p>
      </div>
      <el-button
        type="primary"
        @click="openAddDialog"
        style="background: linear-gradient(135deg, #1CB89D, #0D9488); border: none; border-radius: 10px; padding: 10px 22px;"
      >
        <i class="fa fa-plus mr-2"></i>新增景点
      </el-button>
    </div>

    <!-- 搜索 -->
    <div class="bg-white rounded-xl p-5 mb-5" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1" style="max-width: 320px;">
          <i class="fa fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-sm" style="color: #94A3B8; z-index: 1;"></i>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索景点名称..."
            clearable
            :prefix-icon="null"
            style="width: 100%;"
            @keyup.enter="handleSearch"
          />
        </div>
        <el-select v-model="filterStatus" placeholder="景点状态" clearable style="width: 150px;" @change="handleSearch">
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
    <div class="bg-white rounded-xl overflow-hidden" style="box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
      <el-table
        :data="spotList"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: 600 }"
        @row-click="openDetailDialog"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="封面" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.cover"
              :src="row.cover"
              style="width: 60px; height: 40px; border-radius: 6px; cursor: pointer;"
              fit="cover"
              lazy
              :preview-src-list="[row.cover]"
              preview-teleported
              @click.stop
            >
              <template #error>
                <div class="w-[60px] h-[40px] bg-gray-100 rounded-md flex items-center justify-center text-gray-400 text-xs">
                  <i class="fa fa-image"></i>
                </div>
              </template>
            </el-image>
            <div v-else class="w-[60px] h-[40px] bg-gray-100 rounded-md flex items-center justify-center text-gray-400 text-xs mx-auto">
              <i class="fa fa-image"></i>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" align="center" />
        <el-table-column label="分类" width="120" align="center">
          <template #default="{ row }">
            <span class="inline-flex px-2.5 py-0.5 rounded-full text-xs" style="background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE;">
              {{ row.categoryId?.name || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.price === 0 || row.price === null" style="color: #10B981; font-weight: 600;">免费</span>
            <span v-else style="font-weight: 600;">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="90" align="center">
          <template #default="{ row }">
            <div class="inline-flex items-center gap-1">
              <i class="fa fa-star" style="color: #F59E0B;"></i>
              <span :style="{ fontWeight: 600, color: (row.score || 0) >= 4.5 ? '#D97706' : '#6B7280' }">
                {{ (row.score || 0).toFixed(1) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span
              class="inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full"
              :style="{
                background: row.status === 1 ? '#F0FDF4' : '#FEF2F2',
                color: row.status === 1 ? '#16A34A' : '#DC2626'
              }"
            >
              <span class="w-1.5 h-1.5 rounded-full" :style="{ background: row.status === 1 ? '#22C55E' : '#EF4444' }"></span>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="165" align="center">
          <template #default="{ row }">
            <span style="color: #64748B;">{{ row.createTime?.replace(/\.0$/, '').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="修改时间" width="165" align="center">
          <template #default="{ row }">
            <span style="color: #64748B;">{{ row.updateTime?.replace(/\.0$/, '').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" @click.stop>
              <el-tooltip content="编辑景点" placement="top" :show-after="300">
                <button @click="openEditDialog(row)" class="action-btn edit-btn" title="编辑">
                  <i class="fa fa-pencil-square-o"></i>
                </button>
              </el-tooltip>
              <el-tooltip content="删除景点" placement="top" :show-after="300">
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

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑景点' : '新增景点'"
      :width="650"
      :close-on-click-modal="false"
      :before-close="handleCloseDialog"
      top="5vh"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" label-position="left" status-icon>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-5">
          <el-form-item label="景点名称" prop="name" class="md:col-span-2">
            <el-input v-model="formData.name" placeholder="请输入景点名称" clearable maxlength="100" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%;" filterable>
              <el-option
                v-for="cat in categoryOptions"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="门票价格">
            <el-input-number v-model="formData.price" :min="0" :precision="2" style="width: 100%;" placeholder="0 表示免费" />
          </el-form-item>
          <el-form-item label="开放时间">
            <el-input v-model="formData.openTime" placeholder="如：06:00-18:00" clearable />
          </el-form-item>
          <el-form-item label="当日停售时间">
            <el-time-picker
              v-model="formData.dayTicketCutoffTime"
              value-format="HH:mm"
              format="HH:mm"
              placeholder="默认 17:00"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="默认日容量" class="md:col-span-2">
            <el-input-number v-model="formData.defaultCapacity" :min="0" :max="100000" style="width: 100%;" placeholder="留空使用全站默认 100" />
            <p class="form-tip">定时任务生成未来 30 天库存时使用；留空表示沿用全站默认 100，编辑时留空则保持原值。</p>
          </el-form-item>
          <el-form-item label="默认成人票库存" class="md:col-span-2">
            <el-input-number v-model="formData.defaultAdultStock" :min="0" :max="100000" style="width: 100%;" placeholder="留空使用全站默认 100" />
            <p class="form-tip">当前仅成人票；单日库存可在库存调整接口中单独修改，已售数量不受调总影响。</p>
          </el-form-item>
          <el-form-item label="评分">
            <el-input-number v-model="formData.score" :min="0" :max="5" :step="0.1" :precision="1" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="浏览量">
            <el-input-number v-model="formData.viewCount" :min="0" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="状态" class="md:col-span-2">
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
          <el-form-item label="地址" class="md:col-span-2">
            <el-input v-model="formData.address" placeholder="请输入详细地址" clearable />
          </el-form-item>
          <el-form-item label="经度">
            <el-input-number v-model="formData.longitude" :step="0.001" :precision="6" style="width: 100%;" placeholder="如：110.488" />
          </el-form-item>
          <el-form-item label="纬度">
            <el-input-number v-model="formData.latitude" :step="0.001" :precision="6" style="width: 100%;" placeholder="如：29.1166" />
          </el-form-item>
          <el-form-item label="封面图" class="md:col-span-2">
            <div class="flex gap-2 w-full">
              <el-input v-model="formData.cover" placeholder="请输入封面图片URL，或点右侧上传" clearable />
              <el-upload :show-file-list="false" :before-upload="beforeCoverUpload" :http-request="doCoverUpload" accept=".jpg,.jpeg,.png,.gif,.webp">
                <el-button :loading="uploadingCover">上传</el-button>
              </el-upload>
            </div>
            <div v-if="formData.cover" class="mt-2">
              <el-image :src="formData.cover" style="width: 120px; height: 72px; border-radius: 8px;" fit="cover">
                <template #error>
                  <div class="w-[120px] h-[72px] bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-xs">加载失败</div>
                </template>
              </el-image>
            </div>
          </el-form-item>
          <el-form-item label="图片集" class="md:col-span-2">
            <el-input v-model="formData.images" placeholder="多个URL用英文逗号分隔" clearable type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="简介" class="md:col-span-2">
            <el-input v-model="formData.intro" type="textarea" :rows="3" placeholder="请输入景点简介描述" maxlength="500" show-word-limit />
          </el-form-item>
        </div>
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

    <!-- 景点详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailData.name || '景点详情'"
      :width="720"
      top="5vh"
      destroy-on-close
    >
      <div v-if="detailData.id">
        <!-- 图片集轮播 -->
        <div v-if="detailImages.length > 0" class="mb-6">
          <el-carousel :interval="4000" height="360px" arrow="always" indicator-position="bottom">
            <el-carousel-item v-for="(img, idx) in detailImages" :key="idx">
              <el-image :src="img" fit="contain" style="height: 100%; width: 100%;" :preview-src-list="detailImages" preview-teleported @click.stop>
                <template #error>
                  <div class="h-full flex items-center justify-center bg-gray-100 rounded-lg text-gray-400 text-sm">图片加载失败</div>
                </template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div v-else class="w-full h-48 bg-gray-50 rounded-xl flex items-center justify-center text-gray-400 mb-6">
          <i class="fa fa-image text-3xl mr-2"></i> 暂无图片
        </div>

        <!-- 详情信息 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">景点 ID</p>
            <p class="text-gray-800 font-medium">{{ detailData.id }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">分类</p>
            <p><span class="inline-flex px-3 py-1 rounded-full text-sm" style="background: #EFF6FF; color: #2563EB;">{{ detailData.categoryId?.name || '--' }}</span></p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">门票价格</p>
            <p v-if="detailData.price === 0 || detailData.price === null" style="color: #10B981; font-weight: 600; font-size: 18px;">免费</p>
            <p v-else style="font-weight: 600; font-size: 18px;">¥{{ detailData.price }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">评分</p>
            <p class="inline-flex items-center gap-1">
              <span style="color: #D97706; font-weight: 600; font-size: 18px;">{{ (detailData.score || 0).toFixed(1) }}</span>
              <span v-for="i in 5" :key="i" style="color: #F59E0B;">
                <i v-if="i <= Math.floor(detailData.score || 0)" class="fa fa-star"></i>
                <i v-else-if="i === Math.ceil(detailData.score || 0) && (detailData.score || 0) % 1 >= 0.5" class="fa fa-star-half-o"></i>
                <i v-else class="fa fa-star-o" style="color: #FCD34D;"></i>
              </span>
            </p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">开放时间</p>
            <p class="text-gray-700">{{ detailData.openTime || '--' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">当日停售时间</p>
            <p class="text-gray-700">{{ detailData.dayTicketCutoffTime || '17:00' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">默认日容量</p>
            <p class="text-gray-700">{{ detailData.defaultCapacity ?? '全站默认 100' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">默认成人票库存</p>
            <p class="text-gray-700">{{ detailData.defaultAdultStock ?? '全站默认 100' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">浏览量</p>
            <p class="text-gray-700 font-medium">{{ detailData.viewCount ?? 0 }}</p>
          </div>
          <div class="md:col-span-2">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">地址</p>
            <p class="text-gray-700">{{ detailData.address || '--' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">经度</p>
            <p class="text-gray-700 font-mono text-sm">{{ detailData.longitude ?? '--' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">纬度</p>
            <p class="text-gray-700 font-mono text-sm">{{ detailData.latitude ?? '--' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">创建时间</p>
            <p class="text-gray-600 text-sm">{{ detailData.createTime?.replace(/\.0$/, '').replace('T', ' ') }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">修改时间</p>
            <p class="text-gray-600 text-sm">{{ detailData.updateTime?.replace(/\.0$/, '').replace('T', ' ') }}</p>
          </div>
          <div class="md:col-span-2">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">状态</p>
            <span
              class="inline-flex items-center gap-1.5 text-sm px-3 py-1 rounded-full"
              :style="{
                background: detailData.status === 1 ? '#F0FDF4' : '#FEF2F2',
                color: detailData.status === 1 ? '#16A34A' : '#DC2626'
              }"
            >
              <span class="w-2 h-2 rounded-full" :style="{ background: detailData.status === 1 ? '#22C55E' : '#EF4444' }"></span>
              {{ detailData.status === 1 ? '启用' : '禁用' }}
            </span>
          </div>
          <div v-if="detailData.intro" class="md:col-span-2">
            <p class="text-xs text-gray-400 uppercase tracking-wide mb-1">简介</p>
            <p class="text-gray-700 leading-relaxed">{{ detailData.intro }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { getSpotsByPage, addSpot, editSpot, deleteSpot } from '@/api/scenic'
import { getCategoriesByPage } from '@/api/category'
import { uploadImage, imageFileError, resolveUploadUrl } from '@/api/file'
import { useAdminTable } from '@/composables/useAdminTable'

// 搜索
const searchKeyword = ref('')
const filterStatus = ref(null)

const submitting = ref(false)
const { list: spotList, loading, currentPage, pageSize, totalElements, totalPages, fetchData: fetchSpots } = useAdminTable(getSpotsByPage, {
  extraParams: () => {
    const params = {}
    if (searchKeyword.value) params.name = searchKeyword.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    return params
  }
})

// 分类下拉选项
const categoryOptions = ref([])
async function loadCategories() {
  try {
    const res = await getCategoriesByPage({ page: 1, size: 200 })
    if (res.code === 1) {
      categoryOptions.value = res.content.content || []
    }
  } catch (err) {
    console.error('加载分类失败:', err)
  }
}

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const defaultForm = {
  name: '',
  categoryId: null,
  price: 0,
  openTime: '',
  dayTicketCutoffTime: '17:00',
  defaultCapacity: null,
  defaultAdultStock: null,
  score: 5,
  address: '',
  longitude: null,
  latitude: null,
  viewCount: 0,
  status: 1,
  cover: '',
  images: '',
  intro: ''
}

const formData = reactive({ ...defaultForm })

// 封面上传（§10）：上传拿 url 填入输入框，随表单 add/edit 提交；
// 入库成功后后端自动删除旧文件（单图覆盖），外部手填 URL 不在此管理内
const uploadingCover = ref(false)
function beforeCoverUpload(file) {
  const err = imageFileError(file)
  if (err) { ElMessage.warning(err); return false }
  return true
}
async function doCoverUpload({ file }) {
  uploadingCover.value = true
  try {
    const res = await uploadImage(file)
    if (res.code === 1) {
      formData.cover = resolveUploadUrl(res.content?.url)
      ElMessage.success('封面上传成功')
    }
  } catch (e) { /* 拦截器已提示 */ }
  finally { uploadingCover.value = false }
}

const formRules = {
  name: [
    { required: true, message: '请输入景点名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ]
}

// 详情对话框
const detailVisible = ref(false)
const detailData = ref({})
const detailImages = computed(() => {
  const images = detailData.value.images || ''
  return images ? images.split(',').map(u => u.trim()).filter(u => u) : []
})

function handleSearch() {
  currentPage.value = 1
  fetchSpots()
}

function resetSearch() {
  searchKeyword.value = ''
  filterStatus.value = null
  currentPage.value = 1
  fetchSpots()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchSpots()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchSpots()
}

// 表单
function openAddDialog() {
  isEdit.value = false
  editingId.value = null
  Object.assign(formData, defaultForm)
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  formData.name = row.name
  formData.categoryId = row.categoryId?.id || null
  formData.price = row.price ?? 0
  formData.openTime = row.openTime || ''
  formData.dayTicketCutoffTime = row.dayTicketCutoffTime || '17:00'
  formData.defaultCapacity = row.defaultCapacity ?? null
  formData.defaultAdultStock = row.defaultAdultStock ?? null
  formData.score = row.score ?? 5
  formData.address = row.address || ''
  formData.longitude = row.longitude ?? null
  formData.latitude = row.latitude ?? null
  formData.viewCount = row.viewCount ?? 0
  formData.status = row.status ?? 1
  formData.cover = row.cover || ''
  formData.images = row.images || ''
  formData.intro = row.intro || ''
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

  // 构造 categoryId 对象
  const selectedCat = categoryOptions.value.find(c => c.id === formData.categoryId)
  const categoryIdObj = selectedCat ? { id: selectedCat.id, name: selectedCat.name } : null

  submitting.value = true
  try {
    const payload = {
      name: formData.name,
      categoryId: categoryIdObj,
      price: formData.price,
      openTime: formData.openTime,
      dayTicketCutoffTime: formData.dayTicketCutoffTime || null,
      defaultCapacity: formData.defaultCapacity ?? null,
      defaultAdultStock: formData.defaultAdultStock ?? null,
      score: formData.score,
      address: formData.address,
      longitude: formData.longitude,
      latitude: formData.latitude,
      viewCount: formData.viewCount,
      status: formData.status,
      cover: formData.cover,
      images: formData.images,
      intro: formData.intro
    }

    if (isEdit.value) {
      payload.id = editingId.value
      const res = await editSpot(payload)
      if (res.code === 1) {
        ElMessage.success('修改成功')
        handleCloseDialog()
        fetchSpots()
      }
    } else {
      const now = new Date().toISOString().replace('T', ' ').substring(0, 19)
      payload.createTime = now
      payload.updateTime = now
      const res = await addSpot(payload)
      if (res.code === 1) {
        ElMessage.success('添加成功')
        handleCloseDialog()
        fetchSpots()
      }
    }
  } catch (err) {
    console.error('提交失败:', err)
  } finally {
    submitting.value = false
  }
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除景点「${row.name}」吗？此操作不可撤销。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        icon: undefined
      }
    )
    const res = await deleteSpot(row.id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      fetchSpots()
    }
  } catch (err) {
    if (err !== 'cancel') console.error('删除失败:', err)
  }
}

// 详情
function openDetailDialog(row) {
  detailData.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadCategories()
  fetchSpots()
})
</script>

<style scoped>
.form-tip {
  width: 100%;
  font-size: 12px;
  color: #94A3B8;
  margin: 4px 0 0;
  line-height: 1.5;
}
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
:deep(.el-carousel__item h3) { display: flex; align-items: center; justify-content: center; }
:deep(.relative .el-input__wrapper) {
  padding-left: 36px;            /* 为图标留出空间，与图标 left-3.5 对齐 */
}
:deep(.el-table__body tr) {
  cursor: pointer;               /* 鼠标变成手型，提示可点击 */
}
</style>
