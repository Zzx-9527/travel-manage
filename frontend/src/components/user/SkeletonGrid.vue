<template>
  <div class="skeleton-grid" :style="{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }">
    <div v-for="i in count" :key="i" class="skeleton-card">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="image" class="sk-img" :style="{ height: imageHeight + 'px' }" />
          <div class="sk-body" :style="{ height: bodyHeight + 'px' }">
            <el-skeleton-item variant="h3" style="width: 55%" />
            <el-skeleton-item variant="text" style="width: 90%; margin-top: 12px" />
            <el-skeleton-item variant="text" style="width: 42%; margin-top: 10px" />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
// 列表页统一骨架屏：卡片网格形状与 SpotCard/GuideCard 对齐，
// 加载中不再显示裸转圈图标
defineProps({
  cols: { type: Number, default: 3 },
  count: { type: Number, default: 6 },
  imageHeight: { type: Number, default: 160 },
  bodyHeight: { type: Number, default: 96 }
})
</script>

<style scoped>
.skeleton-grid {
  display: grid;
  gap: 20px;
}

.skeleton-card {
  background: #fff;
  border-radius: var(--radius-card);
  border: 1px solid var(--ink-100);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.sk-img {
  width: 100%;
  display: block;
  border-radius: 0;
}

.sk-body {
  padding: 16px;
  box-sizing: border-box;
}
</style>
