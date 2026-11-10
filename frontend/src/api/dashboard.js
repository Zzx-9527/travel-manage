import request from './request'

// 后台首页统计：用户/景点/攻略/订单总数
export function getStats() {
  return request({
    url: 'dashboard/stats',
    method: 'get'
  })
}

// 后台首页饼图：景点分类分布 + 订单状态分布
export function getCharts() {
  return request({
    url: 'dashboard/charts',
    method: 'get'
  })
}
