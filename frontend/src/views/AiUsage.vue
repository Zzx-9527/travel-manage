<template>
  <div class="page-container">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-xl font-bold" style="color: var(--ink-900, #1f2937);">AI 用量看板</h1>
        <p class="text-sm mt-1" style="color: var(--ink-500, #6b7280);">
          行程生成次数、成功率、耗时与幻觉过滤量
        </p>
      </div>
      <el-button :loading="loading" @click="load">
        <i class="fa fa-refresh mr-1"></i>刷新
      </el-button>
    </div>

    <el-alert
      v-if="!tableReady"
      type="warning"
      :closable="false"
      show-icon
      title="记录表尚未创建"
      description="请先在 travel_management 库执行 backend/sql/ai_plan_record.sql，然后刷新本页。"
      class="mb-5"
    />

    <template v-if="tableReady">
      <!-- 统计卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-6">
        <div
          v-for="card in cards"
          :key="card.label"
          class="bg-white rounded-xl p-5"
          style="box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);"
        >
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm" style="color: var(--ink-500, #6b7280);">{{ card.label }}</span>
            <span
              class="w-9 h-9 rounded-lg flex items-center justify-center text-white"
              :style="{ background: card.color }"
            >
              <i :class="['fa', card.icon]"></i>
            </span>
          </div>
          <div class="text-2xl font-bold" style="color: var(--ink-900, #1f2937);">{{ card.value }}</div>
          <div class="text-xs mt-1" style="color: var(--ink-400, #9ca3af);">{{ card.hint }}</div>
        </div>
      </div>

      <!-- 趋势 -->
      <div
        class="bg-white rounded-xl p-5 mb-6"
        style="box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);"
      >
        <h2 class="text-sm font-semibold mb-3" style="color: var(--ink-800, #374151);">近 7 天生成量</h2>
        <div ref="trendRef" class="w-full" style="height: 260px;"></div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <!-- 热门目的地 -->
        <div
          class="bg-white rounded-xl p-5"
          style="box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);"
        >
          <h2 class="text-sm font-semibold mb-3" style="color: var(--ink-800, #374151);">热门目的地</h2>
          <div v-if="!topDestinations.length" class="text-sm" style="color: var(--ink-400);">暂无数据</div>
          <ul v-else class="space-y-2">
            <li
              v-for="(item, index) in topDestinations"
              :key="item.destination"
              class="flex items-center justify-between text-sm py-1"
            >
              <span class="flex items-center gap-2">
                <span
                  class="w-5 h-5 rounded text-xs flex items-center justify-center text-white"
                  :style="{ background: index === 0 ? '#1CB89D' : '#9CA3AF' }"
                >{{ index + 1 }}</span>
                <span style="color: var(--ink-700, #4b5563);">{{ item.destination }}</span>
              </span>
              <span style="color: var(--ink-500, #6b7280);">{{ item.total }} 次</span>
            </li>
          </ul>
        </div>

        <!-- 最近记录 -->
        <div
          class="bg-white rounded-xl p-5 lg:col-span-2"
          style="box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);"
        >
          <h2 class="text-sm font-semibold mb-3" style="color: var(--ink-800, #374151);">最近生成</h2>
          <el-table :data="recent" size="small" style="width: 100%;">
            <el-table-column prop="destination" label="目的地" width="100" />
            <el-table-column prop="days" label="天数" width="70" align="center" />
            <el-table-column label="结果" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" type="success" size="small">成功</el-tag>
                <el-tag v-else type="danger" size="small">失败</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="scenicCount" label="景点" width="70" align="center" />
            <el-table-column label="耗时" width="90" align="right">
              <template #default="{ row }">{{ row.latencyMs }} ms</template>
            </el-table-column>
            <el-table-column label="失败原因" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span style="color: #e24b4a;">{{ row.errorMsg || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { fetchAiStats } from '@/api/ai'

echarts.use([LineChart, TooltipComponent, GridComponent, CanvasRenderer])

const loading = ref(false)
const tableReady = ref(true)
const summary = ref({})
const trend = ref([])
const topDestinations = ref([])
const recent = ref([])
const trendRef = ref(null)
const chart = shallowRef(null)

const cards = computed(() => {
  const s = summary.value || {}
  const total = Number(s.total || 0)
  const success = Number(s.success || 0)
  const failure = Number(s.failure || 0)
  const rate = total > 0 ? Math.round((success / total) * 100) : 0
  return [
    { label: '今日生成', value: total, hint: `成功 ${success} · 失败 ${failure}`, icon: 'fa-magic', color: '#1CB89D' },
    { label: '成功率', value: `${rate}%`, hint: '今日成功 / 总次数', icon: 'fa-check-circle', color: '#3B82F6' },
    { label: '平均耗时', value: `${Number(s.avgLatency || 0)} ms`, hint: '仅统计成功请求', icon: 'fa-clock-o', color: '#F59E0B' },
    { label: '过滤条目', value: Number(s.dropped || 0), hint: '模型返回但不在候选集的景点', icon: 'fa-filter', color: '#EF4444' }
  ]
})

onMounted(load)
onBeforeUnmount(() => chart.value?.dispose())

async function load() {
  loading.value = true
  try {
    const res = await fetchAiStats()
    const data = res.content || {}
    tableReady.value = data.tableReady !== false
    summary.value = data.summary || {}
    trend.value = data.trend || []
    topDestinations.value = data.topDestinations || []
    recent.value = data.recent || []
    if (tableReady.value) {
      await nextTick()
      renderTrend()
    }
  } finally {
    loading.value = false
  }
}

function renderTrend() {
  if (!trendRef.value) return
  if (!chart.value) {
    chart.value = echarts.init(trendRef.value)
  }
  const days = trend.value.map((item) => item.day)
  const totals = trend.value.map((item) => Number(item.total || 0))
  const successes = trend.value.map((item) => Number(item.success || 0))

  chart.value.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['生成次数', '成功次数'], right: 0, top: 0, textStyle: { fontSize: 12 } },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#E5E7EB' } }, axisLabel: { color: '#9CA3AF' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#F3F4F6' } }, axisLabel: { color: '#9CA3AF' } },
    series: [
      { name: '生成次数', type: 'line', smooth: true, data: totals, itemStyle: { color: '#1CB89D' }, areaStyle: { opacity: 0.12 } },
      { name: '成功次数', type: 'line', smooth: true, data: successes, itemStyle: { color: '#3B82F6' } }
    ]
  })
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}
</style>
