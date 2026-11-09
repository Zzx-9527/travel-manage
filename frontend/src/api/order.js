import request from './request'

// 分页查询订单（普通用户后端只返回本人订单）
export function getOrdersByPage(params) {
  return request({
    url: 'scenicOrders/queryByPage',
    method: 'get',
    params
  })
}

// 创建订单（用户端购票）：用户、金额、状态、订单号全部由后端决定
export function createOrder(data) {
  return request({
    url: 'scenicOrders/add',
    method: 'post',
    data
  })
}

// 我的订单列表（用户端）：后端按 token 过滤，无需再按 userId 筛选
export function getMyOrders(params) {
  return request({
    url: 'scenicOrders/queryByPage',
    method: 'get',
    params
  })
}

// 模拟支付：仅本人、待支付且创建未满 30 分钟
export function payOrder(id) {
  return request({
    url: `scenicOrders/pay/${id}`,
    method: 'post'
  })
}

// 取消订单：本人或管理员，取消后余票回补，订单保留
export function cancelOrder(id) {
  return request({
    url: `scenicOrders/cancel/${id}`,
    method: 'post'
  })
}

// 修改订单：后端只允许修改待支付订单的联系人
export function editOrder(data) {
  return request({
    url: 'scenicOrders/edit',
    method: 'post',
    data
  })
}

// 删除订单：待支付订单不可删除，需先取消
export function deleteOrder(id) {
  return request({
    url: `scenicOrders/deleteById/${id}`,
    method: 'post'
  })
}
