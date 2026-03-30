import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import { MOCK_MODE, MOCK_DELAY } from '@/mock/config'
import { getMockData } from '@/mock/mockData'

// 生成随机 ID
const generateId = () => Math.floor(Math.random() * 10000) + 100

// Mock 拦截器
axios.interceptors.request.use(async (config) => {
  // 添加 token
  const token = localStorage.getItem('token')
  if (token && config.headers) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  
  // Mock 模式处理
  if (MOCK_MODE && config.url?.startsWith('/api/')) {
    const mockResponse = getMockData(config.url!, config.method || 'get', config.params, config.data)
    if (mockResponse !== null) {
      // 延迟模拟网络请求
      await new Promise(resolve => setTimeout(resolve, MOCK_DELAY))
      // 直接返回模拟数据
      return Promise.reject({ __MOCK__: true, response: mockResponse })
    }
  }
  
  return config
})

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const contentType = response.headers['content-type'] || ''
    // Blob / 流响应直接透传，不走 JSON 解析
    if (
      contentType.includes('application/octet-stream') ||
      contentType.includes('spreadsheetml') ||
      contentType.includes('excel') ||
      response.config.responseType === 'blob'
    ) {
      return response.data
    }
    const res = response.data
    if (res.success === false) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error: any) => {
    // 主动取消请求（如关闭弹窗中止预览查询），不打断用户、不弹全局错误
    if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError') {
      return Promise.reject(error)
    }
    // 处理 Mock 响应
    if (error.__MOCK__) {
      const res = error.response
      if (res.success === false) {
        message.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res
    }
    
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message

    if (status === 401) {
      message.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else if (status === 403) {
      message.error('没有权限访问该资源')
    } else if (status === 500) {
      const serverMsg = error.response?.data?.message
      message.error(
        typeof serverMsg === 'string' && serverMsg.trim().length > 0
          ? serverMsg
          : '服务器内部错误'
      )
    } else {
      message.error(msg)
    }
    return Promise.reject(error)
  }
)

export default instance
