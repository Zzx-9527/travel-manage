<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">旅游路线</h2>
      <p class="page-desc">精选路线规划，说走就走</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-card">
      <div class="search-item">
        <i class="fa fa-search"></i>
        <el-input v-model="keyword" placeholder="输入路线名称" clearable style="width: 240px;" @keyup.enter="handleSearch" />
      </div>
      <el-button type="primary" @click="handleSearch">
        <i class="fa fa-search mr-1"></i>查询
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 列表 -->
    <div v-if="degraded" class="list-card">
      <EmptyState title="路线列表加载失败" desc="请稍后重试，或联系管理员确认服务状态" />
    </div>
    <div v-else>
      <div class="list-card">
        <SkeletonGrid v-if="loading && !list.length" :cols="3" :count="6" :image-height="104" :body-height="150" />
        <template v-else>
          <div v-if="list.length" v-loading="loading" class="routes-grid">
            <RouteCard v-for="r in list" :key="r.id" :route="r" @click="viewRoute" />
          </div>
          <EmptyState v-else title="暂无符合条件的路线" desc="换个关键词试试" />
        </template>
      </div>

      <div v-if="list.length" class="pager">
        <span class="pager-text">共 <b>{{ total }}</b> 条记录，第 {{ page }}/{{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :page-sizes="[12, 24, 48]" :total="total" :pager-count="5"
          layout="sizes, prev, pager, next, jumper" background
          @size-change="setSize" @current-change="setPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RouteCard from '@/components/user/RouteCard.vue'
import EmptyState from '@/components/user/EmptyState.vue'
import SkeletonGrid from '@/components/user/SkeletonGrid.vue'
import { usePageList } from '@/composables/usePageList'
import { getRoutesByPage } from '@/api/route'

const route = useRoute()
const router = useRouter()

const { list, total, totalPages, page, size, loading, degraded, load, setPage, setSize } = usePageList(getRoutesByPage, { size: 12 })

const keyword = ref('')

async function fetchList() {
  const params = { status: 1 }
  if (keyword.value) params.name = keyword.value
  await load(params)
}

function handleSearch() {
  page.value = 1
  fetchList()
}

function handleReset() {
  keyword.value = ''
  page.value = 1
  fetchList()
}

function viewRoute(r) {
  router.push({ path: `/travel/home/routes/${r.id}`, state: { route: r } })
}

onMounted(() => {
  if (route.query.keyword) keyword.value = String(route.query.keyword)
  fetchList()
})

// 组件被复用（已在当前页时再次搜索）时，query 变化不会重新触发 onMounted，这里监听刷新
watch(() => route.query.keyword, (val) => {
  keyword.value = val ? String(val) : ''
  page.value = 1
  fetchList()
})
</script>

<style scoped>
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  min-height: 60vh;
}

.page-head {
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #0D1B2A;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 14px;
  color: #94A3B8;
  margin: 0;
}

.search-card {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 14px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  margin-bottom: 20px;
}

.search-item {
  position: relative;
  display: flex;
  align-items: center;
}

.search-item i {
  position: absolute;
  left: 12px;
  color: #CBD5E1;
  font-size: 14px;
  z-index: 2;
}

.search-item :deep(.el-input__wrapper) {
  padding-left: 32px;
}

.list-card {
  background: #fff;
  border: 1px solid #F1F5F9;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  min-height: 200px;
}

.routes-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
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

@media (max-width: 1000px) {
  .routes-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .routes-grid { grid-template-columns: 1fr; }
}
</style>
