import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

import { tokenStorage } from './token'
import type { ApiResponse } from '@/types/api'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10_000
})

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
      ElMessage.error(payload.message || '请求失败，请稍后重试')
      return Promise.reject(payload)
    }
    return payload?.data ?? response.data
  },
  (error) => {
    const axiosError = error as AxiosError<ApiResponse<unknown>>
    const status = axiosError.response?.status
    const message = axiosError.response?.data?.message ?? (status === 401 ? '登录已失效，请重新登录' : '请求失败，请稍后重试')

    ElMessage.error(message)
    return Promise.reject(error)
  }
)
