import type { MetadataScan, MetadataScanResult, MetadataScanLog } from './model';
import { requestClient } from '#/api/request';

enum Api {
  scanExec = '/system/metadata/scan/exec',
  scanList = '/system/metadata/scan/list',
  scanLogRoot = '/system/metadata/scan',
}

/** 执行扫描（POST /system/metadata/scan/exec/{dsId}） */
export function metadataScanExec(dsId: number, data?: Partial<MetadataScan>) {
  return requestClient.post<MetadataScanResult>(`${Api.scanExec}/${dsId}`, data);
}

/** 查询扫描记录列表（分页/全量） */
export function metadataScanLogs(params?: { dsId?: number }) {
  return requestClient.get<MetadataScanLog[]>(Api.scanList, { params });
}

/** 查询扫描记录详情 */
export function metadataScanLogInfo(id: number) {
  return requestClient.get<MetadataScanLog>(`${Api.scanLogRoot}/detail/${id}`);
}
