<template>
  <div class="page-container">

    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#alertRecGrad)"/>
            <path d="M12 4L4 20H20L12 4Z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M12 9V13" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="16" r="1" fill="white"/>
            <defs>
              <linearGradient id="alertRecGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FF4D4F"/>
                <stop offset="100%" stop-color="#FF7875"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">告警记录</h1>
          <p class="page-subtitle">查看所有告警通知，处理告警状态流转：待发送 → 已发送 → 已读 → 已解决</p>
        </div>
      </div>
      <div class="header-right">
        <a-button
          v-if="hasSelected"
          @click="handleBatchMarkRead"
        >
          <template #icon><EyeOutlined /></template>
          批量已读 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button
          v-if="hasSelected"
          type="primary"
          @click="handleBatchResolve"
        >
          <template #icon><CheckOutlined /></template>
          批量处理 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 概览统计 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="6">
        <div class="overview-card total-card">
          <div class="overview-icon"><AlertOutlined /></div>
          <div class="overview-content">
            <div class="overview-value">{{ overviewStats.total }}</div>
            <div class="overview-label">告警总数</div>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="overview-card pending-card">
          <div class="overview-icon"><ClockCircleOutlined /></div>
          <div class="overview-content">
            <div class="overview-value">{{ overviewStats.pending }}</div>
            <div class="overview-label">待处理</div>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="overview-card critical-card">
          <div class="overview-icon"><WarningOutlined /></div>
          <div class="overview-content">
            <div class="overview-value">{{ overviewStats.critical }}</div>
            <div class="overview-label">严重告警</div>
          </div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="overview-card resolved-card">
          <div class="overview-icon"><CheckCircleOutlined /></div>
          <div class="overview-content">
            <div class="overview-value">{{ overviewStats.resolved }}</div>
            <div class="overview-label">已解决</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="filterRuleName"
          placeholder="搜索规则名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterAlertLevel"
          placeholder="告警级别"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="INFO">信息</a-select-option>
          <a-select-option value="WARN">警告</a-select-option>
          <a-select-option value="ERROR">错误</a-select-option>
          <a-select-option value="CRITICAL">严重</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStatus"
          placeholder="处理状态"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="PENDING">待处理</a-select-option>
          <a-select-option value="SENT">已发送</a-select-option>
          <a-select-option value="READ">已读</a-select-option>
          <a-select-option value="RESOLVED">已解决</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterRuleType"
          placeholder="告警类型"
          style="width: 180px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="SENSITIVE_FIELD_SPIKE">字段突增</a-select-option>
          <a-select-option value="SENSITIVE_LEVEL_CHANGE">等级变更</a-select-option>
          <a-select-option value="SENSITIVE_ACCESS_ANOMALY">访问异常</a-select-option>
          <a-select-option value="SENSITIVE_UNREVIEWED_LONG">待审核超期</a-select-option>
          <a-select-option value="QUALITY">质量告警</a-select-option>
          <a-select-option value="AVAILABILITY">可用性告警</a-select-option>
          <a-select-option value="PERFORMANCE">性能告警</a-select-option>
          <a-select-option value="SYSTEM">系统告警</a-select-option>
          <a-select-option value="FLUCTUATION">波动告警</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterSensitivityLevel"
          placeholder="敏感等级"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="L4">L4 机密</a-select-option>
          <a-select-option value="L3">L3 敏感</a-select-option>
          <a-select-option value="L2">L2 内部</a-select-option>
          <a-select-option value="L1">L1 公开</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterTargetType"
          placeholder="告警对象"
          style="width: 130px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="TASK">任务</a-select-option>
          <a-select-option value="METRIC">指标</a-select-option>
          <a-select-option value="SYSTEM">系统</a-select-option>
          <a-select-option value="QUALITY">质量</a-select-option>
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
        :row-selection="rowSelection"
        :row-key="(record: any) => record.id"
        @change="handleTableChange"
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record }">
          <!-- 告警编号 -->
          <template v-if="column.key === 'alertNo'">
            <a-button type="link" size="small" @click="showDetail(record)" style="padding: 0; font-family: 'JetBrains Mono', monospace; font-size: 12px">
              {{ record.alertNo }}
            </a-button>
          </template>

          <!-- 告警级别 -->
          <template v-if="column.key === 'alertLevel'">
            <div class="alert-level-cell">
              <a-tag :color="getAlertLevelColor(record.alertLevel)">
                {{ getAlertLevelLabel(record.alertLevel) }}
              </a-tag>
              <a-tag v-if="record.sensitivityLevel" size="small"
                :color="getSensitivityLevelTagColor(record.sensitivityLevel)"
                style="margin-left: 4px">
                {{ record.sensitivityLevel }}
              </a-tag>
            </div>
          </template>

          <!-- 状态 -->
          <template v-if="column.key === 'status'">
            <div class="status-cell">
              <span class="status-indicator" :class="'status-' + (record.status || 'pending').toLowerCase()"></span>
              <span class="status-label">{{ getStatusLabel(record.status) }}</span>
            </div>
          </template>

          <!-- 告警对象 -->
          <template v-if="column.key === 'target'">
            <div v-if="record.targetName || record.sensitivityTable" class="target-cell">
              <template v-if="record.sensitivityLevel">
                <a-tag size="small" :color="getSensitivityLevelTagColor(record.sensitivityLevel)">
                  {{ record.sensitivityLevel }}
                </a-tag>
                <span class="sensitive-field-text" :title="record.sensitivityTable + '.' + record.sensitivityColumn">
                  {{ record.sensitivityTable }}{{ record.sensitivityColumn ? '.' + record.sensitivityColumn : '' }}
                </span>
              </template>
              <template v-else>
                <a-tag size="small" :color="getTargetTypeColor(record.targetType)">
                  {{ record.targetType }}
                </a-tag>
                <span class="target-name">{{ record.targetName }}</span>
              </template>
            </div>
            <span v-else class="text-muted">-</span>
          </template>

          <!-- 触发值 vs 阈值 -->
          <template v-if="column.key === 'triggerValue'">
            <div class="trigger-cell" v-if="record.triggerValue !== undefined && record.triggerValue !== null">
              <span class="trigger-value">{{ record.triggerValue }}</span>
              <span v-if="record.thresholdValue !== undefined && record.thresholdValue !== null" class="threshold-info">
                / {{ record.thresholdValue }}
              </span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>

          <!-- 触发时间 -->
          <template v-if="column.key === 'createTime'">
            <span class="time-text">{{ formatTime(record.createTime) }}</span>
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-tooltip title="查看详情">
                <a-button type="text" size="small" @click="showDetail(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm
                v-if="record.status !== 'READ' && record.status !== 'RESOLVED'"
                title="确认标记为已读？"
                @confirm="handleMarkRead(record)"
              >
                <a-tooltip title="标记已读">
                  <a-button type="text" size="small">
                    <template #icon><CheckOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
              <a-popconfirm
                v-if="record.status !== 'RESOLVED'"
                title="确认标记为已解决？"
                @confirm="handleResolve(record)"
              >
                <a-tooltip title="标记已解决">
                  <a-button type="text" size="small" style="color: #52C41A">
                    <template #icon><CheckCircleOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="'告警详情 — ' + (currentRecord?.alertNo || '')"
      :width="720"
      placement="right"
      :destroy-on-close="true"
    >
      <template v-if="currentRecord">
        <!-- 概览信息 -->
        <div class="detail-overview" :class="'level-' + (currentRecord.alertLevel || 'warn').toLowerCase()">
          <div class="detail-level-badge" :class="'badge-' + (currentRecord.alertLevel || 'warn').toLowerCase()">
            {{ getAlertLevelLabel(currentRecord.alertLevel) }}
          </div>
          <div class="detail-title">{{ currentRecord.alertTitle || '告警通知' }}</div>
          <div class="detail-status-bar">
            <span class="detail-status-indicator" :class="'status-' + (currentRecord.status || 'pending').toLowerCase()"></span>
            {{ getStatusLabel(currentRecord.status) }}
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">
            <InfoCircleOutlined /> 基本信息
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <div class="detail-label">告警编号</div>
              <div class="detail-value mono-value">{{ currentRecord.alertNo }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">告警规则</div>
              <div class="detail-value">{{ currentRecord.ruleName || '-' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">告警级别</div>
              <div class="detail-value">
                <a-tag :color="getAlertLevelColor(currentRecord.alertLevel)">
                  {{ getAlertLevelLabel(currentRecord.alertLevel) }}
                </a-tag>
                <a-tag v-if="currentRecord.sensitivityLevel" size="small"
                  :color="getSensitivityLevelTagColor(currentRecord.sensitivityLevel)"
                  style="margin-left: 4px">
                  {{ currentRecord.sensitivityLevel }}
                </a-tag>
              </div>
            </div>
            <div class="detail-item" v-if="currentRecord.sensitivityLevel">
              <div class="detail-label">敏感字段</div>
              <div class="detail-value">
                <span class="mono-value">
                  {{ currentRecord.sensitivityTable || '-' }}{{ currentRecord.sensitivityColumn ? '.' + currentRecord.sensitivityColumn : '' }}
                </span>
              </div>
            </div>
            <div class="detail-item" v-if="currentRecord.scanBatchNo">
              <div class="detail-label">扫描批次号</div>
              <div class="detail-value mono-value">{{ currentRecord.scanBatchNo }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">告警对象</div>
              <div class="detail-value">
                <template v-if="currentRecord.sensitivityLevel">
                  <span>{{ currentRecord.sensitivityTable || '-' }}{{ currentRecord.sensitivityColumn ? '.' + currentRecord.sensitivityColumn : '' }}</span>
                </template>
                <template v-else>
                  <a-tag v-if="currentRecord.targetType" size="small" :color="getTargetTypeColor(currentRecord.targetType)">
                    {{ currentRecord.targetType }}
                  </a-tag>
                  {{ currentRecord.targetName || '-' }}
                </template>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-label">触发值</div>
              <div class="detail-value mono-value">{{ currentRecord.triggerValue ?? '-' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">阈值</div>
              <div class="detail-value mono-value">{{ currentRecord.thresholdValue ?? '-' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">触发时间</div>
              <div class="detail-value">{{ currentRecord.createTime ? formatTime(currentRecord.createTime) : '-' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">通知渠道</div>
              <div class="detail-value">{{ currentRecord.sentChannels || '-' }}</div>
            </div>
          </div>
        </div>

        <!-- 告警内容 -->
        <div class="detail-section">
          <div class="section-title">
            <BellOutlined /> 告警内容
          </div>
          <div class="alert-content-box" v-if="currentRecord.alertContent">
            <pre class="content-pre">{{ currentRecord.alertContent }}</pre>
          </div>
          <div v-else class="text-muted" style="padding: 12px 0">无详细告警内容</div>
        </div>

        <!-- 状态流转 -->
        <div class="detail-section">
          <div class="section-title">
            <HistoryOutlined /> 状态流转
          </div>
          <div class="status-timeline">
            <div class="timeline-item" :class="{ 'done': isTimelineDone('create') }">
              <div class="timeline-dot done"></div>
              <div class="timeline-content">
                <div class="timeline-title">告警产生</div>
                <div class="timeline-time">{{ currentRecord.createTime ? formatTime(currentRecord.createTime) : '-' }}</div>
              </div>
            </div>
            <div class="timeline-item" :class="{ 'done': isTimelineDone('sent') }">
              <div class="timeline-dot" :class="{ done: isTimelineDone('sent') }"></div>
              <div class="timeline-content">
                <div class="timeline-title">通知发送</div>
                <div class="timeline-time">{{ currentRecord.sentTime ? formatTime(currentRecord.sentTime) : '-' }}</div>
              </div>
            </div>
            <div class="timeline-item" :class="{ 'done': isTimelineDone('read') }">
              <div class="timeline-dot" :class="{ done: isTimelineDone('read') }"></div>
              <div class="timeline-content">
                <div class="timeline-title">已读</div>
                <div class="timeline-time">{{ currentRecord.readTime ? formatTime(currentRecord.readTime) : '-' }}</div>
              </div>
            </div>
            <div class="timeline-item" :class="{ 'done': isTimelineDone('resolved') }">
              <div class="timeline-dot" :class="{ done: isTimelineDone('resolved') }"></div>
              <div class="timeline-content">
                <div class="timeline-title">已解决</div>
                <div class="timeline-time">{{ currentRecord.resolvedTime ? formatTime(currentRecord.resolvedTime) : '-' }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 解决备注 -->
        <div class="detail-section" v-if="currentRecord.resolveComment">
          <div class="section-title">
            <CheckCircleOutlined /> 解决备注
          </div>
          <div class="resolve-comment">
            <div class="resolve-meta">
              <span>解决人：{{ currentRecord.resolveUser || '-' }}</span>
              <span>{{ currentRecord.resolvedTime ? formatTime(currentRecord.resolvedTime) : '-' }}</span>
            </div>
            <div class="resolve-content">{{ currentRecord.resolveComment }}</div>
          </div>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="detailDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="currentRecord && currentRecord.status !== 'READ' && currentRecord.status !== 'RESOLVED'"
            @click="handleMarkRead(currentRecord)"
          >
            <template #icon><CheckOutlined /></template>
            标记已读
          </a-button>
          <a-button
            v-if="currentRecord && currentRecord.status !== 'RESOLVED'"
            type="primary"
            @click="showResolveModal"
          >
            <template #icon><CheckCircleOutlined /></template>
            标记已解决
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 解决弹窗 -->
    <a-modal
      v-model:open="resolveModalVisible"
      title="处理告警"
      :width="480"
      @ok="handleDoResolve"
    >
      <div class="resolve-form">
        <div class="resolve-tip">
          <AlertOutlined style="color: #FA8C16; margin-right: 8px" />
          请填写处理备注，说明该告警的处理情况
        </div>
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
          <a-form-item label="处理备注" name="resolveComment">
            <a-textarea
              v-model:value="resolveComment"
              placeholder="请输入处理备注，说明告警原因和处理方式"
              :rows="4"
            />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  AlertOutlined, WarningOutlined, ClockCircleOutlined, CheckCircleOutlined,
  EyeOutlined, CheckOutlined, ReloadOutlined, SearchOutlined,
  InfoCircleOutlined, BellOutlined, HistoryOutlined
} from '@ant-design/icons-vue'
import { alertRecordApi } from '@/api/monitor'
import type { AlertRecord } from '@/api/monitor'

// ============ 状态 ============
const loading = ref(false)
const tableData = ref<AlertRecord[]>([])
const selectedRowKeys = ref<number[]>([])

const filterRuleName = ref('')
const filterAlertLevel = ref<string>()
const filterStatus = ref<string>()
const filterTargetType = ref<string>()
const filterRuleType = ref<string>()
const filterSensitivityLevel = ref<string>()
const dateRange = ref<any[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const overviewStats = reactive({
  total: 0,
  pending: 0,
  critical: 0,
  resolved: 0
})

const hasSelected = computed(() => selectedRowKeys.value.length > 0)

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

// ============ 表格列 ============
const columns = [
  { title: '告警编号', key: 'alertNo', width: 180, fixed: 'left' },
  { title: '告警级别', key: 'alertLevel', width: 90 },
  { title: '告警标题', dataIndex: 'alertTitle', key: 'alertTitle', width: 220, ellipsis: true },
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '告警对象', key: 'target', width: 160 },
  { title: '触发值/阈值', key: 'triggerValue', width: 130 },
  { title: '触发时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 120, fixed: 'right' }
]

// ============ 详情 & 处理 ============
const detailDrawerVisible = ref(false)
const currentRecord = ref<AlertRecord | null>(null)
const resolveModalVisible = ref(false)
const resolveComment = ref('')

// ============ 数据加载 ============
async function loadData() {
  loading.value = true
  try {
    const res: any = await alertRecordApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ruleName: filterRuleName.value || undefined,
      alertLevel: filterAlertLevel.value || undefined,
      status: filterStatus.value || undefined,
      targetType: filterTargetType.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD'),
      ruleType: filterRuleType.value || undefined,
      sensitivityLevel: filterSensitivityLevel.value || undefined
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  try {
    const res = await alertRecordApi.getOverview()
    const data: any = res.data?.data || {}
    if (data) {
      // 后端返回 pendingCount（今日待处理/已发送）、todayTotal（今日总数）、todayResolved（今日已解决）、criticalCount（待处理严重告警）
      overviewStats.total = data.todayTotal ?? 0
      overviewStats.pending = data.pendingCount ?? 0
      overviewStats.critical = data.criticalCount ?? 0
      overviewStats.resolved = data.todayResolved ?? 0
    }
  } catch { /* ignore */ }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilters() {
  filterRuleName.value = ''
  filterAlertLevel.value = undefined
  filterStatus.value = undefined
  filterTargetType.value = undefined
  filterRuleType.value = undefined
  filterSensitivityLevel.value = undefined
  dateRange.value = []
  pagination.current = 1
  loadData()
}

// ============ 操作 ============
function showDetail(record: AlertRecord) {
  currentRecord.value = record
  detailDrawerVisible.value = true
}

async function handleMarkRead(record: AlertRecord) {
  try {
    await alertRecordApi.markAsRead(record.id!)
    record.status = 'READ'
    if (currentRecord.value?.id === record.id) {
      currentRecord.value!.status = 'READ'
    }
    message.success('已标记为已读')
    loadData()
    loadOverview()
  } catch {
    message.error('操作失败')
  }
}

function showResolveModal() {
  resolveComment.value = ''
  resolveModalVisible.value = true
}

async function handleDoResolve() {
  if (!currentRecord.value?.id) return
  try {
    await alertRecordApi.resolve(currentRecord.value.id, resolveComment.value)
    currentRecord.value.status = 'RESOLVED'
    currentRecord.value.resolveComment = resolveComment.value
    message.success('已标记为已解决')
    resolveModalVisible.value = false
    detailDrawerVisible.value = false
    loadData()
    loadOverview()
  } catch {
    message.error('操作失败')
  }
}

async function handleResolve(record: AlertRecord) {
  try {
    await alertRecordApi.resolve(record.id!)
    record.status = 'RESOLVED'
    if (currentRecord.value?.id === record.id) {
      currentRecord.value!.status = 'RESOLVED'
    }
    message.success('已标记为已解决')
    loadData()
    loadOverview()
  } catch {
    message.error('操作失败')
  }
}

async function handleBatchMarkRead() {
  try {
    await alertRecordApi.batchMarkAsRead(selectedRowKeys.value)
    message.success(`已标记 ${selectedRowKeys.value.length} 条为已读`)
    selectedRowKeys.value = []
    loadData()
    loadOverview()
  } catch {
    message.error('操作失败')
  }
}

async function handleBatchResolve() {
  try {
    await alertRecordApi.batchResolve(selectedRowKeys.value, '批量处理')
    message.success(`已处理 ${selectedRowKeys.value.length} 条告警`)
    selectedRowKeys.value = []
    loadData()
    loadOverview()
  } catch {
    message.error('操作失败')
  }
}

// ============ 状态流转 ============
function isTimelineDone(step: string) {
  const status = currentRecord.value?.status
  if (!status) return false
  const order: Record<string, number> = { create: 0, sent: 1, read: 2, resolved: 3 }
  const currentOrder: Record<string, number> = { PENDING: 0, SENT: 1, READ: 2, RESOLVED: 3 }
  return (order[step] ?? 0) < (currentOrder[status] ?? 0)
}

// ============ 辅助函数 ============
function getAlertLevelColor(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: 'blue', WARN: 'warning', WARNING: 'warning', ERROR: 'error', CRITICAL: 'error'
  }
  return map[level || ''] || 'default'
}

function getAlertLevelLabel(level: string | undefined) {
  const map: Record<string, string> = {
    INFO: '信息', WARN: '警告', WARNING: '警告', ERROR: '错误', CRITICAL: '严重'
  }
  return map[level || ''] || level || '-'
}

function getStatusLabel(status: string | undefined) {
  const map: Record<string, string> = {
    PENDING: '待处理', SENT: '已发送', READ: '已读', RESOLVED: '已解决'
  }
  return map[status || ''] || status || '-'
}

function getTargetTypeColor(type: string | undefined) {
  const map: Record<string, string> = {
    TASK: 'blue', METRIC: 'purple', SYSTEM: 'orange', QUALITY: 'cyan'
  }
  return map[type || ''] || 'default'
}

function getSensitivityLevelTagColor(level: string | undefined) {
  const map: Record<string, string> = {
    L4: 'error', L3: 'orange', L2: 'processing', L1: 'success'
  }
  return map[level || ''] || 'default'
}

function getSensitivityAlertTypeLabel(ruleName: string | undefined) {
  if (!ruleName) return ''
  if (ruleName.includes('突增')) return '字段突增'
  if (ruleName.includes('等级')) return '等级变更'
  if (ruleName.includes('访问')) return '访问异常'
  if (ruleName.includes('超期') || ruleName.includes('待审核')) return '待审核超期'
  return ''
}

function formatTime(time: string | undefined) {
  if (!time) return '-'
  try {
    return new Date(time).toLocaleString('zh-CN')
  } catch { return time }
}

onMounted(() => {
  loadData()
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

// 概览卡片
.overview-card {
  background: white; border-radius: 10px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  display: flex; align-items: center; gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
  .overview-icon { font-size: 32px; opacity: 0.8; }
  .overview-content { flex: 1; }
  .overview-value { font-size: 28px; font-weight: 700; color: #1F1F1F; }
  .overview-label { font-size: 13px; color: #8C8C8C; margin-top: 4px; }
}
.total-card .overview-icon { color: #1677FF; }
.pending-card .overview-icon { color: #FA8C16; }
.critical-card .overview-icon { color: #FF4D4F; }
.resolved-card .overview-icon { color: #52C41A; }

.filter-bar {
  background: white; border-radius: 10px;
  padding: 16px 20px; margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.table-card {
  background: white; border-radius: 10px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.alert-level-cell { display: flex; align-items: center; }

.status-cell {
  display: flex; align-items: center; gap: 6px;
}
.status-indicator {
  width: 8px; height: 8px; border-radius: 50%;
  &.status-pending { background: #FA8C16; }
  &.status-sent { background: #1677FF; }
  &.status-read { background: #722ED1; }
  &.status-resolved { background: #52C41A; }
}
.status-label { font-size: 13px; }

.target-cell { display: flex; align-items: center; gap: 6px; }
.target-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.trigger-cell { display: flex; align-items: center; gap: 4px; }
.trigger-value { font-family: 'JetBrains Mono', monospace; font-weight: 600; color: #FF4D4F; }
.threshold-info { color: #8C8C8C; font-size: 12px; font-family: 'JetBrains Mono', monospace; }

.time-text { font-size: 13px; }

// 详情抽屉
.detail-overview {
  border-radius: 10px; padding: 20px; margin-bottom: 24px;
  border-left: 4px solid transparent;
  &.level-info { background: #E6F7FF; border-left-color: #1677FF; }
  &.level-warn, &.level-warning { background: #FFFAF0; border-left-color: #FA8C16; }
  &.level-error { background: #FFF1F0; border-left-color: #FF4D4F; }
  &.level-critical { background: #F9F0FF; border-left-color: #722ED1; }
  .detail-level-badge {
    display: inline-block; font-size: 12px; font-weight: 600;
    padding: 2px 10px; border-radius: 4px; margin-bottom: 8px;
    &.badge-info { background: #1677FF; color: white; }
    &.badge-warn, &.badge-warning { background: #FA8C16; color: white; }
    &.badge-error { background: #FF4D4F; color: white; }
    &.badge-critical { background: #722ED1; color: white; }
  }
  .detail-title { font-size: 18px; font-weight: 700; color: #1F1F1F; margin-bottom: 8px; }
  .detail-status-bar {
    display: flex; align-items: center; gap: 8px;
    font-size: 13px; color: #666;
  }
  .detail-status-indicator {
    width: 8px; height: 8px; border-radius: 50%;
    &.status-pending { background: #FA8C16; }
    &.status-sent { background: #1677FF; }
    &.status-read { background: #722ED1; }
    &.status-resolved { background: #52C41A; }
  }
}

.detail-section {
  margin-bottom: 24px;
  .section-title {
    font-size: 14px; font-weight: 600; color: #1F1F1F;
    margin-bottom: 12px; display: flex; align-items: center; gap: 8px;
  }
}

.detail-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;
  .detail-item {
    background: #F5F7FA; border-radius: 8px; padding: 12px 16px;
    .detail-label { font-size: 12px; color: #8C8C8C; margin-bottom: 4px; }
    .detail-value { font-size: 14px; font-weight: 600; color: #1F1F1F; }
    .mono-value { font-family: 'JetBrains Mono', monospace; }
  }
}

.alert-content-box {
  background: #F5F7FA; border-radius: 8px; padding: 16px;
  .content-pre {
    margin: 0; font-family: 'JetBrains Mono', monospace;
    font-size: 13px; color: #333; line-height: 1.8; white-space: pre-wrap;
  }
}

.status-timeline {
  display: flex; flex-direction: column; gap: 0;
  padding-left: 8px;
  .timeline-item {
    display: flex; align-items: flex-start; gap: 12px;
    position: relative; padding-bottom: 16px;
    &.done .timeline-dot { background: #52C41A; border-color: #52C41A; }
    &.done .timeline-title { color: #52C41A; }
    &:not(:last-child) .timeline-dot::after {
      content: ''; position: absolute;
      left: 7px; top: 16px;
      width: 2px; height: calc(100% - 16px);
      background: #E8E8E8;
    }
    &.done:not(:last-child) .timeline-dot::after { background: #52C41A; }
  }
  .timeline-dot {
    width: 16px; height: 16px; border-radius: 50%;
    background: white; border: 2px solid #E8E8E8;
    flex-shrink: 0; margin-top: 2px;
    &.done { background: #52C41A; border-color: #52C41A; }
  }
  .timeline-content { flex: 1; }
  .timeline-title { font-size: 14px; font-weight: 600; color: #1F1F1F; }
  .timeline-time { font-size: 12px; color: #8C8C8C; margin-top: 2px; }
}

.resolve-comment {
  background: #F6FFED; border: 1px solid #B7EB8F; border-radius: 8px; padding: 16px;
  .resolve-meta { font-size: 12px; color: #52C41A; margin-bottom: 8px; display: flex; gap: 12px; }
  .resolve-content { font-size: 14px; color: #333; line-height: 1.8; }
}

// 解决弹窗
.resolve-form {
  .resolve-tip {
    background: #FFFAF0; border: 1px solid #FFE58F;
    border-radius: 6px; padding: 12px; font-size: 13px;
    color: #FA8C16; margin-bottom: 16px;
  }
}

.text-muted { color: #8C8C8C; font-size: 13px; }
</style>
