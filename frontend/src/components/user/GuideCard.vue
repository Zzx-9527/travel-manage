<template>
  <div class="guide-card" @click="$emit('click', guide)">
    <div class="guide-cover">
      <el-image :src="guide.cover" fit="cover" lazy style="width: 100%; height: 100%;">
        <template #error><div class="placeholder-img"><i class="fa fa-image"></i></div></template>
      </el-image>
    </div>
    <div class="guide-body">
      <h3 class="guide-title">{{ guide.title }}</h3>
      <p class="guide-excerpt">{{ truncate(guide.content, 80) }}</p>
      <div class="guide-meta">
        <span><i class="fa fa-eye mr-1"></i>{{ guide.readCount || 0 }}</span>
        <span><i class="fa fa-heart mr-1"></i>{{ guide.collectCount || 0 }}</span>
        <span class="guide-author"><i class="fa fa-user mr-1"></i>{{ guide.userId?.nickname || '未知' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { truncate } from '@/utils/format'

defineProps({
  guide: { type: Object, required: true }
})
defineEmits(['click'])
</script>

<style scoped>
.guide-card {
  display: flex;
  background: #fff;
  border-radius: var(--radius-card);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--ink-100);
  /* 自适应高度：内容多时不截断（原固定 180px 有截断风险） */
  min-height: 176px;
}

.guide-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--ink-200);
}

.guide-cover {
  width: 220px;
  min-width: 220px;
  overflow: hidden;
}

.guide-cover :deep(.el-image__inner) {
  transition: transform 0.45s ease;
}

.guide-card:hover .guide-cover :deep(.el-image__inner) {
  transform: scale(1.06);
}

.guide-body {
  flex: 1;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.guide-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-800);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-excerpt {
  font-size: 13px;
  color: var(--ink-500);
  line-height: 1.6;
  margin: 0 0 auto;
  flex: 1;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.guide-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--ink-400);
  margin-top: 12px;
}

.guide-author {
  margin-left: auto;
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

@media (max-width: 900px) {
  .guide-card { flex-direction: column; }
  .guide-cover { width: 100%; min-width: 0; height: 180px; }
}
</style>
