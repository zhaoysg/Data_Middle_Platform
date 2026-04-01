<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { Page } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dprofileReportList } from '#/api/metadata/dprofile/report';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'dsName', label: '数据源', component: 'Input' },
    { fieldName: 'tableName', label: '表名', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '表名', field: 'tableName', width: 180 },
    { title: '任务名称', field: 'taskName', width: 180 },
    { title: '行数', field: 'rowCount', width: 100 },
    { title: '列数', field: 'columnCount', width: 80 },
    { title: '存储大小', field: 'dataSizeBytes', width: 120 },
    { title: '最后修改', field: 'lastModified', width: 180 },
    { title: '生成时间', field: 'createTime', width: 180 },
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
        const data = await dprofileReportList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dprofile-report-index',
};

const [BasicTable] = useVbenVxeGrid({ formOptions, gridOptions });
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="探查报告" />
  </Page>
</template>
