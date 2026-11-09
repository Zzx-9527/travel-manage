import request from './request'

// 收藏（targetType：1攻略 2景点 3路线）
// 收藏人由后端按 token 识别（§9）：请求体只需 { targetType, targetId }，userId 传了也会被忽略
export function addCollect(data) {
  return request({
    url: 'collect/add',
    method: 'post',
    data
  })
}

// 我的收藏列表：普通用户后端强制只查自己，collectorId 不再需要（§9；管理员可传）
export function getMyFavorites(params) {
  return request({
    url: 'collect/queryByPage',
    method: 'get',
    params
  })
}

// 判断是否已收藏（§6.3）：targetType + targetId 即可，收藏人取当前登录人
export function checkCollect(params) {
  return request({
    url: 'collect/checkCollected',
    method: 'get',
    params
  })
}

// 取消收藏（按目标；收藏人取当前登录人）
export function deleteCollectByTarget(data) {
  return request({
    url: 'collect/deleteByUserAndTarget',
    method: 'post',
    data
  })
}

// 管理端分页查询
export function getCollectsByPage(params) {
  return request({
    url: 'collect/queryByPage',
    method: 'get',
    params
  })
}

// 管理端删除
export function deleteCollect(id) {
  return request({
    url: `collect/deleteById/${id}`,
    method: 'post'
  })
}
