import type { SecSensitivityRule } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/rules/list',
  info = '/system/metadata/security/rules',
  root = '/system/metadata/security/rules',
}

/** 查询敏感识别规则列表 */
export function secSensitivityRuleList(params?: PageQuery) {
  return requestClient.get<PageResult<SecSensitivityRule>>(Api.list, { params });
}

/** 获取敏感识别规则详情 */
export function secSensitivityRuleInfo(id: ID) {
  return requestClient.get<SecSensitivityRule>(`${Api.info}/${id}`);
}

/** 新增敏感识别规则 */
export function secSensitivityRuleAdd(data: Partial<SecSensitivityRule>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改敏感识别规则 */
export function secSensitivityRuleUpdate(data: Partial<SecSensitivityRule>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除敏感识别规则 */
export function secSensitivityRuleRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
