import request from './request'

// 分页查询评论
export function getCommentsByPage(params) {
  return request({
    url: 'strategyComment/queryByPage',
    method: 'get',
    params
  })
}

// 发表评论（用户端）——后端已实现 strategyComment/add
// 入参按后端实体结构：{ strategyId:{id}, userId:{id}, content, rating }，落库 status=0 待审核
export function addComment(data) {
  return request({
    url: 'strategyComment/add',
    method: 'post',
    data
  })
}

// 按攻略查询评论（用户端）——后端通用 CRUD 用 queryByPage，页面内按 strategyId 过滤
export function getCommentsByStrategy(params) {
  return request({
    url: 'strategyComment/queryByPage',
    method: 'get',
    params
  })
}

// 我的评论列表（用户端）——后端通用 CRUD 用 queryByPage，页面内按 userId 过滤
export function getMyComments(params) {
  return request({
    url: 'strategyComment/queryByPage',
    method: 'get',
    params
  })
}

// 删除我的评论（用户端）——通用逻辑删除
export function deleteMyComment(id) {
  return request({
    url: `strategyComment/deleteById/${id}`,
    method: 'post'
  })
}

// 修改评论（审核状态等）
export function editComment(data) {
  return request({
    url: 'strategyComment/edit',
    method: 'post',
    data
  })
}

// 删除评论
export function deleteComment(id) {
  return request({
    url: `strategyComment/deleteById/${id}`,
    method: 'post'
  })
}
