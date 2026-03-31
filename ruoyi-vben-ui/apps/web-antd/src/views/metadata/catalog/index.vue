<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MetadataCatalog } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  metadataCatalogRemove,
  metadataCatalogTree,
  metadataCatalogUpdate,
} from '#/api/metadata/catalog';
import { TableSwitch } from '#/components/table';

import catalogDrawer from './catalog-drawer.vue';

const catalogTypeLabelMap = {
  ALBUM: '专辑',
  BUSINESS_DOMAIN: '业务域',
  DATA_DOMAIN: '数据域',
} as const;

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'catalogName', label: '目录名称', component: 'Input' },
    { fieldName: 'catalogCode', label: '目录编码', component: 'Input' },
    {
      fieldName: 'catalogType',
      label: '目录类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '业务域', value: 'BUSINESS_DOMAIN' },
          { label: '数据域', value: 'DATA_DOMAIN' },
          { label: '专辑', value: 'ALBUM' },
        ],
        allowClear: true,
      },
    },
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
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '目录名称', field: 'catalogName', width: 200, fixed: 'left', treeNode: true },
    { title: '目录编码', field: 'catalogCode', width: 150 },
    {
      title: '目录类型',
      field: 'catalogType',
      width: 120,
      formatter: ({ cellValue }: { cellValue?: keyof typeof catalogTypeLabelMap }) => {
        return cellValue ? catalogTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
    { title: '父目录', field: 'parentName', width: 160 },
    { title: '排序', field: 'sortOrder', width: 80 },
    { title: '备注', field: 'remark', minWidth: 150 },
    { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
    { title: '创建时间', field: 'createTime', width: 180 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  pagerConfig: { enabled: false },
  treeConfig: { childrenField: 'children', expandAll: false },
  proxyConfig: {
    ajax: {
      query: async () => {
        const data = await metadataCatalogTree();
        return data || [];
      },
    },
  },
  rowConfig: { keyField: 'id' },
  id: 'metadata-catalog-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [CatalogDrawer, drawerApi] = useVbenDrawer({ connectedComponent: catalogDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: MetadataCatalog) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: MetadataCatalog) {
  await metadataCatalogRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: MetadataCatalog) => row.id)
    .filter((id): id is number => id !== undefined);
  await metadataCatalogRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="资产目录">
      <template #toolbar-tools>
        <Space>
          <a-button
            type="primary"
            v-access:code="['metadata:catalog:add']"
            @click="handleAdd"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            v-access:code="['metadata:catalog:remove']"
            @click="handleMultiDelete"
          >
            批量删除
          </a-button>
        </Space>
      </template>
      <template #status="{ row }">
        <TableSwitch
          v-model:value="row.status"
          :api="() => metadataCatalogUpdate({ id: row.id, status: row.status === '0' ? '1' : '0' })"
          v-access:code="['metadata:catalog:edit']"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:catalog:edit']"
            @click="handleEdit(row)"
          >
            编辑
          </a-button>
          <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger v-access:code="['metadata:catalog:remove']">
              删除
            </a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <CatalogDrawer @reload="tableApi.query()" />
  </Page>
</template>
