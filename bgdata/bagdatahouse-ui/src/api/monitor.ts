import request from '@/utils/request'
import type { Result } from '@/types'

export interface TaskExecution {
  id?: number
  executionNo?: string
  taskId?: number
  taskName?: string
  taskType?: string
  taskTypeName?: string
  dsId?: number
  dsName?: string
  planId?: number
  planName?: string
  triggerType?: string
  triggerTypeName?: string
  triggerUser?: number
  triggerUserName?: string
  startTime?: string
  endTime?: string
  elapsedMs?: number
  status?: string
  progress?: number
  logContent?: string
  errorMsg?: string
  createTime?: string
}

export interface MonitorDashboardVO {
  todayExecutions?: number
  todaySuccess?: number
  todayFailed?: number
  runningCount?: number
  avgElapsedMs?: number
  avgSuccessRate7d?: number
  pendingAlerts?: number
  successRateToday?: number
  successRateChange?: number
  hourlyTrend?: Record<string, number>
  taskTypeDistribution?: Record<string, number>
  successRateTrend?: TrendPoint[]
  recentExecutions?: TaskExecutionSummary[]
  topFailedTasks?: TaskExecutionSummary[]
}

export interface TrendPoint {
  date: string
  total: number
  success: number
  failed: number
  successRate: number
  avgElapsedMs: number
}

export interface TaskExecutionSummary {
  id: number
  taskName: string
  taskType: string
  taskTypeName: string
  status: string
  elapsedMs?: number
  startTime?: string
  endTime?: string
  dsName?: string
  triggerType?: string
  triggerUserName?: string
  errorMsg?: string
  progress?: number
}

export const monitorApi = {
  // 监控大盘概览
  getOverview() {
    return request.get<Result<MonitorDashboardVO>>('/monitor/dashboard/overview')
  },

  // 执行记录分页查询
  pageExecutions(params: {
    pageNum: number
    pageSize: number
    taskId?: number
    taskType?: string
    status?: string
    triggerType?: string
    startDate?: string
    endDate?: string
  }) {
    return request.get<Result>('/monitor/dashboard/execution/page', { params })
  },

  // 执行记录详情
  getExecutionById(id: number) {
    return request.get<Result<TaskExecution>>(`/monitor/dashboard/execution/${id}`)
  },

  // 正在运行的任务
  getRunningExecutions() {
    return request.get<Result<Record<string, any>[]>>('/monitor/dashboard/execution/running')
  },

  // 取消执行
  cancelExecution(id: number) {
    return request.post<Result<boolean>>(`/monitor/dashboard/execution/${id}/cancel`)
  },

  // 重试执行
  retryExecution(id: number) {
    return request.post<Result<number>>(`/monitor/dashboard/execution/${id}/retry`)
  },

  // 执行趋势
  getExecutionTrend(days?: number) {
    return request.get<Result<Record<string, any>>>('/monitor/dashboard/execution/trend', {
      params: { days }
    })
  }
}

// ============================================================
// 告警规则 API
// 后端路径: /monitor/alert/rule
// ============================================================
export interface AlertRule {
  id?: number
  ruleName?: string
  ruleCode?: string
  ruleType?: string
  ruleTypeName?: string
  targetType?: string
  targetTypeName?: string
  targetId?: string
  targetName?: string
  conditionType?: string
  conditionTypeName?: string
  thresholdValue?: number
  thresholdMaxValue?: number
  fluctuationPct?: number
  comparisonType?: string
  consecutiveTriggers?: number
  alertLevel?: string
  alertLevelName?: string
  alertReceivers?: string
  alertChannels?: string | string[]
  alertTitleTemplate?: string
  alertContentTemplate?: string
  muteUntil?: string
  enabled?: boolean
  deptId?: number
  createUser?: number
  createTime?: string
  updateTime?: string
  // SENSITIVE 类型字段
  sensitivityLevel?: string
  sensitivityTable?: string
  sensitivityColumn?: string
  sensitivityDsId?: number
  spikeThresholdPct?: number
  unreviewThresholdDays?: number
  accessAnomalyOffHours?: string
  accessAnomalyThreshold?: number
}

export interface AlertRuleDTO {
  id?: number
  ruleName?: string
  ruleCode?: string
  ruleType?: string
  targetType?: string
  targetId?: string
  targetName?: string
  conditionType?: string
  thresholdValue?: number
  thresholdMaxValue?: number
  fluctuationPct?: number
  comparisonType?: string
  consecutiveTriggers?: number
  alertLevel?: string
  alertReceivers?: string
  alertChannels?: string | string[]
  alertTitleTemplate?: string
  alertContentTemplate?: string
  muteUntil?: string
  enabled?: boolean
  deptId?: number
  createUser?: number
  // SENSITIVE 类型字段
  sensitivityLevel?: string
  sensitivityTable?: string
  sensitivityColumn?: string
  sensitivityDsId?: number
  spikeThresholdPct?: number
  unreviewThresholdDays?: number
  accessAnomalyOffHours?: string
  accessAnomalyThreshold?: number
}

export const alertRuleApi = {
  page(params: {
    pageNum: number
    pageSize: number
    ruleName?: string
    ruleType?: string
    targetType?: string
    alertLevel?: string
    enabled?: number
  }) {
    return request.get<Result>(`/monitor/alert/rule/page`, { params })
  },

  getById(id: number) {
    return request.get<Result<AlertRule>>(`/monitor/alert/rule/${id}`)
  },

  create(data: AlertRuleDTO) {
    return request.post<Result<number>>('/monitor/alert/rule', data)
  },

  update(id: number, data: AlertRuleDTO) {
    return request.put<Result>(`/monitor/alert/rule/${id}`, data)
  },

  delete(id: number) {
    return request.delete<Result>(`/monitor/alert/rule/${id}`)
  },

  toggle(id: number) {
    return request.post<Result>(`/monitor/alert/rule/${id}/toggle`)
  },

  batchDelete(ids: number[]) {
    return request.post<Result>(`/monitor/alert/rule/batch/delete`, ids)
  },

  batchToggle(ids: number[]) {
    return request.post<Result>(`/monitor/alert/rule/batch/toggle`, ids)
  },

  getRuleTypeOptions() {
    return request.get<Result<Record<string, any>>>(`/monitor/alert/rule/options/rule-types`)
  },

  getTargetTypeOptions() {
    return request.get<Result<Record<string, any>>>(`/monitor/alert/rule/options/target-types`)
  },

  getTargetListByType(targetType: string) {
    return request.get<Result<Record<string, any>>>(`/monitor/alert/rule/targets`, {
      params: { targetType }
    })
  },

  // SENSITIVE 类型告警规则 API
  createSensityAlertRule(dto: AlertRuleDTO) {
    return request.post<Result<number>>(`/monitor/alert/rule/sensity`, dto)
  },

  updateSensityAlertRule(id: number, dto: AlertRuleDTO) {
    return request.put<Result<void>>(`/monitor/alert/rule/${id}/sensity`, dto)
  },

  getSensityAlertRulesByLevel(sensitivityLevel?: string) {
    return request.get<Result<AlertRule[]>>(`/monitor/alert/rule/sensity/by-level`, {
      params: { sensitivityLevel }
    })
  },

  getSensityAlertRulesByDsId(dsId?: number) {
    return request.get<Result<AlertRule[]>>(`/monitor/alert/rule/sensity/by-ds`, {
      params: { dsId }
    })
  }
}

// ============================================================
// 告警记录 API
// 后端路径: /monitor/alert/record
// ============================================================
export interface AlertRecord {
  id?: number
  alertNo?: string
  ruleId?: number
  ruleName?: string
  ruleCode?: string
  alertLevel?: string
  alertLevelName?: string
  alertTitle?: string
  alertContent?: string
  targetType?: string
  targetId?: string
  targetName?: string
  triggerValue?: number
  thresholdValue?: number
  status?: string
  statusName?: string
  sentChannels?: string
  sentTime?: string
  readTime?: string
  readUser?: number
  resolvedTime?: string
  resolveUser?: number
  resolveComment?: string
  createTime?: string
  // SENSITIVE 类型字段
  sensitivityLevel?: string
  sensitivityTable?: string
  sensitivityColumn?: string
  sensitivityDsId?: number
  scanBatchNo?: string
}

export interface AlertOverview {
  pendingCount?: number
  todayTotal?: number
  todayResolved?: number
  criticalCount?: number
}

export const alertRecordApi = {
  page(params: {
    pageNum: number
    pageSize: number
    ruleName?: string
    alertLevel?: string
    status?: string
    targetType?: string
    startDate?: string
    endDate?: string
    ruleType?: string
    sensitivityLevel?: string
  }) {
    return request.get<Result>(`/monitor/alert/record/page`, { params })
  },

  getById(id: number) {
    return request.get<Result<AlertRecord>>(`/monitor/alert/record/${id}`)
  },

  markAsRead(id: number) {
    return request.post<Result>(`/monitor/alert/record/${id}/read`)
  },

  batchMarkAsRead(ids: number[]) {
    return request.post<Result>(`/monitor/alert/record/batch/read`, ids)
  },

  resolve(id: number, resolveComment?: string) {
    return request.post<Result>(`/monitor/alert/record/${id}/resolve`, null, {
      params: { resolveComment }
    })
  },

  batchResolve(ids: number[], resolveComment?: string) {
    return request.post<Result>(`/monitor/alert/record/batch/resolve`, ids, {
      params: { resolveComment }
    })
  },

  getOverview() {
    return request.get<Result<AlertOverview>>(`/monitor/alert/record/overview`)
  },

  getDistribution(params?: { startDate?: string; endDate?: string }) {
    return request.get<Result<Record<string, number>>>(`/monitor/alert/record/distribution`, { params })
  },

  getTrend(days?: number) {
    return request.get<Result<Record<string, any>>>(`/monitor/alert/record/trend`, {
      params: { days }
    })
  }
}
