import type { DqcQualityScore } from '#/api/metadata/model';
import type { ID, PageQuery, PageResult } from '#/api/common';
import { requestClient } from '#/api/request';

enum Api {
  list = '/system/metadata/dqc/score/list',
  trend = '/system/metadata/dqc/score/trend',
  latest = '/system/metadata/dqc/score/latest',
}

/** 查询DQC质量评分列表 */
export function dqcScoreList(params?: PageQuery) {
  return requestClient.get<PageResult<DqcQualityScore>>(Api.list, { params });
}

/** 查询DQC质量评分趋势 */
export function dqcScoreTrend(params?: PageQuery & { days?: number }) {
  return requestClient.get<DqcQualityScore[]>(Api.trend, { params });
}

/** 获取最新DQC质量评分 */
export function dqcScoreLatest(dsId?: ID, tableName?: string) {
  return requestClient.get<DqcQualityScore>(Api.latest, { params: { dsId, tableName } });
}
