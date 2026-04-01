<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcRuleTemplate } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Badge, Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  dqcTemplateAdd,
  dqcTemplateList,
  dqcTemplateRemove,
  dqcTemplateUpdate,
} from '#/api/metadata/dqc/template';
import { TableSwitch } from '#/components/table';

import templateDrawer from './template-drawer.vue';

const dimensionLabelMap: Record<string, string> = {
  COMPLETENESS: '完整性',
  UNIQUENESS: '唯一性',
  ACCURACY: '准确性',
  CONSISTENCY: '一致性',
  TIMELINESS: '时效性',
  VALIDITY: '有效性',
  CUSTOM: '自定义',
};

const ruleTypeLabelMap: Record<string, string> = {
  NULL_CHECK: '空值检查',
  UNIQUE: '唯一性',
  REGEX: '正则匹配',
  THRESHOLD: '阈值检查',
  FLUCTUATION: '波动检查',
  CUSTOM_SQL: '自定义SQL',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'templateName', label: '模板名称', component: 'Input' },
    {
      fieldName: 'dimension',
      label: '质量维度',
      component: 'Select',
      componentProps: {
        options: [
          { label: '完整性', value: 'COMPLETENESS' },
          { label: '唯一性', value: 'UNIQUENESS' },
          { label: '准确性', value: 'ACCURACY' },
          { label: '一致性', value: 'CONSISTENCY' },
          { label: '时效性', value: 'TIMELINESS' },
          { label: '有效性', value: 'VALIDITY' },
          { label: '自定义', value: 'CUSTOM' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'ruleType',
      label: '规则类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '空值检查', value: 'NULL_CHECK' },
          { label: '唯一性', value: 'UNIQUE' },
          { label: '正则匹配', value: 'REGEX' },
          { label: '阈值检查', value: 'THRESHOLD' },
          { label: '波动检查', value: 'FLUCTUATION' },
          { label: '自定义SQL', value: 'CUSTOM_SQL' },
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
      title: '质量维度',
      field: 'dimension',
      width: 100,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? dimensionLabelMap[cellValue] || cellValue : '-';
      },
    },
    {
      title: '规则类型',
      field: 'ruleType',
      width: 120,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? ruleTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
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
        const data = await dqcTemplateList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dqc-template-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [TemplateDrawer, drawerApi] = useVbenDrawer({ connectedComponent: templateDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: DqcRuleTemplate) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: DqcRuleTemplate) {
  await dqcTemplateRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: DqcRuleTemplate) => row.id)
    .filter((id): id is number => id !== undefined);
  await dqcTemplateRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="规则模板">
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
          :api="() => dqcTemplateUpdate({ id: row.id, enabled: row.enabled === '0' ? '1' : '0' })"
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
    <TemplateDrawer @reload="tableApi.query()" />
  </Page>
</template>
