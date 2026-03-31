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
