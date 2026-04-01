import type { MaskQueryResult } from '#/api/metadata/model';
import { requestClient } from '#/api/request';

/** 执行脱敏查询 */
export function maskQuery(data: { dsId: number; sql: string }) {
  return requestClient.post<MaskQueryResult>(
    '/system/metadata/security/mask-query/query',
    data,
  );
}

/** 验证SQL语句 */
export function maskQueryValidate(sql: string) {
  return requestClient.post<{ valid: boolean; message: string }>(
    '/system/metadata/security/mask-query/validate',
    { sql },
  );
}
