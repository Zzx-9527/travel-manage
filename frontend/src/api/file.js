import request from './request'
import { ASSET_BASE_URL } from '@/config'

// 文件上传（§10）：POST file/upload，multipart/form-data，字段名固定 file。
// 需登录（普通用户即可）；后端限制：单文件 ≤ 10MB，仅 jpg/jpeg/png/gif/webp。
export function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: 'file/upload',
    method: 'post',
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'gif', 'webp']
const MAX_SIZE_MB = 10

// 本地预校验（后端还会再校验后缀 + Content-Type）：返回错误文案，null 表示通过
export function imageFileError(file) {
  if (!file) return '未选择文件'
  const ext = String(file.name || '').split('.').pop().toLowerCase()
  if (!ALLOWED_EXT.includes(ext)) return '只支持 jpg / jpeg / png / gif / webp 图片'
  if (file.type && !file.type.startsWith('image/')) return '只支持图片文件'
  if (file.size > MAX_SIZE_MB * 1024 * 1024) return `图片大小不能超过 ${MAX_SIZE_MB}MB`
  return null
}

// 上传返回的 url 是相对路径（/travel/uploads/...），按当前环境拼上后端域名才可直接访问。
// 库里现有头像/封面均为绝对 URL（<img :src> 直接渲染），故入库统一存绝对地址。
// 注意：外部手填的 URL 不受后端「单图覆盖删旧图」管理（deleteByUrl 只认 /travel/uploads/ 下的文件），
// 这是安全行为而非 bug。
export function resolveUploadUrl(url) {
  if (!url || /^https?:\/\//i.test(url)) return url
  return ASSET_BASE_URL + url
}
