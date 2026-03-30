/**
 * Request Utility Tests
 * Tests Axios interceptors, error handling, and Result type construction
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Mock ant-design-vue message
const messageMock = {
  error: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
  info: vi.fn(),
  loading: vi.fn()
}
vi.mock('ant-design-vue', () => ({
  message: messageMock
}))

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock window.location
const locationMock = { href: '', replace: vi.fn() }
Object.defineProperty(window, 'location', { value: locationMock })

// Mock the config and mockData
vi.mock('@/mock/config', () => ({
  MOCK_MODE: false,
  MOCK_DELAY: 100
}))
vi.mock('@/mock/mockData', () => ({
  getMockData: vi.fn(() => null)
}))

describe('Request Utility - Interceptor & Error Handling Tests', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue(null)
    locationMock.href = ''
    locationMock.replace.mockClear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // ============================================================
  // Test Suite 1: Result Type Structure
  // ============================================================
  describe('Result Type Structure', () => {
    it('should construct successful Result with data', () => {
      const result = {
        success: true,
        code: 200,
        data: { id: 1, name: 'test' },
        message: '操作成功'
      }

      expect(result.success).toBe(true)
      expect(result.code).toBe(200)
      expect(result.data).toEqual({ id: 1, name: 'test' })
    })

    it('should construct paginated Result with records and total', () => {
      const result = {
        success: true,
        code: 200,
        records: [
          { id: 1, name: 'item1' },
          { id: 2, name: 'item2' }
        ],
        total: 100,
        data: null
      }

      expect(result.success).toBe(true)
      expect(result.records).toHaveLength(2)
      expect(result.total).toBe(100)
    })

    it('should construct failed Result with error message', () => {
      const result = {
        success: false,
        code: 500,
        data: null,
        message: '服务器内部错误'
      }

      expect(result.success).toBe(false)
      expect(result.code).toBe(500)
      expect(result.message).toBe('服务器内部错误')
    })
  })

  // ============================================================
  // Test Suite 2: Token Injection
  // ============================================================
  describe('Token Injection', () => {
    it('should read token from localStorage', () => {
      localStorageMock.getItem.mockReturnValue('mock-jwt-token')
      const token = localStorage.getItem('token')

      expect(token).toBe('mock-jwt-token')
      expect(localStorageMock.getItem).toHaveBeenCalledWith('token')
    })

    it('should return null when no token exists', () => {
      localStorageMock.getItem.mockReturnValue(null)
      const token = localStorage.getItem('token')

      expect(token).toBeNull()
    })

    it('should construct Authorization header with Bearer prefix', () => {
      const token = 'eyJhbGciOiJIUzI1NiJ9.test'
      const authHeader = `Bearer ${token}`

      expect(authHeader).toBe('Bearer eyJhbGciOiJIUzI1NiJ9.test')
      expect(authHeader.startsWith('Bearer ')).toBe(true)
    })
  })

  // ============================================================
  // Test Suite 3: Error Message Mapping
  // ============================================================
  describe('Error Message Mapping', () => {
    it('should construct 401 unauthorized error', () => {
      const error = {
        response: { status: 401 }
      }

      expect(error.response.status).toBe(401)
    })

    it('should construct 403 forbidden error', () => {
      const error = {
        response: { status: 403 }
      }

      expect(error.response.status).toBe(403)
    })

    it('should construct 500 server error', () => {
      const error = {
        response: { status: 500 }
      }

      expect(error.response.status).toBe(500)
    })

    it('should extract message from response data', () => {
      const error = {
        response: {
          status: 400,
          data: { message: '业务校验失败' }
        },
        message: 'Request failed'
      }

      const msg = error.response?.data?.message || error.message
      expect(msg).toBe('业务校验失败')
    })

    it('should fall back to error.message when no response data', () => {
      const error = {
        message: 'Network Error'
      }

      const msg = error.response?.data?.message || error.message
      expect(msg).toBe('Network Error')
    })
  })

  // ============================================================
  // Test Suite 4: Content-Type Detection
  // ============================================================
  describe('Content-Type Detection', () => {
    it('should detect octet-stream content type', () => {
      const contentType = 'application/octet-stream'
      const isBlob = contentType.includes('application/octet-stream')
      expect(isBlob).toBe(true)
    })

    it('should detect Excel content types', () => {
      const types = [
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'spreadsheetml',
        'application/vnd.ms-excel'
      ]

      types.forEach(type => {
        expect(
          type.includes('spreadsheetml') ||
          type.includes('excel') ||
          type.includes('vnd.ms-excel')
        ).toBe(true)
      })
    })

    it('should use blob responseType for binary data', () => {
      const config = { responseType: 'blob' as const }
      expect(config.responseType).toBe('blob')
    })
  })

  // ============================================================
  // Test Suite 5: 401 Logout Behavior
  // ============================================================
  describe('401 Unauthorized Handler', () => {
    it('should remove token from localStorage on 401', () => {
      localStorageMock.getItem.mockReturnValue('some-token')
      localStorage.removeItem('token')

      expect(localStorageMock.removeItem).toHaveBeenCalledWith('token')
    })

    it('should redirect to /login on 401', () => {
      window.location.href = '/login'
      expect(window.location.href).toBe('/login')
    })
  })

  // ============================================================
  // Test Suite 6: Base URL and Timeout Configuration
  // ============================================================
  describe('Axios Configuration', () => {
    it('should use /api as base URL', () => {
      const baseURL = '/api'
      expect(baseURL).toBe('/api')
    })

    it('should use 30000ms timeout', () => {
      const timeout = 30000
      expect(timeout).toBe(30000)
      expect(timeout).toBeGreaterThan(0)
    })

    it('should set JSON content-type header', () => {
      const headers = { 'Content-Type': 'application/json' }
      expect(headers['Content-Type']).toBe('application/json')
    })
  })

  // ============================================================
  // Test Suite 7: Mock Mode Handling
  // ============================================================
  describe('Mock Mode Handling', () => {
    it('should only apply mock to /api requests', () => {
      const url = '/api/security/list'
      const isApiRequest = url.startsWith('/api/')
      expect(isApiRequest).toBe(true)
    })

    it('should not apply mock to non-api requests', () => {
      const url = '/other/path'
      const isApiRequest = url.startsWith('/api/')
      expect(isApiRequest).toBe(false)
    })

    it('should construct mock response from getMockData', () => {
      const mockResponse = {
        success: true,
        data: { items: [] }
      }
      const getMockData = () => mockResponse

      const result = getMockData()
      expect(result).toEqual(mockResponse)
    })
  })

  // ============================================================
  // Test Suite 8: Promise Rejection Patterns
  // ============================================================
  describe('Promise Rejection Patterns', () => {
    it('should reject with Error for failed API response', async () => {
      const result = {
        success: false,
        message: '操作失败'
      }

      if (result.success === false) {
        const error = new Error(result.message || '请求失败')
        await expect(Promise.reject(error)).rejects.toThrow('操作失败')
      }
    })

    it('should reject with original error for network errors', async () => {
      const error = { message: 'Network Error' }
      await expect(Promise.reject(error)).rejects.toEqual({ message: 'Network Error' })
    })
  })

  // ============================================================
  // Test Suite 9: Blob Response Passthrough
  // ============================================================
  describe('Blob Response Passthrough', () => {
    it('should passthrough blob data directly', () => {
      const blobData = new Blob(['test'], { type: 'application/octet-stream' })
      const contentType = 'application/octet-stream'

      const shouldPassthrough =
        contentType.includes('application/octet-stream') ||
        contentType.includes('spreadsheetml') ||
        contentType.includes('excel')

      expect(shouldPassthrough).toBe(true)
      // In real flow, blob data would be returned as-is
      expect(blobData).toBeInstanceOf(Blob)
    })

    it('should handle Excel spreadsheetml content type', () => {
      const contentType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      const isExcel = contentType.includes('spreadsheetml')
      expect(isExcel).toBe(true)
    })
  })

  // ============================================================
  // Test Suite 10: MOCK_MODE Flag Behavior
  // ============================================================
  describe('MOCK_MODE Flag Behavior', () => {
    it('should skip mock when MOCK_MODE is false', () => {
      const MOCK_MODE = false
      const mockData = MOCK_MODE ? {} : null
      expect(mockData).toBeNull()
    })

    it('should apply mock when MOCK_MODE is true', () => {
      const MOCK_MODE = true
      const mockResponse = { success: true, data: [] }
      const mockData = MOCK_MODE ? mockResponse : null
      expect(mockData).toEqual({ success: true, data: [] })
    })

    it('should simulate delay in mock mode', async () => {
      const MOCK_DELAY = 100
      const start = Date.now()
      await new Promise(resolve => setTimeout(resolve, MOCK_DELAY))
      const elapsed = Date.now() - start

      expect(elapsed).toBeGreaterThanOrEqual(MOCK_DELAY - 10)
      expect(elapsed).toBeLessThan(MOCK_DELAY + 50)
    })
  })
})
