<script setup lang="ts">
import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Alert, Button, Card, Select, Space, Spin, Table, message } from 'ant-design-vue';

import { datasourceEnabled } from '#/api/system/datasource';
import { maskQuery, maskQueryValidate } from '#/api/metadata/security/mask-query';

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const selectedDsId = ref<number | undefined>(undefined);
const sqlText = ref('');
const loading = ref(false);
const queryResult = ref<Record<string, any>[]>([]);
const columns = ref<{ title: string; dataIndex: string }[]>([]);
const truncated = ref(false);
const elapsedMs = ref(0);

const loadDatasources = async () => {
  const dsList = await datasourceEnabled();
  datasourceOptions.value = (dsList || []).map((item: any) => ({
    label: item.dsName,
    value: item.dsId,
  }));
  if (datasourceOptions.value.length > 0) {
    selectedDsId.value = datasourceOptions.value[0].value;
  }
};

loadDatasources();

async function handleValidate() {
  if (!sqlText.value.trim()) {
    message.warning('请输入SQL语句');
    return;
  }
  try {
    const result = await maskQueryValidate(sqlText.value);
    if (result.valid) {
      message.success('SQL语法验证通过');
    } else {
      message.error(`SQL语法验证失败: ${result.message}`);
    }
  } catch (error) {
    console.error(error);
  }
}

async function handleExecute() {
  if (!selectedDsId.value) {
    message.warning('请选择数据源');
    return;
  }
  if (!sqlText.value.trim()) {
    message.warning('请输入SQL语句');
    return;
  }

  loading.value = true;
  try {
    const result = await maskQuery({
      dsId: selectedDsId.value,
      sql: sqlText.value,
    });
    queryResult.value = result.rows || [];
    truncated.value = result.truncated || false;
    elapsedMs.value = result.elapsedMs || 0;

    // Build columns from first row
    if (queryResult.value.length > 0) {
      const firstRow = queryResult.value[0];
      columns.value = Object.keys(firstRow).map((key) => ({
        title: key,
        dataIndex: key,
      }));
    } else {
      columns.value = [];
    }
    message.success(`查询完成，返回 ${queryResult.value.length} 条记录`);
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Page :auto-content-height="true">
    <Card title="脱敏查询" class="mb-4">
      <Space direction="vertical" :size="16" class="w-full">
        <Space>
          <Select
            v-model:value="selectedDsId"
            :options="datasourceOptions"
            placeholder="请选择数据源"
            style="width: 200px"
          />
          <Button @click="handleValidate">验证SQL</Button>
        </Space>
        <Alert
          message="说明"
          description="在此输入的SQL语句将经过脱敏处理后执行，请勿输入DELETE/UPDATE/DROP等危险操作。"
          type="info"
          show-icon
        />
        <div>
          <div class="mb-2 text-gray-600">SQL语句：</div>
          <a-textarea
            v-model:value="sqlText"
            placeholder="请输入SELECT语句，如: SELECT * FROM users WHERE id = 1"
            :rows="6"
            style="font-family: monospace"
          />
        </div>
        <Space>
          <Button type="primary" :loading="loading" @click="handleExecute">
            执行查询
          </Button>
        </Space>
      </Space>
    </Card>

    <Card
      title="查询结果"
      extra={
        <Space>
          <span v-if="truncated" class="text-orange-500">数据已截断</span>
          <span class="text-gray-500">耗时: {{ elapsedMs }}ms</span>
        </Space>
      }
    >
      <Spin :spinning="loading">
        <Table
          :data-source="queryResult"
          :columns="columns"
          :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` }"
          :scroll="{ x: 'max-content' }"
          :scroll="{ y: 400 }"
          size="small"
        />
      </Spin>
    </Card>
  </Page>
</template>
