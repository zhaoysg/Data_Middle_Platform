import type { DqcPlan } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dqc/plan/list',
  info = '/system/metadata/dqc/plan',
  root = '/system/metadata/dqc/plan',
  execute = '/system/metadata/dqc/plan/{id}/execute',
  publish = '/system/metadata/dqc/plan/publish/{id}',
  disable = '/system/metadata/dqc/plan/disable/{id}',
  validateCron = '/system/metadata/dqc/plan/validate-cron',
}

/** 查询DQC质检方案列表 */
export function dqcPlanList(params?: PageQuery & {
  planName?: string;
  layerCode?: string;
  status?: string;
  triggerType?: string;
  sensitivityLevel?: string;
}) {
  return requestClient.get<PageResult<DqcPlan>>(Api.list, { params });
}

/** 获取DQC质检方案详情 */
export function dqcPlanInfo(id: ID) {
  return requestClient.get<DqcPlan>(`${Api.info}/${id}`);
}

/** 新增DQC质检方案 */
export function dqcPlanAdd(data: Record<string, any>) {
  return requestClient.post<number>(Api.root, data);
}

/** 修改DQC质检方案 */
export function dqcPlanUpdate(data: Record<string, any>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除DQC质检方案 */
export function dqcPlanRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

/** 执行DQC质检方案 */
export function dqcPlanExecute(id: ID) {
  return requestClient.postWithMsg<void>(Api.execute.replace('{id}', String(id)));
}

/** 发布质检方案 */
export function dqcPlanPublish(id: ID) {
  return requestClient.postWithMsg<void>(Api.publish.replace('{id}', String(id)));
}

/** 禁用质检方案 */
export function dqcPlanDisable(id: ID) {
  return requestClient.postWithMsg<void>(Api.disable.replace('{id}', String(id)));
}

/** 获取方案已绑定的规则列表 */
export function dqcPlanBoundRules(planId: ID) {
  return requestClient.get<any[]>(`${Api.root}/${planId}/rules`);
}

/** 绑定规则到方案（支持按表-字段细粒度绑定） */
export function dqcPlanBindRules(planId: ID, rules: any[]) {
  return requestClient.postWithMsg<void>(`${Api.root}/${planId}/rules`, rules);
}

/** 解绑方案中的规则 */
export function dqcPlanUnbindRule(planId: ID, ruleId: ID) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${planId}/rules/${ruleId}`);
}

/** 获取方案下次执行时间 */
export function dqcPlanNextExecution(id: ID) {
  return requestClient.get<string>(`${Api.root}/${id}/next-execution`);
}

/** 校验Cron表达式 */
export function dqcPlanValidateCron(cron: string) {
  return requestClient.get<boolean>(Api.validateCron, { params: { cronExpression: cron } });
}
