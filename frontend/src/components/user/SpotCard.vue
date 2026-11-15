<template>
  <div
    class="spot-card"
    role="link"
    tabindex="0"
    :aria-label="`查看景点：${spot.name || '未命名景点'}`"
    @click="$emit('click', spot)"
    @keydown.enter.prevent="$emit('click', spot)"
    @keydown.space.prevent="$emit('click', spot)"
  >
    <div class="spot-cover">
      <el-image :src="resolveUploadUrl(spot.cover)" fit="cover" lazy style="width: 100%; height: 100%;">
        <template #error><div class="placeholder-img"><i class="fa fa-image"></i></div></template>
      </el-image>
      <div class="spot-price">{{ priceText(spot.price) }}</div>
      <div class="spot-score"><i class="fa fa-star"></i> {{ (spot.score || 0).toFixed(1) }}</div>
    </div>
    <div class="spot-info">
      <h3 class="spot-name">{{ spot.name }}</h3>
      <p class="spot-addr"><i class="fa fa-map-marker mr-1"></i>{{ spot.address || '暂无地址' }}</p>
    </div>
  </div>
</template>

<script setup>
import { priceText } from '@/utils/format'
import { resolveUploadUrl } from '@/api/file'

defineProps({
  spot: { type: Object, required: true }
})
defineEmits(['click'])
</script>

<style scoped>
.spot-card {
  background: #fff;
  border-radius: var(--radius-card);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--ink-100);
}

.spot-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--ink-200);
}

.spot-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.spot-cover :deep(.el-image__inner) {
  transition: transform 0.45s ease;
}

.spot-card:hover .spot-cover :deep(.el-image__inner) {
  transform: scale(1.06);
}

.spot-price {
  position: absolute;
  top: 12px; left: 12px;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
  color: #fff;
}

.spot-score {
  position: absolute;
  top: 12px; right: 12px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
  color: #F59E0B;
}

.spot-score i { margin-right: 2px; }

.spot-info {
  padding: 16px;
}

.spot-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-800);
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spot-addr {
  font-size: 13px;
  color: var(--ink-400);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.placeholder-img {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, var(--brand-50), var(--ink-100));
  display: flex; align-items: center; justify-content: center;
  color: var(--ink-300);
  font-size: 24px;
}
</style>
