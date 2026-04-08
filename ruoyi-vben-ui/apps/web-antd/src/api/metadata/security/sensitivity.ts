import type { SecColumnSensitivity, SecSensitivityScanDTO, SecScanResultVO } from '#/api/metadata/model';
import type { ID, IDS, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/security/sensitivity/list',
  info = '/system/metadata/security/sensitivity',
  root = '/system/metadata/security/sensitivity',
  scan = '/system/metadata/security/sensitivity/scan',
}

/** 查询字段敏感记录列表 */
export function secColumnSensitivityList(params?: PageQuery) {
  return requestClient.get<PageResult<SecColumnSensitivity>>(Api.list, { params });
}

/** 获取字段敏感记录详情 */
export function secColumnSensitivityInfo(id: ID) {
  return requestClient.get<SecColumnSensitivity>(`${Api.info}/${id}`);
}

/** 更新字段敏感记录 */
export function secColumnSensitivityUpdate(data: Partial<SecColumnSensitivity>) {
  return requestClient.putWithMsg<void>(Api.root, data);
}

/** 删除字段敏感记录 */
export function secColumnSensitivityRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}

/** 执行敏感字段扫描（完整参数版） */
export function secColumnSensitivityScan(data: SecSensitivityScanDTO) {
  return requestClient.postWithMsg<SecScanResultVO>(Api.scan, data);
}

/** 确认敏感字段 */
export function secColumnSensitivityConfirm(id: ID, confirmed: boolean) {
  return requestClient.putWithMsg<void>(`${Api.root}/confirm`, { id, confirmed });
}
