import type { Datasource, ConnectionTestResult } from './model';

import type { ID, IDS, PageQuery, PageResult } from '#/api/common';

import { commonExport } from '#/api/helper';
import { requestClient } from '#/api/request';

enum Api {
  datasourceChangeStatus = '/system/datasource/changeStatus',
  datasourceColumns = '/system/datasource/{dsId}/columns',
  datasourceEnabled = '/system/datasource/enabled',
  datasourceExport = '/system/datasource/export',
  datasourceList = '/system/datasource/list',
  datasourceTables = '/system/datasource/{dsId}/tables',
  datasourceTest = '/system/datasource/test',
  root = '/system/datasource',
}

/**
 * 查询数据源分页列表
 * @param params 搜索条件
 * @returns 分页列表
 */
export function datasourceList(params?: PageQuery) {
  return requestClient.get<PageResult<Datasource>>(Api.datasourceList, { params });
}

/**
 * 导出数据源信息
 * @param data 查询参数
 * @returns blob
 */
export function datasourceExport(data: Partial<Datasource>) {
  return commonExport(Api.datasourceExport, data);
}

/**
 * 查询数据源信息
 * @param dsId 数据源id
 * @returns 数据源信息
 */
export function datasourceInfo(dsId: ID) {
  return requestClient.get<Datasource>(`${Api.root}/${dsId}`);
}

/**
 * 数据源新增
 * @param data 参数
 * @returns 新增数据源ID
 */
export function datasourceAdd(data: Partial<Datasource>) {
  return requestClient.post<Datasource>(Api.root, data);
}

/**
 * 数据源更新
 * @param data 参数
 * @returns void
 */
export function datasourceUpdate(data: Partial<Datasource>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/**
 * 修改数据源状态
 * @param data 参数
 * @returns void
 */
export function datasourceChangeStatus(data: Partial<Datasource>) {
  return requestClient.putWithMsg<void>(Api.datasourceChangeStatus, data);
}

/**
 * 数据源删除
 * @param dsIds ids
 * @returns void
 */
export function datasourceRemove(dsIds: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${dsIds}`);
}

/**
 * 测试数据源连接
 * @param data 数据源配置
 * @returns 连接测试结果
 */
export function datasourceTest(data: Partial<Datasource>) {
  return requestClient.post<ConnectionTestResult>(Api.datasourceTest, data);
}

/**
 * 获取已启用数据源列表（下拉框用）
 * @returns 数据源列表
 */
export function datasourceEnabled() {
  return requestClient.get<Datasource[]>(Api.datasourceEnabled);
}

/**
 * 获取数据源下的表列表
 * @param dsId 数据源ID
 * @param schema Schema名称
 * @returns 表名列表
 */
export function datasourceTables(dsId: ID, schema?: string) {
  return requestClient.get<string[]>(
    Api.datasourceTables.replace('{dsId}', String(dsId)),
    { params: { schema } },
  );
}

/**
 * 获取数据源指定表的字段列表
 * @param dsId 数据源ID
 * @param tableName 表名
 * @param schema Schema名称
 * @returns 字段信息列表
 */
export function datasourceColumns(dsId: ID, tableName: string, schema?: string) {
  return requestClient.get<any[]>(
    Api.datasourceColumns.replace('{dsId}', String(dsId)),
    { params: { tableName, schema } },
  );
}

/**
 * 获取数据源的 Schema 列表（PostgreSQL 专用）
 * @param dsId 数据源ID
 * @returns Schema 名称列表
 */
export function datasourceSchemas(dsId: ID) {
  return requestClient.get<string[]>(
    `/system/datasource/${dsId}/schemas`,
  );
}
