<template>
  <div>
    <div class="fav-head">
      <h2 class="page-title">我的收藏</h2>
      <div class="type-tabs">
        <button
          v-for="t in typeTabs" :key="t.value"
          class="type-tab" :class="{ active: activeType === t.value }"
          @click="switchType(t.value)"
        >{{ t.label }}</button>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center py-16">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
    </div>

    <EmptyState v-else-if="degraded" title="我的收藏加载失败" desc="请稍后重试，或联系管理员确认服务状态" />

    <el-empty v-else-if="!list.length" :description="emptyText" :image-size="110">
      <router-link to="/travel/home/guides" class="go-btn">去发现</router-link>
    </el-empty>

    <div v-else class="fav-list">
      <div v-for="fav in list" :key="fav.id" class="fav-item">
        <!-- 路线纯文字展示：不渲染封面/图标盒子；攻略/景点有封面才加载 -->
        <div v-if="collectTarget(fav).type !== 3" class="fav-cover" @click="viewFav(fav)">
          <el-image v-if="collectTarget(fav).cover" :src="collectTarget(fav).cover" fit="cover" class="fav-cover-img">
            <template #error><div class="fav-cover-fallback"><i class="fa" :class="favFallbackIcon(fav)"></i></div></template>
          </el-image>
          <div v-else class="fav-cover-fallback"><i class="fa" :class="favFallbackIcon(fav)"></i></div>
        </div>
        <div class="fav-body">
          <div class="fav-title-row">
            <span class="type-badge" :style="collectTypeStyle(collectTarget(fav).type)">{{ collectTypeText(collectTarget(fav).type) }}</span>
            <p class="fav-title" @click="viewFav(fav)">{{ favName(fav) }}</p>
          </div>
          <p class="fav-meta">
            <span v-if="collectTarget(fav).type === 1 && collectTarget(fav).obj?.userId">
              <i class="fa fa-user mr-1"></i>{{ collectTarget(fav).obj.userId.nickname || '未知' }}
            </span>
            <span><i class="fa fa-calendar mr-1"></i>{{ formatTime(fav.createTime) }}</span>
          </p>
        </div>
        <button class="unfav-btn" @click="removeFav(fav)">
          <i class="fa fa-heart-broken mr-1"></i>取消收藏
        </button>
      </div>

      <div v-if="total > size" class="pager">
        <span class="pager-text">共 <b>{{ total }}</b> 条记录，第 {{ page }}/{{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[8, 16, 32]" :total="total" :pager-count="5"
          layout="sizes, prev, pager, next, jumper" background
          @size-change="setSizeSafe" @current-change="setPageSafe"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import { usePageList } from '@/composables/usePageList'
import { useAuthStore } from '@/stores/auth'
import { formatTime, collectTarget, collectTypeText, collectTypeStyle } from '@/utils/format'
import { getMyFavorites, deleteCollectByTarget } from '@/api/collect'
import { getStrategyDetail } from '@/api/strategy'
import { getRouteDetail } from '@/api/route'

const router = useRouter()
const authStore = useAuthStore()

// 分类筛选：全部 / 攻略 / 景点 / 路线（null=全部）
const typeTabs = [
  { value: null, label: '全部' },
  { value: 1, label: '攻略' },
  { value: 2, label: '景点' },
  { value: 3, label: '路线' }
]
const activeType = ref(null)

const emptyText = computed(() => {
  const map = { 1: '还没有收藏的攻略', 2: '还没有收藏的景点', 3: '还没有收藏的路线' }
  return activeType.value ? map[activeType.value] : '还没有收藏的内容'
})

const { list, total, totalPages, page, size, loading, degraded, load, setPage, setSize } = usePageList(getMyFavorites, { size: 8 })

// 收藏支持 攻略(1)/景点(2)/路线(3)。
// 新契约：收藏列表不内嵌攻略对象；路线对象也很薄。
// 攻略用 targetId 调详情挂 item.guide，路线挂 item.route（详情页/卡片需要完整字段）
async function enrichTargets() {
  const tasks = list.value
    .filter(f => Number(f.targetType) === 1 || Number(f.targetType) === 3)
    .map(async f => {
      try {
        const isGuide = Number(f.targetType) === 1
        const res = isGuide ? await getStrategyDetail(f.targetId) : await getRouteDetail(f.targetId)
        if (res.code === 1) {
          if (isGuide) f.guide = res.content
          else f.route = res.content
        }
      } catch (err) { /* 拉取失败则显示占位 */ }
    })
  await Promise.all(tasks)
}

async function reload() {
  const uid = authStore.userInfo?.id
  if (!uid) return
  // collectorId 已失效（§9）：后端按 token 强制只查当前登录人自己的收藏
  const params = {}
  if (activeType.value) params.targetType = activeType.value
  await load(params)
  await enrichTargets()
}

function switchType(v) {
  if (activeType.value === v) return
  activeType.value = v
  page.value = 1
  reload()
}

function setPageSafe(p) { page.value = p; reload() }
function setSizeSafe(s) { size.value = s; page.value = 1; reload() }

function favName(fav) {
  const t = collectTarget(fav)
  return t.name || (t.type === 1 ? `攻略 #${t.id ?? '--'}` : '未知内容')
}

// 封面占位图标：攻略=书本，景点=图片，路线=路线（路线无图，恒用图标）
function favFallbackIcon(fav) {
  const t = collectTarget(fav)
  if (t.type === 3) return 'fa-map-o'
  if (t.type === 2) return 'fa-picture-o'
  return 'fa-book'
}

function viewFav(fav) {
  const t = collectTarget(fav)
  if (!t?.id) return
  const target = {
    1: { path: `/travel/home/guides/${t.id}`, state: { guide: t.obj } },
    2: { path: `/travel/home/spots/${t.id}`, state: { spot: t.obj } },
    3: { path: `/travel/home/routes/${t.id}`, state: { route: fav.route || t.obj } }
  }[t.type]
  if (target) router.push(target)
}

async function removeFav(fav) {
  const uid = authStore.userInfo?.id
  const t = collectTarget(fav)
  if (!uid || !t?.id) return
  try {
    const res = await deleteCollectByTarget({ targetType: t.type, targetId: t.id })
    if (res.code === 1) {
      ElMessage.success('已取消收藏')
      if (list.value.length === 1 && page.value > 1) {
        setPageSafe(page.value - 1)
      } else {
        list.value = list.value.filter(f => f.id !== fav.id)
        total.value = Math.max(0, total.value - 1)
      }
    }
  } catch (err) { /* 拦截器已提示 */ }
}

onMounted(reload)
</script>

<style scoped>
.fav-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0;
}

.type-tabs {
  display: inline-flex;
  background: #F1F5F9;
  border-radius: 999px;
  padding: 3px;
  gap: 2px;
}

.type-tab {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #64748B;
  cursor: pointer;
  transition: all 0.2s;
}

.type-tab:hover { color: #0D9488; }

.type-tab.active {
  background: #fff;
  color: #0D9488;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.15);
}

.go-btn {
  display: inline-block;
  padding: 10px 24px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1CB89D, #0D9488);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.25s;
}

.go-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 15px rgba(28,184,157,0.4); }

.fav-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #F8FAFC;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 12px 16px;
}

.fav-cover {
  width: 120px;
  height: 76px;
  border-radius: 10px;
  flex-shrink: 0;
  cursor: pointer;
  overflow: hidden;
}

.fav-cover-img {
  width: 100%;
  height: 100%;
}

.fav-cover-fallback {
  width: 100%; height: 100%;
  background: #E2E8F0;
  display: flex; align-items: center; justify-content: center;
  color: #94A3B8;
}

.fav-body {
  flex: 1;
  min-width: 0;
}

.fav-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  min-width: 0;
}

.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid transparent;
  flex-shrink: 0;
}

.fav-title {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-title:hover { color: #0D9488; }

.fav-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #94A3B8;
  margin: 0;
}

.unfav-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  border-radius: 10px;
  border: 1px solid #FECACA;
  background: #FFF1F2;
  color: #E11D48;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.unfav-btn:hover { background: #FFE4E6; }

.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.pager-text {
  font-size: 13px;
  color: #94A3B8;
}

.pager-text b { color: #475569; }
</style>
