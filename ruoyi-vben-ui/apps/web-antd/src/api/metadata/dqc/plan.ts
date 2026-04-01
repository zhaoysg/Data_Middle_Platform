import type { DqcPlan } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dqc/plan/list',
  info = '/system/metadata/dqc/plan',
  root = '/system/metadata/dqc/plan',
  execute = '/system/metadata/dqc/plan/{id}/execute',
}

/** 查询DQC质检方案列表 */
export function dqcPlanList(params?: PageQuery) {
  return requestClient.get<PageResult<DqcPlan>>(Api.list, { params });
}

/** 获取DQC质检方案详情 */
export function dqcPlanInfo(id: ID) {
  return requestClient.get<DqcPlan>(`${Api.info}/${id}`);
}

/** 新增DQC质检方案 */
export function dqcPlanAdd(data: Partial<DqcPlan>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改DQC质检方案 */
export function dqcPlanUpdate(data: Partial<DqcPlan>) {
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
