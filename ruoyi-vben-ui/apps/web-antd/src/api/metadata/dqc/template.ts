import type { DqcRuleTemplate } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dqc/template/list',
  info = '/system/metadata/dqc/template',
  root = '/system/metadata/dqc/template',
}

/** 查询DQC规则模板列表 */
export function dqcTemplateList(params?: PageQuery) {
  return requestClient.get<PageResult<DqcRuleTemplate>>(Api.list, { params });
}

/** 获取DQC规则模板详情 */
export function dqcTemplateInfo(id: ID) {
  return requestClient.get<DqcRuleTemplate>(`${Api.info}/${id}`);
}

/** 新增DQC规则模板 */
export function dqcTemplateAdd(data: Partial<DqcRuleTemplate>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改DQC规则模板 */
export function dqcTemplateUpdate(data: Partial<DqcRuleTemplate>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除DQC规则模板 */
export function dqcTemplateRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
