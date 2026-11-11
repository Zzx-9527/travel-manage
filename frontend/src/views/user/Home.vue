<template>
  <div class="user-home">
    <!-- Hero 区域 -->
    <section class="hero-section">
      <!-- 背景层：内置风景插画 -->
      <div class="hero-bg">
        <img :src="heroScene" class="hero-scene" alt="" />
      </div>
      <div class="hero-overlay"></div>
      <div class="hero-bg-deco"></div>
      <div class="hero-content">
        <h1 class="hero-title">探索世界的每一个角落</h1>
        <p class="hero-subtitle">发现最美景点，规划精彩旅程，分享旅行故事</p>
        <div class="hero-search-tabs">
          <button
            v-for="t in searchTabs"
            :key="t.value"
            class="tab-btn"
            :class="{ active: searchType === t.value }"
            @click="searchType = t.value"
          >
            {{ t.label }}
          </button>
        </div>
        <div class="hero-search">
          <i class="fa fa-search search-icon"></i>
          <input v-model="searchText" class="search-input" placeholder="输入关键词，按回车搜索" aria-label="搜索关键词" @keyup.enter="doSearch" />
          <button class="search-btn" aria-label="搜索" @click="doSearch"><i class="fa fa-arrow-right"></i></button>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">{{ spotsTotal }}</span>
            <span class="stat-label">景点</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ guidesTotal }}</span>
            <span class="stat-label">攻略</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ categoriesTotal }}</span>
            <span class="stat-label">分类</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 热门景点区域 -->
    <section class="section">
      <div class="section-header">
        <div>
          <h2 class="section-title">热门景点</h2>
          <p class="section-desc">最受欢迎的目的地，等您来探索</p>
        </div>
        <router-link to="/travel/home/spots" class="section-more">
          查看全部 <i class="fa fa-arrow-right ml-1"></i>
        </router-link>
      </div>
      <SkeletonGrid v-if="spotsLoading" :cols="3" :count="6" :image-height="200" />
      <div v-else class="spots-grid">
        <SpotCard v-for="spot in spotList" :key="spot.id" :spot="spot" @click="viewSpot" />
      </div>
    </section>

    <!-- 分类浏览 -->
    <section class="section section-alt">
      <div class="section-header">
        <div>
          <h2 class="section-title">分类浏览</h2>
          <p class="section-desc">按类别找到您感兴趣的景点</p>
        </div>
      </div>
      <div class="categories-grid">
        <button
          v-for="cat in categoryList"
          :key="cat.id"
          type="button"
          class="category-card"
          :style="getCategoryStyle(cat.name)"
          @click="viewCategory(cat)"
        >
          <div class="cat-icon"><i :class="getCategoryIcon(cat.name)"></i></div>
          <span class="cat-name">{{ cat.name }}</span>
        </button>
      </div>
    </section>

    <!-- 最新攻略 -->
    <section class="section">
      <div class="section-header">
        <div>
          <h2 class="section-title">最新攻略</h2>
          <p class="section-desc">精选旅行攻略，分享真实体验</p>
        </div>
        <router-link to="/travel/home/guides" class="section-more">
          查看全部 <i class="fa fa-arrow-right ml-1"></i>
        </router-link>
      </div>
      <SkeletonGrid v-if="guidesLoading" :cols="2" :count="4" :image-height="150" :body-height="110" />
      <div v-else class="guides-grid">
        <GuideCard v-for="g in guideList" :key="g.id" :guide="g" @click="viewGuide" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SpotCard from '@/components/user/SpotCard.vue'
import GuideCard from '@/components/user/GuideCard.vue'
import SkeletonGrid from '@/components/user/SkeletonGrid.vue'
import heroScene from '@/assets/images/hero-scene.svg'
import { getSpotsByPage } from '@/api/scenic'
import { getCategoriesByPage } from '@/api/category'
import { getStrategiesByPage } from '@/api/strategy'

const router = useRouter()

const searchText = ref('')
const searchType = ref('spot')
const searchTabs = [
  { value: 'spot', label: '景点' },
  { value: 'guide', label: '攻略' },
  { value: 'route', label: '路线' }
]
const searchPaths = {
  spot: '/travel/home/spots',
  guide: '/travel/home/guides',
  route: '/travel/home/routes'
}

// 数据
const spotList = ref([])
const spotsTotal = ref(0)
const spotsLoading = ref(true)

const categoryList = ref([])
const categoriesTotal = ref(0)

const guideList = ref([])
const guidesTotal = ref(0)
const guidesLoading = ref(true)

function doSearch() {
  const path = searchPaths[searchType.value] || searchPaths.spot
  const kw = searchText.value.trim()
  router.push(kw ? { path, query: { keyword: kw } } : { path })
}

async function loadSpots() {
  try {
    const res = await getSpotsByPage({ page: 1, size: 6, status: 1 })
    if (res.code === 1) {
      spotList.value = res.content.content || []
      spotsTotal.value = res.content.totalElements || 0
    }
  } catch (e) { console.error(e)
  } finally { spotsLoading.value = false }
}

async function loadCategories() {
  try {
    const res = await getCategoriesByPage({ page: 1, size: 200 })
    if (res.code === 1) {
      categoryList.value = res.content.content || []
      categoriesTotal.value = res.content.totalElements || 0
    }
  } catch (e) { console.error(e) }
}

async function loadGuides() {
  try {
    const res = await getStrategiesByPage({ page: 1, size: 4, auditStatus: 1 })
    if (res.code === 1) {
      guideList.value = res.content.content || []
      guidesTotal.value = res.content.totalElements || 0
    }
  } catch (e) { console.error(e)
  } finally { guidesLoading.value = false }
}

function viewSpot(spot) {
  router.push({ path: `/travel/home/spots/${spot.id}`, state: { spot } })
}

function viewGuide(g) {
  router.push({ path: `/travel/home/guides/${g.id}`, state: { guide: g } })
}

function viewCategory(cat) {
  router.push({ path: '/travel/home/spots', query: { categoryId: cat.id, categoryName: cat.name } })
}

const categoryIcons = {
  '自然': 'fa-tree', '山川': 'fa-mountain', '河流': 'fa-tint',
  '湖泊': 'fa-tint', '森林': 'fa-tree', '历史': 'fa-university',
  '文化': 'fa-book', '古迹': 'fa-building', '城市': 'fa-building',
  '公园': 'fa-tree', '海滩': 'fa-sun-o', '岛屿': 'fa-ship'
}

function getCategoryIcon(name) {
  return categoryIcons[name] || 'fa-map-marker'
}

function getCategoryStyle(name) {
  const colors = [
    { bg: '#EBF8F5', border: '#B8E6D8', color: '#0D9488' },
    { bg: '#EFF6FF', border: '#BFDBFE', color: '#2563EB' },
    { bg: '#FFFBEB', border: '#FDE68A', color: '#D97706' },
    { bg: '#FEF2F2', border: '#FECACA', color: '#DC2626' },
    { bg: '#F5F3FF', border: '#DDD6FE', color: '#7C3AED' },
    { bg: '#F0FDF4', border: '#BBF7D0', color: '#16A34A' },
  ]
  // 分类名可能为空串/null，charCodeAt 会返回 NaN 导致取色失败、整页渲染崩溃
  const idx = (name?.charCodeAt(0) || 0) % colors.length
  const c = colors[idx]
  return { background: c.bg, borderColor: c.border, color: c.color }
}

onMounted(() => {
  loadSpots()
  loadCategories()
  loadGuides()
})
</script>

<style scoped>
.user-home {
  background: var(--ink-50);
}

/* ===== Hero 区域 ===== */
.hero-section {
  position: relative;
  padding: 96px 24px 64px;
  text-align: center;
  overflow: hidden;
  background: var(--ink-900);
}

.hero-bg {
  position: absolute;
  inset: 0;
}

.hero-scene {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center bottom;
  display: block;
}

/* 遮罩：保证白字在任意背景图上可读 */
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(13, 27, 42, 0.86) 0%, rgba(13, 27, 42, 0.66) 48%, rgba(13, 27, 42, 0.9) 100%);
}

.hero-bg-deco {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(28, 184, 157, 0.12) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 50%, rgba(59, 130, 246, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse at 50% 100%, rgba(28, 184, 157, 0.06) 0%, transparent 40%);
}

.hero-content {
  position: relative;
  max-width: 700px;
  margin: 0 auto;
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 12px;
  line-height: 1.2;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 24px rgba(0, 0, 0, 0.35);
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.68);
  margin: 0 0 36px;
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.3);
}

.hero-search-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin: 0 auto 14px;
}

.tab-btn {
  padding: 7px 22px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.65);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.tab-btn.active {
  background: var(--brand-gradient);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 15px rgba(28, 184, 157, 0.35);
}

.hero-search {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 4px;
  max-width: 520px;
  margin: 0 auto 40px;
  transition: all 0.3s;
}

.hero-search:focus-within {
  border-color: rgba(28, 184, 157, 0.5);
  background: rgba(255, 255, 255, 0.15);
}

.search-icon {
  color: rgba(255, 255, 255, 0.4);
  font-size: 16px;
  margin: 0 12px 0 16px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  padding: 14px 0;
  font-size: 15px;
  color: #fff;
}

.search-input::placeholder { color: rgba(255, 255, 255, 0.35); }

.search-btn {
  width: 44px; height: 44px;
  border-radius: 12px;
  background: var(--brand-gradient);
  border: none;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.search-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 15px rgba(28, 184, 157, 0.4);
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 48px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 6px;
}

/* ===== Sections ===== */
.section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 24px;
}

.section-alt {
  max-width: none;
  background: var(--ink-100);
  padding-left: calc((100% - 1200px) / 2 + 24px);
  padding-right: calc((100% - 1200px) / 2 + 24px);
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--ink-900);
  margin: 0 0 6px;
  position: relative;
  padding-left: 14px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 7px;
  bottom: 7px;
  width: 4px;
  border-radius: 2px;
  background: var(--brand-gradient);
}

.section-desc {
  font-size: 14px;
  color: var(--ink-400);
  margin: 0;
}

.section-more {
  font-size: 14px;
  color: var(--brand-500);
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
  transition: color 0.2s;
}

.section-more:hover { color: var(--brand-600); }

.spots-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* ===== 分类 ===== */
.categories-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 16px;
  border-radius: 16px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.3s ease;
  font: inherit;
  text-align: center;
  appearance: none;
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.cat-icon {
  width: 48px; height: 48px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
  background: rgba(255,255,255,0.8);
}

.cat-name {
  font-size: 13px;
  font-weight: 600;
  color: inherit;
}

/* ===== 攻略 ===== */
.guides-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .spots-grid { grid-template-columns: repeat(2, 1fr); }
  .categories-grid { grid-template-columns: repeat(3, 1fr); }
  .guides-grid { grid-template-columns: 1fr; }
}

@media (max-width: 600px) {
  .spots-grid { grid-template-columns: 1fr; }
  .categories-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-title { font-size: 28px; }
}
</style>
