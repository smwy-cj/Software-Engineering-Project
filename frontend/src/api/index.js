import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export function getApiErrorMessage(error, fallback = '操作失败') {
  if (!error) return fallback
  if (error.response?.data?.message) return error.response.data.message
  if (error.code === 'ECONNABORTED') return '请求超时，请稍后再试'
  if (!error.response) return '网络连接异常，请检查后重试'
  return fallback
}

export function unwrapData(response) {
  return response?.data?.data
}

export function unwrapPage(response) {
  const data = unwrapData(response) || {}
  return {
    content: data.content || [],
    totalPages: data.totalPages || 1,
    unreadCount: data.unreadCount || 0,
    raw: data
  }
}

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      unauthorizedHandler?.()
    }
    return Promise.reject(err)
  }
)

export default api
