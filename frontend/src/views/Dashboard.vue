<template>
  <div class="page-container">
    <!-- 欢迎区域 -->
    <div
      class="rounded-2xl p-8 mb-6 text-white relative overflow-hidden"
      style="background: linear-gradient(135deg, #0D1B2A 0%, #132D46 50%, #1A3A5C 100%);"
    >
      <img :src="heroScene" class="welcome-scene" alt="" />
      <div class="absolute top-0 right-0 w-64 h-64 opacity-5">
        <i class="fa fa-globe text-[200px]"></i>
      </div>
      <div class="relative z-10">
        <h1 class="text-2xl font-bold mb-2">欢迎回来，管理员</h1>
        <p class="text-base opacity-80">旅游信息管理平台 · 数据概览</p>
        <div class="flex items-center gap-4 mt-4 text-sm opacity-70">
          <span><i class="fa fa-calendar mr-1.5"></i>{{ currentDate }}</span>
          <span><i class="fa fa-clock-o mr-1.5"></i>{{ currentTime }}</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-6">
      <div
        v-for="stat in statsCards"
        :key="stat.label"
        class="bg-white rounded-xl p-5 transition-all duration-300 cursor-pointer hover:-translate-y-1"
        :style="{ boxShadow: '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)' }"
        @mouseenter="stat.hover = true"
        @mouseleave="stat.hover = false"
      >
        <div class="flex items-center justify-between mb-4">
          <div
            class="w-12 h-12 rounded-xl flex items-center justify-center text-xl transition-all duration-300"
            :style="{
              background: stat.hover ? stat.gradient : stat.bg,
              boxShadow: stat.hover ? `0 4px 12px ${stat.shadowColor}` : 'none'
            }"
          >
            <i :class="['fa', stat.icon]" class="text-white"></i>
          </div>
        </div>
        <p class="text-sm mb-1" style="color: #94A3B8;">{{ stat.label }}</p>
        <p class="text-2xl font-bold" style="color: #1E293B;">{{ stat.value }}</p>
        <div v-if="stat.trend != null" class="flex items-center gap-1 mt-2 text-xs" :style="{ color: stat.trend > 0 ? '#10B981' : '#EF4444' }">
          <i :class="stat.trend > 0 ? 'fa fa-arrow-up' : 'fa fa-arrow-down'"></i>
          <span>{{ Math.abs(stat.trend) }}% 较上月</span>
        </div>
      </div>
    </div>

    <!-- 数据图表 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-6">
      <div class="bg-white rounded-xl p-6" :style="{ boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }">
        <h3 class="text-base font-semibold mb-2" style="color: #1E293B;">
          <i class="fa fa-pie-chart mr-2" style="color: #1CB89D;"></i>景点分类分布
        </h3>
        <div ref="categoryChartRef" class="chart-box"></div>
      </div>
      <div class="bg-white rounded-xl p-6" :style="{ boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }">
        <h3 class="text-base font-semibold mb-2" style="color: #1E293B;">
          <i class="fa fa-pie-chart mr-2" style="color: #8B5CF6;"></i>订单状态分布
        </h3>
        <div ref="orderChartRef" class="chart-box"></div>
      </div>
    </div>

    <!-- 快捷操作 & 系统信息 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 快捷操作 -->
      <div class="lg:col-span-2 bg-white rounded-xl p-6" :style="{ boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }">
        <h3 class="text-base font-semibold mb-4" style="color: #1E293B;">
          <i class="fa fa-bolt mr-2" style="color: #1CB89D;"></i>快捷操作
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <button
            v-for="action in quickActions"
            :key="action.label"
            @click="handleQuickAction(action)"
            class="flex flex-col items-center gap-2 p-4 rounded-xl transition-all duration-200 hover:-translate-y-0.5"
            :style="{
              background: action.bg,
              border: '1px solid ' + action.borderColor
            }"
          >
            <div
              class="w-10 h-10 rounded-lg flex items-center justify-center"
              :style="{ background: action.iconBg }"
            >
              <i :class="['fa', action.icon]" :style="{ color: action.iconColor }"></i>
            </div>
            <span class="text-xs font-medium" style="color: #475569;">{{ action.label }}</span>
          </button>
        </div>
      </div>

      <!-- 系统信息 -->
      <div class="bg-white rounded-xl p-6" :style="{ boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }">
        <h3 class="text-base font-semibold mb-4" style="color: #1E293B;">
          <i class="fa fa-info-circle mr-2" style="color: #1CB89D;"></i>系统信息
        </h3>
        <div class="space-y-3">
          <div v-for="info in systemInfo" :key="info.label" class="flex items-center justify-between text-sm">
            <span style="color: #94A3B8;">{{ info.label }}</span>
            <span style="color: #475569;">{{ info.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStats } from '@/api/dashboard'
import { getSpotsByPage } from '@/api/scenic'
import { getCategoriesByPage } from '@/api/category'
import { getOrdersByPage } from '@/api/order'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import heroScene from '@/assets/images/hero-scene.svg'

echarts.use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()

// 图表实例与 DOM
const categoryChartRef = ref(null)
const orderChartRef = ref(null)
const charts = {}

// 时间
const currentDate = ref('')
const currentTime = ref('')
let timer = null

function updateTime() {
  const now = new Date()
  const days = ['日', '一', '二', '三', '四', '五', '六']
  currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${days[now.getDay()]}`
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
}

function handleResize() {
  Object.values(charts).forEach(c => c && c.resize())
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  loadStats()
  loadCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  Object.values(charts).forEach(c => c && c.dispose())
})

// 统计卡片（值来自 dashboard/stats，加载失败保留 '--' 占位）
const statsCards = ref([
  {
    icon: 'fa-users',
    label: '用户总数',
    value: '--',
    trend: null,
    bg: 'linear-gradient(135deg, #3B82F6, #2563EB)',
    gradient: 'linear-gradient(135deg, #3B82F6, #1D4ED8)',
    shadowColor: 'rgba(59, 130, 246, 0.3)',
    hover: false
  },
  {
    icon: 'fa-map-marker',
    label: '景点总数',
    value: '--',
    trend: null,
    bg: 'linear-gradient(135deg, #1CB89D, #0D9488)',
    gradient: 'linear-gradient(135deg, #1CB89D, #0F766E)',
    shadowColor: 'rgba(28, 184, 157, 0.3)',
    hover: false
  },
  {
    icon: 'fa-book',
    label: '攻略总数',
    value: '--',
    trend: null,
    bg: 'linear-gradient(135deg, #F59E0B, #D97706)',
    gradient: 'linear-gradient(135deg, #F59E0B, #B45309)',
    shadowColor: 'rgba(245, 158, 11, 0.3)',
    hover: false
  },
  {
    icon: 'fa-ticket',
    label: '订单总数',
    value: '--',
    trend: null,
    bg: 'linear-gradient(135deg, #8B5CF6, #7C3AED)',
    gradient: 'linear-gradient(135deg, #8B5CF6, #6D28D9)',
    shadowColor: 'rgba(139, 92, 246, 0.3)',
    hover: false
  }
])

async function loadStats() {
  try {
    const res = await getStats()
    if (res.code === 1) {
      const s = res.content || {}
      statsCards.value[0].value = s.userCount ?? '--'
      statsCards.value[1].value = s.scenicCount ?? '--'
      statsCards.value[2].value = s.strategyCount ?? '--'
      statsCards.value[3].value = s.orderCount ?? '--'
    }
  } catch (e) {
    // 后端不可用时保留 '--' 占位，不打扰用户
  }
}

// 分页拉全量列表（后端 queryByPage 用 page/size，res.content.content 为当前页数据）
async function fetchAll(fetcher, params = {}) {
  const all = []
  const size = 100
  let page = 1
  for (;;) {
    const res = await fetcher({ page, size, ...params })
    if (res.code !== 1) break
    const c = res.content || {}
    const rows = c.content || []
    all.push(...rows)
    if (page >= (c.totalPages ?? 1) || rows.length === 0) break
    page++
  }
  return all
}

// 环形图统一渲染：中间显示总数，空数据显示占位灰环
// 配色以品牌青绿领衔，功能色作区隔
const CHART_COLORS = ['#0D9488', '#1CB89D', '#3B82F6', '#F59E0B', '#8B5CF6', '#14B8A6', '#F97316', '#64748B']

function renderPie(elRef, key, title, data) {
  const el = elRef.value
  if (!el) return
  if (!charts[key]) charts[key] = echarts.init(el)
  const total = data.reduce((s, d) => s + d.value, 0)
  const empty = data.length === 0
  charts[key].setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#64748B', fontSize: 12 }
    },
    color: CHART_COLORS,
    title: {
      text: empty ? '暂无数据' : `${total}`,
      subtext: empty ? '' : '总计',
      left: 'center',
      top: '36%',
      textStyle: { fontSize: 22, fontWeight: 700, color: '#1E293B' },
      subtextStyle: { fontSize: 12, color: '#94A3B8' }
    },
    series: [{
      name: title,
      type: 'pie',
      radius: ['48%', '68%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      minShowLabelAngle: 5,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      // 标签默认显示（名称 + 占比），过小的扇形不显示避免拥挤
      label: empty ? { show: false } : { show: true, formatter: '{b}\n{d}%', fontSize: 12, color: '#475569', lineHeight: 18 },
      labelLine: empty ? { show: false } : { show: true, length: 12, length2: 14, lineStyle: { color: '#CBD5E1' } },
      emphasis: { label: { show: true, fontWeight: 600 } },
      data: empty ? [{ name: '暂无数据', value: 1, itemStyle: { color: '#E2E8F0' } }] : data
    }]
  })
}

// 图表数据：全部纯前端从现有 queryByPage 拉全量聚合，不改后端
async function loadCharts() {
  try {
    // ① 景点分类分布：scenic 按 categoryId 分组
    const [catsRes, spots] = await Promise.all([
      getCategoriesByPage({ page: 1, size: 100 }).catch(() => null),
      fetchAll(getSpotsByPage)
    ])
    const catName = {}
    ;(catsRes?.content?.content || []).forEach(c => {
      if (c.id != null) catName[c.id] = c.name
    })
    const spotCount = {}
    spots.forEach(s => {
      const name = s.categoryId?.name || catName[s.categoryId?.id] || '未分类'
      spotCount[name] = (spotCount[name] || 0) + 1
    })
    renderPie(categoryChartRef, 'category', '景点分类分布',
      Object.entries(spotCount).filter(([, n]) => n > 0).map(([name, value]) => ({ name, value })))

    // ② 订单状态分布：scenicOrders 按 payStatus 分组
    const orders = await fetchAll(getOrdersByPage)
    const statusMap = { 0: '未支付', 1: '已支付', 2: '免费' }
    const orderCount = {}
    orders.forEach(o => {
      const name = statusMap[o.payStatus] ?? `其他(${o.payStatus})`
      orderCount[name] = (orderCount[name] || 0) + 1
    })
    renderPie(orderChartRef, 'order', '订单状态分布',
      Object.entries(orderCount).filter(([, n]) => n > 0).map(([name, value]) => ({ name, value })))
  } catch (e) {
    // 后端不可用时图表保持空白，不打扰用户
  }
}

// 快捷操作（路径需带 /travel 前缀，与后台路由一致）
const quickActions = ref([
  { icon: 'fa-user-plus', label: '添加用户', path: '/travel/users', bg: '#F0FDF4', borderColor: '#BBF7D0', iconBg: '#DCFCE7', iconColor: '#22C55E' },
  { icon: 'fa-map-marker', label: '管理景点', path: '/travel/spots', bg: '#F0FDFA', borderColor: '#99F6E4', iconBg: '#CCFBF1', iconColor: '#14B8A6' },
  { icon: 'fa-book', label: '发布攻略', path: '/travel/guides', bg: '#FFFBEB', borderColor: '#FDE68A', iconBg: '#FEF3C7', iconColor: '#F59E0B' },
  { icon: 'fa-ticket', label: '查看订单', path: '/travel/orders', bg: '#F5F3FF', borderColor: '#DDD6FE', iconBg: '#EDE9FE', iconColor: '#8B5CF6' },
])

// 系统信息
const systemInfo = ref([
  { label: '后端框架', value: 'Spring Boot' },
  { label: '前端框架', value: 'Vue 3 + Vite' },
  { label: 'UI 组件', value: 'Element Plus' },
  { label: '接口地址', value: 'localhost:8081' },
  { label: '运行状态', value: '正常运行中' },
])

function handleQuickAction(action) {
  router.push(action.path).catch(() => {})
}
</script>

<style scoped>
.chart-box {
  width: 100%;
  height: 300px;
}

/* 欢迎横幅底部山水插画 */
.welcome-scene {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  width: 100%;
  height: 78%;
  object-fit: cover;
  object-position: center bottom;
  opacity: 0.5;
  pointer-events: none;
}
</style>
