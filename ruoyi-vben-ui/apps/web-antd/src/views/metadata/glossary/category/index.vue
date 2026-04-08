<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag, Tree } from 'ant-design-vue';
import { onMounted, ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  glossaryCategoryList,
  glossaryCategoryAdd,
  glossaryCategoryRemove,
  glossaryCategoryTree,
  glossaryCategoryUpdate,
} from '#/api/metadata/glossary';

import categoryDrawer from './category-drawer.vue';

const treeData = ref<any[]>([]);
const selectedId = ref<number | null>(null);

async function loadTree() {
  const data = await glossaryCategoryTree();
  treeData.value = buildTree(data || []);
}

function buildTree(list: any[]): any[] {
  const map: Record<string, any> = {};
  const roots: any[] = [];
  for (const item of list) {
    map[item.id] = { ...item, key: item.id, title: item.categoryName, children: [] };
  }
  for (const item of list) {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id]);
    } else {
      roots.push(map[item.id]);
    }
  }
  return roots;
}

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'categoryName', label: '分类名称', component: 'Input' },
    { fieldName: 'categoryCode', label: '分类编码', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50 },
    { title: '分类名称', field: 'categoryName', width: 200 },
    { title: '分类编码', field: 'categoryCode', width: 150 },
    { title: '上级分类', field: 'parentName', width: 150 },
    { title: '排序', field: 'sortOrder', width: 80 },
    { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
    { title: '创建时间', field: 'createTime', width: 180 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page, form }: any) => {
        const data = await glossaryCategoryList({ ...form, pageNum: page?.currentPage, pageSize: page?.pageSize });
        return data || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'glossary-category-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [CategoryDrawer, drawerApi] = useVbenDrawer({ connectedComponent: categoryDrawer });

function handleAdd() {
  drawerApi.setData({ parentId: selectedId.value });
  drawerApi.open();
}

function handleEdit(record: any) {
  drawerApi.setData({ ...record });
  drawerApi.open();
}

async function handleDelete(row: any) {
  await glossaryCategoryRemove([row.id!]);
  await tableApi.query();
  await loadTree();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows.map((r: any) => r.id).filter((id: any): id is number => id !== undefined);
  await glossaryCategoryRemove(ids);
  await tableApi.query();
  await loadTree();
}

onMounted(() => {
  loadTree();
});
</script>

<template>
  <Page :auto-content-height="true">
    <div class="flex gap-4">
      <!-- 左侧：分类树 -->
      <div class="w-60 flex-shrink-0 border border-solid border-gray-200 rounded p-2">
        <div class="text-sm font-medium text-gray-600 mb-2">术语分类</div>
        <Tree
          :tree-data="treeData"
          :selected-keys="selectedId ? [selectedId] : []"
          :show-icon="true"
          @select="(keys: any) => { selectedId = keys[0] ? Number(keys[0]) : null; tableApi.query(); }"
        >
          <template #title="{ title }">
            <span>{{ title }}</span>
          </template>
        </Tree>
      </div>

      <!-- 右侧：分类列表 -->
      <div class="flex-1 min-w-0">
        <BasicTable table-title="术语分类">
          <template #toolbar-tools>
            <Space>
              <a-button type="primary" @click="handleAdd">
                <template #icon>
                  <PlusOutlined />
                </template>
                新增分类
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
            <Tag :color="row.status === '0' ? 'success' : 'default'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </Tag>
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
      </div>
    </div>
    <CategoryDrawer @reload="() => { tableApi.query(); loadTree(); }" />
  </Page>
</template>
