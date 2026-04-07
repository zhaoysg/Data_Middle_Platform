<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';
import { onMounted, ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  glossaryMappingAdd,
  glossaryMappingList,
  glossaryMappingRemove,
  glossaryMappingUpdate,
} from '#/api/metadata/glossary';
import { TableSwitch } from '#/components/table';

import mappingDrawer from './mapping-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'termName', label: '术语名称', component: 'Input' },
    {
      fieldName: 'assetType',
      label: '资产类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '表', value: 'TABLE' },
          { label: '列', value: 'COLUMN' },
        ],
        allowClear: true,
      },
    },
    { fieldName: 'tableName', label: '表名', component: 'Input' },
    { fieldName: 'columnName', label: '列名', component: 'Input' },
    {
      fieldName: 'mappingType',
      label: '映射类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '包含', value: 'CONTAINS' },
          { label: '定义', value: 'DEFINES' },
          { label: '引用', value: 'REFERENCES' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-3 lg:grid-cols-4',
};

const assetTypeLabelMap: Record<string, string> = {
  TABLE: '表',
  COLUMN: '列',
};
const assetTypeColorMap: Record<string, string> = {
  TABLE: 'blue',
  COLUMN: 'purple',
};
const mappingTypeLabelMap: Record<string, string> = {
  CONTAINS: '包含',
  DEFINES: '定义',
  REFERENCES: '引用',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '术语名称', field: 'termName', width: 180, fixed: 'left' },
    {
      title: '资产类型',
      field: 'assetType',
      width: 100,
      slots: { default: 'assetType' },
    },
    { title: '数据源', field: 'dsName', width: 120 },
    { title: '表名', field: 'tableName', width: 180 },
    { title: '列名', field: 'columnName', width: 150 },
    {
      title: '映射类型',
      field: 'mappingType',
      width: 100,
      slots: { default: 'mappingType' },
    },
    { title: '置信度', field: 'confidence', width: 80 },
    { title: '创建时间', field: 'createTime', width: 180 },
    { title: '操作', field: 'action', width: 200, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page, form }: any) => {
        const params: any = {
          ...form,
          pageNum: page?.currentPage,
          pageSize: page?.pageSize,
        };
        const data = await glossaryMappingList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'glossary-mapping-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [MappingDrawer, drawerApi] = useVbenDrawer({ connectedComponent: mappingDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: any) {
  drawerApi.setData({ id: record.id, ...record });
  drawerApi.open();
}

async function handleDelete(row: any) {
  await glossaryMappingRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows.map((r: any) => r.id).filter((id: any): id is number => id !== undefined);
  await glossaryMappingRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="术语映射">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" @click="handleAdd">
            <template #icon>
              <PlusOutlined />
            </template>
            新增映射
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
      <template #assetType="{ row }">
        <Tag :color="assetTypeColorMap[row.assetType || 'TABLE']">
          {{ assetTypeLabelMap[row.assetType || 'TABLE'] || row.assetType }}
        </Tag>
      </template>
      <template #mappingType="{ row }">
        <Tag>
          {{ mappingTypeLabelMap[row.mappingType || 'CONTAINS'] || row.mappingType }}
        </Tag>
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
    <MappingDrawer @reload="tableApi.query()" />
  </Page>
</template>
