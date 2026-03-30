import request from '@/utils/request'
import type { Result } from '@/types'

// ============================================================
// 数据探查 - 类型定义
// ============================================================

export interface ProfileTask {
  id?: number
  taskName?: string
  taskCode?: string
  taskDesc?: string
  targetDsId?: number
  targetTable?: string
  targetColumns?: string
  profileLevel?: string  // basic / detailed / full
  triggerType?: string   // manual / schedule / event
  triggerCron?: string
  timeoutMinutes?: number
  status?: number        // 0=禁用, 1=启用
  lastExecutionId?: number
  lastExecutionTime?: string
  createUser?: number
  createTime?: string
  updateTime?: string
}

export interface TableStats {
  id?: number
  taskId?: number
  executionId?: number
  dsId?: number
  tableName?: string
  profileTime?: string
  rowCount?: number
  columnCount?: number
  storageBytes?: number
  tableComment?: string
  updateTime?: string
  incrementRows?: number
  createTime?: string
}

export interface ColumnStats {
  id?: number
  tableStatsId?: number
  executionId?: number
  dsId?: number
  tableName?: string
  columnName?: string
  profileTime?: string
  dataType?: string
  nullable?: boolean
  totalCount?: number
  nullCount?: number
  nullRate?: number
  uniqueCount?: number
  uniqueRate?: number
  minValue?: string
  maxValue?: string
  avgValue?: number
  medianValue?: number
  stdDev?: number
  minLength?: number
  maxLength?: number
  avgLength?: number
  topValues?: string
  histogram?: string
  outlierCount?: number
  outlierRate?: number
  zeroCount?: number
  zeroRate?: number
  negativeCount?: number
  createTime?: string
  // 附加字段（从后端 VO 扩展）
  dataTypeCategory?: string
  warnings?: string[]
  topValueItems?: TopValueItem[]
  histogramBuckets?: HistogramBucket[]
  outlierDetail?: OutlierDetail
}

export interface TopValueItem {
  value: string
  count: number
  rate: number
}

export interface HistogramBucket {
  range: string
  lower?: number
  upper?: number
  count: number
  rate: number
}

export interface OutlierDetail {
  q1?: number
  q3?: number
  iqr?: number
  lowerBound?: number
  upperBound?: number
  mean?: number
  std?: number
}

export interface ColumnProfileResultVO extends ColumnStats {
  id?: number
  tableStatsId?: number
  dsId?: number
  tableName?: string
  columnName?: string
  dataType?: string
  dataTypeCategory?: string
  nullable?: boolean
  profileTime?: string
  totalCount?: number
  nullCount?: number
  nullRate?: number
  uniqueCount?: number
  uniqueRate?: number
  minValue?: string
  maxValue?: string
  avgValue?: number
  medianValue?: number
  stdDev?: number
  zeroCount?: number
  zeroRate?: number
  negativeCount?: number
  minLength?: number
  maxLength?: number
  avgLength?: number
  topValues?: string
  topValueItems?: TopValueItem[]
  histogram?: string
  histogramBuckets?: HistogramBucket[]
  outlierCount?: number
  outlierRate?: number
  outlierMethod?: string
  outlierDetail?: OutlierDetail
  warnings?: string[]
  createTime?: string
}

export interface Snapshot {
  id?: number
  snapshotName?: string
  snapshotCode?: string
  targetDsId?: number
  targetTable?: string
  snapshotDesc?: string
  tableStatsId?: number
  columnCount?: number
  createUser?: number
  createTime?: string
}

export interface ProfileResult {
  tableStats: TableStats | null
  columnStats: ColumnStats[]
}

export interface TaskStats {
  totalTasks: number
  runningTasks: number
  enabledTasks: number
  totalExecutions: number
}

export interface ProfileExecutionRecord {
  executionId: number
  taskId: number
  taskName: string
  targetDsId: number
  tableName: string
  /** PENDING / RUNNING / SUCCESS / FAILED / SKIPPED */
  status: string
  /** TABLE_STATS / COLUMN_STATS / COMPLETED 等 */
  phase: string
  /** 进度百分比 0-100 */
  progress: number
  startTime: string
  endTime?: string
  elapsedMs?: number
  message?: string
  errorMessage?: string
  tableStatsId?: number
}

// ============================================================
// 数据探查 API
// ============================================================
export const dprofileApi = {

  // ========== 探查任务 ==========

  pageTasks(params: {
    pageNum: number
    pageSize: number
    taskName?: string
    triggerType?: string
    profileLevel?: string
    targetDsId?: number
    status?: number
  }) {
    return request.get<Result>('/dprofile/task/page', { params })
  },

  getTaskById(id: number) {
    return request.get<Result<ProfileTask>>(`/dprofile/task/${id}`)
  },

  createTask(data: Partial<ProfileTask>) {
    return request.post<Result<number>>('/dprofile/task', data)
  },

  updateTask(id: number, data: Partial<ProfileTask>) {
    return request.put<Result>(`/dprofile/task/${id}`, data)
  },

  deleteTask(id: number) {
    return request.delete<Result>(`/dprofile/task/${id}`)
  },

  executeTask(id: number) {
    return request.post<Result<number>>(`/dprofile/task/${id}/execute`)
  },

  cancelTask(id: number) {
    return request.post<Result>(`/dprofile/task/${id}/cancel`)
  },

  toggleTask(id: number, enabled: boolean) {
    return request.post<Result>(`/dprofile/task/${id}/toggle`, null, { params: { enabled } })
  },

  getTaskStats() {
    return request.get<Result<TaskStats>>('/dprofile/task/stats')
  },

  // ========== 探查统计 ==========

  listTableStats(params: {
    dsId?: number
    tableName?: string
    executionId?: number
    limit?: number
  }) {
    return request.get<Result<TableStats[]>>('/dprofile/table-stats', { params })
  },

  getExecutionRecord(executionId: number) {
    return request.get<Result<ProfileExecutionRecord>>(`/dprofile/profile/execution/${executionId}`)
  },

  listColumnStats(tableStatsId: number) {
    return request.get<Result<ColumnStats[]>>('/dprofile/column-stats', {
      params: { tableStatsId }
    })
  },

  getLatestProfile(metadataId: number) {
    return request.get<Result<ProfileResult>>(`/dprofile/profile/latest/${metadataId}`)
  },

  profileTable(params: {
    dsId: number
    tableName: string
    columns?: string
    collectColumnStats?: boolean
  }) {
    return request.post<Result>('/dprofile/profile/table', null, { params })
  },

  // ========== 快照管理 ==========

  createSnapshot(data: Partial<Snapshot>) {
    return request.post<Result<number>>('/dprofile/snapshot', data)
  },

  listSnapshots(params: {
    targetDsId?: number
    targetTable?: string
  }) {
    return request.get<Result<Snapshot[]>>('/dprofile/snapshot', { params })
  },

  deleteSnapshot(id: number) {
    return request.delete<Result>(`/dprofile/snapshot/${id}`)
  },

  // ========== 列级探查 ==========

  /**
   * 执行列级探查并获取完整结果（包含分布可视化和异常检测）
   */
  profileColumns(tableStatsId: number) {
    return request.get<Result<ColumnProfileResultVO[]>>('/dprofile/column/profile', {
      params: { tableStatsId }
    })
  },

  /**
   * 获取单列的分布可视化数据
   */
  getColumnDistribution(params: {
    dsId: number
    tableName: string
    columnName: string
    dataType?: string
    topN?: number
  }) {
    return request.get<Result<Record<string, any>>>('/dprofile/column/distribution', { params })
  },

  /**
   * 批量获取列的探查结果（带异常警告）
   */
  listColumnProfilesWithWarnings(tableStatsId: number) {
    return request.get<Result<ColumnProfileResultVO[]>>('/dprofile/column/profile/warnings', {
      params: { tableStatsId }
    })
  }
}
