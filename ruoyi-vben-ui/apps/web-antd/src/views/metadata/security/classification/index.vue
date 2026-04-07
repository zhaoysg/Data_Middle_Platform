<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SecClassification } from '#/api/metadata/model';

import { PlusOutlined } from '@ant-design/icons-vue';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { Popconfirm, Space, Tag } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  secClassificationAdd,
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

/* ---- 统计数据（从列表响应中聚合） ---- */
const statsData = ref({ total: 0, enabled: 0, rules: 0, fields: 0 });
const statsLoading = ref(false);

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns: [
    { type: 'checkbox', width: 50, fixed: 'left' },
    {
      title: '分类名称',
      field: 'className',
      width: 190,
      fixed: 'left',
      slots: { default: 'classNameCell' },
    },
    { title: '分类编码', field: 'classCode', width: 150 },
    { title: '分类描述', field: 'classDesc', minWidth: 200, showOverflow: true },
    {
      title: '关联等级',
      field: 'defaultLevelName',
      width: 120,
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
    { title: '状态', field: 'enabled', width: 80, slots: { default: 'enabled' } },
    { title: '创建时间', field: 'createTime', width: 170 },
    { title: '操作', field: 'action', width: 130, fixed: 'right', slots: { default: 'action' } },
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
        statsLoading.value = true;
        try {
          const data = await secClassificationList(params);
          // 同步聚合统计数据（与后端 enrichClassificationRows 结果一致）
          const rows = (data?.rows ?? []) as SecClassification[];
          statsData.value = {
            total: data?.total ?? rows.length,
            enabled: rows.filter((r) => r.enabled === '1').length,
            rules: rows.reduce((s, r) => s + (r.ruleCount ?? 0), 0),
            fields: rows.reduce((s, r) => s + (r.fieldCount ?? 0), 0),
          };
          return data || { rows: [], total: 0 };
        } finally {
          statsLoading.value = false;
        }
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
  <Page :auto-content-height="true" class="clsf-page">
    <!-- ========== 页面标题区 ========== -->
    <div class="clsf-header">
      <div class="clsf-header-left">
        <div class="clsf-header-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#hdrGrad)" />
            <path d="M12 3L3 7.5V12L12 16.5L21 12V7.5L12 3Z" stroke="white" stroke-width="1.5" stroke-linejoin="round" />
            <path d="M12 10.5V16.5" stroke="white" stroke-width="1.5" stroke-linecap="round" />
            <defs>
              <linearGradient id="hdrGrad" x1="0" y1="0" x2="24" y2="24" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#722ED1" />
                <stop offset="100%" stop-color="#9254DE" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="clsf-title">数据分类</h1>
          <p class="clsf-subtitle">构建企业数据分类体系，为敏感字段识别提供分类依据</p>
        </div>
      </div>
      <div class="clsf-header-right">
        <a-button type="primary" size="large" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增分类
        </a-button>
      </div>
    </div>

    <!-- ========== 统计卡片行 ========== -->
    <div class="clsf-stats">
      <div class="clsf-stat-card clsf-stat-card--purple">
        <div class="clsf-stat-value">
          <a-skeleton :active="true" :paragraph="false" :title="{ width: '48px' }" v-if="statsLoading" />
          <span v-else>{{ statsData.total }}</span>
        </div>
        <div class="clsf-stat-label">分类总数</div>
      </div>
      <div class="clsf-stat-card clsf-stat-card--green">
        <div class="clsf-stat-value">
          <a-skeleton :active="true" :paragraph="false" :title="{ width: '48px' }" v-if="statsLoading" />
          <span v-else>{{ statsData.enabled }}</span>
        </div>
        <div class="clsf-stat-label">启用分类</div>
      </div>
      <div class="clsf-stat-card clsf-stat-card--blue">
        <div class="clsf-stat-value">
          <a-skeleton :active="true" :paragraph="false" :title="{ width: '48px' }" v-if="statsLoading" />
          <span v-else>{{ statsData.rules }}</span>
        </div>
        <div class="clsf-stat-label">关联规则总数</div>
      </div>
      <div class="clsf-stat-card clsf-stat-card--orange">
        <div class="clsf-stat-value">
          <a-skeleton :active="true" :paragraph="false" :title="{ width: '48px' }" v-if="statsLoading" />
          <span v-else>{{ statsData.fields }}</span>
        </div>
        <div class="clsf-stat-label">敏感字段总数</div>
      </div>
    </div>

    <!-- ========== 表格卡片 ========== -->
    <div class="clsf-table-card">
      <BasicTable>
        <template #toolbar-tools>
          <Space>
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
            :api="() => secClassificationUpdate({ id: row.id, enabled: row.enabled === '0' ? '1' : '0' })"
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
    </div>

    <ClassificationDrawer @reload="tableApi.query()" />
  </Page>
</template>

<style scoped>
/* ================================================================
   数据分类页面 — 设计系统
   设计方向：SaaS Data Platform — 紫蓝渐变主色，白底卡片，清晰层次
   ================================================================ */

/* ---- 页面容器 ---- */
.clsf-page {
  padding: 0 0 24px;
}

/* ---- 页面标题区 ---- */
.clsf-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f0ff 100%);
  border-bottom: 1px solid #f0e8ff;
}
.clsf-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.clsf-header-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #722ED1 0%, #9254DE 100%);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(114, 46, 209, 0.3);
}
.clsf-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  line-height: 1.4;
  letter-spacing: -0.01em;
}
.clsf-subtitle {
  font-size: 13px;
  color: #8c8ca1;
  margin: 2px 0 0;
  line-height: 1.5;
}

/* ---- 统计卡片行 ---- */
.clsf-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px 24px 0;
}
@media (max-width: 768px) {
  .clsf-stats { grid-template-columns: repeat(2, 1fr); }
}
.clsf-stat-card {
  padding: 16px 18px 14px;
  border-radius: 10px;
  border: 1px solid transparent;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.clsf-stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.06;
}
.clsf-stat-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}
.clsf-stat-card--purple { background: linear-gradient(135deg, #f5f0ff 0%, #ede4ff 100%); border-color: #d9b8ff; }
.clsf-stat-card--purple::before { background: #722ED1; }
.clsf-stat-card--green  { background: linear-gradient(135deg, #f0fff4 0%, #d9f5e0 100%); border-color: #b7eb8f; }
.clsf-stat-card--green::before  { background: #52C41A; }
.clsf-stat-card--blue   { background: linear-gradient(135deg, #f0f7ff 0%, #d6ecff 100%); border-color: #91caff; }
.clsf-stat-card--blue::before   { background: #1677FF; }
.clsf-stat-card--orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7b0 100%); border-color: #ffd591; }
.clsf-stat-card--orange::before { background: #FA8C16; }

.clsf-stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
  margin-bottom: 4px;
  letter-spacing: -0.02em;
}
.clsf-stat-label {
  font-size: 12px;
  color: #8c8ca1;
  font-weight: 500;
  letter-spacing: 0.01em;
}

/* ---- 表格卡片容器 ---- */
.clsf-table-card {
  padding: 16px 24px 0;
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
