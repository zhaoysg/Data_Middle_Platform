<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecMaskTemplate } from '#/api/metadata/model';

import { PlusOutlined, ThunderboltOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Badge, Modal, Popconfirm, Space, Tag } from 'ant-design-vue';
import { computed, ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secMaskTemplateList,
  secMaskTemplateRemove,
  secMaskTemplateUpdate,
} from '#/api/metadata/security/mask-template';
import { TableSwitch } from '#/components/table';

import { applyMask, maskTypeColor } from './mask-preview';
import maskTemplateDrawer from './mask-template-drawer.vue';

const maskTypeLabelMap: Record<string, string> = {
  NONE: '不脱敏',
  MASK: '掩码遮蔽',
  HIDE: '完全隐藏',
  ENCRYPT: '加密',
  DELETE: '删除',
  HASH: '哈希',
  FORMAT_KEEP: '保留格式加密',
  RANGE: '区间化',
  WATERMARK: '水印',
  SHUFFLE: '打乱',
  CUSTOM: '自定义',
};

const maskTypeFilterOptions = [
  { label: '不脱敏', value: 'NONE' },
  { label: '掩码遮蔽', value: 'MASK' },
  { label: '完全隐藏', value: 'HIDE' },
  { label: '加密', value: 'ENCRYPT' },
  { label: '删除', value: 'DELETE' },
  { label: '哈希', value: 'HASH' },
  { label: '保留格式加密', value: 'FORMAT_KEEP' },
  { label: '区间化', value: 'RANGE' },
  { label: '水印', value: 'WATERMARK' },
  { label: '打乱', value: 'SHUFFLE' },
  { label: '自定义', value: 'CUSTOM' },
];

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'templateName', label: '模板名称', component: 'Input' },
    {
      fieldName: 'templateType',
      label: '脱敏类型',
      component: 'Select',
      componentProps: {
        options: maskTypeFilterOptions,
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
      title: '脱敏类型',
      field: 'templateType',
      width: 120,
      slots: { default: 'templateType' },
    },
    { title: '掩码正则', field: 'maskPattern', minWidth: 200, slots: { default: 'maskPattern' } },
    { title: '模板描述', field: 'templateDesc', minWidth: 160 },
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

const testOpen = ref(false);
const testingRow = ref<SecMaskTemplate | null>(null);
const testInput = ref('');
const testOutput = computed(() => {
  if (!testInput.value || !testingRow.value?.templateType) {
    return '';
  }
  return applyMask(
    testInput.value,
    testingRow.value.templateType,
    testingRow.value.maskPattern,
    testingRow.value.maskChar,
    testingRow.value.maskHeadKeep,
    testingRow.value.maskTailKeep,
    testingRow.value.maskPosition,
  );
});

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecMaskTemplate) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

function openTest(row: SecMaskTemplate) {
  testingRow.value = row;
  testInput.value = '';
  testOpen.value = true;
}

function closeTest() {
  testOpen.value = false;
  testingRow.value = null;
  testInput.value = '';
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
      <template #templateType="{ row }">
        <Tag :color="maskTypeColor(row.templateType)">
          {{
            row.templateType
              ? maskTypeLabelMap[row.templateType] || row.templateType
              : '-'
          }}
        </Tag>
      </template>
      <template #maskPattern="{ row }">
        <code v-if="row.maskPattern" class="text-xs">{{ row.maskPattern }}</code>
        <span v-else class="text-muted-foreground">—</span>
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
          :api="
            () =>
              secMaskTemplateUpdate({
                id: row.id,
                enabled: row.enabled === '1' ? '0' : '1',
              })
          "
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <a-tooltip title="测试脱敏">
            <a-button type="link" size="small" @click="openTest(row)">
              <ThunderboltOutlined />
            </a-button>
          </a-tooltip>
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

    <Modal
      v-model:open="testOpen"
      :title="`测试脱敏 — ${testingRow?.templateName || ''}`"
      width="560px"
      :footer="null"
      destroy-on-close
      @cancel="closeTest"
    >
      <template v-if="testingRow">
        <div class="mb-4 flex flex-wrap items-center gap-2">
          <Tag :color="maskTypeColor(testingRow.templateType)">
            {{
              testingRow.templateType
                ? maskTypeLabelMap[testingRow.templateType] || testingRow.templateType
                : '-'
            }}
          </Tag>
          <code class="text-xs text-muted-foreground">{{ testingRow.templateCode }}</code>
        </div>
        <div v-if="testingRow.maskPattern" class="mb-3 text-xs">
          <span class="text-muted-foreground">掩码正则：</span>
          <code>{{ testingRow.maskPattern }}</code>
        </div>
        <div class="mb-2 text-sm font-medium">测试输入</div>
        <a-input
          v-model:value="testInput"
          placeholder="输入样例数据查看预览"
          allow-clear
          class="mb-3"
        />
        <div class="mb-1 text-sm font-medium">预览结果</div>
        <div
          class="bg-muted/40 border-border mb-4 min-h-[44px] rounded border px-3 py-2 font-mono text-sm"
        >
          {{ testOutput || '等待输入…' }}
        </div>
      </template>
    </Modal>
  </Page>
</template>
