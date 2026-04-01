import type { SecMaskStrategy, SecMaskStrategyDetail } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/strategy/list',
  info = '/system/metadata/security/strategy',
  root = '/system/metadata/security/strategy',
  details = '/system/metadata/security/strategy/{id}/details',
}

/** 查询脱敏策略列表 */
export function secMaskStrategyList(params?: PageQuery) {
  return requestClient.get<PageResult<SecMaskStrategy>>(Api.list, { params });
}

/** 获取脱敏策略详情 */
export function secMaskStrategyInfo(id: ID) {
  return requestClient.get<SecMaskStrategy>(`${Api.info}/${id}`);
}

/** 新增脱敏策略 */
export function secMaskStrategyAdd(data: Partial<SecMaskStrategy>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改脱敏策略 */
export function secMaskStrategyUpdate(data: Partial<SecMaskStrategy>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除脱敏策略 */
export function secMaskStrategyRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

/** 获取脱敏策略明细 */
export function secMaskStrategyDetails(id: ID) {
  return requestClient.get<SecMaskStrategyDetail[]>(
    Api.details.replace('{id}', String(id)),
  );
}
