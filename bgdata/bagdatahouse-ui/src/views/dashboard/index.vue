<template>
  <div class="dashboard page-container">
    <!-- 顶部统计卡片 -->
    <a-row :gutter="[20, 20]" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)">
          <div class="stat-icon"><DatabaseOutlined /></div>
          <div class="stat-value">{{ statData.dsCount }}</div>
          <div class="stat-label">数据源总数</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-icon"><SafetyCertificateOutlined /></div>
          <div class="stat-value">{{ statData.ruleCount }}</div>
          <div class="stat-label">质量规则数</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)">
          <div class="stat-icon"><PlayCircleOutlined /></div>
          <div class="stat-value">{{ statData.todayExecutions }}</div>
          <div class="stat-label">今日执行数</div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)">
          <div class="stat-icon"><AlertOutlined /></div>
          <div class="stat-value">{{ statData.pendingAlerts }}</div>
          <div class="stat-label">告警待处理</div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="[20, 20]" style="margin-top: 20px">
      <a-col :xs="24" :lg="16">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">质量评分趋势</span>
            <a-space>
              <a-select v-model:value="trendDays" style="width: 120px" size="small">
                <a-select-option value="7">近7天</a-select-option>
                <a-select-option value="30">近30天</a-select-option>
                <a-select-option value="90">近90天</a-select-option>
              </a-select>
            </a-space>
          </div>
          <div ref="trendChartRef" style="height: 280px"></div>
        </div>
      </a-col>
      <a-col :xs="24" :lg="8">
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">规则类型分布</span>
          </div>
          <div ref="pieChartRef" style="height: 280px"></div>
        </div>
      </a-col>
    </a-row>

    <!-- 下方列表 -->
    <a-row :gutter="[20, 20]" style="margin-top: 20px">
      <a-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="card-header">
            <span class="card-title">最近执行记录</span>
            <RouterLink to="/dqc/execution" class="more-link">查看更多 →</RouterLink>
          </div>
          <a-table
            :columns="executionColumns"
            :data-source="executionData"
            :pagination="false"
            size="small"
            :row-key="(record: any) => record.id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">{{ record.statusText }}</a-tag>
              </template>
              <template v-if="column.key === 'score'">
                <a-progress
                  :percent="record.qualityScore"
                  :stroke-color="getScoreColor(record.qualityScore)"
                  size="small"
                />
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
      <a-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="card-header">
            <span class="card-title">待处理告警</span>
            <RouterLink to="/monitor/alert-record" class="more-link">查看更多 →</RouterLink>
          </div>
          <div class="alert-list">
            <div v-for="alert in alertData" :key="alert.id" class="alert-item">
              <div class="alert-left">
                <a-badge :status="alert.level" :text="alert.title" />
              </div>
              <div class="alert-right">
                <span class="alert-time">{{ alert.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { message } from 'ant-design-vue'
import { DatabaseOutlined, SafetyCertificateOutlined, PlayCircleOutlined, AlertOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { monitorApi } from '@/api/monitor'
import { dataSourceApi, ruleDefApi } from '@/api/dqc'
import { alertRecordApi } from '@/api/monitor'

// 统计数据
const statData = reactive({
  dsCount: 0,
  ruleCount: 0,
  todayExecutions: 0,
  pendingAlerts: 0
})

// 图表数据
const trendDays = ref('7')
const trendChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const loading = ref(false)

const executionColumns = [
  { title: '方案名称', dataIndex: 'planName', key: 'planName' },
  { title: '状态', key: 'status', width: 100 },
  { title: '质量得分', key: 'score', width: 160 },
  { title: '执行时间', dataIndex: 'executeTime', key: 'executeTime' }
]

const executionData = ref([
  { id: 1, planName: 'ODS层完整性监控', status: 'SUCCESS', statusText: '成功', qualityScore: 95, executeTime: '2024-03-21 10:30' },
  { id: 2, planName: 'DWD层一致性校验', status: 'FAILED', statusText: '失败', qualityScore: 62, executeTime: '2024-03-21 09:15' },
  { id: 3, planName: 'ADS层准确性检查', status: 'SUCCESS', statusText: '成功', qualityScore: 88, executeTime: '2024-03-21 08:00' },
  { id: 4, planName: '客户域唯一性校验', status: 'SUCCESS', statusText: '成功', qualityScore: 100, executeTime: '2024-03-20 22:00' },
  { id: 5, planName: '交易域完整性监控', status: 'RUNNING', statusText: '运行中', qualityScore: 0, executeTime: '2024-03-21 11:00' }
])

const alertData = ref([
  { id: 1, title: 'DWD层一致性校验失败', level: 'error' as const, time: '10分钟前' },
  { id: 2, title: '表行数波动超阈值', level: 'warning' as const, time: '1小时前' },
  { id: 3, title: '执行超时告警', level: 'warning' as const, time: '2小时前' }
])

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    SUCCESS: 'success',
    FAILED: 'error',
    RUNNING: 'processing',
    SKIPPED: 'default'
  }
  return map[status] || 'default'
}

function getScoreColor(score: number) {
  if (score >= 90) return '#52C41A'
  if (score >= 70) return '#FAAD14'
  return '#FF4D4F'
}

async function loadStats() {
  try {
    // 并行加载多个统计数据
    const [dsRes, ruleRes, overviewRes] = await Promise.allSettled([
      dataSourceApi.listEnabled(),
      ruleDefApi.page({ pageNum: 1, pageSize: 1 }),
      monitorApi.getOverview()
    ])

    // 数据源数量
    if (dsRes.status === 'fulfilled' && dsRes.value.data?.success !== false) {
      statData.dsCount = dsRes.value.data?.data?.length || 0
    }

    // 规则总数
    if (ruleRes.status === 'fulfilled' && ruleRes.value.data?.success !== false) {
      const page = ruleRes.value.data?.data as any
      statData.ruleCount = page?.total || 0
    }

    // 监控概览
    if (overviewRes.status === 'fulfilled' && overviewRes.value.data?.success !== false) {
      const data = overviewRes.value.data?.data as any
      if (data) {
        statData.todayExecutions = data.todayExecutions || 0
        statData.pendingAlerts = data.pendingAlerts || 0
      }
    }
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

async function loadTrendData() {
  try {
    const days = parseInt(trendDays.value)
    const res = await monitorApi.getExecutionTrend(days)
    if (res.data?.success !== false) {
      const trend = res.data?.data?.successRateTrend || []
      initTrendChart(trend)
    }
  } catch (e) {
    console.error('加载趋势数据失败:', e)
    // 加载失败时使用空数据初始化
    initTrendChart([])
  }
}

async function loadPieData() {
  try {
    const res = await monitorApi.getOverview()
    if (res.data?.success !== false) {
      const data = res.data?.data?.taskTypeDistribution || {}
      const colors = ['#1677FF', '#52C41A', '#FAAD14', '#FF4D4F', '#722ED1', '#00B4DB']
      const distribution = Object.entries(data).map(([name, value], i) => ({
        name,
        value,
        itemStyle: { color: colors[i % colors.length] }
      }))
      initPieChart(distribution)
    }
  } catch (e) {
    console.error('加载分布数据失败:', e)
    initPieChart([])
  }
}

async function loadRecentExecutions() {
  try {
    const res = await monitorApi.pageExecutions({ pageNum: 1, pageSize: 5 })
    if (res.data?.success !== false) {
      const page = res.data?.data as any
      executionData.value = (page?.records || []).map((r: any) => ({
        id: r.id,
        planName: r.planName || r.taskName || '未知方案',
        status: r.status,
        statusText: getStatusText(r.status),
        qualityScore: Math.round(r.qualityScore || 0),
        executeTime: r.startTime || r.createTime || '-'
      }))
    }
  } catch (e) {
    console.error('加载执行记录失败:', e)
  }
}

async function loadAlerts() {
  try {
    const res = await alertRecordApi.page({ pageNum: 1, pageSize: 5, status: 'PENDING' })
    if (res.data?.success !== false) {
      const page = res.data?.data as any
      alertData.value = (page?.records || []).map((r: any, i: number) => ({
        id: r.id,
        title: r.alertName || r.title || '告警消息',
        level: r.level === 'CRITICAL' || r.level === 'ERROR' ? 'error' : 'warning',
        time: formatTime(r.createTime)
      }))
    }
  } catch (e) {
    console.error('加载告警失败:', e)
  }
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    SUCCESS: '成功', FAILED: '失败', RUNNING: '运行中', SKIPPED: '跳过', PENDING: '待处理'
  }
  return map[status] || status || '未知'
}

function formatTime(time: string) {
  if (!time) return '-'
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
}

function initTrendChart(trend: any[]) {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  const days = trend.map((t: any) => t.date?.slice(5) || '')
  const scores = trend.map((t: any) => t.successRate || 0)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: days, boundaryGap: false },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      data: scores,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(22, 119, 255, 0.3)' },
          { offset: 1, color: 'rgba(22, 119, 255, 0.02)' }
        ])
      },
      lineStyle: { color: '#1677FF', width: 3 },
      itemStyle: { color: '#1677FF' }
    }]
  })
}

function initPieChart(distribution: any[]) {
  if (!pieChartRef.value) return
  const chart = echarts.init(pieChartRef.value)
  if (distribution.length === 0) {
    distribution = [
      { value: 1, name: '暂无数据', itemStyle: { color: '#E8E8E8' } }
    ]
  }
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: distribution
    }]
  })
}

onMounted(async () => {
  loading.value = true
  try {
    // 加载统计数据
    await loadStats()
    // 并行加载图表和列表数据
    await Promise.all([
      loadTrendData(),
      loadPieData(),
      loadRecentExecutions(),
      loadAlerts()
    ])
  } finally {
    loading.value = false
  }
})
</script>

<style lang="less" scoped>
.dashboard {
  background: #F5F7FA;
}

.stat-row {
  .stat-card {
    border-radius: 12px;
    padding: 24px;
    color: #fff;
    position: relative;
    overflow: hidden;
    transition: transform 0.3s, box-shadow 0.3s;
    cursor: pointer;
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
    }
    .stat-icon {
      font-size: 40px;
      opacity: 0.25;
      position: absolute;
      right: 16px;
      top: 16px;
    }
    .stat-value {
      font-size: 36px;
      font-weight: 700;
      line-height: 1.2;
    }
    .stat-label {
      font-size: 14px;
      opacity: 0.9;
      margin-top: 6px;
    }
    .stat-change {
      font-size: 12px;
      opacity: 0.75;
      margin-top: 8px;
    }
  }
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: #1F1F1F;
    }
  }
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #1F1F1F;
    }
    .more-link {
      font-size: 13px;
      color: #1677FF;
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.alert-list {
  .alert-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
    &:last-child {
      border-bottom: none;
    }
    .alert-left {
      font-size: 14px;
    }
    .alert-right {
      font-size: 12px;
      color: #8C8C8C;
    }
  }
}
</style>
