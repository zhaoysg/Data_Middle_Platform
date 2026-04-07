<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { GovLineageVo } from '#/api/metadata/lineage';

import { useTabbarStore } from '@vben/stores';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Descriptions, Drawer, Empty, Popconfirm, Space, Tag, message } from 'ant-design-vue';
import { onMounted, ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  datasourceList,
  lineageList,
  lineageUpstream,
  lineageDownstream,
  type DatasourceOption,
} from '#/api/metadata/lineage';

const tabbarStore = useTabbarStore();

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'srcTableName', label: '源表名', component: 'Input' },
    { fieldName: 'tgtTableName', label: '目标表名', component: 'Input' },
    {
      fieldName: 'lineageType',
      label: '血缘类型',
      component: 'Select',
      componentProps: {
        options: [
          { label: '直接血缘', value: 'DIRECT' },
          { label: '派生血缘', value: 'DERIVED' },
        ],
        allowClear: true,
      },
    },
    {
      fieldName: 'verifyStatus',
      label: '核验状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '未核验', value: 'UNVERIFIED' },
          { label: '已核验', value: 'VERIFIED' },
          { label: '无效', value: 'INVALID' },
        ],
        allowClear: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const verifyColorMap: Record<string, string> = {
  UNVERIFIED: 'default',
  VERIFIED: 'success',
  INVALID: 'error',
};
const verifyLabelMap: Record<string, string> = {
  UNVERIFIED: '未核验',
  VERIFIED: '已核验',
  INVALID: '无效',
};
const lineageTypeColorMap: Record<string, string> = {
  DIRECT: 'blue',
  DERIVED: 'purple',
};
const lineageTypeLabelMap: Record<string, string> = {
  DIRECT: '直接血缘',
  DERIVED: '派生血缘',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    { title: '源数据源', field: 'srcDsName', width: 120 },
    { title: '源表名', field: 'srcTableName', width: 180 },
    { title: '源列名', field: 'srcColumnName', width: 150, ellipsis: true },
    { title: '目标数据源', field: 'tgtDsName', width: 120 },
    { title: '目标表名', field: 'tgtTableName', width: 180 },
    { title: '目标列名', field: 'tgtColumnName', width: 150, ellipsis: true },
    {
      title: '血缘类型',
      field: 'lineageType',
      width: 100,
      slots: { default: 'lineageType' },
    },
    {
      title: '核验状态',
      field: 'verifyStatus',
      width: 90,
      slots: { default: 'verifyStatus' },
    },
    { title: '创建时间', field: 'createTime', width: 160 },
    { title: '操作', field: 'action', width: 120, fixed: 'right', slots: { default: 'action' } },
  ],
  height: 'auto',
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues = {}) => {
        return await lineageList({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: { keyField: 'id' },
  id: 'metadata-lineage-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });

// 查看血缘图谱
const lineageDrawerVisible = ref(false);
const lineageDetail = ref<{
  dsId?: number;
  dsName?: string;
  tableName?: string;
} | null>(null);

async function handleViewGraph(row: GovLineageVo) {
  // 跳转到血缘图谱页面
  const routerPath = '/metadata/lineage/graph';
  tabbarStore.closeActiveTab(routerPath);
  tabbarStore.addTab({
    title: '血缘图谱',
    path: routerPath,
    query: {
      dsId: String(row.srcDsId),
      table: row.srcTableName || '',
    },
  });
}

// 从血缘图谱打开（queryParam 方式）
async function openGraphFromQuery(dsId?: number, table?: string) {
  if (!dsId || !table) return;
  const routerPath = '/metadata/lineage/graph';
  tabbarStore.closeActiveTab(routerPath);
  tabbarStore.addTab({
    title: '血缘图谱',
    path: routerPath,
    query: { dsId: String(dsId), table },
  });
}

// 简单展示行详情
const detailDrawerVisible = ref(false);
async function handleViewDetail(row: GovLineageVo) {
  lineageDetail.value = {
    dsId: row.srcDsId,
    dsName: row.srcDsName,
    tableName: row.srcTableName,
  };
  detailDrawerVisible.value = true;
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="血缘管理">
      <template #toolbar-tools>
        <Space>
          <a-button
            type="primary"
            @click="openGraphFromQuery()"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            打开图谱
          </a-button>
        </Space>
      </template>

      <template #lineageType="{ row }">
        <Tag :color="lineageTypeColorMap[row.lineageType || 'DIRECT']">
          {{ lineageTypeLabelMap[row.lineageType || 'DIRECT'] || row.lineageType }}
        </Tag>
      </template>

      <template #verifyStatus="{ row }">
        <Tag :color="verifyColorMap[row.verifyStatus || 'UNVERIFIED']">
          {{ verifyLabelMap[row.verifyStatus || 'UNVERIFIED'] || row.verifyStatus }}
        </Tag>
      </template>

      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" @click="handleViewGraph(row)">
            图谱
          </a-button>
          <a-button type="link" size="small" @click="handleViewDetail(row)">
            详情
          </a-button>
        </Space>
      </template>
    </BasicTable>

    <!-- 血缘详情抽屉 -->
    <Drawer
      v-model:open="detailDrawerVisible"
      title="血缘关系详情"
      width="500"
      placement="right"
    >
      <Descriptions v-if="lineageDetail" :column="1" bordered size="small">
        <Descriptions.Item label="源数据源">{{ lineageDetail.dsName }}</Descriptions.Item>
        <Descriptions.Item label="源表名">{{ lineageDetail.tableName }}</Descriptions.Item>
        <Descriptions.Item label="操作">
          <Space>
            <a-button type="primary" size="small" @click="() => {
              openGraphFromQuery(lineageDetail?.dsId, lineageDetail?.tableName);
              detailDrawerVisible = false;
            }">
              查看图谱
            </a-button>
          </Space>
        </Descriptions.Item>
      </Descriptions>
    </Drawer>
  </Page>
</template>
