import { MOCK_MODE, MOCK_DELAY } from './config'

const mockData = [
  // 登录接口
  {
    url: '/api/auth/login',
    method: 'post',
    statusCode: 200,
    response: () => {
      if (!MOCK_MODE) return
      return {
        success: true,
        data: {
          accessToken: 'mock-token-' + Date.now(),
          tokenType: 'Bearer',
          expiresIn: 7200,
          userId: 1,
          username: 'admin',
          nickname: '管理员',
          avatar: '',
          roles: ['admin'],
          permissions: ['*:*:*']
        }
      }
    },
    timeout: MOCK_DELAY
  },
  // 登出接口
  {
    url: '/api/auth/logout',
    method: 'post',
    statusCode: 200,
    response: () => {
      if (!MOCK_MODE) return
      return { success: true, data: null }
    },
    timeout: MOCK_DELAY
  },
  // 获取当前用户
  {
    url: '/api/auth/current-user',
    method: 'get',
    statusCode: 200,
    response: () => {
      if (!MOCK_MODE) return
      return {
        success: true,
        data: {
          userId: 1,
          username: 'admin',
          nickname: '管理员',
          email: 'admin@example.com',
          phone: '13800138000',
          avatar: '',
          roles: ['admin'],
          permissions: ['*:*:*']
        }
      }
    },
    timeout: MOCK_DELAY
  },
  // 验证 Token
  {
    url: '/api/auth/validate',
    method: 'get',
    statusCode: 200,
    response: () => {
      if (!MOCK_MODE) return
      return { success: true, data: true }
    },
    timeout: MOCK_DELAY
  },
  // 刷新 Token
  {
    url: '/api/auth/refresh-token',
    method: 'post',
    statusCode: 200,
    response: () => {
      if (!MOCK_MODE) return
      return {
        success: true,
        data: {
          accessToken: 'mock-refresh-token-' + Date.now(),
          tokenType: 'Bearer',
          expiresIn: 7200,
          userId: 1,
          username: 'admin',
          nickname: '管理员'
        }
      }
    },
    timeout: MOCK_DELAY
  }
]

export default mockData
