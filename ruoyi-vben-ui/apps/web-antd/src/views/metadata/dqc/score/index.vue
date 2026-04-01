<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcQualityScore } from '#/api/metadata/model';

import { Page } from '@vben/common-ui';
import { Card, Col, Progress, Row, Statistic } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dqcScoreList } from '#/api/metadata/dqc/score';

const dimensionLabelMap: Record<string, string> = {
  COMPLETENESS: '完整性',
  UNIQUENESS: '唯一性',
  ACCURACY: '准确性',
  CONSISTENCY: '一致性',
  TIMELINESS: '时效性',
  VALIDITY: '有效性',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'dsName', label: '数据源名称', component: 'Input' },
    { fieldName: 'targetTable', label: '表名', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '表名', field: 'targetTable', width: 180 },
    { title: '质检方案', field: 'planName', width: 150 },
    { title: '质检日期', field: 'checkDate', width: 120 },
    {
      title: '完整性',
      field: 'completenessScore',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '唯一性',
      field: 'uniquenessScore',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '准确性',
      field: 'accuracyScore',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '一致性',
      field: 'consistencyScore',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '时效性',
      field: 'timelinessScore',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '有效性',
      field: 'validityScore',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '总分',
      field: 'overallScore',
      width: 100,
      slots: { default: 'overallScore' },
    },
    {
      title: '通过率',
      field: 'rulePassRate',
      width: 120,
      slots: { default: 'passRate' },
    },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page, form }: any) => {
        const params = {
          ...form,
          pageNum: page?.currentPage,
          pageSize: page?.pageSize,
        };
        const data = await dqcScoreList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dqc-score-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });

function getScoreColor(score?: number) {
  if (score === undefined) return 'default';
  if (score >= 90) return '#52c41a';
  if (score >= 70) return '#faad14';
  return '#ff4d4f';
}
</script>

<template>
  <Page :auto-content-height="true">
    <Card class="mb-4">
      <Row :gutter="24">
        <Col :span="4">
          <Statistic
            title="平均总分"
            :value="95.5"
            :value-style="{ color: getScoreColor(95.5) }"
          />
        </Col>
        <Col :span="4">
          <Statistic title="平均完整性" suffix="分" :value="92.3" />
        </Col>
        <Col :span="4">
          <Statistic title="平均唯一性" suffix="分" :value="88.7" />
        </Col>
        <Col :span="4">
          <Statistic title="平均准确性" suffix="分" :value="94.1" />
        </Col>
        <Col :span="4">
          <Statistic title="平均一致性" suffix="分" :value="90.5" />
        </Col>
        <Col :span="4">
          <Statistic title="平均时效性" suffix="分" :value="96.2" />
        </Col>
      </Row>
    </Card>
    <BasicTable table-title="质量评分">
      <template #overallScore="{ row }">
        <span :style="{ color: getScoreColor(row.overallScore) }">
          {{ row.overallScore !== undefined ? `${row.overallScore}分` : '-' }}
        </span>
      </template>
      <template #passRate="{ row }">
        <Progress
          :percent="row.rulePassRate || 0"
          :stroke-color="getScoreColor((row.rulePassRate || 0) * 100 / 100)"
          size="small"
          style="width: 100px"
        />
      </template>
    </BasicTable>
  </Page>
</template>
