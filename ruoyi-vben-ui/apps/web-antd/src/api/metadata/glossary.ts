import type { ID, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum TermApi {
  list = '/system/metadata/glossary/term/list',
  keyword = '/system/metadata/glossary/term/keyword',
}

enum CategoryApi {
  list = '/system/metadata/glossary/category/list',
  tree = '/system/metadata/glossary/category/tree',
  options = '/system/metadata/glossary/category/options',
}

enum MappingApi {
  list = '/system/metadata/glossary/mapping/list',
  detail = '/system/metadata/glossary/mapping',
  term = '/system/metadata/glossary/mapping/term',
  table = '/system/metadata/glossary/mapping/table',
}

// ============== 术语 API ==============

/** 查询术语列表 */
export function glossaryTermList(params?: PageQuery) {
  return requestClient.get<PageResult<any>>(TermApi.list, { params });
}

/** 根据关键词搜索术语 */
export function glossaryTermSearch(keyword: string) {
  return requestClient.get<any[]>(`${TermApi.keyword}/${encodeURIComponent(keyword)}`);
}

/** 新增术语 */
export function glossaryTermAdd(data: any) {
  return requestClient.post(TermApi.list, data);
}

/** 修改术语 */
export function glossaryTermUpdate(data: any) {
  return requestClient.put(TermApi.list, data);
}

/** 删除术语 */
export function glossaryTermRemove(ids: ID[]) {
  return requestClient.delete(`${TermApi.list}/${ids.join(',')}`);
}

/** 发布术语 */
export function glossaryTermPublish(id: ID) {
  return requestClient.post(`${TermApi.list}/publish/${id}`);
}

// ============== 分类 API ==============

/** 查询分类列表 */
export function glossaryCategoryList(params?: PageQuery) {
  return requestClient.get<PageResult<any>>(CategoryApi.list, { params });
}

/** 查询分类树 */
export function glossaryCategoryTree() {
  return requestClient.get<any[]>(CategoryApi.tree);
}

/** 查询分类下拉选项 */
export function glossaryCategoryOptions() {
  return requestClient.get<any[]>(CategoryApi.options);
}

/** 新增分类 */
export function glossaryCategoryAdd(data: any) {
  return requestClient.post(CategoryApi.list, data);
}

/** 修改分类 */
export function glossaryCategoryUpdate(data: any) {
  return requestClient.put(CategoryApi.list, data);
}

/** 删除分类 */
export function glossaryCategoryRemove(ids: ID[]) {
  return requestClient.delete(`${CategoryApi.list}/${ids.join(',')}`);
}

// ============== 术语映射 API ==============

/** 查询映射列表 */
export function glossaryMappingList(params?: PageQuery) {
  return requestClient.get<PageResult<any>>(MappingApi.list, { params });
}

/** 查询映射详情 */
export function glossaryMappingDetail(id: ID) {
  return requestClient.get<any>(`${MappingApi.detail}/${id}`);
}

/** 新增映射 */
export function glossaryMappingAdd(data: any) {
  return requestClient.post(MappingApi.detail, data);
}

/** 修改映射 */
export function glossaryMappingUpdate(data: any) {
  return requestClient.put(MappingApi.detail, data);
}

/** 删除映射 */
export function glossaryMappingRemove(ids: ID[]) {
  return requestClient.delete(`${MappingApi.detail}/${ids.join(',')}`);
}

/** 根据术语ID查询映射列表 */
export function glossaryMappingByTerm(termId: ID) {
  return requestClient.get<any[]>(`${MappingApi.term}/${termId}`);
}

/** 根据表名查询映射列表 */
export function glossaryMappingByTable(dsId: ID, tableName: string) {
  return requestClient.get<any[]>(`${MappingApi.table}`, { params: { dsId, tableName } });
}
