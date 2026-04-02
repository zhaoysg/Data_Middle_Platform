<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecClassification } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secClassificationList,
  secClassificationRemove,
  secClassificationUpdate,
} from '#/api/metadata/security/classification';
import { TableSwitch } from '#/components/table';

import classificationDrawer from './classification-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'className', label: '分类名称', component: 'Input' },
    { fieldName: 'classCode', label: '分类编码', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '分类名称', field: 'className', width: 180, fixed: 'left' },
    { title: '分类编码', field: 'classCode', width: 150 },
    { title: '分类描述', field: 'classDesc', minWidth: 200 },
    { title: '排序', field: 'sortOrder', width: 80 },
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
        const data = await secClassificationList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-classification-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [ClassificationDrawer, drawerApi] = useVbenDrawer({ connectedComponent: classificationDrawer });

function buildClassificationPayload(row: SecClassification) {
  return {
    id: row.id,
    classCode: row.classCode,
    className: row.className,
    classDesc: row.classDesc,
    sortOrder: row.sortOrder,
    enabled: row.enabled,
  };
}

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecClassification) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecClassification) {
  await secClassificationRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecClassification) => row.id)
    .filter((id): id is number => id !== undefined);
  await secClassificationRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="数据分类">
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
          :api="() => secClassificationUpdate(buildClassificationPayload(row))"
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
    <ClassificationDrawer @reload="tableApi.query()" />
  </Page>
</template>
