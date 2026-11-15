<template>
  <div class="scenic-map">
    <!-- 未配置高德 Key：降级提示 -->
    <EmptyState
      v-if="!configured"
      title="未配置地图服务"
      desc="请在 lbs.amap.com 申请 Web端(JS API) Key，填写 src/config/amap.js 后刷新查看景点地图"
    />

    <template v-else>
      <div ref="mapEl" class="map-container" :style="{ height: height + 'px' }"></div>

      <!-- 地图加载失败：可重试，不与“缺少经纬度”混淆 -->
      <div v-if="loadError" class="map-error">
        <EmptyState title="地图加载失败" desc="请检查高德 Key、网络连接，或稍后重试">
          <el-button type="primary" @click="retryLoad">重新加载</el-button>
        </EmptyState>
      </div>

      <!-- 加载遮罩（初始化 / 路径规划中） -->
      <div v-if="busy || planning" class="map-mask">
        <i class="fa fa-spinner fa-spin text-3xl" style="color: #1CB89D;"></i>
        <p class="map-mask-text">{{ busy ? '地图加载中…' : '路线规划中…' }}</p>
      </div>

      <el-empty v-if="!route && !busy && !spots.length" description="暂无景点" :image-size="90" />
      <el-empty
        v-else-if="!route && !busy && spots.length && !placedCount"
        description="暂无可以在地图上展示的景点（缺少经纬度）"
        :image-size="90"
      />
      <div v-else-if="!busy && route && placedCount === 1" class="route-hint">
        <i class="fa fa-map-o mr-1"></i>再选一个景点即可规划路线
      </div>
      <div v-else-if="!busy && routeFallback" class="route-hint">
        <i class="fa fa-exclamation-circle mr-1"></i>部分路段未规划出驾车路线，已用直线连接
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import EmptyState from '@/components/user/EmptyState.vue'
import { isAmapConfigured, loadAMap } from '@/utils/amap'
import { estimateLeg } from '@/utils/routePlanner'
import { resolveUploadUrl } from '@/api/file'

const props = defineProps({
  spots: { type: Array, default: () => [] },
  // 地图容器高度（px），详情页单景点地图可调小
  height: { type: Number, default: 520 },
  // 标记放好后自动弹出第一个景点的信息窗体（详情页使用）
  autoOpen: { type: Boolean, default: false },
  // 信息窗体是否显示「查看详情」按钮（详情页自身已是详情，隐藏）
  showViewBtn: { type: Boolean, default: true },
  // 路线规划模式：数字编号标记 + 真实/估算路线绘制（动态规划路线页使用）
  route: { type: Boolean, default: false },
  // 交通模式：driving 驾车（高德真实规划）；hsr 高铁 / train 火车 / flight 飞机（纯前端速度模型估算）
  mode: { type: String, default: 'driving' },
  // 自定义出发地 { name, lng, lat }：渲染旗帜标记，并作为路线规划的真正起点（驾车/估算均从它出发）
  startPoint: { type: Object, default: null },
  // 地图选点模式：开启后点击地图 emit('mapClick', { lng, lat })，供规划页设定出发地
  pickPoint: { type: Boolean, default: false }
})
const emit = defineEmits(['view', 'routeinfo', 'mapClick'])

const configured = isAmapConfigured()

const mapEl = ref(null)
const busy = ref(false)
const loadError = ref(false)
const planning = ref(false)        // 驾车路径规划请求进行中
const routeFallback = ref(false)   // 规划失败退化为直线时提示
const placedCount = ref(0)

let amap = null
let map = null
let markers = []
let infoWindow = null
let routeLine = null
let driving = null      // 高德驾车路径规划服务实例（复用）
let planningSeq = 0     // 规划请求序号：景点快速增删/排序时丢弃过期结果
let routePoints = []    // 当前路线景点坐标（交通模式切换时重规划用）
let destroyed = false

// 取景点坐标：直接用数据自带的 longitude/latitude（兼容 lng/lat 别名）。
// 不是有效数值、或为 0（未填写的默认值）都视为无坐标，返回 null —— 保证不会有 NaN 进入高德。
function coordOf(spot) {
  if (!spot) return null
  const lng = Number(spot.longitude ?? spot.lng)
  const lat = Number(spot.latitude ?? spot.lat)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  if (lng === 0 && lat === 0) return null
  return [lng, lat]
}

async function initMap() {
  if (!configured) return
  loadError.value = false
  busy.value = true
  try {
    if (map) {
      try { map.destroy() } catch (e) { /* 忽略重复销毁 */ }
      map = null
    }
    amap = await loadAMap()
    if (destroyed) return
    // 有坐标时，地图初始化就直接定位到该景点（详情页单景点场景），不闪城市全景
    const first = coordOf(props.spots?.[0])
    map = new amap.Map(mapEl.value, first ? { center: first, zoom: 15 } : { zoom: 8 })
    // 地图选点：点击地图把坐标抛给父组件（规划页设为出发地）
    map.on('click', (e) => {
      if (!props.pickPoint || !e?.lnglat) return
      emit('mapClick', { lng: e.lnglat.getLng(), lat: e.lnglat.getLat() })
    })
    if (props.pickPoint) map.setDefaultCursor('crosshair')
    renderMarkers()
  } catch (err) {
    // key 错误 / 网络失败 / 脚本加载失败：显示可重试的失败态
    loadError.value = true
    console.error('地图初始化失败:', err)
  } finally {
    if (!destroyed) busy.value = false
  }
}

function retryLoad() {
  loadError.value = false
  initMap()
}

function renderMarkers() {
  if (!map) return
  clearMarkers()
  placedCount.value = 0

  // 只保留有有效坐标的景点（纯同步，不做地理编码）
  const placed = (props.spots || [])
    .map(spot => ({ spot, lngLat: coordOf(spot) }))
    .filter(p => p.lngLat)

  if (!placed.length) return

  // 路线规划模式：数字编号标记 + 按顺序连线，不弹信息窗体
  if (props.route) {
    renderRoute(placed)
    return
  }

  placed.forEach(({ spot, lngLat }) => {
    try {
      const marker = new amap.Marker({
        position: lngLat,
        offset: new amap.Pixel(-12, -34),
        title: spot?.name || ''
      })
      marker.on('click', () => openInfo(spot, marker))
      marker.setMap(map)
      markers.push(marker)
    } catch (e) {
      console.warn('景点标记创建失败，已跳过:', spot?.name, e)
    }
  })

  placedCount.value = markers.length
  if (markers.length) {
    try {
      // 单景点（详情页）：直接缩放到景点所在位置，像高德搜索定位一样；多景点才用全景缩放
      // setZoomAndCenter(zoom, center)：zoom 在前、center 在后（见 skill map.md）
      if (placed.length === 1) {
        map.setZoomAndCenter(15, placed[0].lngLat)
      } else {
        map.setFitView(markers, false, [90, 90, 90, 90])
      }
      // 详情页：放置后自动弹出这个景点的信息窗体
      if (props.autoOpen) openInfo(placed[0].spot, markers[0])
    } catch (e) {
      console.warn('地图定位失败:', e)
    }
  }
}

// 路线规划模式：起点旗帜标记（如有）+ 数字编号标记 + 高德驾车路径规划真实路线
function renderRoute(placed) {
  try {
    // 自定义出发地：旗帜标记，视觉上区别于数字编号的景点标记
    const startCoord = coordOf(props.startPoint)
    if (startCoord) {
      const startMarker = new amap.Marker({
        position: startCoord,
        content: `<div class="route-start-marker" title="${escapeHtml(props.startPoint?.name || '出发地')}"><i class="fa fa-flag"></i></div>`,
        offset: new amap.Pixel(-14, -14),
        zIndex: 130
      })
      startMarker.setMap(map)
      markers.push(startMarker)
    }

    placed.forEach(({ spot, lngLat }, i) => {
      const num = i + 1
      const marker = new amap.Marker({
        position: lngLat,
        content: `
          <div class="route-num-marker" ${i === 0 ? 'data-start' : ''}>
            <span>${num}</span>
          </div>`,
        offset: new amap.Pixel(-14, -14),
        title: spot?.name || ''
      })
      marker.on('click', () => openInfo(spot, marker))
      marker.setMap(map)
      markers.push(marker)
    })

    placedCount.value = markers.length
    routeFallback.value = false
    // 起点（自定义出发地或第一个景点）作为路线的第一个点，里程/耗时均从它开始计算
    routePoints = startCoord ? [startCoord, ...placed.map(p => p.lngLat)] : placed.map(p => p.lngLat)

    // 至少两个点才规划路线
    if (routePoints.length >= 2) {
      planRouteByMode()
    } else {
      routePoints = []
      map.setFitView(markers, false, [90, 90, 90, 90])
    }
  } catch (e) {
    console.warn('路线绘制失败:', e)
  }
}

// 按当前交通模式规划：驾车走高德真实规划，高铁/火车/飞机走纯前端估算
function planRouteByMode() {
  if (props.mode === 'driving') {
    planDrivingRoute([...routePoints])
  } else {
    planEstimateRoute([...routePoints])
  }
}

// 交通模式切换：清掉旧路线绘制与状态（保留景点标记），再按新模式重规划
watch(() => props.mode, () => {
  if (!map || routePoints.length < 2) return
  clearRouteDrawing()
  planRouteByMode()
})

// 出发地变更：重绘标记与路线；选点模式切换：同步鼠标指针
watch(() => props.startPoint, () => {
  if (map && props.route) renderMarkers()
})

watch(() => props.pickPoint, (on) => {
  if (map) map.setDefaultCursor(on ? 'crosshair' : 'default')
})

// 模式切换清理：移除旧线并复位状态（不动作进行中的规划，新规划自身的 seq 会使其失效）
function clearRouteDrawing() {
  planning.value = false
  routeFallback.value = false
  emit('routeinfo', null)
  if (routeLine) { map.remove(routeLine); routeLine = null }
}

// 驾车路径规划：按访问顺序走真实道路（插件加载见 skill routing.md）
// 单次 search 最多 1 起点 + 16 途经点，这里每段取 12 个点留足余量；
// 超出时按 11 步长分段接力规划（相邻段共享衔接点），失败退化为直线连接
async function planDrivingRoute(points) {
  const seq = ++planningSeq
  planning.value = true
  try {
    const svc = await ensureDriving()
    if (seq !== planningSeq || destroyed) return

    const allPaths = []
    let distance = 0  // 总里程（米），各分段累加
    let time = 0      // 总耗时（秒），各分段累加
    for (let i = 0; i < points.length - 1; i += 11) {
      const seg = points.slice(i, i + 12)
      const result = await searchDriving(svc, seg)
      if (seq !== planningSeq || destroyed) return
      const route = result?.routes?.[0]
      if (!route?.steps?.length) throw new Error('no_data')
      distance += route.distance || 0
      time += route.time || 0
      route.steps.forEach(step => step.path && allPaths.push(...step.path))
    }
    drawRouteLine(allPaths)
    // 规划成功：把总里程/耗时上报给页面展示
    emit('routeinfo', { distance, time })
  } catch (e) {
    console.warn('驾车路线规划失败，退化为直线连接:', e)
    if (seq !== planningSeq || destroyed) return
    routeFallback.value = true
    drawRouteLine(points)
    emit('routeinfo', null)  // 直线兜底没有真实里程，置空
  } finally {
    if (seq === planningSeq) planning.value = false
  }
}

// 动态加载 AMap.Driving 插件并复用实例（不传 map：结果由回调返回后自行绘制，保持平台配色）
function ensureDriving() {
  return new Promise((resolve, reject) => {
    if (driving) return resolve(driving)
    amap.plugin('AMap.Driving', () => {
      try {
        driving = new amap.Driving({
          policy: amap.DrivingPolicy.LEAST_TIME,
          extensions: 'base'
        })
        resolve(driving)
      } catch (e) {
        reject(e)
      }
    })
  })
}

// Driving.search 回调转 Promise：complete → result；error / no_data → reject
function searchDriving(svc, seg) {
  return new Promise((resolve, reject) => {
    svc.search(seg[0], seg[seg.length - 1], { waypoints: seg.slice(1, -1) }, (status, result) => {
      if (status === 'complete') resolve(result)
      else reject(new Error(status || 'route_error'))
    })
  })
}

// 绘制路线：规划成功时 path 为各 steps 拼接的 LngLat 数组；兜底时为景点坐标直线
function drawRouteLine(path) {
  if (routeLine) { map.remove(routeLine); routeLine = null }
  if (!path || path.length < 2) return
  routeLine = new amap.Polyline({
    path,
    strokeColor: '#0D9488',
    strokeOpacity: 0.9,
    strokeWeight: 6,
    strokeStyle: 'solid',
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true,   // 沿路径白色方向箭头（宽度 > 6 建议，见 skill vector-graphics.md）
    zIndex: 30
  })
  map.add(routeLine)
  map.setFitView([routeLine, ...markers], false, [90, 90, 90, 90])
}

// 高铁/火车/飞机：纯前端估算（无真实时刻数据）——虚线直线连接 + 速度模型估里程耗时
const EST_STYLES = {
  hsr:    { color: '#2563EB' },  // 高铁蓝
  train:  { color: '#F59E0B' },  // 普速琥珀
  flight: { color: '#8B5CF6' }   // 航空紫
}

function planEstimateRoute(points) {
  const style = EST_STYLES[props.mode] || EST_STYLES.hsr
  let distance = 0
  let time = 0
  for (let i = 0; i < points.length - 1; i++) {
    const leg = estimateLeg(props.mode, points[i], points[i + 1])
    distance += leg.distance
    time += leg.duration
  }
  drawEstimateLine(points, style)
  emit('routeinfo', { distance, time, estimated: true })
}

function drawEstimateLine(path, style) {
  if (routeLine) { map.remove(routeLine); routeLine = null }
  routeLine = new amap.Polyline({
    path,
    strokeColor: style.color,
    strokeOpacity: 0.9,
    strokeWeight: 4,
    strokeStyle: 'dashed',  // 虚线：示意线（非实际道路走向）
    lineJoin: 'round',
    lineCap: 'round',
    zIndex: 30
  })
  map.add(routeLine)
  map.setFitView([routeLine, ...markers], false, [90, 90, 90, 90])
}

function clearMarkers() {
  planningSeq++   // 使进行中的规划结果失效
  planning.value = false
  routeFallback.value = false
  routePoints = []
  emit('routeinfo', null)
  markers.forEach(m => m.setMap(null))
  markers = []
  if (routeLine) { map.remove(routeLine); routeLine = null }
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;')
}

function openInfo(spot, marker) {
  const price = spot.price == null || spot.price === 0 ? '免费' : `¥${spot.price}`
  const score = spot.score != null ? Number(spot.score).toFixed(1) : '--'
  const cover = resolveUploadUrl(spot.cover)
  const img = cover
    ? `<img class="mi-img" src="${escapeHtml(cover)}" onerror="this.style.display='none'" />`
    : '<div class="mi-img mi-fallback"><i class="fa fa-map-marker"></i></div>'

  const viewBtn = props.showViewBtn
    ? '<button type="button" class="mi-btn" data-view>查看详情</button>'
    : ''

  const html = `
    <div class="mi">
      ${img}
      <div class="mi-body">
        <p class="mi-name">${escapeHtml(spot.name)}</p>
        <p class="mi-meta">
          <span class="mi-score"><i class="fa fa-star"></i> ${escapeHtml(score)}</span>
          <span class="mi-price">${escapeHtml(price)}</span>
        </p>
        <p class="mi-addr"><i class="fa fa-map-marker"></i> ${escapeHtml(spot.address || '暂无地址')}</p>
        ${viewBtn}
      </div>
    </div>`

  if (!infoWindow) infoWindow = new amap.InfoWindow({ offset: new amap.Pixel(0, -34) })
  // 传 DOM 元素而不是字符串：字符串方式的 getContent() 拿不到节点，无法给按钮绑事件
  const el = document.createElement('div')
  el.innerHTML = html
  infoWindow.setContent(el)
  // open(map, position)：position 传坐标（marker.getPosition() 返回 LngLat），见 skill info-window.md
  infoWindow.open(map, marker.getPosition())
  if (props.showViewBtn) {
    el.querySelector('[data-view]')?.addEventListener('click', () => emit('view', spot))
  }
}

watch(() => props.spots, () => {
  if (map) renderMarkers()
})

onMounted(initMap)
onBeforeUnmount(() => {
  destroyed = true
  clearMarkers()
  if (infoWindow) { infoWindow.close(); infoWindow = null }
  if (driving) { driving.clear?.(); driving = null }
  if (map) { map.destroy(); map = null }
})
</script>

<style scoped>
.scenic-map {
  position: relative;
  width: 100%;
  min-height: 360px;
  border-radius: 12px;
  overflow: hidden;
  background: #F1F5F9;
}

.map-container {
  width: 100%;
}

.map-mask {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.75);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  z-index: 2;
}

.map-mask-text {
  font-size: 13px;
  color: #64748B;
  margin: 0;
}

.map-error {
  position: absolute;
  inset: 0;
  z-index: 3;
  background: rgba(255, 255, 255, 0.94);
  overflow-y: auto;
}

.route-hint {
  position: absolute;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #99F6E4;
  color: #0F766E;
  font-size: 13px;
  padding: 8px 16px;
  border-radius: 999px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  z-index: 3;
  white-space: nowrap;
}

/* 信息窗体（AMap 内部元素用全局样式，放在 :deep 下） */
:deep(.mi) {
  min-width: 200px;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

:deep(.mi-img) {
  width: 100%;
  height: 110px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 10px;
}

:deep(.mi-fallback) {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #E2E8F0;
  color: #94A3B8;
  font-size: 22px;
}

:deep(.mi-name) {
  font-size: 14px;
  font-weight: 700;
  color: #1E293B;
  margin: 0 0 6px;
}

:deep(.mi-meta) {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 6px;
}

:deep(.mi-score) {
  font-size: 13px;
  font-weight: 600;
  color: #D97706;
}

:deep(.mi-score .fa-star) { color: #F59E0B; }

:deep(.mi-price) {
  font-size: 14px;
  font-weight: 700;
  color: #059669;
}

:deep(.mi-addr) {
  font-size: 12px;
  color: #94A3B8;
  margin: 0 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

:deep(.mi-btn) {
  width: 100%;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #1CB89D, #0D9488);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

:deep(.mi-btn:hover) { opacity: 0.9; }

/* 路线规划：数字编号标记（content 注入到地图容器，需 :deep） */
:deep(.route-num-marker) {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1CB89D, #0D9488);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.45);
  cursor: pointer;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 出发地旗帜标记：深色底 + 白旗，与景点编号标记区分 */
:deep(.route-start-marker) {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #0D1B2A;
  color: #7AE2CC;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #7AE2CC;
  box-shadow: 0 2px 10px rgba(13, 27, 42, 0.5);
  cursor: pointer;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>
