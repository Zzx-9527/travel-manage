// 动态规划路线 —— 纯前端工具（不新增后端 API）
// 职责：1) 球面距离计算（Haversine） 2) 最近邻贪心算法求访问顺序 3) 长途交通（高铁/火车/飞机）纯前端估算

const EARTH_RADIUS = 6371000 // 地球平均半径（米）

// 两点球面距离（米）。输入 [lng, lat]
export function haversine(a, b) {
  const toRad = (deg) => (deg * Math.PI) / 180
  const dLng = toRad(b[0] - a[0])
  const dLat = toRad(b[1] - a[1])
  const lat1 = toRad(a[1])
  const lat2 = toRad(b[1])
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * EARTH_RADIUS * Math.asin(Math.sqrt(h))
}

// 相邻景点间的距离表（米）。indexes 与 coords 等长对应
function buildDistanceMatrix(coords) {
  const n = coords.length
  const m = Array.from({ length: n }, () => new Array(n).fill(0))
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const d = haversine(coords[i], coords[j])
      m[i][j] = d
      m[j][i] = d
    }
  }
  return m
}

/**
 * 最近邻贪心算法求访问顺序。
 * 从 startIndex（默认第一个景点）出发，每次取当前点距离最近的未访问景点，
 * 返回重排后的索引数组。O(n²)，对景点规模完全够用。
 *
 * @param {Array<number[]>} coords 景点坐标数组 [[lng, lat], ...]
 * @param {number} [startIndex=0] 起点索引
 * @returns {number[]} 访问顺序的索引数组
 */
export function planRouteOrder(coords, startIndex = 0) {
  const n = coords.length
  if (n <= 2) return coords.map((_, i) => i)

  const dist = buildDistanceMatrix(coords)
  const visited = new Array(n).fill(false)
  const order = [startIndex]
  visited[startIndex] = true

  let current = startIndex
  for (let step = 1; step < n; step++) {
    let best = -1
    let bestDist = Infinity
    for (let j = 0; j < n; j++) {
      if (!visited[j] && dist[current][j] < bestDist) {
        bestDist = dist[current][j]
        best = j
      }
    }
    order.push(best)
    visited[best] = true
    current = best
  }
  return order
}

/**
 * 带外部起点的最近邻排序：起点不在景点列表中（定位/地图选点/搜索地点）时，
 * 从 startCoord 出发，每次取最近的未访问景点，返回重排后的索引数组（不含起点本身）。
 *
 * @param {Array<number[]>} coords 景点坐标数组 [[lng, lat], ...]
 * @param {number[]} startCoord 外部起点坐标 [lng, lat]
 * @returns {number[]} 访问顺序的索引数组
 */
export function planRouteOrderFrom(coords, startCoord) {
  const n = coords.length
  if (n === 0) return []
  if (n <= 2) return coords.map((_, i) => i)

  const dist = buildDistanceMatrix(coords)
  const visited = new Array(n).fill(false)

  // 第一站取离外部起点最近的景点
  let first = 0
  let firstDist = Infinity
  for (let j = 0; j < n; j++) {
    const d = haversine(startCoord, coords[j])
    if (d < firstDist) {
      firstDist = d
      first = j
    }
  }

  const order = [first]
  visited[first] = true

  let current = first
  for (let step = 1; step < n; step++) {
    let best = -1
    let bestDist = Infinity
    for (let j = 0; j < n; j++) {
      if (!visited[j] && dist[current][j] < bestDist) {
        bestDist = dist[current][j]
        best = j
      }
    }
    order.push(best)
    visited[best] = true
    current = best
  }
  return order
}

/**
 * 长途交通估算模型（纯前端，无真实时刻数据）。
 * 按两点直线距离用经验速度折算，供高铁/火车/飞机模式展示「预估」耗时。
 *
 * 三种方式统一「门到门」口径，公式均为：耗时 = 里程 ÷ 巡航速度 + 固定缓冲，
 * 固定缓冲都包含「往返场站 + 安检候车」（飞机额外含起降滑行）：
 * - 高铁：地面里程（直线 ×1.2 路网系数）÷ 250km/h（含停靠折减）+ 40min 固定缓冲
 * - 火车：地面里程 ÷ 90km/h（普速含停站）+ 50min 固定缓冲
 * - 飞机：空中里程（直线 ×1.1）÷ 750km/h + 150min 固定缓冲
 * （飞机缓冲远大于高铁/火车是符合现实的：机场远、安检候机久，这正是中短途高铁更快的原因）
 *
 * @param {string} mode 'hsr' | 'train' | 'flight'
 * @param {number[]} a 起点 [lng, lat]
 * @param {number[]} b 终点 [lng, lat]
 * @returns {{ distance: number, duration: number }} distance 米，duration 秒
 */
export function estimateLeg(mode, a, b) {
  const straight = haversine(a, b) // 直线距离（米）
  if (mode === 'flight') {
    return {
      distance: straight * 1.1,
      duration: (straight / 1000 / 750) * 3600 + 2.5 * 3600
    }
  }
  const ground = straight * 1.2
  if (mode === 'hsr') {
    return { distance: ground, duration: (ground / 1000 / 250) * 3600 + 40 * 60 }
  }
  if (mode === 'train') {
    return { distance: ground, duration: (ground / 1000 / 90) * 3600 + 50 * 60 }
  }
  return { distance: ground, duration: 0 } // 未知模式：只给里程不估时
}
