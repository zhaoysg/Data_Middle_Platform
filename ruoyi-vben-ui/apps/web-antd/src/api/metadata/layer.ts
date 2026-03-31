import type { MetadataLayer } from './model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  layerList = '/system/metadata/layer/list',
  layerOptions = '/system/metadata/layer/options',
  layerRoot = '/system/metadata/layer',
}

/** 查询数仓分层列表（分页） */
export function metadataLayerList(params?: PageQuery & { layerName?: string; layerCode?: string; status?: string }) {
  return requestClient.get<PageResult<MetadataLayer>>(Api.layerList, { params });
}

/** 获取全部分层（下拉框） */
export function metadataLayerOptions() {
  return requestClient.get<MetadataLayer[]>(Api.layerOptions);
}

/** 获取数仓分层详情 */
export function metadataLayerInfo(id: ID) {
  return requestClient.get<MetadataLayer>(`${Api.layerRoot}/${id}`);
}

/** 新增数仓分层 */
export function metadataLayerAdd(data: Partial<MetadataLayer>) {
  return requestClient.post<ID>(Api.layerRoot, data);
}

/** 修改数仓分层 */
export function metadataLayerUpdate(data: Partial<MetadataLayer>) {
  return requestClient.putWithMsg<void>(Api.layerRoot, data);
}

/** 删除数仓分层 */
export function metadataLayerRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.layerRoot}/${ids}`);
}

/** 获取全部分层（供其他模块使用） */
export async function getAllMetadataLayer() {
  return await metadataLayerOptions();
}
