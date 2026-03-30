<template>
  <div class="sensitivity-manage-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#manageGrad)"/>
            <path d="M9 12l2 2 4-4" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M12 2a10 10 0 100 20 10 10 0 000-20z" stroke="white" stroke-width="1.5"/>
            <defs>
              <linearGradient id="manageGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FA8C16"/>
                <stop offset="100%" stop-color="#FFD591"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">敏感字段管理</h1>
          <p class="page-subtitle">审核、调整和管理扫描识别到的敏感字段，确保敏感数据分类分级准确</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="loadData" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #FA8C16 0%, #FFD591 100%)">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">敏感字段总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #1677FF 0%, #69B1FF 100%)">
          <div class="stat-value">{{ stats.pending || 0 }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-value">{{ stats.approved || 0 }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #F5222D 0%, #FF7875 100%)">
          <div class="stat-value">{{ stats.rejected || 0 }}</div>
          <div class="stat-label">已驳回</div>
        </div>
      </a-col>
    </a-row>

    <!-- 主内容卡片 -->
    <div class="table-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <a-input
            v-model:value="filterParams.tableName"
            placeholder="搜索表名/字段名"
            style="width: 200px"
            allowClear
            @pressEnter="handleSearch"
            @change="debouncedSearch"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-select
            v-model:value="filterParams.dsId"
            placeholder="数据源"
            style="width: 160px"
            allowClear
            show-search
            :filter-option="filterDsOption"
            @change="handleSearch"
          >
            <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
              {{ ds.dsName }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterParams.levelId"
            placeholder="敏感等级"
            style="width: 130px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option v-for="lv in levelList" :key="lv.id" :value="lv.id">
              <a-tag :color="lv.color" style="margin-right: 4px">{{ lv.levelCode }}</a-tag>
              {{ lv.levelName }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterParams.classId"
            placeholder="数据分类"
            style="width: 130px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option v-for="cls in classificationList" :key="cls.id" :value="cls.id">
              {{ cls.className }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterParams.reviewStatus"
            placeholder="审核状态"
            style="width: 120px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option value="PENDING">待审核</a-select-option>
            <a-select-option value="APPROVED">已通过</a-select-option>
            <a-select-option value="REJECTED">已驳回</a-select-option>
          </a-select>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </div>
        <div class="toolbar-right">
          <span class="toolbar-hint">勾选行后可批量审核</span>
          <a-button
            type="default"
            danger
            :disabled="selectedRowKeys.length === 0"
            @click="handleBatchReject"
          >
            <template #icon><CloseCircleOutlined /></template>
            批量驳回<span v-if="selectedRowKeys.length"> ({{ selectedRowKeys.length }})</span>
          </a-button>
          <a-button
            type="primary"
            :disabled="selectedRowKeys.length === 0"
            @click="handleBatchApprove"
          >
            <template #icon><CheckCircleOutlined /></template>
            批量通过<span v-if="selectedRowKeys.length"> ({{ selectedRowKeys.length }})</span>
          </a-button>
          <a-button type="primary" @click="handleExport" :loading="exporting">
            <template #icon><ExportOutlined /></template>
            导出 Excel
          </a-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="paginationConfig"
        :row-selection="rowSelection"
        :scroll="{ x: 1400 }"
        :row-key="(r: SecColumnSensitivityVO) => r.id || `${r.dsId}-${r.tableName}-${r.columnName}`"
        @change="handleTableChange"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <!-- 字段名 -->
          <template v-if="column.key === 'columnName'">
            <div class="column-cell">
              <code class="column-name">{{ record.columnName }}</code>
              <div class="column-meta">
                <span v-if="record.columnComment" class="column-comment">{{ record.columnComment }}</span>
                <span v-else class="column-no-comment">无注释</span>
              </div>
            </div>
          </template>

          <!-- 敏感等级 -->
          <template v-if="column.key === 'levelName'">
            <a-tag
              v-if="record.levelName"
              :color="record.levelColor || getLevelColor(record.levelName)"
              style="font-weight: 600; font-size: 12px"
            >
              {{ record.levelName }}
            </a-tag>
            <span v-else style="color: #d9d9d9">—</span>
          </template>

          <!-- 置信度 -->
          <template v-if="column.key === 'confidence'">
            <div class="confidence-wrapper">
              <a-progress
                :percent="Math.round(record.confidence || 0)"
                :size="'small'"
                :stroke-color="getConfidenceColor(record.confidence)"
                :status="record.confidence && record.confidence >= 90 ? 'success' : undefined"
                style="width: 80px"
              />
              <span class="confidence-value">{{ record.confidence ? record.confidence.toFixed(1) : '—' }}%</span>
            </div>
          </template>

          <!-- 审核状态 -->
          <template v-if="column.key === 'reviewStatusLabel'">
            <a-badge
              :status="getReviewStatusBadge(record.reviewStatus)"
              :text="record.reviewStatusLabel"
            />
          </template>

          <!-- 脱敏方式 -->
          <template v-if="column.key === 'maskType'">
            <a-tag :color="getMaskTypeColor(record.maskType)">
              {{ record.maskTypeLabel || record.maskType || '未配置' }}
            </a-tag>
          </template>

          <!-- 扫描时间 -->
          <template v-if="column.key === 'scanTime'">
            <span class="time-text">{{ formatDateTime(record.scanTime) }}</span>
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-tooltip title="查看详情">
                <a-button type="text" size="small" @click="openDetailDrawer(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.reviewStatus === 'PENDING'" title="审核">
                <a-button type="text" size="small" @click="handleReview(record)">
                  <template #icon><AuditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm
                title="确定删除该敏感字段记录？"
                @confirm="handleDelete(record)"
              >
                <a-tooltip title="删除">
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </a-space>
          </template>
        </template>

        <!-- 空状态 -->
        <template #emptyText>
          <div class="table-empty">
            <SafetyOutlined class="empty-icon" />
            <div class="empty-title">暂无敏感字段数据</div>
            <div class="empty-desc">前往敏感字段扫描页面，执行扫描后即可管理识别结果</div>
            <a-space>
              <RouterLink to="/security/sensitivity-scan">
                <a-button type="primary">去扫描</a-button>
              </RouterLink>
            </a-space>
          </div>
        </template>
      </a-table>
    </div>

    <!-- 审核抽屉 -->
    <a-drawer
      v-model:open="reviewDrawerVisible"
      :title="`审核敏感字段 — ${currentRecord?.columnName || ''}`"
      :width="560"
      :mask-closable="false"
      @close="handleReviewDrawerClose"
    >
      <template v-if="currentRecord">
        <div class="record-summary">
          <div class="record-field-name">
            <code>{{ currentRecord.columnName }}</code>
            <span class="record-table">{{ currentRecord.tableName }}</span>
          </div>
          <div class="record-meta-row">
            <a-tag v-if="currentRecord.levelName" :color="currentRecord.levelColor || getLevelColor(currentRecord.levelName)">
              {{ currentRecord.levelName }}
            </a-tag>
            <a-tag v-if="currentRecord.className">{{ currentRecord.className }}</a-tag>
            <a-badge :status="getReviewStatusBadge(currentRecord.reviewStatus)" :text="currentRecord.reviewStatusLabel" />
          </div>
        </div>

        <a-divider>字段信息</a-divider>

        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="数据源">{{ currentRecord.dsName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="数据类型">{{ currentRecord.dataType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="字段注释" :span="2">{{ currentRecord.columnComment || '无' }}</a-descriptions-item>
          <a-descriptions-item label="脱敏方式">{{ currentRecord.maskTypeLabel || '未配置' }}</a-descriptions-item>
          <a-descriptions-item label="置信度">
            <a-progress
              :percent="Math.round(currentRecord.confidence || 0)"
              :size="'small'"
              :stroke-color="getConfidenceColor(currentRecord.confidence)"
              style="width: 100px"
            />
          </a-descriptions-item>
          <a-descriptions-item label="扫描批次" :span="2">
            <span v-if="currentRecord.scanBatchNo" class="batch-no-text">{{ currentRecord.scanBatchNo }}</span>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="扫描时间">{{ formatDateTime(currentRecord.scanTime) }}</a-descriptions-item>
          <a-descriptions-item label="审核人">
            {{ currentRecord.approvedByName || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>审核操作</a-divider>

        <a-form
          ref="reviewFormRef"
          :model="reviewForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          layout="horizontal"
        >
          <a-form-item label="审核结果" name="reviewStatus">
            <a-radio-group v-model:value="reviewForm.reviewStatus">
              <a-radio value="APPROVED">
                <CheckCircleOutlined style="color: #52C41A" /> 通过
              </a-radio>
              <a-radio value="REJECTED">
                <CloseCircleOutlined style="color: #F5222D" /> 驳回
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="审核意见" name="reviewComment">
            <a-textarea
              v-model:value="reviewForm.reviewComment"
              placeholder="请输入审核意见（选填）"
              :rows="3"
              :maxlength="500"
              show-count
            />
          </a-form-item>
        </a-form>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="handleReviewDrawerClose">取消</a-button>
          <a-button type="primary" :loading="reviewLoading" @click="handleSubmitReview">
            提交审核
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="`敏感字段详情 — ${currentRecord?.columnName || ''}`"
      :width="600"
      @close="detailDrawerVisible = false"
    >
      <template v-if="currentRecord">
        <div class="detail-header">
          <div class="detail-field">
            <code class="detail-column">{{ currentRecord.columnName }}</code>
            <span class="detail-table">{{ currentRecord.dsName }} / {{ currentRecord.tableName }}</span>
          </div>
          <div class="detail-tags">
            <a-tag :color="currentRecord.levelColor || getLevelColor(currentRecord.levelName)" style="font-size: 13px">
              {{ currentRecord.levelName }}
            </a-tag>
            <a-tag color="blue">{{ currentRecord.className || '未分类' }}</a-tag>
            <a-badge
              :status="getReviewStatusBadge(currentRecord.reviewStatus)"
              :text="currentRecord.reviewStatusLabel"
            />
          </div>
        </div>

        <a-divider>基本信息</a-divider>
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="数据类型">{{ currentRecord.dataType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="置信度">{{ currentRecord.confidence ? currentRecord.confidence.toFixed(1) + '%' : '-' }}</a-descriptions-item>
          <a-descriptions-item label="脱敏方式">{{ currentRecord.maskTypeLabel || '未配置' }}</a-descriptions-item>
          <a-descriptions-item label="脱敏样式">{{ currentRecord.maskPattern || '-' }}</a-descriptions-item>
          <a-descriptions-item label="字段注释" :span="2">{{ currentRecord.columnComment || '无' }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>扫描信息</a-divider>
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="扫描批次">{{ currentRecord.scanBatchNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="扫描时间">{{ formatDateTime(currentRecord.scanTime) }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>审核信息</a-divider>
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="审核状态">
            <a-badge :status="getReviewStatusBadge(currentRecord.reviewStatus)" :text="currentRecord.reviewStatusLabel" />
          </a-descriptions-item>
          <a-descriptions-item label="审核人">{{ currentRecord.approvedByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="审核时间">{{ formatDateTime(currentRecord.approvedTime) }}</a-descriptions-item>
          <a-descriptions-item label="审核意见" :span="2">{{ currentRecord.reviewComment || '无' }}</a-descriptions-item>
        </a-descriptions>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="detailDrawerVisible = false">关闭</a-button>
          <a-button
            v-if="currentRecord && currentRecord.reviewStatus === 'PENDING'"
            type="primary"
            @click="() => { openDetailForReview() }"
          >
            审核该字段
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 批量审核确认弹窗 -->
    <a-modal
      v-model:open="batchReviewModalVisible"
      :title="batchReviewType === 'APPROVED' ? '批量审核通过' : '批量驳回'"
      @ok="handleConfirmBatchReview"
    >
      <div class="batch-review-summary">
        <div v-if="batchReviewType === 'APPROVED'" class="batch-icon approved">
          <CheckCircleOutlined />
        </div>
        <div v-else class="batch-icon rejected">
          <CloseCircleOutlined />
        </div>
        <div class="batch-info">
          <div class="batch-title">
            确定要{{ batchReviewType === 'APPROVED' ? '审核通过' : '驳回' }}
            <strong style="color: #FA8C16">{{ selectedRowKeys.length }}</strong> 条敏感字段记录吗？
          </div>
          <div class="batch-sub" v-if="selectedRows.length > 0">
            涉及 {{ uniqueDsCount }} 个数据源，{{ uniqueLevelCount }} 种敏感等级
          </div>
        </div>
      </div>

      <a-form :label-col="{ span: 4 }" style="margin-top: 16px">
        <a-form-item label="审核意见">
          <a-textarea
            v-model:value="batchReviewComment"
            placeholder="请输入审核意见（选填）"
            :rows="3"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ExportOutlined, ReloadOutlined, SearchOutlined,
  CheckCircleOutlined, CloseCircleOutlined, EyeOutlined,
  DeleteOutlined, AuditOutlined, SafetyOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  pageSensitivity,
  getSensitivityReviewCounts,
  reviewSensitivity,
  batchReviewSensitivity,
  deleteSensitivity,
  listClassification,
  listLevel,
  type SecColumnSensitivityVO,
  type ReviewStatus
} from '@/api/security'
import { dataSourceApi } from '@/api/dqc'

// ==================== 状态 ====================
const loading = ref(false)
const exporting = ref(false)
const reviewLoading = ref(false)

const tableData = ref<SecColumnSensitivityVO[]>([])
const selectedRowKeys = ref<number[]>([])
const selectedRows = ref<SecColumnSensitivityVO[]>([])

const paginationConfig = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
})

// 统计数据
const stats = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  rejected: 0
})

// 筛选参数
const filterParams = reactive({
  dsId: undefined as number | undefined,
  tableName: '',
  levelId: undefined as number | undefined,
  classId: undefined as number | undefined,
  reviewStatus: undefined as ReviewStatus | undefined
})

// 下拉选项
const dataSourceList = ref<{ id: number; dsName: string }[]>([])
const classificationList = ref<{ id: number; className: string }[]>([])
const levelList = ref<{ id: number; levelCode: string; levelName: string; color: string }[]>([])

// 审核抽屉
const reviewDrawerVisible = ref(false)
const currentRecord = ref<SecColumnSensitivityVO | null>(null)
const reviewFormRef = ref<FormInstance>()
const reviewForm = reactive({
  reviewStatus: 'APPROVED' as ReviewStatus,
  reviewComment: ''
})

// 详情抽屉
const detailDrawerVisible = ref(false)

// 批量审核弹窗
const batchReviewModalVisible = ref(false)
const batchReviewType = ref<ReviewStatus>('APPROVED')
const batchReviewComment = ref('')

// ==================== 计算属性 ====================
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[], rows: SecColumnSensitivityVO[]) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  }
}))

const uniqueDsCount = computed(() => {
  return new Set(selectedRows.value.map(r => r.dsId)).size
})

const uniqueLevelCount = computed(() => {
  return new Set(selectedRows.value.map(r => r.levelName).filter(Boolean)).size
})

// ==================== 表格列 ====================
const columns = [
  { title: '数据源', dataIndex: 'dsName', key: 'dsName', width: 130, ellipsis: true },
  { title: '表名', dataIndex: 'tableName', key: 'tableName', width: 150, ellipsis: true },
  { title: '字段名', key: 'columnName', width: 180 },
  { title: '数据类型', dataIndex: 'dataType', key: 'dataType', width: 100, ellipsis: true },
  { title: '敏感等级', key: 'levelName', width: 100, align: 'center' as const },
  { title: '置信度', key: 'confidence', width: 130 },
  { title: '审核状态', key: 'reviewStatusLabel', width: 100, align: 'center' as const },
  { title: '脱敏方式', key: 'maskType', width: 100, align: 'center' as const },
  { title: '扫描时间', key: 'scanTime', width: 170 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' as const }
]

// ==================== 工具函数 ====================
function filterDsOption(input: string, option: any) {
  return option.children?.[0]?.children?.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

function getLevelColor(level?: string) {
  const map: Record<string, string> = {
    'L4': '#F5222D', 'L3': '#FA8C16', 'L2': '#1677FF', 'L1': '#52C41A',
    'HIGHLY_SENSITIVE': '#F5222D', 'SENSITIVE': '#FA8C16',
    'INTERNAL': '#1677FF', 'NORMAL': '#52C41A'
  }
  return map[level || ''] || 'default'
}

function getConfidenceColor(confidence?: number) {
  if (!confidence) return '#d9d9d9'
  if (confidence >= 90) return '#52C41A'
  if (confidence >= 70) return '#FAAD14'
  return '#F5222D'
}

function getReviewStatusBadge(status?: string): 'success' | 'processing' | 'error' | 'default' {
  const map: Record<string, 'success' | 'processing' | 'error' | 'default'> = {
    APPROVED: 'success',
    PENDING: 'processing',
    REJECTED: 'error'
  }
  return map[status || ''] || 'default'
}

function getMaskTypeColor(maskType?: string) {
  const map: Record<string, string> = {
    NONE: 'default', MASK: 'blue', HIDE: 'orange',
    ENCRYPT: 'red', DELETE: 'purple', PARTIAL_MASK: 'cyan',
    HASH: 'purple', RANDOM_REPLACE: 'cyan', RANGE: 'orange'
  }
  return map[maskType || ''] || 'default'
}

function formatDateTime(time?: string | number[] | Record<string, unknown>) {
  if (time == null || time === '') return '-'
  if (Array.isArray(time)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = time
    const dt = new Date(y, (m as number) - 1, d, h, mi, s)
    return Number.isNaN(dt.getTime()) ? '-' : dt.toLocaleString('zh-CN')
  }
  if (typeof time === 'string') {
    const normalized = time.includes('T') ? time : time.replace(' ', 'T')
    const dt = new Date(normalized)
    if (!Number.isNaN(dt.getTime())) return dt.toLocaleString('zh-CN')
    return time
  }
  return '-'
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  selectedRowKeys.value = []
  selectedRows.value = []
  try {
    const [countRes, pageRes] = await Promise.all([
      getSensitivityReviewCounts(),
      pageSensitivity({
        pageNum: paginationConfig.current,
        pageSize: paginationConfig.pageSize,
        dsId: filterParams.dsId,
        tableName: filterParams.tableName || undefined,
        levelId: filterParams.levelId,
        classId: filterParams.classId,
        reviewStatus: filterParams.reviewStatus
      })
    ])

    if (countRes.code === 200 && countRes.data) {
      stats.total = Number(countRes.data.total) || 0
      stats.pending = Number(countRes.data.pending) || 0
      stats.approved = Number(countRes.data.approved) || 0
      stats.rejected = Number(countRes.data.rejected) || 0
    }

    if (pageRes.code === 200 && pageRes.data) {
      tableData.value = pageRes.data.records || []
      paginationConfig.total = pageRes.data.total || 0
    }
  } catch (error) {
    message.error('加载敏感字段数据失败')
  } finally {
    loading.value = false
  }
}

async function loadFilterOptions() {
  try {
    const [dsRes, classRes, levelRes] = await Promise.all([
      dataSourceApi.listEnabled(),
      listClassification(1),
      listLevel()
    ])

    if (dsRes.code === 200 && dsRes.data) {
      dataSourceList.value = dsRes.data
    }
    if (classRes.code === 200 && classRes.data) {
      classificationList.value = classRes.data
    }
    if (levelRes.code === 200 && levelRes.data) {
      levelList.value = levelRes.data
    }
  } catch (error) {
    console.error('加载筛选选项失败:', error)
  }
}

// ==================== 事件处理 ====================
let searchTimer: any = null
function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    paginationConfig.current = 1
    loadData()
  }, 300)
}

function handleSearch() {
  paginationConfig.current = 1
  loadData()
}

function handleReset() {
  filterParams.dsId = undefined
  filterParams.tableName = ''
  filterParams.levelId = undefined
  filterParams.classId = undefined
  filterParams.reviewStatus = undefined
  paginationConfig.current = 1
  loadData()
}

function handleTableChange(pag: any) {
  paginationConfig.current = pag.current
  paginationConfig.pageSize = pag.pageSize
  loadData()
}

function openDetailDrawer(record: SecColumnSensitivityVO) {
  currentRecord.value = record
  detailDrawerVisible.value = true
}

function openDetailForReview() {
  detailDrawerVisible.value = false
  handleReview(currentRecord.value!)
}

function handleReview(record: SecColumnSensitivityVO) {
  currentRecord.value = record
  reviewForm.reviewStatus = 'APPROVED'
  reviewForm.reviewComment = ''
  reviewDrawerVisible.value = true
}

function handleReviewDrawerClose() {
  reviewDrawerVisible.value = false
  currentRecord.value = null
  reviewFormRef.value?.resetFields()
}

async function handleSubmitReview() {
  if (!currentRecord.value?.id) return
  reviewLoading.value = true
  try {
    const res = await reviewSensitivity(
      currentRecord.value.id,
      reviewForm.reviewStatus,
      reviewForm.reviewComment || undefined
    )
    if (res.code === 200) {
      message.success('审核提交成功')
      handleReviewDrawerClose()
      loadData()
    } else {
      message.error(res.message || '审核提交失败')
    }
  } catch {
    message.error('审核提交失败')
  } finally {
    reviewLoading.value = false
  }
}

async function handleDelete(record: SecColumnSensitivityVO) {
  if (!record.id) return
  try {
    const res = await deleteSensitivity(record.id)
    if (res.code === 200) {
      message.success('删除成功')
      loadData()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
}

function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要审核的记录')
    return
  }
  batchReviewType.value = 'APPROVED'
  batchReviewComment.value = ''
  batchReviewModalVisible.value = true
}

function handleBatchReject() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要驳回的记录')
    return
  }
  batchReviewType.value = 'REJECTED'
  batchReviewComment.value = ''
  batchReviewModalVisible.value = true
}

async function handleConfirmBatchReview() {
  if (selectedRowKeys.value.length === 0) return
  reviewLoading.value = true
  try {
    const res = await batchReviewSensitivity(
      selectedRowKeys.value,
      batchReviewType.value,
      batchReviewComment.value || undefined
    )
    if (res.code === 200) {
      message.success(`批量${batchReviewType.value === 'APPROVED' ? '审核通过' : '驳回'}成功`)
      batchReviewModalVisible.value = false
      selectedRowKeys.value = []
      selectedRows.value = []
      loadData()
    } else {
      message.error(res.message || '批量审核失败')
    }
  } catch {
    message.error('批量审核失败')
  } finally {
    reviewLoading.value = false
  }
}

// ==================== 导出 ====================
async function handleExport() {
  exporting.value = true
  try {
    // 构建导出查询参数
    const params = new URLSearchParams()
    params.append('pageNum', '1')
    params.append('pageSize', String(paginationConfig.total > 0 ? paginationConfig.total : 10000))
    if (filterParams.dsId) params.append('dsId', String(filterParams.dsId))
    if (filterParams.tableName) params.append('tableName', filterParams.tableName)
    if (filterParams.levelId) params.append('levelId', String(filterParams.levelId))
    if (filterParams.classId) params.append('classId', String(filterParams.classId))
    if (filterParams.reviewStatus) params.append('reviewStatus', filterParams.reviewStatus)

    // 请求后端导出接口（如果后端实现了 /gov/security/sensitivity/export 接口）
    // 目前使用前端导出：从已加载数据+分页获取全部数据
    const allRows: any[] = [...tableData.value]
    const totalPages = Math.ceil(paginationConfig.total / 200)
    for (let page = 2; page <= totalPages && allRows.length < 10000; page++) {
      const res = await pageSensitivity({
        pageNum: page,
        pageSize: 200,
        dsId: filterParams.dsId,
        tableName: filterParams.tableName || undefined,
        levelId: filterParams.levelId,
        classId: filterParams.classId,
        reviewStatus: filterParams.reviewStatus
      })
      if (res.code === 200 && res.data?.records) {
        allRows.push(...res.data.records)
      }
    }

    if (allRows.length === 0) {
      message.warning('暂无数据可导出')
      return
    }

    // 生成 CSV 内容
    const headers = ['数据源', '表名', '字段名', '数据类型', '敏感等级', '分类', '脱敏方式', '置信度(%)', '审核状态', '扫描批次', '扫描时间']
    const rows = allRows.map(r => [
      r.dsName || '',
      r.tableName || '',
      r.columnName || '',
      r.dataType || '',
      r.levelName || '',
      r.className || '',
      r.maskTypeLabel || r.maskType || '',
      r.confidence != null ? r.confidence.toFixed(1) : '',
      r.reviewStatusLabel || r.reviewStatus || '',
      r.scanBatchNo || '',
      r.scanTime ? formatDateTime(r.scanTime) : ''
    ])

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    ].join('\n')

    // 下载文件
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `敏感字段管理_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    message.success(`导出成功，共 ${allRows.length} 条记录`)
  } catch (error) {
    message.error('导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadFilterOptions()
  loadData()
})
</script>

<style scoped>
.sensitivity-manage-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #FA8C1610 0%, #FFD59110 100%);
  border: 1px solid #FA8C1620;
  border-radius: 12px;
  padding: 20px 24px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1F1F1F;
  line-height: 1.3;
}
.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8C8C8C;
}
.header-right { display: flex; align-items: center; gap: 8px; }

.stat-row { margin-bottom: 16px; }
.stat-card {
  border-radius: 12px;
  padding: 12px 14px;
  color: #fff;
  min-height: 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.stat-value { font-size: 22px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; opacity: 0.88; margin-top: 4px; }

.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-right: 4px;
  user-select: none;
}

.column-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.column-name {
  font-weight: 600;
  color: #1F1F1F;
  font-size: 13px;
}
.column-comment {
  font-size: 12px;
  color: #8C8C8C;
}
.column-no-comment {
  font-size: 12px;
  color: #d9d9d9;
  font-style: italic;
}

.confidence-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}
.confidence-value {
  font-size: 12px;
  color: #8C8C8C;
  min-width: 38px;
}

.time-text {
  font-size: 12px;
  color: #8C8C8C;
}

.batch-no-text {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #722ED1;
}

/* 记录摘要 */
.record-summary {
  padding: 16px;
  background: linear-gradient(135deg, #FA8C1610 0%, #fff 100%);
  border: 1px solid #FA8C1620;
  border-radius: 8px;
  margin-bottom: 16px;
}
.record-field-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.record-field-name code {
  font-size: 18px;
  font-weight: 700;
  color: #1F1F1F;
}
.record-table {
  font-size: 13px;
  color: #8C8C8C;
}
.record-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 详情抽屉 */
.detail-header {
  padding: 16px;
  background: linear-gradient(135deg, #FA8C1610 0%, #fff 100%);
  border: 1px solid #FA8C1620;
  border-radius: 8px;
  margin-bottom: 16px;
}
.detail-field {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-column {
  font-size: 20px;
  font-weight: 700;
  color: #1F1F1F;
}
.detail-table {
  font-size: 13px;
  color: #8C8C8C;
}
.detail-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 批量审核 */
.batch-review-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.batch-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}
.batch-icon.approved { background: #F6FFED; color: #52C41A; }
.batch-icon.rejected { background: #FFF1F0; color: #F5222D; }
.batch-info { flex: 1; }
.batch-title { font-size: 15px; font-weight: 500; color: #1F1F1F; margin-bottom: 4px; }
.batch-sub { font-size: 12px; color: #8C8C8C; }

/* 空状态 */
.table-empty {
  padding: 48px 0;
  text-align: center;
}
.empty-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 12px;
  display: block;
}
.empty-title { font-size: 16px; color: #595959; margin-bottom: 8px; }
.empty-desc { font-size: 13px; color: #8C8C8C; margin-bottom: 16px; }
</style>
