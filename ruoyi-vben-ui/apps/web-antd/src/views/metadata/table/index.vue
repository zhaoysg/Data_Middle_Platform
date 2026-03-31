<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MetadataScan, MetadataTable } from '#/api/metadata/model';

import { reactive, ref, watch } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Checkbox, Form, Input, Modal, Popconfirm, Radio, Select, Space, Tag, message } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import { metadataScanExec } from '#/api/metadata/scan';
import {
  metadataTableColumns,
  metadataTableExport,
  metadataTableList,
  metadataTableRemove,
  metadataTableUpdate,
} from '#/api/metadata/table';
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
        return await metadataTableList({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: { keyField: 'id' },
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
            @click="handleDownloadExcel"
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
  </Page>
</template>
