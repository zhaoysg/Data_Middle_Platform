<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcRuleDef } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  dqcRuleAdd,
  dqcRuleList,
  dqcRuleRemove,
  dqcRuleUpdate,
} from '#/api/metadata/dqc/rule';
import { TableSwitch } from '#/components/table';

import ruleDrawer from './rule-drawer.vue';

const dimensionLabelMap: Record<string, string> = {
  COMPLETENESS: '完整性',
  UNIQUENESS: '唯一性',
  ACCURACY: '准确性',
  CONSISTENCY: '一致性',
  TIMELINESS: '及时性',
  VALIDITY: '有效性',
  CUSTOM: '自定义',
};

const errorLevelColorMap: Record<string, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'green',
  CRITICAL: 'purple',
};

const errorLevelLabelMap: Record<string, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  CRITICAL: '危急',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'ruleName', label: '规则名称', component: 'Input' },
    {
      fieldName: 'dimensions',
      label: '质量维度',
      component: 'Select',
      componentProps: {
        options: [
          { label: '完整性', value: 'COMPLETENESS' },
          { label: '唯一性', value: 'UNIQUENESS' },
          { label: '准确性', value: 'ACCURACY' },
          { label: '一致性', value: 'CONSISTENCY' },
          { label: '及时性', value: 'TIMELINESS' },
          { label: '有效性', value: 'VALIDITY' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'enabled',
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
    { title: '规则名称', field: 'ruleName', width: 180, fixed: 'left' },
    { title: '规则编码', field: 'ruleCode', width: 150 },
    { title: '模板名称', field: 'templateName', width: 120 },
    {
      title: '质量维度',
      field: 'dimensions',
      width: 140,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        if (!cellValue) return '-';
        return cellValue
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean)
          .map((code) => dimensionLabelMap[code] || code)
          .join('、');
      },
    },
    {
      title: '数据源',
      field: 'targetDsName',
      width: 120,
      formatter: ({ row }: { row: DqcRuleDef }) =>
        (row as any).targetDsName || (row as any).dsName || '-',
    },
    { title: '目标表', field: 'targetTable', width: 150 },
    { title: '目标字段', field: 'targetColumn', width: 120 },
    {
      title: '错误级别',
      field: 'errorLevel',
      width: 100,
      slots: { default: 'errorLevel' },
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
        const data = await dqcRuleList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dqc-rule-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [RuleDrawer, drawerApi] = useVbenDrawer({ connectedComponent: ruleDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: DqcRuleDef) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: DqcRuleDef) {
  await dqcRuleRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: DqcRuleDef) => row.id)
    .filter((id): id is number => id !== undefined);
  await dqcRuleRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="规则定义">
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
      <template #errorLevel="{ row }">
        <Tag :color="errorLevelColorMap[row.errorLevel || '']">
          {{ errorLevelLabelMap[row.errorLevel || ''] || row.errorLevel || '-' }}
        </Tag>
      </template>
      <template #enabled="{ row }">
        <TableSwitch
          v-model:value="row.enabled"
          :api="() => dqcRuleUpdate({ id: row.id, enabled: row.enabled === '0' ? '1' : '0' })"
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
    <RuleDrawer @reload="tableApi.query()" />
  </Page>
</template>
