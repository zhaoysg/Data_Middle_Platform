import request from '@/utils/request'
import type { Result } from '@/types'

// Result with blob response type
type ResultBlob = Result<Blob | void>

// Page type (for MyBatis-Plus pagination)
export interface Page<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ============================================================
// 术语库管理
// 后端路径: /api/gov/glossary-category, /api/gov/glossary-term
// ============================================================

// ---------- 术语分类相关类型 ----------

// 术语状态
export type TermStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'

// 数据类型
export type DataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN'

// 敏感等级
export type SensitivityLevel = 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'SECRET'

// 映射类型
export type MappingType = 'DIRECT' | 'TRANSFORM' | 'AGGREGATE'

// 映射审批状态
export type MappingStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// 术语分类树形节点
export interface GlossaryCategoryTreeVO {
  id?: number
  parentId?: number
  categoryName?: string
  categoryCode?: string
  categoryDesc?: string
  sortOrder?: number
  status?: number
  statusLabel?: string
  termCount?: number
  children?: GlossaryCategoryTreeVO[]
  createTime?: string
}

// 术语分类列表项
export interface GlossaryCategoryVO {
  id?: number
  parentId?: number
  parentName?: string
  categoryName?: string
  categoryCode?: string
  categoryDesc?: string
  sortOrder?: number
  status?: number
  statusLabel?: string
  termCount?: number
  childCount?: number
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 术语分类详情
export interface GlossaryCategoryDetailVO extends GlossaryCategoryVO {
  children?: GlossaryCategoryTreeVO[]
}

// 术语分类保存DTO
export interface GlossaryCategorySaveDTO {
  id?: number
  parentId?: number
  categoryName?: string
  categoryCode?: string
  categoryDesc?: string
  sortOrder?: number
  status?: number
  createUser?: number
}

// 术语分类查询DTO
export interface GlossaryCategoryQueryDTO {
  categoryName?: string
  categoryCode?: string
  status?: number
  parentId?: number
  topLevelOnly?: boolean
}

// ---------- 术语相关类型 ----------

// 术语列表项
export interface GlossaryTermVO {
  id?: number
  termCode?: string
  termName?: string
  termNameEn?: string
  termAlias?: string
  categoryId?: number
  categoryName?: string
  bizDomain?: string
  definition?: string
  dataType?: DataType
  unit?: string
  exampleValue?: string
  sensitivityLevel?: SensitivityLevel
  sensitivityLevelLabel?: string
  status?: TermStatus
  statusLabel?: string
  enabled?: number
  enabledLabel?: string
  ownerId?: number
  ownerName?: string
  deptId?: number
  deptName?: string
  sortOrder?: number
  mappingCount?: number
  publishedTime?: string
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 术语详情
export interface GlossaryTermDetailVO extends GlossaryTermVO {
  formula?: string
  mappings?: GlossaryMappingVO[]
}

// 术语-字段映射
export interface GlossaryMappingVO {
  id?: number
  termId?: number
  dsId?: number
  dsName?: string
  tableName?: string
  tableAlias?: string
  columnName?: string
  mappingType?: MappingType
  mappingTypeLabel?: string
  mappingDesc?: string
  confidence?: number
  status?: MappingStatus
  statusLabel?: string
  approvedBy?: number
  approvedByName?: string
  approvedTime?: string
  rejectReason?: string
  createUser?: number
  createUserName?: string
  createTime?: string
}

// 术语-字段映射保存DTO
export interface GlossaryMappingDTO {
  id?: number
  termId?: number
  dsId?: number
  tableName?: string
  columnName?: string
  mappingType?: MappingType
  mappingDesc?: string
  confidence?: number
  status?: MappingStatus
  approvedBy?: number
  rejectReason?: string
  createUser?: number
}

// 术语保存DTO
export interface GlossaryTermSaveDTO {
  id?: number
  termCode?: string
  termName?: string
  termNameEn?: string
  termAlias?: string
  categoryId?: number
  bizDomain?: string
  definition?: string
  formula?: string
  dataType?: DataType
  unit?: string
  exampleValue?: string
  sensitivityLevel?: SensitivityLevel
  status?: TermStatus
  ownerId?: number
  deptId?: number
  sortOrder?: number
  enabled?: number
  createUser?: number
  mappings?: GlossaryMappingDTO[]
}

// 术语查询DTO
export interface GlossaryTermQueryDTO {
  termName?: string
  termCode?: string
  termNameEn?: string
  termAlias?: string
  categoryId?: number
  bizDomain?: string
  status?: TermStatus
  enabled?: number
  dataType?: DataType
  sensitivityLevel?: SensitivityLevel
  ownerId?: number
  deptId?: number
}

// 术语库统计
export interface GlossaryStatsVO {
  totalTermCount?: number
  publishedTermCount?: number
  draftTermCount?: number
  deprecatedTermCount?: number
  totalCategoryCount?: number
  topCategoryCount?: number
  totalMappingCount?: number
  pendingMappingCount?: number
  approvedMappingCount?: number
  termCountByBizDomain?: Record<string, number>
  termCountByDataType?: Record<string, number>
  termCountBySensitivityLevel?: Record<string, number>
}

// ---------- API ----------

export const glossaryCategoryApi = {
  // 分页查询
  page(params: {
    pageNum: number
    pageSize: number
    categoryName?: string
    categoryCode?: string
    status?: number
    parentId?: number
  }) {
    return request.get<Result<Page<GlossaryCategoryVO>>>('/gov/glossary-category/page', { params })
  },

  // 列表（不分页）
  list(params?: { categoryName?: string; status?: number; parentId?: number; topLevelOnly?: boolean }) {
    return request.get<Result<GlossaryCategoryVO[]>>('/gov/glossary-category/list', { params })
  },

  // 详情
  getById(id: number) {
    return request.get<Result<GlossaryCategoryVO>>(`/gov/glossary-category/${id}`)
  },

  getDetail(id: number) {
    return request.get<Result<GlossaryCategoryDetailVO>>(`/gov/glossary-category/detail/${id}`)
  },

  // 新增
  create(data: GlossaryCategorySaveDTO) {
    return request.post<Result<number>>('/gov/glossary-category', data)
  },

  // 更新
  update(id: number, data: GlossaryCategorySaveDTO) {
    return request.put<Result>(`/gov/glossary-category/${id}`, data)
  },

  // 删除
  delete(id: number) {
    return request.delete<Result>(`/gov/glossary-category/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/glossary-category/batch', { data: ids })
  },

  // 启用/禁用
  enable(id: number) {
    return request.post<Result>(`/gov/glossary-category/${id}/enable`)
  },

  disable(id: number) {
    return request.post<Result>(`/gov/glossary-category/${id}/disable`)
  },

  // 移动分类
  move(id: number, newParentId: number) {
    return request.post<Result>(`/gov/glossary-category/${id}/move`, null, { params: { newParentId } })
  },

  // 获取分类树
  getTree(params?: { categoryName?: string; status?: number }) {
    return request.get<Result<GlossaryCategoryTreeVO[]>>('/gov/glossary-category/tree', { params })
  },

  // 获取全量分类树
  getFullTree() {
    return request.get<Result<GlossaryCategoryTreeVO[]>>('/gov/glossary-category/tree/full')
  },
}

export const glossaryTermApi = {
  // 分页查询
  page(params: {
    pageNum: number
    pageSize: number
    termName?: string
    termCode?: string
    termNameEn?: string
    categoryId?: number
    bizDomain?: string
    status?: TermStatus
    enabled?: number
    dataType?: DataType
    sensitivityLevel?: SensitivityLevel
    ownerId?: number
    deptId?: number
  }) {
    return request.get<Result<Page<GlossaryTermVO>>>('/gov/glossary-term/page', { params })
  },

  // 列表（不分页）
  list(params?: {
    termName?: string
    categoryId?: number
    status?: TermStatus
    enabled?: number
    bizDomain?: string
  }) {
    return request.get<Result<GlossaryTermVO[]>>('/gov/glossary-term/list', { params })
  },

  // 详情
  getById(id: number) {
    return request.get<Result<GlossaryTermVO>>(`/gov/glossary-term/${id}`)
  },

  getDetail(id: number) {
    return request.get<Result<GlossaryTermDetailVO>>(`/gov/glossary-term/detail/${id}`)
  },

  // 新增
  create(data: GlossaryTermSaveDTO) {
    return request.post<Result<number>>('/gov/glossary-term', data)
  },

  // 更新
  update(id: number, data: GlossaryTermSaveDTO) {
    return request.put<Result>(`/gov/glossary-term/${id}`, data)
  },

  // 删除
  delete(id: number) {
    return request.delete<Result>(`/gov/glossary-term/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/glossary-term/batch', { data: ids })
  },

  // 启用/禁用
  enable(id: number) {
    return request.post<Result>(`/gov/glossary-term/${id}/enable`)
  },

  disable(id: number) {
    return request.post<Result>(`/gov/glossary-term/${id}/disable`)
  },

  // 发布/废弃
  publish(id: number) {
    return request.post<Result>(`/gov/glossary-term/${id}/publish`)
  },

  deprecate(id: number) {
    return request.post<Result>(`/gov/glossary-term/${id}/deprecate`)
  },

  // 复制
  copy(id: number) {
    return request.post<Result<GlossaryTermVO>>(`/gov/glossary-term/${id}/copy`)
  },

  // 统计
  getStats() {
    return request.get<Result<GlossaryStatsVO>>('/gov/glossary-term/stats')
  },

  // 全局搜索
  search(keyword: string) {
    return request.get<Result<GlossaryTermVO[]>>('/gov/glossary-term/search', { params: { keyword } })
  },

  // 获取映射列表
  getMappings(id: number) {
    return request.get<Result<GlossaryMappingVO[]>>(`/gov/glossary-term/${id}/mappings`)
  },

  // 批量导入术语（Excel）
  importExcel(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<Result<{ successCount: number; failCount: number; errors?: string[] }>>(
      '/gov/glossary-term/import',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  },

  // 导出术语（Excel）
  exportExcel(params?: {
    categoryId?: number
    bizDomain?: string
    status?: TermStatus
    sensitivityLevel?: SensitivityLevel
  }) {
    return request.get<ResultBlob>('/gov/glossary-term/export', { params, responseType: 'blob' })
  },

  // 导出映射审批结果（Excel）
  exportMappingExcel(params?: {
    termName?: string
    dsId?: number
    status?: MappingStatus
  }) {
    return request.get<ResultBlob>('/gov/glossary-mapping/export', { params, responseType: 'blob' })
  },

  // 下载导入模板
  downloadTemplate() {
    return request.get<ResultBlob>('/gov/glossary-term/template', { responseType: 'blob' })
  },
}

export const glossaryMappingApi = {
  // 分页查询映射
  page(params: {
    pageNum: number
    pageSize: number
    termName?: string
    dsId?: number
    status?: MappingStatus
  }) {
    return request.get<Result<Page<GlossaryMappingVO>>>('/gov/glossary-mapping/pending/page', { params })
  },

  // 列表查询映射
  list(params?: {
    termName?: string
    dsId?: number
    status?: MappingStatus
  }) {
    return request.get<Result<GlossaryMappingVO[]>>('/gov/glossary-mapping/pending/list', { params })
  },

  // 根据术语ID查询映射
  getByTermId(termId: number) {
    return request.get<Result<GlossaryMappingVO[]>>(`/gov/glossary-mapping/term/${termId}`)
  },

  // 新增映射
  create(data: GlossaryMappingDTO) {
    return request.post<Result<number>>('/gov/glossary-mapping', data)
  },

  // 批量新增映射
  batchCreate(termId: number, mappings: GlossaryMappingDTO[]) {
    return request.post<Result>('/gov/glossary-mapping/batch/' + termId, mappings)
  },

  // 更新映射
  update(id: number, data: GlossaryMappingDTO) {
    return request.put<Result>(`/gov/glossary-mapping/${id}`, data)
  },

  // 删除映射
  delete(id: number) {
    return request.delete<Result>(`/gov/glossary-mapping/${id}`)
  },

  // 批量删除映射
  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/glossary-mapping/batch', { data: ids })
  },

  // 审批通过
  approve(id: number) {
    return request.post<Result>(`/gov/glossary-mapping/${id}/approve`)
  },

  // 驳回
  reject(id: number, rejectReason: string) {
    return request.post<Result>(`/gov/glossary-mapping/${id}/reject`, { rejectReason })
  },

  // 批量审批通过
  batchApprove(ids: number[]) {
    return request.post<Result<{ successCount: number; failCount: number }>>('/gov/glossary-mapping/batch/approve', ids)
  },

  // 批量驳回
  batchReject(ids: number[], rejectReason: string) {
    return request.post<Result<{ successCount: number; failCount: number }>>('/gov/glossary-mapping/batch/reject', {
      ids,
      rejectReason,
    })
  },

  // 根据数据源查询已审批映射
  getApprovedByDatasource(dsId: number) {
    return request.get<Result<GlossaryMappingVO[]>>(`/gov/glossary-mapping/approved/by-datasource/${dsId}`)
  },

  // 根据数据源和表查询已审批映射
  getApprovedByTable(dsId: number, tableName: string) {
    return request.get<Result<GlossaryMappingVO[]>>('/gov/glossary-mapping/approved/by-table', {
      params: { dsId, tableName },
    })
  },
}
