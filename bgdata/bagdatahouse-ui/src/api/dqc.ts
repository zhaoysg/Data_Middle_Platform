import request from '@/utils/request'
import type { Result, RuleTemplate, RuleTemplateDTO, RuleDef, Plan, Execution, DataSource, QualityScore, PlanRuleBindVO } from '@/types'

// ============================================================
// 规则模板
// ============================================================
export const ruleTemplateApi = {
  page(params: {
    pageNum: number
    pageSize: number
    templateName?: string
    ruleType?: string
    applyLevel?: string
    builtin?: number
  }) {
    return request.get<Result>('/dqc/rule-template/page', { params })
  },

  getById(id: number) {
    return request.get<Result>(`/dqc/rule-template/${id}`)
  },

  create(data: RuleTemplateDTO) {
    return request.post<Result<number>>('/dqc/rule-template', data)
  },

  update(id: number, data: RuleTemplateDTO) {
    return request.put<Result>(`/dqc/rule-template/${id}`, data)
  },

  delete(id: number) {
    return request.delete<Result>(`/dqc/rule-template/${id}`)
  },

  listEnabled() {
    return request.get<Result<RuleTemplate[]>>('/dqc/rule-template/enabled')
  },

  listGrouped() {
    return request.get<Result<Record<string, RuleTemplate[]>>>('/dqc/rule-template/grouped')
  }
}

// ============================================================
// 规则定义
// 后端路径: /dqc/rule
// ============================================================
export const ruleDefApi = {
  page(params: {
    pageNum: number
    pageSize: number
    ruleName?: string
    ruleType?: string
    applyLevel?: string
    layerCode?: string        // 补充：数据层
    enabled?: number           // 补充：1=启用，0=禁用
    ruleStrength?: string      // 补充：强规则/弱规则
    errorLevel?: string        // 补充：告警级别
    targetDsId?: number
  }) {
    return request.get<Result>(`/dqc/rule/page`, { params })
  },

  getById(id: number) {
    return request.get<Result>(`/dqc/rule/${id}`)
  },

  create(data: any) {
    return request.post<Result<number>>('/dqc/rule', data)
  },

  update(id: number, data: any) {
    return request.put<Result>(`/dqc/rule/${id}`, data)
  },

  delete(id: number) {
    return request.delete<Result>(`/dqc/rule/${id}`)
  },

  toggle(id: number) {
    return request.post<Result>(`/dqc/rule/${id}/toggle`)
  },

  copy(id: number) {
    return request.post<Result>(`/dqc/rule/${id}/copy`)
  },

  execute(id: number) {
    return request.post<Result>(`/dqc/rule/${id}/execute`)
  },

  batchDelete(ids: number[]) {
    return request.post<Result>(`/dqc/rule/batch/delete`, ids)
  },

  batchToggle(ids: number[]) {
    return request.post<Result>(`/dqc/rule/batch/toggle`, ids)
  },

  getRuleTypeOptions() {
    return request.get<Result<Record<string, any>>>(`/dqc/rule/options`)
  },

  listByDsId(dsId: number) {
    return request.get<Result<RuleDef[]>>(`/dqc/rule/ds/${dsId}`)
  },

  listByTemplateId(templateId: number) {
    return request.get<Result<RuleDef[]>>(`/dqc/rule/template/${templateId}`)
  },

  listEnabledWithTemplate() {
    return request.get<Result<any[]>>('/dqc/rule/enabled-with-template')
  },

  preview(id: number, table?: string, column?: string, dsId?: number) {
    return request.get<Result<string>>(`/dqc/rule/${id}/preview`, {
      params: { table, column, dsId }
    })
  },

  validateExpression(params: {
    ruleType: string
    ruleExpr?: string
    targetTable?: string
    targetColumn?: string
    regexPattern?: string
    thresholdMin?: number
    thresholdMax?: number
  }) {
    return request.post<Result<string>>('/dqc/rule/validate-expression', null, { params })
  }
}

// ============================================================
// 质检方案
// 后端路径: /dqc/plan
// ============================================================
export const planApi = {
  page(params: {
    pageNum: number
    pageSize: number
    planName?: string
    layerCode?: string
    status?: string
    triggerType?: string
    sensitivityLevel?: string   // L1/L2/L3/L4
  }) {
    return request.get<Result>(`/dqc/plan/page`, { params })
  },

  getById(id: number) {
    return request.get<Result>(`/dqc/plan/${id}`)
  },

  create(data: any) {
    return request.post<Result<number>>('/dqc/plan', data)
  },

  update(id: number, data: any) {
    return request.put<Result>(`/dqc/plan/${id}`, data)
  },

  delete(id: number) {
    return request.delete<Result>(`/dqc/plan/${id}`)
  },

  publish(id: number) {
    return request.post<Result>(`/dqc/plan/${id}/publish`)
  },

  disable(id: number) {
    return request.post<Result>(`/dqc/plan/${id}/disable`)
  },

  getBoundRules(planId: number) {
    return request.get<Result<PlanRuleBindVO[]>>(`/dqc/plan/${planId}/rules`)
  },

  bindRules(planId: number, rules: {
    ruleId: number
    ruleOrder: number
    enabled: boolean
    skipOnFailure: boolean
    targetTable: string
    targetColumn: string
  }[]) {
    return request.post<Result>(`/dqc/plan/${planId}/rules`, rules)
  },

  unbindRule(planId: number, ruleId: number) {
    return request.delete<Result>(`/dqc/plan/${planId}/rules/${ruleId}`)
  },

  execute(id: number) {
    return request.post<Result<number>>(`/dqc/plan/${id}/execute`)
  },

  triggerByApi(id: number, triggerParams?: string) {
    return request.post<Result<number>>(`/dqc/plan/${id}/trigger`, null, {
      params: { triggerParams }
    })
  },

  getNextExecutionTime(id: number) {
    return request.get<Result<string>>(`/dqc/plan/${id}/next-execution`)
  },

  getExecutionStatus(id: number) {
    return request.get<Result<any>>(`/dqc/plan/${id}/execution-status`)
  },

  validateCron(cronExpression: string) {
    return request.get<Result<any>>('/dqc/plan/validate-cron', {
      params: { cronExpression }
    })
  },

  cancel(id: number) {
    return request.post<Result<boolean>>(`/dqc/plan/${id}/cancel`)
  },

  getOverview() {
    return request.get<Result<Record<string, any>>>('/dqc/plan/overview')
  }
}

// ============================================================
// 执行记录
// 后端路径: /dqc/execution
// ============================================================
export const executionApi = {
  page(params: {
    pageNum: number
    pageSize: number
    planId?: number
    status?: string
    triggerType?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result>(`/dqc/execution/page`, { params })
  },

  getById(id: number) {
    return request.get<Result>(`/dqc/execution/${id}`)
  },

  getByExecutionNo(executionNo: string) {
    return request.get<Result>(`/dqc/execution/no/${executionNo}`)
  },

  getDetails(executionId: number) {
    return request.get<Result<any[]>>(`/dqc/execution/${executionId}/details`)
  },

  getDetailById(detailId: number) {
    return request.get<Result<any>>(`/dqc/execution/details/${detailId}`)
  },

  trigger(planId: number, triggerType = 'MANUAL') {
    return request.post<Result<any>>(`/dqc/execution/execute/${planId}`, null, {
      params: { triggerType }
    })
  },

  cancel(id: number) {
    return request.post<Result<boolean>>(`/dqc/execution/cancel/${id}`)
  },

  export(params: any) {
    return request.get('/dqc/execution/report/export', {
      params,
      responseType: 'blob'
    })
  },

  exportExecutionReport(executionId: number) {
    return request.get(`/dqc/execution/report/export/execution/${executionId}`, {
      responseType: 'blob'
    })
  },

  getReport(params: {
    planId?: number
    startDate?: string
    endDate?: string
    layerCode?: string
  }) {
    return request.get<Result<any>>('/dqc/execution/report/plan', { params })
  },

  getOverallReport(params: { startDate?: string; endDate?: string }) {
    return request.get<Result<any>>('/dqc/execution/report/overall', { params })
  },

  getExecutionReport(executionId: number) {
    return request.get<Result<any>>(`/dqc/execution/report/execution/${executionId}`)
  }
}

// ============================================================
// 质量评分（挂载在 /dqc/execution/score 下）
// ============================================================
export const qualityScoreApi = {
  getTrend(params: {
    dimension?: string
    layerCode?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result<any>>('/dqc/execution/score/trend', { params })
  },

  getTrendComparison(params: {
    layerCode?: string
    currentPeriodStart: string
    currentPeriodEnd: string
    previousPeriodStart: string
    previousPeriodEnd: string
  }) {
    return request.get<Result<any>>('/dqc/execution/score/trend/comparison', { params })
  },

  getOverview(params?: { startDate?: string; endDate?: string }) {
    return request.get<Result<any>>('/dqc/execution/score/overview', { params })
  },

  getDimensionScores(params: {
    dimension?: string
    layerCode?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result<any[]>>('/dqc/execution/score/dimensions', { params })
  },

  getScoreDistribution(params: {
    layerCode?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result<any>>('/dqc/execution/score/distribution', { params })
  },

  getQualityAlerts(params: {
    scoreThreshold?: number
    layerCode?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result<any[]>>('/dqc/execution/score/alerts', { params })
  },

  getDailyScores(params: {
    dsId?: number
    tableName?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result<any[]>>('/dqc/execution/score/daily', { params })
  },

  getTableScores(params: {
    layerCode?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result<any[]>>('/dqc/execution/score/tables', { params })
  },

  getLayerScoreSummary(params?: { startDate?: string; endDate?: string }) {
    return request.get<Result<Record<string, any>>>('/dqc/execution/score/layers', { params })
  }
}

// ============================================================
// 数据源
// ============================================================
export const dataSourceApi = {
  page(params: {
    pageNum: number
    pageSize: number
    dsName?: string
    dsType?: string
    dataLayer?: string
    status?: number
  }) {
    return request.get<Result>('/dqc/datasource/page', { params })
  },

  getById(id: number) {
    return request.get<Result<DataSource>>(`/dqc/datasource/${id}`)
  },

  create(data: any) {
    return request.post<Result<number>>('/dqc/datasource', data)
  },

  update(id: number, data: any) {
    return request.put<Result>(`/dqc/datasource/${id}`, data)
  },

  delete(id: number) {
    return request.delete<Result>(`/dqc/datasource/${id}`)
  },

  testConnection(data: any) {
    return request.post<Result>('/dqc/datasource/test', data)
  },

  listEnabled() {
    return request.get<Result<DataSource[]>>('/dqc/datasource/enabled')
  },

  getTables(id: number) {
    return request.get<Result<string[]>>(`/dqc/datasource/${id}/tables`)
  },

  /** 直连库读取表字段（不依赖元数据扫描） */
  getTableColumns(id: number, tableName: string, schema?: string) {
    return request.get<Result<
      { columnName: string; dataType?: string; columnComment?: string; nullable?: boolean; primaryKey?: boolean }[]
    >>(`/dqc/datasource/${id}/table-columns`, {
      params: { tableName, ...(schema ? { schema } : {}) }
    })
  },

  /** 直连库读取 schema 列表（PostgreSQL 专用） */
  getSchemas(id: number) {
    return request.get<Result<string[]>>(`/dqc/datasource/${id}/schemas`)
  },

  /** 直连库按 schema 查表列表（PostgreSQL 专用） */
  getTableListBySchema(id: number, schema: string) {
    return request.get<Result<string[]>>(`/dqc/datasource/${id}/tables`, {
      params: { schema }
    })
  },

  // 获取数据源统计
  getStatistics() {
    return request.get<Result<any>>('/dqc/datasource/statistics')
  },

  // 启用数据源
  enable(id: number) {
    return request.post<Result<void>>(`/dqc/datasource/${id}/enable`)
  },

  // 禁用数据源
  disable(id: number) {
    return request.post<Result<void>>(`/dqc/datasource/${id}/disable`)
  },

  // 根据类型获取数据源
  listByType(dsType: string) {
    return request.get<Result<any[]>>(`/dqc/datasource/type/${dsType}`)
  },

  // 根据数据层获取数据源
  listByLayer(layerCode: string) {
    return request.get<Result<any[]>>(`/dqc/datasource/layer/${layerCode}`)
  },

  // 测试连接（详细信息）
  testConnectionDetail(data: any) {
    return request.post<Result<any>>('/dqc/datasource/test/detail', data)
  },

  // 根据ID测试连接
  testConnectionById(id: number) {
    return request.post<Result<any>>(`/dqc/datasource/${id}/test`)
  },

  /**
   * 只读 SELECT 预览（规则表达式调试）。关闭抽屉时请传入 AbortSignal 以取消 HTTP，服务端仍有语句超时保护。
   */
  previewSelect(id: number, sql: string, options?: { signal?: AbortSignal }) {
    return request.post<Result<{
      sqlExecuted: string
      rows: Record<string, unknown>[]
      rowCount: number
      truncated: boolean
      normalizeNote?: string
    }>>(`/dqc/datasource/${id}/preview-select`, { sql }, {
      timeout: 35000,
      signal: options?.signal
    })
  }
}
