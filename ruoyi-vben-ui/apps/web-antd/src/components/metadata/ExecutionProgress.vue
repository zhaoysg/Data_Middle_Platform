<script setup lang="ts">
import { computed } from 'vue';
import { Steps } from 'ant-design-vue';

interface ExecutionDetail {
  id?: number;
  ruleName?: string;
  status?: string;
  elapsedMs?: number;
  errorMsg?: string;
  actualValue?: string;
}

interface Props {
  details?: ExecutionDetail[];
  loading?: boolean;
}

const props = defineProps<Props>();

const statusMap = {
  PENDING: { color: 'default', text: '等待执行' },
  RUNNING: { color: 'processing', text: '执行中' },
  SUCCESS: { color: 'success', text: '通过' },
  FAILED: { color: 'error', text: '失败' },
};

function getStepItem(detail: ExecutionDetail) {
  const status = statusMap[detail.status as keyof typeof statusMap] || statusMap.PENDING;
  const elapsed = detail.elapsedMs ? `(${detail.elapsedMs}ms)` : '';
  return {
    title: detail.ruleName || '未知规则',
    description:
      detail.status === 'SUCCESS'
        ? `${status.text} ${elapsed}，实际值: ${detail.actualValue || '-'}`
        : detail.status === 'FAILED'
          ? `${status.text}: ${detail.errorMsg || '未知错误'}`
          : status.text,
    status: detail.status?.toLowerCase() || 'wait',
  };
}

const stepItems = computed(() => {
  return (props.details || []).map(getStepItem);
});
</script>

<template>
  <div class="execution-progress">
    <a-spin v-if="loading" />
    <a-empty v-else-if="!details?.length" description="暂无执行明细" />
    <Steps v-else direction="vertical" size="small" :items="stepItems" />
  </div>
</template>

<style scoped>
.execution-progress {
  min-height: 200px;
}
</style>
