import type { MetadataTable, MetadataColumn } from './model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { commonExport } from '#/api/helper';
import { requestClient } from '#/api/request';

enum Api {
  tableList = '/system/metadata/table/list',
  tableExport = '/system/metadata/table/export',
  tableRoot = '/system/metadata/table',
  tableTagOptions = '/system/metadata/table/tag-options',
  tableColumnAlias = '/system/metadata/table/column/alias',
  tableColumnExport = '/system/metadata/table/column/export',
}

/** 查询元数据表列表 */
export function metadataTableList(params?: PageQuery & { dsId?: number; dataLayer?: string; dataDomain?: string; status?: string; catalogId?: number; keyword?: string }) {
  return requestClient.get<PageResult<MetadataTable>>(Api.tableList, { params });
}

/** 导出元数据表列表 */
export function metadataTableExport(data: Partial<MetadataTable>) {
  return commonExport(Api.tableExport, data);
}

/** 获取元数据表详情 */
export function metadataTableInfo(id: ID) {
  return requestClient.get<MetadataTable>(`${Api.tableRoot}/${id}`);
}

/** 获取元数据标签选项 */
export function metadataTableTagOptions() {
  return requestClient.get<string[]>(Api.tableTagOptions);
}

/** 修改元数据表 */
export function metadataTableUpdate(data: Partial<MetadataTable>) {
  return requestClient.putWithMsg<void>(Api.tableRoot, data);
}

/** 删除元数据表 */
export function metadataTableRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.tableRoot}/${ids}`);
}

/** 批量更新元数据表（分层/域/敏感等级/标签） */
export function metadataTableBatchUpdate(data: { ids: number[]; dataLayer?: string; dataDomain?: string; sensitivityLevel?: string; tags?: string }) {
  return requestClient.putWithMsg<number>(`${Api.tableRoot}/batch-update`, data);
}

/** 查询治理统计（未配置分层/域/敏感等级的表数量） */
export function metadataTableGovernanceStat() {
  return requestClient.get<number[]>('/system/metadata/table/governance-stat');
}

/** 获取表字段列表 */
export function metadataTableColumns(id: ID) {
  return requestClient.get<MetadataColumn[]>(`${Api.tableRoot}/${id}/columns`);
}

/** 更新字段别名 */
export function metadataColumnAlias(id: ID, alias: string) {
  return requestClient.putWithMsg<void>(Api.tableColumnAlias, null, { params: { id, alias } });
}

/** 更新字段（别名/注释/敏感等级） */
export function metadataColumnUpdate(id: ID, data: { columnAlias?: string; columnComment?: string; sensitivityLevel?: string }) {
  return requestClient.putWithMsg<void>(`/system/metadata/column/${id}/info`, data);
}

/** 导出字段列表 */
export function metadataColumnExport(tableId: ID) {
  return requestClient.post(Api.tableColumnExport, null, { params: { tableId } });
}
