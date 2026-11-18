<template>
  <div
    class="route-card"
    role="link"
    tabindex="0"
    :aria-label="`查看路线：${route.name || '精彩路线'}`"
    @click="$emit('click', route)"
    @keydown.enter.prevent="$emit('click', route)"
    @keydown.space.prevent="$emit('click', route)"
  >
    <!-- 渐变头图区：底色固定品牌青，交通方式只换图标块和胶囊的底色 -->
    <div class="route-banner">
      <div class="banner-icon" :style="{ background: chip, borderColor: chip }">
        <i class="fa" :class="trafficIcon(traffic)"></i>
      </div>
      <div class="banner-info">
        <span class="banner-route">{{ route.name || '精彩路线' }}</span>
        <span class="banner-days">{{ route.dayCount || 1 }} 天行程</span>
      </div>
      <span class="banner-traffic" :style="{ background: chip, borderColor: chip }">
        {{ traffic }}
      </span>
      <div class="banner-deco"></div>
    </div>

    <div class="route-body">
      <p class="route-intro">{{ truncate(route.intro, 60) || '暂无简介' }}</p>
      <div class="route-meta">
        <span class="route-city"><i class="fa fa-map-marker mr-1"></i>{{ route.startCity || '--' }} 出发</span>
        <span class="route-budget">{{ priceText(route.budget) }}</span>
      </div>
      <div class="route-author">
        <el-avatar :size="22" :src="route.userId?.avatar" style="flex-shrink: 0;">
          <i class="fa fa-user text-xs"></i>
        </el-avatar>
        <span>{{ route.userId?.nickname || '未知' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { truncate, priceText, trafficIcon, trafficColor, normalizeTraffic } from '@/utils/format'

const props = defineProps({
  route: { type: Object, required: true }
})
defineEmits(['click'])

// 旧数据里的自由行/火车等非法值统一归入「其他」
const traffic = computed(() => normalizeTraffic(props.route?.traffic))
const chip = computed(() => trafficColor(traffic.value))
</script>

<style scoped>
.route-card {
  background: #fff;
  border-radius: var(--radius-card);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--ink-100);
  display: flex;
  flex-direction: column;
}

.route-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--ink-200);
}

/* ===== 头图区 ===== */
.route-banner {
  position: relative;
  height: 104px;
  padding: 18px 18px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
  background: var(--brand-gradient);
}

.banner-deco {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 88% -20%, rgba(255, 255, 255, 0.22) 0%, transparent 46%),
    radial-gradient(circle at 70% 130%, rgba(255, 255, 255, 0.14) 0%, transparent 40%);
  pointer-events: none;
}

.banner-icon {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.banner-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
  z-index: 1;
}

.banner-route {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
}

.banner-days {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
}

.banner-traffic {
  align-self: flex-start;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

/* ===== 信息区 ===== */
.route-body {
  padding: 16px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.route-intro {
  font-size: 13px;
  color: var(--ink-500);
  line-height: 1.6;
  margin: 0;
  flex: 1;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.route-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.route-city {
  font-size: 12px;
  color: var(--ink-400);
}

.route-budget {
  font-size: 15px;
  font-weight: 700;
  color: #059669;
}

.route-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-400);
  border-top: 1px solid var(--ink-100);
  padding-top: 10px;
  overflow: hidden;
  white-space: nowrap;
}

.route-author span {
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
