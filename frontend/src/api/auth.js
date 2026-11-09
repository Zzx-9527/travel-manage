import request from './request'

export function login(data) {
  return request({
    url: 'travelUser/login',
    method: 'post',
    data
  })
}

// 登出（§8.4）：显式传 token——store 是先清本地再调本接口，届时拦截器已读不到 localStorage。
// silent：token 已失效时 401 分支不弹「token 无效」的误导提示，登出动作本身不该报错。
export function logout(token) {
  return request({
    url: 'travelUser/logout',
    method: 'post',
    headers: { Authorization: `Bearer ${token}` },
    silent: true
  })
}
