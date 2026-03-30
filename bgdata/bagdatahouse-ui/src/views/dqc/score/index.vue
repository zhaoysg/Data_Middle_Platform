<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#grad5)"/>
            <path d="M12 3L14.5 8.5L21 9.5L16.5 14L17.5 21L12 18L6.5 21L7.5 14L3 9.5L9.5 8.5L12 3Z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="grad5" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#52C41A"/>
                <stop offset="100%" stop-color="#237804"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">质量评分</h1>
          <p class="page-subtitle">基于每次质检执行落库的表级评分快照；调整下方日期与列表筛选后，概览、趋势与明细一致联动</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="handleExport">
          <template #icon><DownloadOutlined /></template>
          导出报告
        </a-button>
        <a-button @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 综合评分卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="24" :lg="8">
        <div class="overall-score-card">
          <div class="overall-score-left">
            <div class="overall-label">综合质量评分</div>
            <div class="overall-value" :style="{ color: getScoreColor(overviewData.overallScore) }">
              {{ overviewData.overallScore !== undefined ? overviewData.overallScore.toFixed(1) : '-' }}
            </div>
            <div class="overall-sub">
              <span class="overall-pass-rate">
                规则通过率 {{ overviewData.rulePassRate || 0 }}%
              </span>
              <span class="overall-update">
                <ClockCircleOutlined /> 更新于 {{ overviewData.updateTime || '刚刚' }}
              </span>
            </div>
            <div class="overall-tags">
              <a-tag v-for="tag in overviewData.alerts" :key="tag" :color="getScoreColor(60)">
                {{ tag }}
              </a-tag>
            </div>
          </div>
          <div class="overall-score-right">
            <div ref="gaugeChartRef" style="width: 180px; height: 180px"></div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="16">
        <div class="trend-card">
          <div class="trend-header">
            <span class="trend-title">质量评分趋势</span>
            <a-space>
              <a-select v-model:value="trendLayer" placeholder="数据层" style="width: 100px" size="small" allowClear @change="loadTrend">
                <a-select-option value="">全部层</a-select-option>
                <a-select-option value="ODS">ODS</a-select-option>
                <a-select-option value="DWD">DWD</a-select-option>
                <a-select-option value="DWS">DWS</a-select-option>
                <a-select-option value="ADS">ADS</a-select-option>
              </a-select>
              <a-select v-model:value="trendDays" style="width: 100px" size="small" @change="loadTrend">
                <a-select-option value="7">近7天</a-select-option>
                <a-select-option value="30">近30天</a-select-option>
                <a-select-option value="90">近90天</a-select-option>
              </a-select>
            </a-space>
          </div>
          <div ref="trendChartRef" style="height: 200px"></div>
        </div>
      </a-col>
    </a-row>

    <!-- 六维度评分 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="8" :lg="4" v-for="dim in dimensionCards" :key="dim.key">
        <div class="dim-card" :style="{ borderLeft: '4px solid ' + dim.color }">
          <div class="dim-icon">
            <component :is="dim.icon" :style="{ color: dim.color }" />
          </div>
          <div class="dim-info">
            <div class="dim-name">{{ dim.name }}</div>
            <div class="dim-score" :style="{ color: getScoreColor(dim.score) }">
              {{ dim.score.toFixed(1) }}
            </div>
            <div class="dim-bar">
              <div class="dim-bar-fill" :style="{ width: dim.score + '%', background: dim.color }"></div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选 + 表格 -->
    <div class="table-card">
      <div class="table-header">
        <span class="table-title">评分明细</span>
        <div class="table-filters">
          <a-select
            v-model:value="filterLayer"
            placeholder="数据层"
            style="width: 120px"
            allowClear
            @change="onLayerFilterChange"
          >
            <a-select-option value="">全部层</a-select-option>
            <a-select-option value="ODS">ODS</a-select-option>
            <a-select-option value="DWD">DWD</a-select-option>
            <a-select-option value="DWS">DWS</a-select-option>
            <a-select-option value="ADS">ADS</a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="dateRange"
            format="YYYY-MM-DD"
            style="width: 240px"
            @change="onDateChange"
          />
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record: any) => record.id"
        @change="handleTableChange"
        :scroll="{ x: 1200 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'overallScore'">
            <div class="score-cell">
              <span class="score-value" :style="{ color: getScoreColor(record.overallScore) }">
                {{ record.overallScore !== undefined ? record.overallScore.toFixed(1) : '-' }}
              </span>
              <a-progress
                v-if="record.overallScore !== undefined"
                :percent="Number(record.overallScore)"
                :stroke-color="getScoreColor(record.overallScore)"
                :trail-color="'#f0f0f0'"
                size="small"
                style="width: 80px"
                :show-info="false"
              />
            </div>
          </template>

          <template v-if="column.key === 'layerCode'">
            <a-tag :color="getLayerColor(record.layerCode)">
              {{ record.layerCode || '-' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'rulePassRate'">
            <span :style="{ color: record.rulePassRate >= 80 ? '#52C41A' : record.rulePassRate >= 60 ? '#FA8C16' : '#FF4D4F' }">
              {{ record.rulePassRate?.toFixed(1) || 0 }}%
            </span>
          </template>

          <template v-if="column.key === 'ruleCounts'">
            <span class="text-success">{{ record.rulePassCount || 0 }}</span>
            <span class="text-muted"> / {{ record.ruleTotalCount || 0 }}</span>
          </template>

          <template v-if="column.key === 'completenessScore'">
            <span :style="{ color: getScoreColor(record.completenessScore) }">
              {{ record.completenessScore != null ? record.completenessScore.toFixed(1) : '未评估' }}
            </span>
          </template>
          <template v-if="column.key === 'uniquenessScore'">
            <span :style="{ color: getScoreColor(record.uniquenessScore) }">
              {{ record.uniquenessScore != null ? record.uniquenessScore.toFixed(1) : '未评估' }}
            </span>
          </template>
          <template v-if="column.key === 'accuracyScore'">
            <span :style="{ color: getScoreColor(record.accuracyScore) }">
              {{ record.accuracyScore != null ? record.accuracyScore.toFixed(1) : '未评估' }}
            </span>
          </template>
          <template v-if="column.key === 'consistencyScore'">
            <span :style="{ color: getScoreColor(record.consistencyScore) }">
              {{ record.consistencyScore != null ? record.consistencyScore.toFixed(1) : '未评估' }}
            </span>
          </template>
          <template v-if="column.key === 'timelinessScore'">
            <span :style="{ color: getScoreColor(record.timelinessScore) }">
              {{ record.timelinessScore != null ? record.timelinessScore.toFixed(1) : '未评估' }}
            </span>
          </template>
          <template v-if="column.key === 'validityScore'">
            <span :style="{ color: getScoreColor(record.validityScore) }">
              {{ record.validityScore != null ? record.validityScore.toFixed(1) : '未评估' }}
            </span>
          </template>

          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="showDetail(record)">
                <template #icon><EyeOutlined /></template>
                详情
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="评分详情"
      :width="800"
      :footer="null"
      :destroy-on-close="true"
    >
      <template v-if="detailRecord">
        <div class="detail-header">
          <div class="detail-title">
            <DatabaseOutlined />
            {{ detailRecord.targetTable || '未知表' }}
          </div>
          <div class="detail-meta">
            <a-tag :color="getLayerColor(detailRecord.layerCode)">{{ detailRecord.layerCode }}</a-tag>
            <span class="text-muted">{{ detailRecord.checkDate }}</span>
          </div>
        </div>

        <div class="detail-score-display">
          <div class="big-score" :style="{ color: getScoreColor(detailRecord.overallScore) }">
            {{ detailRecord.overallScore?.toFixed(1) || '-' }}
          </div>
          <div class="big-score-label">综合评分</div>
        </div>

        <a-divider />

        <div class="detail-dimensions">
          <div class="dim-row" v-for="dim in detailDimensions" :key="dim.key">
            <div class="dim-row-label">
              <span class="dim-dot" :style="{ background: dim.color }"></span>
              {{ dim.name }}
            </div>
            <div class="dim-row-score" :style="{ color: getScoreColor(dim.score) }">
              {{ dim.score != null ? Number(dim.score).toFixed(1) : '未评估' }}
            </div>
            <div class="dim-row-bar">
              <div class="dim-row-fill" :style="{ width: dim.score + '%', background: dim.color }"></div>
            </div>
          </div>
        </div>

        <a-divider />

        <div class="detail-stats">
          <div class="stat-item">
            <div class="stat-item-value text-success">{{ detailRecord.rulePassCount || 0 }}</div>
            <div class="stat-item-label">通过规则</div>
          </div>
          <div class="stat-item">
            <div class="stat-item-value text-danger">{{ (detailRecord.ruleTotalCount || 0) - (detailRecord.rulePassCount || 0) }}</div>
            <div class="stat-item-label">失败规则</div>
          </div>
          <div class="stat-item">
            <div class="stat-item-value">{{ detailRecord.ruleTotalCount || 0 }}</div>
            <div class="stat-item-label">规则总数</div>
          </div>
          <div class="stat-item">
            <div class="stat-item-value">{{ detailRecord.rulePassRate?.toFixed(1) || 0 }}%</div>
            <div class="stat-item-label">通过率</div>
          </div>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import dayjs from 'dayjs'
import {
  Button,
  Input,
  Select,
  Table,
  Modal,
  Drawer,
  Dropdown,
  Tag,
  Badge,
  Switch,
  Progress,
  DatePicker,
  Pagination,
  Space,
  Row,
  Col,
  Divider,
  Tooltip,
  message
} from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  ReloadOutlined, DownloadOutlined, EyeOutlined, DatabaseOutlined,
  ClockCircleOutlined,
  SafetyOutlined, LockOutlined, ThunderboltOutlined, SwapOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import { qualityScoreApi, executionApi } from '@/api/dqc'
import type { QualityScore } from '@/types'

const loading = ref(false)
const tableData = ref<QualityScore[]>([])
const filterLayer = ref<string>()
const dateRange = ref<any[]>([])
const trendDays = ref('7')
const trendLayer = ref<string>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const overviewData = reactive({
  overallScore: undefined as number | undefined,
  rulePassRate: 0,
  updateTime: '',
  alerts: [] as string[]
})

const dimensionCards = ref([
  { key: 'completeness', name: '完整性', score: 0, color: '#1677FF', icon: SafetyOutlined },
  { key: 'uniqueness', name: '唯一性', score: 0, color: '#52C41A', icon: LockOutlined },
  { key: 'accuracy', name: '准确性', score: 0, color: '#FA8C16', icon: ThunderboltOutlined },
  { key: 'consistency', name: '一致性', score: 0, color: '#722ED1', icon: SwapOutlined },
  { key: 'timeliness', name: '及时性', score: 0, color: '#13C2C2', icon: ClockCircleOutlined },
  { key: 'validity', name: '有效性', score: 0, color: '#FF4D4F', icon: CheckCircleOutlined }
])

const columns = [
  { title: '数据层', key: 'layerCode', width: 90 },
  { title: '目标表', dataIndex: 'targetTable', key: 'targetTable', width: 180, ellipsis: true },
  { title: '综合评分', key: 'overallScore', width: 160 },
  { title: '规则通过率', key: 'rulePassRate', width: 110 },
  { title: '通过/总数', key: 'ruleCounts', width: 100 },
  { title: '完整性', key: 'completenessScore', width: 80 },
  { title: '唯一性', key: 'uniquenessScore', width: 80 },
  { title: '准确性', key: 'accuracyScore', width: 80 },
  { title: '一致性', key: 'consistencyScore', width: 80 },
  { title: '及时性', key: 'timelinessScore', width: 80 },
  { title: '有效性', key: 'validityScore', width: 80 },
  { title: '检查日期', dataIndex: 'checkDate', key: 'checkDate', width: 120 },
  { title: '操作', key: 'actions', width: 80, fixed: 'right' }
]

const trendChartRef = ref<HTMLDivElement>()
const gaugeChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let gaugeChart: echarts.ECharts | null = null

const detailVisible = ref(false)
const detailRecord = ref<QualityScore | null>(null)

const detailDimensions = computed(() => {
  if (!detailRecord.value) return []
  const r = detailRecord.value
  return [
    { key: 'completeness', name: '完整性', score: r.completenessScore, color: '#1677FF' },
    { key: 'uniqueness', name: '唯一性', score: r.uniquenessScore, color: '#52C41A' },
    { key: 'accuracy', name: '准确性', score: r.accuracyScore, color: '#FA8C16' },
    { key: 'consistency', name: '一致性', score: r.consistencyScore, color: '#722ED1' },
    { key: 'timeliness', name: '及时性', score: r.timelinessScore, color: '#13C2C2' },
    { key: 'validity', name: '有效性', score: r.validityScore, color: '#FF4D4F' }
  ]
})

/** 将 dqc_quality_score 接口行转为表格用的数值字段（BigDecimal/字符串兼容） */
function normalizeQualityScoreRow(row: Record<string, any>): QualityScore {
  const num = (v: unknown) => {
    if (v === null || v === undefined || v === '') return undefined
    const n = Number(v)
    return Number.isFinite(n) ? n : undefined
  }
  let checkDate = row.checkDate as string | undefined
  if (checkDate && checkDate.includes('T')) {
    checkDate = checkDate.slice(0, 10)
  }
  return {
    ...row,
    checkDate,
    completenessScore: num(row.completenessScore),
    uniquenessScore: num(row.uniquenessScore),
    accuracyScore: num(row.accuracyScore),
    consistencyScore: num(row.consistencyScore),
    timelinessScore: num(row.timelinessScore),
    validityScore: num(row.validityScore),
    overallScore: num(row.overallScore),
    rulePassRate: num(row.rulePassRate),
    ruleTotalCount: row.ruleTotalCount != null ? Number(row.ruleTotalCount) : undefined,
    rulePassCount: row.rulePassCount != null ? Number(row.rulePassCount) : undefined
  }
}

const allScoreRows = ref<QualityScore[]>([])

function applyScoreTablePage() {
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = allScoreRows.value.slice(start, start + pagination.pageSize)
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await qualityScoreApi.getTableScores({
      layerCode: filterLayer.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    const raw = Array.isArray(res.data) ? res.data : []
    allScoreRows.value = raw.map((r: Record<string, any>) => normalizeQualityScoreRow(r))
    pagination.total = allScoreRows.value.length
    applyScoreTablePage()
    loadDimensionScores()
  } catch {
    allScoreRows.value = []
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  try {
    const res: any = await qualityScoreApi.getOverview({
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    if (res.data) {
      const os = res.data.overallScore
      overviewData.overallScore = os !== undefined && os !== null ? Number(os) : undefined
      const rpr = res.data.rulePassRate
      overviewData.rulePassRate = rpr !== undefined && rpr !== null ? Number(rpr) : 0
      overviewData.updateTime = new Date().toLocaleTimeString()
      const issues = res.data.issues || []
      overviewData.alerts = issues.slice(0, 8).map((i: any) => {
        if (i.tableName && i.issueDescription) return `${i.tableName}：${i.issueDescription}`
        if (i.tableName) return `${i.tableName} 需关注`
        return i.issueDescription || '质量问题'
      })
      updateGaugeChart(overviewData.overallScore ?? 0)
    }
  } catch {
    overviewData.overallScore = undefined
    overviewData.alerts = []
    updateGaugeChart(0)
  }
}

async function loadDimensionScores() {
  try {
    const res: any = await qualityScoreApi.getDimensionScores({
      layerCode: filterLayer.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    if (res.data && Array.isArray(res.data)) {
      const dimKeyMap: Record<string, string> = {
        COMPLETENESS: 'completeness',
        UNIQUENESS: 'uniqueness',
        ACCURACY: 'accuracy',
        CONSISTENCY: 'consistency',
        TIMELINESS: 'timeliness',
        VALIDITY: 'validity'
      }
      dimensionCards.value.forEach(card => {
        const dimItem = res.data.find((d: any) => dimKeyMap[d.dimensionCode] === card.key)
        card.score = dimItem != null && dimItem.score != null ? Number(dimItem.score) : 0
      })
    } else {
      dimensionCards.value.forEach(card => { card.score = 0 })
    }
  } catch {
    dimensionCards.value.forEach(card => { card.score = 0 })
  }
}

async function loadTrend() {
  await nextTick()
  let startDate: string
  let endDate: string
  if (dateRange.value?.[0] && dateRange.value?.[1]) {
    startDate = dateRange.value[0].format('YYYY-MM-DD')
    endDate = dateRange.value[1].format('YYYY-MM-DD')
  } else {
    const days = parseInt(trendDays.value, 10) || 7
    endDate = dayjs().format('YYYY-MM-DD')
    startDate = dayjs().subtract(days - 1, 'day').format('YYYY-MM-DD')
  }
  try {
    const res: any = await qualityScoreApi.getTrend({
      layerCode: trendLayer.value || undefined,
      startDate,
      endDate
    })
    const trendPayload = res.data?.overallTrend ?? res.data
    const data = Array.isArray(trendPayload) ? trendPayload : []
    updateTrendChart(data)
  } catch {
    updateTrendChart([])
  }
  requestAnimationFrame(() => trendChart?.resize())
}

function updateTrendChart(data: any[]) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const sorted = [...data].sort((a, b) => String(a.date || a.checkDate || '').localeCompare(String(b.date || b.checkDate || '')))
  const dates = sorted.map(d => {
    const raw = d.date || d.checkDate || ''
    return typeof raw === 'string' && raw.length >= 10 ? raw.slice(5, 10) : String(raw).slice(5)
  })
  const scores = sorted.map(d => Number(d.score ?? d.overallScore) || 0)

  if (sorted.length === 0) {
    trendChart.setOption({
      graphic: [{
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '所选时间范围内暂无评分快照\n请先执行质检方案或调整日期 / 数据层',
          fill: '#8C8C8C',
          fontSize: 13,
          textAlign: 'center'
        }
      }],
      xAxis: { type: 'category', data: [], show: false },
      yAxis: { type: 'value', show: false },
      series: []
    }, true)
    return
  }

  trendChart.setOption({
    graphic: [],
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        const idx = p.dataIndex
        const row = sorted[idx]
        const pr = row?.passRate != null ? Number(row.passRate).toFixed(1) : '—'
        return `${p.name}<br/><span style="color:${getScoreColor(p.value)};font-weight:700;font-size:16px">${p.value}分</span><br/><span style="color:#8C8C8C;font-size:12px">规则通过率 ${pr}%</span>`
      }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#E8E8E8' } },
      axisLabel: { color: '#8C8C8C', fontSize: 11 },
      show: true
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#F0F0F0', type: 'dashed' } },
      axisLabel: { color: '#8C8C8C', fontSize: 11 },
      show: true
    },
    series: [{
      data: scores,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(22, 119, 255, 0.15)' },
          { offset: 1, color: 'rgba(22, 119, 255, 0.01)' }
        ])
      },
      lineStyle: { color: '#1677FF', width: 2.5 },
      itemStyle: { color: '#1677FF' }
    }]
  }, true)
}

function updateGaugeChart(score: number) {
  if (!gaugeChartRef.value) return
  if (!gaugeChart) {
    gaugeChart = echarts.init(gaugeChartRef.value)
  }
  gaugeChart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      splitNumber: 5,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#FF4D4F' },
          { offset: 0.5, color: '#FA8C16' },
          { offset: 1, color: '#52C41A' }
        ])
      },
      progress: {
        show: true,
        width: 14,
        roundCap: true
      },
      pointer: { show: false },
      axisLine: {
        lineStyle: {
          width: 14,
          color: [[1, '#F0F0F0']]
        },
        roundCap: true
      },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        fontSize: 28,
        fontWeight: 700,
        fontFamily: 'JetBrains Mono',
        color: getScoreColor(score),
        formatter: () => score > 0 ? score.toFixed(1) : '-',
        offsetCenter: [0, '10%']
      },
      data: [{ value: score || 0 }]
    }]
  })
}

function showDetail(record: QualityScore) {
  detailRecord.value = record
  detailVisible.value = true
}

async function handleExport() {
  try {
    await executionApi.export({
      planId: undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    message.success('导出已开始')
  } catch {
    message.error('导出失败')
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  applyScoreTablePage()
}

function onLayerFilterChange() {
  pagination.current = 1
  loadData()
}

function onDateChange() {
  pagination.current = 1
  loadData()
  loadOverview()
  loadTrend()
}

function getScoreColor(score: number | undefined) {
  if (score === null || score === undefined) return '#999'
  if (score === 0) return '#999'
  if (score >= 90) return '#52C41A'
  if (score >= 70) return '#FA8C16'
  return '#FF4D4F'
}

function getLayerColor(layer: string | undefined) {
  const map: Record<string, string> = { ODS: 'blue', DWD: 'green', DWS: 'orange', ADS: 'purple' }
  return map[layer || ''] || 'default'
}

function handleResize() {
  trendChart?.resize()
  gaugeChart?.resize()
}

onMounted(() => {
  if (!dateRange.value?.length) {
    dateRange.value = [dayjs().subtract(6, 'day'), dayjs()]
  }
  void loadData().then(() => {
    void loadOverview()
    void nextTick().then(() => loadTrend())
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  gaugeChart?.dispose()
})
</script>

<style lang="less" scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    .header-icon { flex-shrink: 0; }
    .page-title { font-size: 22px; font-weight: 700; color: #1F1F1F; margin: 0; }
    .page-subtitle { font-size: 13px; color: #8C8C8C; margin: 4px 0 0; }
  }
  .header-right { display: flex; gap: 12px; }
}

.overall-score-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
  .overall-score-left {
    flex: 1;
    .overall-label { font-size: 13px; color: #8C8C8C; margin-bottom: 8px; }
    .overall-value { font-size: 48px; font-weight: 700; line-height: 1; }
    .overall-sub { font-size: 12px; color: #8C8C8C; margin-top: 8px; display: flex; gap: 12px; }
    .overall-tags { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 6px; }
  }
  .overall-score-right { flex-shrink: 0; }
}

.trend-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  height: 100%;
  .trend-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    .trend-title { font-size: 15px; font-weight: 600; color: #1F1F1F; }
  }
}

.dim-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  gap: 12px;
  align-items: flex-start;
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  .dim-icon { font-size: 22px; padding-top: 2px; }
  .dim-info { flex: 1; min-width: 0; }
  .dim-name { font-size: 12px; color: #8C8C8C; margin-bottom: 4px; }
  .dim-score { font-size: 22px; font-weight: 700; line-height: 1.2; }
  .dim-bar {
    height: 4px;
    background: #F0F0F0;
    border-radius: 2px;
    margin-top: 6px;
    .dim-bar-fill { height: 100%; border-radius: 2px; transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1); }
  }
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  .table-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    .table-title { font-size: 15px; font-weight: 600; color: #1F1F1F; }
    .table-filters { display: flex; gap: 12px; align-items: center; }
  }
}

.score-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  .score-value { font-weight: 700; font-size: 14px; min-width: 36px; }
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  .detail-title {
    font-size: 16px;
    font-weight: 600;
    color: #1F1F1F;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .detail-meta { display: flex; gap: 12px; align-items: center; }
}

.detail-score-display {
  text-align: center;
  padding: 20px 0;
  .big-score { font-size: 64px; font-weight: 700; line-height: 1; }
  .big-score-label { font-size: 13px; color: #8C8C8C; margin-top: 8px; }
}

.detail-dimensions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  .dim-row {
    display: grid;
    grid-template-columns: 80px 60px 1fr;
    align-items: center;
    gap: 12px;
    .dim-row-label { font-size: 13px; color: #555; display: flex; align-items: center; gap: 8px; }
    .dim-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
    .dim-row-score { font-size: 16px; font-weight: 700; text-align: right; }
    .dim-row-bar { height: 6px; background: #F0F0F0; border-radius: 3px; }
    .dim-row-fill { height: 100%; border-radius: 3px; transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1); }
  }
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  text-align: center;
  .stat-item {
    padding: 16px;
    background: #F5F7FA;
    border-radius: 8px;
    .stat-item-value { font-size: 24px; font-weight: 700; }
    .stat-item-label { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
  }
}
</style>
