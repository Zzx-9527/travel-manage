/**
 * 运行环境统一配置
 *
 * - API_BASE_URL：axios 请求基地址（含上下文路径，末尾带 /）
 * - ASSET_BASE_URL：上传图片等静态资源基地址（仅域名/源，末尾不带 /）
 *
 * 优先读 Vite 环境变量（.env.development / .env.production / .env.local），
 * 未配置时按开发/生产环境给出兜底值，避免把 localhost 写死在业务代码里。
 */
const trimTrailingSlash = (value) => String(value || '').replace(/\/+$/, '')

const envApiBase = import.meta.env.VITE_API_BASE_URL
const envAssetBase = import.meta.env.VITE_ASSET_BASE_URL

const defaultApiBase = import.meta.env.DEV ? 'http://localhost:8081/travel' : '/travel'
const defaultAssetBase = import.meta.env.DEV ? 'http://localhost:8081' : ''

export const API_BASE_URL = `${trimTrailingSlash(envApiBase || defaultApiBase)}/`
export const ASSET_BASE_URL = trimTrailingSlash(envAssetBase || defaultAssetBase)
