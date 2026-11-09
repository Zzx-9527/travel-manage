import request from './request'

// 分页查询订单
export function getOrdersByPage(params) {
  return request({
    url: 'scenicOrders/queryByPage',
    method: 'get',
    params
  })
}

// 创建订单（用户端购票）——后端通用 CRUD 用 add
export function createOrder(data) {
  return request({
    url: 'scenicOrders/add',
    method: 'post',
    data
  })
}

// 我的订单列表（用户端）——后端通用 CRUD 用 queryByPage，页面内按 userId 过滤
export function getMyOrders(params) {
  return request({
    url: 'scenicOrders/queryByPage',
    method: 'get',
    params
  })
}

// 订单支付（用户端）——无独立支付接口，走 edit 改 payStatus
export function payOrder(id) {
  return request({
    url: 'scenicOrders/edit',
    method: 'post',
    data: { id, payStatus: 1 }
  })
}

// 取消订单（用户端）直接调用 deleteOrder 删除（payStatus=2 的语义是「免费」，不能当已取消用）

// 修改订单（支付状态等）
export function editOrder(data) {
  return request({
    url: 'scenicOrders/edit',
    method: 'post',
    data
  })
}

// 删除订单
export function deleteOrder(id) {
  return request({
    url: `scenicOrders/deleteById/${id}`,
    method: 'post'
  })
}
