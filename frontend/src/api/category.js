import request from './request'

// 分页查询景点分类
export function getCategoriesByPage(params) {
  return request({
    url: 'scenicCategory/queryByPage',
    method: 'get',
    params
  })
}

// 添加景点分类
export function addCategory(data) {
  return request({
    url: 'scenicCategory/add',
    method: 'post',
    data
  })
}

// 修改景点分类
export function editCategory(data) {
  return request({
    url: 'scenicCategory/edit',
    method: 'post',
    data
  })
}

// 删除景点分类（逻辑删除）
export function deleteCategory(id) {
  return request({
    url: `scenicCategory/deleteById/${id}`,
    method: 'post'
  })
}
