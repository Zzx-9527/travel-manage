<template>
  <div class="empty-state">
    <img :src="emptyScene" class="empty-img" alt="" />
    <p class="empty-title">{{ title }}</p>
    <p v-if="desc" class="empty-desc">{{ desc }}</p>
    <slot />
  </div>
</template>

<script setup>
import emptyScene from '@/assets/images/empty-scene.svg'

defineProps({
  title: { type: String, default: '暂无内容' },
  desc: { type: String, default: '' }
})
</script>

<style scoped>
.empty-state {
  padding: 40px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-img {
  width: 200px;
  height: auto;
  margin-bottom: 14px;
  user-select: none;
  pointer-events: none;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-600);
  margin: 0 0 6px;
}

.empty-desc {
  font-size: 13px;
  color: var(--ink-400);
  margin: 0 0 12px;
  text-align: center;
}
</style>
