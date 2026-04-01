import type { SecClassification } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/classification/list',
  info = '/system/metadata/security/classification',
  root = '/system/metadata/security/classification',
}

/** 查询数据分类列表 */
export function secClassificationList(params?: PageQuery) {
  return requestClient.get<PageResult<SecClassification>>(Api.list, { params });
}

/** 获取数据分类详情 */
export function secClassificationInfo(id: ID) {
  return requestClient.get<SecClassification>(`${Api.info}/${id}`);
}

/** 新增数据分类 */
export function secClassificationAdd(data: Partial<SecClassification>) {
  return requestClient.post<ID>(Api.root, data);
}

/** 修改数据分类 */
export function secClassificationUpdate(data: Partial<SecClassification>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除数据分类 */
export function secClassificationRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
