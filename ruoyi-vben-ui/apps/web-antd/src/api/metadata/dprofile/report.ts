import type { DprofileReport, DprofileColumnReport } from '#/api/metadata/model';
import type { ID, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dprofile/report/list',
  info = '/system/metadata/dprofile/report',
  root = '/system/metadata/dprofile/report',
  columns = '/system/metadata/dprofile/report/{id}/columns',
}

/** 查询数据探查报告列表 */
export function dprofileReportList(params?: PageQuery) {
  return requestClient.get<PageResult<DprofileReport>>(Api.list, { params });
}

/** 获取数据探查报告详情 */
export function dprofileReportInfo(id: ID) {
  return requestClient.get<DprofileReport>(`${Api.info}/${id}`);
}

/** 获取列级探查报告 */
export function dprofileReportColumns(id: ID) {
  return requestClient.get<DprofileColumnReport[]>(
    Api.columns.replace('{id}', String(id)),
  );
}
