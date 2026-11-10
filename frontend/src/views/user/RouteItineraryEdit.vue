<template>
  <div class="itin-page">
    <!-- 返回 -->
    <router-link to="/travel/home/mine/publish" class="back-link">
      <i class="fa fa-arrow-left mr-1"></i>返回我的发布
    </router-link>

    <!-- 参数缺失 -->
    <EmptyState v-if="!routeId" title="缺少路线参数" desc="请从「我的发布」中进入配置行程" />

    <div v-else-if="loading" class="flex justify-center py-16">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
    </div>

    <EmptyState v-else-if="degraded || !routeData" title="路线加载失败" desc="请稍后重试，或检查该路线是否存在" />

    <EmptyState v-else-if="!isOwner" title="无权编辑" desc="只有路线发布者可以配置行程" />

    <template v-else>
      <!-- 路线概要 -->
      <div class="route-head">
        <h2 class="route-name"><i class="fa fa-map-o mr-2" style="color: #1CB89D;"></i>{{ routeData.name }}</h2>
        <p class="route-sub">
          <span class="route-tag days"><i class="fa fa-clock-o mr-1"></i>{{ routeData.dayCount }} 天</span>
          <span class="route-tag" :style="trafficStyle(routeData.traffic)">{{ routeData.traffic || '其他' }}</span>
          <span class="route-budget">{{ priceText(routeData.budget) }}</span>
        </p>
      </div>

      <!-- 行程列表 -->
      <div class="itin-card">
        <div class="itin-head">
          <div>
            <h3 class="itin-title">行程安排</h3>
            <p class="itin-desc">按天添加途经景点，配置游玩顺序与停留时间</p>
          </div>
          <el-button type="primary" @click="openAddDialog" style="border-radius: 10px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
            <i class="fa fa-plus mr-1"></i>添加景点
          </el-button>
        </div>

        <el-empty v-if="!itineraryLoading && !items.length" description="还没有行程，点击右上角添加第一个景点" :image-size="90" />
        <div v-else class="journey">
          <div v-for="group in dayGroups" :key="group.day" class="day-block">
            <div class="day-head">
              <span class="day-num">Day {{ group.day }}</span>
              <span class="day-desc">第 {{ group.day }} 天</span>
            </div>
            <div class="day-items">
              <div v-for="(item, idx) in group.items" :key="item.id" class="day-item">
                <div class="item-index">{{ group.startIndex + idx + 1 }}</div>
                <div class="item-main">
                  <p class="item-name">{{ item.scenicId?.name || '未知景点' }}</p>
                  <p class="item-stay"><i class="fa fa-clock-o mr-1"></i>停留 {{ item.stayTime || '未标注' }}</p>
                </div>
                <div class="item-ops">
                  <button class="op-btn" title="编辑" @click="openEditDialog(item)"><i class="fa fa-pencil-square-o"></i></button>
                  <button class="op-btn danger" title="删除" @click="handleDelete(item)"><i class="fa fa-trash-o"></i></button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 添加 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑途经景点' : '添加途经景点'" :width="480" :close-on-click-modal="false" top="15vh">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" label-position="left">
        <el-form-item label="景点" prop="scenicId">
          <el-select v-model="formData.scenicId" placeholder="搜索选择景点" filterable style="width: 100%;">
            <el-option v-for="s in scenicOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="第几天" prop="dayNo">
            <el-input-number v-model="formData.dayNo" :min="1" :max="routeData?.dayCount || 30" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" :min="1" :max="999" style="width: 100%;" />
          </el-form-item>
        </div>
        <el-form-item label="停留时间" prop="stayTime">
          <el-input v-model="formData.stayTime" placeholder="如：4小时 / 半天 / 全天" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="dialogVisible = false" style="border-radius: 8px;">取 消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
            {{ isEdit ? '保存修改' : '确认添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import { priceText, trafficStyle } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { getRouteDetail } from '@/api/route'
import { getRouteDetailsByPage, addRouteDetail, editRouteDetail, deleteRouteDetail } from '@/api/routeDetail'
import { getSpotsByPage } from '@/api/scenic'

const route = useRoute()
const authStore = useAuthStore()

const routeId = computed(() => Number(route.query.routeId) || null)

const routeData = ref(null)
const loading = ref(true)
const degraded = ref(false)
const isOwner = ref(false)

const items = ref([])
const itineraryLoading = ref(true)
const scenicOptions = ref([])

// 按天分组（与详情页一致的排序规则）
const dayGroups = computed(() => {
  const map = {}
  for (const d of items.value) {
    const day = d.dayNo || 1
    if (!map[day]) map[day] = []
    map[day].push(d)
  }
  let startIndex = 0
  return Object.keys(map)
    .sort((a, b) => Number(a) - Number(b))
    .map(day => {
      const items = map[day].sort((a, b) => (a.sort || 0) - (b.sort || 0))
      const group = { day: Number(day), items, startIndex }
      startIndex += items.length
      return group
    })
})

async function loadRoute() {
  if (!routeId.value) return
  loading.value = true
  try {
    const res = await getRouteDetail(routeId.value)
    if (res.code === 1) {
      routeData.value = res.content
      const uid = authStore.userInfo?.id
      isOwner.value = !!uid && Number(res.content.userId?.id) === Number(uid)
    } else {
      degraded.value = true
    }
  } catch (err) {
    degraded.value = true
  } finally {
    loading.value = false
  }
}

async function loadItinerary() {
  if (!routeId.value) return
  itineraryLoading.value = true
  try {
    const res = await getRouteDetailsByPage({ page: 1, size: 100, routeId: routeId.value })
    if (res.code === 1) {
      items.value = res.content?.content || res.content || []
    }
  } catch (err) {
    items.value = []
  } finally {
    itineraryLoading.value = false
  }
}

async function loadScenicOptions() {
  try {
    const res = await getSpotsByPage({ page: 1, size: 200, status: 1 })
    if (res.code === 1) scenicOptions.value = res.content?.content || res.content || []
  } catch (err) { /* 留空，让用户换入口 */ }
}

// ---- 弹窗表单 ----
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const submitting = ref(false)

const defaultForm = { scenicId: null, dayNo: 1, sort: 1, stayTime: '' }
const formData = ref({ ...defaultForm })

const formRules = {
  scenicId: [{ required: true, message: '请选择景点', trigger: 'change' }],
  dayNo: [{ required: true, message: '请输入第几天', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }],
  stayTime: [{ required: true, message: '请输入停留时间', trigger: 'blur' }]
}

function openAddDialog() {
  isEdit.value = false
  editingId.value = null
  const maxSort = items.value.reduce((m, i) => Math.max(m, i.sort || 0), 0)
  formData.value = { ...defaultForm, sort: maxSort + 1 }
  dialogVisible.value = true
}

function openEditDialog(item) {
  isEdit.value = true
  editingId.value = item.id
  formData.value = {
    scenicId: item.scenicId?.id ?? null,
    dayNo: item.dayNo ?? 1,
    sort: item.sort ?? 1,
    stayTime: item.stayTime || ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      routeId: { id: routeId.value },
      scenicId: { id: formData.value.scenicId },
      dayNo: formData.value.dayNo,
      sort: formData.value.sort,
      stayTime: formData.value.stayTime
    }
    if (isEdit.value) payload.id = editingId.value

    const res = isEdit.value ? await editRouteDetail(payload) : await addRouteDetail(payload)
    if (res.code === 1) {
      ElMessage.success(isEdit.value ? '修改成功' : '已添加')
      dialogVisible.value = false
      loadItinerary()
    }
  } catch (err) { /* 拦截器已提示 */ }
  finally { submitting.value = false }
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(
      `确定删除行程中的景点「${item.scenicId?.name || '未知景点'}」吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteRouteDetail(item.id)
    if (res.code === 1) {
      ElMessage.success('已删除')
      loadItinerary()
    }
  } catch (err) { /* 取消或拦截器已提示 */ }
}

onMounted(() => {
  loadRoute()
  loadItinerary()
  loadScenicOptions()
})
</script>

<style scoped>
.itin-page {
  min-height: 60vh;
}

.back-link {
  display: inline-block;
  font-size: 13px;
  color: #64748B;
  text-decoration: none;
  margin-bottom: 16px;
  transition: color 0.2s;
}

.back-link:hover { color: #0D9488; }

.route-head {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 18px;
}

.route-name {
  font-size: 20px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 10px;
}

.route-sub {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 0;
}

.route-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.route-tag.days {
  background: #EFF6FF;
  color: #2563EB;
  border-color: #BFDBFE;
}

.route-budget {
  font-size: 15px;
  font-weight: 700;
  color: #059669;
  margin-left: auto;
}

.itin-card {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  padding: 22px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.itin-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.itin-title {
  font-size: 16px;
  font-weight: 700;
  color: #1E293B;
  margin: 0 0 4px;
}

.itin-desc {
  font-size: 12px;
  color: #94A3B8;
  margin: 0;
}

.journey {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.day-block {
  display: flex;
  gap: 18px;
}

.day-head {
  width: 84px;
  flex-shrink: 0;
  padding-top: 8px;
  text-align: center;
}

.day-num {
  display: inline-block;
  font-size: 13px;
  font-weight: 800;
  color: #0D9488;
  background: #F0FDFA;
  border: 1px solid #99F6E4;
  border-radius: 10px;
  padding: 6px 10px;
  margin-bottom: 4px;
}

.day-desc {
  display: block;
  font-size: 12px;
  color: #94A3B8;
}

.day-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.day-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #F8FAFC;
  border: 1px solid #F1F5F9;
  border-radius: 12px;
  padding: 12px 16px;
  transition: all 0.2s;
}

.day-item:hover {
  border-color: #99F6E4;
  box-shadow: 0 4px 14px rgba(28, 184, 157, 0.08);
}

.item-index {
  width: 30px; height: 30px;
  border-radius: 9px;
  background: #CCFBF1;
  color: #0D9488;
  font-size: 13px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.item-main { flex: 1; min-width: 0; }

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
  margin: 0 0 4px;
}

.item-stay {
  font-size: 12px;
  color: #94A3B8;
  margin: 0;
}

.item-ops {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.op-btn {
  width: 30px; height: 30px;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  background: #fff;
  color: #64748B;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.op-btn:hover { border-color: #0D9488; color: #0D9488; background: #F0FDFA; }
.op-btn.danger:hover { border-color: #FCA5A5; color: #DC2626; background: #FEF2F2; }

.form-row {
  display: flex;
  gap: 20px;
}

.form-row .el-form-item { flex: 1; }
</style>
