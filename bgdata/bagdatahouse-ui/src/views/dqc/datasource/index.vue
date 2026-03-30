<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#grad6)"/>
            <ellipse cx="12" cy="6" rx="8" ry="3" stroke="white" stroke-width="1.5"/>
            <path d="M4 6v12c0 1.66 3.58 3 8 3s8-1.34 8-3V6" stroke="white" stroke-width="1.5"/>
            <path d="M4 12c0 1.66 3.58 3 8 3s8-1.34 8-3" stroke="white" stroke-width="1.5"/>
            <defs>
              <linearGradient id="grad6" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#531DAB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据源管理</h1>
          <p class="page-subtitle">管理数据库连接配置和数据源信息</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建数据源
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[20, 20]" style="margin-bottom: 20px">
      <a-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-icon" style="background: #E6F7FF">
            <DatabaseOutlined style="color: #1677FF; font-size: 24px" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">数据源总数</div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-icon" style="background: #F6FFED">
            <CheckCircleOutlined style="color: #52C41A; font-size: 24px" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.enabled }}</div>
            <div class="stat-label">已启用</div>
          </div>
        </div>
      </a-col>
      <a-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-icon" style="background: #FFF7E6">
            <CloseCircleOutlined style="color: #FA8C16; font-size: 24px" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.disabled }}</div>
            <div class="stat-label">已禁用</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选区 -->
    <div class="filter-card">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="数据源名称">
          <a-input v-model:value="queryParams.dsName" placeholder="请输入数据源名称" style="width: 180px" allow-clear />
        </a-form-item>
        <a-form-item label="数据源类型">
          <a-select v-model:value="queryParams.dsType" placeholder="请选择" style="width: 150px" allow-clear>
            <a-select-option value="MYSQL">MySQL</a-select-option>
            <a-select-option value="POSTGRESQL">PostgreSQL</a-select-option>
            <a-select-option value="ORACLE">Oracle</a-select-option>
            <a-select-option value="SQLSERVER">SQL Server</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="queryParams.status" placeholder="请选择" style="width: 120px" allow-clear>
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 表格区 -->
    <div class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record: any) => record.id"
        @change="handleTableChange"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dsType'">
            <a-tag :color="getDsTypeColor(record.dsType)">
              {{ getDsTypeName(record.dsType) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '启用' : '禁用'" />
          </template>

          <template v-if="column.key === 'connection'">
            <span class="connection-info">
              <DatabaseOutlined /> {{ record.host }}:{{ record.port }}
              <span v-if="record.databaseName"> / {{ record.databaseName }}</span>
            </span>
          </template>

          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button v-if="record.status === 1" type="link" size="small" danger @click="handleDisable(record)">
                <template #icon><CloseCircleOutlined /></template>
                禁用
              </a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleEnable(record)">
                <template #icon><CheckCircleOutlined /></template>
                启用
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑数据源' : '新建数据源'"
      :width="600"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      :confirm-loading="modalLoading"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="数据源名称" name="dsName" :rules="[{ required: true, message: '请输入数据源名称' }]">
          <a-input v-model:value="formData.dsName" placeholder="请输入数据源名称" />
        </a-form-item>
        <a-form-item label="数据源编码" name="dsCode" extra="唯一标识，不填则自动生成">
          <a-input v-model:value="formData.dsCode" placeholder="唯一标识，不填则自动生成" />
        </a-form-item>
        <a-form-item label="数据源类型" name="dsType" :rules="[{ required: true, message: '请选择数据源类型' }]">
          <a-select v-model:value="formData.dsType" placeholder="请选择数据源类型">
            <a-select-option value="MYSQL">MySQL</a-select-option>
            <a-select-option value="POSTGRESQL">PostgreSQL</a-select-option>
            <a-select-option value="ORACLE">Oracle</a-select-option>
            <a-select-option value="SQLSERVER">SQL Server</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="主机地址" name="host" :rules="[{ required: true, message: '请输入主机地址' }]">
          <a-input v-model:value="formData.host" placeholder="请输入主机地址" />
        </a-form-item>
        <a-form-item label="端口" name="port" :rules="[{ required: true, message: '请输入端口' }]">
          <a-input-number v-model:value="formData.port" placeholder="请输入端口" style="width: 100%" />
        </a-form-item>
        <a-form-item label="数据库名" name="databaseName" :rules="[{ required: true, message: '请输入数据库名称' }]">
          <a-input v-model:value="formData.databaseName" placeholder="请输入数据库名称" />
        </a-form-item>
        <a-form-item
          v-if="formData.dsType === 'POSTGRESQL'"
          label="Schema名称"
          name="schemaName"
          :rules="[]"
          extra="PostgreSQL 的 Schema，可留空（默认 public）"
        >
          <a-input v-model:value="formData.schemaName" placeholder="请输入 Schema 名称（可选）" />
        </a-form-item>
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="formData.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="formData.password" placeholder="请输入密码（不修改请留空）" />
        </a-form-item>
        <a-form-item label="数据层" name="dataLayer">
          <a-select v-model:value="formData.dataLayer" placeholder="请选择数据层" allow-clear>
            <a-select-option value="ODS">ODS - 原始数据层</a-select-option>
            <a-select-option value="DWD">DWD - 明细数据层</a-select-option>
            <a-select-option value="DWS">DWS - 汇总数据层</a-select-option>
            <a-select-option value="ADS">ADS - 应用数据层</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入描述信息" :rows="3" />
        </a-form-item>
        <a-form-item :wrapper-col="{ offset: 6 }">
          <a-space>
            <a-button
              type="default"
              html-type="button"
              :loading="testLoading"
              @click.stop="handleTestConnection"
              :disabled="!formData.host || !formData.port || !formData.dsType"
            >
              <template #icon><ApiOutlined /></template>
              测试连接
            </a-button>
            <span v-if="testResult !== null" :style="{ color: testResult ? '#52C41A' : '#FF4D4F', fontSize: '13px' }">
              {{ testResult ? '✓ 连接成功' : '✗ 连接失败' }}
            </span>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  Button,
  Form,
  FormItem,
  Input,
  InputNumber,
  InputPassword,
  Select,
  SelectOption,
  Table,
  Modal,
  Space,
  Tag,
  Badge,
  Row,
  Col,
  Divider,
  message,
  Popconfirm
} from 'ant-design-vue'
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  DatabaseOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ApiOutlined
} from '@ant-design/icons-vue'
import { dataSourceApi } from '@/api/dqc'

const loading = ref(false)
const tableData = ref<any[]>([])
const stats = reactive({
  total: 0,
  enabled: 0,
  disabled: 0
})

const queryParams = reactive({
  dsName: '',
  dsType: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '数据源名称', dataIndex: 'dsName', key: 'dsName', width: 180 },
  { title: '类型', key: 'dsType', width: 120 },
  { title: '连接信息', key: 'connection', width: 200 },
  { title: '数据层', dataIndex: 'dataLayer', key: 'dataLayer', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 150, fixed: 'right' }
]

const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref()
const testLoading = ref(false)
const testResult = ref<boolean | null>(null)

const formData = reactive({
  id: undefined as number | undefined,
  dsCode: '',
  dsName: '',
  dsType: '',
  host: '',
  port: 3306,
  databaseName: '',
  schemaName: '',
  username: '',
  password: '',
  dataLayer: '',
  remark: ''
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await dataSourceApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      dsName: queryParams.dsName || undefined,
      dsType: queryParams.dsType || undefined,
      status: queryParams.status
    })
    tableData.value = res.data?.records || res.data || []
    pagination.total = res.data?.total || tableData.value.length
    
    // 从后端获取统计数据
    try {
      const statsRes: any = await dataSourceApi.getStatistics()
      if (statsRes.data) {
        stats.total = statsRes.data.totalCount || 0
        stats.enabled = statsRes.data.enabledCount || 0
        stats.disabled = (statsRes.data.totalCount || 0) - (statsRes.data.enabledCount || 0)
      }
    } catch {
      // 统计接口失败时使用前端计算值
      stats.total = pagination.total
      stats.enabled = tableData.value.filter((item: any) => item.status === 1).length
      stats.disabled = tableData.value.filter((item: any) => item.status === 0).length
    }
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryParams.dsName = ''
  queryParams.dsType = ''
  queryParams.status = undefined
  handleQuery()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function handleAdd() {
  isEdit.value = false
  testResult.value = null
  Object.assign(formData, {
    id: undefined,
    dsCode: '',
    dsName: '',
    dsType: '',
    host: '',
    port: 3306,
    databaseName: '',
    schemaName: '',
    username: '',
    password: '',
    dataLayer: '',
    remark: ''
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  isEdit.value = true
  testResult.value = null
  Object.assign(formData, { ...record, password: '' })
  modalVisible.value = true
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除数据源「${record.dsName}」吗？删除后将无法恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await dataSourceApi.delete(record.id)
        message.success('删除成功')
        loadData()
      } catch {
        // 删除失败时不关闭确认框，由业务层处理错误
      }
    }
  })
}

async function handleEnable(record: any) {
  try {
    await dataSourceApi.enable(record.id)
    message.success('启用成功')
    loadData()
  } catch {
    // error handled by axios interceptor
  }
}

async function handleDisable(record: any) {
  try {
    await dataSourceApi.disable(record.id)
    message.success('禁用成功')
    loadData()
  } catch {
    // error handled by axios interceptor
  }
}

async function handleModalOk() {
  try {
    await formRef.value.validate()
    modalLoading.value = true
    const payload: any = { ...formData }
    // 编辑时若密码为空则删除该字段，不覆盖原密码
    if (!payload.password) {
      delete payload.password
    }
    // 非 PostgreSQL 类型不提交空的 schemaName
    if (payload.dsType !== 'POSTGRESQL' && !payload.schemaName) {
      delete payload.schemaName
    }
    if (isEdit.value) {
      await dataSourceApi.update(payload.id, payload)
      message.success('编辑成功')
    } else {
      await dataSourceApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch {
    // 验证失败时不关闭弹窗
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() {
  modalVisible.value = false
  testResult.value = null
}

async function handleTestConnection() {
  testLoading.value = true
  testResult.value = null
  try {
    const res: any = await dataSourceApi.testConnection({
      dsType: formData.dsType,
      host: formData.host,
      port: formData.port,
      databaseName: formData.databaseName,
      schemaName: formData.schemaName || undefined,
      username: formData.username,
      password: formData.password
    })
    // 兼容两种响应格式
    const ok = res.success !== false && res.data !== false
    testResult.value = ok
    if (ok) {
      message.success('连接成功')
    } else {
      const errMsg = res.data?.message || res.message || '连接失败，请检查配置'
      message.error(errMsg)
    }
  } catch (err: any) {
    testResult.value = false
    const errMsg = err?.response?.data?.message || err?.message || '连接失败，请检查配置'
    message.error(errMsg)
  } finally {
    testLoading.value = false
  }
}

function getDsTypeName(type: string) {
  const map: Record<string, string> = {
    MYSQL: 'MySQL',
    POSTGRESQL: 'PostgreSQL',
    ORACLE: 'Oracle',
    SQLSERVER: 'SQL Server'
  }
  return map[type] || type
}

function getDsTypeColor(type: string) {
  const map: Record<string, string> = {
    MYSQL: 'blue',
    POSTGRESQL: 'green',
    ORACLE: 'orange',
    SQLSERVER: 'purple'
  }
  return map[type] || 'default'
}

onMounted(() => {
  loadData()
})
</script>

<style lang="less" scoped>
.page-container {
  padding: 24px;
  min-height: 100%;
  background: #F5F7FA;
}
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

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 16px;
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .stat-info {
    .stat-value { font-size: 28px; font-weight: 700; color: #1F1F1F; }
    .stat-label { font-size: 13px; color: #8C8C8C; margin-top: 4px; }
  }
}

.filter-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.connection-info {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #666;
}
</style>
