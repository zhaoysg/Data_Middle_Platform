<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag, Tree } from 'ant-design-vue';
import { ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  glossaryCategoryOptions,
  glossaryCategoryTree,
  glossaryTermAdd,
  glossaryTermList,
  glossaryTermPublish,
  glossaryTermRemove,
  glossaryTermUpdate,
} from '#/api/metadata/glossary';
import { TableSwitch } from '#/components/table';

import termDrawer from './term-drawer.vue';

const selectedCategoryId = ref<number | null>(null);
const categoryTree = ref<any[]>([]);
const expandedKeys = ref<(string | number)[]>([]);

// 加载分类树
async function loadCategoryTree() {
  const data = await glossaryCategoryTree();
  categoryTree.value = data || [];
}

// 分类树选择
function onCategorySelect(selectedKeys: (string | number)[]) {
  selectedCategoryId.value = selectedKeys[0] ? Number(selectedKeys[0]) : null;
  tableApi.query();
}

// 术语状态映射
const statusLabelMap: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
};
const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  PUBLISHED: 'success',
};

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'termName', label: '术语名称', component: 'Input' },
    { fieldName: 'keyword', label: '关键词', component: 'Input' },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '草稿', value: 'DRAFT' },
          { label: '已发布', value: 'PUBLISHED' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-3 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '术语名称', field: 'termName', width: 180, fixed: 'left' },
    { title: '术语别名', field: 'termAlias', width: 150 },
    { title: '分类', field: 'categoryName', width: 120 },
    { title: '关键词', field: 'keyword', width: 150 },
    { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
    { title: '创建时间', field: 'createTime', width: 180 },
    { title: '操作', field: 'action', width: 200, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page, form }: any) => {
        const params: any = {
          ...form,
          pageNum: page?.currentPage,
          pageSize: page?.pageSize,
        };
        if (selectedCategoryId.value) {
          params.categoryId = selectedCategoryId.value;
        }
        const data = await glossaryTermList(params);
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'glossary-term-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [TermDrawer, drawerApi] = useVbenDrawer({ connectedComponent: termDrawer });

function handleAdd() {
  drawerApi.setData({ categoryId: selectedCategoryId.value });
  drawerApi.open();
}

function handleEdit(record: any) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handlePublish(row: any) {
  await glossaryTermPublish(row.id!);
  await tableApi.query();
}

async function handleDelete(row: any) {
  await glossaryTermRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows.map((r: any) => r.id).filter((id: any): id is number => id !== undefined);
  await glossaryTermRemove(ids);
  await tableApi.query();
}

onMounted(() => {
  loadCategoryTree();
});
</script>

<template>
  <Page :auto-content-height="true">
    <div class="flex gap-4">
      <!-- 左侧：分类树 -->
      <div class="w-60 flex-shrink-0 border border-solid border-gray-200 rounded p-2">
        <div class="text-sm font-medium text-gray-600 mb-2">术语分类</div>
        <Tree
          :tree-data="categoryTree"
          :selected-keys="selectedCategoryId ? [selectedCategoryId] : []"
          :show-icon="true"
          @select="onCategorySelect"
        >
          <template #title="{ key, title }">
            <span>{{ title }}</span>
          </template>
        </Tree>
      </div>

      <!-- 右侧：术语列表 -->
      <div class="flex-1 min-w-0">
        <BasicTable table-title="业务术语">
          <template #toolbar-tools>
            <Space>
              <a-button type="primary" @click="handleAdd">
                <template #icon>
                  <PlusOutlined />
                </template>
                新增术语
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
          <template #status="{ row }">
            <Tag :color="statusColorMap[row.status || 'DRAFT']">
              {{ statusLabelMap[row.status || 'DRAFT'] || row.status }}
            </Tag>
          </template>
          <template #action="{ row }">
            <Space>
              <a-button type="link" size="small" @click="handleEdit(row)">编辑</a-button>
              <a-button
                v-if="row.status !== 'PUBLISHED'"
                type="link"
                size="small"
                @click="handlePublish(row)"
              >
                发布
              </a-button>
              <Popconfirm title="确认删除？" @confirm="handleDelete(row)">
                <a-button type="link" size="small" danger>删除</a-button>
              </Popconfirm>
            </Space>
          </template>
        </BasicTable>
      </div>
    </div>
    <TermDrawer @reload="tableApi.query()" />
  </Page>
</template>
