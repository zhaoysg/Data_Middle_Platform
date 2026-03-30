import request from '@/utils/request'
import type { Result } from '@/types'

// ============================================================
// 数据标准管理
// 后端路径: /api/gov/data-standard（axios baseURL=/api，前端路径写 /gov/data-standard）
// ============================================================

// 标准状态
export type StandardStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'

// 标准类型
export type StandardType = 'CODE_STANDARD' | 'NAMING_STANDARD' | 'PRIMARY_DATA'

// 合规状态
export type ComplianceStatus = 'PENDING' | 'COMPLIANT' | 'NON_COMPLIANT'

// 适用对象
export type ApplicableObject = 'TABLE_NAME' | 'COLUMN_NAME' | 'DATA_VALUE'

// 执行动作
export type EnforceAction = 'ALERT' | 'BLOCK'

// 数据标准列表项
export interface DataStandardVO {
  id?: number
  standardCode?: string
  standardName?: string
  standardType?: StandardType
  standardTypeLabel?: string
  standardCategory?: string
  standardDesc?: string
  status?: StandardStatus
  statusLabel?: string
  enabled?: number
  enabledLabel?: string
  bizDomain?: string
  ownerId?: number
  ownerName?: string
  deptId?: number
  deptName?: string
  sortOrder?: number
  bindingCount?: number
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 数据标准详情
export interface DataStandardDetailVO extends DataStandardVO {
  standardContent?: string
  ruleExpr?: string
  exampleValue?: string
  applicableObject?: ApplicableObject
  applicableObjectLabel?: string
  enforceAction?: EnforceAction
  enforceActionLabel?: string
  bindings?: DataStandardBindingVO[]
}

// 标准绑定记录
export interface DataStandardBindingVO {
  id?: number
  standardId?: number
  metadataId?: number
  dsId?: number
  dsName?: string
  targetTable?: string
  targetTableAlias?: string
  targetColumn?: string
  dataLayer?: string
  complianceStatus?: ComplianceStatus
  complianceStatusLabel?: string
  violationCount?: number
  lastCheckTime?: string
  lastCheckResult?: string
  enforceAction?: EnforceAction
  createUser?: number
  createUserName?: string
  createTime?: string
}

// 新增/编辑标准
export interface DataStandardSaveDTO {
  id?: number
  standardCode?: string
  standardName?: string
  standardType?: StandardType
  standardCategory?: string
  standardDesc?: string
  standardContent?: string
  ruleExpr?: string
  exampleValue?: string
  applicableObject?: ApplicableObject
  enforceAction?: EnforceAction
  status?: StandardStatus
  bizDomain?: string
  ownerId?: number
  deptId?: number
  sortOrder?: number
  enabled?: number
  createUser?: number
  bindings?: DataStandardBindingDTO[]
}

// 绑定参数
export interface DataStandardBindingDTO {
  id?: number
  metadataId?: number
  dsId?: number
  targetTable?: string
  targetColumn?: string
  enforceAction?: EnforceAction
  createUser?: number
}

// 统计信息
export interface DataStandardStatsVO {
  totalCount?: number
  publishedCount?: number
  draftCount?: number
  deprecatedCount?: number
  codeStandardCount?: number
  namingStandardCount?: number
  primaryDataCount?: number
  enabledCount?: number
  disabledCount?: number
  bindingCount?: number
  compliantBindingCount?: number
  nonCompliantBindingCount?: number
  pendingBindingCount?: number
  countByType?: Record<string, number>
}

// 查询参数
export interface DataStandardQueryDTO {
  standardType?: StandardType
  standardCategory?: string
  standardName?: string
  standardCode?: string
  status?: StandardStatus
  enabled?: number
  bizDomain?: string
  ownerId?: number
  deptId?: number
}

export const dataStandardApi = {
  // 分页查询
  page(params: {
    pageNum: number
    pageSize: number
    standardType?: StandardType
    standardCategory?: string
    standardName?: string
    standardCode?: string
    status?: StandardStatus
    enabled?: number
    bizDomain?: string
    ownerId?: number
    deptId?: number
  }) {
    return request.get<Result>('/gov/data-standard/page', { params })
  },

  // 列表（不分页，用于下拉）
  list(params?: DataStandardQueryDTO) {
    return request.get<Result<DataStandardVO[]>>('/gov/data-standard/list', { params })
  },

  // 详情
  getById(id: number) {
    return request.get<Result<DataStandardVO>>(`/gov/data-standard/${id}`)
  },

  getDetail(id: number) {
    return request.get<Result<DataStandardDetailVO>>(`/gov/data-standard/detail/${id}`)
  },

  // 新增
  create(data: DataStandardSaveDTO) {
    return request.post<Result<number>>('/gov/data-standard', data)
  },

  // 更新
  update(id: number, data: DataStandardSaveDTO) {
    return request.put<Result>(`/gov/data-standard/${id}`, data)
  },

  // 删除
  delete(id: number) {
    return request.delete<Result>(`/gov/data-standard/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/data-standard/batch', { data: ids })
  },

  // 启用/禁用
  enable(id: number) {
    return request.post<Result>(`/gov/data-standard/${id}/enable`)
  },

  disable(id: number) {
    return request.post<Result>(`/gov/data-standard/${id}/disable`)
  },

  // 发布/废弃
  publish(id: number) {
    return request.post<Result>(`/gov/data-standard/${id}/publish`)
  },

  deprecate(id: number) {
    return request.post<Result>(`/gov/data-standard/${id}/deprecate`)
  },

  // 复制
  copy(id: number) {
    return request.post<Result<number>>(`/gov/data-standard/${id}/copy`)
  },

  // 统计
  getStats() {
    return request.get<Result<DataStandardStatsVO>>('/gov/data-standard/stats')
  },

  // 绑定元数据
  bind(standardId: number, data: DataStandardBindingDTO) {
    return request.post<Result>(`/gov/data-standard/${standardId}/bind`, data)
  },

  // 批量绑定
  batchBind(standardId: number, metadataIds: number[], enforceAction?: EnforceAction, createUser?: number) {
    return request.post<Result>(`/gov/data-standard/${standardId}/bind/batch`, metadataIds, {
      params: { enforceAction, createUser }
    })
  },

  // 解绑
  unbind(bindingId: number) {
    return request.delete<Result>(`/gov/data-standard/binding/${bindingId}`)
  },

  // 批量解绑
  batchUnbind(bindingIds: number[]) {
    return request.delete<Result>('/gov/data-standard/binding/batch', { data: bindingIds })
  },

  // 获取标准的绑定列表
  getBindings(standardId: number) {
    return request.get<Result<DataStandardBindingVO[]>>(`/gov/data-standard/${standardId}/bindings`)
  },

  // 获取元数据关联的标准
  getStandardsForMetadata(metadataId: number) {
    return request.get<Result<DataStandardVO[]>>(`/gov/data-standard/metadata/${metadataId}/standards`)
  }
}
