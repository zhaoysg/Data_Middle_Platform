<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcPlan } from '#/api/metadata/model';

import { PlusOutlined, PlayCircleOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag, message } from 'ant-design-vue';
import { ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  dqcPlanDisable,
  dqcPlanExecute,
  dqcPlanList,
  dqcPlanPublish,
  dqcPlanRemove,
} from '#/api/metadata/dqc/plan';

import planDrawer from './plan-drawer.vue';

const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  PUBLISHED: 'success',
  DISABLED: 'warning',
};

const statusLabelMap: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  DISABLED: '已禁用',
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
          { label: '已发布', value: 'PUBLISHED' },
          { label: '已禁用', value: 'DISABLED' },
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
    { title: '方案名称', field: 'planName', minWidth: 200, fixed: 'left' },
    { title: '方案编码', field: 'planCode', minWidth: 160 },
    { title: '数据层', field: 'layerCode', width: 70 },
    { title: '触发方式', field: 'triggerType', width: 90, slots: { default: 'triggerType' } },
    { title: '涉及表', field: 'tableCount', width: 70 },
    { title: '规则数', field: 'ruleCount', width: 70 },
    { title: '最近得分', field: 'lastScore', width: 90 },
    { title: '状态', field: 'status', width: 90, slots: { default: 'status' } },
    { title: '创建时间', field: 'createTime', minWidth: 170 },
    { title: '操作', field: 'action', width: 220, fixed: 'right', slots: { default: 'action' } },
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

const executingPlanId = ref<number | null>(null);

function isPlanExecuting(planId: number) {
  return executingPlanId.value === planId;
}

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: DqcPlan) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleExecute(row: DqcPlan) {
  executingPlanId.value = row.id!;
  try {
    await dqcPlanExecute(row.id!);
    message.success('执行已启动，请前往「执行记录」查看进度');
    await tableApi.query();
  } catch {
    // error already shown by interceptor
  } finally {
    executingPlanId.value = null;
  }
}

async function handleDelete(row: DqcPlan) {
  await dqcPlanRemove([row.id!]);
  await tableApi.query();
}

async function handlePublish(row: DqcPlan) {
  await dqcPlanPublish(row.id!);
  await tableApi.query();
}

async function handleDisable(row: DqcPlan) {
  await dqcPlanDisable(row.id!);
  await tableApi.query();
}

async function handleEnable(row: DqcPlan) {
  await dqcPlanPublish(row.id!);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: DqcPlan) => row.id)
    .filter((id): id is string | number => id !== undefined);
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
      <template #triggerType="{ row }">
        <Tag>{{ triggerTypeLabelMap[row.triggerType || ''] || row.triggerType || '-' }}</Tag>
      </template>
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleEdit(row)">编辑</a-button>
          <a-button
            v-if="row.status === 'DRAFT'"
            type="link"
            size="small"
            @click="handlePublish(row)"
          >
            发布
          </a-button>
          <a-button
            v-if="row.status === 'DISABLED'"
            type="link"
            size="small"
            style="color: #1677ff; font-weight: 500"
            @click="handleEnable(row)"
          >
            启用
          </a-button>
          <a-button
            v-if="row.status === 'PUBLISHED'"
            type="link"
            size="small"
            @click="handleDisable(row)"
          >
            停用
          </a-button>
          <a-button
            type="link"
            size="small"
            :loading="isPlanExecuting(row.id!)"
            :disabled="row.status !== 'PUBLISHED' && !isPlanExecuting(row.id!)"
            @click="handleExecute(row)"
          >
            <template #icon>
              <PlayCircleOutlined />
            </template>
            {{ isPlanExecuting(row.id!) ? '执行中...' : '执行' }}
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
