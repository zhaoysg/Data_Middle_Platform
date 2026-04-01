import type { SecMaskTemplate } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/mask-template/list',
  info = '/system/metadata/security/mask-template',
  root = '/system/metadata/security/mask-template',
}

/** 查询脱敏模板列表 */
export function secMaskTemplateList(params?: PageQuery) {
  return requestClient.get<PageResult<SecMaskTemplate>>(Api.list, { params });
}

/** 获取脱敏模板详情 */
export function secMaskTemplateInfo(id: ID) {
  return requestClient.get<SecMaskTemplate>(`${Api.info}/${id}`);
}

/** 新增脱敏模板 */
export function secMaskTemplateAdd(data: Partial<SecMaskTemplate>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改脱敏模板 */
export function secMaskTemplateUpdate(data: Partial<SecMaskTemplate>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除脱敏模板 */
export function secMaskTemplateRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
