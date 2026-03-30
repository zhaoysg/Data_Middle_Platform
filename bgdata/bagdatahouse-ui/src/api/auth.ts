import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  nickname: string
  avatar?: string
  roles?: string[]
  permissions?: string[]
}

export interface CurrentUser {
  userId: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  deptName?: string
  roles?: string[]
  permissions?: string[]
  lastLoginIp?: string
  lastLoginTime?: string
}

export function login(data: LoginRequest) {
  return request.post<any, { data: LoginResponse }>('/auth/login', data)
}

export function logout() {
  return request.post<any, void>('/auth/logout')
}

export function getCurrentUser() {
  return request.get<any, { data: CurrentUser }>('/auth/current-user')
}

export function refreshToken(oldToken: string) {
  return request.post<any, { data: LoginResponse }>('/auth/refresh-token', null, {
    params: { oldToken }
  })
}

export function validateToken(token: string) {
  return request.get<any, { data: boolean }>('/auth/validate', {
    params: { token }
  })
}
