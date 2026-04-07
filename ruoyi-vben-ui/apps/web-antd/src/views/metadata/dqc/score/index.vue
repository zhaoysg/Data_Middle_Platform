<script setup lang="ts">
import type { EchartsUIType } from '@vben/plugins/echarts';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import {
  Card,
  Col,
  Input,
  Progress,
  Row,
  Select,
  Spin,
  Tag,
  Table,
  Tooltip,
} from 'ant-design-vue';
import { computed, onMounted, ref, watch } from 'vue';

import { dqcScoreTrend } from '#/api/metadata/dqc/score';
import type { DqcQualityScore } from '#/api/metadata/model';

const loading = ref(false);
const days = ref(30);
const trendData = ref<DqcQualityScore[]>([]);

// Filter controls
const filterDs = ref<string>('');
const filterTable = ref<string>('');

// Chart refs
const trendChartRef = ref<EchartsUIType>();
const dimChartRef = ref<EchartsUIType>();

const { renderEcharts: renderTrend, updateData: updateTrendData } = useEcharts(trendChartRef);
const { renderEcharts: renderDim, updateData: updateDimData } = useEcharts(dimChartRef);

function getScoreColor(score?: number) {
  if (score === undefined) return '#8c8c8c';
  if (score >= 90) return '#52c41a';
  if (score >= 70) return '#faad14';
  return '#ff4d4f';
}

function getScoreTagColor(score?: number) {
  if (score === undefined) return 'default';
  if (score >= 90) return 'success';
  if (score >= 70) return 'warning';
  return 'error';
}

function getScoreLabel(score?: number) {
  if (score === undefined) return '暂无数据';
  if (score >= 90) return '优秀';
  if (score >= 70) return '良好';
  return '较差';
}

// Unique data sources & tables for filter dropdowns
const dsOptions = computed(() => {
  const set = new Set(trendData.value.map((r) => r.dsName).filter(Boolean));
  return [...set].map((v) => ({ label: v, value: v }));
});

const tableOptions = computed(() => {
  const list = trendData.value.filter(
    (r) => !filterDs.value || r.dsName === filterDs.value
  );
  const set = new Set(list.map((r) => r.targetTable).filter(Boolean));
  return [...set].map((v) => ({ label: v, value: v }));
});

// Filtered data
const filteredData = computed(() => {
  return trendData.value.filter((r) => {
    if (filterDs.value && r.dsName !== filterDs.value) return false;
    if (filterTable.value && r.targetTable !== filterTable.value) return false;
    return true;
  });
});

// Per-table aggregated score (latest)
const tableScoreMap = computed(() => {
  const map = new Map<string, DqcQualityScore>();
  for (const r of filteredData.value) {
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
  return map;
});

const tableList = computed(() => {
  return [...tableScoreMap.value.values()].map((r) => ({
    key: `${r.targetDsId}-${r.targetTable}`,
    dsId: r.targetDsId,
    dsName: r.dsName || '未知数据源',
    table: r.targetTable || '',
    score: r.overallScore,
    passRate: r.rulePassRate,
    failCount: r.ruleFailCount,
    totalCount: r.ruleTotalCount,
    dimensions: {
      completeness: r.completenessScore,
      uniqueness: r.uniquenessScore,
      accuracy: r.accuracyScore,
      consistency: r.consistencyScore,
      timeliness: r.timelinessScore,
      validity: r.validityScore,
    },
  }));
});

// Stats
const stats = computed(() => {
  const data = filteredData.value;
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
    data.reduce((s, r) => s + (r.rulePassRate || 0), 0) / n
  );
  const tables = new Set(data.map((r) => `${r.targetDsId}-${r.targetTable}`)).size;
  const totalRules = data.reduce((s, r) => s + (r.ruleTotalCount || 0), 0);
  const alertCount = data.filter(
    (r) => r.overallScore !== undefined && r.overallScore < 70
  ).length;
  return { avgOverall, avgPassRate, totalTables: tables, totalRules, alertCount };
});

// Dimension breakdown for bar chart
const dimBarOption = computed(() => {
  const data = filteredData.value;
  if (data.length === 0) return {};
  const dims = [
    { label: '完整性', key: 'completenessScore' as keyof DqcQualityScore },
    { label: '唯一性', key: 'uniquenessScore' as keyof DqcQualityScore },
    { label: '准确性', key: 'accuracyScore' as keyof DqcQualityScore },
    { label: '一致性', key: 'consistencyScore' as keyof DqcQualityScore },
    { label: '时效性', key: 'timelinessScore' as keyof DqcQualityScore },
    { label: '有效性', key: 'validityScore' as keyof DqcQualityScore },
  ];

  const vals = dims.map((d) => avg(data, d.key));

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 20, right: 20, bottom: 10, left: 10, containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}' } },
    yAxis: {
      type: 'category',
      data: dims.map((d) => d.label),
      axisLabel: { fontSize: 12 },
    },
    series: [
      {
        type: 'bar',
        data: vals.map((v) => ({
          value: v,
          itemStyle: { color: getScoreColor(v), borderRadius: [0, 4, 4, 0] },
        })),
        barWidth: 18,
        label: { show: true, position: 'right', formatter: '{c}分', fontSize: 11 },
      },
    ],
  };
});

// Trend — area chart for overall score
const trendOption = computed(() => {
  const dates = filteredData.value.map((r) => r.checkDate?.slice(5) || '');
  const overallScores = filteredData.value.map((r) => r.overallScore || 0);
  const passRates = filteredData.value.map((r) => r.rulePassRate || 0);

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['综合评分', '通过率'], bottom: 0 },
    grid: { top: 10, right: 20, bottom: 40, left: 50 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { fontSize: 10 },
    },
    yAxis: [
      {
        type: 'value',
        name: '评分',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}分' },
      },
      {
        type: 'value',
        name: '通过率',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%' },
      },
    ],
    series: [
      {
        name: '综合评分',
        type: 'line',
        data: overallScores,
        smooth: true,
        lineStyle: { width: 2 },
        itemStyle: { color: '#5470c6' },
        areaStyle: { color: 'rgba(84,112,198,0.15)' },
      },
      {
        name: '通过率',
        type: 'line',
        yAxisIndex: 1,
        data: passRates,
        smooth: true,
        lineStyle: { width: 2, type: 'dashed' },
        itemStyle: { color: '#91cc75' },
      },
    ],
  };
});

function avg(list: DqcQualityScore[], field: keyof DqcQualityScore): number {
  const vals = list.map((r) => r[field]).filter((v): v is number => v !== undefined);
  if (vals.length === 0) return 0;
  return Math.round(vals.reduce((s, v) => s + v, 0) / vals.length * 10) / 10;
}

// Table columns
const tableColumns = [
  {
    title: '数据源',
    dataIndex: 'dsName',
    key: 'dsName',
    width: 140,
    ellipsis: true,
  },
  {
    title: '表名',
    dataIndex: 'table',
    key: 'table',
    width: 200,
    ellipsis: true,
  },
  {
    title: '等级',
    key: 'level',
    width: 80,
    align: 'center' as const,
  },
  {
    title: '综合评分',
    dataIndex: 'score',
    key: 'score',
    width: 120,
    align: 'center' as const,
  },
  {
    title: '规则通过率',
    key: 'passRate',
    width: 140,
    align: 'center' as const,
  },
  {
    title: '失败/总数',
    key: 'ruleCount',
    width: 120,
    align: 'center' as const,
  },
  {
    title: '六维评分',
    key: 'dimensions',
    minWidth: 220,
  },
];

async function loadData() {
  loading.value = true;
  try {
    trendData.value = (await dqcScoreTrend({ days: days.value })) || [];
  } finally {
    loading.value = false;
  }
}

watch(trendData, () => {
  if (trendData.value.length > 0) {
    updateTrendData(trendOption.value);
    updateDimData(dimBarOption.value);
  }
});

onMounted(() => {
  loadData();
  setTimeout(() => {
    if (trendData.value.length > 0) {
      renderTrend(trendOption.value);
      renderDim(dimBarOption.value);
    }
  }, 100);
});
</script>

<template>
  <Page :auto-content-height="true">
    <Spin :spinning="loading">
      <!-- Header: controls -->
      <Card class="score-controls-card">
        <Row :gutter="16" align="middle">
          <Col>
            <span class="filter-label">统计周期：</span>
            <a-select v-model="days" style="width: 130px" @change="loadData">
              <a-select-option :value="7">近 7 天</a-select-option>
              <a-select-option :value="14">近 14 天</a-select-option>
              <a-select-option :value="30">近 30 天</a-select-option>
              <a-select-option :value="90">近 90 天</a-select-option>
            </a-select>
          </Col>
          <Col>
            <span class="filter-label">数据源：</span>
            <a-select
              v-model="filterDs"
              style="width: 160px"
              placeholder="全部数据源"
              allow-clear
              :options="dsOptions"
            />
          </Col>
          <Col>
            <span class="filter-label">表名：</span>
            <a-select
              v-model="filterTable"
              style="width: 200px"
              placeholder="全部表"
              allow-clear
              :options="tableOptions"
              show-search
              :filter-option="(input, option) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
            />
          </Col>
          <Col>
            <a-button @click="loadData">刷新</a-button>
          </Col>
          <Col style="margin-left: auto">
            <span class="score-page-title">质量评分分析</span>
          </Col>
        </Row>
      </Card>

      <!-- Stats summary row -->
      <Row :gutter="16" class="score-stats-row">
        <Col :span="4">
          <Card class="score-stat-card">
            <div class="stat-score" :style="{ color: getScoreColor(stats.avgOverall) }">
              {{ stats.avgOverall }}
              <span class="stat-unit">分</span>
            </div>
            <div class="stat-label">综合评分</div>
            <div class="stat-sub">{{ getScoreLabel(stats.avgOverall) }}</div>
          </Card>
        </Col>
        <Col :span="4">
          <Card class="score-stat-card">
            <div class="stat-score" :style="{ color: getScoreColor(stats.avgPassRate) }">
              {{ stats.avgPassRate }}
              <span class="stat-unit">%</span>
            </div>
            <div class="stat-label">平均通过率</div>
            <Progress
              :percent="stats.avgPassRate"
              :stroke-color="getScoreColor(stats.avgPassRate)"
              :show-info="false"
              size="small"
              style="margin-top: 6px"
            />
          </Card>
        </Col>
        <Col :span="4">
          <Card class="score-stat-card">
            <div class="stat-score"> {{ stats.totalTables }} </div>
            <div class="stat-label">被监控表数</div>
          </Card>
        </Col>
        <Col :span="4">
          <Card class="score-stat-card">
            <div class="stat-score"> {{ stats.totalRules }} </div>
            <div class="stat-label">质检规则数</div>
          </Card>
        </Col>
        <Col :span="4">
          <Card class="score-stat-card" :class="{ 'card-alert': stats.alertCount > 0 }">
            <div class="stat-score" :style="{ color: stats.alertCount > 0 ? '#ff4d4f' : '#52c41a' }">
              {{ stats.alertCount }}
            </div>
            <div class="stat-label">预警表数</div>
            <div class="stat-sub" :style="{ color: stats.alertCount > 0 ? '#ff4d4f' : '#52c41a' }">
              {{ stats.alertCount > 0 ? '评分 &lt; 70 分' : '无预警' }}
            </div>
          </Card>
        </Col>
      </Row>

      <!-- Charts + dim bar side by side -->
      <Row :gutter="16" class="score-charts-row">
        <!-- Trend -->
        <Col :span="15">
          <Card title="评分趋势（按日期）" class="score-chart-card">
            <EchartsUI ref="trendChartRef" style="height: 280px" />
          </Card>
        </Col>
        <!-- Dimension bar -->
        <Col :span="9">
          <Card title="六维平均得分" class="score-chart-card">
            <EchartsUI ref="dimChartRef" style="height: 280px" />
          </Card>
        </Col>
      </Row>

      <!-- Table list -->
      <Card title="表质量明细" class="score-table-card">
        <template #extra>
          <span style="font-size: 12px; color: #8c8c8c">
            共 {{ tableList.length }} 张表
          </span>
        </template>
        <Table
          :columns="tableColumns"
          :data-source="tableList"
          :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` }"
          size="small"
          row-key="key"
          :scroll="{ x: 1000 }"
        >
          <template #bodyCell="{ column, record }">
            <!-- Level badge -->
            <template v-if="column.key === 'level'">
              <Tag v-if="record.score >= 90" color="green">A</Tag>
              <Tag v-else-if="record.score >= 70" color="orange">B</Tag>
              <Tag v-else-if="record.score !== undefined" color="red">C</Tag>
              <Tag v-else>N/A</Tag>
            </template>

            <!-- Score -->
            <template v-else-if="column.key === 'score'">
              <span
                style="font-weight: 600; font-size: 15px"
                :style="{ color: getScoreColor(record.score) }"
              >
                {{ record.score ?? '-' }}
              </span>
            </template>

            <!-- Pass rate -->
            <template v-else-if="column.key === 'passRate'">
              <div style="display: flex; align-items: center; gap: 8px">
                <Progress
                  :percent="record.passRate || 0"
                  :stroke-color="getScoreColor(record.passRate)"
                  size="small"
                  style="width: 80px"
                />
                <span style="font-size: 12px; color: #8c8c8c">{{ record.passRate || 0 }}%</span>
              </div>
            </template>

            <!-- Rule count -->
            <template v-else-if="column.key === 'ruleCount'">
              <Tooltip :title="`通过 ${(record.totalCount || 0) - (record.failCount || 0)} 条`">
                <span style="color: #52c41a">{{ (record.totalCount || 0) - (record.failCount || 0) }}</span>
                <span style="color: #d9d9d9; margin: 0 2px">/</span>
                <span>{{ record.totalCount || 0 }}</span>
              </Tooltip>
            </template>

            <!-- Dimensions -->
            <template v-else-if="column.key === 'dimensions'">
              <div class="dim-bar-wrap">
                <Tooltip title="完整性">
                  <div class="dim-bar-item">
                    <span class="dim-bar-label">完</span>
                    <Progress
                      :percent="record.dimensions.completeness || 0"
                      :stroke-color="getScoreColor(record.dimensions.completeness)"
                      size="small"
                      style="width: 60px"
                    />
                  </div>
                </Tooltip>
                <Tooltip title="唯一性">
                  <div class="dim-bar-item">
                    <span class="dim-bar-label">唯</span>
                    <Progress
                      :percent="record.dimensions.uniqueness || 0"
                      :stroke-color="getScoreColor(record.dimensions.uniqueness)"
                      size="small"
                      style="width: 60px"
                    />
                  </div>
                </Tooltip>
                <Tooltip title="准确性">
                  <div class="dim-bar-item">
                    <span class="dim-bar-label">准</span>
                    <Progress
                      :percent="record.dimensions.accuracy || 0"
                      :stroke-color="getScoreColor(record.dimensions.accuracy)"
                      size="small"
                      style="width: 60px"
                    />
                  </div>
                </Tooltip>
                <Tooltip title="一致性">
                  <div class="dim-bar-item">
                    <span class="dim-bar-label">一</span>
                    <Progress
                      :percent="record.dimensions.consistency || 0"
                      :stroke-color="getScoreColor(record.dimensions.consistency)"
                      size="small"
                      style="width: 60px"
                    />
                  </div>
                </Tooltip>
                <Tooltip title="时效性">
                  <div class="dim-bar-item">
                    <span class="dim-bar-label">时</span>
                    <Progress
                      :percent="record.dimensions.timeliness || 0"
                      :stroke-color="getScoreColor(record.dimensions.timeliness)"
                      size="small"
                      style="width: 60px"
                    />
                  </div>
                </Tooltip>
                <Tooltip title="有效性">
                  <div class="dim-bar-item">
                    <span class="dim-bar-label">有</span>
                    <Progress
                      :percent="record.dimensions.validity || 0"
                      :stroke-color="getScoreColor(record.dimensions.validity)"
                      size="small"
                      style="width: 60px"
                    />
                  </div>
                </Tooltip>
              </div>
            </template>
          </template>
        </Table>
      </Card>
    </Spin>
  </Page>
</template>

<style scoped>
/* Controls */
.score-controls-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.filter-label {
  font-size: 13px;
  color: #555;
  margin-right: 4px;
}

.score-page-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.5px;
}

/* Stats */
.score-stats-row {
  margin-bottom: 16px;
}

.score-stat-card {
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  text-align: center;
  padding: 4px 0;
}

.stat-score {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  color: #8c8c8c;
  margin-left: 2px;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.stat-sub {
  font-size: 11px;
  margin-top: 2px;
}

.card-alert {
  border-color: #ffccc7 !important;
  background: #fffaf5 !important;
}

/* Charts */
.score-charts-row {
  margin-bottom: 16px;
}

.score-chart-card {
  border-radius: 10px;
}

/* Table */
.score-table-card {
  border-radius: 10px;
}

/* Dimension bars in table */
.dim-bar-wrap {
  display: flex;
  gap: 6px;
  align-items: center;
}

.dim-bar-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dim-bar-label {
  font-size: 11px;
  color: #8c8c8c;
  width: 12px;
  text-align: center;
  flex-shrink: 0;
}
</style>
