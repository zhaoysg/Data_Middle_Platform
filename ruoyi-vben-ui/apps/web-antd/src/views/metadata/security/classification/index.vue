<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecClassification } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';
import { ref } from 'vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secClassificationList,
  secClassificationRemove,
  secClassificationUpdate,
} from '#/api/metadata/security/classification';
import { TableSwitch } from '#/components/table';

import classificationDrawer from './classification-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: [
    { fieldName: 'className', label: '分类名称', component: 'Input' },
    { fieldName: 'classCode', label: '分类编码', component: 'Input' },
  ],
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    {
      title: '分类名称',
      field: 'className',
      width: 220,
      fixed: 'left',
      align: 'left',
      // 全局 grid.showOverflow=true 会按单行处理单元格，与双行插槽叠加易导致行高异常
      showOverflow: false,
      slots: { default: 'classNameCell' },
    },
    { title: '分类编码', field: 'classCode', width: 150 },
    // 勿仅用 minWidth：宽屏下 VXE 会把剩余宽度全塞给该列，左右列像被挤没、描述列空一大块
    {
      title: '分类描述',
      field: 'classDesc',
      width: 280,
      minWidth: 200,
      showOverflow: 'tooltip',
    },
    {
      title: '关联等级',
      field: 'defaultLevelName',
      width: 120,
      showOverflow: false,
      slots: { default: 'assocLevel' },
    },
    {
      title: '规则数',
      field: 'ruleCount',
      width: 88,
      align: 'center',
      slots: { default: 'ruleCountCell' },
    },
    {
      title: '字段数',
      field: 'fieldCount',
      width: 88,
      align: 'center',
      slots: { default: 'fieldCountCell' },
    },
    {
      title: '状态',
      field: 'enabled',
      width: 80,
      showOverflow: false,
      slots: { default: 'enabled' },
    },
    { title: '创建时间', field: 'createTime', width: 170 },
    {
      title: '操作',
      field: 'action',
      width: 130,
      fixed: 'right',
      showOverflow: false,
      slots: { default: 'action' },
    },
  ],
  // 与敏感等级页一致：auto 高度由内容撑开，避免与自定义页头/统计区叠加 fixed 高度导致行被压扁
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page, form }: any) => {
        const params = {
          ...form,
          pageNum: page?.currentPage,
          pageSize: page?.pageSize,
        };
        return await secClassificationList(params) || { rows: [], total: 0 };
      },
    },
  },
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'sec-classification-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [ClassificationDrawer, drawerApi] = useVbenDrawer({ connectedComponent: classificationDrawer });

function handleAdd() {
  drawerApi.setData({});
  drawerApi.open();
}

function handleEdit(record: SecClassification) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: SecClassification) {
  await secClassificationRemove([row.id!]);
  await tableApi.query();
}

async function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: SecClassification) => row.id)
    .filter((id): id is number => id !== undefined);
  await secClassificationRemove(ids);
  await tableApi.query();
}

/* ---- 辅助：零值显示横杠 ---- */
function fmt(n: number | undefined) {
  return n != null && n > 0 ? String(n) : '—';
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="数据分类管理">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
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

      <!-- 分类名称单元格（名称 + 编码） -->
      <template #classNameCell="{ row }">
        <div class="clsf-name-cell">
          <span class="clsf-name-text">{{ row.className ?? '—' }}</span>
          <span class="clsf-name-code">{{ row.classCode ?? '' }}</span>
        </div>
      </template>

      <!-- 关联等级 -->
      <template #assocLevel="{ row }">
        <Tag
          v-if="row.defaultLevelName"
          class="clsf-level-tag"
          :style="{
            color: row.defaultLevelColor || '#722ED1',
            borderColor: row.defaultLevelColor || '#722ED1',
            background: 'transparent',
          }"
        >
          {{ row.defaultLevelName }}
        </Tag>
        <span v-else class="clsf-dash">—</span>
      </template>

      <!-- 规则数列（数字高亮） -->
      <template #ruleCountCell="{ row }">
        <span :class="['clsf-num', (row.ruleCount ?? 0) > 0 ? 'clsf-num--active' : '']">
          {{ fmt(row.ruleCount) }}
        </span>
      </template>

      <!-- 字段数列（数字高亮） -->
      <template #fieldCountCell="{ row }">
        <span :class="['clsf-num', (row.fieldCount ?? 0) > 0 ? 'clsf-num--active' : '']">
          {{ fmt(row.fieldCount) }}
        </span>
      </template>

      <!-- 启用状态 -->
      <template #enabled="{ row }">
          <TableSwitch
            v-model:value="row.enabled"
            checked-value="1"
            un-checked-value="0"
            :api="() => secClassificationUpdate({ id: row.id, enabled: row.enabled === '1' ? '0' : '1' })"
            @reload="tableApi.query()"
          />
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a-button type="link" size="small" class="clsf-action-edit" @click="handleEdit(row)">编辑</a-button>
          <Popconfirm title="确认删除该分类？" @confirm="handleDelete(row)">
            <a-button type="link" size="small" danger class="clsf-action-del">删除</a-button>
          </Popconfirm>
        </Space>
      </template>

      <!-- 空状态 -->
      <template #empty>
        <div class="clsf-empty">
          <div class="clsf-empty-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="22" stroke="#E8E8E8" stroke-width="2" />
              <path d="M24 14L14 18.5V25.5L24 30L34 25.5V18.5L24 14Z" stroke="#D9D9D9" stroke-width="1.5" stroke-linejoin="round" />
              <path d="M14 30L24 34.5L34 30" stroke="#D9D9D9" stroke-width="1.5" stroke-linejoin="round" />
              <path d="M14 24L24 28.5L34 24" stroke="#D9D9D9" stroke-width="1.5" stroke-linejoin="round" />
            </svg>
          </div>
          <p class="clsf-empty-title">暂无数据分类</p>
          <p class="clsf-empty-desc">建立数据分类体系，为敏感字段识别提供分类依据</p>
          <a-button type="primary" class="clsf-empty-btn" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增分类
          </a-button>
        </div>
      </template>
    </BasicTable>

    <ClassificationDrawer @reload="tableApi.query()" />
  </Page>
</template>

<style scoped>
/* ---- 页面标题 ---- */
.vxe-grid--full-wrapper {
  margin-top: 0;
}

/* 表格行间距 */
:deep(.vxe-body--row .vxe-cell) {
  padding-top: 8px;
  padding-bottom: 8px;
  vertical-align: middle;
}
:deep(.vxe-body--row) {
  min-height: 36px;
}

/* ---- 分类名称单元格 ---- */
.clsf-name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.clsf-name-text {
  font-weight: 500;
  color: #1a1a2e;
  line-height: 1.4;
}
.clsf-name-code {
  font-size: 12px;
  color: #8c8ca1;
  font-family: 'JetBrains Mono', 'Cascadia Code', monospace;
  letter-spacing: 0;
}

/* ---- 等级 Tag ---- */
.clsf-level-tag {
  border-width: 1px !important;
  border-style: solid !important;
  border-radius: 4px !important;
  font-size: 12px !important;
  padding: 1px 8px !important;
  font-weight: 500 !important;
  line-height: 1.6 !important;
  margin: 0 !important;
}

/* ---- 数字高亮 ---- */
.clsf-num {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  font-size: 13px;
  color: #bfbfbf;
  letter-spacing: -0.01em;
}
.clsf-num--active {
  color: #1677FF;
}

/* ---- 仪表横杠占位 ---- */
.clsf-dash {
  color: #d9d9d9;
  font-size: 13px;
}

/* ---- 操作按钮 ---- */
.clsf-action-edit {
  padding: 0 4px !important;
  height: auto !important;
  font-size: 13px !important;
}
.clsf-action-del {
  padding: 0 4px !important;
  height: auto !important;
  font-size: 13px !important;
}

/* ---- 空状态 ---- */
.clsf-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0 40px;
  gap: 8px;
}
.clsf-empty-icon {
  margin-bottom: 8px;
  opacity: 0.7;
}
.clsf-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}
.clsf-empty-desc {
  font-size: 13px;
  color: #8c8ca1;
  margin: 0;
}
.clsf-empty-btn {
  margin-top: 8px;
}
</style>
