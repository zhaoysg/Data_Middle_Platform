import request from '@/utils/request'
import type { Result } from '@/types'

// ============================================================
// 数据血缘
// 后端路径: /gov/lineage
// ============================================================

export interface LineageNodeVO {
  nodeId: string
  nodeType: 'TABLE' | 'COLUMN'
  dsId: number
  dsName: string
  dsType: string
  tableName: string
  tableAlias: string
  columnName: string
  columnAlias: string
  dataLayer: string
  sensitivityLevel: string   // L1/L2/L3/L4
  x?: number
  y?: number
  level?: number
}

export interface LineageEdgeVO {
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  lineageId: number
  transformType: string
  transformExpr: string
}

export interface LineageGraphVO {
  nodes: LineageNodeVO[]
  edges: LineageEdgeVO[]
}

export interface LineageVO {
  id: number
  lineageType: string
  lineageTypeLabel: string
  sourceDsId: number
  sourceDsName: string
  sourceDsCode: string
  sourceDsType: string
  sourceTable: string
  sourceColumn: string
  sourceColumnAlias: string
  targetDsId: number
  targetDsName: string
  targetDsCode: string
  targetDsType: string
  targetTable: string
  targetColumn: string
  targetColumnAlias: string
  transformType: string
  transformTypeLabel: string
  transformExpr: string
  jobId: number | null
  jobName: string
  lineageSource: string
  lineageSourceLabel: string
  deptId: number | null
  deptName: string
  status: string
  createUser: number | null
  createUserName: string
  createTime: string
  updateUser: number | null
  updateTime: string | null
}

export interface LineageStatsVO {
  totalCount: number
  tableLineageCount: number
  columnLineageCount: number
  manualCount: number
  autoParserCount: number
  sourceTableCount: number
  targetTableCount: number
  columnCount: number
  l4SensitiveCount?: number
  l3SensitiveCount?: number
  sensitivityDistribution?: Record<string, number>
  byDataSource: Record<string, number>
  byDataLayer: Record<string, number>
  byTransformType: Record<string, number>
}

export interface LineageQueryParams {
  lineageType?: string
  sourceDsId?: number
  sourceTable?: string
  sourceColumn?: string
  targetDsId?: number
  targetTable?: string
  targetColumn?: string
  transformType?: string
  lineageSource?: string
  status?: string
  deptId?: number
}

export interface LineageSaveDTO {
  id?: number
  lineageType: string
  sourceDsId: number
  sourceTable: string
  sourceColumn?: string
  sourceColumnAlias?: string
  targetDsId: number
  targetTable: string
  targetColumn?: string
  targetColumnAlias?: string
  transformType?: string
  transformExpr?: string
  jobId?: number
  jobName?: string
  lineageSource?: string
  deptId?: number
}

export interface BatchImportResultVO {
  totalRows: number
  successCount: number
  failCount: number
  skippedCount: number
  errors: ImportErrorDetail[]
}

export interface ImportErrorDetail {
  rowNum: number
  message: string
}

export interface LineageBatchImportDTO {
  lineageType: string
  lineageSource?: string
  defaultSourceDsId?: number
  defaultTargetDsId?: number
  deptId?: number
  records: LineageRecord[]
}

export interface LineageRecord {
  sourceDsId?: number
  sourceTable: string
  sourceColumn?: string
  sourceColumnAlias?: string
  targetDsId?: number
  targetTable: string
  targetColumn?: string
  targetColumnAlias?: string
  transformType?: string
  transformExpr?: string
  jobName?: string
}

export interface ImpactScope {
  affectedTableCount: number
  affectedColumnCount: number
  affectedPlanCount: number
  depthLevel: number
  totalNodeCount: number
  totalEdgeCount: number
  l4SensitiveCount?: number
  l3SensitiveCount?: number
  sensitivityDistribution?: Record<string, number>
}

export interface ImpactAnalysisResultVO {
  direction: 'DOWNSTREAM' | 'UPSTREAM'
  targetNodeId: string
  maxDepth: number
  scope: ImpactScope
  nodes: LineageNodeVO[]
  edges: LineageEdgeVO[]
  levelDistribution: Record<string, number>
  layerDistribution: Record<string, number>
}

export interface TableColumnSuggestVO {
  dsId: number
  dsName: string
  tables: TableSuggest[]
}

export interface TableSuggest {
  metadataId: number
  tableName: string
  tableAlias: string
  dataLayer: string
  columnCount: number
  columns?: ColumnSuggest[]
}

export interface ColumnSuggest {
  columnId: number
  columnName: string
  columnAlias: string
  dataType: string
  isPrimaryKey: boolean
  isForeignKey: boolean
  isSensitive: boolean
}

export const lineageManualApi = {
  // 影响分析
  analyzeDownstream(dsId: number, tableName: string, column?: string, maxDepth = 5) {
    return request.get<Result<ImpactAnalysisResultVO>>('/gov/lineage-manual/impact/downstream', {
      params: { dsId, tableName, column, maxDepth }
    })
  },

  analyzeUpstream(dsId: number, tableName: string, column?: string, maxDepth = 5) {
    return request.get<Result<ImpactAnalysisResultVO>>('/gov/lineage-manual/impact/upstream', {
      params: { dsId, tableName, column, maxDepth }
    })
  },

  analyzeFromNode(direction: string, dsId: number, tableName: string, column?: string, maxDepth = 5) {
    return request.get<Result<ImpactAnalysisResultVO>>('/gov/lineage-manual/impact/node', {
      params: { direction, dsId, tableName, column, maxDepth }
    })
  },

  // 表/字段建议
  suggest(dsId: number, keyword?: string) {
    return request.get<Result<TableColumnSuggestVO>>('/gov/lineage-manual/suggest', {
      params: { dsId, keyword }
    })
  },

  listTables(dsId: number) {
    return request.get<Result<TableSuggest[]>>('/gov/lineage-manual/tables', {
      params: { dsId }
    })
  },

  listColumns(dsId: number, tableName: string) {
    return request.get<Result<ColumnSuggest[]>>('/gov/lineage-manual/columns', {
      params: { dsId, tableName }
    })
  },

  // 批量导入
  batchImport(data: LineageBatchImportDTO) {
    return request.post<Result<BatchImportResultVO>>('/gov/lineage-manual/batch-import', data)
  },

  getImportTemplate() {
    return request.get<Result<string>>('/gov/lineage-manual/import-template')
  }
}

export const lineageApi = {
  // 图谱数据
  getGraph(params?: LineageQueryParams) {
    return request.get<Result<LineageGraphVO>>('/gov/lineage/graph', { params })
  },

  // 下游追溯
  getDownstream(dsId: number, tableName: string, maxDepth = 3) {
    return request.get<Result<LineageNodeVO[]>>('/gov/lineage/downstream', {
      params: { dsId, tableName, maxDepth }
    })
  },

  // 上游回溯
  getUpstream(dsId: number, tableName: string, maxDepth = 3) {
    return request.get<Result<LineageNodeVO[]>>('/gov/lineage/upstream', {
      params: { dsId, tableName, maxDepth }
    })
  },

  // 分页查询
  page(params: {
    pageNum: number
    pageSize: number
    lineageType?: string
    sourceDsId?: number
    sourceTable?: string
    sourceColumn?: string
    targetDsId?: number
    targetTable?: string
    targetColumn?: string
    transformType?: string
    lineageSource?: string
    status?: string
    deptId?: number
  }) {
    return request.get<Result>('/gov/lineage/page', { params })
  },

  // 统计
  getStats() {
    return request.get<Result<LineageStatsVO>>('/gov/lineage/stats')
  },

  // 单条详情
  getById(id: number) {
    return request.get<Result<LineageVO>>(`/gov/lineage/${id}`)
  },

  // 血缘详情（含关联信息）
  getDetail(id: number) {
    return request.get<Result<LineageVO>>(`/gov/lineage/detail/${id}`)
  },

  // 新增
  save(data: LineageSaveDTO) {
    return request.post<Result<LineageVO>>('/gov/lineage', data)
  },

  // 批量新增
  batchSave(data: LineageSaveDTO[]) {
    return request.post<Result>('/gov/lineage/batch', data)
  },

  // 更新
  update(id: number, data: LineageSaveDTO) {
    return request.put<Result>(`/gov/lineage/${id}`, data)
  },

  // 删除
  delete(id: number) {
    return request.delete<Result>(`/gov/lineage/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/lineage/batch', { data: ids })
  }
}
