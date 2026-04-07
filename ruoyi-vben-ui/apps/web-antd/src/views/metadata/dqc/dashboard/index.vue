<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import {
  Badge,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Empty,
  Progress,
  Row,
  Select,
  Spin,
  Statistic,
  Table,
  Tag,
  Tabs,
  Timeline,
  Tooltip,
} from 'ant-design-vue';
import { computed, onMounted, ref, watch } from 'vue';

import { dqcExecutionList } from '#/api/metadata/dqc/execution';
import { dqcScoreTrend } from '#/api/metadata/dqc/score';
import type { DqcExecution, DqcQualityScore } from '#/api/metadata/model';

const loading = ref(false);
const days = ref(30);
const activeTab = ref('overview');
const trendData = ref<DqcQualityScore[]>([]);
const executionData = ref<DqcExecution[]>([]);

// Filter (detail tab)
const filterLayer = ref<string>('');
const filterStatus = ref<string>('');

// Chart refs
const gaugeRef = ref<EchartsUIType>();
const trendRef = ref<EchartsUIType>();
const radarRef = ref<EchartsUIType>();
const barRef = ref<EchartsUIType>();
const layerBarRef = ref<EchartsUIType>();

const { renderEcharts: renderGauge, updateData: updateGauge } = useEcharts(gaugeRef);
const { renderEcharts: renderTrend, updateData: updateTrend } = useEcharts(trendRef);
const { renderEcharts: renderRadar, updateData: updateRadar } = useEcharts(radarRef);
const { renderEcharts: renderBar, updateData: updateBar } = useEcharts(barRef);
const { renderEcharts: renderLayerBar, updateData: updateLayerBar } = useEcharts(layerBarRef);

// ── Helpers ──────────────────────────────────────────────────────

function getScoreColor(score?: number) {
  if (score === undefined || score === null) return '#8c8c8c';
  if (score >= 90) return '#52c41a';
  if (score >= 70) return '#faad14';
  return '#ff4d4f';
}

function getScoreTagColor(score?: number) {
  if (score === undefined || score === null) return 'default';
  if (score >= 90) return 'success';
  if (score >= 70) return 'warning';
  return 'error';
}

function getScoreLabel(score?: number) {
  if (score === undefined || score === null) return '暂无评分';
  if (score >= 90) return '优秀';
  if (score >= 70) return '良好';
  if (score > 0) return '较差';
  return '—';
}

function getStatusColor(status?: string) {
  switch (status) {
    case 'SUCCESS': return 'success';
    case 'FAILED': return 'error';
    case 'PARTIAL': return 'warning';
    case 'RUNNING': return 'processing';
    default: return 'default';
  }
}

function getStatusLabel(status?: string) {
  switch (status) {
    case 'SUCCESS': return '成功';
    case 'FAILED': return '失败';
    case 'PARTIAL': return '部分失败';
    case 'RUNNING': return '运行中';
    default: return status || '—';
  }
}

function avg(list: DqcQualityScore[], field: keyof DqcQualityScore): number {
  const vals = list
    .map((r) => r[field])
    .filter((v): v is number => v !== undefined && v !== null);
  if (vals.length === 0) return 0;
  return Math.round((vals.reduce((s, v) => s + v, 0) / vals.length) * 10) / 10;
}

function avgExec(list: DqcExecution[], field: keyof DqcExecution): number {
  const vals = list
    .filter((r) => r.status !== 'RUNNING')
    .map((r) => r[field])
    .filter((v): v is number => v !== undefined && v !== null);
  if (vals.length === 0) return 0;
  return Math.round((vals.reduce((s, v) => s + v, 0) / vals.length) * 10) / 10;
}

function fmtElapsed(ms?: number) {
  if (ms === undefined || ms === null) return '—';
  if (ms < 60000) return `${Math.round(ms / 1000)}秒`;
  return `${Math.floor(ms / 60000)}分${Math.round((ms % 60000) / 1000)}秒`;
}

// ── Exec stats (from dqc_execution) ──────────────────────────────

const execOverview = computed(() => {
  const list = executionData.value;
  if (list.length === 0) {
    return {
      total: 0, success: 0, failed: 0, partial: 0, running: 0,
      avgScore: 0, avgPassRate: 0, totalRules: 0,
      alertCount: 0, planCount: 0, layerCount: 0,
    };
  }
  const total = list.length;
  const success = list.filter((r) => r.status === 'SUCCESS').length;
  const failed = list.filter((r) => r.status === 'FAILED').length;
  const partial = list.filter((r) => r.status === 'PARTIAL').length;
  const running = list.filter((r) => r.status === 'RUNNING').length;
  const completed = list.filter((r) => r.status !== 'RUNNING');
  const avgScore = avgExec(list, 'overallScore');
  const totalRules = completed.reduce((s, r) => s + (r.totalRules || 0), 0);
  const totalPassed = completed.reduce((s, r) => s + (r.passedCount || 0), 0);
  const avgPassRate = totalRules > 0 ? Math.round((totalPassed / totalRules) * 100) : 0;
  const alertCount = completed.filter((r) => (r.overallScore || 0) < 70).length;
  const planSet = new Set(list.map((r) => r.planId).filter(Boolean));
  const layerSet = new Set(list.map((r) => r.layerCode).filter(Boolean));
  return { total, success, failed, partial, running, avgScore, avgPassRate, totalRules, alertCount, planCount: planSet.size, layerCount: layerSet.size };
});

// ── Score overview (from dqc_quality_score trend) ────────────────

const scoreOverview = computed(() => {
  const data = trendData.value;
  if (data.length === 0) return { avgOverall: 0, avgPassRate: 0, totalTables: 0, alertCount: 0 };
  const n = data.length;
  const avgOverall = avg(data, 'overallScore');
  const avgPassRate = Math.round(data.reduce((s, r) => s + (r.rulePassRate || 0), 0) / n);
  const tables = new Set(data.map((r) => `${r.targetDsId}-${r.targetTable}`)).size;
  const alertCount = data.filter((r) => (r.overallScore || 0) < 70).length;
  return { avgOverall, avgPassRate, totalTables: tables, alertCount };
});

// Use whichever has data
const effectiveOverall = computed(() => {
  return scoreOverview.value.avgOverall || execOverview.value.avgScore;
});

const effectivePassRate = computed(() => {
  return scoreOverview.value.avgPassRate || execOverview.value.avgPassRate;
});

const effectiveAlert = computed(() => {
  return scoreOverview.value.alertCount || execOverview.value.alertCount;
});

// ── Six dimensions ───────────────────────────────────────────────

const dimensions = computed(() => {
  const data = trendData.value;
  const defs = [
    { label: '完整性', field: 'completenessScore' as keyof DqcQualityScore },
    { label: '唯一性', field: 'uniquenessScore' as keyof DqcQualityScore },
    { label: '准确性', field: 'accuracyScore' as keyof DqcQualityScore },
    { label: '一致性', field: 'consistencyScore' as keyof DqcQualityScore },
    { label: '时效性', field: 'timelinessScore' as keyof DqcQualityScore },
    { label: '有效性', field: 'validityScore' as keyof DqcQualityScore },
  ];
  return defs.map((d) => ({ ...d, value: avg(data, d.field) }));
});

// ── Layer breakdown ─────────────────────────────────────────────

const layerStats = computed(() => {
  const map = new Map<string, { passed: number; total: number; score: number; execCount: number }>();
  for (const r of executionData.value) {
    if (!r.layerCode || r.status === 'RUNNING') continue;
    if (!map.has(r.layerCode)) map.set(r.layerCode, { passed: 0, total: 0, score: 0, execCount: 0 });
    const s = map.get(r.layerCode)!;
    s.passed += r.passedCount || 0;
    s.total += r.totalRules || 0;
    s.score += r.overallScore || 0;
    s.execCount++;
  }
  return [...map.entries()].map(([layer, v]) => ({
    layer,
    passRate: v.total > 0 ? Math.round((v.passed / v.total) * 100) : 0,
    avgScore: v.execCount > 0 ? Math.round(v.score / v.execCount) : 0,
    execCount: v.execCount,
  })).sort((a, b) => b.avgScore - a.avgScore);
});

// ── Execution history ──────────────────────────────────────────

const recentExecutions = computed(() => {
  return [...executionData.value]
    .sort((a, b) => {
      const ta = a.startTime ? new Date(a.startTime).getTime() : 0;
      const tb = b.startTime ? new Date(b.startTime).getTime() : 0;
      return tb - ta;
    })
    .slice(0, 10);
});

// ── Filtered executions (detail tab) ───────────────────────────

const filteredExecutions = computed(() => {
  return [...executionData.value]
    .filter((r) => {
      if (filterLayer.value && r.layerCode !== filterLayer.value) return false;
      if (filterStatus.value && r.status !== filterStatus.value) return false;
      return true;
    })
    .sort((a, b) => {
      const ta = a.startTime ? new Date(a.startTime).getTime() : 0;
      const tb = b.startTime ? new Date(b.startTime).getTime() : 0;
      return tb - ta;
    });
});

// Layer options for filter
const layerOptions = computed(() => {
  const set = new Set(executionData.value.map((r) => r.layerCode).filter(Boolean));
  return [...set].map((v) => ({ label: v, value: v }));
});

// ── Chart options ───────────────────────────────────────────────

// Gauge
const gaugeOption = computed(() => {
  const score = effectiveOverall.value;
  const color = getScoreColor(score);
  return {
    series: [{
      type: 'gauge',
      startAngle: 200, endAngle: -20,
      min: 0, max: 100, splitNumber: 5,
      itemStyle: { color },
      progress: { show: true, width: 22 },
      pointer: { show: false },
      axisLine: { lineStyle: { width: 22, color: [[1, '#f0f0f0']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        offsetCenter: [0, '0%'],
        fontSize: 56,
        fontWeight: 'bold',
        formatter: '{value}',
        color,
      },
      data: [{ value: score || 0 }],
    }],
  };
});

// Trend (only available if score data exists)
const trendOption = computed(() => {
  if (trendData.value.length === 0) return {};
  const dates = trendData.value.map((r) => r.checkDate?.slice(5) || '');
  const scores = trendData.value.map((r) => r.overallScore || 0);
  const passRates = trendData.value.map((r) => r.rulePassRate || 0);
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['综合评分', '规则通过率'], bottom: 0 },
    grid: { top: 8, right: 16, bottom: 40, left: 48 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10 } },
    yAxis: [
      { type: 'value', name: '评分', min: 0, max: 100, axisLabel: { formatter: '{value}' } },
      { type: 'value', name: '通过率', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      {
        name: '综合评分', type: 'line', data: scores, smooth: true,
        lineStyle: { width: 3 }, itemStyle: { color: '#5470c6' },
        areaStyle: { color: 'rgba(84,112,198,0.15)' },
      },
      {
        name: '规则通过率', type: 'line', yAxisIndex: 1, data: passRates, smooth: true,
        lineStyle: { width: 2, type: 'dashed' }, itemStyle: { color: '#91cc75' },
      },
    ],
  };
});

// Radar
const radarOption = computed(() => {
  const data = trendData.value;
  if (data.length === 0) return {};
  return {
    tooltip: {},
    radar: {
      indicator: [
        { name: '完整性', max: 100 }, { name: '唯一性', max: 100 },
        { name: '准确性', max: 100 }, { name: '一致性', max: 100 },
        { name: '时效性', max: 100 }, { name: '有效性', max: 100 },
      ],
      radius: '65%',
      axisName: { color: '#555', fontSize: 11 },
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          avg(data, 'completenessScore'), avg(data, 'uniquenessScore'),
          avg(data, 'accuracyScore'), avg(data, 'consistencyScore'),
          avg(data, 'timelinessScore'), avg(data, 'validityScore'),
        ],
        name: '六维得分',
        areaStyle: { color: 'rgba(84,112,198,0.2)' },
        lineStyle: { color: '#5470c6', width: 2 },
        itemStyle: { color: '#5470c6' },
      }],
    }],
  };
});

// Layer bar chart
const layerBarOption = computed(() => {
  if (layerStats.value.length === 0) return {};
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['平均评分', '通过率(%)'], bottom: 0 },
    grid: { top: 8, right: 60, bottom: 40, left: 10, containLabel: true },
    xAxis: { type: 'category', data: layerStats.value.map((d) => d.layer) },
    yAxis: [
      { type: 'value', name: '评分', min: 0, max: 100 },
      { type: 'value', name: '通过率%', min: 0, max: 100 },
    ],
    series: [
      {
        name: '平均评分', type: 'bar',
        data: layerStats.value.map((d) => ({
          value: d.avgScore,
          itemStyle: { color: getScoreColor(d.avgScore), borderRadius: [4, 4, 0, 0] },
        })),
        barWidth: 32,
        label: { show: true, position: 'top', formatter: '{c}分', fontSize: 11 },
      },
      {
        name: '通过率(%)', type: 'line', yAxisIndex: 1,
        data: layerStats.value.map((d) => d.passRate),
        smooth: true, lineStyle: { width: 2 },
        itemStyle: { color: '#91cc75' },
        label: { show: true, position: 'bottom', formatter: '{c}%', fontSize: 10, color: '#91cc75' },
      },
    ],
  };
});

// Score bar chart (top tables)
const scoreBarOption = computed(() => {
  const data = [...trendData.value]
    .filter((r) => r.overallScore !== undefined)
    .sort((a, b) => (b.overallScore || 0) - (a.overallScore || 0))
    .slice(0, 10);
  if (data.length === 0) return {};
  const names = data.map((r) => `${r.dsName || ''}/${r.targetTable || ''}`.slice(0, 14));
  const scores = data.map((r) => r.overallScore || 0);
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 8, right: 60, bottom: 10, left: 10, containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}' } },
    yAxis: {
      type: 'category', data: names,
      axisLabel: { fontSize: 10, width: 100, overflow: 'truncate' },
    },
    series: [{
      type: 'bar', data: scores.map((s) => ({
        value: s, itemStyle: { color: getScoreColor(s), borderRadius: [0, 4, 4, 0] },
      })),
      barWidth: 14,
      label: { show: true, position: 'right', formatter: '{c}分', fontSize: 11 },
    }],
  };
});

// ── Execution table columns ─────────────────────────────────────

const execColumns = [
  {
    title: '执行编号', dataIndex: 'executionNo', key: 'executionNo',
    width: 160, ellipsis: true,
  },
  {
    title: '质检方案', dataIndex: 'planName', key: 'planName',
    width: 160, ellipsis: true,
  },
  {
    title: '层级', dataIndex: 'layerCode', key: 'layerCode', width: 80, align: 'center' as const,
  },
  {
    title: '触发', dataIndex: 'triggerType', key: 'triggerType', width: 80, align: 'center' as const,
  },
  {
    title: '状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' as const,
  },
  {
    title: '综合评分', dataIndex: 'overallScore', key: 'overallScore', width: 110, align: 'center' as const,
  },
  {
    title: '规则通过', key: 'ruleResult', width: 130, align: 'center' as const,
  },
  {
    title: '耗时', key: 'elapsed', width: 90, align: 'center' as const,
  },
  {
    title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160,
  },
];

// ── Data loading ────────────────────────────────────────────────

async function loadData() {
  loading.value = true;
  try {
    const [scoreRes, execRes] = await Promise.all([
      dqcScoreTrend({ days: days.value }).catch(() => []),
      dqcExecutionList({ pageNum: 1, pageSize: 100 }).catch(() => ({ rows: [] })),
    ]);
    trendData.value = Array.isArray(scoreRes) ? scoreRes : (scoreRes?.rows ?? []);
    executionData.value = execRes?.rows ?? execRes ?? [];
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  if (effectiveOverall.value > 0) renderGauge(gaugeOption.value);
  if (trendData.value.length > 0) {
    renderTrend(trendOption.value);
    renderRadar(radarOption.value);
    renderBar(scoreBarOption.value);
  }
  if (layerStats.value.length > 0) renderLayerBar(layerBarOption.value);
}

function updateCharts() {
  if (effectiveOverall.value > 0) updateGauge(gaugeOption.value);
  if (trendData.value.length > 0) {
    updateTrend(trendOption.value);
    updateRadar(radarOption.value);
    updateBar(scoreBarOption.value);
  }
  updateLayerBar(layerBarOption.value);
}

watch([trendData, executionData], () => { updateCharts(); }, { deep: true });

watch(activeTab, (tab) => {
  setTimeout(() => {
    if (tab === 'overview') renderCharts();
  }, 50);
});

onMounted(() => {
  loadData();
  setTimeout(() => renderCharts(), 200);
});
</script>

<template>
  <Page :auto-content-height="true">
    <Spin :spinning="loading">

      <!-- Page header -->
      <div class="dq-header">
        <div class="dq-header-left">
          <div class="dq-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="rgba(255,255,255,0.4)" stroke-width="2"/>
              <path d="M8 14 L10.5 9.5 L13 12 L16 7" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="16" cy="7" r="1.5" fill="#91cc75"/>
            </svg>
          </div>
          <div>
            <div class="dq-title">数据质量中心</div>
            <div class="dq-subtitle">
              <span>质量评分趋势</span>
              <span class="dot-sep">·</span>
              <span>执行监控</span>
              <span class="dot-sep">·</span>
              <span>数仓分层</span>
            </div>
          </div>
        </div>
        <div class="dq-header-right">
          <a-select v-model="days" style="width: 120px" @change="loadData">
            <a-select-option :value="7">近 7 天</a-select-option>
            <a-select-option :value="14">近 14 天</a-select-option>
            <a-select-option :value="30">近 30 天</a-select-option>
            <a-select-option :value="90">近 90 天</a-select-option>
          </a-select>
          <a-button type="primary" @click="loadData">刷新</a-button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="dq-tabs">
        <Tabs v-model:activeKey="activeTab">
          <Tabs.TabPane key="overview">
            <template #tab>
              <span class="tab-label">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 4px">
                  <rect x="1" y="1" width="12" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M4 9 L6.5 6 L9 7.5 L12 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                质量总览
              </span>
            </template>
          </Tabs.TabPane>
          <Tabs.TabPane key="executions">
            <template #tab>
              <span class="tab-label">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 4px">
                  <circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M7 4.5 L7 7 L9 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                执行记录
              </span>
            </template>
          </Tabs.TabPane>
          <Tabs.TabPane key="detail">
            <template #tab>
              <span class="tab-label">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 4px">
                  <rect x="1" y="2" width="12" height="10" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M4 5.5 h6 M4 8 h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                明细分析
              </span>
            </template>
          </Tabs.TabPane>
        </Tabs>
      </div>

      <!-- ── OVERVIEW TAB ── -->
      <div v-show="activeTab === 'overview'">

        <!-- KPI row -->
        <Row :gutter="16" class="kpi-row">

          <!-- Main gauge -->
          <Col :span="5">
            <Card class="kpi-main-card" :bordered="false">
              <div class="kpi-gauge-wrap">
                <EchartsUI ref="gaugeRef" style="height: 130px" />
              </div>
              <div class="kpi-gauge-label" :style="{ color: getScoreColor(effectiveOverall) }">
                {{ getScoreLabel(effectiveOverall) }}
              </div>
              <div class="kpi-gauge-title">综合质量评分</div>
              <div class="kpi-gauge-source">
                <span v-if="trendData.length > 0">来自 {{ scoreOverview.totalTables }} 张表的评分数据</span>
                <span v-else>来自 {{ execOverview.total }} 次执行记录</span>
              </div>
            </Card>
          </Col>

          <!-- Stat cards -->
          <Col :span="19">
            <Row :gutter="16" style="height: 100%">
              <Col :span="4">
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--green">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M10 2 L12.5 7.5 L18 8 L14 12 L15 18 L10 15 L5 18 L6 12 L2 8 L7.5 7.5 Z" fill="#fff" opacity="0.9"/>
                    </svg>
                  </div>
                  <Statistic
                    title="平均通过率"
                    :value="effectivePassRate"
                    suffix="%"
                    :value-style="{ color: getScoreColor(effectivePassRate), fontSize: '26px' }"
                  />
                  <Progress
                    :percent="effectivePassRate"
                    :stroke-color="getScoreColor(effectivePassRate)"
                    :show-info="false" size="small"
                    style="margin-top: 6px"
                  />
                </Card>
              </Col>

              <Col :span="4">
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--blue">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <rect x="3" y="3" width="14" height="14" rx="2" fill="#fff" opacity="0.9"/>
                    </svg>
                  </div>
                  <Statistic title="被监控表数" :value="scoreOverview.totalTables" :value-style="{ fontSize: '26px' }"/>
                </Card>
              </Col>

              <Col :span="4">
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--orange">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <circle cx="10" cy="10" r="6" fill="#fff" opacity="0.9"/>
                    </svg>
                  </div>
                  <Statistic title="质检规则总数" :value="execOverview.totalRules" :value-style="{ fontSize: '26px' }"/>
                </Card>
              </Col>

              <Col :span="4">
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--purple">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M10 3 L10 10 L16 10" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                      <circle cx="10" cy="10" r="7" stroke="#fff" stroke-width="1.5" opacity="0.5"/>
                    </svg>
                  </div>
                  <Statistic title="执行次数" :value="execOverview.total" :value-style="{ fontSize: '26px' }"/>
                </Card>
              </Col>

              <Col :span="4">
                <Card
                  class="kpi-stat-card"
                  :class="{ 'kpi-alert-active': effectiveAlert > 0 }"
                  :bordered="false"
                >
                  <div class="stat-icon-wrap" :class="effectiveAlert > 0 ? 'stat-icon--red' : 'stat-icon--gray'">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M10 4 L17 16 L3 16 Z" stroke="#fff" stroke-width="1.5" opacity="0.9"/>
                      <path d="M10 8 L10 12" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                      <circle cx="10" cy="14" r="0.8" fill="#fff"/>
                    </svg>
                  </div>
                  <Statistic
                    title="预警数"
                    :value="effectiveAlert"
                    :value-style="{ color: effectiveAlert > 0 ? '#ff4d4f' : '#52c41a', fontSize: '26px' }"
                  />
                  <div class="stat-sub-text" :style="{ color: effectiveAlert > 0 ? '#ff4d4f' : '#52c41a' }">
                    {{ effectiveAlert > 0 ? '评分 &lt; 70 分' : '质量正常' }}
                  </div>
                </Card>
              </Col>

              <Col :span="4">
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--teal">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M3 14 L7 9 L10 12 L14 6 L17 10" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                  <Statistic
                    title="质检方案数"
                    :value="execOverview.planCount"
                    :value-style="{ fontSize: '26px' }"
                  />
                  <div class="stat-sub-text" style="color: #8c8c8c">
                    覆盖 {{ execOverview.layerCount }} 个层级
                  </div>
                </Card>
              </Col>
            </Row>
          </Col>
        </Row>

        <!-- Execution status summary -->
        <Row :gutter="16" class="exec-status-row">
          <Col :span="3">
            <div class="exec-status-chip exec-chip--success">
              <Badge status="success"/>
              成功 {{ execOverview.success }}
            </div>
          </Col>
          <Col :span="3">
            <div class="exec-status-chip exec-chip--partial">
              <Badge status="warning"/>
              部分失败 {{ execOverview.partial }}
            </div>
          </Col>
          <Col :span="3">
            <div class="exec-status-chip exec-chip--failed">
              <Badge status="error"/>
              失败 {{ execOverview.failed }}
            </div>
          </Col>
          <Col :span="3">
            <div class="exec-status-chip exec-chip--running">
              <Badge status="processing"/>
              运行中 {{ execOverview.running }}
            </div>
          </Col>
        </Row>

        <!-- Main charts -->
        <Row :gutter="16" class="chart-row">

          <!-- Trend (score data) or execution history timeline -->
          <Col :span="15">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 6px">
                    <path d="M2 11 L5 7 L8 9 L12 4" stroke="#5470c6" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  {{ trendData.length > 0 ? '评分与通过率趋势' : '最近执行记录' }}
                </span>
              </template>
              <template #extra>
                <span style="font-size: 12px; color: #8c8c8c">近 {{ days }} 天</span>
              </template>

              <template v-if="trendData.length > 0">
                <EchartsUI ref="trendRef" style="height: 260px" />
              </template>
              <Timeline v-else style="margin-top: 8px">
                <Timeline.Item
                  v-for="exec in recentExecutions.slice(0, 6)"
                  :key="exec.id"
                  :color="getStatusColor(exec.status) === 'success' ? 'green' : getStatusColor(exec.status) === 'error' ? 'red' : getStatusColor(exec.status) === 'warning' ? 'orange' : 'blue'"
                >
                  <div class="timeline-item">
                    <div class="timeline-top">
                      <Tag :color="getStatusColor(exec.status)">{{ getStatusLabel(exec.status) }}</Tag>
                      <span class="timeline-plan">{{ exec.planName || '—' }}</span>
                      <span class="timeline-layer">{{ exec.layerCode || '—' }}</span>
                    </div>
                    <div class="timeline-bottom">
                      <span class="timeline-score" v-if="exec.overallScore" :style="{ color: getScoreColor(exec.overallScore) }">
                        评分 {{ exec.overallScore }}分
                      </span>
                      <span class="timeline-rules" v-if="exec.totalRules">
                        {{ exec.passedCount }}/{{ exec.totalRules }} 规则通过
                      </span>
                      <span class="timeline-time">{{ exec.startTime?.slice(0, 16) || '—' }}</span>
                    </div>
                  </div>
                </Timeline.Item>
              </Timeline>
            </Card>
          </Col>

          <!-- Radar -->
          <Col :span="9">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 6px">
                    <circle cx="7" cy="7" r="5.5" stroke="#5470c6" stroke-width="1.5"/>
                    <circle cx="7" cy="7" r="3" stroke="#5470c6" stroke-width="1" opacity="0.5"/>
                    <circle cx="7" cy="7" r="1" fill="#5470c6"/>
                  </svg>
                  {{ trendData.length > 0 ? '六维质量雷达' : '执行统计' }}
                </span>
              </template>

              <template v-if="trendData.length > 0">
                <EchartsUI ref="radarRef" style="height: 260px" />
              </template>
              <div v-else class="exec-stats-empty">
                <div class="exec-stat-row">
                  <span class="exec-stat-label">总执行</span>
                  <span class="exec-stat-val">{{ execOverview.total }}</span>
                </div>
                <div class="exec-stat-row">
                  <span class="exec-stat-label">成功率</span>
                  <span class="exec-stat-val" :style="{ color: getScoreColor(execOverview.total > 0 ? (execOverview.success / execOverview.total * 100) : 0) }">
                    {{ execOverview.total > 0 ? Math.round(execOverview.success / execOverview.total * 100) : 0 }}%
                  </span>
                </div>
                <div class="exec-stat-row">
                  <span class="exec-stat-label">失败率</span>
                  <span class="exec-stat-val" style="color: #ff4d4f">
                    {{ execOverview.total > 0 ? Math.round(execOverview.failed / execOverview.total * 100) : 0 }}%
                  </span>
                </div>
                <div class="exec-stat-row">
                  <span class="exec-stat-label">平均评分</span>
                  <span class="exec-stat-val" :style="{ color: getScoreColor(execOverview.avgScore) }">
                    {{ execOverview.avgScore || '—' }}
                  </span>
                </div>
                <div class="exec-stat-row">
                  <span class="exec-stat-label">质检方案</span>
                  <span class="exec-stat-val">{{ execOverview.planCount }} 个</span>
                </div>
                <div class="exec-stat-row">
                  <span class="exec-stat-label">覆盖层级</span>
                  <span class="exec-stat-val">{{ execOverview.layerCount }} 个</span>
                </div>
              </div>
            </Card>
          </Col>
        </Row>

        <!-- Dimension pills + layer bar -->
        <Row :gutter="16" class="chart-row">
          <Col :span="12">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">六维质量得分</span>
              </template>
              <div class="dim-grid">
                <div v-for="dim in dimensions" :key="dim.label" class="dim-item">
                  <div class="dim-label">{{ dim.label }}</div>
                  <div class="dim-value" :style="{ color: getScoreColor(dim.value) }">{{ dim.value }}</div>
                  <Progress
                    :percent="dim.value"
                    :stroke-color="getScoreColor(dim.value)"
                    :show-info="false" size="small"
                  />
                </div>
              </div>
            </Card>
          </Col>

          <Col :span="12">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">数仓分层质量对比</span>
              </template>
              <template v-if="layerStats.length > 0">
                <EchartsUI ref="layerBarRef" style="height: 200px" />
              </template>
              <Empty v-else description="暂无分层数据" style="margin-top: 32px"/>
            </Card>
          </Col>
        </Row>

        <!-- Table score ranking -->
        <Row :gutter="16" class="chart-row" v-if="trendData.length > 0">
          <Col :span="24">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">表质量排行 TOP 10</span>
              </template>
              <template #extra>
                <span style="font-size: 12px; color: #8c8c8c">按综合评分从高到低</span>
              </template>
              <EchartsUI ref="barRef" style="height: 220px" />
            </Card>
          </Col>
        </Row>
      </div>

      <!-- ── EXECUTIONS TAB ── -->
      <div v-show="activeTab === 'executions'">
        <Card class="table-card" :bordered="false">
          <Table
            :columns="execColumns"
            :data-source="filteredExecutions"
            :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
            size="small"
            row-key="id"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <Tag :color="getStatusColor(record.status)">{{ getStatusLabel(record.status) }}</Tag>
              </template>
              <template v-else-if="column.key === 'triggerType'">
                <Tag>{{ record.triggerType === 'SCHEDULE' ? '定时' : record.triggerType === 'MANUAL' ? '手动' : record.triggerType || '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'overallScore'">
                <Tag :color="getScoreTagColor(record.overallScore)">
                  {{ record.overallScore ?? '—' }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'ruleResult'">
                <Tooltip :title="`通过 ${record.passedCount || 0} 条 / 失败 ${record.failedCount || 0} 条`">
                  <div style="display: flex; align-items: center; gap: 8px">
                    <Progress
                      :percent="(record.totalRules || 0) > 0 ? Math.round(((record.passedCount || 0) / (record.totalRules || 1)) * 100) : 0"
                      :stroke-color="getScoreColor((record.totalRules || 0) > 0 ? ((record.passedCount || 0) / (record.totalRules || 1)) * 100 : 0)"
                      size="small" style="width: 80px"
                    />
                    <span style="font-size: 12px; color: #8c8c8c">
                      {{ record.passedCount || 0 }}/{{ record.totalRules || 0 }}
                    </span>
                  </div>
                </Tooltip>
              </template>
              <template v-else-if="column.key === 'elapsed'">
                {{ fmtElapsed(record.elapsedMs) }}
              </template>
              <template v-else-if="column.key === 'startTime'">
                {{ record.startTime ? record.startTime.replace('T', ' ').slice(0, 16) : '—' }}
              </template>
            </template>
          </Table>
        </Card>
      </div>

      <!-- ── DETAIL TAB ── -->
      <div v-show="activeTab === 'detail'">

        <!-- Filters -->
        <Card class="filter-card" :bordered="false">
          <Row :gutter="16" align="middle">
            <Col>
              <span class="filter-label">数仓层级：</span>
              <a-select v-model="filterLayer" style="width: 130px" placeholder="全部" allow-clear :options="layerOptions"/>
            </Col>
            <Col>
              <span class="filter-label">执行状态：</span>
              <a-select v-model="filterStatus" style="width: 130px" placeholder="全部" allow-clear>
                <a-select-option value="SUCCESS">成功</a-select-option>
                <a-select-option value="FAILED">失败</a-select-option>
                <a-select-option value="PARTIAL">部分失败</a-select-option>
                <a-select-option value="RUNNING">运行中</a-select-option>
              </a-select>
            </Col>
            <Col>
              <a-button @click="() => { filterLayer = ''; filterStatus = '' }">重置</a-button>
            </Col>
            <Col style="margin-left: auto">
              <span style="font-size: 13px; color: #8c8c8c">
                共 {{ filteredExecutions.length }} 条执行记录
              </span>
            </Col>
          </Row>
        </Card>

        <!-- Summary -->
        <Row :gutter="16" class="kpi-row">
          <Col :span="4">
            <Card class="kpi-stat-card" :bordered="false">
              <Statistic title="平均评分" :value="execOverview.avgScore" suffix="分" :value-style="{ color: getScoreColor(execOverview.avgScore), fontSize: '28px' }"/>
            </Card>
          </Col>
          <Col :span="4">
            <Card class="kpi-stat-card" :bordered="false">
              <Statistic title="平均通过率" :value="execOverview.avgPassRate" suffix="%" :value-style="{ color: getScoreColor(execOverview.avgPassRate), fontSize: '28px' }"/>
            </Card>
          </Col>
          <Col :span="4">
            <Card class="kpi-stat-card" :bordered="false">
              <Statistic title="执行总数" :value="execOverview.total" :value-style="{ fontSize: '28px' }"/>
            </Card>
          </Col>
          <Col :span="4">
            <Card class="kpi-stat-card" :bordered="false">
              <Statistic title="质检规则" :value="execOverview.totalRules" :value-style="{ fontSize: '28px' }"/>
            </Card>
          </Col>
          <Col :span="4">
            <Card class="kpi-stat-card" :bordered="false">
              <Statistic title="成功数" :value="execOverview.success" :value-style="{ color: '#52c41a', fontSize: '28px' }"/>
            </Card>
          </Col>
          <Col :span="4">
            <Card class="kpi-stat-card" :bordered="false">
              <Statistic title="失败数" :value="execOverview.failed" :value-style="{ color: execOverview.failed > 0 ? '#ff4d4f' : '#52c41a', fontSize: '28px' }"/>
            </Card>
          </Col>
        </Row>

        <!-- Layer breakdown -->
        <Row :gutter="16" class="chart-row" v-if="layerStats.length > 0">
          <Col :span="24">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">各数仓层级质量详情</span>
              </template>
              <Descriptions :column="layerStats.length" size="small" bordered>
                <DescriptionsItem
                  v-for="ls in layerStats"
                  :key="ls.layer"
                  :label="ls.layer"
                >
                  <div class="layer-desc">
                    <Tag :color="getScoreTagColor(ls.avgScore)">{{ ls.avgScore }}分</Tag>
                    <span style="font-size: 12px; color: #8c8c8c">通过率 {{ ls.passRate }}%</span>
                    <span style="font-size: 12px; color: #8c8c8c">执行 {{ ls.execCount }} 次</span>
                  </div>
                </DescriptionsItem>
              </Descriptions>
            </Card>
          </Col>
        </Row>

        <!-- Table -->
        <Card class="table-card" :bordered="false">
          <Table
            :columns="execColumns"
            :data-source="filteredExecutions"
            :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
            size="small"
            row-key="id"
            :scroll="{ x: 1100 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <Tag :color="getStatusColor(record.status)">{{ getStatusLabel(record.status) }}</Tag>
              </template>
              <template v-else-if="column.key === 'triggerType'">
                <Tag>{{ record.triggerType === 'SCHEDULE' ? '定时' : record.triggerType === 'MANUAL' ? '手动' : record.triggerType || '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'overallScore'">
                <Tag :color="getScoreTagColor(record.overallScore)">{{ record.overallScore ?? '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'ruleResult'">
                <div style="display: flex; align-items: center; gap: 8px">
                  <Progress
                    :percent="(record.totalRules || 0) > 0 ? Math.round(((record.passedCount || 0) / (record.totalRules || 1)) * 100) : 0"
                    :stroke-color="getScoreColor((record.totalRules || 0) > 0 ? ((record.passedCount || 0) / (record.totalRules || 1)) * 100 : 0)"
                    size="small" style="width: 80px"
                  />
                  <span style="font-size: 12px; color: #8c8c8c">{{ record.passedCount || 0 }}/{{ record.totalRules || 0 }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'elapsed'">
                {{ fmtElapsed(record.elapsedMs) }}
              </template>
              <template v-else-if="column.key === 'startTime'">
                {{ record.startTime ? record.startTime.replace('T', ' ').slice(0, 16) : '—' }}
              </template>
            </template>
          </Table>
        </Card>
      </div>

    </Spin>
  </Page>
</template>

<style scoped>
/* ── Header ───────────────────────────────────────── */
.dq-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #0d1b4b 0%, #1a3a8f 60%, #0d47a1 100%);
  border-radius: 12px;
  margin-bottom: 0;
  box-shadow: 0 4px 20px rgba(13, 27, 75, 0.35);
}

.dq-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dq-icon {
  flex-shrink: 0;
}

.dq-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.dq-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot-sep {
  color: rgba(255, 255, 255, 0.3);
}

.dq-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── Tabs ─────────────────────────────────────────── */
.dq-tabs {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.tab-label {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
}

/* ── KPI ──────────────────────────────────────────── */
.kpi-row {
  margin-bottom: 12px;
}

.kpi-main-card {
  border-radius: 14px;
  border: 1px solid #dde3f5;
  background: linear-gradient(160deg, #f0f4ff 0%, #e8eef8 100%);
  text-align: center;
  padding: 4px 8px 12px;
  min-height: 180px;
}

.kpi-gauge-wrap {
  display: flex;
  justify-content: center;
}

.kpi-gauge-label {
  font-size: 14px;
  font-weight: 700;
  margin-top: -8px;
}

.kpi-gauge-title {
  font-size: 13px;
  color: #555;
  margin-top: 2px;
}

.kpi-gauge-source {
  font-size: 11px;
  color: #aaa;
  margin-top: 4px;
}

.kpi-stat-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 8px 12px;
  min-height: 140px;
}

.stat-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}

.stat-icon--green { background: linear-gradient(135deg, #52c41a, #73d13d); }
.stat-icon--blue { background: linear-gradient(135deg, #5470c6, #91cc75); }
.stat-icon--orange { background: linear-gradient(135deg, #fa8c16, #faad14); }
.stat-icon--purple { background: linear-gradient(135deg, #722ed1, #9254de); }
.stat-icon--red { background: linear-gradient(135deg, #ff4d4f, #ff7875); }
.stat-icon--gray { background: linear-gradient(135deg, #d9d9d9, #e8e8e8); }
.stat-icon--teal { background: linear-gradient(135deg, #13c2c2, #36cfc9); }

.kpi-alert-active {
  border-color: #ffccc7 !important;
  background: #fffaf5 !important;
}

.stat-sub-text {
  font-size: 11px;
  margin-top: 4px;
}

/* ── Exec status chips ────────────────────────────── */
.exec-status-row {
  margin-bottom: 12px;
}

.exec-status-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.exec-chip--success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.exec-chip--partial { background: #fffbe6; color: #fa8c16; border: 1px solid #ffe58f; }
.exec-chip--failed { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.exec-chip--running { background: #f0f5ff; color: #1890ff; border: 1px solid #adc6ff; }

/* ── Charts ────────────────────────────────────────── */
.chart-row {
  margin-bottom: 12px;
}

.chart-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  display: flex;
  align-items: center;
}

/* ── Dimension grid ────────────────────────────────── */
.dim-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.dim-item {
  padding: 8px 4px;
  border-radius: 8px;
  background: #fafafa;
  text-align: center;
}

.dim-label {
  font-size: 12px;
  color: #8c8c8c;
}

.dim-value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

/* ── Timeline ──────────────────────────────────────── */
.timeline-item {
  padding-bottom: 4px;
}

.timeline-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.timeline-plan {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.timeline-layer {
  font-size: 11px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 10px;
}

.timeline-bottom {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.timeline-score {
  font-weight: 600;
}

/* ── Exec stats empty state ───────────────────────── */
.exec-stats-empty {
  padding: 8px 0;
}

.exec-stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.exec-stat-row:last-child {
  border-bottom: none;
}

.exec-stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.exec-stat-val {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

/* ── Filter ───────────────────────────────────────── */
.filter-card {
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.filter-label {
  font-size: 13px;
  color: #555;
  margin-right: 4px;
}

/* ── Table ─────────────────────────────────────────── */
.table-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

/* ── Layer description ─────────────────────────────── */
.layer-desc {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
</style>
