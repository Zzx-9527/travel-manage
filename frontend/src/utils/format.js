// 用户端通用格式化工具
// 与后台各管理页保持一致的状态样式与时间格式

// 时间：2024-01-15T08:30:00.0 → 2024-01-15 08:30:00
export function formatTime(dt) {
  if (!dt) return '--'
  return String(dt).replace(/\.0$/, '').replace('T', ' ')
}

// 价格：0 / null → 免费
export function priceText(p) {
  if (p === 0 || p == null) return '免费'
  return `¥${p}`
}

// 列表归一化：兼容 Spring Page（content.content）或裸数组
export function normalizeList(content) {
  return content?.content || content || []
}

export function truncate(text, max) {
  if (!text) return ''
  return text.length > max ? text.substring(0, max) + '...' : text
}

// ---- 订单状态（0待支付 1已支付 2已取消 3已完成）----
// 取消原因 TIMEOUT 单独展示，历史迁移原因也给出可读文案
export function orderStatusText(row) {
  const status = Number(row?.status)
  if (status === 2) {
    if (row?.cancelReason === 'TIMEOUT') return '已超时取消'
    if (row?.cancelReason === 'LEGACY_REFUNDED') return '历史退款（已取消）'
    if (row?.cancelReason === 'LEGACY_UNPAID') return '历史未支付（已取消）'
    return '已取消'
  }
  if (status === 1) return '已支付'
  if (status === 3) return '已完成'
  if (status === 0) return '待支付'
  return '未知'
}

export function orderStatusIcon(row) {
  const status = Number(row?.status)
  if (status === 2) return 'fa fa-ban'
  if (status === 1) return 'fa fa-check-circle'
  if (status === 3) return 'fa fa-flag-checkered'
  return 'fa fa-clock-o'
}

export function orderStatusStyle(row) {
  const status = Number(row?.status)
  if (status === 1) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  if (status === 3) return { background: '#EFF6FF', color: '#2563EB', border: '1px solid #BFDBFE' }
  if (status === 2) return { background: '#F1F5F9', color: '#64748B', border: '1px solid #E2E8F0' }
  return { background: '#FFFBEB', color: '#D97706', border: '1px solid #FDE68A' }
}

// ---- 审核状态（0待审核 1已通过 2已拒绝）----
export function auditText(s) {
  return s === 0 ? '待审核' : s === 1 ? '已通过' : s === 2 ? '已拒绝' : '未知'
}

export function auditIcon(s) {
  return s === 0 ? 'fa fa-clock-o' : s === 1 ? 'fa fa-check-circle' : s === 2 ? 'fa fa-times-circle' : 'fa fa-question-circle'
}

export function auditStyle(s) {
  if (s === 0) return { background: '#FFFBEB', color: '#D97706', border: '1px solid #FDE68A' }
  if (s === 1) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  if (s === 2) return { background: '#FEF2F2', color: '#DC2626', border: '1px solid #FECACA' }
  return { background: '#F1F5F9', color: '#64748B', border: '1px solid #E2E8F0' }
}

// ---- 收藏类型（1攻略 2景点 3路线）----
export function collectTypeText(t) {
  return t === 2 ? '景点' : t === 3 ? '路线' : '攻略'
}

export function collectTypeStyle(t) {
  if (t === 2) return { background: '#EFF6FF', color: '#2563EB', border: '1px solid #BFDBFE' }
  if (t === 3) return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
  return { background: '#FFFBEB', color: '#D97706', border: '1px solid #FDE68A' }
}

// 收藏条目归一化：按 targetType 取出实际目标对象（攻略/景点/路线）
// 新契约：列表不再内嵌攻略对象；类型2/3分别内嵌 scenicId/routeId，类型1仅 targetType+targetId
export function collectTarget(item) {
  const t = Number(item?.targetType) || 1
  if (t === 2 && item?.scenicId) {
    const s = item.scenicId
    return { id: s.id, type: 2, obj: s, name: s.name, cover: s.cover, stateKey: 'spot' }
  }
  if (t === 3 && item?.routeId) {
    const r = item.routeId
    return { id: r.id, type: 3, obj: r, name: r.name, cover: r.cover, stateKey: 'route' }
  }
  // 攻略：后台收藏接口已内嵌 strategyId（title/cover），用户端旧数据仍兼容 item.guide
  if (t === 1 && item?.strategyId) {
    const s = item.strategyId
    return { id: s.id, type: 1, obj: s, name: s.title, cover: s.cover, stateKey: 'guide' }
  }
  const g = item?.guide
  return { id: item?.targetId, type: 1, obj: g, name: g?.title, cover: g?.cover, stateKey: 'guide' }
}

// ---- 路线交通方式 ----
// 项目唯一枚举：顺序即发布表单的选项顺序
export const TRAFFIC_TYPES = ['地铁', '高铁', '飞机', '大巴', '自驾', '其他']

// 旧数据或非法值统一归入「其他」，保证所有页面展示一致
export function normalizeTraffic(traffic) {
  const value = String(traffic ?? '').trim()
  return TRAFFIC_TYPES.includes(value) ? value : '其他'
}

// 交通方式对应图标（Font Awesome 4.7）
export function trafficIcon(traffic) {
  switch (normalizeTraffic(traffic)) {
    case '高铁': return 'fa-train'
    case '地铁': return 'fa-subway'
    case '飞机': return 'fa-plane'
    case '大巴': return 'fa-bus'
    case '自驾': return 'fa-car'
    default: return 'fa-road'
  }
}

// 交通方式底色：只用于图标块和交通胶囊，标题保持白色
// 卡片渐变底色始终是品牌青，不随交通方式变化
const TRAFFIC_COLOR = {
  '地铁': '#4C7A93',
  '高铁': '#8E6B33',
  '飞机': '#63508F',
  '大巴': '#B87A1F',
  '自驾': '#B85A34',
  '其他': '#9C4553'
}

export function trafficColor(traffic) {
  return TRAFFIC_COLOR[normalizeTraffic(traffic)]
}

// ---- 交通方式彩色标签 ----
export function trafficStyle(traffic) {
  switch (normalizeTraffic(traffic)) {
    case '地铁': return { background: '#F0F9FF', color: '#0369A1', border: '1px solid #BAE6FD' }
    case '高铁': return { background: '#EFF6FF', color: '#2563EB', border: '1px solid #BFDBFE' }
    case '飞机': return { background: '#F5F3FF', color: '#7C3AED', border: '1px solid #DDD6FE' }
    case '大巴': return { background: '#FEFCE8', color: '#CA8A04', border: '1px solid #FEF08A' }
    case '自驾': return { background: '#F0FDF4', color: '#16A34A', border: '1px solid #BBF7D0' }
    default: return { background: '#F1F5F9', color: '#64748B', border: '1px solid #E2E8F0' }
  }
}
