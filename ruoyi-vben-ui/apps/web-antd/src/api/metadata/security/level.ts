import type { SecLevel } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/level/list',
  info = '/system/metadata/security/level',
  root = '/system/metadata/security/level',
}

/** 查询敏感等级列表 */
export function secLevelList(params?: PageQuery) {
  return requestClient.get<PageResult<SecLevel>>(Api.list, { params });
}

/** 获取敏感等级详情 */
export function secLevelInfo(id: ID) {
  return requestClient.get<SecLevel>(`${Api.info}/${id}`);
}

/** 新增敏感等级 */
export function secLevelAdd(data: Partial<SecLevel>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改敏感等级 */
export function secLevelUpdate(data: Partial<SecLevel>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除敏感等级 */
export function secLevelRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
