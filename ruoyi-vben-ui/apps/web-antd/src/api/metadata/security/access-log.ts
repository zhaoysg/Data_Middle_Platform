import type { SecAccessLog } from '#/api/metadata/model';
import type { PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/access-log/list',
  root = '/system/metadata/security/access-log',
}

/** 查询脱敏访问日志列表 */
export function secAccessLogList(params?: PageQuery) {
  return requestClient.get<PageResult<SecAccessLog>>(Api.list, { params });
}
