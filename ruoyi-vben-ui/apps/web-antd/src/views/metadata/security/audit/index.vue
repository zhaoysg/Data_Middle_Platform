<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { Page } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { secAccessLogList } from '#/api/metadata/security/access-log';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'dsName', label: '数据源', component: 'Input' },
    { fieldName: 'userName', label: '用户名', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '用户名', field: 'userName', width: 120 },
    { title: 'IP地址', field: 'ipAddress', width: 150 },
    { title: '耗时(ms)', field: 'elapsedMs', width: 100 },
    { title: '返回行数', field: 'resultRows', width: 100 },
    { title: '脱敏字段', field: 'maskedColumns', minWidth: 200 },
    { title: '执行时间', field: 'createTime', width: 180 },
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
        const data = await secAccessLogList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-access-log-index',
};

const [BasicTable] = useVbenVxeGrid({ formOptions, gridOptions });
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="访问日志" />
  </Page>
</template>
