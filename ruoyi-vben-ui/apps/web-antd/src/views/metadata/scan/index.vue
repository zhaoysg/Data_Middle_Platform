<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Form, Modal, Select, Space, Tag, message } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { metadataScanExec, metadataScanLogs } from '#/api/metadata/scan';
import { datasourceEnabled } from '#/api/system/datasource';

const gridOptions: VxeGridProps = {
  columns: [
    { title: '数据源', field: 'dsName', width: 150 },
    {
      title: '扫描类型',
      field: 'scanType',
      width: 110,
      slots: { default: 'scanType' },
    },
    { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
    { title: '总表数', field: 'totalTables', width: 80, align: 'right' as const },
    { title: '成功', field: 'successCount', width: 80, align: 'right' as const },
    { title: '部分成功', field: 'partialCount', width: 90, align: 'right' as const },
    { title: '失败', field: 'failedCount', width: 80, align: 'right' as const },
    {
      title: '耗时',
      field: 'elapsedMs',
      width: 100,
      align: 'right' as const,
      slots: { default: 'elapsedMs' },
    },
    {
      title: '开始时间',
      field: 'startTime',
      width: 170,
      formatter: ({ cellValue }: { cellValue?: string | null }) => {
        return cellValue
          ? String(cellValue).substring(0, 19).replace('T', ' ')
          : '-';
      },
    },
    {
      title: '结束时间',
      field: 'endTime',
      width: 170,
      formatter: ({ cellValue }: { cellValue?: string | null }) => {
        return cellValue
          ? String(cellValue).substring(0, 19).replace('T', ' ')
          : '-';
      },
    },
    { title: '扫描人', field: 'scanUserName', width: 100 },
    {
      title: '错误详情',
      field: 'errorDetail',
      minWidth: 200,
      showOverflow: true,
      slots: { default: 'errorDetail' },
    },
  ],
  height: 'auto',
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async () => {
        return await metadataScanLogs({});
      },
    },
  },
  rowConfig: { keyField: 'id' },
  id: 'metadata-scan-index',
};

const statusMap: Record<string, { color: string; text: string }> = {
  RUNNING: { color: 'blue', text: '扫描中' },
  SUCCESS: { color: 'green', text: '成功' },
  FAILED: { color: 'red', text: '失败' },
  PARTIAL: { color: 'orange', text: '部分成功' },
  CANCELLED: { color: 'default', text: '已取消' },
};

const [BasicTable, tableApi] = useVbenVxeGrid({ gridOptions });

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const selectedDsId = ref<number>();
const isScanning = ref(false);
const scanModalVisible = ref(false);

async function loadDatasources() {
  const data = await datasourceEnabled();
  datasourceOptions.value = data.map((ds: any) => ({
    label: ds.dsName,
    value: ds.dsId,
  }));
}

async function handleOpenScan() {
  selectedDsId.value = undefined;
  await loadDatasources();
  scanModalVisible.value = true;
}

async function handleSubmitScan() {
  if (!selectedDsId.value) {
    message.warning('请选择数据源');
    return;
  }

  isScanning.value = true;
  try {
    const result = await metadataScanExec(selectedDsId.value, { syncColumn: true });
    scanModalVisible.value = false;

    if (result.status === 'SUCCESS' || result.status === 'PARTIAL') {
      message.success(
        `扫描完成：成功 ${result.successCount}，失败 ${result.failedCount || 0}，耗时 ${result.elapsedMs}ms`,
      );
    } else {
      message.error(`扫描失败：${result.errors?.join('；') || '未知错误'}`);
    }

    await tableApi.query();
  } catch (error: any) {
    message.error(`扫描异常：${error?.message || '未知错误'}`);
  } finally {
    isScanning.value = false;
  }
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="扫描记录">
      <template #toolbar-tools>
        <Space>
          <a-button
            type="primary"
            v-access:code="['metadata:scan:exec']"
            @click="handleOpenScan"
          >
            扫描数据源
          </a-button>
        </Space>
      </template>

      <template #scanType="{ row }">
        <Tag :color="row.scanType === 'FULL' ? 'blue' : 'default'">
          {{ row.scanType === 'FULL' ? '全量扫描' : '仅表信息' }}
        </Tag>
      </template>

      <template #status="{ row }">
        <Tag :color="statusMap[row.status!]?.color || 'default'">
          {{ statusMap[row.status!]?.text || row.status }}
        </Tag>
      </template>

      <template #elapsedMs="{ row }">
        {{ row.elapsedMs ? `${row.elapsedMs}ms` : '-' }}
      </template>

      <template #errorDetail="{ row }">
        <span v-if="row.errorDetail" style="color: red;">{{ row.errorDetail }}</span>
        <span v-else style="color: #ccc;">-</span>
      </template>
    </BasicTable>

    <Modal
      v-model:open="scanModalVisible"
      title="扫描数据源"
      :confirm-loading="isScanning"
      @ok="handleSubmitScan"
    >
      <Form layout="vertical">
        <Form.Item label="选择数据源" required>
          <Select
            v-model:value="selectedDsId"
            :options="datasourceOptions"
            placeholder="请选择要扫描的数据源"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="说明">
          <p style="color: #999; font-size: 12px; margin-bottom: 0;">
            扫描将获取数据源下的所有表信息及字段信息，并入库到元数据平台。
            大数据源可能需要较长时间，请耐心等待。
          </p>
        </Form.Item>
      </Form>
    </Modal>
  </Page>
</template>
