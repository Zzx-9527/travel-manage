import request from './request'

// 用户注册：后端无独立 register 接口，复用通用 travelUser/add（添加用户即注册）
// 默认 userRole=0 普通用户、status=1 启用（注册账号默认可直接登录）
// 头像为选填：调用方填了用调用方的，没填/留空走系统默认头像
export function register(data) {
  const { avatar, ...rest } = data || {}
  return request({
    url: 'travelUser/add',
    method: 'post',
    data: {
      userRole: 0,
      status: 1,
      avatar: avatar || 'https://picsum.photos/200/200?random=1',
      ...rest
    }
  })
}

// 我的个人资料（有 token 按 token 解析，否则可传 ?id=）
export function getMyProfile(params) {
  return request({
    url: 'travelUser/myProfile',
    method: 'get',
    params
  })
}

// 更新个人资料（§2.1 自助接口，登录即可）：身份取 token，只传白名单字段 nickname/phone/avatar。
// /travelUser/edit 已收紧为管理员专属，普通用户用它改自己会 403，勿再使用。
export function updateProfile(data) {
  return request({
    url: 'travelUser/updateProfile',
    method: 'post',
    data
  })
}

// 修改密码（§3，后端已提供独立接口并校验原密码）：身份取 token，不需要传 id。
// 原密码错返回 HTTP 200 + { code: 0, msg: "原密码不正确或用户不存在" }，由拦截器统一弹错并 reject；
// 改密成功后不踢已有会话，前端保持当前登录状态即可。
export function updatePassword({ oldPassword, newPassword }) {
  return request({
    url: 'travelUser/updatePassword',
    method: 'post',
    data: { oldPassword, newPassword }
  })
}
