import type { DqcRuleDef } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dqc/rule/list',
  info = '/system/metadata/dqc/rule',
  root = '/system/metadata/dqc/rule',
  bindPlan = '/system/metadata/dqc/rule/bind-plan',
}

/** 查询DQC规则定义列表 */
export function dqcRuleList(params?: PageQuery) {
  return requestClient.get<PageResult<DqcRuleDef>>(Api.list, { params });
}

/** 获取DQC规则定义详情 */
export function dqcRuleInfo(id: ID) {
  return requestClient.get<DqcRuleDef>(`${Api.info}/${id}`);
}

/** 新增DQC规则定义 */
export function dqcRuleAdd(data: Partial<DqcRuleDef>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改DQC规则定义 */
export function dqcRuleUpdate(data: Partial<DqcRuleDef>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除DQC规则定义 */
export function dqcRuleRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

/** 绑定规则到质检方案 */
export function dqcRuleBindPlan(planId: ID, ruleIds: ID[]) {
  return requestClient.postWithMsg<void>(Api.bindPlan, { planId, ruleIds });
}
