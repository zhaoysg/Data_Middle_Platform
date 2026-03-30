<template>
  <div class="security-sensitivity">
    <a-card>
      <template #title>敏感字段识别</template>
      <template #extra>
        <a-space>
          <a-select v-model:value="filterLevel" placeholder="选择敏感等级" style="width: 150px" allow-clear @change="handleSearch">
            <a-select-option value="HIGHLY_SENSITIVE">高敏感</a-select-option>
            <a-select-option value="SENSITIVE">敏感</a-select-option>
            <a-select-option value="NORMAL">一般</a-select-option>
          </a-select>
          <a-button type="primary" @click="handleAutoDetect">
            <template #icon><ScanOutlined /></template>
            智能识别
          </a-button>
        </a-space>
      </template>

      <!-- 识别规则 -->
      <a-tabs v-model:activeKey="activeTab" class="mb-4">
        <a-tab-pane key="columns" tab="字段识别">
          <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'matchRule'">
                <a-tag>{{ record.matchRule }}</a-tag>
              </template>
              <template v-else-if="column.key === 'confidence'">
                <a-progress :percent="record.confidence" size="small" :status="getConfidenceStatus(record.confidence)" />
              </template>
              <template v-else-if="column.key === 'reviewStatus'">
                <a-tag :color="getReviewColor(record.reviewStatus)">
                  {{ getReviewText(record.reviewStatus) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click="handleApprove(record)">审核</a>
                  <a @click="handleRule(record)">规则</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="rules" tab="识别规则">
          <a-table :columns="ruleColumns" :data-source="ruleData" :pagination="false" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ScanOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

interface SensitivityRecord {
  id: number
  tableName: string
  columnName: string
  columnComment: string
  dataType: string
  matchRule: string
  confidence: number
  sensitivityLevel: string
  reviewStatus: string
  scanTime: string
}

const loading = ref(false)
const filterLevel = ref<string>()
const activeTab = ref('columns')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '表名', dataIndex: 'tableName', key: 'tableName' },
  { title: '字段名', dataIndex: 'columnName', key: 'columnName' },
  { title: '字段注释', dataIndex: 'columnComment', key: 'columnComment' },
  { title: '匹配规则', key: 'matchRule' },
  { title: '置信度', key: 'confidence', width: 150 },
  { title: '审核状态', key: 'reviewStatus' },
  { title: '操作', key: 'action', width: 120 }
]

const ruleColumns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName' },
  { title: '匹配模式', dataIndex: 'pattern', key: 'pattern' },
  { title: '敏感等级', dataIndex: 'level', key: 'level' },
  { title: '启用状态', dataIndex: 'enabled', key: 'enabled' }
]

const dataSource = ref<SensitivityRecord[]>([])
const ruleData = ref([
  { ruleName: '身份证号规则', pattern: '*id_card*, *身份证*', level: '高敏感', enabled: true },
  { ruleName: '手机号规则', pattern: '*phone*, *手机*', level: '敏感', enabled: true },
  { ruleName: '邮箱规则', pattern: '*email*, *邮箱*', level: '敏感', enabled: true }
])

function getConfidenceStatus(confidence: number) {
  if (confidence >= 90) return 'success'
  if (confidence >= 70) return 'active'
  return 'exception'
}

function getReviewColor(status: string) {
  const map: Record<string, string> = { APPROVED: 'green', PENDING: 'orange', REJECTED: 'red' }
  return map[status] || 'default'
}

function getReviewText(status: string) {
  const map: Record<string, string> = { APPROVED: '已审核', PENDING: '待审核', REJECTED: '已驳回' }
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

function handleAutoDetect() {
  message.success('智能识别任务已启动...')
}

function handleApprove(record: SensitivityRecord) {
  message.info(`审核字段: ${record.columnName}`)
}

function handleRule(record: SensitivityRecord) {
  message.info(`查看规则: ${record.matchRule}`)
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    dataSource.value = [
      { id: 1, tableName: 'dim_users', columnName: 'id_card', columnComment: '身份证号', dataType: 'VARCHAR', matchRule: '字段名包含id_card', confidence: 99, sensitivityLevel: 'HIGHLY_SENSITIVE', reviewStatus: 'APPROVED', scanTime: '2024-01-15' },
      { id: 2, tableName: 'dim_users', columnName: 'user_phone', columnComment: '手机号', dataType: 'VARCHAR', matchRule: '注释包含手机', confidence: 95, sensitivityLevel: 'SENSITIVE', reviewStatus: 'PENDING', scanTime: '2024-01-15' },
      { id: 3, tableName: 'dim_users', columnName: 'email', columnComment: '邮箱地址', dataType: 'VARCHAR', matchRule: '字段名包含email', confidence: 92, sensitivityLevel: 'SENSITIVE', reviewStatus: 'APPROVED', scanTime: '2024-01-15' }
    ]
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
