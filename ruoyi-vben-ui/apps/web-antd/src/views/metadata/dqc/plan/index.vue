<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcPlan } from '#/api/metadata/model';

import { PlusOutlined, PlayCircleOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { h, Popconfirm, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  dqcPlanAdd,
  dqcPlanExecute,
  dqcPlanList,
  dqcPlanRemove,
  dqcPlanUpdate,
} from '#/api/metadata/dqc/plan';

import planDrawer from './plan-drawer.vue';

const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  STOPPED: 'warning',
};

const statusLabelMap: Record<string, string> = {
  DRAFT: '草稿',
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  STOPPED: '已停止',
};

const triggerTypeLabelMap: Record<string, string> = {
  MANUAL: '手动',
  SCHEDULE: '定时',
  API: 'API调用',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'planName', label: '方案名称', component: 'Input' },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '草稿', value: 'DRAFT' },
          { label: '运行中', value: 'RUNNING' },
          { label: '成功', value: 'SUCCESS' },
          { label: '失败', value: 'FAILED' },
          { label: '已停止', value: 'STOPPED' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '方案名称', field: 'planName', width: 200, fixed: 'left' },
    { title: '方案编码', field: 'planCode', width: 150 },
    { title: '数据源', field: 'dsName', width: 150 },
    {
      title: '触发方式',
      field: 'triggerType',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? triggerTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
    { title: '规则数', field: 'totalRules', width: 80 },
    {
      title: '状态',
      field: 'status',
      width: 100,
      slots: { default: 'status' },
    },
    { title: '创建时间', field: 'createTime', width: 180 },
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
        const data = await dqcPlanList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dqc-plan-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [PlanDrawer, drawerApi] = useVbenDrawer({ connectedComponent: planDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: DqcPlan) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleExecute(row: DqcPlan) {
  await dqcPlanExecute(row.id!);
  await tableApi.query();
}

async function handleDelete(row: DqcPlan) {
  await dqcPlanRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: DqcPlan) => row.id)
    .filter((id): id is number => id !== undefined);
  await dqcPlanRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="质检方案">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" @click="handleAdd">
            <template #icon>
              <PlusOutlined />
            </template>
            新增
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            @click="handleMultiDelete"
          >
            批量删除
          </a-button>
        </Space>
      </template>
      <template #status="{ row }">
        <Tag :color="statusColorMap[row.status || '']">
          {{ statusLabelMap[row.status || ''] || row.status || '-' }}
        </Tag>
      </template>
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleEdit(row)">编辑</a-button>
          <a-button
            type="link"
            size="small"
            @click="handleExecute(row)"
          >
            <template #icon>
              <PlayCircleOutlined />
            </template>
            执行
          </a-button>
          <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger>删除</a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <PlanDrawer @reload="tableApi.query()" />
  </Page>
</template>
