<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcExecution } from '#/api/metadata/model';

import { Page } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';
import { useRouter } from 'vue-router';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dqcExecutionList, dqcExecutionRerun, dqcExecutionStop } from '#/api/metadata/dqc/execution';

const router = useRouter();

const statusColorMap: Record<string, string> = {
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  PARTIAL: 'warning',
  STOPPED: 'default',
};

const statusLabelMap: Record<string, string> = {
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  PARTIAL: '部分成功',
  STOPPED: '已停止',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'executionNo', label: '执行编号', component: 'Input' },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '运行中', value: 'RUNNING' },
          { label: '成功', value: 'SUCCESS' },
          { label: '失败', value: 'FAILED' },
          { label: '部分成功', value: 'PARTIAL' },
          { label: '已停止', value: 'STOPPED' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { title: '执行编号', field: 'executionNo', width: 180, fixed: 'left' },
    { title: '方案名称', field: 'planName', width: 180 },
    {
      title: '触发方式',
      field: 'triggerTypeText',
      width: 100,
    },
    { title: '开始时间', field: 'startTime', width: 180 },
    { title: '耗时(ms)', field: 'elapsedMs', width: 100 },
    { title: '总规则数', field: 'totalRules', width: 100 },
    { title: '通过数', field: 'passedCount', width: 80 },
    { title: '失败数', field: 'failedCount', width: 80 },
    {
      title: '总分',
      field: 'overallScore',
      width: 80,
      formatter: ({ cellValue }: { cellValue?: number }) => {
        return cellValue !== undefined ? `${cellValue}分` : '-';
      },
    },
    {
      title: '状态',
      field: 'status',
      width: 100,
      slots: { default: 'status' },
    },
    { title: '操作', field: 'action', width: 200, fixed: 'right', slots: { default: 'action' } },
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
        const data = await dqcExecutionList(params);
        if (!data) return { rows: [], total: 0 };
        if (Array.isArray(data)) return { rows: data, total: data.length };
        return { rows: data.rows ?? [], total: data.total ?? 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dqc-execution-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });

function handleViewDetail(row: DqcExecution) {
  router.push({
    path: '/metadata/dqc/execution/detail',
    query: { id: row.id },
  });
}

async function handleRerun(row: DqcExecution) {
  await dqcExecutionRerun(row.id!);
  await tableApi.query();
}

async function handleStop(row: DqcExecution) {
  await dqcExecutionStop(row.id!);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="执行记录">
      <template #status="{ row }">
        <Tag :color="statusColorMap[row.status || '']">
          {{ statusLabelMap[row.status || ''] || row.status || '-' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleViewDetail(row)">查看详情</a-button>
          <Popconfirm
            v-if="['FAILED', 'SUCCESS', 'PARTIAL', 'STOPPED'].includes(row.status || '')"
            title="确认重新执行？"
            @confirm="handleRerun(row)"
          >
            <a-button type="link" size="small">重新执行</a-button>
          </Popconfirm>
          <Popconfirm
            v-if="row.status === 'RUNNING'"
            title="确认停止执行？"
            @confirm="handleStop(row)"
          >
            <a-button type="link" size="small" danger>停止</a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
  </Page>
</template>
