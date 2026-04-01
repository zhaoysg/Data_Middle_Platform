<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecColumnSensitivity } from '#/api/metadata/model';

import { PlayCircleOutlined } from '@ant-design/icons-vue';
import { Page } from '@vben/common-ui';
import { Button, Space, Tag, message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { datasourceEnabled } from '#/api/system/datasource';
import { secColumnSensitivityList, secColumnSensitivityScan } from '#/api/metadata/security/sensitivity';

const identifiedByLabelMap: Record<string, string> = {
  AUTO: '自动识别',
  MANUAL: '手动标记',
};

const confirmedColorMap: Record<string, string> = {
  '0': 'warning',
  '1': 'success',
};

const confirmedLabelMap: Record<string, string> = {
  '0': '待确认',
  '1': '已确认',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'dsName', label: '数据源', component: 'Input' },
    { fieldName: 'tableName', label: '表名', component: 'Input' },
    { fieldName: 'columnName', label: '字段名', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '表名', field: 'tableName', width: 150 },
    { title: '字段名', field: 'columnName', width: 150 },
    { title: '数据类型', field: 'dataType', width: 120 },
    { title: '敏感等级', field: 'levelName', width: 100 },
    { title: '识别方式', field: 'identifiedBy', width: 100 },
    {
      title: '确认状态',
      field: 'confirmed',
      width: 100,
      slots: { default: 'confirmed' },
    },
    { title: '创建时间', field: 'createTime', width: 180 },
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
        const data = await secColumnSensitivityList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-sensitivity-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });

async function handleScan(dsId: number) {
  try {
    await secColumnSensitivityScan(dsId);
    message.success('扫描完成');
    await tableApi.query();
  } catch (error) {
    console.error(error);
  }
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="字段敏感记录">
      <template #toolbar-tools>
        <Space>
          <Button type="primary" :icon="h(PlayCircleOutlined)" @click="handleScan(1)">
            扫描敏感字段
          </Button>
        </Space>
      </template>
      <template #identifiedBy="{ row }">
        {{ identifiedByLabelMap[row.identifiedBy || ''] || row.identifiedBy || '-' }}
      </template>
      <template #confirmed="{ row }">
        <Tag :color="confirmedColorMap[row.confirmed || '0']">
          {{ confirmedLabelMap[row.confirmed || '0'] || row.confirmed || '-' }}
        </Tag>
      </template>
    </BasicTable>
  </Page>
</template>
