import { ElMessage } from 'element-plus'

// 功能降级提示：后端接口未实现时优雅提示，同一功能只提示一次，避免刷屏
const shown = new Set()

export function showDegraded(feature) {
  if (shown.has(feature)) return
  shown.add(feature)
  ElMessage.warning(`${feature}功能即将上线，敬请期待`)
}
