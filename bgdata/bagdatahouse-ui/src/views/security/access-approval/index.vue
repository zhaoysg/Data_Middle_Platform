<template>
  <div class="access-approval-page">
    <a-card :bordered="false" class="page-card">
      <!-- 页面标题区 -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <rect width="24" height="24" rx="6" fill="url(#approvalGrad)"/>
              <path d="M9 12l2 2 4-4" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12 2a10 10 0 100 20 10 10 0 000-20z" stroke="white" stroke-width="1.5"/>
              <defs>
                <linearGradient id="approvalGrad" x1="0" y1="0" x2="24" y2="24">
                  <stop offset="0%" stop-color="#722ED1"/>
                  <stop offset="100%" stop-color="#9254DE"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div>
            <h1 class="page-title">访问审批管理</h1>
            <p class="page-subtitle">敏感字段访问申请、审批和全链路审计追踪</p>
          </div>
        </div>
        <div class="header-right">
          <a-button type="primary" @click="handleApplyAccess">
            <template #icon><PlusOutlined /></template>
            申请访问
          </a-button>
        </div>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <!-- Tab 1: 我的申请 -->
        <a-tab-pane key="myApplication" tab="我的申请">
          <div class="filter-bar">
            <a-space wrap :size="12">
              <a-input
                v-model:value="myAppFilters.applicationNo"
                placeholder="申请编号"
                style="width: 180px"
                allow-clear
                @pressEnter="handleSearchMyApplication"
              />
              <a-select
                v-model:value="myAppFilters.status"
                placeholder="状态"
                style="width: 140px"
                allow-clear
                @change="handleSearchMyApplication"
              >
                <a-select-option value="PENDING">审批中</a-select-option>
                <a-select-option value="APPROVED">已通过</a-select-option>
                <a-select-option value="REJECTED">已驳回</a-select-option>
                <a-select-option value="EXPIRED">已过期</a-select-option>
              </a-select>
              <a-button @click="handleResetMyApplication">重置</a-button>
            </a-space>
          </div>

          <a-table
            :columns="myApplicationColumns"
            :data-source="myApplicationData"
            :loading="myApplicationLoading"
            :pagination="myApplicationPagination"
            :row-key="(record: SecAccessApplicationVO) => record.id"
            :scroll="{ x: 1200 }"
            class="mt-4"
            @change="handleMyApplicationTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'targetColumns'">
                <a-tooltip :title="record.targetColumns">
                  <span class="text-ellipsis">{{ truncateText(record.targetColumns, 20) }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'durationHours'">
                {{ formatDuration(record.durationHours) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ record.statusLabel }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="handleViewDetail(record)">详情</a>
                  <a
                    v-if="record.status === 'PENDING'"
                    @click="handleCancelApplication(record)"
                    class="text-danger"
                  >
                    撤销
                  </a>
                </a-space>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无申请记录" />
            </template>
          </a-table>
        </a-tab-pane>

        <!-- Tab 2: 待我审批 -->
        <a-tab-pane key="pendingApproval" tab="待我审批">
          <div class="filter-bar">
            <a-space wrap :size="12">
              <a-input
                v-model:value="pendingFilters.applicationNo"
                placeholder="申请编号"
                style="width: 180px"
                allow-clear
                @pressEnter="handleSearchPending"
              />
              <a-button @click="handleResetPending">重置</a-button>
            </a-space>
          </div>

          <a-table
            :columns="pendingApprovalColumns"
            :data-source="pendingApprovalData"
            :loading="pendingApprovalLoading"
            :pagination="pendingApprovalPagination"
            :row-key="(record: SecAccessApplicationVO) => record.id"
            :scroll="{ x: 1400 }"
            class="mt-4"
            @change="handlePendingTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'targetColumns'">
                <a-tooltip :title="record.targetColumns">
                  <span class="text-ellipsis">{{ truncateText(record.targetColumns, 20) }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'accessReason'">
                <a-tooltip :title="record.accessReason">
                  <span class="text-ellipsis">{{ truncateText(record.accessReason, 30) }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'sensitivityLevel'">
                <span v-html="getSensitivityLevelDisplay(record.sensitivityLevelLabel)"></span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="handleApprove(record)">通过</a>
                  <a @click="handleReject(record)" class="text-danger">驳回</a>
                  <a @click="handleViewDetail(record)">详情</a>
                </a-space>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无待审批记录" />
            </template>
          </a-table>
        </a-tab-pane>

        <!-- Tab 3: 审计日志 -->
        <a-tab-pane key="auditLog" tab="审计日志">
          <div class="filter-bar">
            <a-space wrap :size="12">
              <a-select
                v-model:value="auditFilters.operationType"
                placeholder="操作类型"
                style="width: 140px"
                allow-clear
                @change="handleSearchAudit"
              >
                <a-select-option value="ACCESS">访问</a-select-option>
                <a-select-option value="APPLY">申请</a-select-option>
                <a-select-option value="APPROVE">通过</a-select-option>
                <a-select-option value="REJECT">驳回</a-select-option>
              </a-select>
              <a-input
                v-model:value="auditFilters.operatorName"
                placeholder="操作人"
                style="width: 140px"
                allow-clear
                @pressEnter="handleSearchAudit"
              />
              <a-range-picker
                v-model:value="auditFilters.dateRange"
                style="width: 280px"
                @change="handleSearchAudit"
              />
              <a-button @click="handleResetAudit">重置</a-button>
              <a-button type="primary" ghost @click="handleExportAudit">
                <template #icon><DownloadOutlined /></template>
                导出
              </a-button>
            </a-space>
          </div>

          <a-table
            :columns="auditLogColumns"
            :data-source="auditLogData"
            :loading="auditLogLoading"
            :pagination="auditLogPagination"
            :row-key="(record: SecAccessLogVO) => record.id"
            :scroll="{ x: 1400 }"
            class="mt-4"
            @change="handleAuditTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'operationType'">
                <a-tag :color="getOperationTypeColor(record.operationType)">
                  {{ record.operationTypeLabel }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'operator'">
                {{ record.operatorName }}
                <span v-if="record.operatorDept" class="text-muted">({{ record.operatorDept }})</span>
              </template>
              <template v-else-if="column.key === 'operationContent'">
                <a-tooltip :title="record.operationDetail || record.operationContent">
                  <span class="text-ellipsis">{{ truncateText(record.operationDetail || record.operationContent, 40) }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'ipAddress'">
                <a-tag>{{ record.ipAddress || '-' }}</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ record.statusLabel || record.status }}
                </a-tag>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无审计日志" />
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 申请访问弹窗 -->
    <a-modal
      v-model:open="applyModalVisible"
      title="申请敏感字段访问"
      width="640px"
      :mask-closable="false"
      @ok="handleSubmitApply"
      @cancel="handleCancelApply"
    >
      <a-form
        ref="applyFormRef"
        :model="applyFormData"
        :rules="applyFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="目标数据源" name="targetDsId">
          <a-select
            v-model:value="applyFormData.targetDsId"
            placeholder="请选择数据源"
            :loading="dataSourceLoading"
            show-search
            option-filter-prop="label"
            @change="handleDataSourceChange"
          >
            <a-select-option
              v-for="ds in dataSourceList"
              :key="ds.id"
              :value="ds.id"
              :label="ds.dsName"
            >
              {{ ds.dsName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="目标表名" name="targetTable">
          <a-select
            v-model:value="applyFormData.targetTable"
            placeholder="请先选择数据源"
            :disabled="!applyFormData.targetDsId"
            :loading="tableLoading"
            show-search
            option-filter-prop="label"
            @change="handleTableChange"
          >
            <a-select-option
              v-for="table in tableList"
              :key="table"
              :value="table"
              :label="table"
            >
              {{ table }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="访问字段" name="targetColumns">
          <a-select
            v-model:value="applyFormData.targetColumns"
            placeholder="请先选择目标表"
            :disabled="!applyFormData.targetTable"
            mode="multiple"
            :loading="columnLoading"
            show-search
            option-filter-prop="label"
            :max-tag-count="3"
          >
            <a-select-option
              v-for="col in columnList"
              :key="col.columnName"
              :value="col.columnName"
              :label="col.columnName"
            >
              <div class="column-option">
                <span>{{ col.columnName }}</span>
                <a-tag v-if="col.sensitivityLevel" :color="getSensitivityTagColor(col.sensitivityLevel)">
                  {{ col.sensitivityLevel }}
                </a-tag>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="访问时长" name="durationHours">
          <a-select v-model:value="applyFormData.durationHours" placeholder="请选择访问时长">
            <a-select-option :value="24">24小时</a-select-option>
            <a-select-option :value="168">一周（168小时）</a-select-option>
            <a-select-option :value="720">一个月（720小时）</a-select-option>
            <a-select-option :value="2160">三个月（2160小时）</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="申请原因" name="accessReason">
          <a-textarea
            v-model:value="applyFormData.accessReason"
            placeholder="请详细说明访问目的和数据使用计划..."
            :rows="3"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <!-- 敏感等级提示 -->
        <div v-if="sensitivityWarnings.length > 0" class="sensitivity-tips">
          <div v-for="(warning, index) in sensitivityWarnings" :key="index" class="sensitivity-tip-item">
            <span v-html="warning"></span>
          </div>
        </div>
      </a-form>

      <template #footer>
        <a-button @click="handleCancelApply">取消</a-button>
        <a-button type="primary" :loading="submitLoading" @click="handleSubmitApply">
          提交申请
        </a-button>
      </template>
    </a-modal>

    <!-- 审批弹窗 -->
    <a-modal
      v-model:open="approvalModalVisible"
      title="审批敏感字段访问申请"
      width="560px"
      :mask-closable="false"
      @cancel="handleCancelApproval"
    >
      <a-descriptions :column="2" bordered size="small" class="mb-4">
        <a-descriptions-item label="申请编号">{{ currentApprovalRecord?.applicationNo }}</a-descriptions-item>
        <a-descriptions-item label="申请人">
          {{ currentApprovalRecord?.applicantName }}
          <span v-if="currentApprovalRecord?.applicantDept" class="text-muted">
            （{{ currentApprovalRecord.applicantDept }}）
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="目标表">{{ currentApprovalRecord?.targetTable }}</a-descriptions-item>
        <a-descriptions-item label="访问字段">{{ currentApprovalRecord?.targetColumns }}</a-descriptions-item>
        <a-descriptions-item label="敏感等级">
          <span v-html="getSensitivityLevelDisplay(currentApprovalRecord?.sensitivityLevelLabel)"></span>
        </a-descriptions-item>
        <a-descriptions-item label="申请时长">
          {{ formatDuration(currentApprovalRecord?.durationHours) }}
        </a-descriptions-item>
        <a-descriptions-item label="申请原因" :span="2">
          {{ currentApprovalRecord?.accessReason || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-form ref="approvalFormRef" :model="approvalFormData" :rules="approvalFormRules">
        <a-form-item label="审批意见" name="approvalComment">
          <a-textarea
            v-model:value="approvalFormData.approvalComment"
            placeholder="请输入审批意见（选填）"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="handleRejectDirect" :loading="submitLoading">驳回</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleApproveDirect">通过</a-button>
        </a-space>
      </template>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="申请详情"
      width="680px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered size="small" class="mb-4">
        <a-descriptions-item label="申请编号">{{ currentDetailRecord?.applicationNo }}</a-descriptions-item>
        <a-descriptions-item label="申请人">
          {{ currentDetailRecord?.applicantName }}
          <span v-if="currentDetailRecord?.applicantDept" class="text-muted">
            （{{ currentDetailRecord.applicantDept }}）
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="目标数据源">{{ currentDetailRecord?.targetDsName }}</a-descriptions-item>
        <a-descriptions-item label="目标表">{{ currentDetailRecord?.targetTable }}</a-descriptions-item>
        <a-descriptions-item label="访问字段" :span="2">{{ currentDetailRecord?.targetColumns }}</a-descriptions-item>
        <a-descriptions-item label="敏感等级">
          <span v-html="getSensitivityLevelDisplay(currentDetailRecord?.sensitivityLevelLabel)"></span>
        </a-descriptions-item>
        <a-descriptions-item label="申请时长">{{ formatDuration(currentDetailRecord?.durationHours) }}</a-descriptions-item>
        <a-descriptions-item label="申请时间">{{ currentDetailRecord?.applyTime }}</a-descriptions-item>
        <a-descriptions-item label="申请状态">
          <a-tag :color="getStatusColor(currentDetailRecord?.status)">
            {{ currentDetailRecord?.statusLabel }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="有效期至">{{ currentDetailRecord?.expiryTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请原因" :span="2">{{ currentDetailRecord?.accessReason }}</a-descriptions-item>
        <a-descriptions-item label="审批人" v-if="currentDetailRecord?.approverName">
          {{ currentDetailRecord.approverName }}
        </a-descriptions-item>
        <a-descriptions-item label="审批时间" v-if="currentDetailRecord?.approvalTime">
          {{ currentDetailRecord.approvalTime }}
        </a-descriptions-item>
        <a-descriptions-item label="审批意见" :span="2" v-if="currentDetailRecord?.approvalComment">
          {{ currentDetailRecord.approvalComment }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- 审计历史 -->
      <div v-if="auditHistory.length > 0" class="audit-history">
        <h4>审批历史</h4>
        <a-timeline>
          <a-timeline-item v-for="item in auditHistory" :key="item.id" :color="getAuditTimelineColor(item.operationType)">
            <p><strong>{{ item.operatorName }}</strong> {{ getOperationText(item.operationType) }}</p>
            <p class="text-muted">{{ item.operationTime }}</p>
            <p v-if="item.operationContent">{{ item.operationContent }}</p>
          </a-timeline-item>
        </a-timeline>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  submitAccessApplication,
  pageAccessApply,
  pagePendingApproval,
  getAccessApplyById,
  cancelAccessApplication,
  approveAccessApplication,
  listAccessAuditHistory,
  pageAccessLog,
  listLevel,
  type SecAccessApplicationVO,
  type SecAccessApplicationSaveDTO,
  type SecAccessApprovalDTO,
  type SecAccessAuditVO,
  type SecAccessLogVO
} from '@/api/security'
import { dataSourceApi } from '@/api/dqc'
import { confirmModal } from '@/utils/modal'
import type { FormInstance, TablePaginationConfig } from 'ant-design-vue'
import type { DataSource } from '@/types'

// ==================== 类型定义 ====================

interface ColumnInfo {
  columnName: string
  dataType?: string
  columnComment?: string
  sensitivityLevel?: string
}

interface TableColumn {
  columnName: string
  dataType?: string
  columnComment?: string
  nullable?: boolean
  primaryKey?: boolean
  sensitivityLevel?: string
}

// ==================== 常量定义 ====================

const STATUS_COLORS: Record<string, string> = {
  APPROVED: '#52C41A',
  PENDING: '#FAAD14',
  REJECTED: '#F5222D',
  EXPIRED: '#8C8C8C'
}

const OPERATION_TYPE_COLORS: Record<string, string> = {
  ACCESS: '#1677FF',
  APPLY: '#FAAD14',
  APPROVE: '#52C41A',
  REJECT: '#F5222D'
}

const SENSITIVITY_COLORS: Record<string, string> = {
  L1: '#8C8C8C',
  L2: '#FAAD14',
  L3: '#FF7A45',
  L4: '#F5222D'
}

// ==================== 响应式状态 ====================

const activeTab = ref('myApplication')

// Tab 1: 我的申请
const myApplicationLoading = ref(false)
const myApplicationData = ref<SecAccessApplicationVO[]>([])
const myApplicationPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const myAppFilters = reactive({
  applicationNo: undefined as string | undefined,
  status: undefined as string | undefined
})

// Tab 2: 待我审批
const pendingApprovalLoading = ref(false)
const pendingApprovalData = ref<SecAccessApplicationVO[]>([])
const pendingApprovalPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const pendingFilters = reactive({
  applicationNo: undefined as string | undefined
})

// Tab 3: 审计日志
const auditLogLoading = ref(false)
const auditLogData = ref<SecAccessLogVO[]>([])
const auditLogPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const auditFilters = reactive({
  operationType: undefined as string | undefined,
  operatorName: undefined as string | undefined,
  dateRange: null as [Dayjs, Dayjs] | null
})

// 申请弹窗
const applyModalVisible = ref(false)
const applyFormRef = ref<FormInstance>()
const submitLoading = ref(false)
const applyFormData = reactive<SecAccessApplicationSaveDTO>({
  targetDsId: undefined,
  targetTable: undefined,
  targetColumns: undefined,
  accessReason: undefined,
  durationHours: undefined
})
const applyFormRules = {
  targetDsId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  targetTable: [{ required: true, message: '请选择目标表', trigger: 'change' }],
  targetColumns: [{ required: true, message: '请选择访问字段', trigger: 'change', type: 'array', min: 1 }],
  durationHours: [{ required: true, message: '请选择访问时长', trigger: 'change' }],
  accessReason: [{ required: true, message: '请输入申请原因', trigger: 'blur' }]
}

// 数据源、表的级联选择
const dataSourceLoading = ref(false)
const dataSourceList = ref<DataSource[]>([])
const tableLoading = ref(false)
const tableList = ref<string[]>([])
const columnLoading = ref(false)
const columnList = ref<TableColumn[]>([])

// 敏感等级提示
const sensitivityLevelList = ref<Array<{ levelCode: string; levelName: string; levelValue: number }>>([])

// 审批弹窗
const approvalModalVisible = ref(false)
const approvalFormRef = ref<FormInstance>()
const currentApprovalRecord = ref<SecAccessApplicationVO | null>(null)
const approvalFormData = reactive({
  approvalComment: ''
})
const approvalFormRules = {}

// 详情弹窗
const detailModalVisible = ref(false)
const currentDetailRecord = ref<SecAccessApplicationVO | null>(null)
const auditHistory = ref<SecAccessAuditVO[]>([])

// ==================== 表格列定义 ====================

const myApplicationColumns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo', width: 150, fixed: 'left' },
  { title: '目标数据源', dataIndex: 'targetDsName', key: 'targetDsName', width: 140 },
  { title: '目标表', dataIndex: 'targetTable', key: 'targetTable', width: 140 },
  { title: '访问字段', key: 'targetColumns', width: 150 },
  { title: '访问时长', key: 'durationHours', width: 100 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 160 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const pendingApprovalColumns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo', width: 150, fixed: 'left' },
  { title: '申请人', key: 'applicant', width: 140 },
  { title: '目标表', dataIndex: 'targetTable', key: 'targetTable', width: 120 },
  { title: '访问字段', key: 'targetColumns', width: 150 },
  { title: '敏感等级', key: 'sensitivityLevel', width: 160 },
  { title: '申请原因', key: 'accessReason', width: 180 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 160 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]

const auditLogColumns = [
  { title: '时间', dataIndex: 'operationTime', key: 'operationTime', width: 170 },
  { title: '操作类型', key: 'operationType', width: 90 },
  { title: '操作人', key: 'operator', width: 140 },
  { title: '操作内容', key: 'operationContent', width: 250 },
  { title: '目标表', dataIndex: 'tableName', key: 'tableName', width: 140 },
  { title: '访问字段', dataIndex: 'columnName', key: 'columnName', width: 120 },
  { title: 'IP地址', key: 'ipAddress', width: 130 },
  { title: '状态', key: 'status', width: 90 }
]

// ==================== 计算属性 ====================

const sensitivityWarnings = computed(() => {
  const warnings: string[] = []
  if (!applyFormData.targetColumns || applyFormData.targetColumns.length === 0) {
    return warnings
  }

  const selectedColumns = columnList.value.filter(col =>
    applyFormData.targetColumns?.includes(col.columnName)
  )

  for (const col of selectedColumns) {
    if (col.sensitivityLevel) {
      const level = col.sensitivityLevel.toUpperCase()
      if (level === 'L4' || level === 'L3') {
        const color = SENSITIVITY_COLORS[level] || '#FAAD14'
        const approver = level === 'L4' ? '安全管理员' : '部门负责人'
        warnings.push(
          `<span style="color: ${color}">⚠️ 访问字段中包含 ${level} ${level === 'L4' ? '机密' : '敏感'}字段（${col.columnName}），需${approver}审批</span>`
        )
      }
    }
  }

  return warnings
})

// ==================== 方法定义 ====================

// Tab 切换
function handleTabChange(key: string) {
  switch (key) {
    case 'myApplication':
      if (myApplicationData.value.length === 0) {
        loadMyApplicationData()
      }
      break
    case 'pendingApproval':
      if (pendingApprovalData.value.length === 0) {
        loadPendingApprovalData()
      }
      break
    case 'auditLog':
      if (auditLogData.value.length === 0) {
        loadAuditLogData()
      }
      break
  }
}

// 加载我的申请数据
async function loadMyApplicationData() {
  myApplicationLoading.value = true
  try {
    const params = {
      pageNum: myApplicationPagination.current,
      pageSize: myApplicationPagination.pageSize,
      applicationNo: myAppFilters.applicationNo,
      status: myAppFilters.status
    }
    const res = await pageAccessApply(params)
    if (res.code === 200 && res.data) {
      myApplicationData.value = res.data.records || []
      myApplicationPagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载我的申请失败:', error)
  } finally {
    myApplicationLoading.value = false
  }
}

// 加载待审批数据
async function loadPendingApprovalData() {
  pendingApprovalLoading.value = true
  try {
    const params = {
      pageNum: pendingApprovalPagination.current,
      pageSize: pendingApprovalPagination.pageSize,
      applicationNo: pendingFilters.applicationNo
    }
    const res = await pagePendingApproval(params)
    if (res.code === 200 && res.data) {
      pendingApprovalData.value = res.data.records || []
      pendingApprovalPagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载待审批数据失败:', error)
  } finally {
    pendingApprovalLoading.value = false
  }
}

// 加载审计日志数据
async function loadAuditLogData() {
  auditLogLoading.value = true
  try {
    const params: any = {
      pageNum: auditLogPagination.current,
      pageSize: auditLogPagination.pageSize,
      operationType: auditFilters.operationType,
      operatorName: auditFilters.operatorName
    }
    if (auditFilters.dateRange && auditFilters.dateRange.length === 2) {
      params.startDate = auditFilters.dateRange[0].format('YYYY-MM-DD')
      params.endDate = auditFilters.dateRange[1].format('YYYY-MM-DD')
    }
    const res = await pageAccessLog(params)
    if (res.code === 200 && res.data) {
      auditLogData.value = res.data.records || []
      auditLogPagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载审计日志失败:', error)
  } finally {
    auditLogLoading.value = false
  }
}

// 搜索和重置
function handleSearchMyApplication() {
  myApplicationPagination.current = 1
  loadMyApplicationData()
}

function handleResetMyApplication() {
  myAppFilters.applicationNo = undefined
  myAppFilters.status = undefined
  handleSearchMyApplication()
}

function handleSearchPending() {
  pendingApprovalPagination.current = 1
  loadPendingApprovalData()
}

function handleResetPending() {
  pendingFilters.applicationNo = undefined
  handleSearchPending()
}

function handleSearchAudit() {
  auditLogPagination.current = 1
  loadAuditLogData()
}

function handleResetAudit() {
  auditFilters.operationType = undefined
  auditFilters.operatorName = undefined
  auditFilters.dateRange = null
  handleSearchAudit()
}

// 表格分页变化
function handleMyApplicationTableChange(pag: TablePaginationConfig) {
  myApplicationPagination.current = pag.current || 1
  myApplicationPagination.pageSize = pag.pageSize || 10
  loadMyApplicationData()
}

function handlePendingTableChange(pag: TablePaginationConfig) {
  pendingApprovalPagination.current = pag.current || 1
  pendingApprovalPagination.pageSize = pag.pageSize || 10
  loadPendingApprovalData()
}

function handleAuditTableChange(pag: TablePaginationConfig) {
  auditLogPagination.current = pag.current || 1
  auditLogPagination.pageSize = pag.pageSize || 10
  loadAuditLogData()
}

// 申请访问
async function handleApplyAccess() {
  applyModalVisible.value = true
  // 加载数据源列表
  if (dataSourceList.value.length === 0) {
    await loadDataSources()
  }
  // 加载敏感等级列表
  if (sensitivityLevelList.value.length === 0) {
    await loadSensitivityLevels()
  }
}

async function loadDataSources() {
  dataSourceLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.code === 200 && res.data) {
      dataSourceList.value = res.data
    }
  } catch (error) {
    console.error('加载数据源失败:', error)
  } finally {
    dataSourceLoading.value = false
  }
}

async function loadSensitivityLevels() {
  try {
    const res = await listLevel()
    if (res.code === 200 && res.data) {
      sensitivityLevelList.value = res.data
    }
  } catch (error) {
    console.error('加载敏感等级失败:', error)
  }
}

async function handleDataSourceChange(dsId: number) {
  applyFormData.targetTable = undefined
  applyFormData.targetColumns = undefined
  tableList.value = []
  columnList.value = []

  if (!dsId) return

  tableLoading.value = true
  try {
    const res = await dataSourceApi.getTables(dsId)
    if (res.code === 200 && res.data) {
      tableList.value = res.data
    }
  } catch (error) {
    console.error('加载表列表失败:', error)
  } finally {
    tableLoading.value = false
  }
}

async function handleTableChange(tableName: string) {
  applyFormData.targetColumns = undefined
  columnList.value = []

  if (!tableName || !applyFormData.targetDsId) return

  columnLoading.value = true
  try {
    const res = await dataSourceApi.getTableColumns(applyFormData.targetDsId!, tableName)
    if (res.code === 200 && res.data) {
      columnList.value = res.data
    }
  } catch (error) {
    console.error('加载字段列表失败:', error)
  } finally {
    columnLoading.value = false
  }
}

async function handleSubmitApply() {
  try {
    await applyFormRef.value?.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const dto: SecAccessApplicationSaveDTO = {
      targetDsId: applyFormData.targetDsId,
      targetTable: applyFormData.targetTable,
      targetColumns: Array.isArray(applyFormData.targetColumns)
        ? applyFormData.targetColumns.join(',')
        : applyFormData.targetColumns,
      accessReason: applyFormData.accessReason,
      durationHours: applyFormData.durationHours
    }
    const res = await submitAccessApplication(dto)
    if (res.code === 200) {
      message.success('申请提交成功')
      applyModalVisible.value = false
      resetApplyForm()
      loadMyApplicationData()
    } else {
      message.error(res.message || '提交失败')
    }
  } catch (error: any) {
    message.error(error?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

function resetApplyForm() {
  applyFormRef.value?.resetFields()
  applyFormData.targetDsId = undefined
  applyFormData.targetTable = undefined
  applyFormData.targetColumns = undefined
  applyFormData.accessReason = undefined
  applyFormData.durationHours = undefined
  tableList.value = []
  columnList.value = []
}

function handleCancelApply() {
  resetApplyForm()
}

// 撤销申请
async function handleCancelApplication(record: SecAccessApplicationVO) {
  const confirmed = await confirmModal(`确定要撤销申请 ${record.applicationNo} 吗？`)
  if (!confirmed) return

  try {
    const res = await cancelAccessApplication(record.id!, 1, '当前用户')
    if (res.code === 200) {
      message.success('撤销成功')
      loadMyApplicationData()
    } else {
      message.error(res.message || '撤销失败')
    }
  } catch (error: any) {
    message.error(error?.message || '撤销失败')
  }
}

// 查看详情
async function handleViewDetail(record: SecAccessApplicationVO) {
  try {
    const res = await getAccessApplyById(record.id!)
    if (res.code === 200 && res.data) {
      currentDetailRecord.value = res.data
      // 加载审计历史
      const historyRes = await listAccessAuditHistory(record.id!)
      if (historyRes.code === 200 && historyRes.data) {
        auditHistory.value = historyRes.data
      } else {
        auditHistory.value = []
      }
      detailModalVisible.value = true
    }
  } catch (error: any) {
    message.error(error?.message || '加载详情失败')
  }
}

// 审批
function handleApprove(record: SecAccessApplicationVO) {
  currentApprovalRecord.value = record
  approvalFormData.approvalComment = ''
  approvalModalVisible.value = true
}

function handleReject(record: SecAccessApplicationVO) {
  currentApprovalRecord.value = record
  approvalFormData.approvalComment = ''
  approvalModalVisible.value = true
}

async function handleApproveDirect() {
  if (!currentApprovalRecord.value) return
  submitLoading.value = true
  try {
    const dto: SecAccessApprovalDTO = {
      id: currentApprovalRecord.value.id,
      status: 'APPROVED',
      approvalComment: approvalFormData.approvalComment
    }
    const res = await approveAccessApplication(dto)
    if (res.code === 200) {
      message.success('审批通过')
      approvalModalVisible.value = false
      loadPendingApprovalData()
    } else {
      message.error(res.message || '审批失败')
    }
  } catch (error: any) {
    message.error(error?.message || '审批失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleRejectDirect() {
  if (!currentApprovalRecord.value) return
  submitLoading.value = true
  try {
    const dto: SecAccessApprovalDTO = {
      id: currentApprovalRecord.value.id,
      status: 'REJECTED',
      approvalComment: approvalFormData.approvalComment
    }
    const res = await approveAccessApplication(dto)
    if (res.code === 200) {
      message.success('已驳回')
      approvalModalVisible.value = false
      loadPendingApprovalData()
    } else {
      message.error(res.message || '审批失败')
    }
  } catch (error: any) {
    message.error(error?.message || '审批失败')
  } finally {
    submitLoading.value = false
  }
}

function handleCancelApproval() {
  approvalModalVisible.value = false
  currentApprovalRecord.value = null
}

// 导出审计日志
async function handleExportAudit() {
  try {
    message.info('正在导出...')
    // TODO: 调用导出接口
  } catch (error: any) {
    message.error(error?.message || '导出失败')
  }
}

// ==================== 工具函数 ====================

function getStatusColor(status?: string): string {
  return status ? STATUS_COLORS[status.toUpperCase()] || '#8C8C8C' : '#8C8C8C'
}

function getOperationTypeColor(type?: string): string {
  return type ? OPERATION_TYPE_COLORS[type.toUpperCase()] || '#8C8C8C' : '#8C8C8C'
}

function getSensitivityTagColor(level?: string): string {
  return level ? SENSITIVITY_COLORS[level.toUpperCase()] || '#8C8C8C' : '#8C8C8C'
}

function getSensitivityLevelDisplay(levelLabel?: string): string {
  if (!levelLabel) return '-'
  const level = levelLabel.toUpperCase()
  const color = SENSITIVITY_COLORS[level] || SENSITIVITY_COLORS.L1
  return `<span style="display: inline-flex; align-items: center; gap: 4px;">
    <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: ${color};"></span>
    ${levelLabel}
  </span>`
}

function getAuditTimelineColor(operationType?: string): string {
  return operationType ? OPERATION_TYPE_COLORS[operationType.toUpperCase()] || 'blue' : 'blue'
}

function getOperationText(operationType?: string): string {
  const map: Record<string, string> = {
    ACCESS: '执行了访问操作',
    APPLY: '提交了访问申请',
    APPROVE: '审批通过',
    REJECT: '驳回申请',
    CANCEL: '撤销了申请'
  }
  return operationType ? map[operationType.toUpperCase()] || '执行了操作' : '执行了操作'
}

function formatDuration(hours?: number): string {
  if (!hours) return '-'
  if (hours <= 24) return `${hours}小时`
  if (hours <= 168) return `${hours}小时（一周）`
  if (hours <= 720) return `${hours}小时（一月）`
  return `${hours}小时`
}

function truncateText(text?: string, maxLength: number = 20): string {
  if (!text) return ''
  return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadMyApplicationData()
})
</script>

<style scoped>
.access-approval-page {
  padding: 0;
}

.page-card {
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #722ED110 0%, #9254DE10 100%);
  border: 1px solid #722ED120;
  border-radius: 12px;
  padding: 20px 24px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.header-right { display: flex; align-items: center; gap: 8px; }

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

.filter-bar {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.mt-4 {
  margin-top: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}

.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-danger {
  color: #f5222d;
}

.text-muted {
  color: #8c8c8c;
  font-size: 12px;
}

.column-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sensitivity-tips {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 4px;
}

.sensitivity-tip-item {
  padding: 4px 0;
  font-size: 13px;
  line-height: 1.6;
}

.audit-history {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.audit-history h4 {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
}

:deep(.ant-descriptions-item-label) {
  width: 100px;
  background: #fafafa;
}

:deep(.ant-modal-header) {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-modal-body) {
  padding: 20px 24px;
}

:deep(.ant-modal-footer) {
  padding: 12px 24px;
  border-top: 1px solid #f0f0f0;
}

:deep(.ant-card-head) {
  min-height: 56px;
  padding: 0 24px;
}

:deep(.ant-card-body) {
  padding: 0 24px 24px;
}

:deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-form-item-label > label) {
  font-weight: 500;
}

:deep(.ant-tag) {
  border-radius: 4px;
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
}

:deep(.ant-pagination-total-text) {
  color: #8c8c8c;
}
</style>
