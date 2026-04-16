<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecMaskStrategy } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secMaskStrategyAdd,
  secMaskStrategyList,
  secMaskStrategyRemove,
  secMaskStrategyUpdate,
} from '#/api/metadata/security/strategy';
import { TableSwitch } from '#/components/table';

import strategyDrawer from './strategy-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'strategyName', label: '策略名称', component: 'Input' },
    { fieldName: 'dsName', label: '数据源', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '策略名称', field: 'strategyName', width: 200, fixed: 'left' },
    { title: '策略编码', field: 'strategyCode', width: 150 },
    { title: '数据源', field: 'dsName', width: 150 },
    { title: '策略描述', field: 'strategyDesc', minWidth: 200 },
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
        const data = await secMaskStrategyList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-strategy-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [StrategyDrawer, drawerApi] = useVbenDrawer({ connectedComponent: strategyDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecMaskStrategy) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecMaskStrategy) {
  await secMaskStrategyRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecMaskStrategy) => row.id)
    .filter((id): id is number => id !== undefined);
  await secMaskStrategyRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="脱敏策略">
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
      <template #enabled="{ row }">
        <TableSwitch
          v-model:value="row.enabled"
          checked-value="1"
          un-checked-value="0"
          :api="() => secMaskStrategyUpdate({ id: row.id, enabled: row.enabled === '1' ? '0' : '1' })"
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
    <StrategyDrawer @reload="tableApi.query()" />
  </Page>
</template>
