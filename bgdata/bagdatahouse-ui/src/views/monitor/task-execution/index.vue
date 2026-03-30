<template>
  <div class="page-container">

    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#taskMonGrad)"/>
            <path d="M5 8H19M5 12H19M5 16H13" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="19" cy="8" r="2" fill="#52C41A" stroke="white" stroke-width="1.5"/>
            <defs>
              <linearGradient id="taskMonGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#9254DE"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">任务执行监控</h1>
          <p class="page-subtitle">实时监控所有任务的执行状态、耗时统计与历史记录</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="6" v-for="stat in statCards" :key="stat.label">
        <div class="stat-mini-card" :style="{ background: stat.bg }">
          <div class="mini-value">{{ stat.value }}</div>
          <div class="mini-label">{{ stat.label }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="filterTaskName"
          placeholder="搜索任务名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterTaskType"
          placeholder="任务类型"
          style="width: 140px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部类型</a-select-option>
          <a-select-option value="QUALITY">数据质量</a-select-option>
          <a-select-option value="PROFILE">数据探查</a-select-option>
          <a-select-option value="SYNC">数据同步</a-select-option>
          <a-select-option value="OTHER">其他</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStatus"
          placeholder="执行状态"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="RUNNING">运行中</a-select-option>
          <a-select-option value="SUCCESS">成功</a-select-option>
          <a-select-option value="FAILED">失败</a-select-option>
          <a-select-option value="CANCELLED">已取消</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterTriggerType"
          placeholder="触发方式"
          style="width: 120px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="MANUAL">手动</a-select-option>
          <a-select-option value="SCHEDULE">定时</a-select-option>
          <a-select-option value="API">API</a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="dateRange"
          format="YYYY-MM-DD"
          style="width: 260px"
          @change="loadData"
        />
        <a-button @click="resetFilters">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </a-space>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record: any) => record.id"
        @change="handleTableChange"
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'executionNo'">
            <a-button type="link" size="small" @click="showDetail(record)" style="padding: 0; font-family: 'JetBrains Mono', monospace; font-size: 12px">
              {{ record.executionNo || record.id }}
            </a-button>
          </template>

          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)" class="status-tag">
              <span class="status-dot" :class="'dot-' + record.status?.toLowerCase()"></span>
              {{ getStatusLabel(record.status) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'progress'">
            <a-progress
              v-if="record.status === 'RUNNING'"
              :percent="record.progress || 0"
              :stroke-color="'#722ED1'"
              size="small"
              style="width: 100px"
            />
            <span v-else class="text-muted">-</span>
          </template>

          <template v-if="column.key === 'elapsedMs'">
            <span class="mono-text">{{ formatDuration(record.elapsedMs) }}</span>
          </template>

          <template v-if="column.key === 'triggerType'">
            <a-tag :color="getTriggerColor(record.triggerType)">
              {{ getTriggerLabel(record.triggerType) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'taskType'">
            <a-tag :color="getTaskTypeColor(record.taskType)">
              {{ record.taskTypeName || record.taskType }}
            </a-tag>
          </template>

          <template v-if="column.key === 'actions'">
            <a-space>
              <a-tooltip title="查看详情">
                <a-button type="text" size="small" @click="showDetail(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 'RUNNING'" title="取消执行">
                <a-popconfirm
                  title="确定取消该执行任务？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleCancel(record)"
                >
                  <a-button type="text" size="small" danger>
                    <template #icon><StopOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </a-tooltip>
              <a-tooltip v-if="record.status === 'FAILED'" title="重试">
                <a-button type="text" size="small" @click="handleRetry(record)">
                  <template #icon><RedoOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 'FAILED' && record.errorMsg" title="查看错误">
                <a-button type="text" size="small" @click="showErrorDetail(record)">
                  <template #icon><AlertOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="'执行详情'"
      :width="720"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="detailRecord">
        <div class="detail-info-grid">
          <div class="info-card">
            <div class="info-label">执行编号</div>
            <div class="info-value mono-value">{{ detailRecord.executionNo || detailRecord.id }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">执行状态</div>
            <div class="info-value">
              <a-tag :color="getStatusColor(detailRecord.status)">
                {{ getStatusLabel(detailRecord.status) }}
              </a-tag>
            </div>
          </div>
          <div class="info-card">
            <div class="info-label">任务名称</div>
            <div class="info-value">{{ detailRecord.taskName || '-' }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">任务类型</div>
            <div class="info-value">
              <a-tag :color="getTaskTypeColor(detailRecord.taskType)">
                {{ detailRecord.taskTypeName || detailRecord.taskType }}
              </a-tag>
            </div>
          </div>
          <div class="info-card">
            <div class="info-label">执行耗时</div>
            <div class="info-value mono-value">{{ formatDuration(detailRecord.elapsedMs) }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">触发方式</div>
            <div class="info-value">
              <a-tag :color="getTriggerColor(detailRecord.triggerType)">
                {{ getTriggerLabel(detailRecord.triggerType) }}
              </a-tag>
            </div>
          </div>
          <div class="info-card">
            <div class="info-label">开始时间</div>
            <div class="info-value">{{ detailRecord.startTime || '-' }}</div>
          </div>
          <div class="info-card">
            <div class="info-label">结束时间</div>
            <div class="info-value">{{ detailRecord.endTime || '-' }}</div>
          </div>
          <div class="info-card" v-if="detailRecord.triggerUserName">
            <div class="info-label">触发人</div>
            <div class="info-value">{{ detailRecord.triggerUserName }}</div>
          </div>
          <div class="info-card" v-if="detailRecord.dsName">
            <div class="info-label">数据源</div>
            <div class="info-value">{{ detailRecord.dsName }}</div>
          </div>
          <div class="info-card" v-if="detailRecord.planName">
            <div class="info-label">关联方案</div>
            <div class="info-value">{{ detailRecord.planName }}</div>
          </div>
        </div>

        <div v-if="detailRecord.progress !== undefined" class="progress-section">
          <div class="section-title">执行进度</div>
          <a-progress
            :percent="detailRecord.progress"
            :stroke-color="getProgressColor(detailRecord.progress)"
            status="active"
          />
        </div>

        <div v-if="detailRecord.errorMsg" class="error-section">
          <div class="section-title text-danger">
            <CloseCircleOutlined /> 错误详情
          </div>
          <pre class="error-pre">{{ detailRecord.errorMsg }}</pre>
        </div>

        <div v-if="detailRecord.logContent" class="log-section">
          <div class="section-title">
            <FileTextOutlined /> 执行日志
          </div>
          <pre class="log-pre">{{ detailRecord.logContent }}</pre>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="detailDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="detailRecord && detailRecord.status === 'RUNNING'"
            danger
            :loading="cancelling"
            @click="handleCancel(detailRecord)"
          >
            <template #icon><StopOutlined /></template>
            取消执行
          </a-button>
          <a-button
            v-if="detailRecord && detailRecord.status === 'FAILED'"
            type="primary"
            :loading="retrying"
            @click="handleRetry(detailRecord)"
          >
            <template #icon><RedoOutlined /></template>
            重新执行
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 错误详情弹窗 -->
    <a-modal
      v-model:open="errorModalVisible"
      title="错误详情"
      :width="700"
      :footer="null"
    >
      <pre class="error-pre-modal">{{ errorContent }}</pre>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ReloadOutlined, SearchOutlined, EyeOutlined, StopOutlined,
  RedoOutlined, AlertOutlined, CloseCircleOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import { monitorApi } from '@/api/monitor'
import type { TaskExecution } from '@/api/monitor'

// ============ 状态 ============
const loading = ref(false)
const tableData = ref<TaskExecution[]>([])
const cancelling = ref(false)
const retrying = ref(false)

const filterTaskName = ref('')
const filterTaskType = ref<string>()
const filterStatus = ref<string>()
const filterTriggerType = ref<string>()
const dateRange = ref<any[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const statCards = ref([
  { label: '今日执行', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #69C0FF 100%)' },
  { label: '成功', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '失败', value: 0, bg: 'linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)' },
  { label: '运行中', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' }
])

// ============ 表格列 ============
const columns = [
  { title: '执行编号', key: 'executionNo', width: 200, fixed: 'left' },
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 180, ellipsis: true },
  { title: '任务类型', key: 'taskType', width: 110 },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progress', width: 110 },
  { title: '耗时', key: 'elapsedMs', width: 90 },
  { title: '触发方式', key: 'triggerType', width: 90 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 170 },
  { title: '操作', key: 'actions', width: 160, fixed: 'right' }
]

// ============ 详情 & 错误 ============
const detailDrawerVisible = ref(false)
const detailRecord = ref<TaskExecution | null>(null)
const errorModalVisible = ref(false)
const errorContent = ref('')

// ============ 数据加载 ============
async function loadData() {
  loading.value = true
  try {
    const res: any = await monitorApi.pageExecutions({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      taskId: undefined,
      taskType: filterTaskType.value || undefined,
      status: filterStatus.value || undefined,
      triggerType: filterTriggerType.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD')
    })
    tableData.value = res.data?.records || res.data || []
    pagination.total = res.data?.total || tableData.value.length
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 统计卡片数据：使用接口直接获取今日概览统计，而非仅基于当前页分页数据
async function loadStats() {
  try {
    const res = await monitorApi.getOverview()
    const data: any = res.data?.data || {}
    statCards.value[0].value = data.todayExecutions ?? 0
    statCards.value[1].value = data.todaySuccess ?? 0
    statCards.value[2].value = data.todayFailed ?? 0
    statCards.value[3].value = data.runningCount ?? 0
  } catch { /* ignore */ }
}

async function loadRunningTasks() {
  try {
    const res: any = await monitorApi.getRunningExecutions()
    const running = Array.isArray(res.data) ? res.data : []
    // 同步更新运行中卡片
    statCards.value[3].value = running.length
  } catch { /* ignore */ }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilters() {
  filterTaskName.value = ''
  filterTaskType.value = undefined
  filterStatus.value = undefined
  filterTriggerType.value = undefined
  dateRange.value = []
  pagination.current = 1
  loadData()
}

// ============ 操作 ============
function showDetail(record: TaskExecution) {
  detailRecord.value = record
  detailDrawerVisible.value = true
}

function showErrorDetail(record: TaskExecution) {
  errorContent.value = record.errorMsg || '无错误详情'
  errorModalVisible.value = true
}

async function handleCancel(record: TaskExecution) {
  if (!record.id) return
  cancelling.value = true
  try {
    await monitorApi.cancelExecution(record.id)
    record.status = 'CANCELLED'
    if (detailRecord.value?.id === record.id) detailRecord.value.status = 'CANCELLED'
    loadData()
  } catch { /* ignore */ }
  finally { cancelling.value = false }
}

async function handleRetry(record: TaskExecution) {
  if (!record.id) return
  retrying.value = true
  try {
    await monitorApi.retryExecution(record.id)
    record.status = 'RUNNING'
    if (detailRecord.value?.id === record.id) detailRecord.value.status = 'RUNNING'
    loadData()
  } catch { /* ignore */ }
  finally { retrying.value = false }
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

onMounted(() => {
  loadData()
  loadStats()
  loadRunningTasks()
  // 每30秒刷新一次运行中任务
  setInterval(() => loadRunningTasks(), 30000)
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

.stat-mini-card {
  border-radius: 10px; padding: 16px 20px;
  color: white; box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  .mini-value { font-size: 26px; font-weight: 700; }
  .mini-label { font-size: 13px; opacity: 0.85; margin-top: 4px; }
}

.filter-bar {
  background: white; border-radius: 10px;
  padding: 16px 20px; margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.table-card {
  background: white; border-radius: 10px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.status-tag {
  display: inline-flex; align-items: center; gap: 6px;
  .status-dot {
    width: 6px; height: 6px; border-radius: 50%;
    &.dot-running { background: #1677FF; animation: pulse 1.5s infinite; }
    &.dot-success { background: #52C41A; }
    &.dot-failed { background: #FF4D4F; }
    &.dot-cancelled { background: #999; }
  }
}

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.detail-info-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;
  margin-bottom: 24px;
  .info-card {
    background: #F5F7FA; border-radius: 8px; padding: 12px 16px;
    .info-label { font-size: 12px; color: #8C8C8C; margin-bottom: 6px; }
    .info-value { font-size: 14px; font-weight: 600; color: #1F1F1F; }
    .mono-value { font-family: 'JetBrains Mono', monospace; }
  }
}

.progress-section, .error-section, .log-section {
  margin-bottom: 24px;
  .section-title {
    font-size: 14px; font-weight: 600; color: #1F1F1F; margin-bottom: 12px;
    display: flex; align-items: center; gap: 8px;
    &.text-danger { color: #FF4D4F; }
  }
}

.error-pre, .log-pre {
  background: #1F1F1F; color: #A6E22E;
  padding: 16px; border-radius: 8px;
  font-family: 'JetBrains Mono', monospace; font-size: 13px;
  line-height: 1.6; white-space: pre-wrap; word-break: break-all; margin: 0;
  max-height: 400px; overflow-y: auto;
}

.error-pre-modal {
  background: #FFF1F0; color: #FF4D4F;
  border: 1px solid #FFCCC7;
  padding: 16px; border-radius: 8px;
  font-family: 'JetBrains Mono', monospace; font-size: 13px;
  line-height: 1.6; white-space: pre-wrap; word-break: break-all; margin: 0;
  max-height: 500px; overflow-y: auto;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
