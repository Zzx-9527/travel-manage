import request from './request'

// 分页查询攻略
export function getStrategiesByPage(params) {
  return request({
    url: 'strategy/queryByPage',
    method: 'get',
    params
  })
}

// 查询攻略详情（用户端）——后端按通用 CRUD 提供 queryById
export function getStrategyDetail(id) {
  return request({
    url: `strategy/queryById/${id}`,
    method: 'get'
  })
}

// 添加攻略（用户发布：userId + 默认待审核）
export function addStrategy(data) {
  return request({
    url: 'strategy/add',
    method: 'post',
    data
  })
}

// 修改攻略（编辑审核状态等）
export function editStrategy(data) {
  return request({
    url: 'strategy/edit',
    method: 'post',
    data
  })
}

// 删除攻略（逻辑删除）
export function deleteStrategy(id) {
  return request({
    url: `strategy/deleteById/${id}`,
    method: 'post'
  })
}
