import request from './request'

// 分页查询路线途经景点
export function getRouteDetailsByPage(params) {
  return request({
    url: 'routeDetail/queryByPage',
    method: 'get',
    params
  })
}

// 添加途经景点
export function addRouteDetail(data) {
  return request({
    url: 'routeDetail/add',
    method: 'post',
    data
  })
}

// 修改途经景点
export function editRouteDetail(data) {
  return request({
    url: 'routeDetail/edit',
    method: 'post',
    data
  })
}

// 删除途经景点
export function deleteRouteDetail(id) {
  return request({
    url: `routeDetail/deleteById/${id}`,
    method: 'post'
  })
}
