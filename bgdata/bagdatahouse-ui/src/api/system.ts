import request from '@/utils/request'
import type { AxiosPromise } from 'axios'

// ========== SysUser 相关 ==========

export interface SysUser {
  id?: number
  username?: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  status?: number
  lastLoginIp?: string
  lastLoginTime?: string
  createTime?: string
}

export interface SysUserForm {
  id?: number
  username?: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  status?: number
  roleIds?: number[]
}

export interface UserPageParams {
  pageNum?: number
  pageSize?: number
  username?: string
  nickname?: string
  status?: number
  deptId?: number
}

export interface UserOptionsItem {
  value: number
  label: string
  username: string
  nickname: string
  deptId?: number
}

export function getUserPage(params: UserPageParams) {
  return request.get<any, { data: { records: SysUser[]; total: number; size: number; current: number } }>(
    '/system/user/page',
    { params }
  )
}

export function getUserDetail(id: number) {
  return request.get<any, { data: SysUser }>(`/system/user/${id}`)
}

export function createUser(data: SysUserForm) {
  return request.post<any, { data: number }>('/system/user', data)
}

export function updateUser(id: number, data: SysUserForm) {
  return request.put<any, void>(`/system/user/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete<any, void>(`/system/user/${id}`)
}

export function updateUserStatus(id: number, status: number) {
  return request.put<any, void>(`/system/user/${id}/status`, null, { params: { status } })
}

export function resetUserPassword(id: number, newPassword?: string) {
  return request.put<any, void>(`/system/user/${id}/reset-password`, null, {
    params: { newPassword }
  })
}

export function getUserOptions() {
  return request.get<any, { data: UserOptionsItem[] }>('/system/user/options')
}

export function getDeptOptions() {
  return request.get<any, { data: any[] }>('/system/dept/options')
}

// ========== SysRole 相关 ==========

export interface SysRole {
  id?: number
  roleName?: string
  roleCode?: string
  roleType?: string
  dataScope?: string
  status?: number
  sortOrder?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface SysRoleForm {
  id?: number
  roleName?: string
  roleCode?: string
  roleType?: string
  dataScope?: string
  status?: number
  sortOrder?: number
  remark?: string
  menuIds?: number[]
}

export interface RolePageParams {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleCode?: string
  status?: number
}

export function getRolePage(params: RolePageParams) {
  return request.get<any, { data: { records: SysRole[]; total: number; size: number; current: number } }>(
    '/system/role/page',
    { params }
  )
}

export function getRoleDetail(id: number) {
  return request.get<any, { data: SysRole }>(`/system/role/${id}`)
}

export function createRole(data: SysRoleForm) {
  return request.post<any, { data: number }>('/system/role', data)
}

export function updateRole(id: number, data: SysRoleForm) {
  return request.put<any, void>(`/system/role/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete<any, void>(`/system/role/${id}`)
}

export function updateRoleStatus(id: number, status: number) {
  return request.put<any, void>(`/system/role/${id}/status`, null, { params: { status } })
}

export function getRoleMenus(id: number) {
  return request.get<any, { data: number[] }>(`/system/role/${id}/menus`)
}

export function assignRoleMenus(id: number, menuIds: number[]) {
  return request.put<any, void>(`/system/role/${id}/menus`, menuIds)
}

// ========== SysMenu 相关 ==========

export interface SysMenu {
  id?: number
  parentId?: number
  menuName?: string
  menuCode?: string
  menuType?: string
  path?: string
  component?: string
  icon?: string
  sortOrder?: number
  visible?: number
  status?: number
  perms?: string
  cached?: number
  createTime?: string
}

export interface SysMenuForm {
  id?: number
  parentId?: number
  menuName?: string
  menuCode?: string
  menuType?: string
  path?: string
  component?: string
  icon?: string
  sortOrder?: number
  visible?: number
  status?: number
  perms?: string
  cached?: number
}

export function getMenuTree() {
  return request.get<any, { data: SysMenu[] }>('/system/menu/tree')
}

export function getMenuDetail(id: number) {
  return request.get<any, { data: SysMenu }>(`/system/menu/${id}`)
}

export function createMenu(data: SysMenuForm) {
  return request.post<any, { data: number }>('/system/menu', data)
}

export function updateMenu(id: number, data: SysMenuForm) {
  return request.put<any, void>(`/system/menu/${id}`, data)
}

export function deleteMenu(id: number) {
  return request.delete<any, void>(`/system/menu/${id}`)
}

export function getMenuOptions() {
  return request.get<any, { data: any[] }>('/system/menu/options')
}

// ========== SysDept 相关 ==========

export interface SysDept {
  id?: number
  parentId?: number
  deptName?: string
  deptCode?: string
  deptType?: string
  sortOrder?: number
  leaderId?: number
  phone?: string
  email?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface DeptPageParams {
  pageNum?: number
  pageSize?: number
  deptName?: string
  deptCode?: string
  status?: number
}

export function getDeptPage(params: DeptPageParams) {
  return request.get<any, { data: { records: SysDept[]; total: number; size: number; current: number } }>(
    '/system/dept/page',
    { params }
  )
}

export function getDeptTree() {
  return request.get<any, { data: SysDept[] }>('/system/dept/tree')
}

export function getDeptDetail(id: number) {
  return request.get<any, { data: SysDept }>(`/system/dept/${id}`)
}

export function createDept(data: Partial<SysDept>) {
  return request.post<any, { data: number }>('/system/dept', data)
}

export function updateDept(id: number, data: Partial<SysDept>) {
  return request.put<any, void>(`/system/dept/${id}`, data)
}

export function deleteDept(id: number) {
  return request.delete<any, void>(`/system/dept/${id}`)
}

export function updateDeptStatus(id: number, status: number) {
  return request.put<any, void>(`/system/dept/${id}/status`, null, { params: { status } })
}

// ========== DqDataDomain 相关 ==========

export interface DqDataDomain {
  id?: number
  domainCode?: string
  domainName?: string
  domainDesc?: string
  deptId?: number
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface DataDomainPageParams {
  pageNum?: number
  pageSize?: number
  domainName?: string
  domainCode?: string
  status?: number
}

export function getDataDomainPage(params: DataDomainPageParams) {
  return request.get<any, { data: { records: DqDataDomain[]; total: number; size: number; current: number } }>(
    '/dqc/data-domain/page',
    { params }
  )
}

export function getDataDomainList() {
  return request.get<any, { data: DqDataDomain[] }>('/dqc/data-domain/list')
}

export function getDataDomainTree() {
  return request.get<any, { data: DqDataDomain[] }>('/dqc/data-domain/tree')
}

export function getDataDomainDetail(id: number) {
  return request.get<any, { data: DqDataDomain }>(`/dqc/data-domain/${id}`)
}

export function createDataDomain(data: Partial<DqDataDomain>) {
  return request.post<any, { data: number }>('/dqc/data-domain', data)
}

export function updateDataDomain(id: number, data: Partial<DqDataDomain>) {
  return request.put<any, void>(`/dqc/data-domain/${id}`, data)
}

export function deleteDataDomain(id: number) {
  return request.delete<any, void>(`/dqc/data-domain/${id}`)
}

export function updateDataDomainStatus(id: number, status: number) {
  return request.put<any, void>(`/dqc/data-domain/${id}/status`, null, { params: { status } })
}
