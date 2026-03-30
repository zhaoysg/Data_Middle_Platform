<template>
  <div class="security-access">
    <a-card>
      <template #title>敏感字段访问审批</template>
      <template #extra>
        <a-button type="primary" @click="handleApply">
          <template #icon><PlusOutlined /></template>
          申请访问
        </a-button>
      </template>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="application" tab="我的申请">
          <a-table
            :columns="applicationColumns"
            :data-source="applicationData"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'duration'">
                {{ record.durationHours }}小时
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="handleView(record)">详情</a>
                  <a v-if="record.status === 'PENDING'" @click="handleCancel(record)">撤销</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="approval" tab="待我审批">
          <a-table
            :columns="approvalColumns"
            :data-source="approvalData"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="orange">待审批</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="handleApprove(record)">通过</a>
                  <a @click="handleReject(record)">驳回</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 申请弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      title="申请敏感字段访问"
      width="600"
      @ok="handleSubmit"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="目标数据源" name="dsId">
          <a-select v-model:value="formData.dsId" placeholder="请选择数据源">
            <a-select-option :value="1">MySQL数仓(ODS)</a-select-option>
            <a-select-option :value="2">MySQL数仓(DWD)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="目标表" name="table">
          <a-input v-model:value="formData.table" placeholder="请输入表名" />
        </a-form-item>
        <a-form-item label="访问字段" name="columns">
          <a-select v-model:value="formData.columns" mode="tags" placeholder="选择或输入字段">
            <a-select-option value="user_phone">user_phone</a-select-option>
            <a-select-option value="id_card">id_card</a-select-option>
            <a-select-option value="email">email</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="访问时长" name="duration">
          <a-select v-model:value="formData.duration" placeholder="选择时长">
            <a-select-option value="24">24小时</a-select-option>
            <a-select-option value="168">一周</a-select-option>
            <a-select-option value="720">一个月</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="申请原因" name="reason">
          <a-textarea v-model:value="formData.reason" :rows="3" placeholder="请输入申请原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { confirmModal } from '@/utils/modal'

interface ApplicationRecord {
  id: number
  applicationNo: string
  tableName: string
  columns: string
  durationHours: number
  applyTime: string
  status: string
  approver?: string
  approvalTime?: string
}

interface ApprovalRecord {
  id: number
  applicationNo: string
  applicantName: string
  tableName: string
  columns: string
  applyTime: string
}

const loading = ref(false)
const modalVisible = ref(false)
const activeTab = ref('application')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const applicationColumns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo' },
  { title: '目标表', dataIndex: 'tableName', key: 'tableName' },
  { title: '访问字段', dataIndex: 'columns', key: 'columns' },
  { title: '访问时长', key: 'duration' },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 120 }
]

const approvalColumns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo' },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName' },
  { title: '目标表', dataIndex: 'tableName', key: 'tableName' },
  { title: '访问字段', dataIndex: 'columns', key: 'columns' },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 120 }
]

const applicationData = ref<ApplicationRecord[]>([])
const approvalData = ref<ApprovalRecord[]>([])

const formData = reactive({
  dsId: null as number | null,
  table: '',
  columns: [] as string[],
  duration: '',
  reason: ''
})

function getStatusColor(status: string) {
  const map: Record<string, string> = { APPROVED: 'green', PENDING: 'orange', REJECTED: 'red', EXPIRED: 'gray' }
  return map[status] || 'default'
}

function getStatusText(status: string) {
  const map: Record<string, string> = { APPROVED: '已通过', PENDING: '审批中', REJECTED: '已驳回', EXPIRED: '已过期' }
  return map[status] || status
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadApplicationData()
}

function handleApply() {
  modalVisible.value = true
}

function handleSubmit() {
  if (!formData.table || !formData.columns.length || !formData.reason) {
    message.warning('请填写完整信息')
    return
  }
  message.success('访问申请已提交，请等待审批')
  modalVisible.value = false
  loadApplicationData()
}

function handleView(record: ApplicationRecord) {
  message.info(`查看申请详情: ${record.applicationNo}`)
}

async function handleCancel(record: ApplicationRecord) {
  const confirmed = await confirmModal(`确定撤销申请 ${record.applicationNo} 吗?`)
  if (confirmed) {
    message.success('申请已撤销')
    loadApplicationData()
  }
}

async function handleApprove(record: ApprovalRecord) {
  const confirmed = await confirmModal(`确定通过申请 ${record.applicationNo} 吗?`)
  if (confirmed) {
    message.success('已通过')
    loadApprovalData()
  }
}

async function handleReject(record: ApprovalRecord) {
  const confirmed = await confirmModal(`确定驳回申请 ${record.applicationNo} 吗?`)
  if (confirmed) {
    message.success('已驳回')
    loadApprovalData()
  }
}

function loadApplicationData() {
  loading.value = true
  setTimeout(() => {
    applicationData.value = [
      { id: 1, applicationNo: 'APP20240115001', tableName: 'dim_users', columns: 'user_phone,id_card', durationHours: 168, applyTime: '2024-01-15 09:00:00', status: 'APPROVED', approver: 'admin', approvalTime: '2024-01-15 10:30:00' },
      { id: 2, applicationNo: 'APP20240116001', tableName: 'fact_orders', columns: 'order_amount', durationHours: 24, applyTime: '2024-01-16 14:00:00', status: 'PENDING' }
    ]
    pagination.total = 2
    loading.value = false
  }, 500)
}

function loadApprovalData() {
  approvalData.value = [
    { id: 1, applicationNo: 'APP20240116001', applicantName: '王五', tableName: 'fact_orders', columns: 'order_amount', applyTime: '2024-01-16 14:00:00' }
  ]
}

onMounted(() => {
  loadApplicationData()
  loadApprovalData()
})
</script>
