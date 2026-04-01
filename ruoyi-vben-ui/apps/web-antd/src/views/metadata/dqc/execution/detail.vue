<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Card, Descriptions, Space, Statistic, Tag } from 'ant-design-vue';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { dqcExecutionInfo, dqcExecutionDetails } from '#/api/metadata/dqc/execution';
import { useRoute } from 'vue-router';

const route = useRoute();
const executionId = computed(() => Number(route.query.id));

const executionInfo = ref<Record<string, any>>({});
const loading = ref(true);
const polling = ref(false);
let pollTimer: ReturnType<typeof setInterval> | null = null;

const statusColorMap: Record<string, string> = {
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  PARTIAL: 'warning',
  STOPPED: 'default',
};

const statusLabelMap: Record<string, string> = {
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败',
  PARTIAL: '部分成功',
  STOPPED: '已停止',
  PASS: '通过',
  FAIL: '失败',
};

const dimensionColorMap: Record<string, string> = {
  COMPLETENESS: 'blue',
  UNIQUENESS: 'purple',
  ACCURACY: 'orange',
  CONSISTENCY: 'cyan',
  TIMELINESS: 'geekblue',
  VALIDITY: 'green',
  CUSTOM: 'default',
};

const dimensionLabelMap: Record<string, string> = {
  COMPLETENESS: '完整性',
  UNIQUENESS: '唯一性',
  ACCURACY: '准确性',
  CONSISTENCY: '一致性',
  TIMELINESS: '时效性',
  VALIDITY: '有效性',
  CUSTOM: '自定义',
};

const detailGridOptions: VxeGridProps = {
  columns: [
    { title: '规则名称', field: 'ruleName', width: 180 },
    { title: '规则编码', field: 'ruleCode', width: 150 },
    {
      title: '质量维度',
      field: 'dimension',
      width: 100,
      slots: { default: 'dimension' },
    },
    { title: '目标表', field: 'targetTable', width: 150 },
    { title: '目标字段', field: 'targetColumn', width: 120 },
    { title: '实际值', field: 'actualValue', width: 120 },
    { title: '阈值', field: 'thresholdValue', width: 120 },
    {
      title: '结果',
      field: 'passFlag',
      width: 80,
      slots: { default: 'passFlag' },
    },
    { title: '耗时(ms)', field: 'elapsedMs', width: 100 },
    { title: '错误信息', field: 'errorMsg', minWidth: 200 },
  ],
  height: 500,
  pagerConfig: { enabled: true },
  rowConfig: { keyField: 'id' },
  id: 'dqc-execution-detail-grid',
};

const [DetailTable, detailTableApi] = useVbenVxeGrid({ gridOptions: detailGridOptions });

async function loadData() {
  if (executionId.value) {
    try {
      executionInfo.value = await dqcExecutionInfo(executionId.value);
      const details = await dqcExecutionDetails(executionId.value);
      detailTableApi.loadData({ rows: details || [], total: (details || []).length });
    } finally {
      loading.value = false;
    }
  }
}

function startPolling() {
  if (pollTimer) return;
  pollTimer = setInterval(async () => {
    if (executionInfo.value?.status === 'RUNNING') {
      polling.value = true;
      await loadData();
      polling.value = false;
    } else {
      stopPolling();
    }
  }, 3000);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

onMounted(async () => {
  if (executionId.value) {
    loading.value = true;
    await loadData();
    if (executionInfo.value?.status === 'RUNNING') {
      startPolling();
    }
  }
});

onUnmounted(() => {
  stopPolling();
});
</script>

<template>
  <Page :auto-content-height="true">
    <Space direction="vertical" :size="16" class="w-full">
      <Card title="执行详情" :loading="loading">
        <Descriptions :column="4" bordered>
          <Descriptions.Item label="执行编号">{{ executionInfo.executionNo || '-' }}</Descriptions.Item>
          <Descriptions.Item label="方案名称">{{ executionInfo.planName || '-' }}</Descriptions.Item>
          <Descriptions.Item label="触发方式">
            {{ executionInfo.triggerType === 'MANUAL' ? '手动' : executionInfo.triggerType === 'SCHEDULE' ? '定时' : 'API调用' }}
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag :color="statusColorMap[executionInfo.status || '']">
              {{ statusLabelMap[executionInfo.status || ''] || executionInfo.status || '-' }}
            </Tag>
            <Tag v-if="polling" color="processing">刷新中...</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="开始时间">{{ executionInfo.startTime || '-' }}</Descriptions.Item>
          <Descriptions.Item label="结束时间">{{ executionInfo.endTime || '-' }}</Descriptions.Item>
          <Descriptions.Item label="耗时">{{ executionInfo.elapsedMs ? `${executionInfo.elapsedMs}ms` : '-' }}</Descriptions.Item>
          <Descriptions.Item label="总分">{{ executionInfo.overallScore !== undefined ? `${executionInfo.overallScore}分` : '-' }}</Descriptions.Item>
        </Descriptions>
        <div class="mt-4 flex gap-8">
          <Statistic title="总规则数" :value="executionInfo.totalRules || 0" />
          <Statistic title="通过数" :value="executionInfo.passedCount || 0" :value-style="{ color: '#52c41a' }" />
          <Statistic title="失败数" :value="executionInfo.failedCount || 0" :value-style="{ color: '#ff4d4f' }" />
          <Statistic title="阻塞数" :value="executionInfo.blockedCount || 0" :value-style="{ color: '#faad14' }" />
        </div>
      </Card>

      <Card title="执行明细">
        <DetailTable />
      </Card>
    </Space>

    <template #dimension="{ row }">
      <Tag :color="dimensionColorMap[row.dimension || '']">
        {{ dimensionLabelMap[row.dimension || ''] || row.dimension || '-' }}
      </Tag>
    </template>

    <template #passFlag="{ row }">
      <Tag :color="row.passFlag === '1' ? 'success' : 'error'">
        {{ row.passFlag === '1' ? '通过' : '失败' }}
      </Tag>
    </template>
  </Page>
</template>
