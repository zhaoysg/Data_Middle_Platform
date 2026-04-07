<script setup lang="ts">
import { ref } from 'vue';

import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DqcRuleTemplate } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
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
    { title: '模板名称', field: 'templateName', minWidth: 180, fixed: 'left' },
    { title: '模板编码', field: 'templateCode', minWidth: 150 },
    {
      title: '质量维度',
      field: 'dimension',
      minWidth: 100,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? dimensionLabelMap[cellValue] || cellValue : '-';
      },
    },
    {
      title: '规则类型',
      field: 'ruleType',
      minWidth: 120,
      formatter: ({ cellValue }: { cellValue?: string }) => {
        return cellValue ? ruleTypeLabelMap[cellValue] || cellValue : '-';
      },
    },
    {
      title: '内置',
      field: 'builtin',
      minWidth: 80,
      slots: { default: 'builtin' },
    },
    { title: '状态', field: 'enabled', minWidth: 80, slots: { default: 'enabled' } },
    { title: '创建时间', field: 'createTime', minWidth: 180 },
    { title: '操作', field: 'action', minWidth: 150, fixed: 'right', slots: { default: 'action' } },
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

const [BasicTable, tableApi] = useVbenVxeGrid({
  formOptions,
  gridOptions,
  gridClass: 'w-full',
});
const [TemplateDrawer, drawerApi] = useVbenDrawer({ connectedComponent: templateDrawer });

const detailVisible = ref(false);
const detailRecord = ref<Record<string, any>>({});
const detailLoading = ref(false);

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
  <Page
    :auto-content-height="true"
    content-class="flex min-h-0 w-full flex-col"
  >
    <BasicTable class="min-h-0 flex-1 w-full overflow-hidden" table-title="规则模板">
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
