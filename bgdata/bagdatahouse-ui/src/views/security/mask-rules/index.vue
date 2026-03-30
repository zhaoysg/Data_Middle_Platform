<template>
  <div class="mask-rules-page">
    <!-- ========== 页面标题区 ========== -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#mrGrad)"/>
            <path d="M7 12C7 9.24 9.24 7 12 7C14.76 7 17 9.24 17 12C17 14.76 14.76 17 12 17" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="9" cy="10" r="1.5" fill="white"/>
            <circle cx="15" cy="10" r="1.5" fill="white"/>
            <path d="M9 15H15" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <defs>
              <linearGradient id="mrGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#B37FEB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">脱敏规则管理</h1>
          <p class="page-subtitle">管理数据脱敏模板，支持创建、编辑、启用禁用及脱敏效果预览</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="loadData" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          新增模板
        </a-button>
      </div>
    </div>

    <!-- ========== 统计卡片行 ========== -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">模板总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #13C2C2 0%, #36CBCB 100%)">
          <div class="stat-value">{{ stats.builtin || 0 }}</div>
          <div class="stat-label">内置模板</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-value">{{ stats.enabled || 0 }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #FA541C 0%, #FFBB96 100%)">
          <div class="stat-value">{{ stats.maskCount || 0 }}</div>
          <div class="stat-label">遮蔽类</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #F5222D 0%, #FF7875 100%)">
          <div class="stat-value">{{ stats.encryptCount || 0 }}</div>
          <div class="stat-label">加密/删除</div>
        </div>
      </a-col>
    </a-row>

    <!-- ========== 主内容卡片 ========== -->
    <div class="table-card">
      <!-- 筛选工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <a-select
            v-model:value="filterEnabled"
            placeholder="启用状态"
            style="width: 120px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option :value="1">已启用</a-select-option>
            <a-select-option :value="0">已禁用</a-select-option>
          </a-select>
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索模板名称/编码"
            style="width: 200px"
            allowClear
            @pressEnter="handleSearch"
            @change="debouncedSearch"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-button @click="resetFilters">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </div>
        <div class="toolbar-right">
        </div>
      </div>

      <!-- 表格区域 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="paginationConfig"
        :scroll="{ x: 1100 }"
        row-key="id"
        @change="handleTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <!-- 模板名称 -->
          <template v-if="column.key === 'templateName'">
            <div class="template-name-cell">
              <div class="template-name-row">
                <a-tag v-if="record.builtin === 1" color="gold" style="margin-right: 4px; font-size: 11px">内置</a-tag>
                <a-tooltip :title="record.templateCode || ''">
                  <span class="template-name-text">{{ record.templateName }}</span>
                </a-tooltip>
              </div>
              <div class="template-code-text">{{ record.templateCode }}</div>
            </div>
          </template>

          <!-- 脱敏类型 -->
          <template v-if="column.key === 'maskType'">
            <a-tag :color="getMaskTypeColor(record.maskType)" style="font-weight: 500">
              {{ record.maskTypeLabel || record.maskType || '-' }}
            </a-tag>
          </template>

          <!-- 掩码正则 -->
          <template v-if="column.key === 'maskPattern'">
            <code class="mask-pattern-code" v-if="record.maskPattern">{{ record.maskPattern }}</code>
            <span v-else style="color: #d9d9d9">—</span>
          </template>

          <!-- 启用状态 -->
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :disabled="record.builtin === 1"
              :loading="(record as any).switchLoading"
              checked-children="启用"
              un-checked-children="禁用"
              size="small"
              @change="(checked: boolean) => handleToggleStatus(record, checked)"
            />
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-tooltip title="测试">
                <a-button type="text" size="small" @click="handleTest(record)">
                  <template #icon><ThunderboltOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="text" size="small" @click="handleEdit(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm
                v-if="record.builtin !== 1"
                title="确定删除该模板？删除后不可恢复。"
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
            <SafetyCertificateOutlined class="empty-icon" />
            <div class="empty-title">暂无脱敏模板</div>
            <div class="empty-desc">点击「新增模板」创建第一个脱敏规则</div>
            <a-button type="primary" @click="handleCreate">新增模板</a-button>
          </div>
        </template>
      </a-table>
    </div>

    <!-- ========== 新增/编辑抽屉 ========== -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑脱敏模板' : '新增脱敏模板'"
      :width="600"
      :mask-closable="false"
      @close="drawerVisible = false"
    >
      <template v-if="drawerVisible">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          layout="horizontal"
        >
          <a-form-item label="模板名称" name="templateName">
            <a-input
              v-model:value="formData.templateName"
              placeholder="请输入模板名称，如：手机号中间脱敏"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="模板编码" name="templateCode">
            <a-input
              v-model:value="formData.templateCode"
              placeholder="请输入唯一编码，如：PHONE_MASK"
              :disabled="isEdit"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="脱敏类型" name="maskType">
            <a-select
              v-model:value="formData.maskType"
              placeholder="请选择脱敏类型"
              @change="handleMaskTypeChange"
            >
              <a-select-option value="NONE">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #8C8C8C"></span>
                  不脱敏 (NONE)
                </span>
              </a-select-option>
              <a-select-option value="MASK">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #FAAD14"></span>
                  掩码遮蔽 (MASK)
                </span>
              </a-select-option>
              <a-select-option value="HIDE">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #722ED1"></span>
                  完全隐藏 (HIDE)
                </span>
              </a-select-option>
              <a-select-option value="ENCRYPT">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #F5222D"></span>
                  加密 (ENCRYPT)
                </span>
              </a-select-option>
              <a-select-option value="DELETE">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #F5222D"></span>
                  删除 (DELETE)
                </span>
              </a-select-option>
              <a-select-option value="HASH">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #13C2C2"></span>
                  哈希脱敏 (HASH)
                </span>
              </a-select-option>
              <a-select-option value="FORMAT_KEEP">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #1677FF"></span>
                  保留格式加密 (FORMAT_KEEP)
                </span>
              </a-select-option>
              <a-select-option value="RANGE">
                <span class="mask-type-option">
                  <span class="mask-type-dot" style="background: #FA8C16"></span>
                  区间化 (RANGE)
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>

          <!-- MASK/HIDE 类型显示掩码配置 -->
          <template v-if="formData.maskType === 'MASK' || formData.maskType === 'HIDE'">
            <a-form-item label="掩码字符" name="maskChar">
              <a-input
                v-model:value="formData.maskChar"
                placeholder="默认 *"
                style="width: 100px"
                :maxlength="1"
              />
              <span class="field-tip">用于遮蔽的字符，默认为 *</span>
            </a-form-item>

            <a-form-item label="遮蔽位置" name="maskPosition">
              <a-select v-model:value="formData.maskPosition" placeholder="请选择遮蔽位置">
                <a-select-option value="ALL">
                  <span class="mask-pos-option">
                    <span class="mask-pos-icon">全</span> 全部遮蔽
                  </span>
                </a-select-option>
                <a-select-option value="HEAD">
                  <span class="mask-pos-option">
                    <span class="mask-pos-icon">前</span> 头部遮蔽
                  </span>
                </a-select-option>
                <a-select-option value="TAIL">
                  <span class="mask-pos-option">
                    <span class="mask-pos-icon">后</span> 尾部遮蔽
                  </span>
                </a-select-option>
                <a-select-option value="CENTER">
                  <span class="mask-pos-option">
                    <span class="mask-pos-icon">中</span> 中间遮蔽（默认）
                  </span>
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="保留头部" name="maskHeadKeep">
              <a-input-number
                v-model:value="formData.maskHeadKeep"
                :min="0"
                :max="20"
                placeholder="保留前N位"
                style="width: 120px"
              />
              <span class="field-tip">掩码时保留的前几位字符</span>
            </a-form-item>

            <a-form-item label="保留尾部" name="maskTailKeep">
              <a-input-number
                v-model:value="formData.maskTailKeep"
                :min="0"
                :max="20"
                placeholder="保留后N位"
                style="width: 120px"
              />
              <span class="field-tip">掩码时保留的后几位字符</span>
            </a-form-item>
          </template>

          <a-form-item label="掩码正则" name="maskPattern">
            <a-textarea
              v-model:value="formData.maskPattern"
              placeholder="可选，如: (\d{3})\d{4}(\d{4}) → $1****$2"
              :rows="2"
              :maxlength="200"
              show-count
            />
            <div class="form-help">用于高级替换的正则表达式和替换规则</div>
          </a-form-item>

          <a-form-item label="模板描述" name="templateDesc">
            <a-textarea
              v-model:value="formData.templateDesc"
              placeholder="请输入模板描述，说明适用场景和管理规范"
              :rows="3"
              :maxlength="200"
              show-count
            />
          </a-form-item>

          <a-form-item label="是否启用" name="status">
            <a-switch v-model:checked="formStatusChecked" checked-children="启用" un-checked-children="禁用" />
          </a-form-item>
        </a-form>

        <!-- ========== 实时预览 ========== -->
        <a-divider orientation="left">
          <span class="preview-title">
            <ThunderboltOutlined style="color: #FA541C; margin-right: 4px" />
            脱敏预览
          </span>
        </a-divider>

        <div class="preview-area">
          <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
            <a-form-item label="测试输入">
              <a-input
                v-model:value="previewInput"
                :placeholder="getPreviewPlaceholder()"
                allowClear
                @input="updatePreview"
              />
            </a-form-item>
            <a-form-item label="预览结果">
              <div class="preview-result">
                {{ previewResult || '预览结果将显示在这里' }}
              </div>
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4 }">
              <a-space>
                <a-button @click="previewInput = ''; previewResult = ''">清空</a-button>
                <a-button type="primary" @click="handlePreviewSample">
                  <template #icon><ThunderboltOutlined /></template>
                  示例数据
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建模板' }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- ========== 测试脱敏效果弹窗 ========== -->
    <a-modal
      v-model:open="testModalVisible"
      :title="`测试脱敏效果 — ${testingTemplate?.templateName || ''}`"
      :width="600"
      :footer="null"
      :destroy-on-close="true"
      @cancel="testModalVisible = false"
    >
      <template v-if="testingTemplate">
        <div class="test-modal-header">
          <div class="test-template-info">
            <a-tag :color="getMaskTypeColor(testingTemplate.maskType)" style="font-weight: 600">
              {{ testingTemplate.maskTypeLabel || testingTemplate.maskType }}
            </a-tag>
            <code class="test-template-code">{{ testingTemplate.templateCode }}</code>
          </div>
          <div v-if="testingTemplate.maskPattern" class="test-pattern-info">
            <span style="color: #8C8C8C; font-size: 12px">正则：</span>
            <code style="font-size: 12px; color: #722ED1">{{ testingTemplate.maskPattern }}</code>
          </div>
        </div>

        <div class="test-inputs">
          <div
            v-for="(testCase, index) in testCases"
            :key="index"
            class="test-case-item"
          >
            <div class="test-case-header">
              <span class="test-case-label">测试用例 {{ index + 1 }}</span>
              <a-button
                v-if="testCases.length > 1"
                type="text"
                size="small"
                danger
                @click="removeTestCase(index)"
              >
                <template #icon><MinusCircleOutlined /></template>
              </a-button>
            </div>
            <a-input
              v-model:value="testCase.input"
              :placeholder="getPreviewPlaceholder()"
              @update:value="runTest(index)"
              allowClear
            />
            <div class="test-result-box">
              {{ testCase.output || '等待输入...' }}
            </div>
          </div>
        </div>

        <div class="test-actions">
          <a-button @click="addTestCase">
            <template #icon><PlusOutlined /></template>
            添加测试用例
          </a-button>
          <a-button @click="resetTestCases" v-if="testCases.length > 1">
            <template #icon><ReloadOutlined /></template>
            重置全部
          </a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
  SearchOutlined, SafetyCertificateOutlined, ThunderboltOutlined,
  MinusCircleOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  pageMaskTemplate,
  saveMaskTemplate,
  updateMaskTemplate,
  deleteMaskTemplate,
  enableMaskTemplate,
  disableMaskTemplate,
  type SecMaskTemplateVO,
  type SecMaskTemplateSaveDTO
} from '@/api/security'

// ============ 统计卡片 ============
const stats = reactive({
  total: 0,
  builtin: 0,
  enabled: 0,
  maskCount: 0,
  encryptCount: 0
})

// ============ 状态定义 ============
const loading = ref(false)
const tableData = ref<SecMaskTemplateVO[]>([])
const searchKeyword = ref('')
const filterEnabled = ref<number | undefined>(undefined)

// 分页配置
const paginationConfig = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
}))

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 表格列定义
const columns = [
  { title: '模板名称', key: 'templateName', width: 240 },
  { title: '脱敏类型', key: 'maskType', width: 140, align: 'center' as const },
  { title: '掩码正则', key: 'maskPattern', width: 220 },
  { title: '启用', key: 'status', width: 90, align: 'center' as const },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const }
]

// ============ 抽屉状态 ============
const drawerVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | undefined>()
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<SecMaskTemplateSaveDTO>({
  id: undefined,
  templateName: '',
  templateCode: '',
  templateDesc: '',
  maskType: undefined as any,
  maskChar: '*',
  maskPosition: 'CENTER',
  maskHeadKeep: 3,
  maskTailKeep: 4,
  maskPattern: '',
  status: 1
})

const formStatusChecked = ref(true)

// 表单验证规则
const formRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  maskType: [{ required: true, message: '请选择脱敏类型', trigger: 'change' }]
}

// ============ 测试弹窗状态 ============
const testModalVisible = ref(false)
const testingTemplate = ref<SecMaskTemplateVO | null>(null)

interface TestCase {
  input: string
  output: string
}

const testCases = ref<TestCase[]>([{ input: '', output: '' }])

// ============ 预览状态 ============
const previewInput = ref('')
const previewResult = ref('')

// ============ 脱敏类型颜色映射 ============
function getMaskTypeColor(maskType?: string): string {
  const colorMap: Record<string, string> = {
    NONE: '#8C8C8C',
    MASK: '#FAAD14',
    HIDE: '#722ED1',
    ENCRYPT: '#F5222D',
    DELETE: '#F5222D',
    HASH: '#13C2C2',
    RANDOM_REPLACE: '#1677FF',
    RANGE: '#FA8C16',
    WATERMARK: '#722ED1',
    FORMAT_KEEP: '#1677FF'
  }
  return colorMap[maskType || ''] || 'default'
}

// ============ 示例数据映射 ============
const SAMPLE_DATA: Record<string, string> = {
  NONE: '13812345678',
  MASK: '13812345678',
  HIDE: '13812345678',
  ENCRYPT: 'HelloWorld',
  DELETE: '13812345678',
  HASH: '13812345678',
  FORMAT_KEEP: '13812345678',
  RANGE: '50000',
  WATERMARK: '13812345678',
  RANDOM_REPLACE: '13812345678'
}

function getPreviewPlaceholder(): string {
  const t = formData.maskType || testingTemplate.value?.maskType || ''
  const samples: Record<string, string> = {
    NONE: '任意文本，示例: Hello',
    MASK: '手机号，示例: 13812345678',
    HIDE: '邮箱，示例: user@example.com',
    ENCRYPT: '任意文本，示例: HelloWorld',
    DELETE: '任意内容，示例: 敏感数据',
    HASH: '身份证，示例: 110101199003074515',
    FORMAT_KEEP: '手机号，示例: 13812345678',
    RANGE: '金额，示例: 50000'
  }
  return samples[t] || '请输入测试数据'
}

function handlePreviewSample() {
  const t = formData.maskType || ''
  previewInput.value = SAMPLE_DATA[t] || '13812345678'
  updatePreview()
}

function handleMaskTypeChange() {
  previewInput.value = ''
  previewResult.value = ''
}

// ============ 加载数据 ============
async function loadData() {
  loading.value = true
  try {
    const res = await pageMaskTemplate({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      enabled: filterEnabled.value
    })

    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      tableData.value = records.map(item => ({ ...item, switchLoading: false }))
      pagination.total = res.data.total ?? 0

      // 更新统计
      stats.total = res.data.total ?? 0
      stats.builtin = records.filter((r: any) => r.builtin === 1).length
      stats.enabled = records.filter((r: any) => r.status === 1).length
      stats.maskCount = records.filter((r: any) =>
        r.maskType === 'MASK' || r.maskType === 'HIDE'
      ).length
      stats.encryptCount = records.filter((r: any) =>
        r.maskType === 'ENCRYPT' || r.maskType === 'DELETE' || r.maskType === 'HASH'
      ).length
    } else {
      tableData.value = []
      pagination.total = 0
    }
  } catch (error) {
    message.error('加载脱敏模板失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ============ 搜索 ============
let searchTimer: any = null
function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pagination.current = 1
    loadData()
  }, 300)
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function resetFilters() {
  searchKeyword.value = ''
  filterEnabled.value = undefined
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadData()
}

// ============ 增删改 ============
function handleCreate() {
  isEdit.value = false
  editingId.value = undefined
  resetFormData()
  drawerVisible.value = true
}

function handleEdit(record: SecMaskTemplateVO) {
  isEdit.value = true
  editingId.value = record.id

  Object.assign(formData, {
    id: record.id,
    templateName: record.templateName,
    templateCode: record.templateCode,
    templateDesc: record.templateDesc,
    maskType: (record.maskType || '') as any,
    maskChar: record.maskChar || '*',
    maskPosition: record.maskPosition || 'CENTER',
    maskHeadKeep: record.maskHeadKeep ?? 3,
    maskTailKeep: record.maskTailKeep ?? 4,
    maskPattern: record.maskPattern,
    status: record.status ?? 1
  })

  formStatusChecked.value = formData.status === 1
  setDefaultPreviewValue(record)
  drawerVisible.value = true
}

async function handleDelete(record: SecMaskTemplateVO) {
  if (!record.id) return
  try {
    const res = await deleteMaskTemplate(record.id)
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

async function handleToggleStatus(record: SecMaskTemplateVO, checked: boolean) {
  if (!record.id) return

  const targetRow = tableData.value.find(r => r.id === record.id)
  if (targetRow) (targetRow as any).switchLoading = true

  try {
    const res = checked
      ? await enableMaskTemplate(record.id)
      : await disableMaskTemplate(record.id)

    if (res.code === 200) {
      message.success(`${checked ? '启用' : '禁用'}成功`)
      if (targetRow) {
        targetRow.status = checked ? 1 : 0
        stats.enabled += checked ? 1 : -1
      }
    } else {
      message.error(res.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  } finally {
    if (targetRow) (targetRow as any).switchLoading = false
  }
}

// ============ 测试模板 ============
function handleTest(record: SecMaskTemplateVO) {
  testingTemplate.value = record
  testCases.value = [{ input: '', output: '' }]
  testModalVisible.value = true
}

function addTestCase() {
  testCases.value.push({ input: '', output: '' })
}

function removeTestCase(index: number) {
  testCases.value.splice(index, 1)
}

function resetTestCases() {
  testCases.value = [{ input: '', output: '' }]
}

function runTest(index: number) {
  const testCase = testCases.value[index]
  if (!testCase.input || !testingTemplate.value) {
    testCase.output = ''
    return
  }

  testCase.output = applyMask(
    testCase.input,
    testingTemplate.value.maskType,
    testingTemplate.value.maskPattern,
    testingTemplate.value.maskChar,
    testingTemplate.value.maskHeadKeep,
    testingTemplate.value.maskTailKeep,
    testingTemplate.value.maskPosition
  )
}

// ============ 脱敏引擎（前端模拟） ============
function applyMask(
  input: string,
  maskType?: string,
  pattern?: string,
  maskChar?: string,
  headKeep?: number,
  tailKeep?: number,
  position?: string
): string {
  if (!input) return ''

  const char = maskChar || '*'

  switch (maskType) {
    case 'NONE':
      return input

    case 'MASK':
    case 'HIDE':
      if (position === 'ALL') {
        return char.repeat(input.length)
      } else if (position === 'HEAD') {
        const h = headKeep ?? 0
        return input.slice(0, h) + char.repeat(input.length - h)
      } else if (position === 'TAIL') {
        const t = tailKeep ?? 0
        return char.repeat(input.length - t) + input.slice(-t)
      } else {
        // CENTER 默认
        const h = headKeep ?? 3
        const t = tailKeep ?? 4
        if (input.length <= h + t) {
          return char.repeat(input.length)
        }
        return input.slice(0, h) + char.repeat(input.length - h - t) + input.slice(-t)
      }

    case 'DELETE':
      return '[已删除]'

    case 'ENCRYPT': {
      // 简单字符码加密
      return input.split('').map(c => (c.charCodeAt(0) ^ 42).toString(16).padStart(2, '0')).join('')
    }

    case 'HASH': {
      // 简单哈希模拟
      let hash = 0
      for (let i = 0; i < input.length; i++) {
        hash = ((hash << 5) - hash) + input.charCodeAt(i)
        hash |= 0
      }
      return Math.abs(hash).toString(16).toUpperCase().padStart(8, '0')
    }

    case 'FORMAT_KEEP': {
      // 保留格式的假名化
      return input.split('').map(c => {
        if (/[a-zA-Z]/.test(c)) return String.fromCharCode(65 + Math.floor(Math.random() * 26))
        if (/[0-9]/.test(c)) return String(Math.floor(Math.random() * 10))
        return c
      }).join('')
    }

    case 'RANGE': {
      const num = parseFloat(input)
      if (isNaN(num)) return input
      if (num < 1000) return '< 1,000'
      if (num < 10000) return '1,000 - 10,000'
      if (num < 100000) return '10,000 - 100,000'
      if (num < 1000000) return '100,000 - 1,000,000'
      return '> 1,000,000'
    }

    case 'WATERMARK':
      return input + '【水印】'

    default:
      return input
  }
}

// ============ 预览 ============
function setDefaultPreviewValue(record: SecMaskTemplateVO) {
  switch (record.maskType) {
    case 'MASK':
    case 'HIDE':
      if (record.templateName?.includes('手机')) previewInput.value = '13812345678'
      else if (record.templateName?.includes('邮箱')) previewInput.value = 'test@example.com'
      else if (record.templateName?.includes('身份证')) previewInput.value = '110101199003074515'
      else if (record.templateName?.includes('银行卡')) previewInput.value = '6222021234567890123'
      else previewInput.value = '13812345678'
      break
    case 'ENCRYPT': previewInput.value = 'HelloWorld'; break
    case 'HASH': previewInput.value = '110101199003074515'; break
    case 'DELETE': previewInput.value = '敏感内容'; break
    case 'FORMAT_KEEP': previewInput.value = '13812345678'; break
    case 'RANGE': previewInput.value = '50000'; break
    default: previewInput.value = ''
  }
  updatePreview()
}

function updatePreview() {
  if (!previewInput.value || !formData.maskType) {
    previewResult.value = ''
    return
  }

  previewResult.value = applyMask(
    previewInput.value,
    formData.maskType,
    formData.maskPattern,
    formData.maskChar,
    formData.maskHeadKeep,
    formData.maskTailKeep,
    formData.maskPosition
  )
}

// ============ 表单提交 ============
function resetFormData() {
  Object.assign(formData, {
    id: undefined,
    templateName: '',
    templateCode: '',
    templateDesc: '',
    maskType: undefined as any,
    maskChar: '*',
    maskPosition: 'CENTER',
    maskHeadKeep: 3,
    maskTailKeep: 4,
    maskPattern: '',
    status: 1
  })
  formStatusChecked.value = true
  previewInput.value = ''
  previewResult.value = ''
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    formData.status = formStatusChecked.value ? 1 : 0

    if (isEdit.value && editingId.value) {
      const res = await updateMaskTemplate(editingId.value, formData)
      if (res.code === 200) {
        message.success('模板更新成功')
        drawerVisible.value = false
        loadData()
      } else {
        message.error(res.message || '更新失败')
      }
    } else {
      const res = await saveMaskTemplate(formData)
      if (res.code === 200) {
        message.success('模板创建成功')
        drawerVisible.value = false
        loadData()
      } else {
        message.error(res.message || '创建失败')
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// ============ 监听表单变化更新预览 ============
watch(
  () => [formData.maskType, formData.maskChar, formData.maskPosition, formData.maskHeadKeep, formData.maskTailKeep],
  () => {
    updatePreview()
  },
  { deep: true }
)

// ============ 初始化 ============
onMounted(() => {
  loadData()
})
</script>

<style lang="less" scoped>
.mask-rules-page { padding: 0; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; background: linear-gradient(135deg, #722ED110 0%, #B37FEB10 100%);
  border: 1px solid #722ED120; border-radius: 12px; padding: 20px 24px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1F1F1F; line-height: 1.3; }
.page-subtitle { margin: 4px 0 0; font-size: 13px; color: #8C8C8C; }
.header-right { display: flex; align-items: center; gap: 8px; }
.stat-row { margin-bottom: 16px; }
.stat-card {
  border-radius: 12px; padding: 16px; color: #fff; min-height: 88px;
  display: flex; flex-direction: column; justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: transform 0.2s, box-shadow 0.2s; cursor: default;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; opacity: 0.88; margin-top: 4px; }
.table-card { background: #fff; border-radius: 10px; padding: 16px; box-shadow: 0 1px 2px rgba(0,0,0,0.03); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.toolbar-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.template-name-cell { display: flex; flex-direction: column; gap: 2px; }
.template-name-row { display: flex; align-items: center; flex-wrap: wrap; gap: 2px; }
.template-name-text { font-weight: 600; color: #1F1F1F; font-size: 13px; }
.template-code-text { font-size: 11px; color: #8C8C8C; font-family: 'JetBrains Mono', monospace; }

.mask-pattern-code {
  font-size: 12px; padding: 2px 6px; background: #F5F5F5;
  border-radius: 4px; color: #722ED1; max-width: 200px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block;
}

.field-tip { margin-left: 8px; font-size: 12px; color: #8C8C8C; }
.form-help { font-size: 12px; color: #8C8C8C; margin-top: 4px; line-height: 1.5; }
.form-help code { padding: 0 4px; background: #F5F5F5; border-radius: 3px; color: #722ED1; font-size: 11px; }

.mask-type-option { display: flex; align-items: center; gap: 8px; }
.mask-type-dot { width: 8px; height: 8px; border-radius: 50%; }
.mask-pos-option { display: flex; align-items: center; gap: 8px; }
.mask-pos-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; background: #722ED1; color: #fff;
  border-radius: 4px; font-size: 11px; font-weight: 600;
}

.preview-title { font-weight: 600; font-size: 14px; color: #FA541C; }
.preview-area { background: #FAFAFA; border: 1px solid #F0F0F0; border-radius: 8px; padding: 16px; margin-bottom: 8px; }
.preview-result { min-height: 40px; padding: 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-family: 'JetBrains Mono', 'Consolas', monospace; font-size: 14px; color: #722ED1; word-break: break-all; }

.test-modal-header { padding: 16px; background: linear-gradient(135deg, #722ED110 0%, #fff 100%); border: 1px solid #722ED120; border-radius: 8px; margin-bottom: 16px; }
.test-template-info { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.test-template-code { font-family: 'JetBrains Mono', monospace; font-size: 12px; color: #8C8C8C; }
.test-pattern-info { display: flex; align-items: center; gap: 4px; }
.test-inputs { max-height: 400px; overflow-y: auto; }
.test-case-item { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px dashed #E8E8E8; }
.test-case-item:last-child { border-bottom: none; }
.test-case-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.test-case-label { font-size: 13px; font-weight: 500; color: #595959; }
.test-result-box { min-height: 40px; padding: 10px 12px; background: linear-gradient(135deg, rgba(114,46,209,0.05) 0%, rgba(146,84,222,0.08) 100%); border: 1px solid rgba(114,46,209,0.2); border-radius: 6px; font-family: 'JetBrains Mono', 'Consolas', monospace; font-size: 14px; color: #722ED1; word-break: break-all; margin-top: 8px; }
.test-actions { display: flex; align-items: center; gap: 8px; margin-top: 16px; }

.table-empty { padding: 48px 0; text-align: center; }
.empty-icon { font-size: 48px; color: #d9d9d9; margin-bottom: 12px; display: block; }
.empty-title { font-size: 16px; color: #595959; margin-bottom: 8px; }
.empty-desc { font-size: 13px; color: #8C8C8C; margin-bottom: 16px; }
</style>
