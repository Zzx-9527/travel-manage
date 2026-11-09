import request from './request'

// 分页查询用户
export function getUsersByPage(params) {
  return request({
    url: 'travelUser/queryByPage',
    method: 'get',
    params
  })
}

// 添加用户
export function addUser(data) {
  return request({
    url: 'travelUser/add',
    method: 'post',
    data
  })
}

// 修改用户
export function editUser(data) {
  return request({
    url: 'travelUser/edit',
    method: 'post',
    data
  })
}

// 删除用户（逻辑删除）
export function deleteUser(id) {
  return request({
    url: `travelUser/deleteById/${id}`,
    method: 'post'
  })
}
