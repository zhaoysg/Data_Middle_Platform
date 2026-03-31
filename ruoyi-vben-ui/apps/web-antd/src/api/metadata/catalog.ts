import type { MetadataCatalog } from './model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  catalogList = '/system/metadata/catalog/list',
  catalogTree = '/system/metadata/catalog/tree',
  catalogOptions = '/system/metadata/catalog/options',
  catalogExport = '/system/metadata/catalog/export',
  catalogRoot = '/system/metadata/catalog',
}

/** 查询资产目录列表（分页） */
export function metadataCatalogList(params?: PageQuery & { catalogName?: string; catalogCode?: string; catalogType?: string; status?: string }) {
  return requestClient.get<PageResult<MetadataCatalog>>(Api.catalogList, { params });
}

/** 查询资产目录树形 */
export function metadataCatalogTree() {
  return requestClient.get<MetadataCatalog[]>(Api.catalogTree);
}

/** 获取资产目录下拉框 */
export function metadataCatalogOptions() {
  return requestClient.get<MetadataCatalog[]>(Api.catalogOptions);
}

/** 导出资产目录列表 */
export function metadataCatalogExport(data: Partial<MetadataCatalog>) {
  return requestClient.post(Api.catalogExport, data);
}

/** 获取资产目录详情 */
export function metadataCatalogInfo(id: ID) {
  return requestClient.get<MetadataCatalog>(`${Api.catalogRoot}/${id}`);
}

/** 新增资产目录 */
export function metadataCatalogAdd(data: Partial<MetadataCatalog>) {
  return requestClient.post<ID>(Api.catalogRoot, data);
}

/** 修改资产目录 */
export function metadataCatalogUpdate(data: Partial<MetadataCatalog>) {
  return requestClient.putWithMsg<void>(Api.catalogRoot, data);
}

/** 删除资产目录 */
export function metadataCatalogRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.catalogRoot}/${ids}`);
}

/** 获取全量目录树（供其他模块使用） */
export async function getAllMetadataCatalog() {
  return await metadataCatalogTree();
}
