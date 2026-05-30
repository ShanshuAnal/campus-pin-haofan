import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

import { tokenStorage } from './token'
import type { ApiResponse } from '@/types/api'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10_000
})

const friendlyErrorMessage = (status?: number, rawMessage?: string) => {
  const message = rawMessage?.trim()
  const lower = message?.toLowerCase() ?? ''

  if (status === 401) return '登录已失效，请重新登录'
  if (status === 403) {
    if (lower.includes('event')) return '你无权查看该拼单的事件时间线'
    if (lower.includes('permission') || lower.includes('forbidden')) return '你无权查看该内容或执行该操作'
    return message || '你无权查看该内容或执行该操作'
  }
  if (status === 404) return '拼单不存在或已不可访问'
  if (status === 409) return message || '拼单状态已变化，请刷新后重试'
  if (status === 400) return message || '请求参数有误，请检查后重试'
  if (lower.includes('no permission')) return '你无权查看该内容或执行该操作'

  return message || '请求失败，请稍后重试'
}

http.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (typeof payload?.code === 'number' && payload.code !== 200) {
      ElMessage.error(friendlyErrorMessage(payload.code, payload.message))
      return Promise.reject(payload)
    }
    return payload?.data ?? response.data
  },
  (error) => {
    const axiosError = error as AxiosError<ApiResponse<unknown>>
    const status = axiosError.response?.status
    const message = friendlyErrorMessage(status, axiosError.response?.data?.message)

    ElMessage.error(message)
    return Promise.reject(error)
  }
)
