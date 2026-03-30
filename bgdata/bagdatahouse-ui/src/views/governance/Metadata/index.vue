<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#metaGrad)"/>
            <path d="M12 7L12 17M7 12L17 12" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <rect x="7" y="7" width="10" height="10" rx="2" stroke="white" stroke-width="1.5" opacity="0.6"/>
            <defs>
              <linearGradient id="metaGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#1677FF"/>
                <stop offset="100%" stop-color="#00B4DB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">元数据管理</h1>
          <p class="page-subtitle">自动采集外部数据源表结构信息，支持 MySQL / SQL Server / Oracle / PostgreSQL / TiDB</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="openScanDrawer">
          <template #icon><ScanOutlined /></template>
          扫描元数据
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
    <div class="filter-bar">
      <a-space wrap>
        <a-select
          v-model:value="filterDsId"
          placeholder="选择数据源"
          style="width: 180px"
          allowClear
          :loading="dsLoading"
          @change="loadData"
        >
          <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
            {{ ds.dsName }} ({{ ds.dsType }})
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterLayer"
          placeholder="数据层"
          style="width: 120px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部层</a-select-option>
          <a-select-option v-for="l in layerOptions" :key="l.value" :value="l.value">
            {{ l.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filterSensitive"
          placeholder="敏感字段"
          style="width: 120px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="true">敏感</a-select-option>
          <a-select-option value="false">非敏感</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterStatus"
          placeholder="状态"
          style="width: 110px"
          allowClear
          @change="loadData"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="ACTIVE">活跃</a-select-option>
          <a-select-option value="ARCHIVED">归档</a-select-option>
          <a-select-option value="DEPRECATED">废弃</a-select-option>
        </a-select>
        <a-input
          v-model:value="searchTableName"
          placeholder="搜索表名"
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
          <template v-if="column.key === 'tableName'">
            <div class="table-name-cell">
              <FileOutlined class="table-icon" />
              <span class="table-name-text">{{ record.tableName }}</span>
            </div>
          </template>

          <template v-if="column.key === 'dataLayer'">
            <a-tag :color="getLayerColor(record.dataLayer)">
              {{ record.dataLayer || '-' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'sensitivityLevel'">
            <a-tag :color="getSensitivityColor(record.sensitivityLevel)">
              {{ getSensitivityLabel(record.sensitivityLevel) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'isSensitive'">
            <a-tag v-if="record.isSensitive" color="red" style="font-weight: 600">
              <template #icon><SafetyOutlined /></template>
              敏感
            </a-tag>
            <span v-else style="color: #d9d9d9">—</span>
          </template>

          <template v-if="column.key === 'rowCount'">
            <span class="mono-text">{{ formatNumber(record.rowCount) }}</span>
          </template>

          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'ACTIVE' ? 'success' : 'default'"
              :text="getStatusLabel(record.status)"
            />
          </template>

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
              <a-tooltip title="删除">
                <a-popconfirm
                  title="确定删除该元数据？删除后将同时删除关联字段信息"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleDelete(record)"
                >
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 扫描抽屉 -->
    <a-drawer
      v-model:open="scanDrawerVisible"
      title="扫描元数据"
      :width="520"
      @close="scanDrawerVisible = false"
    >
      <a-form
        ref="scanFormRef"
        :model="scanForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="数据源" name="dsId" :rules="[{ required: true, message: '请选择数据源' }]">
          <a-select
            v-model:value="scanForm.dsId"
            placeholder="请选择数据源"
            :loading="dsLoading"
            @change="onScanDsChange"
          >
            <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
              <span>{{ ds.dsName }}</span>
              <span style="color: #8C8C8C; margin-left: 8px">({{ ds.dsType }})</span>
            </a-select-option>
          </a-select>
          <div v-if="selectedScanDs" class="ds-instance-card">
            <div class="ds-instance-line">
              <span class="ds-instance-label">连接实例</span>
              <code>{{ selectedScanDs.host }}:{{ selectedScanDs.port ?? '—' }}</code>
            </div>
            <div class="ds-instance-line">
              <span class="ds-instance-label">库 / Schema</span>
              <span>{{ selectedScanDs.databaseName || '—' }}</span>
              <template v-if="selectedScanDs.dsType === 'POSTGRESQL' && selectedScanDs.schemaName">
                <span class="ds-instance-sep">·</span>
                <span>{{ selectedScanDs.schemaName }}</span>
              </template>
            </div>
          </div>
        </a-form-item>

        <a-form-item
          v-if="selectedScanDs?.dsType === 'POSTGRESQL' && scanForm.scanScope === 'SPECIFIED'"
          label="Schema"
        >
          <a-select
            v-model:value="scanForm.pgSchema"
            placeholder="选择要列出表的 Schema（与敏感识别一致）"
            :loading="schemaLoading"
            show-search
            :options="schemaOptions"
            allow-clear
            @change="onPgSchemaChange"
          />
          <div class="form-tip">指定表扫描前需选择 Schema，以便从库中加载表清单</div>
        </a-form-item>

        <a-form-item label="扫描范围" name="scanScope">
          <a-radio-group v-model:value="scanForm.scanScope" @change="onScanScopeChange">
            <a-radio value="ALL">全部表</a-radio>
            <a-radio value="SPECIFIED">指定表</a-radio>
            <a-radio value="PATTERN">按模式匹配</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="scanForm.scanScope === 'SPECIFIED'" label="选择表" name="tableNames">
          <a-space direction="vertical" style="width: 100%">
            <a-select
              v-model:value="scanForm.selectedTableNames"
              mode="multiple"
              placeholder="请选择要扫描的表"
              :loading="physicalTableLoading"
              :options="physicalTableSelectOptions"
              :filter-option="filterTableOption"
              show-search
              allow-clear
              :max-tag-count="3"
              style="width: 100%"
            />
            <a-space>
              <a-button size="small" :loading="physicalTableLoading" :disabled="!scanForm.dsId" @click="loadPhysicalTablesForScan">
                刷新表列表
              </a-button>
              <a-button size="small" @click="openTablePickerModal">弹窗选择…</a-button>
            </a-space>
            <div class="form-tip">表名来自数据源实时查询；数据层请在扫描完成后在列表中编辑元数据时设置</div>
          </a-space>
        </a-form-item>

        <a-form-item v-if="scanForm.scanScope === 'PATTERN'" label="匹配模式" name="tablePattern">
          <a-input v-model:value="scanForm.tablePattern" placeholder="如 dim_% 或 %order%" />
          <div class="form-tip">支持 % 和 _ 通配符，% 匹配任意字符</div>
        </a-form-item>

        <a-divider>高级选项</a-divider>

        <a-form-item label="收集统计" name="collectStats">
          <a-switch v-model:checked="scanForm.collectStats" />
          <span class="switch-label">扫描时收集行数、存储大小等统计信息</span>
        </a-form-item>

        <a-form-item label="覆盖已有" name="overwriteExisting">
          <a-switch v-model:checked="scanForm.overwriteExisting" />
          <span class="switch-label">已存在的元数据将被覆盖更新</span>
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="scanDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="scanning" @click="handleScan">
            <template #icon><ScanOutlined /></template>
            开始扫描
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 扫描进度 -->
    <a-modal
      v-model:open="scanProgressVisible"
      :title="scanResult?.status === 'RUNNING' ? '正在扫描元数据' : '扫描结果'"
      :footer="null"
      :maskClosable="scanResult?.status !== 'RUNNING'"
      width="600"
      @cancel="onScanProgressModalClose"
    >
      <div class="scan-progress">
        <div v-if="scanResult?.status === 'RUNNING'" class="scan-running-block">
          <a-progress :percent="scanProgressPercent" :status="'active'" />
          <p class="scan-running-text">
            已处理 {{ scanResult.processedTableCount ?? 0 }} / {{ scanResult.plannedTableCount ?? 0 }} 张表
            <span v-if="scanResult.dsName">（{{ scanResult.dsName }}）</span>
          </p>
          <p class="form-tip">扫描在后台执行，请勿关闭浏览器；可随时关闭本窗口，任务仍将继续</p>
        </div>

        <a-result
          v-else-if="scanResult"
          :status="scanResult.status === 'SUCCESS' ? 'success' : scanResult.status === 'FAILED' ? 'error' : scanResult.status === 'PARTIAL_SUCCESS' ? 'warning' : 'info'"
          :title="getScanStatusTitle(scanResult.status)"
          :sub-title="getScanStatusSubTitle(scanResult)"
        >
          <template #extra>
            <a-descriptions :column="2" size="small" bordered>
              <a-descriptions-item label="数据源">{{ scanResult.dsName }}</a-descriptions-item>
              <a-descriptions-item label="耗时">{{ scanResult.elapsedMs }}ms</a-descriptions-item>
              <a-descriptions-item label="成功">{{ scanResult.successTables }} 表</a-descriptions-item>
              <a-descriptions-item label="新增">{{ scanResult.insertedTables }} 表</a-descriptions-item>
              <a-descriptions-item label="失败">{{ scanResult.failedTables }} 表</a-descriptions-item>
              <a-descriptions-item label="跳过">{{ scanResult.skippedTables }} 表</a-descriptions-item>
              <a-descriptions-item label="总字段数">{{ scanResult.totalColumns }}</a-descriptions-item>
            </a-descriptions>
          </template>
        </a-result>

        <div v-if="scanResult && scanResult.status !== 'RUNNING' && scanResult.errors && scanResult.errors.length > 0" class="scan-errors">
          <div class="error-title">失败详情</div>
          <div v-for="(err, idx) in scanResult.errors" :key="idx" class="error-item">
            <span class="error-table">{{ err.tableName }}</span>
            <span class="error-msg">{{ err.errorMessage }}</span>
          </div>
        </div>

        <div v-if="scanResult && scanResult.status !== 'RUNNING'" style="text-align: right; margin-top: 16px">
          <a-button type="primary" @click="closeScanProgressModal">关闭</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 指定表：弹窗多选 -->
    <a-modal
      v-model:open="tablePickerVisible"
      title="选择要扫描的表"
      width="720"
      ok-text="确定"
      cancel-text="取消"
      @ok="applyTablePicker"
      @cancel="tablePickerVisible = false"
    >
      <div style="margin-bottom: 12px">
        <a-input
          v-model:value="tablePickerSearch"
          placeholder="搜索表名"
          allow-clear
          style="max-width: 280px"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>
      <a-table
        size="small"
        :row-selection="tablePickerRowSelection"
        :columns="[{ title: '表名', dataIndex: 'name', key: 'name', ellipsis: true }]"
        :data-source="filteredPickerTableRows"
        :pagination="{ pageSize: 8 }"
        :row-key="(r: { name: string }) => r.name"
        :scroll="{ y: 320 }"
      />
    </a-modal>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="`表详情: ${currentRecord?.tableName || ''}`"
      :width="720"
      @close="detailDrawerVisible = false"
    >
      <div v-if="currentRecord" class="detail-content">
        <a-descriptions :column="2" size="small" bordered title="基本信息">
          <a-descriptions-item label="表名">{{ currentRecord.tableName }}</a-descriptions-item>
          <a-descriptions-item label="中文名">{{ currentRecord.tableAlias || '-' }}</a-descriptions-item>
          <a-descriptions-item label="表注释" :span="2">{{ currentRecord.tableComment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="表类型">{{ currentRecord.tableType }}</a-descriptions-item>
          <a-descriptions-item label="数据层">
            <a-tag :color="getLayerColor(currentRecord.dataLayer)">{{ currentRecord.dataLayer || '-' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="数据域">{{ currentRecord.dataDomain || '-' }}</a-descriptions-item>
          <a-descriptions-item label="行数">{{ formatNumber(currentRecord.rowCount) }}</a-descriptions-item>
          <a-descriptions-item label="存储大小">{{ formatBytes(currentRecord.storageBytes) }}</a-descriptions-item>
          <a-descriptions-item label="敏感等级">
            <a-tag :color="getSensitivityColor(currentRecord.sensitivityLevel)">
              {{ getSensitivityLabel(currentRecord.sensitivityLevel) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="currentRecord.status === 'ACTIVE' ? 'success' : 'default'" :text="getStatusLabel(currentRecord.status)" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
          <a-descriptions-item label="最后修改">{{ currentRecord.lastModifiedAt || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>字段列表</a-divider>

        <a-table
          :columns="columnColumns"
          :data-source="currentColumns"
          :pagination="{ pageSize: 10 }"
          :row-key="(record: any) => record.id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'isPrimaryKey'">
              <a-tag v-if="record.isPrimaryKey" color="gold">PK</a-tag>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'isNullable'">
              <span :class="record.isNullable ? 'nullable' : 'not-nullable'">
                {{ record.isNullable ? 'YES' : 'NO' }}
              </span>
            </template>
            <template v-if="column.key === 'isSensitive'">
              <a-tag v-if="record.isSensitive" color="red">敏感</a-tag>
            </template>
          </template>
        </a-table>

        <!-- 敏感字段区块 -->
        <a-divider>敏感字段</a-divider>
        <div v-if="sensitiveLoading" class="sensitive-loading">
          <a-spin size="small" /> 加载中...
        </div>
        <div v-else-if="sensitiveColumns.length === 0" class="sensitive-empty">
          <SafetyOutlined class="empty-icon" />
          <span>该表暂无敏感字段</span>
        </div>
        <div v-else class="sensitive-fields-block">
          <div class="sensitive-summary">
            <a-tag color="red">{{ sensitiveColumns.length }} 个敏感字段</a-tag>
          </div>
          <a-table
            :data-source="sensitiveColumns"
            :columns="sensitiveColumnsDef"
            :pagination="{ pageSize: 5 }"
            :row-key="(r: SecColumnSensitivityVO) => r.id || r.columnName"
            size="small"
            :scroll="{ x: 600 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'levelName'">
                <a-tag :color="record.levelColor || getSensitivityColor(record.levelId as unknown as string)">
                  {{ record.levelName || record.levelId }}
                </a-tag>
              </template>
              <template v-if="column.key === 'maskType'">
                <a-tag>{{ record.maskTypeLabel || record.maskType || '未配置' }}</a-tag>
              </template>
              <template v-if="column.key === 'reviewStatus'">
                <a-tag :color="record.reviewStatus === 'APPROVED' ? 'green' : record.reviewStatus === 'REJECTED' ? 'red' : 'orange'">
                  {{ record.reviewStatusLabel || record.reviewStatus || '待审核' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </a-drawer>

    <!-- 编辑抽屉 -->
    <a-drawer
      v-model:open="editDrawerVisible"
      title="编辑元数据"
      :width="520"
      @close="editDrawerVisible = false"
    >
      <a-form
        ref="editFormRef"
        :model="editForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="表名">
          <a-input :value="currentRecord?.tableName" disabled />
        </a-form-item>
        <a-form-item label="中文名" name="tableAlias">
          <a-input v-model:value="editForm.tableAlias" placeholder="填写表的中文名" />
        </a-form-item>
        <a-form-item label="表注释" name="tableComment">
          <a-textarea v-model:value="editForm.tableComment" :rows="2" />
        </a-form-item>
        <a-form-item label="数据层" name="dataLayer">
          <a-select v-model:value="editForm.dataLayer" placeholder="选择数据层">
            <a-select-option v-for="l in layerOptions" :key="l.value" :value="l.value">
              {{ l.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据域" name="dataDomain">
          <a-input v-model:value="editForm.dataDomain" placeholder="填写数据域" />
        </a-form-item>
        <a-form-item label="生命周期" name="lifecycleDays">
          <a-input-number v-model:value="editForm.lifecycleDays" :min="0" placeholder="0表示永久保留" style="width: 100%" />
        </a-form-item>
        <a-form-item label="敏感等级" name="sensitivityLevel">
          <a-select v-model:value="editForm.sensitivityLevel" placeholder="选择敏感等级">
            <a-select-option value="NORMAL">普通</a-select-option>
            <a-select-option value="INNER">内部</a-select-option>
            <a-select-option value="SENSITIVE">敏感</a-select-option>
            <a-select-option value="HIGHLY_SENSITIVE">高度敏感</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="标签" name="tags">
          <a-input v-model:value="editForm.tags" placeholder="多个标签用逗号分隔" />
        </a-form-item>
      </a-form>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="editDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSaveEdit">保存</a-button>
        </a-space>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import {
  ScanOutlined, SearchOutlined, ReloadOutlined,
  FileOutlined, EyeOutlined, EditOutlined, DeleteOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import { metadataApi } from '@/api/gov'
import type { Metadata, MetadataColumn, MetadataScanResult, MetadataStats } from '@/api/gov'
import { dataSourceApi } from '@/api/dqc'
import { getColumnSensitivityByTable, type SecColumnSensitivityVO } from '@/api/security'

// ============ 状态 ============
const loading = ref(false)
const dsLoading = ref(false)
const scanning = ref(false)
const saving = ref(false)
const tableData = ref<Metadata[]>([])
const dataSourceList = ref<any[]>([])
const currentRecord = ref<Metadata | null>(null)
const currentColumns = ref<MetadataColumn[]>([])

// ============ 筛选 ============
const filterDsId = ref<number | undefined>()
const filterLayer = ref<string | undefined>()
const filterSensitive = ref<string | undefined>()
const filterStatus = ref<string | undefined>()
const searchTableName = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const layerOptions = [
  { label: 'ODS（原始层）', value: 'ODS' },
  { label: 'DWD（明细层）', value: 'DWD' },
  { label: 'DWS（汇总层）', value: 'DWS' },
  { label: 'ADS（应用层）', value: 'ADS' }
]

const statCards = ref([
  { label: '总表数', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '活跃表', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '总字段数', value: 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' },
  { label: '数据源数', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' }
])

// ============ 扫描 ============
const scanDrawerVisible = ref(false)
const scanProgressVisible = ref(false)
const scanResult = ref<MetadataScanResult | null>(null)
const scanFormRef = ref<FormInstance>()

const scanForm = reactive({
  dsId: undefined as number | undefined,
  scanScope: 'ALL',
  /** PostgreSQL：与 dqc 接口一致，用于拉取表清单 */
  pgSchema: undefined as string | undefined,
  selectedTableNames: [] as string[],
  tablePattern: '',
  collectStats: true,
  overwriteExisting: false
})

const selectedScanDs = computed(() => dataSourceList.value.find((d: any) => d.id === scanForm.dsId))
const schemaLoading = ref(false)
const schemaList = ref<string[]>([])
const schemaOptions = computed(() => schemaList.value.map((s) => ({ label: s, value: s })))

const physicalTableLoading = ref(false)
const physicalTableNames = ref<string[]>([])
const physicalTableSelectOptions = computed(() =>
  physicalTableNames.value.map((t) => ({ label: t, value: t }))
)

const tablePickerVisible = ref(false)
const tablePickerSearch = ref('')
const tablePickerSelectedKeys = ref<string[]>([])
let scanPollTimer: ReturnType<typeof setInterval> | null = null

const filteredPickerTableRows = computed(() => {
  const q = tablePickerSearch.value.trim().toLowerCase()
  const names = physicalTableNames.value
  const list = q ? names.filter((n) => n.toLowerCase().includes(q)) : names
  return list.map((name) => ({ name }))
})

const tablePickerRowSelection = computed(() => ({
  selectedRowKeys: tablePickerSelectedKeys.value,
  onChange: (keys: (string | number)[]) => {
    tablePickerSelectedKeys.value = keys.map(String)
  }
}))

const scanProgressPercent = computed(() => {
  const r = scanResult.value
  if (!r || r.status !== 'RUNNING') return 0
  const p = r.plannedTableCount ?? 0
  const d = r.processedTableCount ?? 0
  if (p <= 0) return 0
  return Math.min(100, Math.round((d / p) * 100))
})

// ============ 详情 ============
const detailDrawerVisible = ref(false)
const sensitiveColumns = ref<SecColumnSensitivityVO[]>([])
const sensitiveLoading = ref(false)

// ============ 编辑 ============
const editDrawerVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  tableAlias: '',
  tableComment: '',
  dataLayer: '',
  dataDomain: '',
  lifecycleDays: 0,
  sensitivityLevel: 'NORMAL',
  tags: ''
})

// ============ 表格列 ============
const columns: TableProps['columns'] = [
  { title: '表名', key: 'tableName', width: 220, fixed: 'left', ellipsis: true },
  { title: '中文名', dataIndex: 'tableAlias', width: 160, ellipsis: true },
  { title: '数据层', key: 'dataLayer', width: 100 },
  { title: '敏感', key: 'isSensitive', width: 90, align: 'center' as const },
  { title: '敏感等级', key: 'sensitivityLevel', width: 110 },
  { title: '行数', key: 'rowCount', width: 120, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const columnColumns: TableProps['columns'] = [
  { title: '序号', width: 60, align: 'center', customRender: ({ index }) => index + 1 },
  { title: '字段名', dataIndex: 'columnName', width: 160, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', width: 120 },
  { title: '注释', dataIndex: 'columnComment', ellipsis: true },
  { title: '主键', key: 'isPrimaryKey', width: 60, align: 'center' },
  { title: '可空', key: 'isNullable', width: 70, align: 'center' },
  { title: '默认值', dataIndex: 'defaultValue', width: 120, ellipsis: true },
  { title: '敏感', key: 'isSensitive', width: 70, align: 'center' }
]

// 详情抽屉-敏感字段列表列定义
const sensitiveColumnsDef: TableProps['columns'] = [
  { title: '字段名', dataIndex: 'columnName', key: 'columnName', width: 140, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', key: 'dataType', width: 100 },
  { title: '注释', dataIndex: 'columnComment', key: 'columnComment', width: 140, ellipsis: true },
  { title: '敏感等级', key: 'levelName', width: 100 },
  { title: '分类', dataIndex: 'className', key: 'className', width: 100, ellipsis: true },
  { title: '脱敏方式', key: 'maskType', width: 100 },
  { title: '审核状态', key: 'reviewStatus', width: 90 }
]

// ============ 方法 ============
async function loadData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      dsId: filterDsId.value,
      dataLayer: filterLayer.value,
      status: filterStatus.value,
      tableName: searchTableName.value || undefined
    }
    // 如果筛选敏感字段，通过后端返回的 isSensitive 字段过滤
    const res = await metadataApi.page(params)
    if (res.data?.success !== false) {
      let records = res.data?.records || []
      // 前端筛选敏感字段（如果后端支持 isSensitive 筛选则移至 params）
      if (filterSensitive.value === 'true') {
        records = records.filter((r: any) => r.isSensitive === true || r.sensitivityLevel)
      } else if (filterSensitive.value === 'false') {
        records = records.filter((r: any) => !r.isSensitive && !r.sensitivityLevel)
      }
      tableData.value = records
      pagination.total = res.data?.total || 0
    }
  } catch {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await metadataApi.getStats()
    if (res.data?.success !== false) {
      const s = res.data?.data as MetadataStats
      statCards.value[0].value = s?.totalTables || 0
      statCards.value[1].value = s?.activeTables || 0
      statCards.value[2].value = s?.totalColumns || 0
      statCards.value[3].value = s?.dsCount || 0
    }
  } catch { /* ignore */ }
}

async function loadDataSources() {
  dsLoading.value = true
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.data?.success !== false) {
      dataSourceList.value = res.data || []
    }
  } catch { /* ignore */ }
  finally { dsLoading.value = false }
}

function resetFilters() {
  filterDsId.value = undefined
  filterLayer.value = undefined
  filterSensitive.value = undefined
  filterStatus.value = undefined
  searchTableName.value = ''
  pagination.current = 1
  loadData()
}

const handleTableChange: TableProps['onChange'] = (p) => {
  pagination.current = p.current || 1
  pagination.pageSize = p.pageSize || 10
  loadData()
}

function openScanDrawer() {
  scanForm.dsId = undefined
  scanForm.scanScope = 'ALL'
  scanForm.pgSchema = undefined
  scanForm.selectedTableNames = []
  scanForm.tablePattern = ''
  scanForm.collectStats = true
  scanForm.overwriteExisting = false
  schemaList.value = []
  physicalTableNames.value = []
  scanDrawerVisible.value = true
}

function clearScanPoll() {
  if (scanPollTimer != null) {
    clearInterval(scanPollTimer)
    scanPollTimer = null
  }
}

async function loadSchemasForScan() {
  if (!scanForm.dsId || selectedScanDs.value?.dsType !== 'POSTGRESQL') return
  schemaLoading.value = true
  try {
    const res: any = await dataSourceApi.getSchemas(scanForm.dsId)
    const list = (res.data ?? res) as string[]
    schemaList.value = Array.isArray(list) ? list : []
    const def = selectedScanDs.value?.schemaName
    if (def && schemaList.value.includes(def) && !scanForm.pgSchema) {
      scanForm.pgSchema = def
    }
  } catch {
    schemaList.value = []
  } finally {
    schemaLoading.value = false
  }
}

async function loadPhysicalTablesForScan() {
  if (!scanForm.dsId) {
    message.warning('请先选择数据源')
    return
  }
  const ds = selectedScanDs.value
  if (ds?.dsType === 'POSTGRESQL') {
    if (!scanForm.pgSchema) {
      message.warning('请先选择 Schema，再加载表列表')
      return
    }
  }
  physicalTableLoading.value = true
  try {
    let list: string[] = []
    if (ds?.dsType === 'POSTGRESQL' && scanForm.pgSchema) {
      const res: any = await dataSourceApi.getTableListBySchema(scanForm.dsId, scanForm.pgSchema)
      list = (res.data ?? res) as string[]
    } else {
      const res: any = await dataSourceApi.getTables(scanForm.dsId)
      list = (res.data ?? res) as string[]
    }
    physicalTableNames.value = Array.isArray(list) ? list : []
    if (physicalTableNames.value.length === 0) {
      message.info('未获取到表列表，请检查数据源权限或连接配置')
    }
  } catch {
    physicalTableNames.value = []
    message.error('加载表列表失败')
  } finally {
    physicalTableLoading.value = false
  }
}

function filterTableOption(input: string, option: any) {
  const label = (option?.label ?? option?.value ?? '') as string
  return label.toLowerCase().includes(input.trim().toLowerCase())
}

function onScanDsChange() {
  scanForm.pgSchema = undefined
  scanForm.selectedTableNames = []
  physicalTableNames.value = []
  schemaList.value = []
  if (selectedScanDs.value?.dsType === 'POSTGRESQL') {
    loadSchemasForScan()
  }
}

function onPgSchemaChange() {
  scanForm.selectedTableNames = []
  physicalTableNames.value = []
  if (scanForm.scanScope === 'SPECIFIED') {
    loadPhysicalTablesForScan()
  }
}

function onScanScopeChange() {
  if (scanForm.scanScope === 'SPECIFIED' && scanForm.dsId) {
    if (selectedScanDs.value?.dsType === 'POSTGRESQL' && !scanForm.pgSchema) {
      return
    }
    loadPhysicalTablesForScan()
  }
}

function openTablePickerModal() {
  if (!scanForm.dsId) {
    message.warning('请先选择数据源')
    return
  }
  if (selectedScanDs.value?.dsType === 'POSTGRESQL' && !scanForm.pgSchema) {
    message.warning('请先选择 Schema')
    return
  }
  tablePickerSearch.value = ''
  tablePickerSelectedKeys.value = [...scanForm.selectedTableNames]
  if (physicalTableNames.value.length === 0) {
    loadPhysicalTablesForScan().then(() => {
      tablePickerVisible.value = true
    })
  } else {
    tablePickerVisible.value = true
  }
}

function applyTablePicker() {
  scanForm.selectedTableNames = [...tablePickerSelectedKeys.value]
  tablePickerVisible.value = false
}

function closeScanProgressModal() {
  clearScanPoll()
  scanProgressVisible.value = false
  scanning.value = false
}

function onScanProgressModalClose() {
  clearScanPoll()
  scanning.value = false
}

async function pollScanUntilDone(taskId: string) {
  const pull = async () => {
    try {
      const res: any = await metadataApi.getScanProgress(taskId)
      const r = res.data as MetadataScanResult
      scanResult.value = r
      if (r.status !== 'RUNNING') {
        clearScanPoll()
        scanning.value = false
        if (r.status === 'SUCCESS' || r.status === 'PARTIAL_SUCCESS') {
          message.success('扫描完成')
          loadData()
          loadStats()
        } else if (r.status === 'FAILED') {
          message.error('扫描失败，请查看下方详情')
        } else if (r.status === 'CANCELLED') {
          message.warning('扫描已取消')
        }
      }
    } catch (e: any) {
      clearScanPoll()
      scanning.value = false
      message.error(e?.message || '获取扫描进度失败')
    }
  }
  await pull()
  if (scanResult.value?.status !== 'RUNNING') {
    return
  }
  clearScanPoll()
  scanPollTimer = setInterval(pull, 500)
}

async function handleScan() {
  if (!scanForm.dsId) {
    message.warning('请先选择数据源')
    return
  }
  if (scanForm.scanScope === 'SPECIFIED') {
    if (!scanForm.selectedTableNames.length) {
      message.warning('请选择至少一张表，或点击「刷新表列表」加载可选表')
      return
    }
    if (selectedScanDs.value?.dsType === 'POSTGRESQL' && !scanForm.pgSchema) {
      message.warning('PostgreSQL 请先选择 Schema')
      return
    }
  }
  if (scanForm.scanScope === 'PATTERN' && !scanForm.tablePattern?.trim()) {
    message.warning('请填写表名匹配模式')
    return
  }

  scanning.value = true
  scanDrawerVisible.value = false
  scanProgressVisible.value = true
  scanResult.value = {
    status: 'RUNNING',
    processedTableCount: 0,
    plannedTableCount: 0,
    dsName: selectedScanDs.value?.dsName
  } as MetadataScanResult

  clearScanPoll()
  try {
    const res: any = await metadataApi.scanAsync({
      dsId: scanForm.dsId,
      scanScope: scanForm.scanScope as any,
      tableNames: scanForm.scanScope === 'SPECIFIED' ? [...scanForm.selectedTableNames] : undefined,
      tablePattern: scanForm.scanScope === 'PATTERN' ? scanForm.tablePattern.trim() : undefined,
      collectStats: scanForm.collectStats,
      overwriteExisting: scanForm.overwriteExisting
    })
    const taskId = res.data as string
    if (!taskId) {
      message.error('未返回任务编号')
      scanning.value = false
      return
    }
    await pollScanUntilDone(taskId)
  } catch (e: any) {
    clearScanPoll()
    scanning.value = false
    message.error('扫描失败: ' + (e.message || '未知错误'))
  }
}

async function openDetailDrawer(record: Metadata) {
  currentRecord.value = record
  detailDrawerVisible.value = true
  sensitiveColumns.value = []
  sensitiveLoading.value = true
  try {
    const [colRes, sensRes] = await Promise.all([
      metadataApi.getColumns(record.id!),
      getColumnSensitivityByTable(record.dsId!, record.tableName!)
    ])
    if (colRes.data?.success !== false) {
      currentColumns.value = colRes.data || []
    }
    if (sensRes.code === 200 && sensRes.data) {
      sensitiveColumns.value = sensRes.data
    }
  } catch {
    currentColumns.value = []
    sensitiveColumns.value = []
  } finally {
    sensitiveLoading.value = false
  }
}

function openEditDrawer(record: Metadata) {
  currentRecord.value = record
  editForm.tableAlias = record.tableAlias || ''
  editForm.tableComment = record.tableComment || ''
  editForm.dataLayer = record.dataLayer || ''
  editForm.dataDomain = record.dataDomain || ''
  editForm.lifecycleDays = record.lifecycleDays || 0
  editForm.sensitivityLevel = record.sensitivityLevel || 'NORMAL'
  editForm.tags = record.tags || ''
  editDrawerVisible.value = true
}

async function handleSaveEdit() {
  if (!currentRecord.value?.id) return
  saving.value = true
  try {
    const res = await metadataApi.updateMetadata(currentRecord.value.id, editForm as any)
    if (res.data?.success !== false) {
      message.success('保存成功')
      editDrawerVisible.value = false
      loadData()
      loadStats()
    }
  } catch { message.error('保存失败') }
  finally { saving.value = false }
}

async function handleDelete(record: Metadata) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除元数据「${record.tableName}」吗？删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        const res = await metadataApi.deleteMetadata(record.id!)
        if (res.data?.success !== false) {
          message.success('删除成功')
          loadData()
          loadStats()
        }
      } catch { message.error('删除失败') }
    }
  })
}

// ============ 辅助方法 ============
function getLayerColor(layer: string | undefined) {
  const map: Record<string, string> = {
    ODS: '#1677FF', DWD: '#52C41A', DWS: '#FAAD14', ADS: '#722ED1'
  }
  return map[layer || ''] || '#8C8C8C'
}

function getSensitivityColor(level: string | undefined) {
  const map: Record<string, string> = {
    NORMAL: 'green', INNER: 'orange', SENSITIVE: 'red', HIGHLY_SENSITIVE: '#722ED1'
  }
  return map[level || ''] || 'default'
}

function getSensitivityLabel(level: string | undefined) {
  const map: Record<string, string> = {
    NORMAL: '普通', INNER: '内部', SENSITIVE: '敏感', HIGHLY_SENSITIVE: '高度敏感'
  }
  return map[level || ''] || level || '-'
}

function getStatusLabel(status: string | undefined) {
  const map: Record<string, string> = { ACTIVE: '活跃', ARCHIVED: '归档', DEPRECATED: '废弃' }
  return map[status || ''] || status || '-'
}

function getScanStatusTitle(status: string) {
  const map: Record<string, string> = {
    RUNNING: '正在扫描',
    SUCCESS: '扫描完成',
    FAILED: '扫描失败',
    PARTIAL_SUCCESS: '部分成功',
    CANCELLED: '已取消'
  }
  return map[status] || '扫描结束'
}

function getScanStatusSubTitle(result: MetadataScanResult) {
  if (!result) return ''
  return `成功扫描 ${result.successTables} 个表，新增 ${result.insertedTables} 个，更新 ${result.updatedTables} 个`
}

function formatNumber(n: number | null | undefined) {
  if (n == null) return '-'
  return n.toLocaleString()
}

function formatBytes(bytes: number | null | undefined) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

onMounted(() => {
  loadData()
  loadStats()
  loadDataSources()
})

onUnmounted(() => {
  clearScanPoll()
})
</script>

<style scoped>
.page-container { padding: 24px; }

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

.filter-bar { margin-bottom: 16px; }

.table-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
}

.table-name-cell { display: flex; align-items: center; gap: 6px; }
.table-icon { color: #1677FF; }
.table-name-text { font-weight: 500; font-family: 'JetBrains Mono', monospace; }

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.nullable { color: #8C8C8C; }
.not-nullable { color: #1F1F1F; font-weight: 500; }

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: white;
  display: flex;
  justify-content: flex-end;
}

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }
.switch-label { margin-left: 8px; font-size: 13px; color: #8C8C8C; }

.scan-progress { padding: 8px 0; }
.scan-errors { margin-top: 16px; max-height: 200px; overflow-y: auto; }
.error-title { font-weight: 600; font-size: 13px; margin-bottom: 8px; color: #FF4D4F; }
.error-item { display: flex; gap: 8px; padding: 4px 0; font-size: 12px; border-bottom: 1px solid #f5f5f5; }
.error-table { font-weight: 500; color: #1F1F1F; min-width: 120px; }
.error-msg { color: #8C8C8C; word-break: break-all; }

.detail-content { padding-bottom: 60px; }

/* 敏感字段区块 */
.sensitive-loading {
  text-align: center;
  padding: 16px;
  color: #8C8C8C;
}
.sensitive-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: #d9d9d9;
  font-size: 13px;
}
.sensitive-empty .empty-icon {
  font-size: 16px;
}
.sensitive-fields-block {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}
.sensitive-summary {
  padding: 8px 12px;
  background: #fff1f0;
  border-bottom: 1px solid #ffccc7;
}

.ds-instance-card {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  font-size: 13px;
}
.ds-instance-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}
.ds-instance-line:last-child {
  margin-bottom: 0;
}
.ds-instance-label {
  color: #8c8c8c;
  min-width: 72px;
}
.ds-instance-sep {
  color: #d9d9d9;
}

.scan-running-block {
  padding: 16px 8px 8px;
}
.scan-running-text {
  margin-top: 12px;
  font-size: 14px;
  color: #434343;
}
</style>
