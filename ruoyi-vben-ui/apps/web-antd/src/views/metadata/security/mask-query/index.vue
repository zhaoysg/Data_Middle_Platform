<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import {
  Alert,
  Button,
  Card,
  Empty,
  Select,
  Space,
  Spin,
  Table,
  Textarea,
  message,
} from 'ant-design-vue';

import { datasourceEnabled } from '#/api/system/datasource';
import { maskQuery, maskQueryValidate } from '#/api/metadata/security/mask-query';

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const datasourceLoading = ref(false);
const selectedDsId = ref<number>();
const sqlText = ref('');
const loading = ref(false);
const queryResult = ref<Record<string, any>[]>([]);
const columns = ref<{ title: string; dataIndex: string; width?: number }[]>([]);
const truncated = ref(false);
const elapsedMs = ref(0);

const datasourcePlaceholder = computed(() => {
  if (datasourceLoading.value) {
    return '数据源加载中...';
  }
  if (!datasourceOptions.value.length) {
    return '无可用数据源，请先在数据源管理中添加';
  }
  return '请选择数据源';
});

const canValidate = computed(() => sqlText.value.trim().length > 0 && !loading.value);
const canExecute = computed(() => !!selectedDsId.value && canValidate.value);

async function loadDatasources() {
  datasourceLoading.value = true;
  try {
    const dsList = await datasourceEnabled();
    datasourceOptions.value = (dsList || []).map((item: any) => ({
      label: item.dsName,
      value: item.dsId,
    }));
    const firstDatasource = datasourceOptions.value[0];
    if (!selectedDsId.value && firstDatasource) {
      selectedDsId.value = firstDatasource.value;
    }
  } finally {
    datasourceLoading.value = false;
  }
}

async function handleValidate() {
  if (!canValidate.value) {
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

    if (queryResult.value.length > 0) {
      const firstRow = queryResult.value[0];
      columns.value = firstRow
        ? Object.keys(firstRow).map((key) => ({
            title: key,
            dataIndex: key,
            width: 160,
          }))
        : [];
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

onMounted(() => {
  void loadDatasources();
});
</script>

<template>
  <Page :auto-content-height="true">
    <Card title="脱敏查询" class="mb-4">
      <Space direction="vertical" :size="16" class="w-full">
        <Space>
          <Select
            v-model:value="selectedDsId"
            :options="datasourceOptions"
            :loading="datasourceLoading"
            :disabled="!datasourceOptions.length"
            :placeholder="datasourcePlaceholder"
            style="width: 220px"
          />
          <Button :disabled="!canValidate" @click="handleValidate">验证SQL</Button>
        </Space>
        <Alert
          message="说明"
          description="在此输入的 SQL 会先校验，再按脱敏策略改写后执行。系统会自动限制结果集大小，请勿输入 DELETE、UPDATE、DROP 等危险语句。"
          type="info"
          show-icon
        />
        <div>
          <div class="mb-2 text-gray-600">SQL语句：</div>
          <Textarea
            v-model:value="sqlText"
            placeholder="请输入 SELECT 语句，如：SELECT * FROM users WHERE id = 1"
            :rows="6"
            style="font-family: monospace"
          />
        </div>
        <Space>
          <Button type="primary" :loading="loading" :disabled="!canExecute" @click="handleExecute">
            执行查询
          </Button>
        </Space>
      </Space>
    </Card>

    <Card title="查询结果">
      <template #extra>
        <Space>
          <span v-if="truncated" class="text-orange-500">已按 LIMIT 1000 截断</span>
          <span class="text-gray-500">耗时: {{ elapsedMs }}ms</span>
        </Space>
      </template>

      <Spin :spinning="loading">
        <Alert
          v-if="truncated"
          class="mb-4"
          type="warning"
          show-icon
          message="查询结果已自动追加 LIMIT 1000，仅展示前 1000 行。"
        />

        <Empty
          v-if="!loading && !queryResult.length"
          description="输入 SQL 并选择数据源后，即可预览脱敏结果"
        />

        <Table
          v-else
          :data-source="queryResult"
          :columns="columns"
          :pagination="{
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total: number) => `共 ${total} 条`,
          }"
          :scroll="{ x: 'max-content', y: 400 }"
          size="small"
        />
      </Spin>
    </Card>
  </Page>
</template>
