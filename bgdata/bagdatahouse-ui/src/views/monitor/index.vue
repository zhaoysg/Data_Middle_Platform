<template>
  <div class="page-container">

    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#monitorGrad)"/>
            <path d="M4 14L8 8L12 12L16 6L20 10" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="8" cy="8" r="1.5" fill="white" opacity="0.8"/>
            <circle cx="12" cy="12" r="1.5" fill="white" opacity="0.8"/>
            <circle cx="16" cy="6" r="1.5" fill="white" opacity="0.8"/>
            <defs>
              <linearGradient id="monitorGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FF4D4F"/>
                <stop offset="100%" stop-color="#FA8C16"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据监控中心</h1>
          <p class="page-subtitle">任务执行监控 · 指标监控 · 告警规则 · 告警记录</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="loadOverview">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="6" v-for="stat in statCards" :key="stat.label">
        <div class="stat-card" :style="{ background: stat.bg }">
          <div class="stat-icon-wrapper">
            <component :is="stat.icon" style="font-size: 22px; color: white; opacity: 0.9" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-trend" v-if="stat.trend !== undefined">
              <component :is="stat.trend >= 0 ? CaretUpOutlined : CaretDownOutlined" style="font-size: 12px" />
              <span>{{ Math.abs(stat.trend) }}{{ stat.trendUnit }}</span>
              <span class="trend-period">{{ stat.trendPeriod }}</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="24" :lg="16">
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-title">
              <LineChartOutlined />
              执行趋势（近7天）
            </div>
            <div class="chart-actions">
              <a-radio-group v-model:value="trendDays" button-style="solid" size="small" @change="loadTrend">
                <a-radio-button value="7">近7天</a-radio-button>
                <a-radio-button value="14">近14天</a-radio-button>
                <a-radio-button value="30">近30天</a-radio-button>
              </a-radio-group>
            </div>
          </div>
          <div ref="trendChartRef" style="height: 260px"></div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="8">
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-title">
              <PieChartOutlined />
              任务类型分布
            </div>
          </div>
          <div ref="pieChartRef" style="height: 260px"></div>
        </div>
      </a-col>
    </a-row>

    <!-- 任务类型 + 告警分布 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-title">
              <AlertOutlined />
              告警等级分布（今日）
            </div>
            <a-tag :color="alertTagColor" v-if="alertTagColor">
              {{ alertTagText }}
            </a-tag>
          </div>
          <div ref="alertDistChartRef" style="height: 220px"></div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-card-header">
            <div class="chart-title">
              <ClockCircleOutlined />
              各时段执行量（今日）
            </div>
          </div>
          <div ref="hourlyChartRef" style="height: 220px"></div>
        </div>
      </a-col>
    </a-row>

    <!-- 最近执行 + 待处理告警 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="24" :lg="14">
        <div class="table-card">
          <div class="table-card-header">
            <div class="table-title">
              <HistoryOutlined />
              最近执行记录
            </div>
            <a-button type="link" size="small" @click="$router.push('/monitor/task-execution')">
              查看全部 <ArrowRightOutlined />
            </a-button>
          </div>
          <a-table
            :columns="recentColumns"
            :data-source="recentExecutions"
            :loading="recentLoading"
            :pagination="false"
            :row-key="(record: any) => record.id"
            size="small"
            :scroll="{ x: 800 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)" class="status-tag">
                  <span class="status-dot" :class="'dot-' + record.status?.toLowerCase()"></span>
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'elapsedMs'">
                <span class="mono-text">{{ formatDuration(record.elapsedMs) }}</span>
              </template>
              <template v-if="column.key === 'triggerType'">
                <a-tag :color="getTriggerColor(record.triggerType)">{{ getTriggerLabel(record.triggerType) }}</a-tag>
              </template>
              <template v-if="column.key === 'actions'">
                <a-tooltip title="查看详情">
                  <a-button type="text" size="small" @click="showExecutionDetail(record)">
                    <template #icon><EyeOutlined /></template>
                  </a-button>
                </a-tooltip>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>

      <a-col :xs="24" :lg="10">
        <div class="table-card">
          <div class="table-card-header">
            <div class="table-title">
              <BellOutlined />
              待处理告警
            </div>
            <a-badge :count="pendingAlertCount" :overflow-count="99" />
            <a-button type="link" size="small" @click="$router.push('/monitor/alert-record')">
              查看全部 <ArrowRightOutlined />
            </a-button>
          </div>

          <div v-if="recentAlerts.length === 0 && !recentAlertLoading" class="alert-empty">
            <CheckCircleOutlined style="font-size: 28px; color: #52C41A; margin-bottom: 8px" />
            <div style="font-size: 13px; color: #52C41A; font-weight: 500">暂无待处理告警</div>
            <div style="font-size: 12px; color: #8C8C8C; margin-top: 4px">系统运行正常</div>
          </div>

          <div v-else class="alert-list">
            <div
              v-for="alert in recentAlerts"
              :key="alert.id"
              class="alert-item"
              :class="'alert-level-' + (alert.alertLevel || 'warn').toLowerCase()"
              @click="showAlertDetail(alert)"
            >
              <div class="alert-left">
                <div class="alert-level-badge" :class="'badge-' + (alert.alertLevel || 'warn').toLowerCase()">
                  {{ getAlertLevelName(alert.alertLevel) }}
                </div>
              </div>
              <div class="alert-body">
                <div class="alert-title">{{ alert.alertTitle || '告警通知' }}</div>
                <div class="alert-meta">
                  <span>{{ alert.ruleName }}</span>
                  <span class="alert-sep">·</span>
                  <span>{{ formatTime(alert.createTime) }}</span>
                </div>
              </div>
              <div class="alert-right">
                <component
                  :is="getAlertStatusIcon(alert.status)"
                  :style="{ color: getAlertStatusColor(alert.status) }"
                />
              </div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 执行详情抽屉 -->
    <a-drawer
      v-model:open="executionDrawerVisible"
      :title="'执行详情 — ' + (currentExecution?.taskName || '')"
      :width="800"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="currentExecution">
        <div class="execution-info-grid">
          <div class="exec-info-card">
            <div class="exec-info-label">执行状态</div>
            <div class="exec-info-value">
              <a-tag :color="getStatusColor(currentExecution.status)">
                {{ getStatusLabel(currentExecution.status) }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">任务类型</div>
            <div class="exec-info-value">
              <a-tag :color="getTaskTypeColor(currentExecution.taskType)">
                {{ currentExecution.taskTypeName || currentExecution.taskType }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">执行耗时</div>
            <div class="exec-info-value mono-value">{{ formatDuration(currentExecution.elapsedMs) }}</div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">触发方式</div>
            <div class="exec-info-value">
              <a-tag :color="getTriggerColor(currentExecution.triggerType)">
                {{ getTriggerLabel(currentExecution.triggerType) }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">开始时间</div>
            <div class="exec-info-value">{{ currentExecution.startTime || '-' }}</div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">结束时间</div>
            <div class="exec-info-value">{{ currentExecution.endTime || '-' }}</div>
          </div>
          <div class="exec-info-card" v-if="currentExecution.triggerUserName">
            <div class="exec-info-label">触发人</div>
            <div class="exec-info-value">{{ currentExecution.triggerUserName }}</div>
          </div>
          <div class="exec-info-card" v-if="currentExecution.dsName">
            <div class="exec-info-label">数据源</div>
            <div class="exec-info-value">{{ currentExecution.dsName }}</div>
          </div>
        </div>

        <div v-if="currentExecution.progress !== undefined" class="progress-section">
          <div class="section-title">执行进度</div>
          <a-progress
            :percent="currentExecution.progress"
            :stroke-color="getProgressColor(currentExecution.progress)"
            status="active"
          />
        </div>

        <div v-if="currentExecution.errorMsg" class="error-section">
          <div class="section-title text-danger">
            <CloseCircleOutlined /> 错误信息
          </div>
          <pre class="error-pre">{{ currentExecution.errorMsg }}</pre>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="executionDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="currentExecution && currentExecution.status === 'RUNNING'"
            danger
            :loading="cancelling"
            @click="handleCancelExecution"
          >
            <template #icon><StopOutlined /></template>
            取消执行
          </a-button>
          <a-button
            v-if="currentExecution && (currentExecution.status === 'FAILED' || currentExecution.status === 'SUCCESS')"
            type="primary"
            :loading="retrying"
            @click="handleRetryExecution"
          >
            <template #icon><RedoOutlined /></template>
            重新执行
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 告警详情抽屉 -->
    <a-drawer
      v-model:open="alertDrawerVisible"
      :title="'告警详情 — ' + (currentAlert?.alertNo || '')"
      :width="640"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="currentAlert">
        <div class="execution-info-grid">
          <div class="exec-info-card">
            <div class="exec-info-label">告警编号</div>
            <div class="exec-info-value mono-value">{{ currentAlert.alertNo }}</div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">告警等级</div>
            <div class="exec-info-value">
              <a-tag :color="getAlertLevelColor(currentAlert.alertLevel)">
                {{ getAlertLevelName(currentAlert.alertLevel) }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">告警状态</div>
            <div class="exec-info-value">
              <a-tag :color="getAlertStatusColor(currentAlert.status)">
                {{ getAlertStatusName(currentAlert.status) }}
              </a-tag>
            </div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">触发时间</div>
            <div class="exec-info-value">{{ currentAlert.createTime || '-' }}</div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">触发值</div>
            <div class="exec-info-value mono-value">{{ currentAlert.triggerValue ?? '-' }}</div>
          </div>
          <div class="exec-info-card">
            <div class="exec-info-label">阈值</div>
            <div class="exec-info-value mono-value">{{ currentAlert.thresholdValue ?? '-' }}</div>
          </div>
          <div class="exec-info-card" v-if="currentAlert.targetName" style="grid-column: span 2">
            <div class="exec-info-label">告警对象</div>
            <div class="exec-info-value">{{ currentAlert.targetType }} · {{ currentAlert.targetName }}</div>
          </div>
        </div>

        <div class="alert-content-section">
          <div class="section-title">告警内容</div>
          <div class="alert-content-box">
            <div class="alert-title-text">{{ currentAlert.alertTitle || '告警通知' }}</div>
            <div v-if="currentAlert.alertContent" class="alert-content-text">
              {{ currentAlert.alertContent }}
            </div>
          </div>
        </div>

        <div class="alert-flow-section">
          <div class="section-title">状态流转</div>
          <div class="alert-flow-timeline">
            <div class="flow-item" :class="{ 'flow-done': flowDone('create') }">
              <div class="flow-dot">
                <CheckOutlined v-if="flowDone('create')" />
                <span v-else>1</span>
              </div>
              <div class="flow-content">
                <div class="flow-name">告警产生</div>
                <div class="flow-time">{{ currentAlert.createTime || '-' }}</div>
              </div>
            </div>
            <div class="flow-item" :class="{ 'flow-done': flowDone('sent') }">
              <div class="flow-dot">
                <CheckOutlined v-if="flowDone('sent')" />
                <span v-else>2</span>
              </div>
              <div class="flow-content">
                <div class="flow-name">通知发送 {{ currentAlert.sentChannels ? `(${currentAlert.sentChannels})` : '' }}</div>
                <div class="flow-time">{{ currentAlert.sentTime || '-' }}</div>
              </div>
            </div>
            <div class="flow-item" :class="{ 'flow-done': flowDone('read') }">
              <div class="flow-dot">
                <CheckOutlined v-if="flowDone('read')" />
                <span v-else>3</span>
              </div>
              <div class="flow-content">
                <div class="flow-name">已读</div>
                <div class="flow-time">{{ currentAlert.readTime || '-' }}</div>
              </div>
            </div>
            <div class="flow-item" :class="{ 'flow-done': flowDone('resolved') }">
              <div class="flow-dot">
                <CheckOutlined v-if="flowDone('resolved')" />
                <span v-else>4</span>
              </div>
              <div class="flow-content">
                <div class="flow-name">已解决</div>
                <div class="flow-time">{{ currentAlert.resolvedTime || '-' }}</div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="alertDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="currentAlert && currentAlert.status !== 'SENT' && currentAlert.status !== 'READ'"
            @click="handleMarkAsRead"
          >
            <template #icon><EyeOutlined /></template>
            标记已读
          </a-button>
          <a-button
            v-if="currentAlert && currentAlert.status !== 'RESOLVED'"
            type="primary"
            @click="handleResolveAlert"
          >
            <template #icon><CheckOutlined /></template>
            标记已解决
          </a-button>
        </a-space>
      </template>
    </a-drawer>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import {
  ReloadOutlined, EyeOutlined, RedoOutlined, StopOutlined,
  CheckOutlined, CloseCircleOutlined, ArrowRightOutlined,
  CaretUpOutlined, CaretDownOutlined,
  LineChartOutlined, PieChartOutlined, AlertOutlined, ClockCircleOutlined,
  HistoryOutlined, BellOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { monitorApi, alertRecordApi } from '@/api/monitor'
import type { MonitorDashboardVO, TaskExecutionSummary, AlertRecord } from '@/api/monitor'

// ============ 状态 ============
const trendDays = ref('7')
const trendChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const alertDistChartRef = ref<HTMLDivElement>()
const hourlyChartRef = ref<HTMLDivElement>()

const recentLoading = ref(false)
const recentAlertLoading = ref(false)
const cancelling = ref(false)
const retrying = ref(false)

const recentExecutions = ref<TaskExecutionSummary[]>([])
const recentAlerts = ref<AlertRecord[]>([])
const pendingAlertCount = ref(0)

const executionDrawerVisible = ref(false)
const currentExecution = ref<TaskExecutionSummary | null>(null)

const alertDrawerVisible = ref(false)
const currentAlert = ref<AlertRecord | null>(null)

// ============ 统计卡片 ============
const statCards = ref([
  {
    label: '今日执行', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #69C0FF 100%)',
    icon: 'PlayCircleOutlined', trend: 0, trendPeriod: '较昨日', trendUnit: '次'
  },
  {
    label: '今日成功', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)',
    icon: 'CheckCircleOutlined', trend: 0, trendPeriod: '较上周', trendUnit: '%'
  },
  {
    label: '今日失败', value: 0, bg: 'linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)',
    icon: 'CloseCircleOutlined', trend: 0, trendPeriod: '较昨日', trendUnit: '次'
  },
  {
    label: '运行中', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)',
    icon: 'LoadingOutlined', trend: 0, trendPeriod: '', trendUnit: ''
  }
])

const alertTagColor = ref('')
const alertTagText = ref('')

// ============ 表格列 ============
const recentColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 180, ellipsis: true },
  { title: '类型', key: 'taskType', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '耗时', key: 'elapsedMs', width: 90 },
  { title: '触发', key: 'triggerType', width: 80 },
  { title: '时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '操作', key: 'actions', width: 60, align: 'center' as const }
]

// ============ 数据加载 ============
async function loadOverview() {
  try {
    const res: any = await monitorApi.getOverview()
    // res 本身就是 { code, message, data, success } 格式
    const data = res?.data || {}
    statCards.value[0].value = data.todayExecutions ?? 0
    statCards.value[1].value = data.todaySuccess ?? 0
    statCards.value[2].value = data.todayFailed ?? 0
    statCards.value[3].value = data.runningCount ?? 0
    pendingAlertCount.value = data.pendingAlerts ?? 0
    recentExecutions.value = data.recentExecutions || []
    loadTrend()
    loadPieChart(data.taskTypeDistribution || {})
    loadAlertOverview()
  } catch {
    // ignore
  }
}

async function loadTrend() {
  await nextTick()
  if (!trendChartRef.value) return

  const chart = echarts.getInstanceByDom(trendChartRef.value)
  if (!chart) {
    echarts.init(trendChartRef.value).dispose()
  }
  const chartInstance = echarts.init(trendChartRef.value)

  // 模拟趋势数据
  const days = parseInt(trendDays.value)
  const dates = []
  const successData = []
  const failedData = []
  const totalData = []

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
    const total = Math.floor(Math.random() * 20) + 5
    const success = Math.floor(total * (0.8 + Math.random() * 0.18))
    totalData.push(total)
    successData.push(success)
    failedData.push(total - success)
  }

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['成功', '失败'],
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    grid: { left: 50, right: 20, top: 10, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#E8E8E8' } },
      axisLabel: { fontSize: 11, color: '#666' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { fontSize: 11, color: '#666' },
      splitLine: { lineStyle: { color: '#F5F7FA' } }
    },
    series: [
      {
        name: '成功',
        type: 'bar',
        stack: 'total',
        data: successData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#52C41A' },
            { offset: 1, color: '#73D13D' }
          ])
        },
        barMaxWidth: 32
      },
      {
        name: '失败',
        type: 'bar',
        stack: 'total',
        data: failedData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#FF4D4F' },
            { offset: 1, color: '#FF7875' }
          ])
        },
        barMaxWidth: 32
      }
    ]
  })
}

function loadPieChart(distribution: Record<string, number>) {
  if (!pieChartRef.value) return
  const chart = echarts.getInstanceByDom(pieChartRef.value)
  if (chart) chart.dispose()
  const chartInstance = echarts.init(pieChartRef.value)

  const data = Object.entries(distribution).map(([name, value]) => ({ name, value }))
  if (data.length === 0) {
    // 模拟数据
    data.push(
      { name: '数据质量', value: 45 },
      { name: '数据探查', value: 25 },
      { name: '数据同步', value: 20 },
      { name: '其他', value: 10 }
    )
  }

  chartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 11 } },
    color: ['#1677FF', '#722ED1', '#52C41A', '#FA8C16', '#FF4D4F', '#13C2C2'],
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['35%', '50%'],
      label: { show: false },
      data
    }]
  })
}

async function loadAlertOverview() {
  recentAlertLoading.value = true
  try {
    const [overviewRes, distRes, alertRes] = await Promise.all([
      alertRecordApi.getOverview(),
      alertRecordApi.getDistribution(),
      alertRecordApi.page({
        pageNum: 1, pageSize: 10,
        status: 'PENDING'
      })
    ])

    // 更新告警标签 - res.data 是 Result<AlertOverview>，直接取 data
    const overview: any = overviewRes?.data || {}
    if (overview) {
      pendingAlertCount.value = overview.pendingCount ?? 0
      if (overview.criticalCount && overview.criticalCount > 0) {
        alertTagColor.value = 'error'
        alertTagText.value = `${overview.criticalCount} 个严重告警`
      } else {
        alertTagColor.value = 'success'
        alertTagText.value = '今日已处理 ' + (overview.todayResolved ?? 0) + ' 个'
      }
    }

    const dist: any = distRes?.data || {}
    loadAlertDistChart(dist)
    loadHourlyChart()

    const records = alertRes?.data?.records || []
    recentAlerts.value = records
  } catch {
    recentAlerts.value = []
  } finally {
    recentAlertLoading.value = false
  }
}

function loadAlertDistChart(dist: Record<string, number>) {
  if (!alertDistChartRef.value) return
  const chart = echarts.getInstanceByDom(alertDistChartRef.value)
  if (chart) chart.dispose()
  const chartInstance = echarts.init(alertDistChartRef.value)

  const data = Object.entries(dist).map(([name, value]) => ({ name, value }))
  if (data.length === 0) {
    data.push(
      { name: '信息', value: 5 },
      { name: '警告', value: 8 },
      { name: '错误', value: 3 },
      { name: '严重', value: 1 }
    )
  }

  const colorMap: Record<string, string> = {
    '信息': '#1677FF', 'INFO': '#1677FF',
    '警告': '#FA8C16', 'WARN': '#FA8C16', 'WARNING': '#FA8C16',
    '错误': '#FF4D4F', 'ERROR': '#FF4D4F',
    '严重': '#722ED1', 'CRITICAL': '#722ED1'
  }

  chartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'horizontal', bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['38%', '65%'],
      center: ['50%', '45%'],
      label: { show: true, formatter: '{b}\n{c}', fontSize: 11 },
      data: data.map(d => ({
        ...d,
        itemStyle: { color: colorMap[d.name] || '#8C8C8C' }
      }))
    }]
  })
}

function loadHourlyChart() {
  if (!hourlyChartRef.value) return
  const chart = echarts.getInstanceByDom(hourlyChartRef.value)
  if (chart) chart.dispose()
  const chartInstance = echarts.init(hourlyChartRef.value)

  const hours = Array.from({ length: 24 }, (_, i) => `${i.toString().padStart(2, '0')}:00`)
  const hourlyData = hours.map(() => Math.floor(Math.random() * 8))

  chartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
    grid: { left: 45, right: 15, top: 10, bottom: 30 },
    xAxis: {
      type: 'category',
      data: hours,
      axisLabel: { fontSize: 10, color: '#666', interval: 3 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { fontSize: 10, color: '#666' },
      splitLine: { lineStyle: { color: '#F5F7FA' } }
    },
    series: [{
      type: 'line',
      data: hourlyData,
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(114, 46, 209, 0.3)' },
          { offset: 1, color: 'rgba(114, 46, 209, 0.02)' }
        ])
      },
      lineStyle: { color: '#722ED1', width: 2 },
      itemStyle: { color: '#722ED1' },
      symbol: 'circle',
      symbolSize: 4
    }]
  })
}

// ============ 执行详情 ============
function showExecutionDetail(record: TaskExecutionSummary) {
  currentExecution.value = record
  executionDrawerVisible.value = true
}

async function handleCancelExecution() {
  if (!currentExecution.value?.id) return
  cancelling.value = true
  try {
    await monitorApi.cancelExecution(currentExecution.value.id)
    currentExecution.value.status = 'FAILED'
    recentExecutions.value = recentExecutions.value.map(e =>
      e.id === currentExecution.value?.id ? { ...e, status: 'FAILED' } : e
    )
  } catch { /* ignore */ }
  finally { cancelling.value = false }
}

async function handleRetryExecution() {
  if (!currentExecution.value?.id) return
  retrying.value = true
  try {
    await monitorApi.retryExecution(currentExecution.value.id)
    currentExecution.value.status = 'RUNNING'
    recentExecutions.value = recentExecutions.value.map(e =>
      e.id === currentExecution.value?.id ? { ...e, status: 'RUNNING' } : e
    )
  } catch { /* ignore */ }
  finally { retrying.value = false }
}

// ============ 告警详情 ============
function showAlertDetail(alert: AlertRecord) {
  currentAlert.value = alert
  alertDrawerVisible.value = true
}

async function handleMarkAsRead() {
  if (!currentAlert.value?.id) return
  try {
    await alertRecordApi.markAsRead(currentAlert.value.id)
    currentAlert.value.status = 'READ'
    recentAlerts.value = recentAlerts.value.filter(a => a.id !== currentAlert.value?.id)
    if (pendingAlertCount.value > 0) pendingAlertCount.value--
  } catch { /* ignore */ }
}

async function handleResolveAlert() {
  if (!currentAlert.value?.id) return
  try {
    await alertRecordApi.resolve(currentAlert.value.id)
    currentAlert.value.status = 'RESOLVED'
    recentAlerts.value = recentAlerts.value.filter(a => a.id !== currentAlert.value?.id)
    if (pendingAlertCount.value > 0) pendingAlertCount.value--
  } catch { /* ignore */ }
}

function flowDone(step: string) {
  const status = currentAlert.value?.status
  if (!status) return false
  const order = { create: 0, sent: 1, read: 2, resolved: 3 }
  const currentOrder = { PENDING: 0, SENT: 1, READ: 2, RESOLVED: 3 }[status] ?? 0
  return (order[step as keyof typeof order] || 0) < currentOrder
}

// ============ 辅助函数 ============
function getStatusColor(status: string | undefined) {
  const map: Record<string, string> = {
    RUNNING: 'processing', SUCCESS: 'success', FAILED: 'error',
    SKIPPED: 'default', BLOCKED: 'error', CANCELLED: 'default'
  }
  return map[status || ''] || 'default'
}

function getStatusLabel(status: string | undefined) {
  const map: Record<string, string> = {
    RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败',
    SKIPPED: '已跳过', BLOCKED: '已阻塞', CANCELLED: '已取消'
  }
  return map[status || ''] || status || '-'
}

function getTriggerColor(trigger: string | undefined) {
  const map: Record<string, string> = { MANUAL: 'blue', SCHEDULE: 'purple', API: 'green' }
  return map[trigger || ''] || 'default'
}

function getTriggerLabel(trigger: string | undefined) {
  const map: Record<string, string> = { MANUAL: '手动', SCHEDULE: '定时', API: 'API' }
  return map[trigger || ''] || trigger || '-'
}

function getTaskTypeColor(type: string | undefined) {
  const map: Record<string, string> = {
    QUALITY: 'blue', PROFILE: 'purple', SYNC: 'green', OTHER: 'orange'
  }
  return map[type || ''] || 'default'
}

function getProgressColor(progress: number) {
  if (progress >= 80) return '#52C41A'
  if (progress >= 40) return '#FA8C16'
  return '#FF4D4F'
}

function formatDuration(ms: number | undefined) {
  if (!ms) return '-'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return (ms / 60000).toFixed(1) + 'min'
}

function formatTime(time: string | undefined) {
  if (!time) return '-'
  try {
    return new Date(time).toLocaleString('zh-CN')
  } catch { return time }
}

function getAlertLevelColor(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: 'blue', WARN: 'warning', WARNING: 'warning', ERROR: 'error', CRITICAL: 'error'
  }
  return map[level || ''] || 'default'
}

function getAlertLevelName(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: '信息', WARN: '警告', WARNING: '警告', ERROR: '错误', CRITICAL: '严重'
  }
  return map[level || ''] || level || '-'
}

function getAlertStatusColor(status: string | undefined) {
  const map: Record<string, string> = {
    PENDING: '#FA8C16', SENT: '#1677FF', READ: '#722ED1', RESOLVED: '#52C41A'
  }
  return map[status || ''] || '#8C8C8C'
}

function getAlertStatusName(status: string | undefined) {
  const map: Record<string, string> = {
    PENDING: '待处理', SENT: '已发送', READ: '已读', RESOLVED: '已解决'
  }
  return map[status || ''] || status || '-'
}

function getAlertStatusIcon(status: string | undefined) {
  const map: Record<string, string> = {
    PENDING: 'ClockCircleOutlined', SENT: 'BellOutlined', READ: 'EyeOutlined', RESOLVED: 'CheckCircleOutlined'
  }
  return map[status || ''] || 'BellOutlined'
}

onMounted(() => {
  loadOverview()
})
</script>

<style lang="less" scoped>
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;
  .header-left { display: flex; align-items: center; gap: 16px; }
  .header-icon { flex-shrink: 0; }
  .page-title { font-size: 22px; font-weight: 700; color: #1F1F1F; margin: 0; }
  .page-subtitle { font-size: 13px; color: #8C8C8C; margin: 4px 0 0; }
  .header-right { display: flex; gap: 12px; }
}

.stat-card {
  border-radius: 10px; padding: 20px;
  color: white; display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1); transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  .stat-icon-wrapper { opacity: 0.85; }
  .stat-content { flex: 1; }
  .stat-value { font-size: 28px; font-weight: 700; line-height: 1.2; }
  .stat-label { font-size: 13px; opacity: 0.85; margin-top: 4px; }
  .stat-trend {
    font-size: 12px; opacity: 0.8; margin-top: 4px;
    display: flex; align-items: center; gap: 2px;
    .trend-period { opacity: 0.7; margin-left: 4px; }
  }
}

.chart-card {
  background: white; border-radius: 10px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  .chart-card-header {
    display: flex; align-items: center; justify-content: space-between;
    margin-bottom: 16px;
  }
  .chart-title {
    font-size: 15px; font-weight: 600; color: #1F1F1F;
    display: flex; align-items: center; gap: 8px;
  }
}

.table-card {
  background: white; border-radius: 10px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  .table-card-header {
    display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
  }
  .table-title {
    font-size: 15px; font-weight: 600; color: #1F1F1F;
    display: flex; align-items: center; gap: 8px;
    flex: 1;
  }
}

.status-tag {
  display: inline-flex; align-items: center; gap: 6px;
  .status-dot {
    width: 6px; height: 6px; border-radius: 50%;
    &.dot-running { background: #1677FF; animation: pulse 1.5s infinite; }
    &.dot-success { background: #52C41A; }
    &.dot-failed { background: #FF4D4F; }
    &.dot-skipped { background: #999; }
    &.dot-blocked { background: #FF4D4F; }
  }
}

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

// 告警列表
.alert-empty {
  text-align: center; padding: 40px 20px;
}

.alert-list {
  display: flex; flex-direction: column; gap: 8px;
  max-height: 380px; overflow-y: auto;
}

.alert-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
  border: 1px solid transparent;
  &:hover { border-color: #E8E8E8; background: #FAFAFA; }

  &.alert-level-info, &.alert-level-warn, &.alert-level-warning {
    border-left: 3px solid #FA8C16;
    background: #FFFAF0;
    &:hover { background: #FFF7E6; }
  }
  &.alert-level-error {
    border-left: 3px solid #FF4D4F;
    background: #FFF1F0;
    &:hover { background: #FFE7E6; }
  }
  &.alert-level-critical {
    border-left: 3px solid #722ED1;
    background: #F9F0FF;
    &:hover { background: #F0E6FF; }
  }
}

.alert-left {
  flex-shrink: 0;
  .alert-level-badge {
    font-size: 11px; font-weight: 600; padding: 2px 8px;
    border-radius: 4px; white-space: nowrap;
    &.badge-info { background: #E6F7FF; color: #1677FF; }
    &.badge-warn, &.badge-warning { background: #FFFBE6; color: #FA8C16; }
    &.badge-error { background: #FFF1F0; color: #FF4D4F; }
    &.badge-critical { background: #F9F0FF; color: #722ED1; }
  }
}

.alert-body { flex: 1; min-width: 0; }
.alert-title {
  font-size: 13px; font-weight: 500; color: #1F1F1F;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.alert-meta {
  font-size: 12px; color: #8C8C8C; margin-top: 2px;
  .alert-sep { margin: 0 4px; }
}

.alert-right { font-size: 16px; flex-shrink: 0; }

// 执行详情
.execution-info-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;
  margin-bottom: 24px;
  .exec-info-card {
    background: #F5F7FA; border-radius: 8px; padding: 12px 16px;
    .exec-info-label { font-size: 12px; color: #8C8C8C; margin-bottom: 6px; }
    .exec-info-value { font-size: 14px; font-weight: 600; color: #1F1F1F; }
    .mono-value { font-family: 'JetBrains Mono', monospace; }
  }
}

.progress-section, .error-section {
  margin-bottom: 24px;
  .section-title {
    font-size: 14px; font-weight: 600; color: #1F1F1F; margin-bottom: 12px;
    display: flex; align-items: center; gap: 8px;
    &.text-danger { color: #FF4D4F; }
  }
}

.error-pre {
  background: #1F1F1F; color: #FF7875;
  padding: 16px; border-radius: 8px;
  font-family: 'JetBrains Mono', monospace; font-size: 13px;
  line-height: 1.6; white-space: pre-wrap; word-break: break-all; margin: 0;
  max-height: 300px; overflow-y: auto;
}

// 告警详情
.alert-content-section, .alert-flow-section {
  margin-bottom: 24px;
  .section-title {
    font-size: 14px; font-weight: 600; color: #1F1F1F; margin-bottom: 12px;
  }
}

.alert-content-box {
  background: #FFF7E6; border: 1px solid #FFE58F;
  border-radius: 8px; padding: 16px;
  .alert-title-text { font-size: 15px; font-weight: 600; color: #FA8C16; margin-bottom: 8px; }
  .alert-content-text { font-size: 13px; color: #333; line-height: 1.8; }
}

.alert-flow-timeline {
  display: flex; flex-direction: column; gap: 0;
  .flow-item {
    display: flex; align-items: flex-start; gap: 12px;
    padding: 10px 0; position: relative;
    &:not(:last-child) {
      border-left: 2px solid #E8E8E8;
      margin-left: 14px; padding-left: 20px;
    }
    &.flow-done .flow-dot {
      background: #52C41A; color: white;
      border-color: #52C41A;
    }
    .flow-dot {
      width: 30px; height: 30px; border-radius: 50%;
      background: white; border: 2px solid #E8E8E8;
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; font-weight: 600; color: #8C8C8C;
      flex-shrink: 0; position: absolute; left: -17px; background: white;
      z-index: 1;
    }
    .flow-content { flex: 1; }
    .flow-name { font-size: 13px; font-weight: 600; color: #1F1F1F; }
    .flow-time { font-size: 12px; color: #8C8C8C; margin-top: 2px; }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
