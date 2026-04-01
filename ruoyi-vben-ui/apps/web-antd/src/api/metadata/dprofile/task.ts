import type { DprofileTask } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dprofile/task/list',
  info = '/system/metadata/dprofile/task',
  root = '/system/metadata/dprofile/task',
}

/** 查询数据探查任务列表 */
export function dprofileTaskList(params?: PageQuery) {
  return requestClient.get<PageResult<DprofileTask>>(Api.list, { params });
}

/** 获取数据探查任务详情 */
export function dprofileTaskInfo(id: ID) {
  return requestClient.get<DprofileTask>(`${Api.info}/${id}`);
}

/** 新增数据探查任务 */
export function dprofileTaskAdd(data: Partial<DprofileTask>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改数据探查任务 */
export function dprofileTaskUpdate(data: Partial<DprofileTask>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除数据探查任务 */
export function dprofileTaskRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

/** 启动数据探查任务 */
export function dprofileTaskStart(id: ID) {
  return requestClient.postWithMsg<void>(`${Api.root}/${id}/start`);
}

/** 停止数据探查任务 */
export function dprofileTaskStop(id: ID) {
  return requestClient.putWithMsg<void>(`${Api.root}/${id}/stop`);
}
