<script setup lang="ts">
import { computed } from 'vue';
import { Pie, Line } from '@ant-design/charts';
import { Card, Row, Col, Statistic } from 'ant-design-vue';

interface ScoreData {
  checkDate?: string;
  overallScore?: number;
  completenessScore?: number;
  uniquenessScore?: number;
  accuracyScore?: number;
  consistencyScore?: number;
  timelinessScore?: number;
  validityScore?: number;
  rulePassRate?: number;
  ruleTotalCount?: number;
  rulePassCount?: number;
}

interface Props {
  latestScore?: ScoreData;
  trendData?: ScoreData[];
  loading?: boolean;
}

const props = defineProps<Props>();

const scoreChartData = computed(() => {
  if (!props.latestScore) return [];
  const s = props.latestScore;
  return [
    { dimension: '完整性', score: s.completenessScore || 0 },
    { dimension: '唯一性', score: s.uniquenessScore || 0 },
    { dimension: '准确性', score: s.accuracyScore || 0 },
    { dimension: '一致性', score: s.consistencyScore || 0 },
    { dimension: '及时性', score: s.timelinessScore || 0 },
    { dimension: '有效性', score: s.validityScore || 0 },
  ];
});

const pieConfig = computed(() => ({
  data: scoreChartData.value,
  angleField: 'score',
  colorField: 'dimension',
  radius: 0.8,
  innerRadius: 0.65,
  label: { text: 'score', style: { fontWeight: 400 } },
  legend: { color: { title: false, position: 'right' } },
  statistic: {
    title: { content: '综合得分', style: { fontSize: 14 } },
    content: {
      content: String(props.latestScore?.overallScore || 0),
      style: { fontSize: 28, fontWeight: 'bold' },
    },
  },
  color: ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD'],
}));

const trendConfig = computed(() => ({
  data: (props.trendData || []).map((d) => ({
    date: d.checkDate,
    score: d.overallScore || 0,
  })),
  xField: 'date',
  yField: 'score',
  smooth: true,
  color: '#1677FF',
  point: { size: 4, shape: 'circle' },
  yAxis: { min: 0, max: 100 },
  xAxis: { label: { formatter: (v: string) => v?.slice(5) } },
}));
</script>

<template>
  <div class="score-dashboard">
    <a-spin v-if="loading" class="flex justify-center py-10" />

    <template v-else-if="latestScore">
      <a-row :gutter="16" class="mb-4">
        <a-col :span="8">
          <a-card title="综合得分" size="small">
            <Pie v-bind="pieConfig" style="height: 280px" />
          </a-card>
        </a-col>

        <a-col :span="16">
          <a-row :gutter="[12, 12]">
            <a-col :span="8" v-for="dim in scoreChartData" :key="dim.dimension">
              <a-card size="small">
                <Statistic
                  :title="dim.dimension"
                  :value="dim.score"
                  suffix="/ 100"
                  :valueStyle="{
                    color: dim.score >= 80 ? '#52C41A' : dim.score >= 60 ? '#FAAD14' : '#FF4D4F',
                  }"
                />
              </a-card>
            </a-col>
          </a-row>

          <a-row :gutter="12" class="mt-3">
            <a-col :span="8">
              <a-card size="small">
                <Statistic title="规则总数" :value="latestScore.ruleTotalCount || 0" />
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card size="small">
                <Statistic
                  title="通过数"
                  :value="latestScore.rulePassCount || 0"
                  :valueStyle="{ color: '#52C41A' }"
                />
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card size="small">
                <Statistic title="通过率" :value="latestScore.rulePassRate || 0" suffix="%" />
              </a-card>
            </a-col>
          </a-row>
        </a-col>
      </a-row>

      <a-card title="评分趋势" size="small" class="mt-4">
        <Line v-if="trendConfig.data?.length" v-bind="trendConfig" style="height: 200px" />
        <a-empty v-else description="暂无趋势数据" />
      </a-card>
    </template>

    <a-result v-else status="warning" title="暂无评分数据" sub-title="请先运行质检方案" />
  </div>
</template>

<style scoped>
.score-dashboard {
  padding: 0;
}
</style>
