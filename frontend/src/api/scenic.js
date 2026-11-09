import request from './request'

// 分页查询景点
export function getSpotsByPage(params) {
  return request({
    url: 'scenic/queryByPage',
    method: 'get',
    params
  })
}

// 查询景点详情（用户端）——后端按通用 CRUD 提供 queryById
export function getSpotDetail(id) {
  return request({
    url: `scenic/queryById/${id}`,
    method: 'get'
  })
}

// 查询某景区某日的可售余票（silent：无库存/售罄是正常业务态，不弹全局错误）
export function getStock(scenicId, playDate) {
  return request({
    url: 'scenic/stock',
    method: 'get',
    params: { scenicId, playDate },
    silent: true
  })
}

// 添加景点
export function addSpot(data) {
  return request({
    url: 'scenic/add',
    method: 'post',
    data
  })
}

// 修改景点
export function editSpot(data) {
  return request({
    url: 'scenic/edit',
    method: 'post',
    data
  })
}

// 删除景点（逻辑删除）
export function deleteSpot(id) {
  return request({
    url: `scenic/deleteById/${id}`,
    method: 'post'
  })
}
