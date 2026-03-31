<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MetadataDomain } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  metadataDomainList,
  metadataDomainRemove,
  metadataDomainUpdate,
} from '#/api/metadata/domain';
import { TableSwitch } from '#/components/table';

import domainDrawer from './domain-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'domainName', label: '数据域名称', component: 'Input' },
    { fieldName: 'domainCode', label: '数据域编码', component: 'Input' },
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
    { title: '数据域名称', field: 'domainName', width: 200 },
    { title: '数据域编码', field: 'domainCode', width: 150 },
    { title: '数据域描述', field: 'domainDesc', minWidth: 200 },
    { title: '负责人', field: 'ownerName', width: 120 },
    { title: '部门', field: 'deptName', width: 150 },
    { title: '备注', field: 'remark', minWidth: 150 },
    { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
    { title: '创建时间', field: 'createTime', width: 160 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues = {}) => {
        return await metadataDomainList({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: { keyField: 'id' },
  id: 'metadata-domain-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [DomainDrawer, drawerApi] = useVbenDrawer({ connectedComponent: domainDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: MetadataDomain) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: MetadataDomain) {
  await metadataDomainRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: MetadataDomain) => row.id)
    .filter((id): id is number => id !== undefined);
  await metadataDomainRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="数据域">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" v-access:code="['metadata:domain:add']" @click="handleAdd">
            <template #icon>
              <PlusOutlined />
            </template>
            新增
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            v-access:code="['metadata:domain:remove']"
            @click="handleMultiDelete"
          >
            批量删除
          </a-button>
        </Space>
      </template>
      <template #status="{ row }">
        <TableSwitch
          v-model:value="row.status"
          :api="() => metadataDomainUpdate({ id: row.id, status: row.status === '0' ? '1' : '0' })"
          v-access:code="['metadata:domain:edit']"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:domain:edit']"
            @click="handleEdit(row)"
          >
            编辑
          </a-button>
          <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger v-access:code="['metadata:domain:remove']">
              删除
            </a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <DomainDrawer @reload="tableApi.query()" />
  </Page>
</template>
