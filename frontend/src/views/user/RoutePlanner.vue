<template>
  <div class="page">
    <!-- 头部 -->
    <div class="page-head">
      <div>
        <h2 class="page-title"><i class="fa fa-map-o mr-2" style="color: var(--brand-600);"></i>动态规划路线</h2>
        <p class="page-desc">勾选想去的景点、自定义出发地、拖拽调整顺序：驾车走高德真实规划，高铁/火车/飞机按速度模型估算</p>
      </div>
      <div class="head-actions">
        <el-radio-group v-model="mode" size="default">
          <el-radio-button value="driving"><i class="fa fa-car mr-1"></i>驾车</el-radio-button>
          <el-radio-button value="hsr"><i class="fa fa-train mr-1"></i>高铁</el-radio-button>
          <el-radio-button value="train"><i class="fa fa-subway mr-1"></i>火车</el-radio-button>
          <el-radio-button value="flight"><i class="fa fa-plane mr-1"></i>飞机</el-radio-button>
        </el-radio-group>
        <template v-if="selected.length">
          <span class="selected-count">已选 <b>{{ selected.length }}</b> 个景点</span>
          <span v-if="routeInfo" class="route-stats">
            <i class="fa fa-road"></i>{{ fmtDistance(routeInfo.distance) }}
            <span class="sep"></span>
            <i class="fa fa-clock-o"></i>{{ routeInfo.estimated ? '约 ' : '' }}{{ fmtDuration(routeInfo.time) }}
            <span v-if="routeInfo.estimated" class="est-tag">预估</span>
          </span>
          <el-dropdown @command="handleExport">
            <el-button size="default" style="border-radius: 10px;">
              <i class="fa fa-download mr-1"></i>导出行程<i class="fa fa-angle-down ml-1"></i>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="copy"><i class="fa fa-clipboard mr-2"></i>复制行程文本</el-dropdown-item>
                <el-dropdown-item command="download"><i class="fa fa-file-text-o mr-2"></i>下载 TXT 文件</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button size="default" @click="clearAll" style="border-radius: 10px;">
            <i class="fa fa-eraser mr-1"></i>清空
          </el-button>
        </template>
      </div>
    </div>

    <div class="planner">
      <!-- 左侧：出发地 + 我的行程 + 景点选择器 -->
      <aside class="side">
        <!-- 出发地设置 -->
        <el-popover ref="startPopRef" placement="bottom-start" :width="336" trigger="click">
          <template #reference>
            <button class="start-row">
              <i class="fa fa-flag start-row-icon"></i>
              <span class="start-info">
                <span class="start-label">出发地</span>
                <span class="start-name">{{ startPoint ? startPoint.name : '默认（第一个景点）' }}</span>
              </span>
              <i v-if="startPoint" class="fa fa-times start-clear" title="恢复默认" @click.stop="clearStart"></i>
              <i class="fa fa-pencil start-edit"></i>
            </button>
          </template>

          <div class="start-pop">
            <div class="start-pop-search">
              <input
                v-model="startKeyword"
                class="start-pop-input"
                placeholder="搜索地点为出发地…"
                @keyup.enter="doSearchStart"
              />
              <button class="start-pop-btn" :disabled="searching" @click="doSearchStart">
                <i class="fa" :class="searching ? 'fa-spinner fa-spin' : 'fa-search'"></i>
              </button>
            </div>

            <div v-if="searchResults.length" class="start-results">
              <div
                v-for="(p, i) in searchResults"
                :key="i"
                class="start-result-item"
                @click="choosePoi(p)"
              >
                <i class="fa fa-map-marker"></i>
                <span class="result-name">{{ p.name }}</span>
                <span class="result-addr">{{ p.address }}</span>
              </div>
            </div>

            <div class="start-pop-actions">
              <button class="start-action" :class="{ active: pickMode }" @click="togglePick">
                <i class="fa fa-hand-pointer-o"></i>{{ pickMode ? '点击地图选点…' : '地图选点' }}
              </button>
              <button class="start-action" @click="locateMe"><i class="fa fa-location-arrow"></i>我的位置</button>
            </div>

            <template v-if="selected.length">
              <div class="start-pop-tip">或从已选景点中指定：</div>
              <div class="start-spots">
                <button
                  v-for="s in selected.slice(0, 6)"
                  :key="s.id"
                  class="start-spot"
                  @click="chooseSpotAsStart(s)"
                >{{ s.name }}</button>
              </div>
            </template>
          </div>
        </el-popover>

        <!-- 我的行程：拖拽排序 -->
        <div v-if="selected.length" class="stops-panel">
          <div class="stops-head">
            <span class="stops-title"><i class="fa fa-list-ol"></i> 我的行程</span>
            <label class="auto-switch">
              <el-switch v-model="autoSort" size="small" />
              <span>自动排序</span>
            </label>
          </div>
          <div class="stops-list">
            <div
              v-for="(s, i) in selected"
              :key="s.id"
              class="stop-item"
              :class="{ dragging: dragIndex === i }"
              draggable="true"
              @dragstart="dragIndex = i"
              @dragover.prevent
              @drop.prevent="moveStop(dragIndex, i)"
              @dragend="dragIndex = -1"
            >
              <span class="stop-no">{{ i + 1 }}</span>
              <span class="stop-name" :title="s.name">{{ s.name }}</span>
              <span class="stop-ops">
                <button :disabled="i === 0" title="上移" @click.stop="moveStop(i, i - 1)"><i class="fa fa-angle-up"></i></button>
                <button :disabled="i === selected.length - 1" title="下移" @click.stop="moveStop(i, i + 1)"><i class="fa fa-angle-down"></i></button>
                <button class="danger" title="移除" @click.stop="toggleSpot(s)"><i class="fa fa-times"></i></button>
              </span>
            </div>
          </div>
          <div class="stops-tip"><i class="fa fa-hand-paper-o"></i> 拖拽或用箭头调整顺序，关闭自动排序后由你掌控</div>
        </div>

        <!-- 景点选择器 -->
        <div class="side-search">
          <i class="fa fa-search"></i>
          <input v-model="keyword" class="side-search-input" placeholder="搜索景点名称…" aria-label="搜索景点名称" @input="handleSpotSearch" />
        </div>

        <div v-loading="loading" class="side-list">
          <EmptyState v-if="!loading && !filteredList.length" title="暂无符合条件的景点" />
          <div
            v-for="spot in filteredList"
            :key="spot.id"
            class="side-item"
            :class="{ checked: isSelected(spot.id) }"
            @click="toggleSpot(spot)"
          >
            <span class="check">
              <i class="fa" :class="isSelected(spot.id) ? 'fa-check-square-o' : 'fa-square-o'"></i>
            </span>
            <span class="item-name">{{ spot.name }}</span>
            <span class="item-price">{{ priceText(spot.price) }}</span>
          </div>
        </div>

        <div class="side-footer" v-if="spotPage < spotTotalPages">
          <button class="load-more-btn" :disabled="loadingMore" @click="loadMoreSpots">
            {{ loadingMore ? '加载中…' : '加载更多' }}
          </button>
        </div>
      </aside>

      <!-- 右侧：地图 -->
      <section class="map-wrap">
        <ScenicMap
          :spots="selected"
          :route="true"
          :mode="mode"
          :start-point="startPoint"
          :pick-point="pickMode"
          :height="mapHeight"
          :show-view-btn="true"
          @view="viewSpot"
          @routeinfo="routeInfo = $event"
          @map-click="onMapPick"
        />
        <div v-if="pickMode" class="pick-mask-tip">
          <i class="fa fa-hand-pointer-o mr-1"></i>在地图上点击任意位置设为出发地
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ScenicMap from '@/components/user/ScenicMap.vue'
import EmptyState from '@/components/user/EmptyState.vue'
import { priceText } from '@/utils/format'
import { getSpotsByPage } from '@/api/scenic'
import { loadAMap } from '@/utils/amap'
import { planRouteOrder, planRouteOrderFrom } from '@/utils/routePlanner'

const router = useRouter()
const route = useRoute()

const list = ref([])          // 当前页已上架景点
const selected = ref([])      // 已选景点（顺序即访问顺序）
const mode = ref('driving')   // 交通模式：driving 驾车 / hsr 高铁 / train 火车 / flight 飞机
const keyword = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const spotPage = ref(1)
const spotTotalPages = ref(1)
let spotSearchTimer = null
const mapHeight = ref(480)

// ---- 出发地 ----
const startPoint = ref(null)  // { name, lng, lat }；null = 默认从第一个景点出发
const startPopRef = ref(null)
const pickMode = ref(false)
const startKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)

// ---- 手动排序 ----
const autoSort = ref(true)    // 开启时每次增删/换出发地后自动最近邻排序
const dragIndex = ref(-1)

// 高德规划结果：总里程（米）/ 总耗时（秒），ScenicMap emit 上来；null 表示暂无（直线兜底/已清空）
const routeInfo = ref(null)

// 景点是否有效坐标（与 ScenicMap.coordOf 同规则，用于可选过滤）
function hasCoord(spot) {
  if (!spot) return false
  const lng = Number(spot.longitude ?? spot.lng)
  const lat = Number(spot.latitude ?? spot.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return false
  return !(lng === 0 && lat === 0)
}

function coordArr(spot) {
  return [Number(spot.longitude ?? spot.lng), Number(spot.latitude ?? spot.lat)]
}

// 可勾选列表：只过滤无坐标景点（名称搜索已交给服务端）
const filteredList = computed(() => {
  return list.value.filter(s => hasCoord(s))
})

function isSelected(id) {
  return selected.value.some(s => s.id === id)
}

function toggleSpot(spot) {
  const idx = selected.value.findIndex(s => s.id === spot.id)
  // 总是返回新数组：ScenicMap 通过 watch(() => props.spots) 感知变化，原地 splice 不会触发刷新
  selected.value = idx >= 0
    ? selected.value.filter(s => s.id !== spot.id)
    : [...selected.value, spot]
}

// 自动排序：增删景点 / 切换开关 / 更换出发地后按最近邻重排（手动排序时跳过）
// 顺序无变化时不重新赋值，避免 watch 触发死循环
function maybeReorder() {
  if (!autoSort.value) return
  const val = selected.value
  if (val.length < 3) return
  const coords = val.map(coordArr)

  let order
  if (startPoint.value) {
    // 出发地是已选景点 → 从它开始排；外部地点 → 先就近接入再贪心
    const idx = val.findIndex(s => {
      const [lng, lat] = coordArr(s)
      return Math.abs(lng - startPoint.value.lng) < 1e-6 && Math.abs(lat - startPoint.value.lat) < 1e-6
    })
    order = idx >= 0
      ? planRouteOrder(coords, idx)
      : planRouteOrderFrom(coords, [startPoint.value.lng, startPoint.value.lat])
  } else {
    order = planRouteOrder(coords)
  }
  if (order.every((v, i) => v === i)) return
  selected.value = order.map(i => val[i])
}

watch([selected, autoSort, startPoint], () => maybeReorder())

// ---- 手动排序 ----
function moveStop(from, to) {
  if (from < 0 || to < 0 || from === to || from >= selected.value.length) return
  const arr = [...selected.value]
  const [item] = arr.splice(from, 1)
  arr.splice(to, 0, item)
  autoSort.value = false // 拖拽即接管排序
  selected.value = arr
}

// ---- 出发地操作 ----
function closeStartPop() {
  startPopRef.value?.hide()
}

function setStart(p) {
  startPoint.value = p
  closeStartPop()
  ElMessage.success(`出发地已设为：${p.name}`)
}

function clearStart() {
  startPoint.value = null
  closeStartPop()
}

function chooseSpotAsStart(spot) {
  const [lng, lat] = coordArr(spot)
  setStart({ name: spot.name, lng, lat })
}

function choosePoi(p) {
  setStart({ name: p.name, lng: p.lng, lat: p.lat })
}

function togglePick() {
  pickMode.value = !pickMode.value
  if (pickMode.value) closeStartPop()
}

// 地图选点 → 逆地理编码出地名
async function onMapPick(lnglat) {
  pickMode.value = false
  const fallback = { name: '地图选点', lng: lnglat.lng, lat: lnglat.lat }
  try {
    const AMap = await loadAMap()
    AMap.plugin('AMap.Geocoder', () => {
      try {
        const geo = new AMap.Geocoder()
        geo.getAddress(lnglat, (status, result) => {
          const name = status === 'complete' && result?.regeocode?.formattedAddress
            ? result.regeocode.formattedAddress
            : fallback.name
          setStart({ name, lng: lnglat.lng, lat: lnglat.lat })
        })
      } catch (e) {
        setStart(fallback)
      }
    })
  } catch (e) {
    setStart(fallback)
  }
}

// 我的位置：浏览器 + 高德混合定位
async function locateMe() {
  try {
    const AMap = await loadAMap()
    AMap.plugin('AMap.Geolocation', () => {
      const geo = new AMap.Geolocation({ enableHighAccuracy: true, timeout: 8000 })
      geo.getCurrentPosition((status, result) => {
        if (status === 'complete' && result?.position) {
          setStart({
            name: '我的位置',
            lng: result.position.getLng(),
            lat: result.position.getLat()
          })
        } else {
          ElMessage.warning('定位失败，请检查浏览器定位权限，或用地图选点 / 搜索地点')
        }
      })
    })
  } catch (e) {
    ElMessage.warning('定位服务暂不可用，请用地图选点 / 搜索地点')
  }
}

// 关键字搜索地点（高德 PlaceSearch）
async function doSearchStart() {
  const kw = startKeyword.value.trim()
  if (!kw) return
  searching.value = true
  try {
    const AMap = await loadAMap()
    AMap.plugin('AMap.PlaceSearch', () => {
      const svc = new AMap.PlaceSearch({ city: '全国', citylimit: false, pageSize: 6 })
      svc.search(kw, (status, result) => {
        searching.value = false
        if (status === 'complete' && result?.pois?.length) {
          searchResults.value = result.pois.map(p => {
            const loc = p.location || {}
            return {
              name: p.name,
              address: [p.pname, p.cityname, p.adname, p.address].filter(Boolean).join(' '),
              lng: typeof loc.getLng === 'function' ? loc.getLng() : Number(loc.lng),
              lat: typeof loc.getLat === 'function' ? loc.getLat() : Number(loc.lat)
            }
          })
        } else {
          searchResults.value = []
          ElMessage.info('没有找到相关地点，换个关键词试试')
        }
      })
    })
  } catch (e) {
    searching.value = false
    ElMessage.warning('地点搜索暂不可用')
  }
}

// ---- 导出行程 ----
const MODE_TEXT = { driving: '驾车', hsr: '高铁', train: '火车', flight: '飞机' }

function fmtDistance(m) {
  if (!Number.isFinite(m)) return '--'
  return m >= 1000 ? `${(m / 1000).toFixed(1)} 公里` : `${Math.round(m)} 米`
}

function fmtDuration(sec) {
  if (!Number.isFinite(sec)) return '--'
  const h = Math.floor(sec / 3600)
  const min = Math.round((sec % 3600) / 60)
  return h > 0 ? `${h}小时${min}分钟` : `${min || 1}分钟`
}

function buildItineraryText() {
  const lines = []
  lines.push('旅迹 · 行程单')
  lines.push('------------------------')
  const stats = []
  if (routeInfo.value) {
    stats.push(`总里程：${fmtDistance(routeInfo.value.distance)}`)
    stats.push(`${routeInfo.value.estimated ? '预估耗时' : '预计耗时'}：${fmtDuration(routeInfo.value.time)}`)
  }
  lines.push(`交通方式：${MODE_TEXT[mode.value] || '驾车'}${stats.length ? `　${stats.join('　')}` : ''}`)
  if (startPoint.value) lines.push(`出发地：${startPoint.value.name}`)
  lines.push('')
  selected.value.forEach((s, i) => {
    lines.push(`${i + 1}. ${s.name}${s.address ? `（${s.address}）` : ''}`)
  })
  lines.push('')
  lines.push(`共 ${selected.value.length} 个景点 · 生成于 ${new Date().toLocaleString()}`)
  return lines.join('\n')
}

function downloadItinerary() {
  const blob = new Blob([buildItineraryText()], { type: 'text/plain;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `旅迹行程单-${Date.now()}.txt`
  a.click()
  URL.revokeObjectURL(a.href)
}

function handleExport(cmd) {
  if (!selected.value.length) return
  if (cmd === 'download') {
    downloadItinerary()
    return
  }
  const text = buildItineraryText()
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text)
      .then(() => ElMessage.success('行程已复制到剪贴板'))
      .catch(() => { downloadItinerary(); ElMessage.success('已改为下载 TXT 行程单') })
  } else {
    downloadItinerary()
    ElMessage.success('已下载 TXT 行程单')
  }
}

function clearAll() {
  selected.value = []
  routeInfo.value = null
}

function viewSpot(spot) {
  router.push({ path: `/travel/home/spots/${spot.id}`, state: { spot } })
}

async function loadSpots(reset = true) {
  if (reset) {
    spotPage.value = 1
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const params = { page: spotPage.value, size: 20, status: 1 }
    const kw = keyword.value.trim()
    if (kw) params.name = kw
    const res = await getSpotsByPage(params)
    if (res.code === 1) {
      const rows = res.content?.content || res.content || []
      if (reset) list.value = rows
      else list.value.push(...rows)
      spotTotalPages.value = res.content?.totalPages ?? 1
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function handleSpotSearch() {
  clearTimeout(spotSearchTimer)
  spotSearchTimer = setTimeout(() => loadSpots(true), 300)
}

function loadMoreSpots() {
  if (spotPage.value >= spotTotalPages.value) return
  spotPage.value += 1
  loadSpots(false)
}

function updateHeight() {
  const h = window.innerHeight
  mapHeight.value = Math.max(380, Math.min(560, h - 300))
}

onMounted(() => {
  loadSpots()
  updateHeight()
  importAiPlan()
  window.addEventListener('resize', updateHeight)
})

/**
 * 接收 AI 规划页带过来的行程。
 *
 * <p>关掉自动排序：规划页默认按最近邻重排，会把 AI"按天"的顺序打乱，
 * 带过来就失去意义了。用户想优化可以自己重新打开自动排序或拖拽。
 */
function importAiPlan() {
  if (route.query.from !== 'ai') {
    return
  }
  const raw = sessionStorage.getItem('travel:ai:planner')
  // 清掉 query，避免刷新页面时重复导入；同时让抽屉里的路由监听自然关闭
  router.replace({ path: route.path, query: {} })
  if (!raw) {
    return
  }
  sessionStorage.removeItem('travel:ai:planner')
  try {
    const payload = JSON.parse(raw)
    const spots = (payload.spots || []).filter(
      (spot) => spot && spot.id && spot.longitude != null && spot.latitude != null
    )
    if (!spots.length) {
      ElMessage.warning('AI 行程里的景点缺少坐标，无法在地图上规划')
      return
    }
    autoSort.value = false
    selected.value = spots
    ElMessage.success(`已带入 AI 行程的 ${spots.length} 个景点`)
  } catch {
    ElMessage.warning('AI 行程解析失败，请回到 AI 规划重新生成')
  }
}

watch(() => route.query.from, importAiPlan)

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateHeight)
  clearTimeout(spotSearchTimer)
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
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--ink-900);
  margin: 0 0 6px;
}

.page-desc {
  font-size: 14px;
  color: var(--ink-400);
  margin: 0;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.selected-count {
  font-size: 13px;
  color: var(--ink-600);
}
.selected-count b { color: var(--brand-600); }

/* 规划结果：总里程 / 耗时 */
.route-stats {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--brand-50);
  border: 1px solid var(--brand-200);
  color: var(--brand-700);
  font-size: 13px;
  font-weight: 600;
  padding: 7px 14px;
  border-radius: 999px;
}

.route-stats i { font-size: 12px; opacity: 0.85; }

.route-stats .sep {
  width: 1px;
  height: 12px;
  background: var(--brand-200);
}

/* 预估标记（高铁/火车/飞机为估算值，非真实时刻） */
.est-tag {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: #F59E0B;
  border-radius: 6px;
  padding: 1px 6px;
  margin-left: 2px;
}

/* ===== 主体两栏 ===== */
.planner {
  display: flex;
  gap: 18px;
  align-items: stretch;
}

.side {
  width: 340px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid var(--ink-100);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--shadow-card);
  height: 600px;
}

/* ===== 出发地行 ===== */
.start-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: none;
  border-bottom: 1px solid var(--ink-100);
  background: linear-gradient(135deg, var(--brand-50), #fff);
  cursor: pointer;
  text-align: left;
  width: 100%;
  box-sizing: border-box;
  transition: background 0.2s;
}

.start-row:hover { background: var(--brand-50); }

.start-row-icon {
  width: 32px; height: 32px;
  border-radius: 10px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.start-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.start-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-400);
  letter-spacing: 1px;
}

.start-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.start-clear {
  color: var(--ink-300);
  font-size: 13px;
  padding: 6px;
  border-radius: 8px;
  flex-shrink: 0;
}

.start-clear:hover { color: #DC2626; background: #FEF2F2; }

.start-edit {
  color: var(--ink-300);
  font-size: 12px;
  flex-shrink: 0;
}

/* ===== 出发地弹层 ===== */
.start-pop {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.start-pop-search {
  display: flex;
  gap: 6px;
}

.start-pop-input {
  flex: 1;
  min-width: 0;
  height: 34px;
  border: 1px solid var(--ink-200);
  border-radius: 8px;
  padding: 0 10px;
  font-size: 13px;
  color: var(--ink-800);
  outline: none;
  transition: border-color 0.2s;
}

.start-pop-input:focus { border-color: var(--brand-500); }

.start-pop-btn {
  width: 34px; height: 34px;
  border: none;
  border-radius: 8px;
  background: var(--brand-gradient);
  color: #fff;
  cursor: pointer;
  flex-shrink: 0;
}

.start-results {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 150px;
  overflow-y: auto;
}

.start-result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.start-result-item:hover { background: var(--brand-50); }
.start-result-item i { color: var(--brand-500); font-size: 13px; flex-shrink: 0; }

.result-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-800);
  white-space: nowrap;
}

.result-addr {
  font-size: 12px;
  color: var(--ink-400);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.start-pop-actions {
  display: flex;
  gap: 8px;
}

.start-action {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 0;
  border: 1px solid var(--ink-200);
  border-radius: 10px;
  background: #fff;
  color: var(--ink-600);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.start-action:hover { border-color: var(--brand-500); color: var(--brand-600); }

.start-action.active {
  background: var(--brand-gradient);
  border-color: transparent;
  color: #fff;
}

.start-pop-tip {
  font-size: 12px;
  color: var(--ink-400);
}

.start-spots {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.start-spot {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--ink-200);
  background: #fff;
  color: var(--ink-600);
  font-size: 12px;
  cursor: pointer;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: all 0.2s;
}

.start-spot:hover {
  border-color: var(--brand-500);
  color: var(--brand-600);
  background: var(--brand-50);
}

/* ===== 我的行程（排序面板） ===== */
.stops-panel {
  border-bottom: 1px solid var(--ink-100);
  display: flex;
  flex-direction: column;
}

.stops-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px 6px;
}

.stops-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-800);
}

.stops-title i { color: var(--brand-500); margin-right: 4px; }

.auto-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-400);
  cursor: pointer;
}

.stops-list {
  max-height: 168px;
  overflow-y: auto;
  padding: 2px 8px;
}

.stop-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 10px;
  cursor: grab;
  transition: background 0.15s;
}

.stop-item:hover { background: var(--ink-50); }
.stop-item.dragging { opacity: 0.45; background: var(--brand-50); }

.stop-no {
  width: 20px; height: 20px;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.stop-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stop-ops {
  display: none;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.stop-item:hover .stop-ops { display: flex; }
.stop-item:focus-within .stop-ops { display: flex; }

.stop-ops button {
  width: 22px; height: 22px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ink-400);
  font-size: 12px;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}

@media (hover: none), (max-width: 900px) {
  .stop-ops {
    display: flex;
  }
  .stop-ops button {
    width: 32px;
    height: 32px;
  }
}

.stop-ops button:hover:not(:disabled) { background: var(--ink-100); color: var(--ink-700); }

.stop-ops button.danger:hover { background: #FEF2F2; color: #DC2626; }

.stop-ops button:disabled { opacity: 0.3; cursor: not-allowed; }

.stops-tip {
  padding: 6px 14px 10px;
  font-size: 11px;
  color: var(--ink-300);
}

/* ===== 景点选择器 ===== */
.side-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--ink-100);
}

.side-search i {
  color: var(--ink-300);
  font-size: 14px;
}

.side-search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--ink-800);
  background: transparent;
}

.side-search-input::placeholder { color: var(--ink-300); }

.side-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.side-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.side-item:hover { background: var(--ink-50); }

.side-item.checked { background: var(--brand-50); }

.check {
  color: var(--ink-300);
  font-size: 16px;
  flex-shrink: 0;
}

.side-item.checked .check { color: var(--brand-600); }

.item-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 12px;
  font-weight: 600;
  color: var(--brand-600);
  flex-shrink: 0;
}

.side-footer {
  padding: 10px 14px;
  border-top: 1px solid var(--ink-100);
  font-size: 12px;
  color: var(--ink-400);
}

.load-more-btn {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--ink-200);
  border-radius: 8px;
  background: #fff;
  color: var(--ink-600);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--brand-400);
  color: var(--brand-600);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== 地图区 ===== */
.map-wrap {
  flex: 1;
  min-width: 0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-card);
  position: relative;
}

.map-wrap :deep(.scenic-map) {
  border-radius: 16px;
  min-height: 380px;
}

/* 选点模式提示条 */
.pick-mask-tip {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(13, 27, 42, 0.88);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  padding: 9px 18px;
  border-radius: 999px;
  z-index: 5;
  white-space: nowrap;
  pointer-events: none;
}

@media (max-width: 860px) {
  .planner { flex-direction: column; }
  .side { width: 100%; height: 480px; }
  .map-wrap :deep(.scenic-map) { min-height: 360px; }
}
</style>
