export interface Result<T = any> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp: number
}

export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface PageParams {
  pageNum?: number
  pageSize?: number
}

export interface Plan {
  id?: number
  planName: string
  planCode: string
  planDesc?: string
  bindType?: string
  bindValue?: string
  layerCode?: string
  deptId?: number
  triggerType?: string
  triggerCron?: string
  alertOnSuccess?: boolean
  alertOnFailure?: boolean
  autoBlock?: boolean
  status?: string
  createUser?: number
  createTime?: string
  updateTime?: string
  ruleCount?: number
  tableCount?: number
  sensitivityLevel?: string    // L1/L2/L3/L4，质检方案关联的敏感等级维度
  sensitivityClass?: string    // 关联的敏感分类
  sensitivityComplianceRate?: number  // 敏感字段合规率（0-100）
}

export interface PlanRule {
  planId?: number
  ruleId?: number
  ruleName?: string
  ruleCode?: string
  ruleType?: string
  ruleStrength?: string
  ruleOrder?: number
  customThreshold?: string
  skipOnFailure?: boolean
  targetTable?: string
  targetColumn?: string
}

export interface TableColumnBinding {
  tableName: string
  columns: string[]
}

export interface RuleBinding {
  id: number
  ruleName: string
  ruleType: string
  ruleStrength: string
  enabled: boolean
  targetColumn?: string
}

export interface PlanRuleBindVO {
  id?: number
  planId?: number
  ruleId?: number
  ruleName?: string
  ruleCode?: string
  ruleType?: string
  ruleStrength?: string
  ruleOrder?: number
  enabled?: boolean
  skipOnFailure?: boolean
  customThreshold?: string
  targetTable?: string
  targetColumn?: string
}

export interface Execution {
  id?: number
  executionNo: string
  planId?: number
  planName?: string
  planCode?: string
  ruleId?: number
  ruleName?: string
  targetTable?: string
  triggerType?: string
  triggerUser?: number
  startTime?: string
  endTime?: string
  elapsedMs?: number
  status?: string
  totalRules?: number
  passedRules?: number
  failedRules?: number
  skippedRules?: number
  totalCount?: number
  errorCount?: number
  qualityScore?: number
  scoreBreakdown?: string
  dimensionScores?: string
  sensitivityLevel?: string
  sensitivityComplianceRate?: number
  errorDetail?: string
  createTime?: string
}

export interface ExecutionDetail {
  id?: number
  executionId?: number
  ruleId?: number
  ruleName?: string
  targetTable?: string
  status?: string
  totalCount?: number
  errorCount?: number
  qualityScore?: number
  elapsedMs?: number
  startTime?: string
  endTime?: string
  errorDetail?: string
}

export interface QualityScore {
  id?: number
  checkDate?: string
  targetDsId?: number
  targetTable?: string
  layerCode?: string
  deptId?: number
  completenessScore?: number
  uniquenessScore?: number
  accuracyScore?: number
  consistencyScore?: number
  timelinessScore?: number
  validityScore?: number
  overallScore?: number
  rulePassRate?: number
  ruleTotalCount?: number
  rulePassCount?: number
  createTime?: string
}

export interface RuleTemplate {
  id?: number
  templateCode: string
  templateName: string
  templateDesc?: string
  ruleType: string
  applyLevel: string
  defaultExpr?: string
  defaultThreshold?: string
  paramSpec?: string
  dimension?: string
  builtin?: boolean
  enabled?: boolean
  createUser?: number
  createTime?: string
  updateUser?: number
  updateTime?: string
}

export interface RuleTemplateDTO {
  id?: number
  templateCode: string
  templateName: string
  templateDesc?: string
  ruleType: string
  applyLevel: string
  defaultExpr?: string
  defaultThreshold?: string
  paramSpec?: string
  dimension?: string
  builtin?: boolean
  enabled?: boolean
}

export interface RuleDef {
  id?: number
  ruleCode: string
  ruleName: string
  templateId?: number
  ruleType: string
  applyLevel: string
  layerCode?: string
  ruleExpr?: string
  targetDsId?: number
  targetTable?: string
  targetColumn?: string
  compareTable?: string
  compareColumn?: string
  thresholdMin?: number
  thresholdMax?: number
  fluctuationThreshold?: number
  dimensions?: string | string[]
  errorLevel?: string
  ruleStrength?: string
  alertReceivers?: string
  sortOrder?: number
  enabled?: boolean
  builtin?: boolean
  createUser?: number
  createTime?: string
  updateUser?: number
  updateTime?: string
}

export interface DataSource {
  id?: number
  dsName: string
  dsCode: string
  dsType: string
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string
  username?: string
  password?: string
  connectionParams?: string
  dataLayer?: string
  deptId?: number
  ownerId?: number
  status?: number
  remark?: string
  createTime?: string
}
