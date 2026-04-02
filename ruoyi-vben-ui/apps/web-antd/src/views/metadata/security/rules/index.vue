<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecSensitivityRule } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Badge, Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secSensitivityRuleList,
  secSensitivityRuleRemove,
  secSensitivityRuleUpdate,
} from '#/api/metadata/security/rules';
import { TableSwitch } from '#/components/table';

import rulesDrawer from './rules-drawer.vue';

const ruleTypeLabelMap: Record<string, string> = {
  REGEX: '正则匹配',
  COLUMN_NAME: '字段名匹配',
  DATA_TYPE: '数据类型',
  CUSTOM: '自定义',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'ruleName', label: '规则名称', component: 'Input' },
    {
      fieldName: 'ruleType',
      label: '规则类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '正则匹配', value: 'REGEX' },
          { label: '字段名匹配', value: 'COLUMN_NAME' },
          { label: '数据类型', value: 'DATA_TYPE' },
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
    { title: '规则名称', field: 'ruleName', width: 180, fixed: 'left' },
    { title: '规则编码', field: 'ruleCode', width: 150 },
    {
      title: '规则类型',
      field: 'ruleType',
      width: 120,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? ruleTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
    { title: '目标等级', field: 'targetLevelCode', width: 100 },
    { title: '目标分类', field: 'targetClassCode', width: 120 },
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
        const data = await secSensitivityRuleList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-rules-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [RulesDrawer, drawerApi] = useVbenDrawer({ connectedComponent: rulesDrawer });

function buildRulePayload(row: SecSensitivityRule) {
  return {
    id: row.id,
    ruleCode: row.ruleCode,
    ruleName: row.ruleName,
    ruleType: row.ruleType,
    ruleExpr: row.ruleExpr,
    targetLevelCode: row.targetLevelCode,
    targetClassCode: row.targetClassCode,
    builtin: row.builtin,
    enabled: row.enabled,
  };
}

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecSensitivityRule) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecSensitivityRule) {
  await secSensitivityRuleRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecSensitivityRule) => row.id)
    .filter((id): id is number => id !== undefined);
  await secSensitivityRuleRemove(ids);
  await tableApi.query();
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="敏感识别规则">
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
          checked-value="1"
          un-checked-value="0"
          :api="() => secSensitivityRuleUpdate(buildRulePayload(row))"
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
    <RulesDrawer @reload="tableApi.query()" />
  </Page>
</template>
