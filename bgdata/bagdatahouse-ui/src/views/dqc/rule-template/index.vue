<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#grad1)"/>
            <path d="M7 12L10 9L13 12L17 8" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M7 16L10 13L13 16L17 12" stroke="white" stroke-width="1.5" stroke-linecap="round" opacity="0.6"/>
            <defs>
              <linearGradient id="grad1" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#1677FF"/>
                <stop offset="100%" stop-color="#00B4DB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">规则模板管理</h1>
          <p class="page-subtitle">管理 21 种内置模板，支持自定义模板创建与维护</p>
        </div>
      </div>
      <div class="header-right">
        <a-button
          v-if="hasSelected"
          danger
          @click="handleBatchDelete"
        >
          <template #icon><DeleteOutlined /></template>
          批量删除 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button type="primary" @click="openCreateDrawer">
          <template #icon><PlusOutlined /></template>
          新增模板
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
          v-model:value="searchName"
          placeholder="搜索模板名称"
          style="width: 200px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filterType"
          placeholder="规则类型"
          style="width: 160px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部类型</a-select-option>
          <a-select-option v-for="t in ruleTypeOptions" :key="t.value" :value="t.value">
            {{ t.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterLevel"
          placeholder="适用级别"
          style="width: 140px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部级别</a-select-option>
          <a-select-option v-for="l in applyLevelOptions" :key="l.value" :value="l.value">
            {{ l.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterBuiltin"
          placeholder="模板来源"
          style="width: 140px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option :value="1">内置模板</a-select-option>
          <a-select-option :value="0">自定义模板</a-select-option>
        </a-select>
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
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'builtin'">
            <a-tag :color="record.builtin ? 'blue' : 'purple'">
              {{ record.builtin ? '内置' : '自定义' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled"
              size="small"
              :disabled="record.builtin === true"
              @change="(checked: boolean) => handleToggleEnabled(record, checked)"
            />
          </template>

          <template v-if="column.key === 'ruleType'">
            <span class="rule-type-tag" :class="getRuleTypeClass(record.ruleType)">
              {{ getRuleTypeLabel(record.ruleType) }}
            </span>
          </template>

          <template v-if="column.key === 'dimension'">
            <span class="dimension-tag">{{ getDimensionLabel(record.dimension) }}</span>
          </template>

          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <a-divider type="vertical" />
              <a-button
                type="link"
                size="small"
                :disabled="record.builtin === true"
                @click="handleEdit(record)"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-divider type="vertical" />
              <a-button
                type="link"
                size="small"
                danger
                :disabled="record.builtin === true"
                @click="handleDelete(record)"
              >
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerTitle"
      :width="720"
      placement="right"
      :destroy-on-close="true"
      @close="drawerVisible = false"
    >
      <a-steps :current="currentStep" size="small" class="drawer-steps">
        <a-step title="基本信息" />
        <a-step title="配置规则表达式" />
        <a-step title="配置参数规格" />
        <a-step title="预览保存" />
      </a-steps>

      <!-- Step 1: 基本信息 -->
      <div v-show="currentStep === 0" class="step-content">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="模板编码" name="templateCode">
            <a-input
              v-model:value="formData.templateCode"
              placeholder="请输入模板编码，如 TEMPLATE_022"
              :disabled="isEdit"
            />
          </a-form-item>
          <a-form-item label="模板名称" name="templateName">
            <a-input v-model:value="formData.templateName" placeholder="请输入模板名称" />
          </a-form-item>
          <a-form-item label="模板描述" name="templateDesc">
            <a-textarea
              v-model:value="formData.templateDesc"
              :rows="3"
              placeholder="请输入模板描述，说明该模板的用途和使用场景"
            />
          </a-form-item>
          <a-form-item label="适用级别" name="applyLevel">
            <a-select v-model:value="formData.applyLevel" placeholder="请选择适用级别">
              <a-select-option v-for="l in applyLevelOptions" :key="l.value" :value="l.value">
                <span>
                  <span class="level-dot" :class="'level-' + l.value.toLowerCase()"></span>
                  {{ l.label }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="规则类型" name="ruleType">
            <a-select v-model:value="formData.ruleType" placeholder="请选择规则类型">
              <a-select-option v-for="t in ruleTypeOptions" :key="t.value" :value="t.value">
                {{ t.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="质量维度" name="dimension">
            <a-select v-model:value="formData.dimension" placeholder="请选择质量维度">
              <a-select-option v-for="d in dimensionOptions" :key="d.value" :value="d.value">
                {{ d.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </div>

      <!-- Step 2: 配置规则表达式 -->
      <div v-show="currentStep === 1" class="step-content">
        <div class="expr-guide">
          <div class="guide-title">
            <InfoCircleOutlined /> 表达式说明
          </div>
          <div class="guide-tips">
            <p>• 支持的变量占位符：<code>${table}</code>、<code>${column}</code>、<code>${column_a}</code>、<code>${column_b}</code></p>
            <p>• 支持的占位符：<code>${source_table}</code>、<code>${target_table}</code>、<code>${partition_column}</code></p>
            <p>• 支持的占位符：<code>${threshold}</code>、<code>${threshold_min}</code>、<code>${threshold_max}</code>、<code>${enum_values}</code></p>
            <p>• 支持的占位符：<code>${min_length}</code>、<code>${max_length}</code></p>
            <p>• 自定义SQL模板：直接编写 SQL，校验结果行数 = 0 表示通过</p>
          </div>
        </div>

        <a-form
          ref="formRef2"
          :model="formData"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="默认表达式" name="defaultExpr">
            <a-textarea
              v-model:value="formData.defaultExpr"
              :rows="8"
              placeholder="SELECT COUNT(*) FROM ${table} WHERE ${column} IS NULL"
              style="font-family: 'JetBrains Mono', monospace; font-size: 13px"
            />
          </a-form-item>
          <a-form-item label="默认阈值" name="defaultThreshold">
            <a-input
              v-model:value="formData.defaultThreshold"
              placeholder='JSON 格式，如 {"threshold": 5}'
              style="font-family: 'JetBrains Mono', monospace"
            />
          </a-form-item>
        </a-form>

        <!-- 表达式预览 -->
        <div v-if="formData.defaultExpr" class="expr-preview">
          <div class="preview-title">表达式预览</div>
          <pre class="preview-code">{{ formData.defaultExpr }}</pre>
        </div>
      </div>

      <!-- Step 3: 配置参数规格 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="param-guide">
          <div class="guide-title">
            <InfoCircleOutlined /> 参数规格说明
          </div>
          <p>定义该模板支持的配置参数，供规则定义时填写。参数将以表单形式呈现给用户。</p>
        </div>

        <div class="param-list">
          <div
            v-for="(param, index) in paramList"
            :key="index"
            class="param-item"
          >
            <a-input
              v-model:value="param.name"
              placeholder="参数名称"
              style="width: 140px"
            />
            <a-select
              v-model:value="param.type"
              placeholder="参数类型"
              style="width: 120px"
            >
              <a-select-option value="STRING">字符串</a-select-option>
              <a-select-option value="NUMBER">数值</a-select-option>
              <a-select-option value="BOOLEAN">布尔</a-select-option>
              <a-select-option value="ENUM">枚举</a-select-option>
            </a-select>
            <a-input
              v-model:value="param.defaultValue"
              placeholder="默认值"
              style="width: 120px"
            />
            <a-input
              v-model:value="param.description"
              placeholder="参数说明"
              style="flex: 1"
            />
            <a-checkbox v-model:checked="param.required">必填</a-checkbox>
            <a-button type="text" danger @click="removeParam(index)">
              <template #icon><MinusCircleOutlined /></template>
            </a-button>
          </div>
        </div>

        <a-button type="dashed" block @click="addParam" style="margin-top: 12px">
          <template #icon><PlusOutlined /></template>
          添加参数
        </a-button>
      </div>

      <!-- Step 4: 预览保存 -->
      <div v-show="currentStep === 3" class="step-content">
        <div class="preview-section">
          <div class="preview-title">
            <CheckCircleOutlined /> 模板配置总览
          </div>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="模板编码">
              <code>{{ formData.templateCode }}</code>
            </a-descriptions-item>
            <a-descriptions-item label="模板名称">
              {{ formData.templateName }}
            </a-descriptions-item>
            <a-descriptions-item label="模板描述">
              {{ formData.templateDesc || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="适用级别">
              <a-tag>{{ getApplyLevelLabel(formData.applyLevel) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="规则类型">
              <span class="rule-type-tag" :class="getRuleTypeClass(formData.ruleType)">
                {{ getRuleTypeLabel(formData.ruleType) }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="质量维度">
              <a-tag color="cyan">{{ getDimensionLabel(formData.dimension) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="默认表达式">
              <pre class="preview-code-sm">{{ formData.defaultExpr }}</pre>
            </a-descriptions-item>
            <a-descriptions-item label="默认阈值">
              <code>{{ formData.defaultThreshold || '-' }}</code>
            </a-descriptions-item>
            <a-descriptions-item label="参数数量">
              {{ paramList.length }} 个参数
              <template v-if="paramList.length > 0">
                <a-tag v-for="p in paramList" :key="p.name" size="small" style="margin-left: 4px">
                  {{ p.name }}
                </a-tag>
              </template>
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </div>

      <!-- 抽屉底部按钮 -->
      <div class="drawer-footer">
        <a-space>
          <a-button v-if="currentStep > 0" @click="currentStep--">
            <template #icon><LeftOutlined /></template>
            上一步
          </a-button>
          <a-button v-if="currentStep < 3" type="primary" @click="nextStep">
            下一步
            <template #icon><RightOutlined /></template>
          </a-button>
          <a-button
            v-if="currentStep === 3"
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            <template #icon><CheckOutlined /></template>
            {{ isEdit ? '保存修改' : '创建模板' }}
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 查看详情抽屉 -->
    <a-drawer
      v-model:open="viewDrawerVisible"
      title="模板详情"
      :width="560"
      placement="right"
    >
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="模板编码">
          <code>{{ viewRecord?.templateCode }}</code>
        </a-descriptions-item>
        <a-descriptions-item label="模板名称">{{ viewRecord?.templateName }}</a-descriptions-item>
        <a-descriptions-item label="模板描述">{{ viewRecord?.templateDesc || '-' }}</a-descriptions-item>
        <a-descriptions-item label="适用级别">
          <a-tag>{{ getApplyLevelLabel(viewRecord?.applyLevel) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="规则类型">
          <span class="rule-type-tag" :class="getRuleTypeClass(viewRecord?.ruleType)">
            {{ getRuleTypeLabel(viewRecord?.ruleType) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="质量维度">
          <a-tag color="cyan">{{ getDimensionLabel(viewRecord?.dimension) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="模板来源">
          <a-tag :color="viewRecord?.builtin ? 'blue' : 'purple'">
            {{ viewRecord?.builtin ? '内置模板' : '自定义模板' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="启用状态">
          <a-badge :status="viewRecord?.enabled ? 'success' : 'default'" :text="viewRecord?.enabled ? '已启用' : '已禁用'" />
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ viewRecord?.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ viewRecord?.updateTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="默认表达式">
          <pre class="preview-code-sm">{{ viewRecord?.defaultExpr }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="默认阈值">
          <code>{{ viewRecord?.defaultThreshold || '-' }}</code>
        </a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Button,
  Input,
  Select,
  Table,
  Modal,
  Drawer,
  Dropdown,
  Menu,
  Tag,
  Badge,
  Switch,
  Progress,
  Pagination,
  Space,
  Row,
  Col,
  Divider,
  Tooltip,
  Avatar,
  Breadcrumb,
  Steps,
  Form,
  ConfigProvider,
  message
} from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { ruleTemplateApi } from '@/api/dqc'
import type { RuleTemplate, RuleTemplateDTO } from '@/types'

// ======================== 状态 ========================
const loading = ref(false)
const tableData = ref<RuleTemplate[]>([])
const selectedRowKeys = ref<number[]>([])

const searchName = ref('')
const filterType = ref<string>()
const filterLevel = ref<string>()
const filterBuiltin = ref<number>()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// ======================== 统计 ========================
const statCards = ref([
  { label: '模板总数', value: 21, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '内置模板', value: 21, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '自定义模板', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' },
  { label: '停用模板', value: 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' }
])

// ======================== 选项 ========================
const ruleTypeOptions = [
  { value: 'ROW_COUNT_NOT_ZERO', label: '行数非0校验' },
  { value: 'ROW_COUNT_FLUCTUATION', label: '行数波动监测' },
  { value: 'TABLE_UPDATE_TIMELINESS', label: '表更新时效' },
  { value: 'NULL_CHECK', label: '空值检查' },
  { value: 'UNIQUE_CHECK', label: '唯一性校验' },
  { value: 'DUPLICATE_CHECK', label: '重复值检测' },
  { value: 'THRESHOLD_MIN', label: '值域-最小值' },
  { value: 'THRESHOLD_MAX', label: '值域-最大值' },
  { value: 'THRESHOLD_RANGE', label: '值域-范围' },
  { value: 'ENUM_CHECK', label: '枚举值校验' },
  { value: 'REGEX_PHONE', label: '手机号格式' },
  { value: 'REGEX_EMAIL', label: '邮箱格式' },
  { value: 'REGEX_IDCARD', label: '身份证格式' },
  { value: 'CARDINALITY', label: '离散度分析' },
  { value: 'LENGTH_CHECK', label: '字符串长度' },
  { value: 'CROSS_FIELD_COMPARE', label: '跨字段-A>B' },
  { value: 'CROSS_FIELD_SUM', label: '跨字段-和值' },
  { value: 'CROSS_FIELD_NULL_CHECK', label: '跨字段-空值一致性' },
  { value: 'CROSS_TABLE_COUNT', label: '跨表-记录数' },
  { value: 'CROSS_TABLE_PRIMARY_KEY', label: '跨表-主键一致性' },
  { value: 'CUSTOM_SQL', label: '自定义SQL' },
  { value: 'CUSTOM_FUNC', label: '自定义函数' }
]

const applyLevelOptions = [
  { value: 'TABLE', label: '表级' },
  { value: 'COLUMN', label: '字段级' },
  { value: 'CROSS_FIELD', label: '跨字段' },
  { value: 'CROSS_TABLE', label: '跨表' },
  { value: 'DATABASE', label: '数据库级' }
]

const dimensionOptions = [
  { value: 'COMPLETENESS', label: '完整性' },
  { value: 'UNIQUENESS', label: '唯一性' },
  { value: 'ACCURACY', label: '准确性' },
  { value: 'CONSISTENCY', label: '一致性' },
  { value: 'TIMELINESS', label: '及时性' },
  { value: 'VALIDITY', label: '有效性' },
  { value: 'CUSTOM', label: '自定义' }
]

// ======================== 表格列 ========================
const columns = [
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode', width: 160, fixed: 'left' },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: 180, ellipsis: true },
  { title: '规则类型', key: 'ruleType', width: 130 },
  { title: '适用级别', dataIndex: 'applyLevel', key: 'applyLevel', width: 100 },
  { title: '质量维度', key: 'dimension', width: 100 },
  { title: '是否内置', key: 'builtin', width: 90 },
  { title: '启用状态', key: 'enabled', width: 90 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

const hasSelected = computed(() => selectedRowKeys.value.length > 0)

// ======================== 抽屉 & 表单 ========================
const drawerVisible = ref(false)
const drawerTitle = ref('新增模板')
const currentStep = ref(0)
const submitLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number>()

const viewDrawerVisible = ref(false)
const viewRecord = ref<RuleTemplate | null>(null)

const formData = reactive<Partial<RuleTemplateDTO>>({
  templateCode: '',
  templateName: '',
  templateDesc: '',
  ruleType: undefined,
  applyLevel: undefined,
  dimension: undefined,
  defaultExpr: '',
  defaultThreshold: '',
  builtin: false,
  enabled: true
})

const paramList = reactive<any[]>([])

const formRef = ref()
const formRef2 = ref()

const formRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  applyLevel: [{ required: true, message: '请选择适用级别', trigger: 'change' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }]
}

// ======================== 方法 ========================
async function loadData() {
  loading.value = true
  try {
    const res: any = await ruleTemplateApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      templateName: searchName.value || undefined,
      ruleType: filterType.value || undefined,
      applyLevel: filterLevel.value || undefined,
      builtin: filterBuiltin.value
    })
    tableData.value = res.data?.records || res.data || []
    pagination.total = res.data?.total || tableData.value.length
    statCards.value[0].value = pagination.total
  } catch {
    // 后端未启动时使用空数据
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilters() {
  searchName.value = ''
  filterType.value = undefined
  filterLevel.value = undefined
  filterBuiltin.value = undefined
  pagination.current = 1
  loadData()
}

function openCreateDrawer() {
  isEdit.value = false
  editingId.value = undefined
  drawerTitle.value = '新增模板'
  currentStep.value = 0
  resetForm()
  drawerVisible.value = true
}

function handleEdit(record: RuleTemplate) {
  isEdit.value = true
  editingId.value = record.id
  drawerTitle.value = '编辑模板'
  currentStep.value = 0

  Object.assign(formData, {
    templateCode: record.templateCode,
    templateName: record.templateName,
    templateDesc: record.templateDesc,
    ruleType: record.ruleType,
    applyLevel: record.applyLevel,
    dimension: record.dimension,
    defaultExpr: record.defaultExpr,
    defaultThreshold: record.defaultThreshold,
    builtin: record.builtin,
    enabled: record.enabled
  })

  try {
    const parsed = record.paramSpec ? JSON.parse(record.paramSpec) : []
    paramList.splice(0, paramList.length, ...parsed)
  } catch {
    paramList.splice(0, paramList.length)
  }

  drawerVisible.value = true
}

function handleView(record: RuleTemplate) {
  viewRecord.value = record
  viewDrawerVisible.value = true
}

async function handleDelete(record: RuleTemplate) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板「${record.templateName}」吗？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await ruleTemplateApi.delete(record.id!)
        message.success('删除成功')
        loadData()
      } catch {
        message.error('删除失败，请重试')
      }
    }
  })
}

async function handleBatchDelete() {
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个模板吗？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await Promise.all(selectedRowKeys.value.map(id => ruleTemplateApi.delete(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

async function handleToggleEnabled(record: RuleTemplate, checked: boolean) {
  try {
    await ruleTemplateApi.update(record.id!, { ...record, enabled: checked } as any)
    record.enabled = checked
    message.success(checked ? '已启用' : '已禁用')
  } catch {
    message.error('操作失败')
  }
}

function nextStep() {
  if (currentStep.value === 0) {
    formRef.value?.validate().then(() => {
      currentStep.value++
    })
  } else {
    currentStep.value++
  }
}

function resetForm() {
  Object.assign(formData, {
    templateCode: '',
    templateName: '',
    templateDesc: '',
    ruleType: undefined,
    applyLevel: undefined,
    dimension: undefined,
    defaultExpr: '',
    defaultThreshold: '',
    builtin: false,
    enabled: true
  })
  paramList.splice(0, paramList.length)
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    const dto: RuleTemplateDTO = {
      templateCode: formData.templateCode!,
      templateName: formData.templateName!,
      templateDesc: formData.templateDesc,
      ruleType: formData.ruleType!,
      applyLevel: formData.applyLevel!,
      dimension: formData.dimension,
      defaultExpr: formData.defaultExpr,
      defaultThreshold: formData.defaultThreshold,
      builtin: false,
      enabled: true
    }
    dto.paramSpec = paramList.length > 0 ? JSON.stringify(paramList) : ''

    if (isEdit.value) {
      await ruleTemplateApi.update(editingId.value!, dto)
      message.success('修改成功')
    } else {
      await ruleTemplateApi.create(dto)
      message.success('创建成功')
    }

    drawerVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

function addParam() {
  paramList.push({ name: '', type: 'STRING', required: false, defaultValue: '', description: '' })
}

function removeParam(index: number) {
  paramList.splice(index, 1)
}

// ======================== 辅助函数 ========================
function getRuleTypeLabel(type: string | undefined) {
  return ruleTypeOptions.find(t => t.value === type)?.label || type || '-'
}

function getRuleTypeClass(type: string | undefined) {
  const map: Record<string, string> = {
    NULL_CHECK: 'type-null',
    UNIQUE_CHECK: 'type-unique',
    REGEX_PHONE: 'type-regex',
    REGEX_EMAIL: 'type-regex',
    REGEX_IDCARD: 'type-regex',
    THRESHOLD_MIN: 'type-threshold',
    THRESHOLD_MAX: 'type-threshold',
    THRESHOLD_RANGE: 'type-threshold',
    CROSS_FIELD_COMPARE: 'type-cross',
    CROSS_FIELD_SUM: 'type-cross',
    CROSS_FIELD_NULL_CHECK: 'type-cross',
    CROSS_TABLE_COUNT: 'type-cross',
    CROSS_TABLE_PRIMARY_KEY: 'type-cross',
    CUSTOM_SQL: 'type-custom',
    CUSTOM_FUNC: 'type-custom',
    CARDINALITY: 'type-analysis',
    LENGTH_CHECK: 'type-check',
    ENUM_CHECK: 'type-check',
    ROW_COUNT_NOT_ZERO: 'type-table',
    ROW_COUNT_FLUCTUATION: 'type-table',
    TABLE_UPDATE_TIMELINESS: 'type-table',
    DUPLICATE_CHECK: 'type-unique'
  }
  return map[type || ''] || 'type-default'
}

function getApplyLevelLabel(level: string | undefined) {
  return applyLevelOptions.find(l => l.value === level)?.label || level || '-'
}

function getDimensionLabel(dim: string | undefined) {
  return dimensionOptions.find(d => d.value === dim)?.label || dim || '-'
}

onMounted(() => {
  loadData()
})
</script>

<style lang="less" scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    .header-icon {
      flex-shrink: 0;
    }
    .page-title {
      font-size: 22px;
      font-weight: 700;
      color: #1F1F1F;
      margin: 0;
    }
    .page-subtitle {
      font-size: 13px;
      color: #8C8C8C;
      margin: 4px 0 0;
    }
  }
  .header-right {
    display: flex;
    gap: 12px;
  }
}

.stat-mini-card {
  border-radius: 8px;
  padding: 16px 20px;
  color: #fff;
  .mini-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.2;
  }
  .mini-label {
    font-size: 13px;
    opacity: 0.85;
    margin-top: 4px;
  }
}

.filter-bar {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.rule-type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  &.type-null      { background: #E6F7FF; color: #1677FF; }
  &.type-unique    { background: #F6FFED; color: #52C41A; }
  &.type-regex     { background: #FFF7E6; color: #FA8C16; }
  &.type-threshold { background: #FFF1F0; color: #FF4D4F; }
  &.type-cross     { background: #F9F0FF; color: #722ED1; }
  &.type-custom    { background: #E6F7FF; color: #13C2C2; }
  &.type-analysis  { background: #F9F0FF; color: #9254DE; }
  &.type-check     { background: #FFF7E6; color: #FA8C16; }
  &.type-table     { background: #E6F7FF; color: #1890FF; }
}

.dimension-tag {
  font-size: 12px;
  color: #1890FF;
}

.drawer-steps {
  margin-bottom: 32px;
  padding: 0 20px;
}

.step-content {
  min-height: 400px;
}

.expr-guide {
  background: #E6F7FF;
  border: 1px solid #91D5FF;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  .guide-title {
    font-size: 14px;
    font-weight: 600;
    color: #1677FF;
    margin-bottom: 8px;
  }
  .guide-tips {
    font-size: 13px;
    color: #333;
    p { margin: 4px 0; }
    code {
      background: #BAE0FF;
      padding: 1px 4px;
      border-radius: 3px;
      font-family: 'JetBrains Mono', monospace;
      font-size: 12px;
    }
  }
}

.param-guide {
  background: #F9F0FF;
  border: 1px solid #D3ADF7;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  .guide-title {
    font-size: 14px;
    font-weight: 600;
    color: #722ED1;
    margin-bottom: 8px;
  }
  p {
    margin: 0;
    font-size: 13px;
    color: #333;
  }
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  .param-item {
    display: flex;
    align-items: center;
    gap: 8px;
    background: #F5F7FA;
    border-radius: 8px;
    padding: 12px;
  }
}

.expr-preview {
  margin-top: 16px;
  .preview-title {
    font-size: 13px;
    font-weight: 600;
    color: #52C41A;
    margin-bottom: 8px;
  }
  .preview-code {
    background: #1F1F1F;
    color: #A6E22E;
    padding: 16px;
    border-radius: 8px;
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 13px;
    line-height: 1.6;
    overflow-x: auto;
    margin: 0;
  }
}

.preview-section {
  .preview-title {
    font-size: 15px;
    font-weight: 600;
    color: #1F1F1F;
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.preview-code-sm {
  background: #F5F7FA;
  padding: 8px 12px;
  border-radius: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #E8E8E8;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.level-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  &.level-table   { background: #1677FF; }
  &.level-column  { background: #52C41A; }
  &.level-cross_field { background: #722ED1; }
  &.level-cross_table { background: #FA8C16; }
  &.level-database { background: #FF4D4F; }
}
</style>
