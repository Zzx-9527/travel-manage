import request from './request'
import { API_BASE_URL } from '@/config'

/**
 * AI 相关接口。
 *
 * 同步接口走统一 axios 封装；流式接口必须用原生 fetch，原因有两个：
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

/** 当前用户的生成历史 */
export function fetchPlanHistory(params = { page: 1, size: 6 }) {
  return request.get('ai/plan/history', { params })
}

/** 恢复某次生成结果（只读快照） */
export function fetchPlanHistoryDetail(id) {
  return request.get(`ai/plan/history/${id}`)
}

/** 对话历史（AI 助手悬浮窗） */
export function fetchChatHistory(limit = 30) {
  return request.get('ai/chat/history', { params: { limit } })
}

/** 清空对话 */
export function clearChatHistory() {
  return request.delete('ai/chat/history')
}

/** 把对话里生成的行程卡片写回对话记录，重开窗口仍能看到 */
export function appendChatPlan(title, plan) {
  return request.post('ai/chat/plan', { title, plan })
}

/**
 * POST 一个 SSE 接口，按帧回调。
 *
 * @param path    相对 API_BASE_URL 的路径
 * @param payload 请求体
 * @param options { signal, onEvent(event, data) }
 */
async function postSse(path, payload, { signal, onEvent } = {}) {
  const token = localStorage.getItem('travel_admin_token') || sessionStorage.getItem('travel_admin_token')

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
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
      // 非 JSON 的 data 按字符串处理
    }
    onEvent?.(event, data)
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

/**
 * 流式生成行程。
 *
 * @param handlers { signal, onDelta(text), onPlan(plan), onDone(meta), onError(msg) }
 */
export function planTripStream(payload, handlers = {}) {
  return postSse('ai/plan/stream', payload, {
    signal: handlers.signal,
    onEvent: (event, data) => {
      if (event === 'delta') handlers.onDelta?.(data.text || '')
      else if (event === 'plan') handlers.onPlan?.(data)
      else if (event === 'done') handlers.onDone?.(data)
      else if (event === 'error') handlers.onError?.(data.msg || '生成失败')
    }
  })
}

/**
 * 流式对话。
 *
 * @param message  用户这句话
 * @param handlers { signal, onDelta(text), onDone(meta), onError(msg) }
 */
export function chatStream(message, handlers = {}) {
  return postSse('ai/chat/stream', { message }, {
    signal: handlers.signal,
    onEvent: (event, data) => {
      if (event === 'delta') handlers.onDelta?.(data.text || '')
      else if (event === 'done') handlers.onDone?.(data)
      else if (event === 'error') handlers.onError?.(data.msg || '对话失败')
    }
  })
}
