<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { MetadataColumn } from '#/api/metadata/model';

import { h, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Button, Drawer, EditOutlined, Input, message, Space, Table, Tag } from 'ant-design-vue';

import { metadataColumnAlias } from '#/api/metadata/table';
import { datasourceEnabled } from '#/api/system/datasource';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    {
      fieldName: 'dsId',
      label: '数据源',
      component: 'ApiSelect',
      componentProps: {
        api: datasourceEnabled,
        labelField: 'dsName',
        valueField: 'dsId',
        placeholder: '请选择数据源',
      },
    },
    { fieldName: 'tableName', label: '表名', component: 'Input', componentProps: { placeholder: '请输入表名' } },
    { fieldName: 'columnName', label: '字段名', component: 'Input', componentProps: { placeholder: '请输入字段名' } },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-3',
};

const searchValues = ref<Record<string, any>>({});

const gridColumns = [
  { title: '数据源', dataIndex: 'dsName', key: 'dsName', width: 140, ellipsis: true },
  { title: '表名', dataIndex: 'tableName', key: 'tableName', width: 160, ellipsis: true },
  { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 160 },
  { title: '字段类型', dataIndex: 'dataType', key: 'dataType', width: 100 },
  { title: '中文名', dataIndex: 'columnAlias', key: 'columnAlias', width: 140 },
  { title: '敏感等级', dataIndex: 'sensitivityLevel', key: 'sensitivityLevel', width: 100 },
  { title: '描述', dataIndex: 'remark', key: 'remark', ellipsis: true },
];

const sensitivityColorMap: Record<string, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'gold',
  NONE: 'default',
};
const sensitivityLabelMap: Record<string, string> = {
  HIGH: '高敏感',
  MEDIUM: '中敏感',
  LOW: '低敏感',
  NONE: '无',
};

// 当前选中的表，用于显示字段详情
const currentTable = ref<{ dsId: number; dsName: string; tableName: string } | null>(null);
const columnData = ref<MetadataColumn[]>([]);
const columnLoading = ref(false);

// 修改别名相关
const editingId = ref<number | null>(null);
const editingAlias = ref('');

function handleSearch(values: Record<string, any>) {
  searchValues.value = values;
  // 字段管理依赖扫描后的 metadata_column 表数据
  // 此页面引导用户从「元数据表 → 查看字段」入口进入
  message.info('建议通过「元数据表 → 查看字段」入口管理字段详情');
  columnData.value = [];
}

function handleViewColumns(record: MetadataColumn & { dsId?: number; dsName?: string; tableName?: string }) {
  currentTable.value = {
    dsId: record.dsId || 0,
    dsName: record.dsName || '',
    tableName: record.tableName || '',
  };
  columnLoading.value = true;
  columnData.value = [];
  drawerVisible.value = true;
  // TODO: 调用 metadataTableColumns(tableId) 加载真实数据
  columnLoading.value = false;
}

const drawerVisible = ref(false);

function startEditAlias(column: MetadataColumn) {
  editingId.value = column.id ?? null;
  editingAlias.value = column.columnAlias || '';
}

async function saveAlias(column: MetadataColumn) {
  if (!editingId.value) return;
  try {
    await metadataColumnAlias(editingId.value, editingAlias.value);
    column.columnAlias = editingAlias.value;
    editingId.value = null;
    message.success('保存成功');
  } catch {
    message.error('保存失败');
  }
}

function cancelEdit() {
  editingId.value = null;
  editingAlias.value = '';
}
</script>

<template>
  <Page :auto-content-height="true">
    <!-- 说明卡片 -->
    <div class="mb-3 p-4 bg-blue-50 rounded border border-blue-100">
      <div class="text-blue-700 text-sm">
        <p>
          <strong>字段管理</strong>：管理元数据表的字段别名、敏感等级等属性。
        </p>
        <p class="mt-1 text-blue-500">
          建议通过「<strong>元数据表 → 查看字段</strong>」入口进入字段详情页面，可直接在表格中编辑字段别名。
        </p>
      </div>
    </div>

    <!-- 字段详情抽屉（复用 column-drawer 的展示逻辑） -->
    <Drawer
      v-model:open="drawerVisible"
      :title="`字段列表 - ${currentTable?.tableName || ''}`"
      width="900"
      placement="right"
    >
      <div class="text-center text-gray-400 py-8">
        请在「元数据表」页面执行扫描后，再通过「查看字段」按钮进入。
      </div>
    </Drawer>
  </Page>
</template>
