<script setup lang="ts">
import type { VbenFormProps } from '@vben/common-ui';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { Datasource } from '#/api/system/datasource/model';

import { Page, useVbenModal } from '@vben/common-ui';
import { getVxePopupContainer } from '@vben/utils';

import { Modal, Popconfirm, Space, message } from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import {
  datasourceChangeStatus,
  datasourceExport,
  datasourceList,
  datasourceRemove,
  datasourceTest,
} from '#/api/system/datasource';
import { TableSwitch } from '#/components/table';
import { commonDownloadExcel } from '#/utils/file/download';

import { columns, querySchema } from './data';
import datasourceDrawer from './datasource-drawer.vue';

const formOptions: VbenFormProps = {
  commonConfig: {
    labelWidth: 80,
    componentProps: {
      allowClear: true,
    },
  },
  schema: querySchema(),
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  checkboxConfig: {
    highlight: true,
    reserve: true,
  },
  columns,
  height: 'auto',
  keepSource: true,
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues = {}) => {
        return await datasourceList({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        });
      },
    },
  },
  rowConfig: {
    keyField: 'dsId',
  },
  id: 'system-datasource-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({
  formOptions,
  gridOptions,
});

const [DatasourceModal, modalApi] = useVbenModal({
  connectedComponent: datasourceDrawer,
});

function handleAdd() {
  modalApi.setData({});
  modalApi.open();
}

async function handleEdit(record: Datasource) {
  modalApi.setData({ id: record.dsId });
  modalApi.open();
}

async function handleDelete(row: Datasource) {
  await datasourceRemove([row.dsId!]);
  await tableApi.query();
}

function handleMultiDelete() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows
    .map((row: Datasource) => row.dsId)
    .filter((id): id is number => id !== undefined);
  Modal.confirm({
    title: '提示',
    okType: 'danger',
    content: `确认删除选中的 ${ids.length} 条数据源吗？删除后将无法恢复！`,
    onOk: async () => {
      await datasourceRemove(ids);
      await tableApi.query();
    },
  });
}

function handleDownloadExcel() {
  commonDownloadExcel(datasourceExport, '数据源数据', tableApi.formApi.form.values);
}

/** 列表测试连接 */
async function handleRowTest(row: Datasource) {
  if (!row.dsId) {
    message.warning('请选择有效的数据源');
    return;
  }
  const hide = message.loading('正在测试连接...');
  try {
    const result = await datasourceTest({
      dsId: row.dsId,
      dsType: row.dsType,
      host: row.host,
      port: row.port,
      databaseName: row.databaseName,
      schemaName: row.schemaName,
    });
    hide();
    if (result.success) {
      message.success(`连接成功！版本：${result.databaseVersion || '未知'}，耗时：${result.elapsedMs}ms`);
    } else {
      message.error(`连接失败：${result.message}`);
    }
  } catch (error: any) {
    hide();
    message.error(`连接异常：${error?.message || '未知错误'}`);
  }
}
</script>

<template>
  <Page :auto-content-height="true">
    <BasicTable table-title="数据源列表">
      <template #toolbar-tools>
        <Space>
          <a-button
            v-access:code="['system:ds:export']"
            @click="handleDownloadExcel"
          >
            {{ $t('pages.common.export') }}
          </a-button>
          <a-button
            :disabled="!vxeCheckboxChecked(tableApi)"
            danger
            type="primary"
            v-access:code="['system:ds:remove']"
            @click="handleMultiDelete"
          >
            {{ $t('pages.common.delete') }}
          </a-button>
          <a-button
            type="primary"
            v-access:code="['system:ds:add']"
            @click="handleAdd"
          >
            {{ $t('pages.common.add') }}
          </a-button>
        </Space>
      </template>
      <template #status="{ row }">
        <TableSwitch
          v-model:value="row.status"
          :api="() => datasourceChangeStatus(row)"
          v-access:code="['system:ds:edit']"
          @reload="tableApi.query()"
        />
      </template>
      <template #action="{ row }">
        <Space>
          <ghost-button
            v-access:code="['system:ds:test']"
            @click.stop="handleRowTest(row)"
          >
            测试
          </ghost-button>
          <ghost-button
            v-access:code="['system:ds:edit']"
            @click.stop="handleEdit(row)"
          >
            {{ $t('pages.common.edit') }}
          </ghost-button>
          <Popconfirm
            :get-popup-container="getVxePopupContainer"
            placement="left"
            title="确认删除？删除后将无法恢复！"
            @confirm="handleDelete(row)"
          >
            <ghost-button
              danger
              v-access:code="['system:ds:remove']"
              @click.stop=""
            >
              {{ $t('pages.common.delete') }}
            </ghost-button>
          </Popconfirm>
        </Space>
      </template>
    </BasicTable>
    <DatasourceModal @reload="tableApi.query()" />
  </Page>
</template>
