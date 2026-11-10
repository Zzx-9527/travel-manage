import request from './request'

// 全站公告约定：接收人为管理员（id=10）的消息都是系统公告（存库）；
// 横幅组件取其最新一条在用户端顶部展示
export const BANNER_ADMIN_ID = 10

// 分页查询消息（userId 传接收人用户ID数字；内部序列化为 userId.id=1 走 Spring 嵌套绑定）
// 额外支持 userIdNe（排除指定接收人）、typeNe（排除指定类型）、keyword（标题/昵称/账号联合搜索）
export function getMessagesByPage(params) {
  const { userId, userIdNe, typeNe, keyword, ...rest } = params || {}
  const query = { ...rest }
  if (userId != null) query['userId.id'] = userId
  if (userIdNe != null) query.userIdNe = userIdNe
  if (typeNe != null) query.typeNe = typeNe
  if (keyword != null && keyword !== '') query.keyword = keyword
  return request({
    url: 'message/queryByPage',
    method: 'get',
    params: query
  })
}

// 标记已读：单条 { id } 或批量 { ids: [..] }
export function markMessageRead(data) {
  return request({
    url: 'message/markRead',
    method: 'post',
    data
  })
}

// 未读数量（铃铛小红点）：后端只统计当前登录人，不收参数（§4.3，传了也会被忽略）
export function getMessageUnreadCount() {
  return request({
    url: 'message/unreadCount',
    method: 'get'
  })
}

// 新增消息（管理员发通知）：data = { userId: { id }, type, title, content }，userId 必须是对象
export function addMessage(data) {
  return request({
    url: 'message/add',
    method: 'post',
    data
  })
}

// 详情 / 编辑 / 删除
export function getMessageById(id) {
  return request({
    url: `message/queryById/${id}`,
    method: 'get'
  })
}

export function editMessage(data) {
  return request({
    url: 'message/edit',
    method: 'post',
    data
  })
}

export function deleteMessage(id) {
  return request({
    url: `message/deleteById/${id}`,
    method: 'post'
  })
}
