<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecMaskTemplate } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Badge, Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secMaskTemplateAdd,
  secMaskTemplateList,
  secMaskTemplateRemove,
  secMaskTemplateUpdate,
} from '#/api/metadata/security/mask-template';
import { TableSwitch } from '#/components/table';

import maskTemplateDrawer from './mask-template-drawer.vue';

const templateTypeLabelMap: Record<string, string> = {
  ENCRYPT: '加密',
  MASK: '掩码',
  HIDE: '隐藏',
  DELETE: '删除',
  SHUFFLE: '打乱',
  CUSTOM: '自定义',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'templateName', label: '模板名称', component: 'Input' },
    {
      fieldName: 'templateType',
      label: '模板类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '加密', value: 'ENCRYPT' },
          { label: '掩码', value: 'MASK' },
          { label: '隐藏', value: 'HIDE' },
          { label: '删除', value: 'DELETE' },
          { label: '打乱', value: 'SHUFFLE' },
          { label: '自定义', value: 'CUSTOM' },
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
    { title: '模板名称', field: 'templateName', width: 180, fixed: 'left' },
    { title: '模板编码', field: 'templateCode', width: 150 },
    {
      title: '模板类型',
      field: 'templateType',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? templateTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
    { title: '模板描述', field: 'templateDesc', minWidth: 200 },
    {
      title: '内置',
      field: 'builtin',
      width: 80,
      slots: { default: 'builtin' },
    },
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
        const data = await secMaskTemplateList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-mask-template-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [MaskTemplateDrawer, drawerApi] = useVbenDrawer({ connectedComponent: maskTemplateDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecMaskTemplate) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecMaskTemplate) {
  await secMaskTemplateRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecMaskTemplate) => row.id)
    .filter((id): id is number => id !== undefined);
  await secMaskTemplateRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="脱敏模板">
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
      <template #builtin="{ row }">
        <Badge
          :color="row.builtin === '1' ? 'green' : 'default'"
          :text="row.builtin === '1' ? '是' : '否'"
        />
      </template>
      <template #enabled="{ row }">
        <TableSwitch
          v-model:value="row.enabled"
          :api="() => secMaskTemplateUpdate({ id: row.id, enabled: row.enabled === '0' ? '1' : '0' })"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleEdit(row)">编辑</a-button>
          <Popconfirm
            v-if="row.builtin !== '1'"
            title="确认删除？"
            @confirm="handleDelete(row)"
          >
            <a-button type="link" size="small" danger>删除</a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <MaskTemplateDrawer @reload="tableApi.query()" />
  </Page>
</template>
