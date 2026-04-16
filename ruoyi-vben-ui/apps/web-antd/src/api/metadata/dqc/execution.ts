import type { DqcExecution, DqcExecutionDetail } from '#/api/metadata/model';
import type { DqcQualityScore } from '#/api/metadata/model';
import type { ID, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dqc/execution/list',
  info = '/system/metadata/dqc/execution',
  root = '/system/metadata/dqc/execution',
  stop = '/system/metadata/dqc/execution/{id}/stop',
  rerun = '/system/metadata/dqc/execution/{id}/rerun',
  detail = '/system/metadata/dqc/execution/detail/{executionId}',
  scoreTrend = '/system/metadata/dqc/score/trend',
}

/** 查询DQC执行记录列表 */
export function dqcExecutionList(params?: PageQuery) {
  return requestClient.get<PageResult<DqcExecution>>(Api.list, { params });
}

/** 获取DQC执行记录详情 */
export function dqcExecutionInfo(id: ID) {
  return requestClient.get<DqcExecution>(`${Api.info}/${id}`);
}

/** 停止DQC执行 */
export function dqcExecutionStop(id: ID) {
  return requestClient.putWithMsg<void>(Api.stop.replace('{id}', String(id)));
}

/** 重新执行DQC */
export function dqcExecutionRerun(id: ID) {
  return requestClient.postWithMsg<void>(Api.rerun.replace('{id}', String(id)));
}

/** 获取DQC执行明细列表 */
export function dqcExecutionDetails(executionId: ID) {
  return requestClient.get<DqcExecutionDetail[]>(
    Api.detail.replace('{executionId}', String(executionId))
  );
}

/** 查询评分趋势（驾驶舱用） */
export function dqcScoreTrend(params?: { days?: number }) {
  return requestClient.get<DqcQualityScore[]>(Api.scoreTrend, { params });
}
