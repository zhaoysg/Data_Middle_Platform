<script setup lang="ts">
import type { DqcExecution, DqcQualityScore } from '#/api/metadata/model';
import type { EchartsUIType } from '@vben/plugins/echarts';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import {
  Button,
  Card,
  Col,
  Empty,
  Popconfirm,
  Progress,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Timeline,
  Tooltip,
} from 'ant-design-vue';
import {
  CaretDownOutlined,
  CaretUpOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
  FieldNumberOutlined,
  FontSizeOutlined,
  FundViewOutlined,
  LoadingOutlined,
  MenuOutlined,
  ReloadOutlined,
  SafetyOutlined,
  StopOutlined,
  TableOutlined,
} from '@ant-design/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';

import {
  dqcExecutionList,
  dqcExecutionRerun,
  dqcExecutionStop,
  dqcScoreTrend,
} from '#/api/metadata/dqc/execution';

const loading = ref(false);
const days = ref(30);
const trendData = ref<DqcQualityScore[]>([]);
const executionData = ref<DqcExecution[]>([]);

const filterLayer = ref<string>('');
const filterStatus = ref<string>('');

// 展开/折叠状态
const execDetailExpanded = ref(false);

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
  if (score >= 90) return '#22c55e';
  if (score >= 70) return '#f59e0b';
  return '#ef4444';
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
    .filter((r) => r && r.status !== 'RUNNING')
    .map((r) => r![field as keyof DqcExecution])
    .filter((v): v is number => typeof v === 'number' && Number.isFinite(v));
  if (vals.length === 0) return 0;
  return Math.round(vals.reduce((s, v) => s + v, 0) / vals.length * 10) / 10;
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
  const list = executionData.value ?? [];
  if (list.length === 0) {
    return {
      total: 0, success: 0, failed: 0, partial: 0, running: 0,
      avgScore: 0, avgPassRate: 0, totalRules: 0,
      alertCount: 0, planCount: 0, layerCount: 0,
    };
  }
  const total = list.length;
  const success = list.filter((r) => r?.status === 'SUCCESS').length;
  const failed = list.filter((r) => r?.status === 'FAILED').length;
  const partial = list.filter((r) => r?.status === 'PARTIAL').length;
  const running = list.filter((r) => r?.status === 'RUNNING').length;
  const completed = list.filter((r) => r && r.status !== 'RUNNING');
  const avgScore = avgExec(list, 'overallScore');
  const totalRules = completed.reduce((s, r) => s + (r?.totalRules || 0), 0);
  const totalPassed = completed.reduce((s, r) => s + (r?.passedCount || 0), 0);
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
    { label: '完整性', field: 'completenessScore' as keyof DqcQualityScore, color: '#3b82f6', bg: '#eff6ff', border: '#bfdbfe' },
    { label: '唯一性', field: 'uniquenessScore' as keyof DqcQualityScore, color: '#22c55e', bg: '#f0fdf4', border: '#bbf7d0' },
    { label: '准确性', field: 'accuracyScore' as keyof DqcQualityScore, color: '#f59e0b', bg: '#fffbeb', border: '#fde68a' },
    { label: '一致性', field: 'consistencyScore' as keyof DqcQualityScore, color: '#8b5cf6', bg: '#f5f3ff', border: '#ddd6fe' },
    { label: '及时性', field: 'timelinessScore' as keyof DqcQualityScore, color: '#06b6d4', bg: '#ecfeff', border: '#a5f3fc' },
    { label: '有效性', field: 'validityScore' as keyof DqcQualityScore, color: '#ef4444', bg: '#fef2f2', border: '#fecaca' },
  ];
  return defs.map((d) => ({ ...d, value: avg(data, d.field) }));
});

// ── Layer breakdown ─────────────────────────────────────────────

const layerStats = computed(() => {
  const map = new Map<string, { passed: number; total: number; score: number; execCount: number }>();
  for (const r of executionData.value ?? []) {
    if (!r || !r.layerCode || r.status === 'RUNNING') continue;
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
  return [...(executionData.value ?? [])]
    .sort((a, b) => {
      const ta = a.startTime ? new Date(a.startTime).getTime() : 0;
      const tb = b.startTime ? new Date(b.startTime).getTime() : 0;
      return tb - ta;
    })
    .slice(0, 10);
});

const filteredExecutions = computed(() => {
  return [...(executionData.value ?? [])]
    .filter((r) => {
      if (!r) return false;
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
  const set = new Set((executionData.value ?? []).map((r) => r.layerCode).filter(Boolean));
  return [...set].map((v) => ({ label: v, value: v }));
});

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

// ── Chart options ────────────────────────────────────────────────

const gaugeOption = computed<any>(() => {
  const score = effectiveOverall.value;
  const has = score > 0;
  const color = getScoreColor(score);
  const lineWidth = 16;
  return {
    series: [{
      type: 'gauge',
      center: ['50%', '58%'],
      radius: '88%',
      startAngle: 205,
      endAngle: -25,
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
                [0.25, '#fca5a5'],
                [0.55, '#fcd34d'],
                [0.8, '#86efac'],
                [1, '#4ade80'],
              ]
            : [[1, '#e2e8f0']],
        },
      },
      progress: {
        show: has,
        roundCap: true,
        width: lineWidth,
        itemStyle: {
          color: has ? color : '#e2e8f0',
        },
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: has,
        offsetCenter: [0, '0%'],
        fontSize: has ? 44 : 32,
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

const trendOption = computed<any>(() => {
  if (trendData.value.length === 0) return {};
  const dates = trendData.value.map((r) => r.checkDate?.slice(5) || '');
  const scores = trendData.value.map((r) => r.overallScore || 0);
  const passRates = trendData.value.map((r) => r.rulePassRate || 0);
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['综合评分', '规则通过率'], bottom: 0, textStyle: { color: '#64748b', fontSize: 12 } },
    grid: { top: 16, right: 24, bottom: 48, left: 48, containLabel: false },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { fontSize: 12, color: '#94a3b8' },
    },
    yAxis: [
      {
        type: 'value', name: '评分', min: 0, max: 100,
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { formatter: '{value}', color: '#94a3b8', fontSize: 12 },
        nameTextStyle: { color: '#94a3b8', fontSize: 12, padding: [0, 0, 0, 8] },
      },
      {
        type: 'value', name: '通过率', min: 0, max: 100,
        splitLine: { show: false },
        axisLabel: { formatter: '{value}%', color: '#94a3b8', fontSize: 12 },
        nameTextStyle: { color: '#94a3b8', fontSize: 12, padding: [0, 8, 0, 0] },
      },
    ],
    series: [
      {
        name: '综合评分', type: 'line', data: scores, smooth: 0.4,
        symbol: 'circle', symbolSize: 7,
        lineStyle: { width: 2.5, cap: 'round', join: 'round' },
        itemStyle: { color: '#4f6ef7', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(79,110,247,0.18)' },
              { offset: 1, color: 'rgba(79,110,247,0.01)' },
            ],
          },
        },
      },
      {
        name: '规则通过率', type: 'line', yAxisIndex: 1, data: passRates, smooth: 0.4,
        symbol: 'circle', symbolSize: 5,
        lineStyle: { width: 2, type: 'dashed', cap: 'round', color: '#22c55e' },
        itemStyle: { color: '#22c55e', borderColor: '#fff', borderWidth: 1 },
      },
    ],
  };
});

const radarOption = computed<any>(() => {
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
      radius: '65%',
      axisName: { color: '#475569', fontSize: 12, fontWeight: 600 },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      splitArea: { areaStyle: { color: ['#fff', '#f8fafc'] } },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
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
        areaStyle: { color: 'rgba(79,110,247,0.15)' },
        lineStyle: { color: '#4f6ef7', width: 2 },
        itemStyle: { color: '#4f6ef7' },
      }],
    }],
  };
});

const layerBarOption = computed<any>(() => {
  if (layerStats.value.length === 0) return {};
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['平均评分', '通过率(%)'], bottom: 0 },
    grid: { top: 12, right: 64, bottom: 44, left: 12, containLabel: true },
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
          itemStyle: { color: getScoreColor(d.avgScore), borderRadius: [6, 6, 0, 0] },
        })),
        barWidth: 36,
        label: { show: true, position: 'top', formatter: '{c}分', fontSize: 12 },
      },
      {
        name: '通过率(%)', type: 'line', yAxisIndex: 1,
        data: layerStats.value.map((d) => d.passRate),
        smooth: true, lineStyle: { width: 2, color: '#22c55e' },
        itemStyle: { color: '#22c55e' },
        label: { show: true, position: 'bottom', formatter: '{c}%', fontSize: 11, color: '#22c55e' },
      },
    ],
  };
});

const scoreBarOption = computed<any>(() => {
  const data = [...trendData.value]
    .filter((r) => r.overallScore !== undefined)
    .sort((a, b) => (b.overallScore || 0) - (a.overallScore || 0))
    .slice(0, 10);
  if (data.length === 0) return {};
  const names = data.map((r) => `${r.dsName || ''}/${r.targetTable || ''}`.slice(0, 14));
  const scores = data.map((r) => r.overallScore || 0);
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 8, right: 64, bottom: 8, left: 12, containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}' } },
    yAxis: {
      type: 'category', data: names,
      axisLabel: { fontSize: 11, width: 110, overflow: 'truncate' },
    },
    series: [{
      type: 'bar', data: scores.map((s) => ({
        value: s, itemStyle: { color: getScoreColor(s), borderRadius: [0, 6, 6, 0] },
      })),
      barWidth: 16,
      label: { show: true, position: 'right', formatter: '{c}分', fontSize: 12 },
    }],
  };
});

// ── Export report ────────────────────────────────────────────────

function exportReport() {
  const rows = trendData.value;
  if (rows.length === 0) return;
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
      dqcScoreTrend({ days: days.value }).catch(() => [] as DqcQualityScore[]),
      dqcExecutionList({ pageNum: 1, pageSize: 500 }).catch(() => ({ rows: [] as DqcExecution[] })),
    ]);
    trendData.value = Array.isArray(scoreRes) ? scoreRes : [];
    executionData.value = Array.isArray(execRes) ? execRes : (execRes?.rows ?? []);
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

function toggleExecDetail() {
  execDetailExpanded.value = !execDetailExpanded.value;
}

</script>

<template>
  <Page :auto-content-height="true">
    <Spin :spinning="loading">

      <!-- 工具栏：简洁横条 -->
      <div class="dash-toolbar">
        <div class="toolbar-left">
          <Select
            v-model:value="days"
            :options="dayOptions"
            style="width: 120px"
            @change="loadData"
          />
        </div>
        <div class="toolbar-right">
          <Button @click="exportReport" :disabled="trendData.length === 0">
            <template #icon><DownloadOutlined /></template>
            导出报告
          </Button>
          <Button type="primary" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </Button>
        </div>
      </div>

      <!-- ════ 主内容区域 ════ -->
      <div class="dash-body">

        <!-- 第一行：评分仪表盘 + 核心指标 -->
        <Row :gutter="20" class="dash-row" style="align-items: stretch;">

          <!-- 左侧：评分仪表盘 -->
          <Col :span="5">
            <Card class="card-gauge" :bordered="false">
              <div class="gauge-wrap">
                <EchartsUI ref="gaugeRef" style="height: 160px" />
              </div>
              <div class="gauge-label" :style="{ color: getScoreColor(effectiveOverall) }">
                {{ getScoreLabel(effectiveOverall) }}
              </div>
              <div class="gauge-title">综合质量评分</div>
              <div class="gauge-meta">
                <span>规则通过率</span>
                <strong :style="{ color: getScoreColor(effectivePassRate) }">{{ effectivePassRate }}%</strong>
              </div>
              <div class="gauge-progress">
                <Progress
                  :percent="effectivePassRate"
                  :stroke-color="getScoreColor(effectivePassRate)"
                  :show-info="false"
                  size="small"
                />
              </div>
            </Card>
          </Col>

          <!-- 右侧：6个核心指标卡片 -->
          <Col :span="19" style="display: flex; flex-direction: column;">
            <div style="display: flex; flex-direction: column; flex: 1; gap: 16px; align-items: stretch;">
              <div style="display: flex; gap: 16px;">
                <!-- 成功率 -->
                <div style="flex: 1;">
                  <Card class="card-kpi" :bordered="false">
                    <div class="kpi-inner">
                      <div class="kpi-icon-wrap kpi-icon--green">
                        <CheckCircleOutlined />
                      </div>
                      <div class="kpi-content">
                        <div class="kpi-value" :style="{ color: getScoreColor(effectivePassRate) }">
                          {{ effectivePassRate }}<span class="kpi-unit">%</span>
                        </div>
                        <div class="kpi-label">规则通过率</div>
                      </div>
                      <div class="kpi-sub">
                        <Tag :color="getScoreColor(effectivePassRate)" style="font-size: 11px; padding: 0 6px; line-height: 18px;">
                          {{ execOverview.totalRules }} 条规则
                        </Tag>
                      </div>
                    </div>
                  </Card>
                </div>
                <!-- 监控表数 -->
                <div style="flex: 1;">
                  <Card class="card-kpi" :bordered="false">
                    <div class="kpi-inner">
                      <div class="kpi-icon-wrap kpi-icon--blue">
                        <TableOutlined />
                      </div>
                      <div class="kpi-content">
                        <div class="kpi-value" style="color: #475569">{{ scoreOverview.totalTables }}<span class="kpi-unit"> 张</span></div>
                        <div class="kpi-label">被监控表数</div>
                      </div>
                      <div class="kpi-sub">
                        <span class="kpi-sub-text">覆盖 {{ execOverview.layerCount }} 个层级</span>
                      </div>
                    </div>
                  </Card>
                </div>
                <!-- 质检规则 -->
                <div style="flex: 1;">
                  <Card class="card-kpi" :bordered="false">
                    <div class="kpi-inner">
                      <div class="kpi-icon-wrap kpi-icon--amber">
                        <SafetyOutlined />
                      </div>
                      <div class="kpi-content">
                        <div class="kpi-value" style="color: #475569">{{ execOverview.totalRules }}<span class="kpi-unit"> 条</span></div>
                        <div class="kpi-label">质检规则总数</div>
                      </div>
                      <div class="kpi-sub">
                        <span class="kpi-sub-text">{{ execOverview.planCount }} 个质检方案</span>
                      </div>
                    </div>
                  </Card>
                </div>
              </div>
              <div style="display: flex; gap: 16px;">
                <!-- 成功 -->
                <div style="flex: 1;">
                  <Card class="card-kpi" :bordered="false">
                    <div class="kpi-inner">
                      <div class="kpi-icon-wrap kpi-icon--success">
                        <CheckCircleOutlined />
                      </div>
                      <div class="kpi-content">
                        <div class="kpi-value" style="color: #22c55e">{{ execOverview.success }}<span class="kpi-unit"> 次</span></div>
                        <div class="kpi-label">成功执行</div>
                      </div>
                      <div class="kpi-sub">
                        <Progress
                          :percent="execOverview.total > 0 ? Math.round((execOverview.success / execOverview.total) * 100) : 0"
                          :stroke-color="'#22c55e'"
                          :show-info="false"
                          size="small"
                          style="width: 80px"
                        />
                        <span class="kpi-sub-text" style="margin-left: 8px">
                          {{ execOverview.total > 0 ? Math.round((execOverview.success / execOverview.total) * 100) : 0 }}%
                        </span>
                      </div>
                    </div>
                  </Card>
                </div>
                <!-- 预警 -->
                <div style="flex: 1;">
                  <Card class="card-kpi" :bordered="false">
                    <div class="kpi-inner">
                      <div class="kpi-icon-wrap kpi-icon--danger">
                        <ExclamationCircleOutlined />
                      </div>
                      <div class="kpi-content">
                        <div class="kpi-value" :style="{ color: effectiveAlert > 0 ? '#ef4444' : '#22c55e' }">
                          {{ effectiveAlert }}<span class="kpi-unit"> 条</span>
                        </div>
                        <div class="kpi-label">预警项</div>
                      </div>
                      <div class="kpi-sub">
                        <span class="kpi-sub-text" :style="{ color: effectiveAlert > 0 ? '#ef4444' : '#22c55e' }">
                          {{ effectiveAlert > 0 ? '评分 &lt; 70 分' : '质量正常' }}
                        </span>
                      </div>
                    </div>
                  </Card>
                </div>
                <!-- 执行总数 -->
                <div style="flex: 1;">
                  <Card class="card-kpi" :bordered="false">
                    <div class="kpi-inner">
                      <div class="kpi-icon-wrap kpi-icon--purple">
                        <LoadingOutlined />
                      </div>
                      <div class="kpi-content">
                        <div class="kpi-value" style="color: #475569">{{ execOverview.total }}<span class="kpi-unit"> 次</span></div>
                        <div class="kpi-label">执行总数</div>
                      </div>
                      <div class="kpi-sub">
                        <span class="kpi-sub-text">运行中 {{ execOverview.running }} 次</span>
                      </div>
                    </div>
                  </Card>
                </div>
              </div>
            </div>
          </Col>
        </Row>

        <!-- 执行状态条 -->
        <div class="exec-status-bar">
          <div class="exec-chip exec-chip--success">
            <CheckCircleOutlined /> 成功 {{ execOverview.success }}
          </div>
          <div class="exec-chip exec-chip--partial">
            <ClockCircleOutlined /> 部分失败 {{ execOverview.partial }}
          </div>
          <div class="exec-chip exec-chip--failed">
            <CloseCircleOutlined /> 失败 {{ execOverview.failed }}
          </div>
          <div class="exec-chip exec-chip--running">
            <LoadingOutlined /> 运行中 {{ execOverview.running }}
          </div>
        </div>

        <!-- 第二行：趋势图 + 雷达图 -->
        <Row :gutter="20" class="dash-row">
          <Col :span="15">
            <Card class="card-chart" :bordered="false">
              <template #title>
                <div class="chart-header">
                  <span class="chart-title">
                    <FundViewOutlined style="margin-right: 6px; color: #4f6ef7" />
                    评分与通过率趋势
                  </span>
                  <span class="chart-subtitle">近 {{ days }} 天</span>
                </div>
              </template>
              <template v-if="trendData.length > 0">
                <EchartsUI ref="trendRef" style="height: 260px" />
              </template>
              <div v-else class="chart-empty">
                <Timeline>
                  <Timeline.Item
                    v-for="exec in recentExecutions.slice(0, 5)"
                    :key="exec.id"
                    :color="getStatusColor(exec.status) === 'success' ? 'green' : getStatusColor(exec.status) === 'error' ? 'red' : getStatusColor(exec.status) === 'warning' ? 'orange' : 'blue'"
                  >
                    <div class="tl-row">
                      <Tag :color="getStatusColor(exec.status)">{{ getStatusLabel(exec.status) }}</Tag>
                      <span class="tl-plan">{{ exec.planName || '—' }}</span>
                      <Tag>{{ exec.layerCode || '—' }}</Tag>
                      <span class="tl-score" v-if="exec.overallScore" :style="{ color: getScoreColor(exec.overallScore) }">{{ exec.overallScore }}分</span>
                      <span class="tl-time">{{ exec.startTime?.slice(0, 16) || '—' }}</span>
                    </div>
                  </Timeline.Item>
                </Timeline>
              </div>
            </Card>
          </Col>

          <Col :span="9">
            <Card class="card-chart" :bordered="false">
              <template #title>
                <div class="chart-header">
                  <span class="chart-title">
                    <MenuOutlined style="margin-right: 6px; color: #4f6ef7" />
                    六维质量雷达
                  </span>
                </div>
              </template>
              <template v-if="trendData.length > 0">
                <EchartsUI ref="radarRef" style="height: 260px" />
              </template>
              <div v-else class="chart-empty">
                <div class="stat-row"><span class="stat-lbl">总执行</span><span class="stat-val">{{ execOverview.total }} 次</span></div>
                <div class="stat-row"><span class="stat-lbl">成功率</span><span class="stat-val" :style="{ color: getScoreColor(execOverview.total > 0 ? (execOverview.success / execOverview.total * 100) : 0) }">{{ execOverview.total > 0 ? Math.round((execOverview.success / execOverview.total) * 100) : 0 }}%</span></div>
                <div class="stat-row"><span class="stat-lbl">失败率</span><span class="stat-val" style="color: #ef4444">{{ execOverview.total > 0 ? Math.round((execOverview.failed / execOverview.total) * 100) : 0 }}%</span></div>
                <div class="stat-row"><span class="stat-lbl">平均评分</span><span class="stat-val" :style="{ color: getScoreColor(execOverview.avgScore) }">{{ execOverview.avgScore || '—' }}</span></div>
                <div class="stat-row"><span class="stat-lbl">质检方案</span><span class="stat-val">{{ execOverview.planCount }} 个</span></div>
                <div class="stat-row"><span class="stat-lbl">覆盖层级</span><span class="stat-val">{{ execOverview.layerCount }} 个</span></div>
              </div>
            </Card>
          </Col>
        </Row>

        <!-- 第三行：六维质量得分 -->
        <Row :gutter="20" class="dash-row">
          <Col :span="12">
            <Card class="card-chart" :bordered="false">
              <template #title>
                <div class="chart-header">
                  <span class="chart-title">
                    <FontSizeOutlined style="margin-right: 6px; color: #4f6ef7" />
                    六维质量得分
                  </span>
                  <span class="chart-subtitle">综合评分各维度表现</span>
                </div>
              </template>
              <div class="six-grid">
                <div
                  v-for="dim in dimensions"
                  :key="dim.label"
                  class="six-item"
                  :style="{ background: dim.bg, borderColor: dim.border }"
                >
                  <div class="six-label" :style="{ color: dim.color }">{{ dim.label }}</div>
                  <div class="six-value" :style="{ color: trendData.length > 0 ? getScoreColor(dim.value) : '#94a3b8' }">
                    {{ trendData.length > 0 ? dim.value.toFixed(1) : '—' }}
                  </div>
                  <div class="six-bar">
                    <div
                      class="six-bar-fill"
                      :style="{
                        width: trendData.length > 0 ? `${Math.min(100, dim.value)}%` : '0%',
                        background: dim.color
                      }"
                    />
                  </div>
                </div>
              </div>
            </Card>
          </Col>

          <Col :span="12">
            <Card class="card-chart" :bordered="false">
              <template #title>
                <div class="chart-header">
                  <span class="chart-title">
                    <FieldNumberOutlined style="margin-right: 6px; color: #4f6ef7" />
                    数仓分层质量对比
                  </span>
                  <span class="chart-subtitle">各层级评分与通过率</span>
                </div>
              </template>
              <template v-if="layerStats.length > 0">
                <EchartsUI ref="layerBarRef" style="height: 200px" />
              </template>
              <Empty v-else description="暂无分层数据" />
            </Card>
          </Col>
        </Row>

        <!-- 第四行：表质量排行 -->
        <Row :gutter="20" class="dash-row" v-if="trendData.length > 0">
          <Col :span="24">
            <Card class="card-chart" :bordered="false">
              <template #title>
                <div class="chart-header">
                  <span class="chart-title">
                    <FundViewOutlined style="margin-right: 6px; color: #4f6ef7" />
                    表质量排行 TOP 10
                  </span>
                  <span class="chart-subtitle">按综合评分从高到低排列</span>
                </div>
              </template>
              <EchartsUI ref="barRef" style="height: 220px" />
            </Card>
          </Col>
        </Row>

        <!-- ════ 执行记录（可展开） ════ -->
        <div class="section-card">
          <!-- 展开/收起 标题栏 -->
          <div class="section-header" @click="toggleExecDetail">
            <div class="section-title">
              <LoadingOutlined style="margin-right: 8px; color: #4f6ef7" />
              执行记录明细
              <span class="section-count">{{ filteredExecutions.length }} 条</span>
            </div>
            <div class="section-toggle">
              <span v-if="execDetailExpanded">收起明细 <CaretUpOutlined /></span>
              <span v-else>查看明细 <CaretDownOutlined /></span>
            </div>
          </div>

          <!-- 展开后的内容 -->
          <div v-show="execDetailExpanded" class="section-body">
            <!-- 筛选栏 -->
            <div class="detail-filter">
              <div class="filter-item">
                <span class="filter-label">数仓层级：</span>
                <Select v-model:value="filterLayer" style="width: 130px" placeholder="全部" allow-clear :options="layerOptions" />
              </div>
              <div class="filter-item">
                <span class="filter-label">执行状态：</span>
                <Select v-model:value="filterStatus" style="width: 130px" placeholder="全部" allow-clear :options="executionStatusOptions" />
              </div>
              <Button size="small" @click="() => { filterLayer = ''; filterStatus = '' }">重置</Button>
            </div>

            <!-- 统计汇总 -->
            <div class="detail-stats">
              <div class="detail-stat">
                <span class="detail-stat-label">平均评分</span>
                <span class="detail-stat-value" :style="{ color: getScoreColor(execOverview.avgScore) }">{{ execOverview.avgScore }}分</span>
              </div>
              <div class="detail-stat">
                <span class="detail-stat-label">平均通过率</span>
                <span class="detail-stat-value" :style="{ color: getScoreColor(execOverview.avgPassRate) }">{{ execOverview.avgPassRate }}%</span>
              </div>
              <div class="detail-stat">
                <span class="detail-stat-label">执行总数</span>
                <span class="detail-stat-value">{{ execOverview.total }} 次</span>
              </div>
              <div class="detail-stat">
                <span class="detail-stat-label">质检规则</span>
                <span class="detail-stat-value">{{ execOverview.totalRules }} 条</span>
              </div>
              <div class="detail-stat">
                <span class="detail-stat-label">成功数</span>
                <span class="detail-stat-value" style="color: #22c55e">{{ execOverview.success }} 次</span>
              </div>
              <div class="detail-stat">
                <span class="detail-stat-label">失败数</span>
                <span class="detail-stat-value" :style="{ color: execOverview.failed > 0 ? '#ef4444' : '#22c55e' }">{{ execOverview.failed }} 次</span>
              </div>
            </div>

            <!-- 数据分类层级详情（无标题） -->
            <div v-if="layerStats.length > 0" class="layer-detail">
              <div class="layer-cards">
                <div v-for="ls in layerStats" :key="ls.layer" class="layer-card">
                  <div class="layer-card-name">{{ ls.layer }}</div>
                  <div class="layer-card-score" :style="{ color: getScoreColor(ls.avgScore) }">{{ ls.avgScore }}分</div>
                  <div class="layer-card-meta">通过率 {{ ls.passRate }}%</div>
                  <div class="layer-card-meta">执行 {{ ls.execCount }} 次</div>
                </div>
              </div>
            </div>

            <!-- 执行表格 -->
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
                      <span style="font-size: 12px; color: #94a3b8">{{ record.passedCount || 0 }}/{{ record.totalRules || 0 }}</span>
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
          </div>
        </div>

      </div>
    </Spin>
  </Page>
</template>

<style scoped>
/* ── 工具栏 ─────────────────────────────────────────────────────── */
.dash-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
}
.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── 主体布局 ─────────────────────────────────────────────────── */
.dash-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.dash-row {
  width: 100%;
}

/* ── 评分仪表卡片 ─────────────────────────────────────────────── */
.card-gauge {
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  background: #fff;
  text-align: center;
  padding: 16px 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  height: 100%;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.gauge-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
  flex-shrink: 0;
}
.gauge-label {
  font-size: 16px;
  font-weight: 700;
  margin-top: 8px;
  letter-spacing: 0.5px;
}
.gauge-title {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}
.gauge-meta {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.gauge-meta strong {
  font-weight: 700;
  font-size: 14px;
}
.gauge-progress {
  width: 80%;
  margin-top: 6px;
}

/* ── KPI 卡片 ──────────────────────────────────────────────────── */
.card-kpi {
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
  height: 100%;
  min-height: 88px;
}
.card-kpi:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.kpi-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px;
  height: 100%;
}
.kpi-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.kpi-icon--green { background: #f0fdf4; color: #22c55e; }
.kpi-icon--blue { background: #eff6ff; color: #3b82f6; }
.kpi-icon--amber { background: #fffbeb; color: #f59e0b; }
.kpi-icon--success { background: #f0fdf4; color: #22c55e; }
.kpi-icon--danger { background: #fef2f2; color: #ef4444; }
.kpi-icon--purple { background: #f5f3ff; color: #8b5cf6; }
.kpi-icon--teal { background: #ecfeff; color: #06b6d4; }

.kpi-content {
  flex: 1;
  min-width: 0;
}
.kpi-value {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.kpi-unit {
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
}
.kpi-label {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.kpi-sub {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.kpi-sub-text {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

/* ── 执行状态条 ────────────────────────────────────────────────── */
.exec-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.exec-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}
.exec-chip--success { background: #f0fdf4; color: #22c55e; }
.exec-chip--partial { background: #fffbeb; color: #f59e0b; }
.exec-chip--failed { background: #fef2f2; color: #ef4444; }
.exec-chip--running { background: #eff6ff; color: #3b82f6; }

/* ── 图表卡片 ──────────────────────────────────────────────────── */
.card-chart {
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
}
.chart-subtitle {
  font-size: 12px;
  color: #94a3b8;
}
.chart-empty {
  padding: 8px 0;
}

/* ── 六维网格 ──────────────────────────────────────────────────── */
.six-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.six-item {
  border-radius: 10px;
  border: 1px solid;
  padding: 14px 16px 10px;
}
.six-label {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
}
.six-value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  margin-bottom: 8px;
}
.six-bar {
  height: 5px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 3px;
  overflow: hidden;
}
.six-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

/* ── 时间线 ───────────────────────────────────────────────────── */
.tl-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tl-plan { font-size: 13px; font-weight: 600; color: #334155; }
.tl-score { font-size: 13px; font-weight: 700; }
.tl-time { font-size: 12px; color: #94a3b8; }

/* ── 统计行（雷达图空状态） ───────────────────────────────────── */
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 0;
  border-bottom: 1px solid #f8fafc;
}
.stat-row:last-child { border-bottom: none; }
.stat-lbl { font-size: 13px; color: #94a3b8; }
.stat-val { font-size: 15px; font-weight: 700; color: #334155; }

/* ── 分隔区域（执行记录） ──────────────────────────────────────── */
.section-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.section-header:hover {
  background: #f8fafc;
}
.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
}
.section-count {
  margin-left: 8px;
  background: #eff6ff;
  color: #3b82f6;
  font-size: 12px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 10px;
}
.section-toggle {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}
.section-body {
  padding: 0 20px 20px;
  border-top: 1px solid #f1f5f9;
}

/* ── 明细筛选 ─────────────────────────────────────────────────── */
.detail-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0 0;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.filter-label {
  font-size: 13px;
  color: #64748b;
}

/* ── 统计汇总行 ────────────────────────────────────────────────── */
.detail-stats {
  display: flex;
  gap: 0;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 16px;
}
.detail-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border-right: 1px solid #f1f5f9;
}
.detail-stat:last-child { border-right: none; }
.detail-stat-label { font-size: 12px; color: #94a3b8; }
.detail-stat-value { font-size: 20px; font-weight: 800; color: #334155; font-variant-numeric: tabular-nums; }

/* ── 数据分类层级卡片（优化间距） ─────────────────────────────── */
.layer-detail {
  margin-bottom: 16px;
}
.layer-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}
.layer-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  text-align: center;
  transition: all 0.2s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.layer-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
  border-color: #4f6ef7;
}
.layer-card-name {
  font-size: 14px;
  font-weight: 800;
  color: #334155;
  margin-bottom: 8px;
}
.layer-card-score {
  font-size: 26px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  margin-bottom: 8px;
}
.layer-card-meta {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
}
</style>
