<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { Card, Row, Col, Statistic } from 'ant-design-vue';
import type { EchartsUIType } from '@vben/plugins/echarts';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

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

const pieRef = ref<EchartsUIType>();
const trendRef = ref<EchartsUIType>();
const { renderEcharts: renderPie, updateData: updatePie } = useEcharts(pieRef);
const { renderEcharts: renderTrend, updateData: updateTrend } = useEcharts(trendRef);

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

const pieOption = computed(() => {
  const data = scoreChartData.value;
  const overall = props.latestScore?.overallScore || 0;
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}分' },
    series: [
      {
        type: 'pie',
        radius: ['52%', '78%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        data: data.map((d) => ({ name: d.dimension, value: d.score })),
        color: ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      },
    ],
    title: {
      text: overall.toString(),
      subtext: '综合得分',
      left: 'center',
      top: '36%',
      textStyle: { fontSize: 28, fontWeight: 700, color: '#334155' },
      subtextStyle: { fontSize: 13, color: '#94a3b8', top: 8 },
    },
  };
});

const trendOption = computed(() => {
  const data = props.trendData || [];
  if (data.length === 0) return {};
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 12, right: 16, bottom: 32, left: 48 },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.checkDate?.slice(5) || ''),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { fontSize: 11, color: '#94a3b8' },
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { fontSize: 11, color: '#94a3b8' },
    },
    series: [{
      type: 'line',
      data: data.map((d) => d.overallScore || 0),
      smooth: 0.4,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2, color: '#1677FF' },
      itemStyle: { color: '#1677FF', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(22,119,255,0.15)' },
            { offset: 1, color: 'rgba(22,119,255,0.01)' },
          ],
        },
      },
    }],
  };
});

watch(
  () => [props.latestScore, props.trendData],
  () => {
    if (pieOption.value?.series) updatePie(pieOption.value);
    if (trendOption.value?.series) updateTrend(trendOption.value);
  },
  { deep: true },
);

onMounted(() => {
  if (pieOption.value?.series) renderPie(pieOption.value);
  if (trendOption.value?.series) renderTrend(trendOption.value);
});
</script>

<template>
  <div class="score-dashboard">
    <a-spin v-if="loading" class="flex justify-center py-10" />

    <template v-else-if="latestScore">
      <a-row :gutter="16" class="mb-4">
        <a-col :span="8">
          <a-card title="综合得分" size="small">
            <EchartsUI ref="pieRef" style="height: 280px" />
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
        <EchartsUI v-if="trendOption.series?.length" ref="trendRef" style="height: 200px" />
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
