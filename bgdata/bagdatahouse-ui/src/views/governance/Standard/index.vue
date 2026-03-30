<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#stdGrad)"/>
            <path d="M9 12L11 14L15 10M9 16H15" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="12" r="8" stroke="white" stroke-width="1.5" opacity="0.3"/>
            <defs>
              <linearGradient id="stdGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FAAD14"/>
                <stop offset="100%" stop-color="#FFC53D"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据标准管理</h1>
          <p class="page-subtitle">建立编码标准、命名规范、主数据管理规范，支持元数据绑定与合规检测</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="openAddDrawer">
          <template #icon><PlusOutlined /></template>
          新建标准
        </a-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 20px">
      <a-col :xs="12" :sm="8" :md="6" v-for="stat in statCards" :key="stat.label">
        <div class="stat-mini-card" :style="{ background: stat.bg }">
          <div class="mini-value">{{ stat.value }}</div>
          <div class="mini-label">{{ stat.label }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- 筛选区 -->
    <div class="filter-card">
      <div class="filter-bar">
        <a-space wrap>
          <a-select
            v-model:value="filterType"
            placeholder="标准类型"
            style="width: 140px"
            allowClear
            @change="loadData"
          >
            <a-select-option value="CODE_STANDARD">编码标准</a-select-option>
            <a-select-option value="NAMING_STANDARD">命名规范</a-select-option>
            <a-select-option value="PRIMARY_DATA">主数据</a-select-option>
          </a-select>
          <a-select
            v-model:value="filterStatus"
            placeholder="状态"
            style="width: 120px"
            allowClear
            @change="loadData"
          >
            <a-select-option value="DRAFT">草稿</a-select-option>
            <a-select-option value="PUBLISHED">已发布</a-select-option>
            <a-select-option value="DEPRECATED">已废弃</a-select-option>
          </a-select>
          <a-select
            v-model:value="filterEnabled"
            placeholder="是否启用"
            style="width: 120px"
            allowClear
            @change="loadData"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索标准名称/编码"
            style="width: 200px"
            allowClear
            @pressEnter="loadData"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-button @click="resetFilters">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
      </div>
      <div class="filter-tags" v-if="hasActiveFilters">
        <span class="filter-tip">筛选条件：</span>
        <a-tag v-if="filterType" closable @close="filterType = undefined; loadData()">
          类型: {{ getTypeLabel(filterType) }}
        </a-tag>
        <a-tag v-if="filterStatus" closable @close="filterStatus = undefined; loadData()">
          状态: {{ getStatusLabel(filterStatus) }}
        </a-tag>
        <a-tag v-if="filterEnabled !== undefined" closable @close="filterEnabled = undefined; loadData()">
          {{ filterEnabled === 1 ? '已启用' : '已禁用' }}
        </a-tag>
        <a-tag v-if="searchKeyword" closable @close="searchKeyword = ''; loadData()">
          关键词: {{ searchKeyword }}
        </a-tag>
      </div>
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
          <!-- 标准类型 -->
          <template v-if="column.key === 'standardType'">
            <a-tag :color="getTypeColor(record.standardType)">
              {{ record.standardTypeLabel || record.standardType }}
            </a-tag>
          </template>

          <!-- 标准名称 -->
          <template v-if="column.key === 'standardName'">
            <div class="standard-name-cell">
              <div class="name-text">{{ record.standardName }}</div>
              <div class="code-text">{{ record.standardCode }}</div>
            </div>
          </template>

          <!-- 状态 -->
          <template v-if="column.key === 'status'">
            <a-badge
              :status="getStatusBadge(record.status)"
              :text="record.statusLabel || record.status"
            />
          </template>

          <!-- 启用状态 -->
          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled === 1"
              size="small"
              @change="(checked: boolean) => handleToggle(record, checked)"
            />
          </template>

          <!-- 绑定数量 -->
          <template v-if="column.key === 'bindingCount'">
            <span class="binding-count" :class="{ 'has-bindings': record.bindingCount > 0 }">
              {{ record.bindingCount || 0 }}
            </span>
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-tooltip title="查看详情">
                <a-button type="text" size="small" @click="openDetailDrawer(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="text" size="small" @click="openEditDrawer(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown :trigger="['click']">
                <a-button type="text" size="small">
                  <template #icon><MoreOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleAction(key as string, record)">
                    <a-menu-item key="publish" v-if="record.status === 'DRAFT'">
                      <CheckCircleOutlined /> 发布
                    </a-menu-item>
                    <a-menu-item key="deprecate" v-if="record.status === 'PUBLISHED'">
                      <StopOutlined /> 废弃
                    </a-menu-item>
                    <a-menu-item key="copy">
                      <CopyOutlined /> 复制
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="bind">
                      <LinkOutlined /> 绑定元数据
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete" style="color: #FF4D4F" v-if="record.status !== 'PUBLISHED'">
                      <DeleteOutlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <div class="empty-state">
            <FileSearchOutlined class="empty-icon" />
            <div class="empty-title">暂无数据标准</div>
            <div class="empty-desc">建立编码标准、命名规范或主数据标准，统一企业数据定义</div>
            <a-button type="primary" @click="openAddDrawer">
              <template #icon><PlusOutlined /></template>
              新建标准
            </a-button>
          </div>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑标准抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerMode === 'add' ? '新建数据标准' : '编辑数据标准'"
      :width="600"
      @close="drawerVisible = false"
    >
      <a-form
        ref="formRef"
        :model="form"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="formRules"
      >
        <a-form-item label="标准类型" name="standardType">
          <a-select v-model:value="form.standardType" placeholder="选择标准类型" :disabled="drawerMode === 'edit'">
            <a-select-option value="CODE_STANDARD">编码标准</a-select-option>
            <a-select-option value="NAMING_STANDARD">命名规范</a-select-option>
            <a-select-option value="PRIMARY_DATA">主数据</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="标准编码" name="standardCode" v-if="drawerMode === 'add'">
          <a-input v-model:value="form.standardCode" placeholder="输入标准编码，唯一标识" :maxlength="50" show-count />
          <div class="form-tip">建议使用英文，如 "phone_number_format"</div>
        </a-form-item>

        <a-form-item label="标准名称" name="standardName">
          <a-input v-model:value="form.standardName" placeholder="输入标准名称" :maxlength="100" show-count />
        </a-form-item>

        <a-form-item label="标准分类" name="standardCategory">
          <a-input v-model:value="form.standardCategory" placeholder="输入标准分类，如" :maxlength="50" />
          <div class="form-tip">如"电话号码编码"、"表名命名"等</div>
        </a-form-item>

        <a-form-item label="标准描述" name="standardDesc">
          <a-textarea v-model:value="form.standardDesc" placeholder="简要描述标准的用途" :rows="2" :maxlength="500" show-count />
        </a-form-item>

        <!-- 编码标准专属 -->
        <template v-if="form.standardType === 'CODE_STANDARD'">
          <a-divider orientation="left">编码规则配置</a-divider>
          <a-form-item label="编码正则" name="ruleExpr">
            <a-textarea
              v-model:value="form.ruleExpr"
              placeholder="填写正则表达式，如 ^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$"
              :rows="3"
              :maxlength="500"
              show-count
            />
            <div class="form-tip">用于校验字段值是否符合编码规范</div>
          </a-form-item>
          <a-form-item label="编码示例" name="exampleValue">
            <a-input v-model:value="form.exampleValue" placeholder="填写符合规范的示例值" />
            <div class="form-tip">如 01=启用，02=禁用</div>
          </a-form-item>
        </template>

        <!-- 命名规范专属 -->
        <template v-if="form.standardType === 'NAMING_STANDARD'">
          <a-divider orientation="left">命名规范配置</a-divider>
          <a-form-item label="标准内容" name="standardContent">
            <a-textarea
              v-model:value="form.standardContent"
              placeholder="填写命名规范描述，如：表名统一用 dim_ / fact_ 前缀"
              :rows="3"
              :maxlength="1000"
              show-count
            />
          </a-form-item>
          <a-form-item label="命名正则" name="ruleExpr">
            <a-input
              v-model:value="form.ruleExpr"
              placeholder="填写命名正则，如 ^(?i)(dim_|fact_)[a-z_]+$"
            />
            <div class="form-tip">用于校验表名/字段名是否符合命名规范</div>
          </a-form-item>
          <a-form-item label="适用对象" name="applicableObject">
            <a-select v-model:value="form.applicableObject" placeholder="选择适用对象">
              <a-select-option value="TABLE_NAME">表名</a-select-option>
              <a-select-option value="COLUMN_NAME">字段名</a-select-option>
              <a-select-option value="DATA_VALUE">数据值</a-select-option>
            </a-select>
          </a-form-item>
        </template>

        <!-- 主数据专属 -->
        <template v-if="form.standardType === 'PRIMARY_DATA'">
          <a-divider orientation="left">主数据配置</a-divider>
          <a-form-item label="标准内容" name="standardContent">
            <a-textarea
              v-model:value="form.standardContent"
              placeholder="填写主数据标准描述，如客户主数据统一管理"
              :rows="3"
              :maxlength="1000"
              show-count
            />
          </a-form-item>
          <a-form-item label="主数据字段" name="exampleValue">
            <a-input v-model:value="form.exampleValue" placeholder="填写主数据唯一标识字段，如 customer_id" />
          </a-form-item>
        </template>

        <a-divider orientation="left">管理信息</a-divider>

        <a-form-item label="不合规处理" name="enforceAction">
          <a-select v-model:value="form.enforceAction" placeholder="选择不合规时的处理方式">
            <a-select-option value="ALERT">仅告警</a-select-option>
            <a-select-option value="BLOCK">阻断创建</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-select v-model:value="form.status" placeholder="选择状态">
            <a-select-option value="DRAFT">草稿</a-select-option>
            <a-select-option value="PUBLISHED">已发布</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="启用" name="enabled">
          <a-switch v-model:checked="formEnabled" />
        </a-form-item>

        <a-form-item label="排序号" name="sortOrder">
          <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">
            {{ drawerMode === 'add' ? '创建' : '保存' }}
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 标准详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="`标准详情: ${currentRecord?.standardName || ''}`"
      :width="720"
      @close="detailDrawerVisible = false"
    >
      <div v-if="currentRecord" class="detail-content">
        <a-descriptions :column="2" size="small" bordered title="基本信息">
          <a-descriptions-item label="标准编码" :span="2">
            <code class="code-text">{{ currentRecord.standardCode }}</code>
          </a-descriptions-item>
          <a-descriptions-item label="标准类型">
            <a-tag :color="getTypeColor(currentRecord.standardType)">
              {{ currentRecord.standardTypeLabel }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="标准分类">{{ currentRecord.standardCategory || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="getStatusBadge(currentRecord.status)" :text="currentRecord.statusLabel" />
          </a-descriptions-item>
          <a-descriptions-item label="启用状态">
            <a-tag :color="currentRecord.enabled === 1 ? 'green' : 'default'">
              {{ currentRecord.enabled === 1 ? '已启用' : '已禁用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="绑定数量">
            <span class="binding-count" :class="{ 'has-bindings': currentRecord.bindingCount > 0 }">
              {{ currentRecord.bindingCount || 0 }} 个
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="不合规处理">
            {{ currentRecord.enforceActionLabel || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="负责人">{{ currentRecord.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="业务域">{{ currentRecord.bizDomain || '-' }}</a-descriptions-item>
          <a-descriptions-item label="标准描述" :span="2">{{ currentRecord.standardDesc || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 编码规范内容 -->
        <template v-if="currentRecord.standardType === 'CODE_STANDARD'">
          <a-divider>编码规则</a-divider>
          <div class="rule-content">
            <div class="rule-label">正则表达式</div>
            <code class="rule-code" v-if="currentRecord.ruleExpr">{{ currentRecord.ruleExpr }}</code>
            <div class="rule-empty" v-else>未配置</div>
            <div class="rule-label" style="margin-top: 12px">示例值</div>
            <div class="rule-value">{{ currentRecord.exampleValue || '-' }}</div>
          </div>
        </template>

        <!-- 命名规范内容 -->
        <template v-if="currentRecord.standardType === 'NAMING_STANDARD'">
          <a-divider>命名规范</a-divider>
          <div class="rule-content">
            <div class="rule-label">规范内容</div>
            <div class="rule-text">{{ currentRecord.standardContent || '-' }}</div>
            <div class="rule-label" style="margin-top: 12px">正则表达式</div>
            <code class="rule-code" v-if="currentRecord.ruleExpr">{{ currentRecord.ruleExpr }}</code>
            <div class="rule-empty" v-else>未配置</div>
            <div class="rule-label" style="margin-top: 12px">适用对象</div>
            <div class="rule-value">{{ currentRecord.applicableObjectLabel || '-' }}</div>
          </div>
        </template>

        <!-- 主数据内容 -->
        <template v-if="currentRecord.standardType === 'PRIMARY_DATA'">
          <a-divider>主数据信息</a-divider>
          <div class="rule-content">
            <div class="rule-label">标准内容</div>
            <div class="rule-text">{{ currentRecord.standardContent || '-' }}</div>
            <div class="rule-label" style="margin-top: 12px">标识字段</div>
            <div class="rule-value">{{ currentRecord.exampleValue || '-' }}</div>
          </div>
        </template>

        <!-- 绑定列表 -->
        <a-divider>
          绑定元数据
          <a-badge :count="currentRecord.bindingCount || 0" :number-style="{ backgroundColor: '#1677FF' }" />
        </a-divider>

        <div class="binding-section">
          <div class="binding-header">
            <a-button size="small" @click="openBindDrawer">
              <template #icon><PlusOutlined /></template>
              绑定元数据
            </a-button>
          </div>

          <a-table
            :columns="bindingColumns"
            :data-source="bindingData"
            :pagination="{ pageSize: 5 }"
            :row-key="(record: any) => record.id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'complianceStatus'">
                <a-tag :color="getComplianceColor(record.complianceStatus)">
                  {{ record.complianceStatusLabel || record.complianceStatus }}
                </a-tag>
              </template>
              <template v-if="column.key === 'violationCount'">
                <span v-if="record.violationCount > 0" style="color: #FF4D4F; font-weight: 600">
                  {{ record.violationCount }}
                </span>
                <span v-else style="color: #52C41A">0</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-popconfirm
                  title="确定解绑该记录？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleUnbind(record)"
                >
                  <a-button type="text" size="small" danger>
                    <template #icon><DisconnectOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </template>
            </template>
            <template #emptyText>
              <div class="binding-empty">
                <span style="color: #8C8C8C">暂无绑定记录</span>
              </div>
            </template>
          </a-table>
        </div>
      </div>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="openEditDrawer(currentRecord)">
            <template #icon><EditOutlined /></template>
            编辑
          </a-button>
          <a-button
            v-if="currentRecord?.status === 'DRAFT'"
            type="primary"
            @click="handlePublish(currentRecord)"
          >
            <template #icon><CheckCircleOutlined /></template>
            发布
          </a-button>
          <a-button
            v-if="currentRecord?.status === 'PUBLISHED'"
            @click="handleDeprecate(currentRecord)"
          >
            <template #icon><StopOutlined /></template>
            废弃
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 绑定元数据抽屉 -->
    <a-drawer
      v-model:open="bindDrawerVisible"
      title="绑定元数据"
      :width="720"
      @close="bindDrawerVisible = false"
    >
      <div class="bind-picker">
        <div class="picker-header">
          <a-space wrap>
            <a-select
              v-model:value="bindDsId"
              placeholder="选择数据源"
              style="width: 180px"
              allowClear
              :loading="dsLoading"
              @change="loadBindTables"
            >
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                {{ ds.dsName }} ({{ ds.dsType }})
              </a-select-option>
            </a-select>
            <a-input
              v-model:value="bindKeyword"
              placeholder="搜索表名"
              style="width: 160px"
              allowClear
              @pressEnter="loadBindTables"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-button @click="loadBindTables">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
          </a-space>
        </div>

        <a-transfer
          v-model:target-keys="selectedMetadataKeys"
          :data-source="bindTransferData"
          :titles="['可选元数据', '已选元数据']"
          :render="(item: any) => item.title"
          :row-key="(item: any) => item.key"
          :list-style="{ width: '280px', height: '360px' }"
          show-search
          :filter-option="(input: string, item: any) => item.title.toLowerCase().indexOf(input.toLowerCase()) >= 0"
          @change="handleBindTransferChange"
        />
      </div>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="bindDrawerVisible = false">取消</a-button>
          <a-button
            type="primary"
            :loading="binding"
            @click="handleBind"
            :disabled="selectedMetadataKeys.length === 0"
          >
            确认绑定 ({{ selectedMetadataKeys.length }})
          </a-button>
        </a-space>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, SearchOutlined, ReloadOutlined, EyeOutlined,
  EditOutlined, DeleteOutlined, MoreOutlined, CopyOutlined,
  CheckCircleOutlined, StopOutlined, LinkOutlined, DisconnectOutlined,
  FileSearchOutlined
} from '@ant-design/icons-vue'
import { dataStandardApi } from '@/api/standard'
import type { DataStandardVO, DataStandardDetailVO, DataStandardBindingVO, DataStandardSaveDTO } from '@/api/standard'
import { dataSourceApi } from '@/api/dqc'
import { metadataApi } from '@/api/gov'

// ============ 状态 ============
const loading = ref(false)
const saving = ref(false)
const binding = ref(false)
const dsLoading = ref(false)
const tableData = ref<DataStandardVO[]>([])
const currentRecord = ref<DataStandardDetailVO | null>(null)
const bindingData = ref<DataStandardBindingVO[]>([])

// ============ 筛选 ============
const filterType = ref<string | undefined>()
const filterStatus = ref<string | undefined>()
const filterEnabled = ref<number | undefined>()
const searchKeyword = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const hasActiveFilters = computed(() => {
  return !!(filterType.value || filterStatus.value || filterEnabled.value !== undefined || searchKeyword.value)
})

// ============ 抽屉 ============
const drawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const bindDrawerVisible = ref(false)
const drawerMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const formEnabled = ref(true)

const form = reactive<DataStandardSaveDTO>({
  standardType: undefined,
  standardCode: '',
  standardName: '',
  standardCategory: '',
  standardDesc: '',
  standardContent: '',
  ruleExpr: '',
  exampleValue: '',
  applicableObject: undefined,
  enforceAction: undefined,
  status: 'DRAFT',
  enabled: 1,
  sortOrder: 0
})

const formRules = {
  standardType: [{ required: true, message: '请选择标准类型' }],
  standardCode: [{ required: true, message: '请输入标准编码' }],
  standardName: [{ required: true, message: '请输入标准名称' }]
}

// ============ 绑定元数据 ============
const bindDsId = ref<number | undefined>()
const bindKeyword = ref('')
const selectedMetadataKeys = ref<string[]>([])
const bindTransferData = ref<any[]>([])
const dataSourceList = ref<any[]>([])

// ============ 统计数据 ============
const statCards = ref([
  { label: '标准总数', value: 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' },
  { label: '已发布', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '草稿', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '已废弃', value: 0, bg: 'linear-gradient(135deg, #8C8C8C 0%, #A6A6A6 100%)' }
])

// ============ 表格列 ============
const columns = [
  { title: '标准名称', key: 'standardName', width: 240, ellipsis: true },
  { title: '类型', key: 'standardType', width: 120 },
  { title: '分类', dataIndex: 'standardCategory', width: 120, ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '启用', key: 'enabled', width: 80, align: 'center' },
  { title: '绑定数', key: 'bindingCount', width: 90, align: 'center' },
  { title: '负责人', dataIndex: 'ownerName', width: 100, ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const bindingColumns = [
  { title: '数据源', dataIndex: 'dsName', width: 120, ellipsis: true },
  { title: '表名', dataIndex: 'targetTable', width: 160, ellipsis: true },
  { title: '字段', dataIndex: 'targetColumn', width: 120, ellipsis: true },
  { title: '合规状态', key: 'complianceStatus', width: 100 },
  { title: '违规次数', key: 'violationCount', width: 90, align: 'center' },
  { title: '最近检测', dataIndex: 'lastCheckTime', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 60, align: 'center' }
]

// ============ 加载数据 ============
async function loadData() {
  loading.value = true
  try {
    const res = await dataStandardApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      standardType: filterType.value as any,
      status: filterStatus.value as any,
      enabled: filterEnabled.value,
      standardName: searchKeyword.value || undefined
    })
    if (res.data?.success !== false) {
      tableData.value = (res.data as any)?.records || []
      pagination.total = (res.data as any)?.total || 0
    }
  } catch {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await dataStandardApi.getStats()
    if (res.data?.success !== false) {
      const s = res.data?.data
      statCards.value[0].value = s?.totalCount || 0
      statCards.value[1].value = s?.publishedCount || 0
      statCards.value[2].value = s?.draftCount || 0
      statCards.value[3].value = s?.deprecatedCount || 0
    }
  } catch { /* ignore */ }
}

async function loadBindings(standardId: number) {
  try {
    const res = await dataStandardApi.getBindings(standardId)
    if (res.data?.success !== false) {
      bindingData.value = (res.data as any)?.data || []
    }
  } catch { bindingData.value = [] }
}

// ============ 加载数据源 ============
async function loadDataSources() {
  dsLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.data?.success !== false) {
      dataSourceList.value = (res.data as any)?.data || []
    }
  } catch { /* ignore */ }
  finally { dsLoading.value = false }
}

// ============ 加载绑定可选元数据 ============
async function loadBindTables() {
  if (!bindDsId.value) {
    bindTransferData.value = []
    return
  }
  try {
    const res = await metadataApi.page({
      pageNum: 1,
      pageSize: 500,
      dsId: bindDsId.value,
      tableName: bindKeyword.value || undefined,
      status: 'ACTIVE'
    })
    if (res.data?.success !== false) {
      const records = (res.data as any)?.records || []
      bindTransferData.value = records.map((r: any) => ({
        key: `${r.id}`,
        title: `${r.tableName}${r.tableAlias ? ' (' + r.tableAlias + ')' : ''}`,
        description: `${r.dataLayer || ''} | ${r.tableComment || ''}`
      }))
    }
  } catch { /* ignore */ }
}

// ============ 抽屉操作 ============
function openAddDrawer() {
  drawerMode.value = 'add'
  Object.assign(form, {
    id: undefined,
    standardType: undefined,
    standardCode: '',
    standardName: '',
    standardCategory: '',
    standardDesc: '',
    standardContent: '',
    ruleExpr: '',
    exampleValue: '',
    applicableObject: undefined,
    enforceAction: undefined,
    status: 'DRAFT',
    enabled: 1,
    sortOrder: 0
  })
  formEnabled.value = true
  drawerVisible.value = true
}

function openEditDrawer(record: DataStandardVO | null | undefined) {
  if (!record) return
  drawerMode.value = 'edit'
  Object.assign(form, {
    id: record.id,
    standardType: record.standardType as any,
    standardCode: record.standardCode || '',
    standardName: record.standardName || '',
    standardCategory: record.standardCategory || '',
    standardDesc: record.standardDesc || '',
    standardContent: (record as any).standardContent || '',
    ruleExpr: (record as any).ruleExpr || '',
    exampleValue: (record as any).exampleValue || '',
    applicableObject: (record as any).applicableObject as any,
    enforceAction: (record as any).enforceAction as any,
    status: record.status as any,
    enabled: record.enabled,
    sortOrder: record.sortOrder || 0
  })
  formEnabled.value = form.enabled === 1
  drawerVisible.value = true
}

async function openDetailDrawer(record: DataStandardVO) {
  currentRecord.value = null
  detailDrawerVisible.value = true
  try {
    const res = await dataStandardApi.getDetail(record.id!)
    if (res.data?.success !== false) {
      currentRecord.value = res.data?.data as DataStandardDetailVO
      loadBindings(record.id!)
    }
  } catch {
    currentRecord.value = record as DataStandardDetailVO
  }
}

function openBindDrawer() {
  if (!currentRecord.value?.id) return
  selectedMetadataKeys.value = []
  bindDsId.value = undefined
  bindKeyword.value = ''
  bindTransferData.value = []
  bindDrawerVisible.value = true
}

// ============ 保存 ============
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  form.enabled = formEnabled.value ? 1 : 0
  try {
    if (drawerMode.value === 'add') {
      const res = await dataStandardApi.create(form as DataStandardSaveDTO)
      if (res.data?.success !== false) {
        message.success('创建成功')
        drawerVisible.value = false
        loadData()
        loadStats()
      }
    } else {
      const res = await dataStandardApi.update(form.id!, form as DataStandardSaveDTO)
      if (res.data?.success !== false) {
        message.success('保存成功')
        drawerVisible.value = false
        loadData()
        loadStats()
      }
    }
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ============ 操作 ============
async function handleToggle(record: DataStandardVO, checked: boolean) {
  try {
    const res = checked
      ? await dataStandardApi.enable(record.id!)
      : await dataStandardApi.disable(record.id!)
    if (res.data?.success !== false) {
      message.success(checked ? '已启用' : '已禁用')
      loadData()
      loadStats()
    }
  } catch {
    message.error('操作失败')
    loadData()
  }
}

async function handleAction(key: string, record: DataStandardVO) {
  switch (key) {
    case 'publish':
      await handlePublish(record)
      break
    case 'deprecate':
      await handleDeprecate(record)
      break
    case 'copy':
      await handleCopy(record)
      break
    case 'bind':
      await openBindDrawer()
      currentRecord.value = record as any
      break
    case 'delete':
      await handleDelete(record)
      break
  }
}

async function handlePublish(record: DataStandardVO | null | undefined) {
  if (!record?.id) return
  try {
    await Modal.confirm({
      title: '确认发布',
      content: `确定发布标准「${record.standardName}」？发布后可以绑定元数据。`,
      okText: '确定',
      cancelText: '取消'
    })
  } catch { return }
  try {
    const res = await dataStandardApi.publish(record.id)
    if (res.data?.success !== false) {
      message.success('发布成功')
      loadData()
      loadStats()
      if (currentRecord.value?.id === record.id) {
        currentRecord.value.status = 'PUBLISHED'
        currentRecord.value.statusLabel = '已发布'
      }
    }
  } catch { /* cancel */ }
}

async function handleDeprecate(record: DataStandardVO | null | undefined) {
  if (!record?.id) return
  try {
    await Modal.confirm({
      title: '确认废弃',
      content: `确定废弃标准「${record.standardName}」？废弃后将不可再使用。`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true }
    })
  } catch { return }
  try {
    const res = await dataStandardApi.deprecate(record.id)
    if (res.data?.success !== false) {
      message.success('已废弃')
      loadData()
      loadStats()
      if (currentRecord.value?.id === record.id) {
        currentRecord.value.status = 'DEPRECATED'
        currentRecord.value.statusLabel = '已废弃'
      }
    }
  } catch { /* cancel */ }
}

async function handleCopy(record: DataStandardVO) {
  try {
    const res = await dataStandardApi.copy(record.id!)
    if (res.data?.success !== false) {
      message.success('复制成功，已创建草稿')
      loadData()
    }
  } catch {
    message.error('复制失败')
  }
}

async function handleDelete(record: DataStandardVO) {
  try {
    await Modal.confirm({
      title: '确认删除',
      content: `确定删除标准「${record.standardName}」？删除后将同时删除所有绑定记录。`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true }
    })
  } catch {
    return
  }
  try {
    const res = await dataStandardApi.delete(record.id!)
    if (res.data?.success !== false) {
      message.success('删除成功')
      loadData()
      loadStats()
    }
  } catch {
    message.error('删除失败')
  }
}

function handleBindTransferChange(targetKeys: string[]) {
  selectedMetadataKeys.value = targetKeys
}

async function handleBind() {
  if (!currentRecord.value?.id || selectedMetadataKeys.value.length === 0) return
  binding.value = true
  try {
    const res = await dataStandardApi.batchBind(
      currentRecord.value.id,
      selectedMetadataKeys.value.map(k => parseInt(k)),
      form.enforceAction as any
    )
    if (res.data?.success !== false) {
      message.success(`成功绑定 ${selectedMetadataKeys.value.length} 个元数据`)
      bindDrawerVisible.value = false
      loadBindings(currentRecord.value.id)
      loadData()
      loadStats()
    }
  } catch {
    message.error('绑定失败')
  } finally {
    binding.value = false
  }
}

async function handleUnbind(record: DataStandardBindingVO) {
  try {
    const res = await dataStandardApi.unbind(record.id!)
    if (res.data?.success !== false) {
      message.success('已解绑')
      if (currentRecord.value?.id) {
        loadBindings(currentRecord.value.id)
      }
      loadData()
      loadStats()
    }
  } catch {
    message.error('解绑失败')
  }
}

// ============ 辅助 ============
function resetFilters() {
  filterType.value = undefined
  filterStatus.value = undefined
  filterEnabled.value = undefined
  searchKeyword.value = ''
  pagination.current = 1
  loadData()
}

function handleTableChange(pagination: any) {
  pagination.current = pagination.current
  pagination.pageSize = pagination.pageSize
  loadData()
}

function getTypeLabel(type?: string) {
  const map: Record<string, string> = {
    CODE_STANDARD: '编码标准',
    NAMING_STANDARD: '命名规范',
    PRIMARY_DATA: '主数据'
  }
  return map[type || ''] || type || '-'
}

function getTypeColor(type?: string) {
  const map: Record<string, string> = {
    CODE_STANDARD: 'blue',
    NAMING_STANDARD: 'green',
    PRIMARY_DATA: 'purple'
  }
  return map[type || ''] || 'default'
}

function getStatusLabel(status?: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    DEPRECATED: '已废弃'
  }
  return map[status || ''] || status || '-'
}

function getStatusBadge(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'error' | 'default' | 'processing' | undefined> = {
    DRAFT: 'warning',
    PUBLISHED: 'success',
    DEPRECATED: 'default'
  }
  return map[status || ''] || 'default'
}

function getComplianceColor(status?: string) {
  const map: Record<string, string> = {
    PENDING: 'default',
    COMPLIANT: 'green',
    NON_COMPLIANT: 'red'
  }
  return map[status || ''] || 'default'
}

onMounted(() => {
  loadData()
  loadStats()
  loadDataSources()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.page-title { font-size: 20px; font-weight: 700; color: #1F1F1F; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: #8C8C8C; margin: 0; }

.stat-mini-card {
  border-radius: 8px;
  padding: 16px 20px;
  color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.mini-value { font-size: 24px; font-weight: 700; }
.mini-label { font-size: 12px; opacity: 0.9; margin-top: 4px; }

.filter-card {
  background: white;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.filter-bar { margin-bottom: 0; }
.filter-tags { margin-top: 8px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.filter-tip { font-size: 12px; color: #8C8C8C; }

.table-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.standard-name-cell { display: flex; flex-direction: column; gap: 2px; }
.name-text { font-weight: 500; }
.code-text { font-size: 12px; color: #8C8C8C; font-family: 'JetBrains Mono', monospace; }

.binding-count { color: #8C8C8C; }
.binding-count.has-bindings { color: #1677FF; font-weight: 600; }

.empty-state {
  text-align: center;
  padding: 60px 20px;
}
.empty-icon { font-size: 64px; color: #d9d9d9; margin-bottom: 16px; }
.empty-title { font-size: 16px; color: #1F1F1F; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #8C8C8C; margin-bottom: 24px; }

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: white;
  display: flex;
  justify-content: space-between;
}

.detail-content { padding-bottom: 70px; }

.rule-content {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 16px;
}
.rule-label { font-size: 13px; color: #8C8C8C; margin-bottom: 6px; }
.rule-code {
  display: block;
  background: #1C1F2E;
  color: #E8F4FF;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  word-break: break-all;
}
.rule-empty { color: #8C8C8C; font-style: italic; font-size: 13px; }
.rule-text { font-size: 13px; color: #1F1F1F; white-space: pre-wrap; }
.rule-value { font-size: 13px; color: #1F1F1F; font-family: 'JetBrains Mono', monospace; }

.binding-section { }
.binding-header { margin-bottom: 12px; }
.binding-empty { text-align: center; padding: 20px; }

.bind-picker { display: flex; flex-direction: column; gap: 12px; }
.picker-header { display: flex; flex-wrap: wrap; gap: 8px; }

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }

code.code-text {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}
</style>
