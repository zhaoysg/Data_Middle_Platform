<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import {
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Empty,
  Popconfirm,
  Progress,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tabs,
  Timeline,
  Tooltip,
} from 'ant-design-vue';
import {
  DownloadOutlined,
  ReloadOutlined,
  StopOutlined,
} from '@ant-design/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';

import {
  dqcExecutionList,
  dqcExecutionRerun,
  dqcExecutionStop,
} from '#/api/metadata/dqc/execution';

/** 内联评分趋势接口（已合并到驾驶舱，不再独立导出） */
async function dqcScoreTrend(params?: { days?: number }) {
  const url = '/system/metadata/dqc/score/trend';
  const query = params?.days ? `?days=${params.days}` : '';
  const res = await fetch(`${url}${query}`, { credentials: 'include' });
  if (!res.ok) return [];
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

const loading = ref(false);
const days = ref(30);
const activeTab = ref('overview');
const trendData = ref([]);
const executionData = ref([]);

// Filter (detail / execution tab)
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

const dayOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 14 天', value: 14 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 },
];

const executionStatusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '部分失败', value: 'PARTIAL' },
  { label: '运行中', value: 'RUNNING' },
];

// ── Helpers ──────────────────────────────────────────────────────

function getScoreColor(score?: number | null) {
  if (score === undefined || score === null) return '#9aa3b5';
  if (score >= 90) return '#52c41a';
  if (score >= 70) return '#faad14';
  return '#ff4d4f';
}

function getScoreTagColor(score?: number | null) {
  if (score === undefined || score === null) return 'default';
  if (score >= 90) return 'success';
  if (score >= 70) return 'warning';
  return 'error';
}

function getScoreLabel(score?: number | null) {
  if (score === undefined || score === null) return '暂无数据';
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
    case 'STOPPED': return 'default';
    default: return 'default';
  }
}

function getStatusLabel(status?: string) {
  switch (status) {
    case 'SUCCESS': return '成功';
    case 'FAILED': return '失败';
    case 'PARTIAL': return '部分失败';
    case 'RUNNING': return '运行中';
    case 'STOPPED': return '已停止';
    default: return status || '—';
  }
}

function avg(list: DqcQualityScore[], field: keyof DqcQualityScore): number {
  const vals = list
    .map((r) => r[field])
    .filter((v): v is number => typeof v === 'number' && Number.isFinite(v));
  if (vals.length === 0) return 0;
  return Math.round((vals.reduce((s, v) => s + v, 0) / vals.length) * 10) / 10;
}

function avgExec(list: DqcExecution[], field: keyof DqcExecution): number {
  const vals = list
    .filter((r) => r.status !== 'RUNNING')
    .map((r) => r[field as keyof DqcExecution])
    .filter((v): v is number => typeof v === 'number' && Number.isFinite(v));
  if (vals.length === 0) return 0;
  return Math.round((vals.reduce((s, v) => s + v, 0) / vals.length) * 10) / 10;
}

function fmtElapsed(ms?: number) {
  if (ms === undefined || ms === null) return '—';
  if (ms < 60000) return `${Math.round(ms / 1000)}秒`;
  return `${Math.floor(ms / 60000)}分${Math.round((ms % 60000) / 1000)}秒`;
}

function layerTagColor(layer?: string) {
  if (!layer) return 'default';
  const map: Record<string, string> = {
    ODS: 'orange',
    DWD: 'blue',
    DWS: 'cyan',
    ADS: 'purple',
    DIM: 'geekblue',
    ALL: 'default',
  };
  return map[layer.toUpperCase()] || 'processing';
}

// ── Exec stats ───────────────────────────────────────────────────

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

// ── Score overview ──────────────────────────────────────────────

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

const effectiveOverall = computed(() => scoreOverview.value.avgOverall || execOverview.value.avgScore);
const effectivePassRate = computed(() => scoreOverview.value.avgPassRate || execOverview.value.avgPassRate);
const effectiveAlert = computed(() => scoreOverview.value.alertCount || execOverview.value.alertCount);

// ── Six dimensions ──────────────────────────────────────────────

const dimensions = computed(() => {
  const data = trendData.value;
  const defs = [
    { label: '完整性', field: 'completenessScore' as keyof DqcQualityScore, accent: 'dim-accent--blue', icon: 'shield' as const },
    { label: '唯一性', field: 'uniquenessScore' as keyof DqcQualityScore, accent: 'dim-accent--green', icon: 'lock' as const },
    { label: '准确性', field: 'accuracyScore' as keyof DqcQualityScore, accent: 'dim-accent--orange', icon: 'bolt' as const },
    { label: '一致性', field: 'consistencyScore' as keyof DqcQualityScore, accent: 'dim-accent--purple', icon: 'swap' as const },
    { label: '及时性', field: 'timelinessScore' as keyof DqcQualityScore, accent: 'dim-accent--teal', icon: 'clock' as const },
    { label: '有效性', field: 'validityScore' as keyof DqcQualityScore, accent: 'dim-accent--red', icon: 'check' as const },
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

// ── Execution history ────────────────────────────────────────────

const recentExecutions = computed(() => {
  return [...executionData.value]
    .sort((a, b) => {
      const ta = a.startTime ? new Date(a.startTime).getTime() : 0;
      const tb = b.startTime ? new Date(b.startTime).getTime() : 0;
      return tb - ta;
    })
    .slice(0, 10);
});

// ── Filtered executions ─────────────────────────────────────────

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

const layerOptions = computed(() => {
  const set = new Set(executionData.value.map((r) => r.layerCode).filter(Boolean));
  return [...set].map((v) => ({ label: v, value: v }));
});

// ── Chart options ────────────────────────────────────────────────

// Gauge (质量评分页面样式：渐变色进度条)
const gaugeOption = computed(() => {
  const score = effectiveOverall.value;
  const has = score > 0;
  const color = getScoreColor(score);
  const lineWidth = 14;
  return {
    series: [{
      type: 'gauge',
      center: ['50%', '60%'],
      radius: '92%',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      splitNumber: 5,
      pointer: { show: false },
      axisLine: {
        roundCap: true,
        lineStyle: {
          width: lineWidth,
          color: has
            ? [
                [0.25, '#ff7875'],
                [0.5, '#ffd666'],
                [0.78, '#b7eb8f'],
                [1, '#389e0d'],
              ]
            : [[1, '#e4e8f0']],
        },
      },
      progress: {
        show: has,
        roundCap: true,
        width: lineWidth,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: has
              ? [
                  { offset: 0, color: '#ff7875' },
                  { offset: 0.45, color: '#faad14' },
                  { offset: 1, color: '#52c41a' },
                ]
              : [{ offset: 0, color: '#e4e8f0' }, { offset: 1, color: '#e4e8f0' }],
          },
        },
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: has,
        offsetCenter: [0, '2%'],
        fontSize: has ? 42 : 30,
        fontWeight: 700,
        formatter: (val: number | string) => {
          if (!has) return '—';
          const n = typeof val === 'number' ? val : Number(val);
          return Number.isFinite(n) ? n.toFixed(1) : '—';
        },
        color,
      },
      data: [{ value: has ? Math.min(100, Math.max(0, score)) : 0 }],
    }],
  };
});

// Trend
const trendOption = computed(() => {
  if (trendData.value.length === 0) return {};
  const dates = trendData.value.map((r) => r.checkDate?.slice(5) || '');
  const scores = trendData.value.map((r) => r.overallScore || 0);
  const passRates = trendData.value.map((r) => r.rulePassRate || 0);
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['综合评分', '规则通过率'], bottom: 0, textStyle: { color: '#64748b', fontSize: 11 } },
    grid: { top: 14, right: 20, bottom: 44, left: 52, containLabel: false },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { fontSize: 11, color: '#94a3b8' },
    },
    yAxis: [
      {
        type: 'value', name: '评分', min: 0, max: 100,
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { formatter: '{value}', color: '#94a3b8', fontSize: 11 },
        nameTextStyle: { color: '#94a3b8', fontSize: 11, padding: [0, 0, 0, 8] },
      },
      {
        type: 'value', name: '通过率', min: 0, max: 100,
        splitLine: { show: false },
        axisLabel: { formatter: '{value}%', color: '#94a3b8', fontSize: 11 },
        nameTextStyle: { color: '#94a3b8', fontSize: 11, padding: [0, 8, 0, 0] },
      },
    ],
    series: [
      {
        name: '综合评分', type: 'line', data: scores, smooth: 0.35,
        symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2.5, cap: 'round', join: 'round' },
        itemStyle: { color: '#4f6ef7', borderColor: '#fff', borderWidth: 1 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(79,110,247,0.22)' },
              { offset: 1, color: 'rgba(79,110,247,0.02)' },
            ],
          },
        },
      },
      {
        name: '规则通过率', type: 'line', yAxisIndex: 1, data: passRates, smooth: 0.35,
        symbol: 'circle', symbolSize: 5,
        lineStyle: { width: 2, type: 'dashed', cap: 'round' },
        itemStyle: { color: '#34b27b', borderColor: '#fff', borderWidth: 1 },
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
        { name: '及时性', max: 100 }, { name: '有效性', max: 100 },
      ],
      radius: '68%',
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
        areaStyle: { color: 'rgba(79,110,247,0.18)' },
        lineStyle: { color: '#4f6ef7', width: 2 },
        itemStyle: { color: '#4f6ef7' },
      }],
    }],
  };
});

// Layer bar
const layerBarOption = computed(() => {
  if (layerStats.value.length === 0) return {};
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['平均评分', '通过率(%)'], bottom: 0 },
    grid: { top: 8, right: 60, bottom: 44, left: 10, containLabel: true },
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
        itemStyle: { color: '#34b27b' },
        label: { show: true, position: 'bottom', formatter: '{c}%', fontSize: 10, color: '#34b27b' },
      },
    ],
  };
});

// Score bar (top tables)
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

// ── Exec table columns ──────────────────────────────────────────

const execColumns = [
  { title: '执行编号', dataIndex: 'executionNo', key: 'executionNo', width: 170, ellipsis: true },
  { title: '质检方案', dataIndex: 'planName', key: 'planName', width: 160, ellipsis: true },
  { title: '数据层', dataIndex: 'layerCode', key: 'layerCode', width: 80, align: 'center' as const },
  { title: '触发', dataIndex: 'triggerType', key: 'triggerType', width: 80, align: 'center' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' as const },
  { title: '综合评分', dataIndex: 'overallScore', key: 'overallScore', width: 110, align: 'center' as const },
  { title: '规则通过', key: 'ruleResult', width: 140, align: 'center' as const },
  { title: '耗时', key: 'elapsed', width: 90, align: 'center' as const },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const, align: 'center' as const },
];

// ── Export report ────────────────────────────────────────────────

function exportReport() {
  const rows = trendData.value;
  if (rows.length === 0) {
    return;
  }
  const map = new Map<string, DqcQualityScore>();
  for (const r of rows) {
    const key = `${r.targetDsId}-${r.targetTable}`;
    if (!map.has(key)) {
      map.set(key, r);
    } else {
      const existing = map.get(key)!;
      if (r.checkDate && existing.checkDate && r.checkDate > existing.checkDate) {
        map.set(key, r);
      }
    }
  }
  const tableList = [...map.values()];
  const headers = [
    '数据层', '数据源', '目标表', '综合评分', '规则通过率',
    '通过数', '规则总数', '完整性', '唯一性', '准确性',
    '一致性', '及时性', '有效性', '检查日期',
  ];
  const lines = tableList.map((r) => {
    const pass = r.rulePassCount !== undefined && r.rulePassCount !== null
      ? r.rulePassCount
      : Math.max(0, (r.ruleTotalCount || 0) - (r.ruleFailCount || 0));
    return [
      r.layerCode || '',
      r.dsName || '未知数据源',
      r.targetTable || '',
      r.overallScore ?? '',
      r.rulePassRate ?? '',
      pass,
      r.ruleTotalCount || 0,
      r.completenessScore ?? '',
      r.uniquenessScore ?? '',
      r.accuracyScore ?? '',
      r.consistencyScore ?? '',
      r.timelinessScore ?? '',
      r.validityScore ?? '',
      r.checkDate || '',
    ].map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',');
  });
  const bom = '\uFEFF';
  const csv = bom + [headers.join(','), ...lines].join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  const filename = `质量评分报告_${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}.csv`;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// ── Data loading ────────────────────────────────────────────────

async function loadData() {
  loading.value = true;
  try {
    const [scoreRes, execRes] = await Promise.all([
      dqcScoreTrend({ days: days.value }).catch(() => []),
      dqcExecutionList({ pageNum: 1, pageSize: 500 }).catch(() => ({ rows: [] })),
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
  void loadData();
  setTimeout(() => renderCharts(), 200);
});

// ── Execution actions ────────────────────────────────────────────

async function handleRerun(row: DqcExecution) {
  await dqcExecutionRerun(row.id!);
  await loadData();
}

async function handleStop(row: DqcExecution) {
  await dqcExecutionStop(row.id!);
  await loadData();
}
</script>

<template>
  <Page :auto-content-height="true">
    <Spin :spinning="loading">

      <!-- 页头 -->
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
          <Button @click="exportReport" :disabled="trendData.length === 0" title="导出 CSV 报告">
            <template #icon><DownloadOutlined /></template>
            导出报告
          </Button>
          <Select
            v-model:value="days"
            :options="dayOptions"
            style="width: 120px"
            @change="loadData"
          />
          <Button type="primary" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </Button>
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
          <!-- Gauge -->
          <Col :span="5">
            <Card class="kpi-main-card" :bordered="false">
              <div class="kpi-gauge-wrap">
                <EchartsUI ref="gaugeRef" style="height: 150px" />
              </div>
              <div class="kpi-gauge-label" :style="{ color: getScoreColor(effectiveOverall) }">
                {{ getScoreLabel(effectiveOverall) }}
              </div>
              <div class="kpi-gauge-title">综合质量评分</div>
              <div class="kpi-gauge-meta">
                <span>规则通过率 <strong :style="{ color: getScoreColor(effectivePassRate) }">{{ effectivePassRate }}%</strong></span>
              </div>
            </Card>
          </Col>
          <!-- Stats -->
          <Col :span="19">
            <Row :gutter="16" style="height: 100%">
              <Col :span="4">
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--green">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M10 2 L12.5 7.5 L18 8 L14 12 L15 18 L10 15 L5 18 L6 12 L2 8 L7.5 7.5 Z" fill="#fff" opacity="0.9"/>
                    </svg>
                  </div>
                  <Statistic title="平均通过率" :value="effectivePassRate" suffix="%" :value-style="{ color: getScoreColor(effectivePassRate), fontSize: '26px' }" />
                  <Progress :percent="effectivePassRate" :stroke-color="getScoreColor(effectivePassRate)" :show-info="false" size="small" style="margin-top: 6px" />
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
                <Card class="kpi-stat-card" :bordered="false">
                  <div class="stat-icon-wrap stat-icon--red">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M10 4 L17 16 L3 16 Z" stroke="#fff" stroke-width="1.5" opacity="0.9"/>
                      <path d="M10 8 L10 12" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
                      <circle cx="10" cy="14" r="0.8" fill="#fff"/>
                    </svg>
                  </div>
                  <Statistic title="预警数" :value="effectiveAlert" :value-style="{ color: effectiveAlert > 0 ? '#ff4d4f' : '#52c41a', fontSize: '26px' }" />
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
                  <Statistic title="质检方案数" :value="execOverview.planCount" :value-style="{ fontSize: '26px' }"/>
                  <div class="stat-sub-text" style="color: #8c8c8c">覆盖 {{ execOverview.layerCount }} 个层级</div>
                </Card>
              </Col>
            </Row>
          </Col>
        </Row>

        <!-- Exec status chips -->
        <Row :gutter="12" class="exec-status-row">
          <Col :span="3">
            <div class="exec-status-chip exec-chip--success"><Badge status="success"/>成功 {{ execOverview.success }}</div>
          </Col>
          <Col :span="3">
            <div class="exec-status-chip exec-chip--partial"><Badge status="warning"/>部分失败 {{ execOverview.partial }}</div>
          </Col>
          <Col :span="3">
            <div class="exec-status-chip exec-chip--failed"><Badge status="error"/>失败 {{ execOverview.failed }}</div>
          </Col>
          <Col :span="3">
            <div class="exec-status-chip exec-chip--running"><Badge status="processing"/>运行中 {{ execOverview.running }}</div>
          </Col>
        </Row>

        <!-- Main charts -->
        <Row :gutter="16" class="chart-row">
          <!-- Trend -->
          <Col :span="15">
            <Card class="chart-card" :bordered="false">
              <template #title>
                <span class="chart-title">
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style="margin-right: 6px">
                    <path d="M2 11 L5 7 L8 9 L12 4" stroke="#4f6ef7" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  {{ trendData.length > 0 ? '评分与通过率趋势' : '最近执行记录' }}
                </span>
              </template>
              <template #extra><span style="font-size: 12px; color: #8c8c8c">近 {{ days }} 天</span></template>
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
                      <span class="timeline-score" v-if="exec.overallScore" :style="{ color: getScoreColor(exec.overallScore) }">评分 {{ exec.overallScore }}分</span>
                      <span class="timeline-rules" v-if="exec.totalRules">{{ exec.passedCount || 0 }}/{{ exec.totalRules }} 规则通过</span>
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
                    <circle cx="7" cy="7" r="5.5" stroke="#4f6ef7" stroke-width="1.5"/>
                    <circle cx="7" cy="7" r="3" stroke="#4f6ef7" stroke-width="1" opacity="0.5"/>
                    <circle cx="7" cy="7" r="1" fill="#4f6ef7"/>
                  </svg>
                  {{ trendData.length > 0 ? '六维质量雷达' : '执行统计' }}
                </span>
              </template>
              <template v-if="trendData.length > 0">
                <EchartsUI ref="radarRef" style="height: 260px" />
              </template>
              <div v-else class="exec-stats-empty">
                <div class="exec-stat-row"><span class="exec-stat-label">总执行</span><span class="exec-stat-val">{{ execOverview.total }}</span></div>
                <div class="exec-stat-row"><span class="exec-stat-label">成功率</span><span class="exec-stat-val" :style="{ color: getScoreColor(execOverview.total > 0 ? (execOverview.success / execOverview.total * 100) : 0) }">{{ execOverview.total > 0 ? Math.round((execOverview.success / execOverview.total) * 100) : 0 }}%</span></div>
                <div class="exec-stat-row"><span class="exec-stat-label">失败率</span><span class="exec-stat-val" style="color: #ff4d4f">{{ execOverview.total > 0 ? Math.round((execOverview.failed / execOverview.total) * 100) : 0 }}%</span></div>
                <div class="exec-stat-row"><span class="exec-stat-label">平均评分</span><span class="exec-stat-val" :style="{ color: getScoreColor(execOverview.avgScore) }">{{ execOverview.avgScore || '—' }}</span></div>
                <div class="exec-stat-row"><span class="exec-stat-label">质检方案</span><span class="exec-stat-val">{{ execOverview.planCount }} 个</span></div>
                <div class="exec-stat-row"><span class="exec-stat-label">覆盖层级</span><span class="exec-stat-val">{{ execOverview.layerCount }} 个</span></div>
              </div>
            </Card>
          </Col>
        </Row>

        <!-- Six dimension cards + layer bar -->
        <Row :gutter="16" class="chart-row">
          <!-- Six dim cards (质量评分样式) -->
          <Col :span="12">
            <Card class="chart-card" :bordered="false">
              <template #title><span class="chart-title">六维质量得分</span></template>
              <div class="dim-grid">
                <div v-for="dim in dimensions" :key="dim.label" class="dim-item" :class="dim.accent">
                  <div class="dim-item-icon" aria-hidden="true">
                    <svg v-if="dim.icon === 'shield'" class="dim-svg" viewBox="0 0 24 24" fill="none"><path d="M12 3L5 6v6c0 4.5 3.5 8 7 9 3.5-1 7-4.5 7-9V6l-7-3z" stroke="currentColor" stroke-width="1.6"/></svg>
                    <svg v-else-if="dim.icon === 'lock'" class="dim-svg" viewBox="0 0 24 24" fill="none"><rect x="5" y="10" width="14" height="11" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M8 10V7a4 4 0 018 0v3" stroke="currentColor" stroke-width="1.6"/></svg>
                    <svg v-else-if="dim.icon === 'bolt'" class="dim-svg" viewBox="0 0 24 24" fill="none"><path d="M13 2L4 14h7l-1 8 10-12h-7l0-8z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/></svg>
                    <svg v-else-if="dim.icon === 'swap'" class="dim-svg" viewBox="0 0 24 24" fill="none"><path d="M7 7h13M17 3l4 4-4 4M17 17H4M7 21l-4-4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                    <svg v-else-if="dim.icon === 'clock'" class="dim-svg" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M12 7v6l4 2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
                    <svg v-else class="dim-svg" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6"/><path d="M8 12l2.5 2.5L16 9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
                  </div>
                  <div class="dim-label">{{ dim.label }}</div>
                  <div class="dim-value" :style="trendData.length > 0 ? { color: getScoreColor(dim.value) } : { color: '#a8b0c2' }">
                    {{ trendData.length > 0 ? dim.value.toFixed(1) : '—' }}
                  </div>
                </div>
              </div>
            </Card>
          </Col>
          <!-- Layer bar -->
          <Col :span="12">
            <Card class="chart-card" :bordered="false">
              <template #title><span class="chart-title">数仓分层质量对比</span></template>
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
              <template #title><span class="chart-title">表质量排行 TOP 10</span></template>
              <template #extra><span style="font-size: 12px; color: #8c8c8c">按综合评分从高到低</span></template>
              <EchartsUI ref="barRef" style="height: 220px" />
            </Card>
          </Col>
        </Row>
      </div>

      <!-- ── EXECUTIONS TAB ── -->
      <div v-show="activeTab === 'executions'">
        <Card class="table-card" :bordered="false">
          <template #extra><span style="font-size: 13px; color: #8c8c8c">共 {{ filteredExecutions.length }} 条记录</span></template>
          <Table
            :columns="execColumns"
            :data-source="filteredExecutions"
            :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
            size="small"
            row-key="id"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'layerCode'">
                <Tag :color="layerTagColor(record.layerCode)">{{ record.layerCode || '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'triggerType'">
                <Tag>{{ record.triggerType === 'SCHEDULE' ? '定时' : record.triggerType === 'MANUAL' ? '手动' : record.triggerType || '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="getStatusColor(record.status)">{{ getStatusLabel(record.status) }}</Tag>
              </template>
              <template v-else-if="column.key === 'overallScore'">
                <Tag :color="getScoreTagColor(record.overallScore)">{{ record.overallScore ?? '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'ruleResult'">
                <Tooltip :title="`通过 ${record.passedCount || 0} 条 / 失败 ${record.failedCount || 0} 条`">
                  <div style="display: flex; align-items: center; gap: 8px">
                    <Progress :percent="(record.totalRules || 0) > 0 ? Math.round(((record.passedCount || 0) / (record.totalRules || 1)) * 100) : 0" :stroke-color="getScoreColor((record.totalRules || 0) > 0 ? ((record.passedCount || 0) / (record.totalRules || 1)) * 100 : 0)" size="small" style="width: 80px" />
                    <span style="font-size: 12px; color: #8c8c8c">{{ record.passedCount || 0 }}/{{ record.totalRules || 0 }}</span>
                  </div>
                </Tooltip>
              </template>
              <template v-else-if="column.key === 'elapsed'">{{ fmtElapsed(record.elapsedMs) }}</template>
              <template v-else-if="column.key === 'startTime'">{{ record.startTime ? record.startTime.replace('T', ' ').slice(0, 16) : '—' }}</template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Popconfirm v-if="['FAILED', 'SUCCESS', 'PARTIAL', 'STOPPED'].includes(record.status || '')" title="确认重新执行？" @confirm="handleRerun(record)">
                    <Button type="link" size="small">重新执行</Button>
                  </Popconfirm>
                  <Popconfirm v-if="record.status === 'RUNNING'" title="确认停止执行？" @confirm="handleStop(record)">
                    <Button type="link" size="small" danger><StopOutlined /></Button>
                  </Popconfirm>
                </Space>
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
              <Select v-model:value="filterLayer" style="width: 130px" placeholder="全部" allow-clear :options="layerOptions" />
            </Col>
            <Col>
              <span class="filter-label">执行状态：</span>
              <Select v-model:value="filterStatus" style="width: 130px" placeholder="全部" allow-clear :options="executionStatusOptions" />
            </Col>
            <Col>
              <Button @click="() => { filterLayer = ''; filterStatus = '' }">重置</Button>
            </Col>
            <Col style="margin-left: auto">
              <span style="font-size: 13px; color: #8c8c8c">共 {{ filteredExecutions.length }} 条执行记录</span>
            </Col>
          </Row>
        </Card>

        <!-- Summary -->
        <Row :gutter="16" class="kpi-row">
          <Col :span="4"><Card class="kpi-stat-card" :bordered="false"><Statistic title="平均评分" :value="execOverview.avgScore" suffix="分" :value-style="{ color: getScoreColor(execOverview.avgScore), fontSize: '28px' }"/></Card></Col>
          <Col :span="4"><Card class="kpi-stat-card" :bordered="false"><Statistic title="平均通过率" :value="execOverview.avgPassRate" suffix="%" :value-style="{ color: getScoreColor(execOverview.avgPassRate), fontSize: '28px' }"/></Card></Col>
          <Col :span="4"><Card class="kpi-stat-card" :bordered="false"><Statistic title="执行总数" :value="execOverview.total" :value-style="{ fontSize: '28px' }"/></Card></Col>
          <Col :span="4"><Card class="kpi-stat-card" :bordered="false"><Statistic title="质检规则" :value="execOverview.totalRules" :value-style="{ fontSize: '28px' }"/></Card></Col>
          <Col :span="4"><Card class="kpi-stat-card" :bordered="false"><Statistic title="成功数" :value="execOverview.success" :value-style="{ color: '#52c41a', fontSize: '28px' }"/></Card></Col>
          <Col :span="4"><Card class="kpi-stat-card" :bordered="false"><Statistic title="失败数" :value="execOverview.failed" :value-style="{ color: execOverview.failed > 0 ? '#ff4d4f' : '#52c41a', fontSize: '28px' }"/></Card></Col>
        </Row>

        <!-- Layer breakdown -->
        <Row :gutter="16" class="chart-row" v-if="layerStats.length > 0">
          <Col :span="24">
            <Card class="chart-card" :bordered="false">
              <template #title><span class="chart-title">各数仓层级质量详情</span></template>
              <Descriptions :column="layerStats.length" size="small" bordered>
                <DescriptionsItem v-for="ls in layerStats" :key="ls.layer" :label="ls.layer">
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
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'layerCode'">
                <Tag :color="layerTagColor(record.layerCode)">{{ record.layerCode || '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'triggerType'">
                <Tag>{{ record.triggerType === 'SCHEDULE' ? '定时' : record.triggerType === 'MANUAL' ? '手动' : record.triggerType || '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="getStatusColor(record.status)">{{ getStatusLabel(record.status) }}</Tag>
              </template>
              <template v-else-if="column.key === 'overallScore'">
                <Tag :color="getScoreTagColor(record.overallScore)">{{ record.overallScore ?? '—' }}</Tag>
              </template>
              <template v-else-if="column.key === 'ruleResult'">
                <div style="display: flex; align-items: center; gap: 8px">
                  <Progress :percent="(record.totalRules || 0) > 0 ? Math.round(((record.passedCount || 0) / (record.totalRules || 1)) * 100) : 0" :stroke-color="getScoreColor((record.totalRules || 0) > 0 ? ((record.passedCount || 0) / (record.totalRules || 1)) * 100 : 0)" size="small" style="width: 80px" />
                  <span style="font-size: 12px; color: #8c8c8c">{{ record.passedCount || 0 }}/{{ record.totalRules || 0 }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'elapsed'">{{ fmtElapsed(record.elapsedMs) }}</template>
              <template v-else-if="column.key === 'startTime'">{{ record.startTime ? record.startTime.replace('T', ' ').slice(0, 16) : '—' }}</template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Popconfirm v-if="['FAILED', 'SUCCESS', 'PARTIAL', 'STOPPED'].includes(record.status || '')" title="确认重新执行？" @confirm="handleRerun(record)">
                    <Button type="link" size="small">重新执行</Button>
                  </Popconfirm>
                  <Popconfirm v-if="record.status === 'RUNNING'" title="确认停止执行？" @confirm="handleStop(record)">
                    <Button type="link" size="small" danger><StopOutlined /></Button>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </Card>
      </div>

    </Spin>
  </Page>
</template>

<style scoped>
/* ── Header ─────────────────────────────────────────────────────── */
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
.dq-header-left { display: flex; align-items: center; gap: 12px; }
.dq-icon { flex-shrink: 0; }
.dq-title { font-size: 18px; font-weight: 700; color: #fff; letter-spacing: 1px; }
.dq-subtitle { font-size: 12px; color: rgba(255, 255, 255, 0.55); margin-top: 2px; display: flex; align-items: center; gap: 4px; }
.dot-sep { color: rgba(255, 255, 255, 0.3); }
.dq-header-right { display: flex; align-items: center; gap: 8px; }

/* ── Tabs ──────────────────────────────────────────────────────── */
.dq-tabs { background: #fff; border-bottom: 1px solid #f0f0f0; margin-bottom: 16px; }
.tab-label { display: flex; align-items: center; font-size: 14px; font-weight: 500; }

/* ── KPI ──────────────────────────────────────────────────────── */
.kpi-row { margin-bottom: 12px; }

.kpi-main-card {
  border-radius: 14px;
  border: 1px solid #dde3f5;
  background: linear-gradient(160deg, #f0f4ff 0%, #e8eef8 100%);
  text-align: center;
  padding: 4px 8px 14px;
  min-height: 220px;
}
.kpi-gauge-wrap { display: flex; justify-content: center; }
.kpi-gauge-label { font-size: 14px; font-weight: 700; margin-top: -8px; }
.kpi-gauge-title { font-size: 13px; color: #555; margin-top: 2px; }
.kpi-gauge-meta { font-size: 12px; color: #8c8c8c; margin-top: 4px; }
.kpi-gauge-meta strong { font-weight: 700; }

.kpi-stat-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 8px 12px;
  min-height: 140px;
}
.stat-icon-wrap { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-bottom: 6px; }
.stat-icon--green { background: linear-gradient(135deg, #52c41a, #73d13d); }
.stat-icon--blue { background: linear-gradient(135deg, #5470c6, #91cc75); }
.stat-icon--orange { background: linear-gradient(135deg, #fa8c16, #faad14); }
.stat-icon--purple { background: linear-gradient(135deg, #722ed1, #9254de); }
.stat-icon--red { background: linear-gradient(135deg, #ff4d4f, #ff7875); }
.stat-icon--gray { background: linear-gradient(135deg, #d9d9d9, #e8e8e8); }
.stat-icon--teal { background: linear-gradient(135deg, #13c2c2, #36cfc9); }
.stat-sub-text { font-size: 11px; margin-top: 4px; }

/* ── Exec status chips ────────────────────────────────────────── */
.exec-status-row { margin-bottom: 12px; }
.exec-status-chip { display: flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: 500; }
.exec-chip--success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.exec-chip--partial { background: #fffbe6; color: #fa8c16; border: 1px solid #ffe58f; }
.exec-chip--failed { background: #fff2f0; color: #ff4d4f; border: 1px solid #ffccc7; }
.exec-chip--running { background: #f0f5ff; color: #1890ff; border: 1px solid #adc6ff; }

/* ── Charts ───────────────────────────────────────────────────── */
.chart-row { margin-bottom: 12px; }
.chart-card { border-radius: 12px; border: 1px solid #f0f0f0; }
.chart-title { font-size: 14px; font-weight: 600; color: #1a1a1a; display: flex; align-items: center; }

/* ── Six dim grid (质量评分样式) ──────────────────────────────── */
.dim-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.dim-item {
  border-radius: 10px;
  background: #fff;
  border: 1px solid #f0f0f0;
  padding: 12px 14px;
  position: relative;
  overflow: hidden;
  min-height: 96px;
  transition: box-shadow 0.2s;
}
.dim-item:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.dim-item::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; border-radius: 0 0 10px 10px; }
.dim-accent--blue::after { background: linear-gradient(90deg, #1890ff, #69c0ff); }
.dim-accent--green::after { background: linear-gradient(90deg, #52c41a, #95de64); }
.dim-accent--orange::after { background: linear-gradient(90deg, #fa8c16, #ffc069); }
.dim-accent--purple::after { background: linear-gradient(90deg, #722ed1, #b37feb); }
.dim-accent--teal::after { background: linear-gradient(90deg, #13c2c2, #5cdbd3); }
.dim-accent--red::after { background: linear-gradient(90deg, #ff4d4f, #ff7875); }
.dim-item-icon { color: #bfbfbf; margin-bottom: 4px; }
.dim-svg { width: 22px; height: 22px; }
.dim-label { font-size: 12px; color: #8c8c8c; }
.dim-value { font-size: 24px; font-weight: 800; font-variant-numeric: tabular-nums; line-height: 1.2; margin-top: 2px; }

/* ── Timeline ─────────────────────────────────────────────────── */
.timeline-item { padding-bottom: 4px; }
.timeline-top { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; }
.timeline-plan { font-size: 13px; font-weight: 500; color: #333; }
.timeline-layer { font-size: 11px; color: #8c8c8c; background: #f5f5f5; padding: 1px 6px; border-radius: 10px; }
.timeline-bottom { display: flex; align-items: center; gap: 12px; font-size: 12px; color: #8c8c8c; }
.timeline-score { font-weight: 600; }

/* ── Exec stats empty ─────────────────────────────────────────── */
.exec-stats-empty { padding: 8px 0; }
.exec-stat-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.exec-stat-row:last-child { border-bottom: none; }
.exec-stat-label { font-size: 13px; color: #8c8c8c; }
.exec-stat-val { font-size: 15px; font-weight: 600; color: #333; }

/* ── Filter ───────────────────────────────────────────────────── */
.filter-card { border-radius: 10px; border: 1px solid #f0f0f0; margin-bottom: 12px; }
.filter-label { font-size: 13px; color: #555; margin-right: 4px; }

/* ── Table ────────────────────────────────────────────────────── */
.table-card { border-radius: 12px; border: 1px solid #f0f0f0; }

/* ── Layer description ────────────────────────────────────────── */
.layer-desc { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
</style>
