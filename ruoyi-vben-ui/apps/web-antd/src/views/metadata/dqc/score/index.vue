<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import {
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Empty,
  Modal,
  Progress,
  Row,
  Select,
  Spin,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import { RangePicker } from 'ant-design-vue/es/date-picker';
import {
  DownloadOutlined,
  EyeOutlined,
  LineChartOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { dqcScoreTrend } from '#/api/metadata/dqc/score';
import type { DqcQualityScore } from '#/api/metadata/model';

const loading = ref(false);
const days = ref(7);
const trendData = ref<DqcQualityScore[]>([]);

const filterDs = ref<string>('');
const filterTable = ref<string>('');
const filterLayer = ref<string>('');
const detailDateRange = ref<[Dayjs, Dayjs] | null>(null);

type TableScoreRow = {
  key: string;
  layerCode: string;
  dsName: string;
  table: string;
  checkDate: string;
  score?: number | null;
  passRate?: number | null;
  passCount: number;
  totalCount: number;
  completeness?: number | null;
  uniqueness?: number | null;
  accuracy?: number | null;
  consistency?: number | null;
  timeliness?: number | null;
  validity?: number | null;
  raw: DqcQualityScore;
};

const detailModalOpen = ref(false);
const detailRecord = ref<TableScoreRow | null>(null);

const gaugeRef = ref<EchartsUIType>();
const trendChartRef = ref<EchartsUIType>();

const { renderEcharts: renderGauge, updateData: updateGauge } = useEcharts(gaugeRef);
const { renderEcharts: renderTrend } = useEcharts(trendChartRef);

/** 无评分样本时的中性色，避免把「0」渲染成告警红 */
const NEUTRAL_MUTED = '#9aa3b5';
const NEUTRAL_TRACK = '#e4e8f0';

function getScoreColor(score?: number | null) {
  if (score === undefined || score === null) return '#8c8c8c';
  if (score >= 90) return '#52c41a';
  if (score >= 70) return '#faad14';
  return '#ff4d4f';
}

function displayMetricColor(score: number, hasData: boolean) {
  if (!hasData) return NEUTRAL_MUTED;
  return getScoreColor(score);
}

function getScoreTagColor(score?: number | null) {
  if (score === undefined || score === null) return 'default';
  if (score >= 90) return 'success';
  if (score >= 70) return 'warning';
  return 'error';
}

function getScoreLabel(score?: number | null, hasData = true) {
  if (!hasData) return '暂无数据';
  if (score === undefined || score === null) return '—';
  if (score >= 90) return '优秀';
  if (score >= 70) return '良好';
  return '较差';
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

const dsOptions = computed(() => {
  const set = new Set(trendData.value.map((r) => r.dsName).filter(Boolean));
  return [...set].map((v) => ({ label: v!, value: v! }));
});

const layerOptions = computed(() => {
  const set = new Set(trendData.value.map((r) => r.layerCode).filter(Boolean));
  return [...set].sort().map((v) => ({ label: v!, value: v! }));
});

const tableOptions = computed(() => {
  const list = trendData.value.filter(
    (r) => !filterDs.value || r.dsName === filterDs.value,
  );
  const set = new Set(list.map((r) => r.targetTable).filter(Boolean));
  return [...set].map((v) => ({ label: v!, value: v! }));
});

/** 行级筛选（数据源、表、数据层），不含明细日期 */
const scopedRows = computed(() => {
  return trendData.value.filter((r) => {
    if (filterDs.value && r.dsName !== filterDs.value) return false;
    if (filterTable.value && r.targetTable !== filterTable.value) return false;
    if (filterLayer.value && r.layerCode !== filterLayer.value) return false;
    return true;
  });
});

const hasScoreData = computed(() => scopedRows.value.length > 0);

/** 趋势图：按检查日期排序的明细行 */
const trendSeriesRows = computed(() => {
  return [...scopedRows.value].sort((a, b) => {
    const da = a.checkDate || '';
    const db = b.checkDate || '';
    return da.localeCompare(db);
  });
});

function rowInDetailDateRange(r: DqcQualityScore) {
  const range = detailDateRange.value;
  if (!range || range.length !== 2) return true;
  const [start, end] = range;
  if (!r.checkDate) return false;
  const d = dayjs(r.checkDate.slice(0, 10));
  return !d.isBefore(start, 'day') && !d.isAfter(end, 'day');
}

/** 用于表级聚合：仅在明细日期范围内的记录 */
const rowsForTableAgg = computed(() =>
  scopedRows.value.filter((r) => rowInDetailDateRange(r)),
);

function avg(list: DqcQualityScore[], field: keyof DqcQualityScore): number {
  const vals = list
    .map((r) => r[field])
    .filter((v): v is number => v !== undefined && v !== null);
  if (vals.length === 0) return 0;
  return Math.round((vals.reduce((s, v) => s + v, 0) / vals.length) * 10) / 10;
}

const stats = computed(() => {
  const data = scopedRows.value;
  if (data.length === 0) {
    return {
      avgOverall: 0,
      avgPassRate: 0,
      totalTables: 0,
      totalRules: 0,
      alertCount: 0,
    };
  }
  const n = data.length;
  const avgOverall = avg(data, 'overallScore');
  const avgPassRate = Math.round(
    data.reduce((s, r) => s + (r.rulePassRate || 0), 0) / n,
  );
  const tables = new Set(data.map((r) => `${r.targetDsId}-${r.targetTable}`)).size;
  const totalRules = data.reduce((s, r) => s + (r.ruleTotalCount || 0), 0);
  const alertCount = data.filter(
    (r) => r.overallScore !== undefined && r.overallScore !== null && r.overallScore < 70,
  ).length;
  return { avgOverall, avgPassRate, totalTables, totalRules, alertCount };
});

const lastUpdatedText = computed(() => {
  const rows = scopedRows.value;
  if (rows.length === 0) return '—';
  let latest = '';
  for (const r of rows) {
    const t = r.createTime || r.checkDate;
    if (t && t > latest) latest = t;
  }
  if (!latest) return '—';
  const parsed = dayjs(latest.includes('T') ? latest : `${latest}T00:00:00`);
  return parsed.isValid() ? parsed.format('HH:mm:ss') : latest.slice(11, 19) || '—';
});

const dimensions = computed(() => {
  const data = scopedRows.value;
  const defs = [
    { label: '完整性', field: 'completenessScore' as const, accent: 'dim-accent--blue', icon: 'shield' as const },
    { label: '唯一性', field: 'uniquenessScore' as const, accent: 'dim-accent--green', icon: 'lock' as const },
    { label: '准确性', field: 'accuracyScore' as const, accent: 'dim-accent--orange', icon: 'bolt' as const },
    { label: '一致性', field: 'consistencyScore' as const, accent: 'dim-accent--purple', icon: 'swap' as const },
    { label: '及时性', field: 'timelinessScore' as const, accent: 'dim-accent--teal', icon: 'clock' as const },
    { label: '有效性', field: 'validityScore' as const, accent: 'dim-accent--red', icon: 'check' as const },
  ];
  return defs.map((d) => ({ ...d, value: avg(data, d.field) }));
});

const gaugeOption = computed(() => {
  const has = hasScoreData.value;
  const score = has ? stats.value.avgOverall : 0;
  const color = displayMetricColor(score, has);
  const lineWidth = 12;
  return {
    series: [
      {
        type: 'gauge',
        center: ['50%', '62%'],
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
              : [[1, NEUTRAL_TRACK]],
          },
        },
        progress: {
          show: has,
          roundCap: true,
          width: lineWidth,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 1,
              y2: 0,
              colorStops: has
                ? [
                    { offset: 0, color: '#ff7875' },
                    { offset: 0.45, color: '#faad14' },
                    { offset: 1, color: '#52c41a' },
                  ]
                : [{ offset: 0, color: NEUTRAL_TRACK }, { offset: 1, color: NEUTRAL_TRACK }],
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
          offsetCenter: [0, '0%'],
          fontSize: has ? 40 : 28,
          fontWeight: 700,
          formatter: (val: number | string) => {
            if (!has) return '—';
            const n = typeof val === 'number' ? val : Number(val);
            return Number.isFinite(n) ? n.toFixed(1) : '—';
          },
          color,
        },
        data: [{ value: has ? Math.min(100, Math.max(0, score)) : 0 }],
      },
    ],
  };
});

const trendOption = computed(() => {
  const data = trendSeriesRows.value;
  if (data.length === 0) {
    return {
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', min: 0, max: 100 },
      series: [],
    };
  }
  const dates = data.map((r) => r.checkDate?.slice(5) || r.checkDate || '');
  const scores = data.map((r) => r.overallScore || 0);
  const passRates = data.map((r) => r.rulePassRate || 0);
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['综合评分', '规则通过率'], bottom: 0, textStyle: { color: '#64748b', fontSize: 11 } },
    grid: { top: 14, right: 20, bottom: 42, left: 52, containLabel: false },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: { fontSize: 11, color: '#94a3b8' },
    },
    yAxis: [
      {
        type: 'value',
        name: '评分',
        min: 0,
        max: 100,
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { formatter: '{value}', color: '#94a3b8', fontSize: 11 },
        nameTextStyle: { color: '#94a3b8', fontSize: 11, padding: [0, 0, 0, 8] },
      },
      {
        type: 'value',
        name: '通过率',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { formatter: '{value}%', color: '#94a3b8', fontSize: 11 },
        nameTextStyle: { color: '#94a3b8', fontSize: 11, padding: [0, 8, 0, 0] },
      },
    ],
    series: [
      {
        name: '综合评分',
        type: 'line',
        data: scores,
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5, cap: 'round', join: 'round' },
        itemStyle: { color: '#4f6ef7', borderColor: '#fff', borderWidth: 1 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(79, 110, 247, 0.22)' },
              { offset: 1, color: 'rgba(79, 110, 247, 0.02)' },
            ],
          },
        },
      },
      {
        name: '规则通过率',
        type: 'line',
        yAxisIndex: 1,
        data: passRates,
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, type: 'dashed', cap: 'round' },
        itemStyle: { color: '#34b27b', borderColor: '#fff', borderWidth: 1 },
      },
    ],
  };
});

function buildTableRow(r: DqcQualityScore, key: string): TableScoreRow {
  const pass =
    r.rulePassCount !== undefined && r.rulePassCount !== null
      ? r.rulePassCount
      : Math.max(0, (r.ruleTotalCount || 0) - (r.ruleFailCount || 0));
  return {
    key,
    layerCode: r.layerCode || '—',
    dsName: r.dsName || '未知数据源',
    table: r.targetTable || '',
    checkDate: r.checkDate || '—',
    score: r.overallScore,
    passRate: r.rulePassRate,
    passCount: pass,
    totalCount: r.ruleTotalCount || 0,
    completeness: r.completenessScore,
    uniqueness: r.uniquenessScore,
    accuracy: r.accuracyScore,
    consistency: r.consistencyScore,
    timeliness: r.timelinessScore,
    validity: r.validityScore,
    raw: r,
  };
}

const tableList = computed(() => {
  const map = new Map<string, DqcQualityScore>();
  for (const r of rowsForTableAgg.value) {
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
  return [...map.entries()].map(([key, r]) => buildTableRow(r, key));
});

const lowScoreAlerts = computed(() => {
  return tableList.value
    .filter((t) => t.score !== undefined && t.score !== null && t.score < 70)
    .slice(0, 3);
});

const tableColumns = [
  { title: '数据层', key: 'layerCode', width: 88, align: 'center' as const },
  { title: '目标表', dataIndex: 'table', key: 'table', width: 200, ellipsis: true },
  {
    title: '综合评分',
    key: 'score',
    width: 140,
    align: 'center' as const,
  },
  { title: '规则通过率', key: 'passRate', width: 130, align: 'center' as const },
  { title: '通过/总数', key: 'ruleCount', width: 100, align: 'center' as const },
  { title: '完整性', key: 'completeness', width: 72, align: 'center' as const },
  { title: '唯一性', key: 'uniqueness', width: 72, align: 'center' as const },
  { title: '准确性', key: 'accuracy', width: 72, align: 'center' as const },
  { title: '一致性', key: 'consistency', width: 72, align: 'center' as const },
  { title: '及时性', key: 'timeliness', width: 72, align: 'center' as const },
  { title: '有效性', key: 'validity', width: 72, align: 'center' as const },
  { title: '检查日期', dataIndex: 'checkDate', key: 'checkDate', width: 110 },
  { title: '操作', key: 'action', width: 88, align: 'center' as const, fixed: 'right' as const },
];

function openDetail(row: TableScoreRow) {
  detailRecord.value = row;
  detailModalOpen.value = true;
}

function fmtDim(v?: number | null) {
  if (v === undefined || v === null) return '—';
  return Number(v).toFixed(1);
}

function exportReport() {
  const rows = tableList.value;
  if (rows.length === 0) {
    Modal.info({ title: '提示', content: '当前没有可导出的评分明细数据。' });
    return;
  }
  const headers = [
    '数据层',
    '数据源',
    '目标表',
    '综合评分',
    '规则通过率',
    '通过数',
    '规则总数',
    '完整性',
    '唯一性',
    '准确性',
    '一致性',
    '及时性',
    '有效性',
    '检查日期',
  ];
  const lines = rows.map((r) =>
    [
      r.layerCode,
      r.dsName,
      r.table,
      r.score ?? '',
      r.passRate ?? '',
      r.passCount,
      r.totalCount,
      r.completeness ?? '',
      r.uniqueness ?? '',
      r.accuracy ?? '',
      r.consistency ?? '',
      r.timeliness ?? '',
      r.validity ?? '',
      r.checkDate,
    ].map((c) => `"${String(c).replace(/"/g, '""')}"`).join(','),
  );
  const bom = '\uFEFF';
  const csv = bom + [headers.join(','), ...lines].join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `质量评分报告_${dayjs().format('YYYY-MM-DD_HHmm')}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

async function loadData() {
  loading.value = true;
  try {
    trendData.value = (await dqcScoreTrend({ days: days.value })) || [];
  } finally {
    loading.value = false;
  }
}

function refreshCharts() {
  nextTick(() => {
    renderGauge(gaugeOption.value);
    if (trendSeriesRows.value.length > 0) {
      renderTrend(trendOption.value);
    }
  });
}

watch(
  [scopedRows, trendSeriesRows, stats],
  () => {
    updateGauge(gaugeOption.value);
    nextTick(() => {
      if (trendSeriesRows.value.length > 0) {
        renderTrend(trendOption.value);
      }
    });
  },
  { deep: true },
);

onMounted(() => {
  void loadData().then(() => {
    setTimeout(refreshCharts, 150);
  });
});
</script>

<template>
  <Page :auto-content-height="true">
    <Spin :spinning="loading">
      <!-- 页头（对齐参考：标题 + 副标题 + 导出 / 刷新） -->
      <div class="qs-header">
        <div class="qs-header-text">
          <h1 class="qs-title">质量评分</h1>
          <p class="qs-subtitle">六维度质量评分与历史趋势分析</p>
        </div>
        <div class="qs-header-actions">
          <Button @click="exportReport">
            <template #icon>
              <DownloadOutlined />
            </template>
            导出报告
          </Button>
          <Button type="primary" @click="loadData">
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </Button>
        </div>
      </div>

      <!-- 筛选条 -->
      <Card class="qs-toolbar" :bordered="false">
        <Row :gutter="[16, 12]" align="middle">
          <Col>
            <span class="filter-label">统计周期</span>
            <Select
              v-model:value="days"
              style="width: 120px"
              :options="[
                { label: '近 7 天', value: 7 },
                { label: '近 14 天', value: 14 },
                { label: '近 30 天', value: 30 },
                { label: '近 90 天', value: 90 },
              ]"
              @change="loadData"
            />
          </Col>
          <Col>
            <span class="filter-label">数据源</span>
            <Select
              v-model:value="filterDs"
              style="width: 160px"
              placeholder="全部"
              allow-clear
              :options="dsOptions"
            />
          </Col>
          <Col>
            <span class="filter-label">表名</span>
            <Select
              v-model:value="filterTable"
              style="width: 200px"
              placeholder="全部"
              allow-clear
              :options="tableOptions"
              show-search
              :filter-option="(input: string, option: { label?: string }) =>
                String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
            />
          </Col>
          <Col>
            <span class="filter-label">数据层</span>
            <Select
              v-model:value="filterLayer"
              style="width: 120px"
              placeholder="全部"
              allow-clear
              :options="layerOptions"
            />
          </Col>
        </Row>
      </Card>

      <!-- 上：仪表盘 + 趋势 -->
      <Row :gutter="16" class="qs-top-row" align="stretch">
        <Col :xs="24" :lg="9" class="qs-top-col">
          <Card class="qs-gauge-card" :bordered="false">
            <div class="qs-gauge-inner">
              <div class="qs-gauge-head">综合质量评分</div>
              <p v-if="!hasScoreData" class="qs-gauge-hint">当前周期内暂无评分记录，执行质检或调整筛选后查看</p>
              <div class="qs-gauge-chart">
                <EchartsUI ref="gaugeRef" style="height: 212px" />
              </div>
              <div class="qs-gauge-meta">
                <span class="qs-meta-line">
                  规则通过率
                  <strong
                    class="qs-meta-strong"
                    :style="{ color: displayMetricColor(stats.avgPassRate, hasScoreData) }"
                  >
                    {{ hasScoreData ? `${stats.avgPassRate}%` : '—' }}
                  </strong>
                </span>
                <span class="qs-meta-sep">·</span>
                <span class="qs-meta-muted">更新于 {{ hasScoreData ? lastUpdatedText : '—' }}</span>
              </div>
              <div v-if="lowScoreAlerts.length" class="qs-alert-strip">
                <Tag v-for="a in lowScoreAlerts" :key="a.key" color="error" class="qs-alert-tag">
                  {{ a.table }}：综合评分偏低，需要关注
                </Tag>
              </div>
              <div v-else-if="tableList.length > 0" class="qs-alert-strip qs-alert-ok">
                <Tag color="success">当前筛选范围内无低分预警表</Tag>
              </div>
            </div>
          </Card>
        </Col>
        <Col :xs="24" :lg="15" class="qs-top-col">
          <Card class="qs-chart-card" :bordered="false">
            <template #title>
              <span class="card-title-text">质量评分趋势</span>
            </template>
            <template #extra>
              <div class="chart-card-extra">
                <span class="filter-label muted">数据层</span>
                <Select
                  v-model:value="filterLayer"
                  size="small"
                  style="width: 100px"
                  placeholder="全部"
                  allow-clear
                  :options="layerOptions"
                />
                <span class="filter-label muted">周期</span>
                <Select
                  v-model:value="days"
                  size="small"
                  style="width: 100px"
                  :options="[
                    { label: '近7天', value: 7 },
                    { label: '近14天', value: 14 },
                    { label: '近30天', value: 30 },
                    { label: '近90天', value: 90 },
                  ]"
                  @change="loadData"
                />
              </div>
            </template>
            <div class="qs-trend-body">
              <EchartsUI
                v-if="trendSeriesRows.length > 0"
                ref="trendChartRef"
                class="qs-trend-chart"
                style="height: 280px"
              />
              <div v-else class="qs-trend-empty" role="status">
                <div class="qs-trend-empty-visual" aria-hidden="true">
                  <div class="qs-trend-empty-grid" />
                  <LineChartOutlined class="qs-trend-empty-icon" />
                </div>
                <div class="qs-trend-empty-title">暂无趋势数据</div>
                <p class="qs-trend-empty-desc">
                  请确认已配置质检方案并产生评分，或放宽统计周期与筛选条件
                </p>
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      <!-- 六维卡片 -->
      <div class="qs-dim-section">
        <Row :gutter="[12, 12]">
          <Col v-for="dim in dimensions" :key="dim.label" :xs="12" :sm="8" :md="8" :lg="4">
            <div class="dim-card" :class="dim.accent">
              <div class="dim-card-icon" aria-hidden="true">
                <svg v-if="dim.icon === 'shield'" class="dim-svg" viewBox="0 0 24 24" fill="none">
                  <path d="M12 3L5 6v6c0 4.5 3.5 8 7 9 3.5-1 7-4.5 7-9V6l-7-3z" stroke="currentColor" stroke-width="1.6" />
                </svg>
                <svg v-else-if="dim.icon === 'lock'" class="dim-svg" viewBox="0 0 24 24" fill="none">
                  <rect x="5" y="10" width="14" height="11" rx="2" stroke="currentColor" stroke-width="1.6" />
                  <path d="M8 10V7a4 4 0 018 0v3" stroke="currentColor" stroke-width="1.6" />
                </svg>
                <svg v-else-if="dim.icon === 'bolt'" class="dim-svg" viewBox="0 0 24 24" fill="none">
                  <path d="M13 2L4 14h7l-1 8 10-12h-7l0-8z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
                </svg>
                <svg v-else-if="dim.icon === 'swap'" class="dim-svg" viewBox="0 0 24 24" fill="none">
                  <path d="M7 7h13M17 3l4 4-4 4M17 17H4M7 21l-4-4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <svg v-else-if="dim.icon === 'clock'" class="dim-svg" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                  <path d="M12 7v6l4 2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                </svg>
                <svg v-else class="dim-svg" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                  <path d="M8 12l2.5 2.5L16 9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                </svg>
              </div>
              <div class="dim-card-label">{{ dim.label }}</div>
              <div
                class="dim-card-value"
                :class="{ 'dim-card-value--idle': !hasScoreData }"
                :style="hasScoreData ? { color: getScoreColor(dim.value) } : undefined"
              >
                {{ hasScoreData ? dim.value.toFixed(1) : '—' }}
              </div>
            </div>
          </Col>
        </Row>
      </div>

      <!-- 评分明细 -->
      <Card class="qs-detail-card" :bordered="false">
        <template #title>
          <span class="card-title-text">评分明细</span>
        </template>
        <template #extra>
          <div class="detail-extra">
            <span class="filter-label muted">检查日期</span>
            <RangePicker
              v-model:value="detailDateRange"
              style="width: 260px"
            />
            <span class="table-count">共 {{ tableList.length }} 张表</span>
          </div>
        </template>
        <Table
          :columns="tableColumns"
          :data-source="tableList"
          :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
          size="small"
          row-key="key"
          :scroll="{ x: 1320 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'layerCode'">
              <Tag :color="layerTagColor(record.layerCode)">{{ record.layerCode }}</Tag>
            </template>
            <template v-else-if="column.key === 'score'">
              <div class="cell-score">
                <span class="cell-score-num" :style="{ color: getScoreColor(record.score) }">
                  {{ record.score ?? '—' }}
                </span>
                <Progress
                  v-if="record.score !== undefined && record.score !== null"
                  :percent="record.score"
                  :stroke-color="getScoreColor(record.score)"
                  :show-info="false"
                  size="small"
                  class="cell-score-bar"
                />
              </div>
            </template>
            <template v-else-if="column.key === 'passRate'">
              <div class="cell-pass">
                <Progress
                  :percent="record.passRate || 0"
                  :stroke-color="getScoreColor(record.passRate)"
                  size="small"
                  style="width: 72px"
                />
                <span class="cell-pass-txt">{{ record.passRate ?? 0 }}%</span>
              </div>
            </template>
            <template v-else-if="column.key === 'ruleCount'">
              <Tooltip :title="`通过 ${record.passCount} / 总数 ${record.totalCount}`">
                <span>{{ record.passCount }}/{{ record.totalCount }}</span>
              </Tooltip>
            </template>
            <template v-else-if="column.key === 'completeness'">
              {{ fmtDim(record.completeness) }}
            </template>
            <template v-else-if="column.key === 'uniqueness'">
              {{ fmtDim(record.uniqueness) }}
            </template>
            <template v-else-if="column.key === 'accuracy'">
              {{ fmtDim(record.accuracy) }}
            </template>
            <template v-else-if="column.key === 'consistency'">
              {{ fmtDim(record.consistency) }}
            </template>
            <template v-else-if="column.key === 'timeliness'">
              {{ fmtDim(record.timeliness) }}
            </template>
            <template v-else-if="column.key === 'validity'">
              {{ fmtDim(record.validity) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <Button type="link" size="small" @click="openDetail(record)">
                <template #icon>
                  <EyeOutlined />
                </template>
                详情
              </Button>
            </template>
          </template>
          <template #emptyText>
            <Empty description="暂无数据" />
          </template>
        </Table>
      </Card>

      <Modal
        v-model:open="detailModalOpen"
        title="评分详情"
        width="560px"
        :footer="null"
        destroy-on-close
      >
        <template v-if="detailRecord">
          <Descriptions bordered size="small" :column="1">
            <DescriptionsItem label="数据层">
              <Tag :color="layerTagColor(detailRecord.layerCode)">{{ detailRecord.layerCode }}</Tag>
            </DescriptionsItem>
            <DescriptionsItem label="数据源">{{ detailRecord.dsName }}</DescriptionsItem>
            <DescriptionsItem label="目标表">{{ detailRecord.table }}</DescriptionsItem>
            <DescriptionsItem label="检查日期">{{ detailRecord.checkDate }}</DescriptionsItem>
            <DescriptionsItem label="综合评分">
              <Tag :color="getScoreTagColor(detailRecord.score)">{{ detailRecord.score ?? '—' }}</Tag>
              <span class="detail-sub">{{ getScoreLabel(detailRecord.score, true) }}</span>
            </DescriptionsItem>
            <DescriptionsItem label="规则通过率">{{ detailRecord.passRate ?? '—' }}%</DescriptionsItem>
            <DescriptionsItem label="通过/总数">{{ detailRecord.passCount }} / {{ detailRecord.totalCount }}</DescriptionsItem>
          </Descriptions>
          <div class="detail-dim-grid">
            <div v-for="d in [
              { k: '完整性', v: detailRecord.completeness },
              { k: '唯一性', v: detailRecord.uniqueness },
              { k: '准确性', v: detailRecord.accuracy },
              { k: '一致性', v: detailRecord.consistency },
              { k: '及时性', v: detailRecord.timeliness },
              { k: '有效性', v: detailRecord.validity },
            ]" :key="d.k" class="detail-dim-cell">
              <span class="detail-dim-k">{{ d.k }}</span>
              <span class="detail-dim-v" :style="{ color: getScoreColor(d.v) }">{{ fmtDim(d.v) }}</span>
            </div>
          </div>
        </template>
      </Modal>
    </Spin>
  </Page>
</template>

<style scoped>
.qs-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 16px;
}

.qs-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.5px;
}

.qs-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8c8c8c;
}

.qs-header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.qs-toolbar {
  margin-bottom: 16px;
  border-radius: 10px;
}

.filter-label {
  font-size: 13px;
  color: #555;
  margin-right: 8px;
}

.filter-label.muted {
  color: #8c8c8c;
  margin-right: 6px;
}

.qs-top-row {
  margin-bottom: 16px;
}

.qs-top-col {
  display: flex;
}

.qs-top-col :deep(.ant-card) {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.qs-top-col :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.qs-gauge-card {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background:
    radial-gradient(120% 80% at 50% 0%, rgba(59, 130, 246, 0.07) 0%, transparent 55%),
    linear-gradient(180deg, #fbfcff 0%, #f1f5f9 100%);
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.06);
  min-height: 380px;
}

.qs-gauge-card :deep(.ant-card-body) {
  padding: 16px 14px 18px;
}

.qs-gauge-inner {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 340px;
}

.qs-gauge-head {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  text-align: center;
  letter-spacing: 0.02em;
}

.qs-gauge-hint {
  margin: 8px 12px 0;
  text-align: center;
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}

.qs-gauge-chart {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -8px;
}

.qs-gauge-meta {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin-top: -20px;
  padding: 0 8px 4px;
}

.qs-meta-line {
  white-space: nowrap;
}

.qs-meta-strong {
  margin-left: 4px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.qs-meta-sep {
  margin: 0 8px;
  color: #cbd5e1;
}

.qs-meta-muted {
  color: #94a3b8;
  font-size: 12px;
}

.qs-alert-strip {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.qs-alert-ok {
  justify-content: center;
}

.qs-alert-tag {
  margin: 0;
  max-width: 100%;
  white-space: normal;
  height: auto;
  line-height: 1.4;
  padding: 4px 8px;
}

.qs-chart-card {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #fff;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.06);
  min-height: 380px;
}

.qs-chart-card :deep(.ant-card-head) {
  border-bottom-color: #f1f5f9;
  min-height: 48px;
  padding: 0 18px;
}

.qs-chart-card :deep(.ant-card-head-title) {
  padding: 12px 0;
}

.qs-chart-card :deep(.ant-card-extra) {
  padding: 12px 0;
}

.qs-chart-card :deep(.ant-card-body) {
  padding: 12px 18px 18px;
}

.qs-trend-body {
  position: relative;
  min-height: 280px;
}

.qs-trend-chart {
  display: block;
}

.qs-trend-empty {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px 20px;
  border-radius: 12px;
  background:
    linear-gradient(145deg, rgba(241, 245, 249, 0.9) 0%, rgba(248, 250, 252, 0.5) 48%, rgba(239, 246, 255, 0.65) 100%);
  border: 1px dashed rgba(148, 163, 184, 0.35);
}

.qs-trend-empty-visual {
  position: relative;
  width: 120px;
  height: 72px;
  margin-bottom: 16px;
}

.qs-trend-empty-grid {
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background-image:
    linear-gradient(to right, rgba(148, 163, 184, 0.12) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(148, 163, 184, 0.12) 1px, transparent 1px);
  background-size: 20px 16px;
  opacity: 0.85;
}

.qs-trend-empty-icon {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 36px;
  color: #3b82f6;
  opacity: 0.35;
  filter: drop-shadow(0 2px 8px rgba(59, 130, 246, 0.2));
}

.qs-trend-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.qs-trend-empty-desc {
  margin: 0;
  max-width: 320px;
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}

.card-title-text {
  font-size: 15px;
  font-weight: 600;
}

.chart-card-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.qs-dim-section {
  margin-bottom: 16px;
}

.dim-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 12px 14px;
  position: relative;
  overflow: hidden;
  min-height: 96px;
}

.dim-card::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  border-radius: 0 0 10px 10px;
}

.dim-accent--blue::after {
  background: linear-gradient(90deg, #1890ff, #69c0ff);
}

.dim-accent--green::after {
  background: linear-gradient(90deg, #52c41a, #95de64);
}

.dim-accent--orange::after {
  background: linear-gradient(90deg, #fa8c16, #ffc069);
}

.dim-accent--purple::after {
  background: linear-gradient(90deg, #722ed1, #b37feb);
}

.dim-accent--teal::after {
  background: linear-gradient(90deg, #13c2c2, #5cdbd3);
}

.dim-accent--red::after {
  background: linear-gradient(90deg, #ff4d4f, #ff7875);
}

.dim-card-icon {
  color: #bfbfbf;
  margin-bottom: 4px;
}

.dim-svg {
  width: 22px;
  height: 22px;
}

.dim-card-label {
  font-size: 12px;
  color: #8c8c8c;
}

.dim-card-value {
  font-size: 24px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.dim-card-value--idle {
  color: #a8b0c2 !important;
  font-weight: 650;
  font-size: 22px;
  letter-spacing: 0.04em;
}

.dim-card:has(.dim-card-value--idle)::after {
  opacity: 0.38;
}

.qs-detail-card {
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.detail-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.table-count {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: 8px;
}

.cell-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.cell-score-num {
  font-weight: 700;
  font-size: 15px;
}

.cell-score-bar {
  width: 100%;
  max-width: 88px;
}

.cell-pass {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.cell-pass-txt {
  font-size: 12px;
  color: #8c8c8c;
  min-width: 36px;
}

.detail-sub {
  margin-left: 8px;
  color: #8c8c8c;
  font-size: 12px;
}

.detail-dim-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.detail-dim-cell {
  background: #fafafa;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-dim-k {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-dim-v {
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
</style>
