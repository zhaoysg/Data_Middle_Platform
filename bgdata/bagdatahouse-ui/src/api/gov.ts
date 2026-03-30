import request from '@/utils/request'
import type { Result } from '@/types'

// ============================================================
// 元数据管理
// 后端路径: /gov/metadata
// ============================================================
export interface MetadataScanRequest {
  dsId: number
  scanScope?: 'ALL' | 'SPECIFIED' | 'PATTERN'
  tableNames?: string[]
  tablePattern?: string
  dataLayer?: string
  dataDomain?: string
  bizDomain?: string
  collectStats?: boolean
  collectColumnStats?: boolean
  overwriteExisting?: boolean
  createUser?: number
}

export interface MetadataScanResult {
  taskId: string
  status: string
  dsId: number
  dsName: string
  totalTables: number
  /** 计划扫描表数（进度条分母） */
  plannedTableCount?: number
  /** 已处理表数（成功+失败+跳过） */
  processedTableCount?: number
  successTables: number
  failedTables: number
  insertedTables: number
  updatedTables: number
  skippedTables: number
  totalColumns: number
  elapsedMs: number
  errors: TableScanError[]
  tables: TableScanSummary[]
}

export interface TableScanError {
  tableName: string
  errorMessage: string
  exceptionType: string
}

export interface TableScanSummary {
  metadataId: number
  tableName: string
  tableType: string
  tableComment: string
  columnCount: number
  rowCount: number
  inserted: boolean
}

export interface MetadataStats {
  totalTables: number
  totalColumns: number
  dsCount: number
  activeTables: number
  byDataLayer: Record<string, number>
  byDsType: Record<string, number>
}

export interface Metadata {
  id?: number
  dsId?: number
  tableName?: string
  tableAlias?: string
  tableComment?: string
  tableType?: string
  dataLayer?: string
  dataDomain?: string
  bizDomain?: string
  lifecycleDays?: number
  isPartitioned?: boolean
  partitionColumn?: string
  storageBytes?: number
  rowCount?: number
  accessFreq?: number
  sensitivityLevel?: string
  /** 列表接口填充：是否存在敏感字段 */
  isSensitive?: boolean
  ownerId?: number
  deptId?: number
  tags?: string
  lastProfiledAt?: string
  lastModifiedAt?: string
  lastAccessedAt?: string
  etlSource?: string
  status?: string
  createUser?: number
  createTime?: string
}

export interface MetadataColumn {
  id?: number
  metadataId?: number
  columnName?: string
  columnAlias?: string
  columnComment?: string
  dataType?: string
  isNullable?: boolean
  columnKey?: string
  defaultValue?: string
  isPrimaryKey?: boolean
  isForeignKey?: boolean
  fkReference?: string
  isSensitive?: boolean
  sensitivityLevel?: string
  sortOrder?: number
  createUser?: number
  createTime?: string
}

export const metadataApi = {
  // 扫描
  scan(data: MetadataScanRequest) {
    return request.post<Result<MetadataScanResult>>('/gov/metadata/scan', data)
  },

  scanAsync(data: MetadataScanRequest) {
    return request.post<Result<string>>('/gov/metadata/scan/async', data)
  },

  getScanProgress(taskId: string) {
    return request.get<Result<MetadataScanResult>>(`/gov/metadata/scan/progress/${taskId}`)
  },

  cancelScan(taskId: string) {
    return request.post<Result>(`/gov/metadata/scan/cancel/${taskId}`)
  },

  // 查询
  page(params: {
    pageNum: number
    pageSize: number
    dsId?: number
    dataLayer?: string
    dataDomain?: string
    tableName?: string
    status?: string
  }) {
    return request.get<Result>('/gov/metadata/page', { params })
  },

  getById(id: number) {
    return request.get<Result<Metadata>>(`/gov/metadata/${id}`)
  },

  getByDsIdAndTable(dsId: number, tableName: string) {
    return request.get<Result<Metadata>>('/gov/metadata/table', {
      params: { dsId, tableName }
    })
  },

  getColumns(metadataId: number) {
    return request.get<Result<MetadataColumn[]>>(`/gov/metadata/${metadataId}/columns`)
  },

  // 更新
  updateMetadata(id: number, data: Partial<Metadata>) {
    return request.put<Result>(`/gov/metadata/${id}`, data)
  },

  updateColumn(id: number, data: Partial<MetadataColumn>) {
    return request.put<Result>(`/gov/metadata/column/${id}`, data)
  },

  // 删除
  deleteMetadata(id: number) {
    return request.delete<Result>(`/gov/metadata/${id}`)
  },

  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/metadata/batch', { data: ids })
  },

  // 统计
  syncStats(dsId: number, tableNames: string[]) {
    return request.post<Result>('/gov/metadata/sync-stats', tableNames, {
      params: { dsId }
    })
  },

  getStats() {
    return request.get<Result<MetadataStats>>('/gov/metadata/stats')
  }
}
