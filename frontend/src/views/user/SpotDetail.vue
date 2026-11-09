<template>
  <div class="page">
    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center py-20">
      <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
    </div>

    <!-- 降级空态 -->
    <EmptyState v-else-if="degraded || !spot" title="景点详情暂不可用" desc="站内进入即可查看，或稍后刷新重试" />

    <template v-else>
      <!-- 返回 -->
      <router-link to="/travel/home/spots" class="back-link">
        <i class="fa fa-arrow-left mr-1"></i>返回景点列表
      </router-link>

      <!-- 影音大图 Hero -->
      <section class="hero" :style="heroStyle">
        <div class="hero-shade"></div>
        <div class="hero-inner">
          <div class="hero-badge"><i class="fa fa-compass mr-1"></i>自然目的地</div>
          <h1 class="hero-name">{{ spot.name }}</h1>
          <p class="hero-category">
            <span class="cat-tag">{{ spot.categoryId?.name || '未分类' }}</span>
            <span class="hero-score"><i class="fa fa-star"></i> {{ (spot.score || 0).toFixed(1) }}</span>
          </p>
          <p class="hero-price">{{ priceText(spot.price) }}</p>
        </div>
      </section>

      <!-- 快速信息 -->
      <section class="facts">
        <div class="fact">
          <i class="fa fa-clock-o"></i>
          <div><p class="fact-label">开放时间</p><p class="fact-value">{{ spot.openTime || '--' }}</p></div>
        </div>
        <div class="fact">
          <i class="fa fa-map-marker"></i>
          <div><p class="fact-label">地址</p><p class="fact-value">{{ spot.address || '--' }}</p></div>
        </div>
        <div class="fact">
          <i class="fa fa-eye"></i>
          <div><p class="fact-label">浏览量</p><p class="fact-value">{{ spot.viewCount ?? 0 }}</p></div>
        </div>
        <div class="fact">
          <i class="fa fa-calendar"></i>
          <div><p class="fact-label">收录时间</p><p class="fact-value">{{ formatTime(spot.createTime) }}</p></div>
        </div>
      </section>

      <!-- 位置地图 -->
      <section class="map-card">
        <h2 class="sec-title"><i class="fa fa-map-marker mr-2"></i>位置地图</h2>
        <p class="map-addr"><i class="fa fa-map-pin mr-1"></i>{{ spot.address || '暂无地址' }}</p>
        <ScenicMap :spots="[spot]" :height="420" :auto-open="true" :show-view-btn="false" />
      </section>

      <!-- 主体内容 -->
      <div class="body-grid">
        <!-- 图片墙 -->
        <section class="gallery card">
          <h2 class="sec-title"><i class="fa fa-image mr-2"></i>影像集</h2>
          <el-carousel v-if="imageList.length" :interval="4000" height="340px" arrow="always" indicator-position="outside" class="gallery-carousel">
            <el-carousel-item v-for="(img, idx) in imageList" :key="idx">
              <el-image :src="img" fit="cover" style="height: 100%; width: 100%; border-radius: 14px;" :preview-src-list="imageList" preview-teleported @click.stop>
                <template #error>
                  <div class="img-error"><i class="fa fa-image"></i> 图片加载失败</div>
                </template>
              </el-image>
            </el-carousel-item>
          </el-carousel>
          <div v-else class="img-empty"><i class="fa fa-image"></i> 暂无图片</div>
        </section>

        <!-- 简介 -->
        <section class="intro card">
          <h2 class="sec-title"><i class="fa fa-leaf mr-2"></i>景点简介</h2>
          <p class="intro-text">{{ spot.intro || '暂无简介' }}</p>
          <div class="action-row">
            <button class="collect-btn" :class="{ collected: isCollected }" @click="toggleCollect">
              <i class="fa mr-2" :class="isCollected ? 'fa-heart' : 'fa-heart-o'"></i>
              {{ isCollected ? '已收藏' : '收藏' }}
            </button>
            <button class="buy-btn" @click="orderVisible = true">
              <i class="fa fa-ticket mr-2"></i>在线购票
            </button>
          </div>
        </section>
      </div>
    </template>

    <OrderDialog v-model="orderVisible" :spot="spot" @order-created="onOrderCreated" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/user/EmptyState.vue'
import OrderDialog from '@/components/user/OrderDialog.vue'
import ScenicMap from '@/components/user/ScenicMap.vue'
import { formatTime, priceText } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { getSpotDetail } from '@/api/scenic'
import { checkCollect, addCollect, deleteCollectByTarget } from '@/api/collect'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const spot = ref(null)
const loading = ref(true)
const degraded = ref(false)
const orderVisible = ref(false)

// 收藏（targetType=2 景点）
const isCollected = ref(false)
const collectBusy = ref(false)

const imageList = computed(() => {
  if (!spot.value) return []
  const imgs = spot.value.images
    ? String(spot.value.images).split(',').map(s => s.trim()).filter(Boolean)
    : []
  return imgs.length ? imgs : (spot.value.cover ? [spot.value.cover] : [])
})

const heroStyle = computed(() => {
  if (!imageList.value.length) return {}
  return { backgroundImage: `linear-gradient(180deg, rgba(6,32,28,0.15) 0%, rgba(6,32,28,0.55) 100%), url(${imageList.value[0]})` }
})

async function resolveSpot() {
  // 1) 优先使用站内跳转携带的整对象
  if (route.params.id && history.state?.spot?.id === Number(route.params.id)) {
    spot.value = history.state.spot
    loading.value = false
    checkFavorite()
    return
  }
  // 2) 其次请求详情接口
  try {
    const res = await getSpotDetail(route.params.id)
    if (res.code === 1) {
      spot.value = res.content
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
  if (!uid || !spot.value?.id) { isCollected.value = false; return }
  try {
    const res = await checkCollect({ targetType: 2, targetId: spot.value.id })
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
      ? await deleteCollectByTarget({ targetType: 2, targetId: spot.value.id })
      : await addCollect({ targetType: 2, targetId: spot.value.id })
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

function onOrderCreated(order) {
  ElMessage.success(`订单 ${order?.orderNo || ''} 已生成`)
  router.push('/travel/home/mine/orders')
}

onMounted(resolveSpot)
</script>

<style scoped>
.page {
  max-width: 1120px;
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

.back-link:hover { color: #0D9488; }

/* ===== Hero ===== */
.hero {
  position: relative;
  height: 320px;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(135deg, #052E2B 0%, #0D5C4F 60%, #147D6E 100%);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-end;
  box-shadow: 0 18px 48px rgba(5, 60, 50, 0.28);
  margin-bottom: 20px;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 30%, rgba(4, 28, 25, 0.7) 100%);
}

.hero-inner {
  position: relative;
  padding: 30px 34px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.28);
  color: #E7FFF8;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

.hero-name {
  font-size: 36px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 10px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
}

.hero-category {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 8px;
}

.cat-tag {
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.hero-score {
  font-size: 13px;
  color: #FFE9A8;
  font-weight: 600;
}
.hero-score i { color: #FFD75E; margin-right: 4px; }

.hero-price {
  font-size: 22px;
  font-weight: 800;
  color: #7CF5CE;
  margin: 0;
}

/* ===== 快速信息 ===== */
.facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.fact {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #E3F2EE;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 4px 14px rgba(13, 148, 136, 0.06);
}

.fact > i {
  width: 40px; height: 40px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: #E7F6F1;
  color: #0D9488;
  font-size: 16px;
  flex-shrink: 0;
}

.fact-label {
  font-size: 12px;
  color: #94A3B8;
  margin: 0 0 2px;
}

.fact-value {
  font-size: 13px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
  word-break: break-all;
}

/* ===== 位置地图 ===== */
.map-card {
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 18px;
  padding: 22px 24px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.04);
}

.map-addr {
  font-size: 13px;
  color: #64748B;
  margin: 0 0 14px;
}

/* ===== 主体 ===== */
.body-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
}

.card {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 18px;
  padding: 22px 24px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.04);
}

.sec-title {
  font-size: 16px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 16px;
}
.sec-title i { color: #0D9488; }

.gallery-carousel :deep(.el-carousel__indicators--outside button) {
  background-color: #CBE7DF;
}
.gallery-carousel :deep(.el-carousel__indicators--outside .is-active button) {
  background-color: #0D9488;
}

.img-error,
.img-empty {
  height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #E9F5F1;
  border-radius: 14px;
  color: #7BB5A9;
  font-size: 15px;
}

.intro-text {
  font-size: 14px;
  color: #475569;
  line-height: 1.9;
  margin: 0 0 24px;
  white-space: pre-wrap;
}

.action-row {
  display: flex;
  gap: 12px;
}

.collect-btn {
  flex-shrink: 0;
  width: 128px;
  height: 52px;
  border-radius: 14px;
  border: 1px solid #FECACA;
  background: #FFF1F2;
  color: #E11D48;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.collect-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(225, 29, 72, 0.15);
}

.collect-btn.collected {
  background: linear-gradient(135deg, #FB7185, #E11D48);
  border: none;
  color: #fff;
  box-shadow: 0 4px 15px rgba(225, 29, 72, 0.28);
}

.buy-btn {
  flex: 1;
  height: 50px;
  border-radius: 13px;
  border: none;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #059669, #0D9488);
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 6px 18px rgba(5, 150, 105, 0.3);
}

.buy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(5, 150, 105, 0.4);
}

@media (max-width: 860px) {
  .body-grid { grid-template-columns: 1fr; }
  .facts { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 520px) {
  .facts { grid-template-columns: 1fr; }
  .hero { height: 260px; }
  .hero-name { font-size: 28px; }
}
</style>
