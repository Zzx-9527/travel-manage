<template>
  <div>
    <h2 class="page-title">我的发布</h2>
    <p class="page-desc">查看自己发布的攻略与路线，以及审核状态</p>

    <el-tabs v-model="activeTab" class="pub-tabs">
      <!-- ===== 我发布的攻略 ===== -->
      <el-tab-pane label="我发布的攻略" name="guide">
        <div v-if="guideLoading" class="flex justify-center py-14">
          <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
        </div>
        <EmptyState v-else-if="guideDegraded" title="攻略加载失败" desc="请稍后重试，或联系管理员确认服务状态" />
        <el-empty v-else-if="!guideList.length" description="还没有发布过攻略" :image-size="100">
          <router-link to="/travel/home/publish" class="go-btn">去发布攻略</router-link>
        </el-empty>
        <div v-else class="pub-list">
          <div v-for="g in guideList" :key="g.id" class="pub-item">
            <div class="pub-cover" @click="viewGuide(g)">
              <el-image v-if="g.cover" :src="g.cover" fit="cover" lazy class="pub-cover-img">
                <template #error><div class="pub-cover-fallback"><i class="fa fa-book"></i></div></template>
              </el-image>
              <div v-else class="pub-cover-fallback"><i class="fa fa-book"></i></div>
            </div>
            <div class="pub-body">
              <div class="pub-title-row">
                <span class="status-badge" :style="auditStyle(g.auditStatus)">
                  <i :class="auditIcon(g.auditStatus)"></i>{{ auditText(g.auditStatus) }}
                </span>
                <p class="pub-title" @click="viewGuide(g)">{{ g.title || '无标题' }}</p>
              </div>
              <p class="pub-meta">
                <span><i class="fa fa-calendar mr-1"></i>{{ formatTime(g.createTime) }}</span>
              </p>
            </div>
            <div class="pub-actions">
              <el-button size="small" type="primary" @click="viewGuide(g)" style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">查看</el-button>
            </div>
          </div>
          <div v-if="guideTotal > guideSize" class="pager">
            <span class="pager-text">共 <b>{{ guideTotal }}</b> 条，第 {{ guidePage }}/{{ guideTotalPages }} 页</span>
            <el-pagination
              v-model:current-page="guidePage" v-model:page-size="guideSize"
              :page-sizes="[8, 16, 32]" :total="guideTotal" :pager-count="5"
              layout="sizes, prev, pager, next, jumper" background
              @size-change="setGuideSize" @current-change="setGuidePage"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== 我发布的路线 ===== -->
      <el-tab-pane label="我发布的路线" name="route">
        <div v-if="routeLoading" class="flex justify-center py-14">
          <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
        </div>
        <EmptyState v-else-if="routeDegraded" title="路线加载失败" desc="请稍后重试，或联系管理员确认服务状态" />
        <el-empty v-else-if="!routeList.length" description="还没有发布过路线" :image-size="100">
          <router-link to="/travel/home/publish" class="go-btn">去发布路线</router-link>
        </el-empty>
        <div v-else class="pub-list">
          <div v-for="r in routeList" :key="r.id" class="pub-item route">
            <div class="route-head">
              <span class="status-badge" :style="auditStyle(r.status)">
                <i :class="auditIcon(r.status)"></i>{{ auditText(r.status) }}
              </span>
              <p class="pub-title" @click="viewRoute(r)">{{ r.name || '未命名路线' }}</p>
            </div>
            <p class="route-sub">
              <span class="route-tag"><i class="fa fa-clock-o mr-1"></i>{{ r.dayCount }} 天</span>
              <span class="route-tag" :style="trafficStyle(r.traffic)">{{ normalizeTraffic(r.traffic) }}</span>
              <span class="route-price">{{ priceText(r.budget) }}</span>
            </p>
            <p class="route-intro">{{ truncate(r.intro, 50) || '暂无简介' }}</p>
            <p class="pub-meta">
              <span><i class="fa fa-calendar mr-1"></i>{{ formatTime(r.createTime) }}</span>
            </p>
            <div class="pub-actions">
              <el-button size="small" type="primary" plain @click="viewRoute(r)" style="border-radius: 8px; color: #0D9488; border-color: #5EEAD4;">查看</el-button>
              <el-button size="small" type="primary" @click="configItinerary(r)" style="border-radius: 8px; background: linear-gradient(135deg, #1CB89D, #0D9488); border: none;">
                <i class="fa fa-map-marker mr-1"></i>配置行程
              </el-button>
            </div>
          </div>
          <div v-if="routeTotal > routeSize" class="pager">
            <span class="pager-text">共 <b>{{ routeTotal }}</b> 条，第 {{ routePage }}/{{ routeTotalPages }} 页</span>
            <el-pagination
              v-model:current-page="routePage" v-model:page-size="routeSize"
              :page-sizes="[8, 16, 32]" :total="routeTotal" :pager-count="5"
              layout="sizes, prev, pager, next, jumper" background
              @size-change="setRouteSize" @current-change="setRoutePage"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/user/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import { formatTime, truncate, priceText, trafficStyle, normalizeTraffic, auditText, auditIcon, auditStyle } from '@/utils/format'
import { getStrategiesByPage } from '@/api/strategy'
import { getRoutesByPage } from '@/api/route'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('guide')

// 后端 queryByPage 不支持按用户过滤，取全量后前端按 userId 过滤 + 本地分页（同 MyOrders 方案）
const guideList = ref([])
const guideTotal = ref(0)
const guideTotalPages = ref(0)
const guidePage = ref(1)
const guideSize = ref(8)
const guideLoading = ref(false)
const guideDegraded = ref(false)

const routeList = ref([])
const routeTotal = ref(0)
const routeTotalPages = ref(0)
const routePage = ref(1)
const routeSize = ref(8)
const routeLoading = ref(false)
const routeDegraded = ref(false)
const routeLoaded = ref(false)

function uid() {
  return authStore.userInfo?.id
}

async function fetchGuides() {
  const id = uid()
  if (!id) {
    guideList.value = []
    guideTotal.value = 0
    guideTotalPages.value = 0
    return
  }
  guideLoading.value = true
  try {
    const res = await getStrategiesByPage({ page: guidePage.value, size: guideSize.value, mine: true })
    if (res.code === 1) {
      guideList.value = res.content?.content || []
      guideTotal.value = res.content?.totalElements ?? guideList.value.length
      guideTotalPages.value = res.content?.totalPages ?? Math.ceil(guideTotal.value / guideSize.value)
      guideDegraded.value = false
    }
  } catch (err) {
    guideDegraded.value = true
  } finally {
    guideLoading.value = false
  }
}

async function fetchRoutes() {
  const id = uid()
  if (!id) {
    routeList.value = []
    routeTotal.value = 0
    routeTotalPages.value = 0
    return
  }
  routeLoading.value = true
  try {
    const res = await getRoutesByPage({ page: routePage.value, size: routeSize.value, mine: true })
    if (res.code === 1) {
      routeList.value = res.content?.content || []
      routeTotal.value = res.content?.totalElements ?? routeList.value.length
      routeTotalPages.value = res.content?.totalPages ?? Math.ceil(routeTotal.value / routeSize.value)
      routeDegraded.value = false
      routeLoaded.value = true
    }
  } catch (err) {
    routeDegraded.value = true
  } finally {
    routeLoading.value = false
  }
}

function setGuidePage(p) { guidePage.value = p; fetchGuides() }
function setGuideSize(s) { guideSize.value = s; guidePage.value = 1; fetchGuides() }
function setRoutePage(p) { routePage.value = p; fetchRoutes() }
function setRouteSize(s) { routeSize.value = s; routePage.value = 1; fetchRoutes() }

function viewGuide(g) {
  router.push({ path: `/travel/home/guides/${g.id}`, state: { guide: g } })
}

function viewRoute(r) {
  router.push({ path: `/travel/home/routes/${r.id}`, state: { route: r } })
}

function configItinerary(r) {
  router.push({ path: '/travel/home/mine/publish/itinerary', query: { routeId: r.id } })
}

watch(activeTab, (tab) => {
  if (tab === 'route' && !routeLoaded.value) fetchRoutes()
})

onMounted(() => {
  fetchGuides()
})
</script>

<style scoped>
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: #94A3B8;
  margin: 0 0 16px;
}

.pub-tabs { margin-top: 4px; }

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

.pub-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pub-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #F8FAFC;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 12px 16px;
}

.pub-item.route { align-items: flex-start; flex-direction: column; gap: 10px; }

.pub-cover {
  width: 100px;
  height: 64px;
  border-radius: 10px;
  flex-shrink: 0;
  cursor: pointer;
  overflow: hidden;
}

.pub-cover-img { width: 100%; height: 100%; }

.pub-cover-fallback {
  width: 100%; height: 100%;
  background: #E2E8F0;
  display: flex; align-items: center; justify-content: center;
  color: #94A3B8;
  font-size: 18px;
}

.pub-body {
  flex: 1;
  min-width: 0;
}

.pub-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  min-width: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid transparent;
  flex-shrink: 0;
}

.pub-title {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pub-title:hover { color: #0D9488; }

.pub-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #94A3B8;
  margin: 0;
}

.pub-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.route-head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.route-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  flex-wrap: wrap;
}

.route-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.route-price {
  font-size: 14px;
  font-weight: 700;
  color: #059669;
  margin-left: auto;
}

.route-intro {
  font-size: 13px;
  color: #64748B;
  line-height: 1.6;
  margin: 0;
}

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

@media (max-width: 600px) {
  .pub-item {
    flex-wrap: wrap;
    align-items: flex-start;
  }
  .pub-cover {
    width: 80px;
    height: 52px;
  }
  .pub-body {
    flex: 1 1 calc(100% - 96px);
  }
  .pub-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
