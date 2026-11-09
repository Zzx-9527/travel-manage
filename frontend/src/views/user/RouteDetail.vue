<template>
  <div class="page">
    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #4F46E5;"></i>
    </div>

    <!-- 降级空态 -->
    <EmptyState v-else-if="degraded || !routeData" title="路线详情暂不可用" desc="站内进入即可查看，或稍后刷新重试" />

    <template v-else>
      <!-- 返回 -->
      <router-link to="/travel/home/routes" class="back-link">
        <i class="fa fa-arrow-left mr-1"></i>返回路线列表
      </router-link>

      <!-- 行程概要 -->
      <section class="summary-card">
        <div class="summary-banner">
          <div class="banner-shade"></div>
          <div class="banner-top">
            <span class="days-badge"><i class="fa fa-clock-o mr-1"></i>{{ routeData.dayCount }} 天</span>
            <span class="traffic-tag" :style="trafficStyle(routeData.traffic)">{{ routeData.traffic || '其他' }}</span>
            <span class="approved-tag"><i class="fa fa-check-circle mr-1"></i>已通过</span>
            <button class="route-collect" :class="{ collected: isCollected }" @click="toggleCollect">
              <i class="fa mr-1" :class="isCollected ? 'fa-heart' : 'fa-heart-o'"></i>
              {{ isCollected ? '已收藏' : '收藏' }}
            </button>
          </div>
          <h1 class="name">{{ routeData.name }}</h1>
          <p class="intro">{{ routeData.intro || '暂无简介' }}</p>
        </div>

        <div class="summary-meta">
          <div class="meta-item">
            <i class="fa fa-map-marker meta-icon"></i>
            <div><p class="meta-label">出发城市</p><p class="meta-value">{{ routeData.startCity || '--' }}</p></div>
          </div>
          <div class="meta-item">
            <i class="fa fa-money meta-icon"></i>
            <div><p class="meta-label">预算</p><p class="meta-value price">{{ priceText(routeData.budget) }}</p></div>
          </div>
          <div class="meta-item">
            <i class="fa fa-user meta-icon"></i>
            <div><p class="meta-label">发布者</p><p class="meta-value">{{ routeData.userId?.nickname || '未知' }}</p></div>
          </div>
          <div class="meta-item">
            <i class="fa fa-calendar meta-icon"></i>
            <div><p class="meta-label">发布时间</p><p class="meta-value">{{ formatTime(routeData.createTime) }}</p></div>
          </div>
        </div>
      </section>

      <!-- 按天行程 -->
      <section class="itinerary-card">
        <div class="itinerary-title-wrap">
          <div class="title-icon"><i class="fa fa-map-o"></i></div>
          <div>
            <h3 class="itinerary-title">行程安排</h3>
            <p class="itinerary-desc">一路向西，逐站打卡</p>
          </div>
        </div>

        <div v-if="itineraryLoading" class="flex justify-center py-10">
          <i class="fa fa-spinner fa-spin text-2xl" style="color: #4F46E5;"></i>
        </div>
        <EmptyState v-else-if="itineraryDegraded" title="路线行程暂未收录" desc="行程数据待后端完善" />
        <el-empty v-else-if="!dayGroups.length" description="该路线暂无行程安排" :image-size="90" />
        <div v-else class="journey">
          <div v-for="(group, gidx) in dayGroups" :key="group.day" class="day-block">
            <div class="day-head">
              <span class="day-num">Day {{ group.day }}</span>
              <span class="day-desc">第 {{ group.day }} 天</span>
            </div>
            <div class="day-items">
              <div v-for="(item, idx) in group.items" :key="item.id" class="day-item">
                <div class="item-dot" :class="{ first: idx === 0 }">
                  <i class="fa" :class="idx === 0 ? 'fa-flag-checkered' : 'fa-map-marker'"></i>
                </div>
                <div class="item-content">
                  <span class="item-index">{{ gidx * 100 + idx + 1 }}</span>
                  <div class="item-main">
                    <p class="item-name">{{ item.scenicId?.name || '未知景点' }}</p>
                    <p class="item-stay"><i class="fa fa-clock-o mr-1"></i>停留 {{ item.stayTime || '未标注' }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import { formatTime, priceText, trafficStyle } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { getRouteDetail } from '@/api/route'
import { getRouteDetailsByPage } from '@/api/routeDetail'
import { checkCollect, addCollect, deleteCollectByTarget } from '@/api/collect'

const route = useRoute()
const authStore = useAuthStore()

const routeData = ref(null)
const loading = ref(true)
const degraded = ref(false)

// 收藏（targetType=3 路线）
const isCollected = ref(false)
const collectBusy = ref(false)

// 行程
const itinerary = ref([])
const itineraryLoading = ref(true)
const itineraryDegraded = ref(false)

const dayGroups = computed(() => {
  const map = {}
  for (const d of itinerary.value) {
    const day = d.dayNo || 1
    if (!map[day]) map[day] = []
    map[day].push(d)
  }
  return Object.keys(map)
    .sort((a, b) => Number(a) - Number(b))
    .map(day => ({
      day: Number(day),
      items: map[day].sort((a, b) => (a.sort || 0) - (b.sort || 0))
    }))
})

async function resolveRoute() {
  // 站内跳转已带完整路线对象（含 dayCount）可直接用；对象不完整（如收藏列表内嵌的薄对象）则重新拉取详情
  const st = history.state?.route
  if (route.params.id && st?.id === Number(route.params.id) && st?.dayCount != null) {
    routeData.value = st
    loading.value = false
    loadItinerary()
    checkFavorite()
    return
  }
  try {
    const res = await getRouteDetail(route.params.id)
    if (res.code === 1) {
      routeData.value = res.content
      loadItinerary()
      checkFavorite()
    } else {
      degraded.value = true
    }
  } catch (err) {
    degraded.value = true
  } finally {
    loading.value = false
  }
}

async function checkFavorite() {
  const uid = authStore.userInfo?.id
  if (!uid || !routeData.value?.id) { isCollected.value = false; return }
  try {
    const res = await checkCollect({ targetType: 3, targetId: routeData.value.id })
    if (res.code === 1) isCollected.value = !!res.content
  } catch (err) {
    isCollected.value = false
  }
}

async function toggleCollect() {
  const uid = authStore.userInfo?.id
  if (!uid) { ElMessage.warning('请先登录'); return }
  if (collectBusy.value) return
  collectBusy.value = true
  try {
    const res = isCollected.value
      ? await deleteCollectByTarget({ targetType: 3, targetId: routeData.value.id })
      : await addCollect({ targetType: 3, targetId: routeData.value.id })
    if (res.code === 1) {
      isCollected.value = !isCollected.value
      ElMessage.success(isCollected.value ? '收藏成功' : '已取消收藏')
    }
  } catch (err) {
    // 失败时由拦截器统一提示
  } finally {
    collectBusy.value = false
  }
}

// routeDetail/queryByPage 已支持按 routeId 过滤，直接后端过滤
async function loadItinerary() {
  itineraryLoading.value = true
  try {
    const res = await getRouteDetailsByPage({ page: 1, size: 100, routeId: routeData.value.id })
    if (res.code === 1) {
      itinerary.value = res.content?.content || res.content || []
    }
  } catch (err) {
    itineraryDegraded.value = true
  } finally {
    itineraryLoading.value = false
  }
}

onMounted(resolveRoute)
</script>

<style scoped>
.page {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px 24px 56px;
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

.back-link:hover { color: #4338CA; }

/* ===== 概要 ===== */
.summary-card {
  background: #fff;
  border: 1px solid #E5E7F5;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(67, 56, 202, 0.10);
  margin-bottom: 20px;
}

.summary-banner {
  position: relative;
  padding: 26px 30px 24px;
  background: linear-gradient(135deg, #1E1B4B 0%, #3730A3 55%, #4F46E5 100%);
}

.banner-shade {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 15% 30%, rgba(99, 102, 241, 0.25) 0%, transparent 55%),
    radial-gradient(ellipse at 90% 80%, rgba(129, 140, 248, 0.18) 0%, transparent 50%);
}

.banner-top {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.days-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  backdrop-filter: blur(4px);
}

.traffic-tag {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.approved-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(74, 222, 128, 0.18);
  color: #86EFAC;
  border: 1px solid rgba(74, 222, 128, 0.35);
}

.route-collect {
  margin-left: auto;
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid #FECACA;
  background: #FFF1F2;
  color: #E11D48;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}

.route-collect:hover { transform: translateY(-1px); }

.route-collect.collected {
  background: linear-gradient(135deg, #FB7185, #E11D48);
  border: none;
  color: #fff;
  box-shadow: 0 4px 15px rgba(225, 29, 72, 0.28);
}

.name {
  position: relative;
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 10px;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

.intro {
  position: relative;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
  max-width: 720px;
}

.summary-meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  padding: 20px 28px;
}

.meta-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.meta-icon {
  font-size: 15px;
  color: #4F46E5;
  margin-top: 2px;
}

.meta-label {
  font-size: 12px;
  color: #94A3B8;
  margin: 0 0 2px;
}

.meta-value {
  font-size: 14px;
  font-weight: 500;
  color: #1E293B;
  margin: 0;
  word-break: break-all;
}

.meta-value.price {
  color: #4F46E5;
  font-weight: 700;
}

/* ===== 行程 ===== */
.itinerary-card {
  background: #fff;
  border: 1px solid #E5E7F5;
  border-radius: 20px;
  padding: 26px 30px;
  box-shadow: 0 10px 30px rgba(67, 56, 202, 0.10);
}

.itinerary-title-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.title-icon {
  width: 44px; height: 44px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #4F46E5, #818CF8);
  color: #fff;
  font-size: 18px;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
}

.itinerary-title {
  font-size: 17px;
  font-weight: 700;
  color: #1E1B4B;
  margin: 0;
}

.itinerary-desc {
  font-size: 12px;
  color: #94A3B8;
  margin: 2px 0 0;
}

.journey {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.day-block {
  display: flex;
  gap: 22px;
}

.day-head {
  width: 92px;
  flex-shrink: 0;
  padding-top: 10px;
}

.day-num {
  display: inline-block;
  font-size: 14px;
  font-weight: 800;
  color: #4F46E5;
  background: #EEF2FF;
  border: 1px solid #C7D2FE;
  border-radius: 10px;
  padding: 8px 12px;
  margin-bottom: 4px;
}

.day-desc {
  display: block;
  font-size: 12px;
  color: #94A3B8;
  text-align: center;
}

.day-items {
  flex: 1;
  position: relative;
  padding-left: 26px;
}

.day-items::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 3px;
  background: linear-gradient(180deg, #4F46E5, #A5B4FC, #E0E7FF);
}

.day-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 12px 0;
}

.item-dot {
  position: absolute;
  left: -27px;
  top: 18px;
  width: 15px; height: 15px;
  border-radius: 50%;
  background: #fff;
  border: 4px solid #A5B4FC;
  display: flex; align-items: center; justify-content: center;
  z-index: 1;
}

.item-dot.first {
  border-color: #4F46E5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.15);
}

.item-dot i { display: none; }

.item-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  background: #F8FAFF;
  border: 1px solid #E5E7F5;
  border-radius: 14px;
  padding: 14px 18px;
  transition: all 0.2s;
}

.item-content:hover {
  border-color: #C7D2FE;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.08);
  transform: translateX(2px);
}

.item-index {
  width: 30px; height: 30px;
  border-radius: 9px;
  background: #EEF2FF;
  color: #4F46E5;
  font-size: 13px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.item-main { min-width: 0; }

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: #1E1B4B;
  margin: 0 0 5px;
}

.item-stay {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

@media (max-width: 640px) {
  .summary-meta { grid-template-columns: 1fr 1fr; }
  .day-block { flex-direction: column; gap: 8px; }
  .day-head { width: 100%; }
  .banner-top { flex-wrap: wrap; }
}
</style>
