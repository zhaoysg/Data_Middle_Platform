<template>
  <div class="security-audit">
    <a-card>
      <template #title>敏感数据访问审计</template>
      <template #extra>
        <a-space>
          <a-range-picker v-model:value="dateRange" @change="handleSearch" />
          <a-select v-model:value="filterType" placeholder="操作类型" style="width: 120px" allow-clear @change="handleSearch">
            <a-select-option value="ACCESS">访问</a-select-option>
            <a-select-option value="APPLY">申请</a-select-option>
            <a-select-option value="APPROVE">审批</a-select-option>
          </a-select>
          <a-button @click="handleExport">
            <template #icon><DownloadOutlined /></template>
            导出
          </a-button>
        </a-space>
      </template>

      <!-- 统计概览 -->
      <a-row :gutter="16" class="mb-4">
        <a-col :span="6">
          <a-statistic title="访问总数" :value="statistics.totalAccess" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="今日访问" :value="statistics.todayAccess" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="申请总数" :value="statistics.totalApply" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="待审批" :value="statistics.pending" :value-style="{ color: '#faad14' }" />
        </a-col>
      </a-row>

      <!-- 审计日志 -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'operationType'">
            <a-tag :color="getTypeColor(record.operationType)">
              {{ getTypeText(record.operationType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'ipAddress'">
            <a-tag>{{ record.ipAddress }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'

interface AuditRecord {
  id: number
  applicationNo: string
  operatorName: string
  operatorDept: string
  operationType: string
  operationContent: string
  targetTable: string
  targetColumn: string
  ipAddress: string
  operationTime: string
  status: string
}

const loading = ref(false)
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const filterType = ref<string>()

const statistics = reactive({
  totalAccess: 0,
  todayAccess: 0,
  totalApply: 0,
  pending: 0
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '时间', dataIndex: 'operationTime', key: 'operationTime', width: 180 },
  { title: '操作类型', key: 'operationType', width: 100 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName' },
  { title: '部门', dataIndex: 'operatorDept', key: 'operatorDept' },
  { title: '操作内容', dataIndex: 'operationContent', key: 'operationContent', ellipsis: true },
  { title: '目标表', dataIndex: 'targetTable', key: 'targetTable' },
  { title: '访问字段', dataIndex: 'targetColumn', key: 'targetColumn', ellipsis: true },
  { title: 'IP地址', key: 'ipAddress', width: 140 },
  { title: '状态', key: 'status', width: 80 }
]

const dataSource = ref<AuditRecord[]>([])

function getTypeColor(type: string) {
  const map: Record<string, string> = { ACCESS: 'blue', APPLY: 'orange', APPROVE: 'green', REJECT: 'red' }
  return map[type] || 'default'
}

function getTypeText(type: string) {
  const map: Record<string, string> = { ACCESS: '访问', APPLY: '申请', APPROVE: '审批', REJECT: '驳回' }
  return map[type] || type
}

function getStatusColor(status: string) {
  const map: Record<string, string> = { SUCCESS: 'green', FAILED: 'red', PENDING: 'orange' }
  return map[status] || 'default'
}

function getStatusText(status: string) {
  const map: Record<string, string> = { SUCCESS: '成功', FAILED: '失败', PENDING: '待处理' }
  return map[status] || status
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function handleExport() {
  message.success('审计日志导出中...')
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    dataSource.value = [
      { id: 1, applicationNo: 'APP20240115001', operatorName: '李四', operatorDept: '数据质量组', operationType: 'APPROVE', operationContent: '审批通过敏感字段访问申请', targetTable: 'dim_users', targetColumn: 'user_phone,email,id_card', ipAddress: '192.168.1.50', operationTime: '2024-01-15 10:30:00', status: 'SUCCESS' },
      { id: 2, applicationNo: 'APP20240115001', operatorName: '李四', operatorDept: '数据质量组', operationType: 'ACCESS', operationContent: '查询敏感字段用于数据质量检测', targetTable: 'dim_users', targetColumn: 'user_phone,email,id_card', ipAddress: '192.168.1.100', operationTime: '2024-01-15 11:00:00', status: 'SUCCESS' },
      { id: 3, applicationNo: 'APP20240116001', operatorName: '王五', operatorDept: '数据开发组', operationType: 'APPLY', operationContent: '提交敏感字段访问申请', targetTable: 'fact_orders', targetColumn: 'order_amount', ipAddress: '192.168.1.101', operationTime: '2024-01-16 14:00:00', status: 'PENDING' }
    ]
    statistics.totalAccess = 1256
    statistics.todayAccess = 23
    statistics.totalApply = 89
    statistics.pending = 5
    pagination.total = 3
    loading.value = false
  }, 500)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mb-4 {
  margin-bottom: 24px;
}
</style>
