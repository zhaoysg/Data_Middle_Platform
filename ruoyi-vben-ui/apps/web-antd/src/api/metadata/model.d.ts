/** 元数据表 */
export interface MetadataTable {
  id?: number;
  dsId?: number;
  dsName?: string;
  dsCode?: string;
  tableName?: string;
  tableAlias?: string;
  tableComment?: string;
  tableType?: string;
  dataLayer?: string;
  dataDomain?: string;
  rowCount?: number;
  storageBytes?: number;
  sourceUpdateTime?: string;
  sensitivityLevel?: string;
  ownerId?: number;
  ownerName?: string;
  deptId?: number;
  deptName?: string;
  catalogId?: number;
  catalogName?: string;
  tags?: string;
  lastScanTime?: string;
  status?: string;
  createTime?: string;
  createBy?: string;
  updateBy?: string;
  updateTime?: string;
}

/** 元数据字段 */
export interface MetadataColumn {
  id?: number;
  tableId?: number;
  dsId?: number;
  tableName?: string;
  columnName?: string;
  columnAlias?: string;
  columnComment?: string;
  dataType?: string;
  isNullable?: string;
  columnKey?: string;
  defaultValue?: string;
  isPrimaryKey?: boolean;
  isForeignKey?: boolean;
  fkReference?: string;
  isSensitive?: boolean;
  sensitivityLevel?: string;
  sortOrder?: number;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** 资产目录 */
export interface MetadataCatalog {
  id?: number;
  catalogName?: string;
  catalogCode?: string;
  catalogType?: string;
  parentId?: number;
  parentName?: string;
  sortOrder?: number;
  status?: string;
  remark?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
  children?: MetadataCatalog[];
}

/** 数据域 */
export interface MetadataDomain {
  id?: number;
  domainName?: string;
  domainCode?: string;
  domainDesc?: string;
  ownerId?: number;
  ownerName?: string;
  deptId?: number;
  deptName?: string;
  status?: string;
  remark?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** 数仓分层 */
export interface MetadataLayer {
  id?: number;
  layerCode?: string;
  layerName?: string;
  layerDesc?: string;
  layerColor?: string;
  sortOrder?: number;
  status?: string;
  remark?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** 扫描请求 */
export interface MetadataScan {
  dsId: number;
  scanMode?: 'ALL' | 'TABLES' | 'RULE';
  tableNames?: string[];
  ruleType?: 'CONTAINS' | 'PREFIX' | 'SUFFIX' | 'EXACT' | 'REGEX';
  includePattern?: string;
  excludePattern?: string;
  ignoreCase?: boolean;
  schemaName?: string;
  syncColumn?: boolean;
}

/** 扫描结果 */
export interface MetadataScanResult {
  scanLogId?: number;
  dsId?: number;
  dsName?: string;
  status?: string;
  totalTables?: number;
  successCount?: number;
  partialCount?: number;
  failedCount?: number;
  elapsedMs?: number;
  startTime?: string;
  endTime?: string;
  errors?: string[];
  logId?: number;
}

/** 扫描记录 */
export interface MetadataScanLog {
  id?: number;
  dsId?: number;
  dsName?: string;
  dsCode?: string;
  scanType?: string;
  status?: string;
  totalTables?: number;
  successCount?: number;
  partialCount?: number;
  failedCount?: number;
  errorDetail?: string;
  startTime?: string;
  endTime?: string;
  elapsedMs?: number;
  scanUserId?: number;
  scanUserName?: string;
  remark?: string;
}

// ============== DQC (Track A) ==============

/** DQC规则模板 */
export interface DqcRuleTemplate {
  id?: number;
  templateCode?: string;
  templateName?: string;
  templateDesc?: string;
  ruleType?: string; // NULL_CHECK/UNIQUE/REGEX/THRESHOLD/FLUCTUATION/CUSTOM_SQL
  applyLevel?: string; // TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE
  defaultExpr?: string;
  thresholdJson?: string;
  paramSpec?: string;
  dimension?: string; // COMPLETENESS/UNIQUENESS/ACCURACY/CONSISTENCY/TIMELINESS/VALIDITY/CUSTOM
  builtin?: string; // 1=内置
  enabled?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** DQC规则定义 */
export interface DqcRuleDef {
  id?: number;
  ruleCode?: string;
  ruleName?: string;
  ruleDesc?: string;
  templateId?: number;
  templateName?: string;
  /** 关联元数据表ID (metadata_table.id) */
  tableId?: number;
  /** 关联元数据字段ID (metadata_column.id) */
  columnId?: number;
  /** 元数据表名称（用于展示） */
  tableName?: string;
  /** 元数据字段名称（用于展示） */
  columnName?: string;
  /** 数据源ID（从元数据获取） */
  dsId?: number;
  ruleType?: string;
  /** 质量维度，多个英文逗号分隔 */
  dimensions?: string;
  applyLevel?: string;
  ruleExpr?: string;
  targetDsId?: number;
  targetDsName?: string;
  dsName?: string;
  targetTable?: string;
  targetColumn?: string;
  compareDsId?: number;
  compareTable?: string;
  compareColumn?: string;
  thresholdMin?: number | string;
  thresholdMax?: number | string;
  fluctuationThreshold?: number | string;
  regexPattern?: string;
  errorLevel?: string;
  ruleStrength?: string;
  alertReceivers?: string;
  sortOrder?: number;
  enabled?: string;
  createBy?: string;
  createTime?: string;
}

/** DQC质检方案 */
export interface DqcPlan {
  id?: number;
  planCode?: string;
  planName?: string;
  planDesc?: string;
  bindType?: string; // TABLE / SENSITIVITY_LEVEL
  bindValue?: string; // JSON: { dsId, schema, tables, bindSensitivityLevel }
  dsId?: number;
  dsName?: string;
  layerCode?: string;
  triggerType?: string; // MANUAL / SCHEDULE / API
  triggerCron?: string;
  alertOnFailure?: string; // '1' = 告警
  alertOnSuccess?: string;
  autoBlock?: string; // '1' = 阻塞
  bindSensitivityLevel?: string; // L1/L2/L3/L4
  status?: string; // DRAFT / PUBLISHED / DISABLED
  ruleCount?: number;
  tableCount?: number;
  lastExecutionId?: number;
  lastScore?: number;
  lastExecutionTime?: string;
  sensitivityLevel?: string;
  sensitivityComplianceRate?: number;
  enabled?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** DQC方案-规则绑定记录 */
export interface DqcPlanRule {
  id?: number;
  planId?: number;
  ruleId?: number;
  targetTable?: string;
  targetColumn?: string;
  sortOrder?: number;
  skipOnFailure?: boolean;
  enabled?: boolean;
  createBy?: string;
  createTime?: string;
}

/** 规则选择器中的规则项 */
export interface DqcRuleSelectorItem {
  id?: number;
  ruleCode?: string;
  ruleName?: string;
  templateId?: number;
  templateName?: string;
  ruleType?: string;
  applyLevel?: string;
  dimension?: string;
  errorLevel?: string;
  ruleStrength?: string;
  ruleExpr?: string;
  defaultExpr?: string;
  paramSpec?: string;
  enabled?: string;
}

/** DQC执行记录 */
export interface DqcExecution {
  id?: number;
  executionNo?: string;
  planId?: number;
  planName?: string;
  layerCode?: string;
  triggerType?: string;
  triggerUser?: number;
  startTime?: string;
  endTime?: string;
  elapsedMs?: number;
  totalRules?: number;
  passedCount?: number;
  failedCount?: number;
  blockedCount?: number;
  overallScore?: number;
  status?: string; // RUNNING/SUCCESS/FAILED/PARTIAL
  createBy?: string;
  createTime?: string;
}

/** DQC执行明细 */
export interface DqcExecutionDetail {
  id?: number;
  executionId?: number;
  ruleId?: number;
  ruleName?: string;
  ruleCode?: string;
  ruleType?: string;
  dimension?: string;
  targetDsId?: number;
  targetTable?: string;
  targetColumn?: string;
  actualValue?: string;
  thresholdValue?: string;
  resultValue?: number;
  passFlag?: string; // 1=通过 0=失败
  errorLevel?: string;
  errorMsg?: string;
  executeSql?: string;
  elapsedMs?: number;
}

/** DQC质量评分 */
export interface DqcQualityScore {
  id?: number;
  checkDate?: string;
  targetDsId?: number;
  dsName?: string;
  targetTable?: string;
  layerCode?: string;
  executionId?: number;
  planId?: number;
  planName?: string;
  completenessScore?: number;
  uniquenessScore?: number;
  accuracyScore?: number;
  consistencyScore?: number;
  timelinessScore?: number;
  validityScore?: number;
  overallScore?: number;
  rulePassRate?: number;
  ruleTotalCount?: number;
  rulePassCount?: number;
  ruleFailCount?: number;
  createBy?: string;
  createTime?: string;
}

// ============== SECURITY (Track B) ==============

/** 数据分类 */
export interface SecClassification {
  id?: number;
  classCode?: string;
  className?: string;
  classDesc?: string;
  sortOrder?: number;
  /** 默认关联敏感等级编码（sec_level.level_code） */
  defaultLevelCode?: string;
  /** 等级名称（列表聚合展示） */
  defaultLevelName?: string;
  /** 等级颜色（列表 Tag） */
  defaultLevelColor?: string;
  /** 指向该分类的敏感识别规则数量 */
  ruleCount?: number;
  /** 该分类下敏感字段记录数量 */
  fieldCount?: number;
  enabled?: string;
  createBy?: string;
  createTime?: string;
}

/** 敏感等级 */
export interface SecLevel {
  id?: number;
  levelCode?: string;
  levelName?: string;
  levelValue?: number;
  levelDesc?: string;
  color?: string;
  sortOrder?: number;
  enabled?: string;
  createBy?: string;
  createTime?: string;
}

/** 敏感识别规则 */
export interface SecSensitivityRule {
  id?: number;
  ruleCode?: string;
  ruleName?: string;
  ruleType?: string; // REGEX/COLUMN_NAME/DATA_TYPE/CUSTOM
  ruleExpr?: string; // JSON
  targetLevelCode?: string;
  targetClassCode?: string;
  builtin?: string;
  enabled?: string;
  createBy?: string;
  createTime?: string;
}

/** 脱敏模板 */
export interface SecMaskTemplate {
  id?: number;
  templateCode?: string;
  templateName?: string;
  /** 脱敏类型：与 bgdata maskType 对齐 NONE/MASK/HIDE/ENCRYPT/DELETE/HASH/FORMAT_KEEP/RANGE/WATERMARK/SHUFFLE/CUSTOM */
  templateType?: string;
  /** 执行层 SQL 片段（可选） */
  maskExpr?: string;
  maskChar?: string;
  maskPosition?: string;
  maskHeadKeep?: number;
  maskTailKeep?: number;
  maskPattern?: string;
  templateDesc?: string;
  builtin?: string;
  enabled?: string;
  createBy?: string;
  createTime?: string;
}

/** 字段敏感记录 */
export interface SecColumnSensitivity {
  id?: number;
  dsId?: number;
  dsName?: string;
  tableName?: string;
  columnName?: string;
  dataType?: string;
  levelCode?: string;
  levelName?: string;
  levelColor?: string;
  classCode?: string;
  className?: string;
  matchRuleId?: number;
  matchRuleName?: string;
  identifiedBy?: string; // AUTO/MANUAL
  scanTaskId?: number;
  confirmed?: string; // 0待确认 1已确认
  confidence?: number; // 0-100
  confidenceLabel?: string;
  maskType?: string;
  maskTypeLabel?: string;
  maskPattern?: string;
  scanBatchNo?: string;
  scanTime?: string;
  createBy?: string;
  createTime?: string;
}

/** 敏感字段扫描参数 DTO */
export interface SecSensitivityScanDTO {
  dsId?: number;
  schema?: string;
  scanScope?: string; // ALL/SPECIFIC/LAYER
  tableNames?: string;
  layerCode?: string;
  scanColumnName?: boolean;
  scanColumnComment?: boolean;
  scanDataType?: boolean;
  useRegex?: boolean;
  excludeSystemTables?: boolean;
  sampleSize?: number;
  enableContentScan?: boolean;
  incremental?: boolean;
  scanCycle?: string; // ONCE/DAILY/WEEKLY/MONTHLY
  directScan?: boolean;
  columnNames?: string[];
  enableColumnNameMatch?: boolean;
  enableColumnCommentMatch?: boolean;
  scanMode?: string; // ALL/SINGLE_COLUMN
}

/** 敏感字段扫描结果 VO */
export interface SecScanResultVO {
  batchNo?: string;
  dsId?: number;
  dsName?: string;
  startTime?: number;
  endTime?: number;
  costMs?: number;
  totalTableCount?: number;
  totalColumnCount?: number;
  scannedTableCount?: number;
  scannedColumnCount?: number;
  foundSensitiveCount?: number;
  newSensitiveCount?: number;
  updatedSensitiveCount?: number;
  status?: string; // SUCCESS/FAILED/PARTIAL
  errorMessage?: string;
  sensitiveFields?: SecColumnSensitivity[];
}

/** 脱敏策略 */
export interface SecMaskStrategy {
  id?: number;
  strategyCode?: string;
  strategyName?: string;
  strategyDesc?: string;
  dsId?: number;
  dsName?: string;
  enabled?: string;
  createBy?: string;
  createTime?: string;
}

/** 脱敏策略明细 */
export interface SecMaskStrategyDetail {
  id?: number;
  strategyId?: number;
  dsId?: number;
  tableName?: string;
  columnName?: string;
  templateCode?: string;
  templateName?: string;
  outputAlias?: string;
}

/** 脱敏访问日志 */
export interface SecAccessLog {
  id?: number;
  dsId?: number;
  dsName?: string;
  userId?: number;
  userName?: string;
  querySql?: string;
  maskedSql?: string;
  resultRows?: number;
  maskedColumns?: string;
  elapsedMs?: number;
  status?: string;
  errorMsg?: string;
  ipAddress?: string;
  createBy?: string;
  createTime?: string;
}

/** 脱敏查询结果 */
export interface MaskQueryResult {
  rows?: Record<string, any>[];
  totalRows?: number;
  truncated?: boolean;
  maskedColumns?: string[];
  elapsedMs?: number;
}

// ============== DATA PROFILE (Track C) ==============

/** 数据探查任务 */
export interface DprofileTask {
  id?: number;
  taskCode?: string;
  taskName?: string;
  taskDesc?: string;
  dsId?: number;
  dsName?: string;
  profileLevel?: string; // BASIC/DETAILED/FULL
  tablePattern?: string;
  /** 指定探查的列名（逗号分隔，留空表示全部列） */
  targetColumns?: string;
  triggerType?: string;
  cronExpr?: string;
  status?: string; // DRAFT/RUNNING/SUCCESS/FAILED/STOPPED
  lastRunTime?: string;
  lastRunStatus?: string;
  totalTables?: number;
  enabled?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

/** 数据探查报告 */
export interface DprofileReport {
  id?: number;
  taskId?: number;
  taskName?: string;
  dsId?: number;
  dsName?: string;
  tableName?: string;
  snapshotId?: number;
  rowCount?: number;
  columnCount?: number;
  dataSizeBytes?: number;
  storageComment?: string;
  lastModified?: string;
  profileData?: string; // JSON
  createBy?: string;
  createTime?: string;
}

/** 列级探查报告 */
export interface DprofileColumnReport {
  id?: number;
  reportId?: number;
  dsId?: number;
  tableName?: string;
  columnName?: string;
  dataType?: string;
  columnComment?: string;
  nullable?: string;
  isPrimaryKey?: string;
  totalCount?: number;
  nullCount?: number;
  nullRate?: number;
  uniqueCount?: number;
  uniqueRate?: number;
  sampleValues?: string; // JSON array
  topValues?: string; // JSON object
  createBy?: string;
  createTime?: string;
}

/** 数据探查快照 */
export interface DprofileSnapshot {
  id?: number;
  snapshotCode?: string;
  snapshotName?: string;
  snapshotDesc?: string;
  dsId?: number;
  dsName?: string;
  snapshotData?: string; // JSON
  tableCount?: number;
  createBy?: string;
  createTime?: string;
}
