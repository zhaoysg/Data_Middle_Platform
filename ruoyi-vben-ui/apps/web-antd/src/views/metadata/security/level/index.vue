<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecLevel } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secLevelList,
  secLevelRemove,
  secLevelUpdate,
} from '#/api/metadata/security/level';
import { TableSwitch } from '#/components/table';

import levelDrawer from './level-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'levelName', label: '敏感等级', component: 'Input' },
    { fieldName: 'levelCode', label: '等级编码', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '等级名称', field: 'levelName', width: 160, fixed: 'left' },
    { title: '等级编码', field: 'levelCode', width: 140 },
    { title: '等级值', field: 'levelValue', width: 90, align: 'center' },
    { title: '颜色', field: 'color', width: 120, slots: { default: 'color' } },
    {
      title: '等级描述',
      field: 'levelDesc',
      width: 280,
      minWidth: 200,
      showOverflow: 'tooltip',
    },
    { title: '排序', field: 'sortOrder', width: 80, align: 'center' },
    { title: '状态', field: 'enabled', width: 80, slots: { default: 'enabled' } },
    { title: '创建时间', field: 'createTime', width: 180 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', slots: { default: 'action' } },
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
        const data = await secLevelList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-level-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [LevelDrawer, drawerApi] = useVbenDrawer({ connectedComponent: levelDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecLevel) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecLevel) {
  await secLevelRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecLevel) => row.id)
    .filter((id): id is number => id !== undefined);
  await secLevelRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="敏感等级管理">
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
      <template #color="{ row }">
        <Tag :color="row.color || 'default'">
          {{ row.color || '-' }}
        </Tag>
      </template>
      <template #enabled="{ row }">
        <TableSwitch
          v-model:value="row.enabled"
          :api="() => secLevelUpdate({ id: row.id, enabled: row.enabled === '0' ? '1' : '0' })"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleEdit(row)">编辑</a-button>
          <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger>删除</a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <LevelDrawer @reload="tableApi.query()" />
  </Page>
</template>
