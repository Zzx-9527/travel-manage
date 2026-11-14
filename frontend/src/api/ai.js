import request from './request'
import { API_BASE_URL } from '@/config'

/**
 * AI 行程规划接口。
 *
 * 同步版走统一的 axios 封装；流式版必须用原生 fetch，原因有两个：
 * 1. EventSource 不支持自定义请求头，无法携带 Authorization；
 * 2. axios（浏览器端基于 XHR）拿不到流式响应体，只能等整个响应结束。
 */

/** 查询 AI 是否就绪 + 今日剩余次数 */
export function fetchAiHealth() {
  return request.get('ai/health')
}

/** 同步生成（备用：流式不可用时降级） */
export function planTrip(data) {
  return request.post('ai/plan', data)
}

/** 把生成结果保存为"我的路线" */
export function savePlan(data) {
  return request.post('ai/plan/save', data)
}

/** 管理端：AI 用量看板（仅 admin 可访问） */
export function fetchAiStats() {
  return request.get('ai/stats')
}

/**
 * 流式生成。
 *
 * @param {Object} payload 目的地/天数/预算/偏好/补充
 * @param {Object} handlers
 *   signal     AbortSignal，用于取消
 *   onDelta    (text) => void        增量文本
 *   onPlan     (plan) => void        校验后的结构化行程
 *   onDone     (meta) => void        完成（含耗时）
 *   onError    (msg) => void         业务错误（配额用完、模型不可用等）
 */
export async function planTripStream(payload, handlers = {}) {
  const { signal, onDelta, onPlan, onDone, onError } = handlers
  const token = localStorage.getItem('travel_admin_token') || sessionStorage.getItem('travel_admin_token')

  let response
  try {
    response = await fetch(`${API_BASE_URL}ai/plan/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(payload),
      signal
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('网络异常，无法连接 AI 服务')
  }

  if (response.status === 401) {
    throw new Error('登录已过期，请重新登录')
  }
  if (!response.ok || !response.body) {
    throw new Error(`AI 服务返回异常（HTTP ${response.status}）`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const dispatch = (rawFrame) => {
    let event = 'message'
    const dataLines = []
    rawFrame.split('\n').forEach((line) => {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
    })
    if (!dataLines.length) return

    let data = dataLines.join('\n')
    try {
      data = JSON.parse(data)
    } catch {
      // 非 JSON 的 data 直接按字符串处理
    }

    if (event === 'delta') onDelta?.(data.text || '')
    else if (event === 'plan') onPlan?.(data)
    else if (event === 'done') onDone?.(data)
    else if (event === 'error') onError?.(data.msg || '生成失败')
  }

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    // 统一换行符，避免服务端用 \r\n 时切不出帧
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    let index
    while ((index = buffer.indexOf('\n\n')) >= 0) {
      dispatch(buffer.slice(0, index))
      buffer = buffer.slice(index + 2)
    }
  }
  if (buffer.trim()) dispatch(buffer)
}
