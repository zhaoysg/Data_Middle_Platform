<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DprofileTask } from '#/api/metadata/model';

import { PlusOutlined, PlayCircleOutlined, StopOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Button, Popconfirm, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  dprofileTaskAdd,
  dprofileTaskList,
  dprofileTaskRemove,
  dprofileTaskStart,
  dprofileTaskStop,
  dprofileTaskUpdate,
} from '#/api/metadata/dprofile/task';
import { TableSwitch } from '#/components/table';

import taskDrawer from './task-drawer.vue';

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
    { fieldName: 'taskName', label: '任务名称', component: 'Input' },
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
    { title: '任务名称', field: 'taskName', width: 200, fixed: 'left' },
    { title: '任务编码', field: 'taskCode', width: 150 },
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '探查级别', field: 'profileLevel', width: 100 },
    {
      title: '触发方式',
      field: 'triggerType',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? triggerTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
    { title: '上次运行', field: 'lastRunTime', width: 180 },
    {
      title: '状态',
      field: 'status',
      width: 100,
      slots: { default: 'status' },
    },
    { title: '状态', field: 'enabled', width: 80, slots: { default: 'enabled' } },
    { title: '操作', field: 'action', width: 180, fixed: 'right', slots: { default: 'action' } },
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
        const data = await dprofileTaskList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dprofile-task-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [TaskDrawer, drawerApi] = useVbenDrawer({ connectedComponent: taskDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: DprofileTask) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleStart(row: DprofileTask) {
  await dprofileTaskStart(row.id!);
  await tableApi.query();
}

async function handleStop(row: DprofileTask) {
  await dprofileTaskStop(row.id!);
  await tableApi.query();
}

async function handleDelete(row: DprofileTask) {
  await dprofileTaskRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: DprofileTask) => row.id)
    .filter((id): id is number => id !== undefined);
  await dprofileTaskRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="探查任务">
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
      <template #enabled="{ row }">
        <TableSwitch
          v-model:value="row.enabled"
          :api="() => dprofileTaskUpdate({ id: row.id, enabled: row.enabled === '0' ? '1' : '0' })"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleEdit(row)">编辑</a-button>
          <Button
            v-if="row.status !== 'RUNNING'"
            type="link"
            size="small"
            :icon="h(PlayCircleOutlined)"
            @click="handleStart(row)"
          >
            启动
          </Button>
          <Button
            v-if="row.status === 'RUNNING'"
            type="link"
            size="small"
            :icon="h(StopOutlined)"
            danger
            @click="handleStop(row)"
          >
            停止
          </Button>
          <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger>删除</a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <TaskDrawer @reload="tableApi.query()" />
  </Page>
</template>
