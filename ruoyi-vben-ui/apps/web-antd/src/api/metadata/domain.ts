import type { MetadataDomain } from './model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  domainList = '/system/metadata/domain/list',
  domainOptions = '/system/metadata/domain/options',
  domainRoot = '/system/metadata/domain',
}

/** 查询数据域列表（分页） */
export function metadataDomainList(params?: PageQuery & { domainName?: string; domainCode?: string; status?: string }) {
  return requestClient.get<PageResult<MetadataDomain>>(Api.domainList, { params });
}

/** 获取数据域下拉框 */
export function metadataDomainOptions() {
  return requestClient.get<MetadataDomain[]>(Api.domainOptions);
}

/** 获取数据域详情 */
export function metadataDomainInfo(id: ID) {
  return requestClient.get<MetadataDomain>(`${Api.domainRoot}/${id}`);
}

/** 新增数据域 */
export function metadataDomainAdd(data: Partial<MetadataDomain>) {
  return requestClient.post<ID>(Api.domainRoot, data);
}

/** 修改数据域 */
export function metadataDomainUpdate(data: Partial<MetadataDomain>) {
  return requestClient.putWithMsg<void>(Api.domainRoot, data);
}

/** 删除数据域 */
export function metadataDomainRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.domainRoot}/${ids}`);
}

/** 获取全量数据域（供其他模块使用） */
export async function getAllMetadataDomain() {
  return await metadataDomainOptions();
}
