import type { ID, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

/** 血缘关系视图对象（与后端 GovLineageVo 对应） */
export interface GovLineageVo {
  id?: number;
  /** 源数据源ID */
  srcDsId?: number;
  /** 源数据源名称 */
  srcDsName?: string;
  /** 源表名 */
  srcTableName?: string;
  /** 源列名 */
  srcColumnName?: string;
  /** 目标数据源ID */
  tgtDsId?: number;
  /** 目标数据源名称 */
  tgtDsName?: string;
  /** 目标表名 */
  tgtTableName?: string;
  /** 目标列名 */
  tgtColumnName?: string;
  /** 血缘类型：DIRECT/DERIVED */
  lineageType?: string;
  /** 转换类型：ETL/SQL/STREAMING/COPY */
  transformType?: string;
  /** 转换SQL */
  transformSql?: string;
  /** 业务描述 */
  bizDescription?: string;
  /** 负责人ID */
  ownerId?: number;
  /** 负责人名称 */
  ownerName?: string;
  /** 核验状态：UNVERIFIED/VERIFIED/INVALID */
  verifyStatus?: string;
  /** 血缘深度 */
  level?: number;
  createTime?: string;
  updateTime?: string;
}

/** 血缘图谱节点 */
export interface LineageNode {
  id: string;
  dsId: number;
  dsName: string;
  tableName: string;
  columnName?: string;
  layerCode?: string;
  layerName?: string;
  rowCount?: number;
  lastScanTime?: string;
  /** 敏感字段数量 */
  sensitiveCount?: number;
  /** 敏感等级 */
  sensitivityLevel?: string;
}

/** 血缘图谱边 */
export interface LineageEdge {
  id: string;
  source: string;
  target: string;
  lineageType: string;
  columnName?: string;
  verifyStatus?: string;
}

/** 数据源选项 */
export interface DatasourceOption {
  dsId: number;
  dsName: string;
  dsType?: string;
}

enum Api {
  upstream = '/system/metadata/lineage/upstream',
  downstream = '/system/metadata/lineage/downstream',
  list = '/system/metadata/lineage/list',
  datasourceList = '/system/metadata/datasource/enabled',
}

/** 查询上游血缘 */
export function lineageUpstream(dsId: ID, tableName: string, depth = 5) {
  return requestClient.get<GovLineageVo[]>(
    `${Api.upstream}/${dsId}/${encodeURIComponent(tableName)}`,
    { params: { depth } },
  );
}

/** 查询下游血缘 */
export function lineageDownstream(dsId: ID, tableName: string, depth = 5) {
  return requestClient.get<GovLineageVo[]>(
    `${Api.downstream}/${dsId}/${encodeURIComponent(tableName)}`,
    { params: { depth } },
  );
}

/** 查询血缘列表 */
export function lineageList(params?: PageQuery) {
  return requestClient.get<PageResult<GovLineageVo>>(Api.list, { params });
}

/** 获取可用数据源列表 */
export function datasourceList() {
  return requestClient.get<DatasourceOption[]>(Api.datasourceList);
}
