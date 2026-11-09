import request from './request'

// 后台首页统计：用户/景点/攻略/订单总数
export function getStats() {
  return request({
    url: 'dashboard/stats',
    method: 'get'
  })
}
