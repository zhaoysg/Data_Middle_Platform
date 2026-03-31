<script setup lang="ts">
import type { MetadataColumn, MetadataTable } from '#/api/metadata/model';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { EditOutlined } from '@ant-design/icons-vue';
import { Button, Input, Table, Tag, message } from 'ant-design-vue';

import { metadataColumnAlias } from '#/api/metadata/table';

const columnsData = ref<MetadataColumn[]>([]);
const currentTable = ref<MetadataTable>();
const editingColumnId = ref<number | null>(null);
const editingAlias = ref('');

const title = computed(() => {
  return currentTable.value?.tableName
    ? `字段列表 - ${currentTable.value.tableName}`
    : '字段列表';
});

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }

    const data = drawerApi.getData() as {
      columns?: MetadataColumn[];
      table?: MetadataTable;
    };
    columnsData.value = [...(data.columns || [])];
    currentTable.value = data.table;
    editingColumnId.value = null;
    editingAlias.value = '';
  },
  onClosed: handleClosed,
});

function startEditAlias(column: MetadataColumn) {
  editingColumnId.value = column.id ?? null;
  editingAlias.value = column.columnAlias || '';
}

async function saveAlias(column: MetadataColumn) {
  try {
    await metadataColumnAlias(column.id!, editingAlias.value);
    message.success('别名保存成功');

    const index = columnsData.value.findIndex((item) => item.id === column.id);
    if (index >= 0) {
      columnsData.value[index] = {
        ...columnsData.value[index],
        columnAlias: editingAlias.value,
      };
    }

    editingColumnId.value = null;
    editingAlias.value = '';
  } catch {
    message.error('保存失败');
  }
}

function cancelEdit() {
  editingColumnId.value = null;
  editingAlias.value = '';
}

function handleClosed() {
  columnsData.value = [];
  currentTable.value = undefined;
  editingColumnId.value = null;
  editingAlias.value = '';
}

const columns = [
  {
    title: '序号',
    dataIndex: 'sortOrder',
    width: 60,
    customCell: () => ({ style: 'text-align: center' }),
  },
  { title: '字段名', dataIndex: 'columnName', width: 160 },
  {
    title: '别名（可编辑）',
    dataIndex: 'columnAlias',
    width: 160,
    slots: { customRender: 'columnAlias' },
  },
  { title: '注释', dataIndex: 'columnComment', minWidth: 150, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', width: 120 },
  { title: '可空', dataIndex: 'isNullable', width: 60, align: 'center' as const },
  { title: '键', dataIndex: 'columnKey', width: 60, align: 'center' as const },
  {
    title: '主键',
    dataIndex: 'isPrimaryKey',
    width: 60,
    align: 'center' as const,
    slots: { customRender: 'primaryKey' },
  },
  {
    title: '敏感',
    dataIndex: 'isSensitive',
    width: 60,
    align: 'center' as const,
    slots: { customRender: 'sensitive' },
  },
];
</script>

<template>
  <BasicDrawer :title="title" class="w-[1100px]">
    <Table
      :columns="columns"
      :data-source="columnsData"
      :pagination="{ pageSize: 20 }"
      :row-key="(record: MetadataColumn) => record.id || 0"
      :scroll="{ x: 1100 }"
      size="small"
    >
      <template #columnAlias="{ record }">
        <template v-if="editingColumnId === record.id">
          <Input
            v-model:value="editingAlias"
            size="small"
            style="width: 120px; margin-right: 4px;"
            @press-enter="saveAlias(record)"
            @keyup.esc="cancelEdit"
          />
          <Button type="link" size="small" @click="saveAlias(record)">
            保存
          </Button>
          <Button type="link" size="small" @click="cancelEdit">
            取消
          </Button>
        </template>
        <template v-else>
          <span style="margin-right: 4px;">{{ record.columnAlias || '-' }}</span>
          <EditOutlined
            style="color: #1890ff; cursor: pointer; font-size: 12px;"
            @click="startEditAlias(record)"
          />
        </template>
      </template>

      <template #primaryKey="{ text }">
        <Tag v-if="text" color="blue" style="margin: 0;">
          PK
        </Tag>
        <span v-else>-</span>
      </template>

      <template #sensitive="{ text }">
        <Tag v-if="text" color="orange" style="margin: 0;">
          敏
        </Tag>
        <span v-else>-</span>
      </template>
    </Table>
  </BasicDrawer>
</template>
