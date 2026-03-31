<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MetadataLayer } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  metadataLayerList,
  metadataLayerRemove,
  metadataLayerUpdate,
} from '#/api/metadata/layer';
import { TableSwitch } from '#/components/table';

import layerDrawer from './layer-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'layerName', label: '分层名称', component: 'Input' },
    { fieldName: 'layerCode', label: '分层编码', component: 'Input' },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '正常', value: '0' },
          { label: '停用', value: '1' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50 },
    { title: '分层编码', field: 'layerCode', width: 120 },
    { title: '分层名称', field: 'layerName', width: 150 },
    { title: '分层描述', field: 'layerDesc', minWidth: 250 },
    { title: '颜色标识', field: 'layerColor', width: 120, slots: { default: 'layerColor' } },
    { title: '排序', field: 'sortOrder', width: 80 },
    { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
    { title: '备注', field: 'remark', minWidth: 150 },
    { title: '创建时间', field: 'createTime', width: 180 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues = {}) => {
        return await metadataLayerList({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: { keyField: 'id' },
  id: 'metadata-layer-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [LayerDrawer, drawerApi] = useVbenDrawer({ connectedComponent: layerDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: MetadataLayer) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: MetadataLayer) {
  await metadataLayerRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: MetadataLayer) => row.id)
    .filter((id): id is number => id !== undefined);
  await metadataLayerRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="数仓分层">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" v-access:code="['metadata:layer:add']" @click="handleAdd">
            <template #icon>
              <PlusOutlined />
            </template>
            新增
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            v-access:code="['metadata:layer:remove']"
            @click="handleMultiDelete"
          >
            批量删除
          </a-button>
        </Space>
      </template>
      <template #layerColor="{ row }">
        <Tag :color="row.layerColor" style="margin: 0;">
          {{ row.layerColor }}
        </Tag>
      </template>
      <template #status="{ row }">
        <TableSwitch
          v-model:value="row.status"
          :api="() => metadataLayerUpdate({ id: row.id, status: row.status === '0' ? '1' : '0' })"
          v-access:code="['metadata:layer:edit']"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:layer:edit']"
            @click="handleEdit(row)"
          >
            编辑
          </a-button>
          <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger v-access:code="['metadata:layer:remove']">
              删除
            </a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <LayerDrawer @reload="tableApi.query()" />
  </Page>
</template>
