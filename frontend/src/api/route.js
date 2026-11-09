import request from './request'

// 分页查询路线
export function getRoutesByPage(params) {
  return request({
    url: 'route/queryByPage',
    method: 'get',
    params
  })
}

// 查询路线详情（用户端）——后端按通用 CRUD 提供 queryById
export function getRouteDetail(id) {
  return request({
    url: `route/queryById/${id}`,
    method: 'get'
  })
}

// 添加路线
export function addRoute(data) {
  return request({
    url: 'route/add',
    method: 'post',
    data
  })
}

// 修改路线
export function editRoute(data) {
  return request({
    url: 'route/edit',
    method: 'post',
    data
  })
}

// 删除路线（逻辑删除）
export function deleteRoute(id) {
  return request({
    url: `route/deleteById/${id}`,
    method: 'post'
  })
}
