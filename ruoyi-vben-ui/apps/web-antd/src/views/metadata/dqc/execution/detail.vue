<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Button, Card, Descriptions, Modal, Progress, Space, Statistic, Tag } from 'ant-design-vue';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { ExecutionProgress } from '#/components/metadata';
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

const allDimensions = [
  { key: 'completenessScore', label: '完整性', color: 'blue' },
  { key: 'uniquenessScore', label: '唯一性', color: 'purple' },
  { key: 'accuracyScore', label: '准确性', color: 'orange' },
  { key: 'consistencyScore', label: '一致性', color: 'cyan' },
  { key: 'timelinessScore', label: '时效性', color: 'geekblue' },
  { key: 'validityScore', label: '有效性', color: 'green' },
];

function getScoreColor(score?: number) {
  if (score === undefined) return '#d9d9d9';
  if (score >= 80) return '#52c41a';
  if (score >= 60) return '#faad14';
  return '#ff4d4f';
}

function getScoreStatus(score?: number): 'success' | 'normal' | 'exception' {
  if (score === undefined) return 'normal';
  if (score >= 80) return 'success';
  if (score >= 60) return 'normal';
  return 'exception';
}

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
  data: [],
};

const [DetailTable, detailTableApi] = useVbenVxeGrid({ gridOptions: detailGridOptions });

const stepsModalVisible = ref(false);
const stepsDetails = ref<any[]>([]);
const failedModalVisible = ref(false);
const failedRules = ref<any[]>([]);

async function loadData() {
  if (executionId.value) {
    try {
      const info = await dqcExecutionInfo(executionId.value);
      executionInfo.value = info || {};
      const details = await dqcExecutionDetails(executionId.value);
      const rows = Array.isArray(details) ? details : [];
      detailTableApi.grid?.loadData(rows);
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

function openStepsModal() {
  const allData = detailTableApi.grid?.getData() ?? [];
  stepsDetails.value = allData.map((row: any) => ({
    id: row.id,
    ruleName: row.ruleName,
    status: row.passFlag === '1' ? 'SUCCESS' : row.passFlag === '0' ? 'FAILED' : 'PENDING',
    elapsedMs: row.elapsedMs,
    errorMsg: row.errorMsg,
    actualValue: row.actualValue,
  }));
  stepsModalVisible.value = true;
}

function openFailedModal() {
  const allData = detailTableApi.grid?.getData() ?? [];
  failedRules.value = allData.filter((row: any) => row.passFlag === '0');
  failedModalVisible.value = true;
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

      <!-- Score Breakdown -->
      <Card
        title="评分细分"
        :loading="loading"
        class="score-breakdown-card"
      >
        <div class="grid grid-cols-2 lg:grid-cols-3 gap-x-8 gap-y-5">
          <div
            v-for="dim in allDimensions"
            :key="dim.key"
            class="flex flex-col"
          >
            <div class="flex justify-between items-center mb-1">
              <span class="text-sm text-gray-600">{{ dim.label }}</span>
              <span
                class="text-sm font-semibold"
                :style="{ color: getScoreColor(executionInfo?.[dim.key]) }"
              >
                {{ executionInfo?.[dim.key] !== undefined ? `${executionInfo[dim.key]}分` : '-' }}
              </span>
            </div>
            <Progress
              :percent="executionInfo?.[dim.key] || 0"
              :status="getScoreStatus(executionInfo?.[dim.key])"
              :stroke-color="getScoreColor(executionInfo?.[dim.key])"
              :show-info="false"
              size="small"
            />
          </div>
        </div>
      </Card>

      <!-- Action Buttons -->
      <Card :loading="loading">
        <Space wrap>
          <Button type="primary" @click="openStepsModal">
            查看执行步骤
          </Button>
          <Button
            v-if="(executionInfo.failedCount || 0) > 0"
            danger
            @click="openFailedModal"
          >
            查看失败规则 ({{ executionInfo.failedCount }})
          </Button>
        </Space>
      </Card>

      <Card title="执行明细">
        <DetailTable>
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
        </DetailTable>
      </Card>
    </Space>

    <!-- Steps Modal -->
    <Modal
      v-model:open="stepsModalVisible"
      title="执行步骤"
      :footer="null"
      width="700"
    >
      <ExecutionProgress :details="stepsDetails" />
    </Modal>

    <!-- Failed Rules Modal -->
    <Modal
      v-model:open="failedModalVisible"
      title="失败规则列表"
      :footer="null"
      width="900"
    >
      <div class="space-y-4">
        <div v-if="failedRules.length === 0" class="text-center text-gray-400 py-8">
          暂无失败规则
        </div>
        <div
          v-for="(rule, index) in failedRules"
          :key="index"
          class="border border-red-200 rounded-lg p-4 bg-red-50"
        >
          <div class="flex justify-between items-start mb-2">
            <div>
              <span class="font-medium text-red-700">{{ rule.ruleName }}</span>
              <Tag
                :color="dimensionColorMap[rule.dimension || '']"
                class="ml-2"
              >
                {{ dimensionLabelMap[rule.dimension || ''] || rule.dimension }}
              </Tag>
            </div>
            <Tag color="error">失败</Tag>
          </div>
          <div class="grid grid-cols-2 gap-3 text-sm mb-2">
            <div>
              <span class="text-gray-500">目标表：</span>
              <span>{{ rule.targetTable || '-' }}</span>
            </div>
            <div>
              <span class="text-gray-500">目标字段：</span>
              <span>{{ rule.targetColumn || '-' }}</span>
            </div>
            <div>
              <span class="text-gray-500">实际值：</span>
              <span class="text-red-600">{{ rule.actualValue || '-' }}</span>
            </div>
            <div>
              <span class="text-gray-500">阈值：</span>
              <span>{{ rule.thresholdValue || '-' }}</span>
            </div>
          </div>
          <div v-if="rule.errorMsg" class="text-sm">
            <span class="text-gray-500">错误信息：</span>
            <span class="text-red-600">{{ rule.errorMsg }}</span>
          </div>
          <div v-if="rule.executeSql" class="mt-3">
            <div class="text-xs text-gray-500 mb-1">执行的SQL：</div>
            <pre
              class="bg-white border border-gray-200 rounded p-3 text-xs text-gray-700 whitespace-pre-wrap overflow-auto max-h-40">{{ rule.executeSql }}</pre>
          </div>
        </div>
      </div>
    </Modal>
  </Page>
</template>
