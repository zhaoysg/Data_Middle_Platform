<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MetadataScan, MetadataTable } from '#/api/metadata/model';

import { computed, h, reactive, ref, watch } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Checkbox, Divider, Form, Input, Modal, Popconfirm, Radio, Select, Space, Spin, Tag, message } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import { metadataScanExec } from '#/api/metadata/scan';
import {
  metadataTableBatchUpdate,
  metadataTableColumns,
  metadataTableExport,
  metadataTableGovernanceStat,
  metadataTableList,
  metadataTableRemove,
  metadataTableUpdate,
} from '#/api/metadata/table';
import { metadataDomainList, metadataDomainOptions } from '#/api/metadata/domain';
import { metadataLayerList, metadataLayerOptions } from '#/api/metadata/layer';
import { secLevelList } from '#/api/metadata/security/level';
import { datasourceEnabled, datasourceTables } from '#/api/system/datasource';
import { TableSwitch } from '#/components/table';
import { commonDownloadExcel } from '#/utils/file/download';

import {
  columns,
  layerColorMap,
  querySchema,
  sensitivityColorMap,
  sensitivityLabelMap,
} from './data';
import columnDrawer from './column-drawer.vue';
import tableDrawer from './table-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: { allowClear: true },
  },
  schema: querySchema(),
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: { highlight: true, reserve: true },
  columns,
  height: 'auto',
  keepSource: true,
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues = {}) => {
        await loadGovernanceStat();
        return await metadataTableList({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: { keyField: 'id' },
  rowClassName: ({ row }) => {
    const hasNoLayer = !row.dataLayer;
    const hasNoDomain = !row.dataDomain;
    return hasNoLayer || hasNoDomain ? 'row-governance-warning' : '';
  },
  id: 'metadata-table-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ formOptions, gridOptions });
const [TableDrawer, drawerApi] = useVbenDrawer({ connectedComponent: tableDrawer });
const [ColumnDrawer, columnDrawerApi] = useVbenDrawer({ connectedComponent: columnDrawer });

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const datasourceTableOptions = ref<{ label: string; value: string }[]>([]);
const isScanning = ref(false);
const scanModalVisible = ref(false);
const scanModeOptions = [
  { label: '整库扫描', value: 'ALL' },
  { label: '选表扫描', value: 'TABLES' },
  { label: '规则扫描', value: 'RULE' },
];
const ruleTypeOptions = [
  { label: '包含', value: 'CONTAINS' },
  { label: '前缀', value: 'PREFIX' },
  { label: '后缀', value: 'SUFFIX' },
  { label: '精确匹配', value: 'EXACT' },
  { label: '正则', value: 'REGEX' },
];
const scanForm = reactive<Partial<MetadataScan>>({
  scanMode: 'ALL',
  syncColumn: true,
  tableNames: [],
  ruleType: 'CONTAINS',
  includePattern: '',
  excludePattern: '',
  ignoreCase: true,
});

// 治理统计
const governanceStat = ref<[number, number, number]>([0, 0, 0]);

async function loadGovernanceStat() {
  try {
    const data = await metadataTableGovernanceStat();
    if (Array.isArray(data) && data.length >= 3) {
      governanceStat.value = [data[0], data[1], data[2]];
    }
  } catch {
    // ignore
  }
}

loadGovernanceStat();

const aliasModalVisible = ref(false);
const aliasModalLoading = ref(false);
const aliasRow = ref<MetadataTable>();
const aliasValue = ref('');

async function loadDatasources() {
  const data = await datasourceEnabled();
  const options = data.map((ds: any) => ({
    label: ds.dsName,
    value: ds.dsId,
  }));

  datasourceOptions.value = options;
  tableApi.formApi.updateSchema([
    {
      fieldName: 'dsId',
      componentProps: {
        options,
      },
    },
  ]);
}
loadDatasources();

function openScanModal() {
  scanForm.dsId = undefined;
  scanForm.scanMode = 'ALL';
  scanForm.syncColumn = true;
  scanForm.tableNames = [];
  scanForm.ruleType = 'CONTAINS';
  scanForm.includePattern = '';
  scanForm.excludePattern = '';
  scanForm.ignoreCase = true;
  datasourceTableOptions.value = [];
  scanModalVisible.value = true;
}

watch(
  () => scanForm.dsId,
  async (dsId) => {
    scanForm.tableNames = [];
    datasourceTableOptions.value = [];
    if (!dsId || scanForm.scanMode !== 'TABLES') {
      return;
    }
    await loadDatasourceTables(dsId);
  },
);

watch(
  () => scanForm.scanMode,
  async (scanMode) => {
    scanForm.tableNames = [];
    if (scanMode === 'TABLES' && scanForm.dsId) {
      await loadDatasourceTables(scanForm.dsId);
    }
  },
);

async function loadDatasourceTables(dsId: number) {
  const tables = await datasourceTables(dsId);
  datasourceTableOptions.value = tables.map((tableName) => ({
    label: tableName,
    value: tableName,
  }));
}

async function handleSubmitScan() {
  if (!scanForm.dsId) {
    message.warning('请选择数据源');
    return;
  }
  if (scanForm.scanMode === 'TABLES' && (!scanForm.tableNames || scanForm.tableNames.length === 0)) {
    message.warning('请至少选择一张表');
    return;
  }
  if (scanForm.scanMode === 'RULE' && !scanForm.includePattern?.trim()) {
    message.warning('请填写包含规则');
    return;
  }

  isScanning.value = true;
  try {
    const result = await metadataScanExec(scanForm.dsId, { ...scanForm });
    if (result.status === 'SUCCESS' || result.status === 'PARTIAL') {
      message.success(`扫描完成：成功 ${result.successCount}，失败 ${result.failedCount || 0}`);
      scanModalVisible.value = false;
      await tableApi.query();
      // 扫描成功后弹出引导配置弹窗
      if (result.successCount > 0) {
        const allRows = tableApi.grid.getData() || [];
        const scannedRows = (allRows as MetadataTable[]).slice(0, result.successCount);
        const ids = scannedRows.map((r) => r.id!).filter(Boolean);
        const ds = datasourceOptions.value.find((d) => d.value === scanForm.dsId);
        openGuideModal({
          dsId: scanForm.dsId,
          dsName: ds?.label || '',
          successCount: result.successCount,
          tableIds: ids,
          tableNames: scannedRows.map((r) => r.tableName).filter(Boolean),
        });
      }
    } else {
      message.error(`扫描失败：${result.errors?.join(', ') || '未知错误'}`);
    }
  } catch (error: any) {
    message.error(`扫描异常：${error?.message || '未知错误'}`);
  } finally {
    isScanning.value = false;
  }
}

async function handleViewColumns(record: MetadataTable) {
  const columnsData = await metadataTableColumns(record.id!);
  columnDrawerApi.setData({ columns: columnsData, table: record });
  columnDrawerApi.open();
}

async function handleScan(record: MetadataTable) {
  const hide = message.loading(`正在扫描表 ${record.tableName}...`, 0);
  try {
    const result = await metadataScanExec(record.dsId!, {
      scanMode: 'TABLES',
      syncColumn: true,
      tableNames: record.tableName ? [record.tableName] : [],
    });
    hide();
    if (result.status === 'SUCCESS' || result.status === 'PARTIAL') {
      message.success(`扫描完成：成功 ${result.successCount}，失败 ${result.failedCount || 0}`);
      await tableApi.query();
    } else {
      message.error(`扫描失败：${result.errors?.join(', ') || '未知错误'}`);
    }
  } catch (error: any) {
    hide();
    message.error(`扫描异常：${error?.message || '未知错误'}`);
  }
}

function handleUpdateAlias(row: MetadataTable) {
  aliasRow.value = row;
  aliasValue.value = row.tableAlias || '';
  aliasModalVisible.value = true;
}

async function handleSubmitAlias() {
  if (!aliasRow.value?.id) {
    return;
  }

  aliasModalLoading.value = true;
  try {
    await metadataTableUpdate({
      id: aliasRow.value.id,
      tableAlias: aliasValue.value.trim(),
    });
    message.success('保存成功');
    closeAliasModal();
    await tableApi.query();
  } catch (error) {
    console.error(error);
  } finally {
    aliasModalLoading.value = false;
  }
}

function closeAliasModal() {
  aliasModalVisible.value = false;
  aliasRow.value = undefined;
  aliasValue.value = '';
}

function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: MetadataTable) => row.id)
    .filter((id): id is number => id !== undefined);
  Modal.confirm({
    title: '提示',
    okType: 'danger',
    content: `确认删除选中的 ${ids.length} 张表吗？`,
    onOk: async () => {
      await metadataTableRemove(ids);
      await tableApi.query();
    },
  });
}

function handleDownloadExcel() {
  commonDownloadExcel(metadataTableExport, '元数据表', tableApi.formApi.form.values);
}

function handleEdit(record: MetadataTable) {
  drawerApi.setData({ id: record.id });
  drawerApi.open();
}

async function handleDelete(row: MetadataTable) {
  await metadataTableRemove([row.id!]);
  await tableApi.query();
}

// ==================== 扫描完成引导配置弹窗 ====================
const guideModalVisible = ref(false);
const guideModalLoading = ref(false);
const guideModalData = ref<{
  dsId?: number;
  dsName?: string;
  successCount?: number;
  tableIds?: number[];
  tableNames?: string[];
} | null>(null);

const batchForm = reactive({
  dataLayer: '',
  dataDomain: '',
  sensitivityLevel: '',
  tags: '',
});
const layerOptions = ref<{ label: string; value: string }[]>([]);
const domainOptions = ref<{ label: string; value: string }[]>([]);
const batchSensitivityOptions = ref<{ label: string; value: string }[]>([]);
const guideOptionsLoading = ref(false);

function unwrapList(raw: unknown): any[] {
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'rows' in raw && Array.isArray((raw as { rows: unknown }).rows)) {
    return (raw as { rows: any[] }).rows;
  }
  return [];
}

async function loadBatchGuideOptions() {
  const [layersRaw, domainsRaw, levelsPage] = await Promise.all([
    metadataLayerOptions().catch(() => []),
    metadataDomainOptions().catch(() => []),
    secLevelList({ pageNum: 1, pageSize: 500 }).catch(() => null),
  ]);

  let layers = unwrapList(layersRaw);
  let domains = unwrapList(domainsRaw);
  if (layers.length === 0) {
    const p = await metadataLayerList({ pageNum: 1, pageSize: 500 }).catch(() => null);
    layers = unwrapList(p);
  }
  if (domains.length === 0) {
    const p = await metadataDomainList({ pageNum: 1, pageSize: 500 }).catch(() => null);
    domains = unwrapList(p);
  }

  layerOptions.value = layers
    .map((l: any) => ({
      label: l.layerCode ? `${l.layerCode} — ${l.layerName ?? ''}` : (l.layerName ?? ''),
      value: l.layerCode,
    }))
    .filter((o) => o.value);

  domainOptions.value = domains
    .map((d: any) => ({
      label: d.domainCode ? `${d.domainCode} — ${d.domainName}` : d.domainName,
      value: d.domainName ?? d.domainCode,
    }))
    .filter((o) => o.value);

  const levelRows = levelsPage?.rows ? levelsPage.rows : unwrapList(levelsPage);
  batchSensitivityOptions.value =
    levelRows.length > 0
      ? levelRows.map((lv: any) => ({
          label: lv.levelCode ? `${lv.levelCode} — ${lv.levelName ?? ''}` : (lv.levelName ?? ''),
          value: lv.levelCode,
        }))
      : [
          { label: 'NORMAL — 普通', value: 'NORMAL' },
          { label: 'INNER — 内部', value: 'INNER' },
          { label: 'SENSITIVE — 敏感', value: 'SENSITIVE' },
          { label: 'HIGHLY_SENSITIVE — 高度敏感', value: 'HIGHLY_SENSITIVE' },
        ];
}

async function openGuideModal(data: typeof guideModalData.value) {
  guideModalData.value = data;
  batchForm.dataLayer = '';
  batchForm.dataDomain = '';
  batchForm.sensitivityLevel = '';
  batchForm.tags = '';
  guideModalVisible.value = true;
  guideOptionsLoading.value = true;
  try {
    await loadBatchGuideOptions();
    if (layerOptions.value.length === 0 && domainOptions.value.length === 0) {
      message.warning('分层与数据域列表为空，请先在「数仓分层」「数据域」中维护主数据，或检查菜单权限');
    }
  } catch {
    message.error('加载下拉选项失败，请确认已登录且拥有分层/数据域/敏感等级查询权限');
  } finally {
    guideOptionsLoading.value = false;
  }
}

async function handleSubmitBatch() {
  if (!guideModalData.value?.tableIds?.length) return;
  if (!batchForm.dataLayer && !batchForm.dataDomain && !batchForm.sensitivityLevel && !batchForm.tags) {
    message.warning('请至少填写一项配置');
    return;
  }
  guideModalLoading.value = true;
  try {
    const count = await metadataTableBatchUpdate({
      ids: guideModalData.value.tableIds,
      dataLayer: batchForm.dataLayer || undefined,
      dataDomain: batchForm.dataDomain || undefined,
      sensitivityLevel: batchForm.sensitivityLevel || undefined,
      tags: batchForm.tags || undefined,
    });
    message.success(`批量配置成功，共更新 ${count} 张表`);
    guideModalVisible.value = false;
    await tableApi.query();
  } catch {
    message.error('批量配置失败');
  } finally {
    guideModalLoading.value = false;
  }
}

function closeGuideModal() {
  guideModalVisible.value = false;
  guideModalData.value = null;
}

// 导出阻断提示
function checkBeforeExport(callback: () => void) {
  const [noLayer, noDomain, noLevel] = governanceStat.value;
  if (noLayer === 0 && noDomain === 0 && noLevel === 0) {
    callback();
    return;
  }
  Modal.confirm({
    title: '治理提醒',
    content: h('div', [
      h('p', { style: 'margin-bottom: 8px; font-size: 14px;' }, '当前存在未完善配置的表：'),
      noLayer > 0 ? h('p', { style: 'color: #fa8c16; margin: 4px 0;' }, `⚠ ${noLayer} 张表未配置分层`) : null,
      noDomain > 0 ? h('p', { style: 'color: #fa8c16; margin: 4px 0;' }, `⚠ ${noDomain} 张表未配置数据域`) : null,
      noLevel > 0 ? h('p', { style: 'color: #fa8c16; margin: 4px 0;' }, `⚠ ${noLevel} 张表未配置敏感等级`) : null,
      h('p', { style: 'margin-top: 12px; color: #8c8c8c; font-size: 12px;' }, '建议先完善配置后再导出。继续导出吗？'),
    ]),
    okText: '继续导出',
    cancelText: '先去配置',
    onOk: () => callback(),
  });
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="元数据表列表">
      <template #toolbar-tools>
        <Space>
          <a-button type="primary" @click="openScanModal">
            扫描数据源
          </a-button>

          <a-button
            v-access:code="['metadata:table:export']"
            @click="checkBeforeExport(handleDownloadExcel)"
          >
            导出
          </a-button>

          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            v-access:code="['metadata:table:remove']"
            @click="handleMultiDelete"
          >
            批量删除
          </a-button>
        </Space>
      </template>

      <!-- 治理提醒横幅 -->
      <template v-if="governanceStat[0] > 0 || governanceStat[1] > 0 || governanceStat[2] > 0" #header-after-toolbar>
        <div class="governance-warning-bar">
          <span style="font-weight: 600; color: #fa8c16;">治理提醒：</span>
          <span v-if="governanceStat[0] > 0" class="governance-item">
            <Tag color="orange">{{ governanceStat[0] }} 张表</Tag> 未配置分层
          </span>
          <span v-if="governanceStat[1] > 0" class="governance-item">
            <Tag color="orange">{{ governanceStat[1] }} 张表</Tag> 未配置数据域
          </span>
          <span v-if="governanceStat[2] > 0" class="governance-item">
            <Tag color="orange">{{ governanceStat[2] }} 张表</Tag> 未配置敏感等级
          </span>
          <span style="color: #8c8c8c; font-size: 12px; margin-left: auto;">
            建议完善配置后再进行导出等操作
          </span>
        </div>
      </template>

      <template #dataLayer="{ row }">
        <Tag :color="layerColorMap[row.dataLayer ?? ''] || '#1890ff'" style="margin: 0;">
          {{ row.dataLayer || '-' }}
        </Tag>
      </template>

      <template #sensitivityLevel="{ row }">
        <Tag :color="sensitivityColorMap[row.sensitivityLevel ?? ''] || 'default'" style="margin: 0;">
          {{ sensitivityLabelMap[row.sensitivityLevel ?? ''] || '-' }}
        </Tag>
      </template>

      <template #tableAlias="{ row }">
        <span v-if="row.tableAlias">{{ row.tableAlias }}</span>
        <span v-else style="color: #bbb; font-style: italic;">-</span>
      </template>

      <template #status="{ row }">
        <TableSwitch
          v-model:value="row.status"
          checked-text="活跃"
          un-checked-text="归档"
          :checked-value="'ACTIVE'"
          :un-checked-value="'ARCHIVED'"
          :api="() => metadataTableUpdate({ id: row.id, status: row.status })"
          v-access:code="['metadata:table:edit']"
          @reload="tableApi.query()"
        />
      </template>

      <template #action="{ row }">
        <Space>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:table:query']"
            @click="handleViewColumns(row)"
          >
            字段
          </a-button>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:scan:exec']"
            @click="handleScan(row)"
          >
            扫描
          </a-button>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:table:edit']"
            @click="handleEdit(row)"
          >
            编辑
          </a-button>
          <a-button
            type="link"
            size="small"
            v-access:code="['metadata:table:edit']"
            @click="handleUpdateAlias(row)"
          >
            别名
          </a-button>
          <Popconfirm
            placement="left"
            title="确认删除？删除后将无法恢复！"
            @confirm="handleDelete(row)"
          >
            <a-button
              type="link"
              size="small"
              danger
              v-access:code="['metadata:table:remove']"
            >
              删除
            </a-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>

    <TableDrawer @reload="tableApi.query()" />
    <ColumnDrawer />

    <Modal
      v-model:open="scanModalVisible"
      title="扫描数据源"
      :confirm-loading="isScanning"
      @ok="handleSubmitScan"
    >
        <Form layout="vertical">
        <Form.Item label="选择数据源" required>
          <Select
            v-model:value="scanForm.dsId"
            :options="datasourceOptions"
            placeholder="请选择要扫描的数据源"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="扫描方式" required>
          <Radio.Group
            v-model:value="scanForm.scanMode"
            :options="scanModeOptions"
            option-type="button"
            button-style="solid"
          />
        </Form.Item>
        <Form.Item v-if="scanForm.scanMode === 'TABLES'" label="选择表" required>
          <Select
            v-model:value="scanForm.tableNames"
            mode="multiple"
            :options="datasourceTableOptions"
            :max-tag-count="4"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="请选择要扫描的表"
            style="width: 100%"
          />
        </Form.Item>
        <template v-if="scanForm.scanMode === 'RULE'">
          <Form.Item label="规则类型" required>
            <Select
              v-model:value="scanForm.ruleType"
              :options="ruleTypeOptions"
              placeholder="请选择规则类型"
            />
          </Form.Item>
          <Form.Item label="包含规则" required>
            <Input.TextArea
              v-model:value="scanForm.includePattern"
              :rows="2"
              placeholder="支持多个规则，逗号或换行分隔"
            />
          </Form.Item>
          <Form.Item label="排除规则">
            <Input.TextArea
              v-model:value="scanForm.excludePattern"
              :rows="2"
              placeholder="可选，支持多个规则，逗号或换行分隔"
            />
          </Form.Item>
          <Form.Item>
            <Checkbox v-model:checked="scanForm.ignoreCase">
              忽略大小写
            </Checkbox>
          </Form.Item>
        </template>
        <Form.Item>
          <Checkbox v-model:checked="scanForm.syncColumn">
            同步字段信息
          </Checkbox>
        </Form.Item>
        <Form.Item label="说明">
          <p style="color: #999; font-size: 12px; margin-bottom: 0;">
            支持整库扫描、选表扫描和规则扫描。规则扫描适合按表名前缀、后缀、关键字或正则批量筛选。
          </p>
        </Form.Item>
      </Form>
    </Modal>

    <Modal
      v-model:open="aliasModalVisible"
      title="编辑表别名"
      :confirm-loading="aliasModalLoading"
      @cancel="closeAliasModal"
      @ok="handleSubmitAlias"
    >
      <Form layout="vertical">
        <Form.Item label="表别名">
          <Input
            v-model:value="aliasValue"
            :maxlength="128"
            placeholder="请输入表别名"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 扫描完成引导批量配置弹窗 -->
    <Modal
      v-model:open="guideModalVisible"
      title="配置表属性"
      :width="560"
      :confirm-loading="guideModalLoading"
      @cancel="closeGuideModal"
      @ok="handleSubmitBatch"
    >
      <div v-if="guideModalData" class="guide-modal-content">
        <div class="guide-info-banner">
          <span style="font-weight: 600;">本次扫描新增了 {{ guideModalData.successCount }} 张表</span>
          <span style="color: #8c8c8c; font-size: 13px;">
            （数据源：{{ guideModalData.dsName }}）
          </span>
        </div>
        <div v-if="guideModalData.tableNames?.length" class="guide-table-list">
          <Tag v-for="name in (guideModalData.tableNames as string[]).slice(0, 20)" :key="name" size="small">
            {{ name }}
          </Tag>
          <span v-if="(guideModalData.tableNames?.length || 0) > 20" style="color: #8c8c8c; font-size: 12px;">
            ...共 {{ guideModalData.tableNames?.length }} 张
          </span>
        </div>
        <Divider style="margin: 12px 0;" />
        <p class="mb-4 text-sm font-semibold text-slate-700">
          批量配置表属性（至少填写一项）
        </p>
        <Spin :spinning="guideOptionsLoading">
          <Form layout="vertical" :model="batchForm" class="batch-guide-form">
            <Form.Item
              label="数仓分层"
              name="dataLayer"
              extra="选项来自「数仓分层 /options 或列表」接口"
            >
              <Select
                v-model:value="batchForm.dataLayer"
                :options="layerOptions"
                placeholder="请选择分层，留空则不更新"
                allow-clear
                show-search
                option-filter-prop="label"
              />
            </Form.Item>
            <Form.Item
              label="数据域"
              name="dataDomain"
              extra="选项来自「数据域 /options 或列表」接口"
            >
              <Select
                v-model:value="batchForm.dataDomain"
                :options="domainOptions"
                placeholder="请选择数据域，留空则不更新"
                allow-clear
                show-search
                option-filter-prop="label"
              />
            </Form.Item>
            <Form.Item
              label="敏感等级"
              name="sensitivityLevel"
              extra="选项来自「敏感等级」列表，与表元数据字段一致"
            >
              <Select
                v-model:value="batchForm.sensitivityLevel"
                :options="batchSensitivityOptions"
                placeholder="请选择敏感等级，留空则不更新"
                allow-clear
                show-search
                option-filter-prop="label"
              />
            </Form.Item>
            <Form.Item label="标签" name="tags" extra="多个标签用英文逗号分隔">
              <Input
                v-model:value="batchForm.tags"
                placeholder="多个标签用逗号分隔，留空则不更新"
              />
            </Form.Item>
          </Form>
        </Spin>
        <p style="color: #999; font-size: 12px; margin-top: 8px;">
          提示：可以只配置部分属性，未填写的字段将保持原值
        </p>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.governance-warning-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 13px;
}
.governance-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>

<!-- 未配置域/分层的行高亮 -->
<style>
.vxe-table .row-governance-warning td {
  background-color: #fffbe6 !important;
}
</style>