import request from '@/utils/request'
import type { Result } from '@/types'

// ============================================================
// 数据安全-分类分级标准管理
// 后端路径: /api/gov/security
// ============================================================

// Page type (for MyBatis-Plus pagination)
export interface Page<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ---------- 枚举类型 ----------

// 敏感级别
export type SensitivityLevel = 'L1' | 'L2' | 'L3' | 'L4'

// 脱敏类型
export type MaskType =
  | 'NONE' | 'MASK' | 'HIDE' | 'ENCRYPT' | 'DELETE'
  | 'PARTIAL_MASK' | 'HASH' | 'FORMAT_KEEP' | 'RANGE'
  | 'WATERMARK' | 'RANDOM_REPLACE'

// 匹配类型
export type MatchType = 'COLUMN_NAME' | 'COLUMN_COMMENT' | 'DATA_TYPE' | 'REGEX' | 'CONTENT_ALGORITHM' | 'CONTENT_REGEX'

// 审核状态
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// 扫描范围
export type ScanScope = 'ALL' | 'LAYER' | 'SPECIFIC'

// 表达式类型
export type ExprType = 'CONTAINS' | 'EQUALS' | 'STARTS_WITH' | 'REGEX'

// ---------- 数据分类相关类型 ----------

// 数据分类列表项
export interface SecClassificationVO {
  id?: number
  classCode?: string
  className?: string
  classDesc?: string
  classOrder?: number
  sensitivityLevel?: string
  status?: number
  statusLabel?: string
  ruleCount?: number
  sensitiveFieldCount?: number
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 数据分类详情
export interface SecClassificationDetailVO {
  id?: number
  classCode?: string
  className?: string
  classDesc?: string
  classOrder?: number
  sensitivityLevel?: string
  sensitivityLevelLabel?: string
  status?: number
  statusLabel?: string
  ruleCount?: number
  sensitiveFieldCount?: number
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 数据分类保存DTO
export interface SecClassificationSaveDTO {
  id?: number
  classCode?: string
  className?: string
  classDesc?: string
  classOrder?: number
  sensitivityLevel?: string
  status?: number
}

// 数据分类查询DTO
export interface SecClassificationQueryDTO {
  classCode?: string
  className?: string
  enabled?: number
}

// ---------- 敏感等级相关类型 ----------

// 敏感等级列表项
export interface SecLevelVO {
  id?: number
  levelCode?: string
  levelName?: string
  levelValue?: number
  levelDesc?: string
  color?: string
  icon?: string
  status?: number
  statusLabel?: string
  sensitiveFieldCount?: number
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 敏感等级保存DTO
export interface SecLevelSaveDTO {
  id?: number
  levelCode?: string
  levelName?: string
  levelValue?: number
  levelDesc?: string
  color?: string
  icon?: string
}

// ---------- 敏感字段识别规则相关类型 ----------

// 敏感字段识别规则列表项
export interface SecSensitivityRuleVO {
  id?: number
  ruleName?: string
  ruleCode?: string
  classId?: number
  className?: string
  levelId?: number
  levelName?: string
  levelColor?: string
  matchType?: MatchType
  matchTypeLabel?: string
  matchExpr?: string
  matchExprType?: ExprType
  matchExprTypeLabel?: string
  builtinAlgorithm?: string
  builtinAlgorithmLabel?: string
  suggestionAction?: string
  suggestionMaskPattern?: string
  suggestionMaskType?: string
  priority?: number
  builtin?: number
  builtinLabel?: string
  status?: number
  statusLabel?: string
  description?: string
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 敏感字段识别规则保存DTO
export interface SecSensitivityRuleSaveDTO {
  id?: number
  ruleName?: string
  ruleCode?: string
  classId?: number
  levelId?: number
  matchType?: MatchType
  matchExpr?: string
  matchExprType?: ExprType
  suggestionAction?: string
  suggestionMaskPattern?: string
  suggestionMaskType?: string
  priority?: number
  status?: number
  description?: string
}

// 敏感字段识别规则查询DTO
export interface SecSensitivityRuleQueryDTO {
  matchType?: MatchType
  enabled?: number
}

// ---------- 敏感字段记录相关类型 ----------

// 敏感字段记录列表项
export interface SecColumnSensitivityVO {
  id?: number
  dsId?: number
  dsName?: string
  metadataId?: number
  tableName?: string
  columnName?: string
  columnComment?: string
  dataType?: string
  classId?: number
  className?: string
  levelId?: number
  levelName?: string
  levelColor?: string
  maskType?: MaskType
  maskTypeLabel?: string
  maskPattern?: string
  maskPosition?: string
  maskChar?: string
  confidence?: number
  scanBatchNo?: string
  scanTime?: string
  reviewStatus?: ReviewStatus
  reviewStatusLabel?: string
  reviewComment?: string
  approvedBy?: number
  approvedByName?: string
  approvedTime?: string
  createUser?: number
  createTime?: string
}

// 敏感字段记录查询DTO
export interface SecColumnSensitivityQueryDTO {
  id?: number
  dsId?: number
  classId?: number
  levelId?: number
  reviewStatus?: ReviewStatus
  sensitivityLevel?: SensitivityLevel
  scanBatchNo?: string
}

// 敏感字段记录保存DTO
export interface SecColumnSensitivitySaveDTO {
  id?: number
  dsId?: number
  metadataId?: number
  tableName?: string
  columnName?: string
  columnComment?: string
  dataType?: string
  classId?: number
  levelId?: number
  maskType?: MaskType
  maskPattern?: string
  maskPosition?: string
  maskChar?: string
  createUser?: number
}

// 敏感字段扫描DTO
export interface SecSensitivityScanDTO {
  dsId?: number
  /** PostgreSQL 等：直连扫描时使用的 Schema，优先于数据源默认配置 */
  schema?: string
  scanScope?: ScanScope
  layerCode?: string
  tableNames?: string
  excludeSystemTables?: boolean
  scanColumnName?: boolean
  scanColumnComment?: boolean
  scanDataType?: boolean
  useRegex?: boolean
  sampleSize?: number
  enableContentScan?: boolean
  incremental?: boolean
  directScan?: boolean
  scanCycle?: string
}

// 敏感字段扫描结果
export interface SecScanResultVO {
  scanBatchNo?: string
  startTime?: string
  endTime?: string
  elapsedMs?: number
  dsId?: number
  dsName?: string
  totalTableCount?: number
  totalColumnCount?: number
  foundSensitiveCount?: number
  results?: SecColumnSensitivityVO[]
  countByLevel?: Record<string, number>
  countByClassification?: Record<string, number>
}

// ---------- 脱敏模板相关类型 ----------

// 脱敏模板列表项
export interface SecMaskTemplateVO {
  id?: number
  templateName?: string
  templateCode?: string
  templateDesc?: string
  description?: string
  dataType?: string
  maskType?: MaskType
  maskTypeLabel?: string
  maskPosition?: string
  maskChar?: string
  maskHeadKeep?: number
  maskTailKeep?: number
  maskPattern?: string
  sortOrder?: number
  builtin?: number
  builtinLabel?: string
  status?: number
  statusLabel?: string
  enabled?: number
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
}

// 脱敏模板保存DTO
export interface SecMaskTemplateSaveDTO {
  id?: number
  templateName?: string
  templateCode?: string
  templateDesc?: string
  description?: string
  dataType?: string
  maskType?: MaskType
  maskPosition?: string
  maskChar?: string
  maskHeadKeep?: number
  maskTailKeep?: number
  maskPattern?: string
  status?: number
}

// ---------- 统计相关类型 ----------

// 安全分类统计VO
export interface SecStatsVO {
  totalClassificationCount?: number
  totalLevelCount?: number
  totalRuleCount?: number
  builtinRuleCount?: number
  customRuleCount?: number
  totalSensitiveFieldCount?: number
  pendingReviewCount?: number
  approvedCount?: number
  rejectedCount?: number
  totalMaskTemplateCount?: number
  countByClassification?: Record<string, number>
  countByLevel?: Record<string, number>
  countByReviewStatus?: Record<string, number>
  countByMaskType?: Record<string, number>
  countByMatchType?: Record<string, number>
  disabledClassificationCount?: number
}

// ---------- 枚举选项 ----------

export interface SecurityEnumsVO {
  sensitivityLevel: Array<{ code: string; label: string; value: string; color: string }>
  maskType: Array<{ code: string; label: string }>
  matchType: Array<{ code: string; label: string }>
  reviewStatus: Array<{ code: string; label: string }>
  scanScope: Array<{ code: string; label: string }>
  exprType: Array<{ code: string; label: string }>
}

// ============================================================
// API 方法
// ============================================================

// ---------- 统计概览 ----------

export const getSecurityStats = () => {
  return request.get<Result<SecStatsVO>>('/gov/security/stats')
}

// ---------- 数据分类管理 ----------

export const pageClassification = (params: {
  pageNum: number
  pageSize: number
  classCode?: string
  className?: string
  enabled?: number
}) => {
  return request.get<Result<Page<SecClassificationVO>>>('/gov/security/classification/page', { params })
}

export const listClassification = (enabled?: number) => {
  return request.get<Result<SecClassificationVO[]>>('/gov/security/classification/list', {
    params: { enabled },
  })
}

export const saveClassification = (data: SecClassificationSaveDTO) => {
  return request.post<Result<any>>('/gov/security/classification', data)
}

export const updateClassification = (id: number, data: SecClassificationSaveDTO) => {
  return request.put<Result<void>>(`/gov/security/classification/${id}`, data)
}

export const deleteClassification = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/classification/${id}`)
}

export const enableClassification = (id: number) => {
  return request.post<Result<void>>(`/gov/security/classification/${id}/enable`)
}

export const disableClassification = (id: number) => {
  return request.post<Result<void>>(`/gov/security/classification/${id}/disable`)
}

export const getClassificationDetail = (id: number) => {
  return request.get<Result<SecClassificationDetailVO>>(`/gov/security/classification/${id}`)
}

// ---------- 敏感等级管理 ----------

export const pageLevel = (params: { pageNum: number; pageSize: number }) => {
  return request.get<Result<Page<SecLevelVO>>>('/gov/security/level/page', { params })
}

export const listLevel = () => {
  return request.get<Result<SecLevelVO[]>>('/gov/security/level/list')
}

export const saveLevel = (data: SecLevelSaveDTO) => {
  return request.post<Result<any>>('/gov/security/level', data)
}

export const updateLevel = (id: number, data: SecLevelSaveDTO) => {
  return request.put<Result<void>>(`/gov/security/level/${id}`, data)
}

export const deleteLevel = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/level/${id}`)
}

// ---------- 敏感字段识别规则 ----------

export const pageRule = (params: {
  pageNum: number
  pageSize: number
  matchType?: MatchType
  enabled?: number
}) => {
  return request.get<Result<Page<SecSensitivityRuleVO>>>('/gov/security/rule/page', { params })
}

export const listRule = (enabled?: number) => {
  return request.get<Result<SecSensitivityRuleVO[]>>('/gov/security/rule/list', {
    params: { enabled },
  })
}

export const saveRule = (data: SecSensitivityRuleSaveDTO) => {
  return request.post<Result<any>>('/gov/security/rule', data)
}

export const updateRule = (id: number, data: SecSensitivityRuleSaveDTO) => {
  return request.put<Result<void>>(`/gov/security/rule/${id}`, data)
}

export const deleteRule = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/rule/${id}`)
}

export const enableRule = (id: number) => {
  return request.post<Result<void>>(`/gov/security/rule/${id}/enable`)
}

export const disableRule = (id: number) => {
  return request.post<Result<void>>(`/gov/security/rule/${id}/disable`)
}

// ---------- 敏感字段扫描与审核 ----------

export const scanSensitiveFields = (data: SecSensitivityScanDTO) => {
  return request.post<Result<SecScanResultVO>>('/gov/security/scan', data)
}

export const pageSensitivity = (params: {
  pageNum: number
  pageSize: number
  dsId?: number
  tableName?: string
  classId?: number
  levelId?: number
  reviewStatus?: ReviewStatus
  sensitivityLevel?: SensitivityLevel
}) => {
  return request.get<Result<Page<SecColumnSensitivityVO>>>('/gov/security/sensitivity/page', { params })
}

export const getSensitivityById = (id: number) => {
  return request.get<Result<SecColumnSensitivityVO>>(`/gov/security/sensitivity/${id}`)
}

export const reviewSensitivity = (
  id: number,
  reviewStatus: ReviewStatus,
  reviewComment?: string
) => {
  return request.post<Result<void>>(`/gov/security/sensitivity/${id}/review`, null, {
    params: { reviewStatus, reviewComment },
  })
}

export const batchReviewSensitivity = (
  ids: number[],
  reviewStatus: ReviewStatus,
  reviewComment?: string
) => {
  return request.post<Result<void>>('/gov/security/sensitivity/batch-review', ids, {
    params: { reviewStatus, reviewComment },
  })
}

export const deleteSensitivity = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/sensitivity/${id}`)
}

export const saveColumnSensitivity = (data: any) => {
  return request.post<Result<void>>('/gov/security/sensitivity/save', data)
}

export const batchSaveColumnSensitivity = (data: any[]) => {
  return request.post<Result<void>>('/gov/security/sensitivity/batch-save', data)
}

export const getColumnSensitivityByTable = (dsId: number, tableName: string) => {
  return request.get<Result<SecColumnSensitivityVO[]>>('/gov/security/sensitivity/table', {
    params: { dsId, tableName }
  })
}

export const getSensitivityByBatchNo = (batchNo: string) => {
  return request.get<Result<SecColumnSensitivityVO[]>>(`/gov/security/sensitivity/batch/${batchNo}`)
}

export const getSensitivityStatsByDs = () => {
  return request.get<Result<any[]>>('/gov/security/sensitivity/ds-stats')
}

/** 敏感字段审核汇总（总数/待审/通过/驳回），单条聚合 SQL */
export const getSensitivityReviewCounts = () => {
  return request.get<Result<Record<string, number>>>('/gov/security/sensitivity/review-counts')
}

// ---------- 脱敏模板 ----------

export const pageMaskTemplate = (params: { pageNum: number; pageSize: number; enabled?: number }) => {
  return request.get<Result<Page<SecMaskTemplateVO>>>('/gov/security/mask-template/page', { params })
}

export const listMaskTemplate = (enabled?: number) => {
  return request.get<Result<SecMaskTemplateVO[]>>('/gov/security/mask-template/list', {
    params: { enabled },
  })
}

export const saveMaskTemplate = (data: SecMaskTemplateSaveDTO) => {
  return request.post<Result<any>>('/gov/security/mask-template', data)
}

export const updateMaskTemplate = (id: number, data: SecMaskTemplateSaveDTO) => {
  return request.put<Result<void>>(`/gov/security/mask-template/${id}`, data)
}

export const deleteMaskTemplate = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/mask-template/${id}`)
}

export const enableMaskTemplate = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-template/${id}/enable`)
}

export const disableMaskTemplate = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-template/${id}/disable`)
}

// ---------- 枚举选项 ----------

export const getSecurityEnums = () => {
  return request.get<Result<SecurityEnumsVO>>('/gov/security/enums')
}

// ---------- 安全总览大盘 ----------

// 安全总览统计数据
export interface SecurityOverviewVO {
  totalSensitiveFieldCount?: number
  sensitiveCoverage?: number
  weeklyScanCount?: number
  pendingReviewCount?: number
  healthScore?: number
  healthGrade?: string
  healthGradeLabel?: string
  l4Count?: number
  l3Count?: number
  l2Count?: number
  l1Count?: number
  sensitiveFieldTrend?: Array<{ date: string; l4: number; l3: number; l2: number; l1: number }>
  sensitiveFieldDistribution?: Array<{ classification: string; count: number }>
  pendingReviewFields?: SecColumnSensitivityVO[]
  recentAlerts?: Array<{ id: number; alertTitle: string; alertLevel: string; alertType: string; createdAt: string }>
}

export const getSecurityOverview = () => {
  return request.get<Result<SecurityOverviewVO>>('/gov/security/overview')
}

export const getSecurityOverviewTrend = (days: number = 30) => {
  return request.get<Result<any>>('/gov/security/overview/trend', {
    params: { days }
  })
}

// ---------- 脱敏策略 ----------

export interface SecMaskStrategyVO {
  id?: number
  strategyName?: string
  strategyCode?: string
  sceneType?: string
  sceneTypeLabel?: string
  status?: number
  statusLabel?: string
  ruleCount?: number
  whitelistCount?: number
  description?: string
  priority?: number
  conflictCheck?: boolean
  levelMaskMapping?: string
  createUser?: number
  createUserName?: string
  createTime?: string
}

export interface SecMaskStrategySaveDTO {
  id?: number
  strategyName?: string
  strategyCode?: string
  sceneType?: string
  description?: string
  status?: number
  levelRules?: Array<{ levelCode: string; maskType: string; maskPattern?: string }>
  priority?: number
  conflictCheck?: boolean
  levelMaskMapping?: string
}

export interface SecMaskStrategyDetailVO extends SecMaskStrategyVO {
  levelRules?: Array<{ levelCode: string; levelName: string; maskType: string; maskTypeLabel: string; maskPattern: string }>
  whitelistData?: SecMaskWhitelistVO[]
}

export const pageMaskStrategy = (params: {
  pageNum: number
  pageSize: number
  sceneType?: string
  status?: string
}) => {
  return request.get<Result<Page<SecMaskStrategyVO>>>('/gov/security/mask-strategy/page', { params })
}

export const getMaskStrategyById = (id: number) => {
  return request.get<Result<SecMaskStrategyDetailVO>>(`/gov/security/mask-strategy/${id}`)
}

export const listMaskStrategy = (sceneType?: string) => {
  return request.get<Result<SecMaskStrategyVO[]>>('/gov/security/mask-strategy/list', {
    params: { sceneType }
  })
}

export const saveMaskStrategy = (data: SecMaskStrategySaveDTO) => {
  return request.post<Result<any>>('/gov/security/mask-strategy', data)
}

export const updateMaskStrategy = (id: number, data: SecMaskStrategySaveDTO) => {
  return request.put<Result<void>>(`/gov/security/mask-strategy/${id}`, data)
}

export const deleteMaskStrategy = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/mask-strategy/${id}`)
}

export const enableMaskStrategy = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-strategy/${id}/enable`)
}

export const disableMaskStrategy = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-strategy/${id}/disable`)
}

export const detectStrategyConflicts = () => {
  return request.get<Result<string[]>>('/gov/security/mask-strategy/conflicts')
}

// ---------- 脱敏白名单 ----------

export interface SecMaskWhitelistVO {
  id?: number
  strategyId?: number
  strategyName?: string
  entityType?: string
  entityTypeLabel?: string
  entityId?: number
  entityName?: string
  validFrom?: string
  validTo?: string
  status?: string
  statusLabel?: string
  description?: string
  reason?: string
  createUser?: number
  createUserName?: string
  createTime?: string
}

export interface SecMaskWhitelistSaveDTO {
  id?: number
  strategyId?: number
  entityType?: string
  entityId?: number
  entityName?: string
  validFrom?: string
  validTo?: string
  description?: string
  status?: number
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type DayjsLike = { format: (fmt: string) => string } & any

export const pageMaskWhitelist = (params: {
  pageNum: number
  pageSize: number
  strategyId?: number
  entityType?: string
  status?: string
}) => {
  return request.get<Result<Page<SecMaskWhitelistVO>>>('/gov/security/mask-whitelist/page', { params })
}

export const saveMaskWhitelist = (data: SecMaskWhitelistSaveDTO) => {
  return request.post<Result<any>>('/gov/security/mask-whitelist', data)
}

export const updateMaskWhitelist = (id: number, data: SecMaskWhitelistSaveDTO) => {
  return request.put<Result<void>>(`/gov/security/mask-whitelist/${id}`, data)
}

export const deleteMaskWhitelist = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/mask-whitelist/${id}`)
}

export const approveMaskWhitelist = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-whitelist/${id}/approve`)
}

export const revokeMaskWhitelist = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-whitelist/${id}/revoke`)
}

export const isInWhitelist = (strategyId: number, entityType: string, entityId: number) => {
  return request.get<Result<boolean>>('/gov/security/mask-whitelist/check', {
    params: { strategyId, entityType, entityId }
  })
}

// ---------- 访问审批管理 ----------

export interface SecAccessApplicationVO {
  id?: number
  applicationNo?: string
  applicantId?: number
  applicantName?: string
  applicantDept?: string
  targetDsId?: number
  targetDsName?: string
  targetTable?: string
  targetColumns?: string
  sensitivityLevel?: string
  sensitivityLevelLabel?: string
  accessReason?: string
  durationHours?: number
  status?: string
  statusLabel?: string
  approverId?: number
  approverName?: string
  approvalComment?: string
  approvalTime?: string
  applyTime?: string
  expiryTime?: string
}

export interface SecAccessApplicationSaveDTO {
  targetDsId?: number
  targetTable?: string
  targetColumns?: string
  accessReason?: string
  durationHours?: number
}

export interface SecAccessApprovalDTO {
  id?: number
  status?: string
  approvalComment?: string
  approverId?: number
  approverName?: string
}

export interface SecAccessAuditVO {
  id?: number
  applicationId?: number
  applicationNo?: string
  operatorId?: number
  operatorName?: string
  operatorDept?: string
  operationType?: string
  operationTypeLabel?: string
  operationContent?: string
  targetTable?: string
  targetColumn?: string
  ipAddress?: string
  status?: string
  statusLabel?: string
  operationTime?: string
}

export interface SecAccessLogVO {
  id?: number
  operationType?: string
  operationTypeLabel?: string
  operatorId?: number
  operatorName?: string
  dsId?: number
  dsName?: string
  tableName?: string
  columnName?: string
  operationDetail?: string
  ipAddress?: string
  status?: string
  statusLabel?: string
  operationTime?: string
}

export const submitAccessApplication = (data: SecAccessApplicationSaveDTO) => {
  return request.post<Result<SecAccessApplicationVO>>('/gov/security/access-apply', data)
}

export const pageAccessApply = (params: {
  pageNum: number
  pageSize: number
  applicationNo?: string
  applicantId?: number
  applicantName?: string
  targetDsId?: number
  targetTable?: string
  status?: string
}) => {
  return request.get<Result<Page<SecAccessApplicationVO>>>('/gov/security/access-apply/page', { params })
}

export const pagePendingApproval = (params: {
  pageNum: number
  pageSize: number
  approverId?: number
}) => {
  return request.get<Result<Page<SecAccessApplicationVO>>>('/gov/security/access-apply/pending', { params })
}

export const getAccessApplyById = (id: number) => {
  return request.get<Result<SecAccessApplicationVO>>(`/gov/security/access-apply/${id}`)
}

export const cancelAccessApplication = (id: number, operatorId: number, operatorName: string) => {
  return request.post<Result<void>>(`/gov/security/access-apply/${id}/cancel`, null, {
    params: { operatorId, operatorName }
  })
}

export const approveAccessApplication = (data: SecAccessApprovalDTO) => {
  return request.post<Result<void>>('/gov/security/access-apply/approve', data)
}

export const listAccessAuditHistory = (applicationId: number) => {
  return request.get<Result<SecAccessAuditVO[]>>(`/gov/security/access-apply/${applicationId}/audit-history`)
}

export const pageAccessLog = (params: {
  pageNum: number
  pageSize: number
  applicationId?: number
  operationType?: string
  operatorId?: number
}) => {
  return request.get<Result<Page<SecAccessLogVO>>>('/gov/security/access-log/page', { params })
}

export const getAccessStats = () => {
  return request.get<Result<any>>('/gov/security/access-stats')
}

// ---------- 扫描任务管理 ----------

export interface SecScanScheduleVO {
  id?: number
  scheduleName?: string
  scanType?: string
  scanTypeLabel?: string
  dsId?: number
  dsName?: string
  scanScope?: string
  cronExpression?: string
  cronDescription?: string
  enabled?: number
  enabledLabel?: string
  lastRunTime?: string
  lastRunStatus?: string
  lastRunStatusLabel?: string
  nextRunTime?: string
  createUser?: number
  createUserName?: string
  createTime?: string
}

export const pageScanSchedule = (params: {
  pageNum: number
  pageSize: number
  scanType?: string
  dsId?: number
  enabled?: number
}) => {
  return request.get<Result<Page<SecScanScheduleVO>>>('/gov/security/scan-schedule/page', { params })
}

export const getScanHistory = (params: {
  pageNum: number
  pageSize: number
  batchNo?: string
  dsId?: number
  status?: string
}) => {
  return request.get<Result<Page<any>>>('/gov/security/scan/history', { params })
}

// ---------- 脱敏执行日志 ----------

export interface SecMaskExecutionLogVO {
  id?: number
  taskId?: number
  taskName?: string
  taskType?: string
  taskTypeLabel?: string
  status?: string
  statusLabel?: string
  startTime?: string
  endTime?: string
  elapsedMs?: number
  sourceRows?: number
  maskedRows?: number
  errorMessage?: string
  createTime?: string
}

export const pageMaskExecutionLog = (taskId: number, params: {
  pageNum: number
  pageSize: number
}) => {
  return request.get<Result<Page<SecMaskExecutionLogVO>>>(`/gov/security/mask-task/${taskId}/log/page`, { params })
}

// ---------- 脱敏任务管理 ----------

export interface SecMaskTaskVO {
  id?: number
  taskName?: string
  taskCode?: string
  taskType?: string
  taskTypeLabel?: string
  sourceDsId?: number
  sourceDsName?: string
  sourceSql?: string
  targetDsId?: number
  targetDsName?: string
  targetTable?: string
  triggerType?: string
  triggerTypeLabel?: string
  triggerCron?: string
  status?: string
  statusLabel?: string
  lastRunTime?: string
  lastRunStatus?: string
  lastRunStatusLabel?: string
  enabled?: number
  enabledLabel?: string
  createUser?: number
  createUserName?: string
  createTime?: string
}

export interface SecMaskTaskSaveDTO {
  id?: number
  taskName?: string
  taskCode?: string
  taskType?: string
  sourceDsId?: number
  sourceSql?: string
  targetDsId?: number
  targetTable?: string
  maskRules?: string
  triggerType?: string
  triggerCron?: string
  status?: number
}

export const pageMaskTask = (params: {
  pageNum: number
  pageSize: number
  dsId?: number
  status?: string
  taskType?: string
}) => {
  return request.get<Result<Page<SecMaskTaskVO>>>('/gov/security/mask-task/page', { params })
}

export const getMaskTaskById = (id: number) => {
  return request.get<Result<SecMaskTaskVO>>(`/gov/security/mask-task/${id}`)
}

export const saveMaskTask = (data: SecMaskTaskSaveDTO) => {
  return request.post<Result<any>>('/gov/security/mask-task', data)
}

export const updateMaskTask = (id: number, data: SecMaskTaskSaveDTO) => {
  return request.put<Result<void>>(`/gov/security/mask-task/${id}`, data)
}

export const deleteMaskTask = (id: number) => {
  return request.delete<Result<void>>(`/gov/security/mask-task/${id}`)
}

export const batchDeleteMaskTask = (ids: number[]) => {
  return request.delete<Result<void>>('/gov/security/mask-task/batch', { data: ids })
}

export const enableMaskTask = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-task/${id}/enable`)
}

export const disableMaskTask = (id: number) => {
  return request.post<Result<void>>(`/gov/security/mask-task/${id}/disable`)
}

export const executeMaskTask = (id: number) => {
  return request.post<Result<SecMaskExecutionLogVO>>(`/gov/security/mask-task/${id}/execute`)
}

// ---------- 健康分 ----------

export interface HealthScoreVO {
  score?: number
  grade?: string
  gradeLabel?: string
  sensitivityCoverageScore?: number
  complianceRateScore?: number
  alertResponseRateScore?: number
  governanceTimelinessScore?: number
  lastUpdated?: string
}

export const getHealthScore = () => {
  return request.get<Result<HealthScoreVO>>('/gov/security/health-score')
}

// ---------- 新字段发现（T6） ----------

export interface SecNewColumnAlert {
  id?: number
  dsId?: number
  tableName?: string
  columnName?: string
  dataType?: string
  columnComment?: string
  alertType?: string
  status?: string
  scanBatchNo?: string
  handleComment?: string
  handleUser?: number
  handleTime?: string
  createdAt?: string
  dsName?: string
}

export const pageNewColumnAlerts = (params: {
  pageNum?: number
  pageSize?: number
  status?: string
  dsId?: number
  tableName?: string
}) => {
  return request.get<Result<Page<SecNewColumnAlert>>>('/gov/security/new-column-alerts', { params })
}

export const countPendingNewColumnAlerts = () => {
  return request.get<Result<number>>('/gov/security/new-column-alerts/pending-count')
}

export const scanNewColumnAlert = (alertId: number, operatorId?: number) => {
  return request.post<Result<any>>(`/gov/security/new-column-alerts/${alertId}/scan`, null, {
    params: { operatorId: operatorId ?? 1 }
  })
}

export const dismissNewColumnAlert = (alertId: number, operatorId?: number, comment?: string) => {
  return request.delete<Result<void>>(`/gov/security/new-column-alerts/${alertId}`, {
    params: { operatorId: operatorId ?? 1, comment }
  })
}

