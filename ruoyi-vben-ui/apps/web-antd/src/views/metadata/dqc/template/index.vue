<script setup lang="ts">
import { computed, ref } from 'vue';

import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcRuleTemplate } from '#/api/metadata/model';

import { PlusOutlined, ContainerOutlined, AppstoreOutlined, StopOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Badge, Descriptions, Modal, Popconfirm, Space } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  dqcTemplateList,
  dqcTemplateInfo,
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

const builtinLabelMap: Record<string, string> = {
  '1': '是',
  '0': '否',
};

const enabledLabelMap: Record<string, string> = {
  '0': '启用',
  '1': '停用',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'templateName', label: '模板名称', component: 'Input', defaultValue: '' },
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
    {
      fieldName: 'builtin',
      label: '模板来源',
      component: 'Select',
      componentProps: {
        options: [
          { label: '内置', value: '1' },
          { label: '自定义', value: '0' },
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
    { title: '操作', field: 'action', width: 150, fixed: 'right', slots: { default: 'action' } },
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

const detailVisible = ref(false);
const detailRecord = ref<Record<string, any>>({});
const detailLoading = ref(false);

const gridData = computed<DqcRuleTemplate[]>(() => {
  try {
    const data = tableApi.grid?.getData();
    return Array.isArray(data) ? (data as DqcRuleTemplate[]) : [];
  } catch {
    return [];
  }
});

async function loadDetail(id: number) {
  detailLoading.value = true;
  detailVisible.value = true;
  try {
    detailRecord.value = (await dqcTemplateInfo(id)) || {};
  } catch (e) {
    console.error(e);
  } finally {
    detailLoading.value = false;
  }
}

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: DqcRuleTemplate) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

function handleViewDetail(record: DqcRuleTemplate) {
  loadDetail(record.id!);
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
    <!-- Page Title Area -->
    <div class="flex items-center gap-3 mb-6">
      <div
        class="flex items-center justify-center w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-md"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="currentColor"
          class="w-5 h-5"
        >
          <path
            fill-rule="evenodd"
            d="M2.25 4.125c0-1.036.84-1.875 1.875-1.875h16.5c1.036 0 1.875.84 1.875 1.875v15.75c0 1.036-.84 1.875-1.875 1.875H4.125A1.875 1.875 0 0 1 2.25 19.875V4.125Zm1.875 1.125c.621 0 1.125.504 1.125 1.125v13.5c0 .621-.504 1.125-1.125 1.125H4.125A1.125 1.125 0 0 1 3 19.875V6.375c0-.621.504-1.125 1.125-1.125h16.5Z"
            clip-rule="evenodd"
          />
          <path
            d="M11.25 9h1.5v2.25H11.25V9ZM8.625 9h1.5v2.25H8.625V9ZM6 9h1.5v2.25H6V9Zm2.625 4.5h1.5V15H8.625V13.5Zm4.125 0h1.5V15h-1.5V13.5Zm-3.375 0H9.75V15h1.125V13.5ZM14.625 9h1.5v2.25h-1.5V9Zm-1.125 4.5h1.5V15h-1.5V13.5Z"
          />
        </svg>
      </div>
      <div>
        <h2 class="text-lg font-semibold text-gray-800 m-0 leading-tight">规则模板管理</h2>
        <p class="text-sm text-gray-500 m-0">配置和管理数据质量规则模板</p>
      </div>
    </div>

    <!-- Stat Cards -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div
        class="bg-gradient-to-r from-blue-500 to-blue-600 rounded-xl p-5 text-white shadow-md hover:shadow-lg transition-shadow"
      >
        <div class="flex items-center justify-between">
          <div>
            <div class="text-blue-100 text-sm font-medium mb-1">模板总数</div>
            <div class="text-2xl font-bold">
              {{ gridData.length }}
            </div>
          </div>
          <div class="opacity-80">
            <ContainerOutlined class="text-3xl" />
          </div>
        </div>
      </div>
      <div
        class="bg-gradient-to-r from-emerald-500 to-emerald-600 rounded-xl p-5 text-white shadow-md hover:shadow-lg transition-shadow"
      >
        <div class="flex items-center justify-between">
          <div>
            <div class="text-emerald-100 text-sm font-medium mb-1">内置模板</div>
            <div class="text-2xl font-bold">
              {{ gridData.filter((r) => r.builtin === '1').length }}
            </div>
          </div>
          <div class="opacity-80">
            <AppstoreOutlined class="text-3xl" />
          </div>
        </div>
      </div>
      <div
        class="bg-gradient-to-r from-purple-500 to-purple-600 rounded-xl p-5 text-white shadow-md hover:shadow-lg transition-shadow"
      >
        <div class="flex items-center justify-between">
          <div>
            <div class="text-purple-100 text-sm font-medium mb-1">自定义模板</div>
            <div class="text-2xl font-bold">
              {{ gridData.filter((r) => r.builtin === '0').length }}
            </div>
          </div>
          <div class="opacity-80">
            <AppstoreOutlined class="text-3xl" />
          </div>
        </div>
      </div>
      <div
        class="bg-gradient-to-r from-gray-500 to-gray-600 rounded-xl p-5 text-white shadow-md hover:shadow-lg transition-shadow"
      >
        <div class="flex items-center justify-between">
          <div>
            <div class="text-gray-100 text-sm font-medium mb-1">停用模板</div>
            <div class="text-2xl font-bold">
              {{ gridData.filter((r) => r.enabled === '1').length }}
            </div>
          </div>
          <div class="opacity-80">
            <StopOutlined class="text-3xl" />
          </div>
        </div>
      </div>
    </div>

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
          <a-button type="link" size="small" @click="handleViewDetail(row)">查看详情</a-button>
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

    <!-- Detail Drawer -->
    <Modal
      v-model:open="detailVisible"
      title="模板详情"
      :footer="null"
      width="600"
      :loading="detailLoading"
    >
      <Descriptions :column="2" bordered size="small" class="mt-2">
        <Descriptions.Item label="模板名称" :span="2">{{
          detailRecord.templateName || '-'
        }}</Descriptions.Item>
        <Descriptions.Item label="模板编码" :span="2">{{
          detailRecord.templateCode || '-'
        }}</Descriptions.Item>
        <Descriptions.Item label="质量维度">
          {{ dimensionLabelMap[detailRecord.dimension || ''] || detailRecord.dimension || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="规则类型">
          {{ ruleTypeLabelMap[detailRecord.ruleType || ''] || detailRecord.ruleType || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="适用级别">
          {{ detailRecord.applyLevel || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="内置">
          {{ builtinLabelMap[detailRecord.builtin || ''] || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          {{ enabledLabelMap[detailRecord.enabled || ''] || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="创建时间">
          {{ detailRecord.createTime || '-' }}
        </Descriptions.Item>
        <Descriptions.Item label="模板描述" :span="2">{{
          detailRecord.templateDesc || '-'
        }}</Descriptions.Item>
        <Descriptions.Item label="默认表达式" :span="2">
          <pre
            class="bg-gray-50 rounded p-2 text-xs whitespace-pre-wrap m-0 max-h-32 overflow-auto">{{
              detailRecord.defaultExpr || '-'
            }}</pre>
        </Descriptions.Item>
        <Descriptions.Item label="阈值配置" :span="2">
          <pre
            class="bg-gray-50 rounded p-2 text-xs whitespace-pre-wrap m-0 max-h-24 overflow-auto">{{
              detailRecord.thresholdJson || '-'
            }}</pre>
        </Descriptions.Item>
        <Descriptions.Item label="参数规格" :span="2">
          <pre
            class="bg-gray-50 rounded p-2 text-xs whitespace-pre-wrap m-0 max-h-24 overflow-auto">{{
              detailRecord.paramSpec || '-'
            }}</pre>
        </Descriptions.Item>
      </Descriptions>
    </Modal>
  </Page>
</template>
