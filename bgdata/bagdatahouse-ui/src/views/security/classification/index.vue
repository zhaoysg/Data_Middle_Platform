<template>
  <div class="page-container classification-page">
    <!-- ========== 页面标题区 ========== -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#secGrad2)"/>
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="secGrad2" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#B37FEB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据分类分级标准</h1>
          <p class="page-subtitle">构建企业数据分类与敏感等级体系，为敏感字段识别提供分类依据</p>
        </div>
      </div>
    </div>

    <!-- ========== 统计卡片行 ========== -->
    <a-row :gutter="[20, 20]" class="stat-row">
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)">
          <div class="stat-value">{{ stats.totalClassification || 0 }}</div>
          <div class="stat-label">分类总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #F5222D 0%, #FF7875 100%)">
          <div class="stat-value">{{ stats.totalLevel || 0 }}</div>
          <div class="stat-label">敏感等级</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-value">{{ stats.enabledClassification || 0 }}</div>
          <div class="stat-label">启用分类</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #1677FF 0%, #69B1FF 100%)">
          <div class="stat-value">{{ stats.pendingReview || 0 }}</div>
          <div class="stat-label">待审核字段</div>
        </div>
      </a-col>
    </a-row>

    <!-- ========== 主内容卡片 ========== -->
    <div class="table-card">
      <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
        <!-- ==================== 分类管理 Tab ==================== -->
        <a-tab-pane key="classification" tab="分类管理">
          <!-- 工具栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <a-input
                v-model:value="classSearch"
                placeholder="搜索分类名称/编码"
                style="width: 220px"
                allowClear
                @pressEnter="loadClassification"
                @change="debouncedSearch"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
              <a-select
                v-model:value="classEnabled"
                placeholder="启用状态"
                style="width: 120px"
                allowClear
                @change="loadClassification"
              >
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
              <a-button @click="resetClassFilters">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </div>
            <div class="toolbar-right">
              <a-button type="primary" @click="openClassDrawer('add')">
                <template #icon><PlusOutlined /></template>
                新增分类
              </a-button>
            </div>
          </div>

          <!-- 分类列表 -->
          <a-table
            :columns="classColumns"
            :data-source="classData"
            :loading="classLoading"
            :pagination="classPagination"
            :row-key="(r: SecClassificationVO) => r.id"
            @change="handleClassTableChange"
            :scroll="{ x: 1000 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <!-- 分类名称 -->
              <template v-if="column.key === 'className'">
                <div class="class-name-cell">
                  <div style="font-weight: 500; color: #1F1F1F">{{ record.className }}</div>
                  <div style="color: #8C8C8C; font-size: 12px">{{ record.classCode }}</div>
                </div>
              </template>
              <!-- 关联等级 -->
              <template v-if="column.key === 'sensitivityLevel'">
                <a-tag
                  v-if="record.sensitivityLevel"
                  :color="getLevelColor(record.sensitivityLevel)"
                  style="font-weight: 500"
                >
                  {{ record.sensitivityLevelLabel || record.sensitivityLevel }}
                </a-tag>
                <span v-else style="color: #d9d9d9">—</span>
              </template>
              <!-- 启用状态 -->
              <template v-if="column.key === 'enabled'">
                <a-badge
                  :status="record.status === 1 ? 'success' : 'default'"
                  :text="record.status === 1 ? '启用' : '禁用'"
                />
              </template>
              <!-- 操作 -->
              <template v-if="column.key === 'action'">
                <a-space size="small">
                  <a-tooltip title="编辑">
                    <a-button type="text" size="small" @click="openClassDrawer('edit', record)">
                      <template #icon><EditOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="record.status === 1 ? '禁用' : '启用'">
                    <a-button type="text" size="small" @click="toggleClass(record)">
                      <template #icon>
                        <CheckCircleOutlined v-if="record.status === 0" style="color: #52C41A" />
                        <CloseCircleOutlined v-else style="color: #FF4D4F" />
                      </template>
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm
                    title="确定删除该分类？"
                    @confirm="deleteClass(record.id!)"
                  >
                    <a-button type="text" size="small" danger>
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>

            <template #emptyText>
              <div class="table-empty">
                <SafetyOutlined class="empty-icon" />
                <div class="empty-title">暂无数据分类</div>
                <div class="empty-desc">建立数据分类体系，为敏感字段识别提供分类依据</div>
                <a-button type="primary" @click="openClassDrawer('add')">新增分类</a-button>
              </div>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ==================== 等级管理 Tab ==================== -->
        <a-tab-pane key="level" tab="等级管理">
          <!-- 工具栏 -->
          <div class="toolbar">
            <div class="toolbar-left">
              <a-input
                v-model:value="levelSearch"
                placeholder="搜索等级名称/编码"
                style="width: 220px"
                allowClear
                @pressEnter="loadLevel"
                @change="debouncedLevelSearch"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </div>
            <div class="toolbar-right">
              <a-button type="primary" @click="openLevelDrawer('add')">
                <template #icon><PlusOutlined /></template>
                新增等级
              </a-button>
            </div>
          </div>

          <!-- 等级列表 -->
          <a-table
            :columns="levelColumns"
            :data-source="levelData"
            :loading="levelLoading"
            :pagination="levelPagination"
            :row-key="(r: SecLevelVO) => r.id"
            @change="handleLevelTableChange"
            :scroll="{ x: 900 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <!-- 等级值圆形徽章 -->
              <template v-if="column.key === 'levelValue'">
                <div
                  class="level-badge"
                  :style="{ background: record.color || '#1677FF' }"
                  :title="'等级数值: ' + record.levelValue"
                >
                  {{ record.levelValue }}
                </div>
              </template>
              <!-- 等级名称 -->
              <template v-if="column.key === 'levelName'">
                <div class="level-name-cell">
                  <span :style="{ color: record.color, fontWeight: 600, fontSize: '14px' }">
                    {{ record.levelName }}
                  </span>
                  <span style="color: #8C8C8C; font-size: 12px; margin-left: 8px">
                    {{ record.levelCode }}
                  </span>
                </div>
              </template>
              <!-- 描述 -->
              <template v-if="column.key === 'levelDesc'">
                <a-tooltip :title="record.levelDesc">
                  <span style="color: #8C8C8C; font-size: 13px">
                    {{ record.levelDesc || '—' }}
                  </span>
                </a-tooltip>
              </template>
              <!-- 颜色预览 -->
              <template v-if="column.key === 'color'">
                <div style="display: flex; align-items: center; gap: 8px">
                  <div
                    style="width: 24px; height: 24px; border-radius: 4px"
                    :style="{ background: record.color }"
                  ></div>
                  <span style="font-family: monospace; color: #8C8C8C; font-size: 12px">
                    {{ record.color }}
                  </span>
                </div>
              </template>
              <!-- 字段统计 -->
              <template v-if="column.key === 'fieldCount'">
                <span style="color: #1677FF; font-weight: 500">
                  {{ record.sensitiveFieldCount ?? 0 }}
                </span>
              </template>
              <!-- 创建时间（须与 levelColumns 中 key 一致；仅用 dataIndex 时 bodyCell 无匹配会空白） -->
              <template v-if="column.key === 'createTime'">
                <span style="color: #8C8C8C; font-size: 13px">{{ record.createTime || '—' }}</span>
              </template>
              <!-- 操作 -->
              <template v-if="column.key === 'action'">
                <a-space size="small">
                  <a-tooltip title="编辑">
                    <a-button type="text" size="small" @click="openLevelDrawer('edit', record)">
                      <template #icon><EditOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm
                    title="确定删除该等级？删除后，引用该等级的数据将失去等级信息。"
                    @confirm="deleteLevel(record.id!)"
                  >
                    <a-button type="text" size="small" danger>
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>

            <template #emptyText>
              <div class="table-empty">
                <SafetyOutlined class="empty-icon" />
                <div class="empty-title">暂无敏感等级</div>
                <div class="empty-desc">创建敏感等级体系，定义数据的敏感程度分级</div>
                <a-button type="primary" @click="openLevelDrawer('add')">新增等级</a-button>
              </div>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- ========== 分类新增/编辑抽屉 ========== -->
    <a-drawer
      v-model:open="classDrawerVisible"
      :title="classDrawerMode === 'add' ? '新增数据分类' : '编辑数据分类'"
      :width="520"
      :mask-closable="false"
      @close="classDrawerVisible = false"
    >
      <a-form
        ref="classFormRef"
        :model="classForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="classFormRules"
        layout="horizontal"
      >
        <a-form-item label="分类编码" name="classCode">
          <a-input
            v-model:value="classForm.classCode"
            placeholder="如 C01"
            :disabled="classDrawerMode === 'edit'"
            :maxlength="30"
            show-count
          />
        </a-form-item>

        <a-form-item label="分类名称" name="className">
          <a-input
            v-model:value="classForm.className"
            placeholder="如 个人信息、财务数据"
            :maxlength="50"
            show-count
          />
        </a-form-item>

        <a-form-item label="分类描述" name="classDesc">
          <a-textarea
            v-model:value="classForm.classDesc"
            placeholder="简要描述该分类的适用范围和管理规范"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>

        <a-form-item label="关联等级" name="sensitivityLevel">
          <a-select
            v-model:value="classForm.sensitivityLevel"
            placeholder="选择默认关联的敏感等级"
            allowClear
          >
            <a-select-option v-for="lv in levelList" :key="lv.levelCode" :value="lv.levelCode">
              <div style="display: flex; align-items: center; gap: 8px">
                <div
                  style="width: 14px; height: 14px; border-radius: 3px"
                  :style="{ background: lv.color }"
                ></div>
                <span>{{ lv.levelCode }} - {{ lv.levelName }}</span>
              </div>
            </a-select-option>
          </a-select>
          <div class="form-tip">选择后，该分类下的字段将默认采用此敏感等级</div>
        </a-form-item>

        <a-form-item label="排序值" name="classOrder">
          <a-input-number
            v-model:value="classForm.classOrder"
            :min="0"
            :max="9999"
            style="width: 100%"
            placeholder="数值越小排序越靠前"
          />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-switch
            v-model:checked="classFormStatus"
            checked-children="启用"
            un-checked-children="禁用"
          />
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="classDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="classSaving" @click="saveClass">
            {{ classDrawerMode === 'add' ? '创建分类' : '保存修改' }}
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- ========== 等级新增/编辑抽屉 ========== -->
    <a-drawer
      v-model:open="levelDrawerVisible"
      :title="levelDrawerMode === 'add' ? '新增敏感等级' : '编辑敏感等级'"
      :width="560"
      :mask-closable="false"
      @close="levelDrawerVisible = false"
    >
      <a-form
        ref="levelFormRef"
        :model="levelForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="levelFormRules"
        layout="horizontal"
      >
        <a-form-item label="等级编码" name="levelCode">
          <a-input
            v-model:value="levelForm.levelCode"
            placeholder="如 L1, L2, L3, L4"
            :disabled="levelDrawerMode === 'edit'"
            :maxlength="10"
            show-count
          />
          <div class="form-tip">建议使用 L1-L4 格式，便于系统识别和排序</div>
        </a-form-item>

        <a-form-item label="等级名称" name="levelName">
          <a-input
            v-model:value="levelForm.levelName"
            placeholder="如 公开、内部、敏感、机密"
            :maxlength="20"
            show-count
          />
        </a-form-item>

        <a-form-item label="等级数值" name="levelValue">
          <a-input-number
            v-model:value="levelForm.levelValue"
            :min="1"
            :max="99"
            style="width: 100%"
          />
          <div class="form-tip">
            数值越大敏感程度越高，用于排序和比较。推荐：L1=1, L2=2, L3=3, L4=4
          </div>
        </a-form-item>

        <a-form-item label="等级描述" name="levelDesc">
          <a-textarea
            v-model:value="levelForm.levelDesc"
            placeholder="描述该等级的含义、管理要求和处理规范"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>

        <a-form-item label="等级颜色" name="color">
          <div class="color-picker-row">
            <div class="color-preview" :style="{ background: levelForm.color }"></div>
            <a-input
              v-model:value="levelForm.color"
              style="flex: 1; font-family: monospace"
              placeholder="#52C41A"
            />
            <div class="color-input-wrapper">
              <input
                type="color"
                :value="levelForm.color"
                @input="(e: Event) => levelForm.color = (e.target as HTMLInputElement).value"
                class="native-color-picker"
                title="选择颜色"
              />
            </div>
          </div>
          <!-- 预设颜色 -->
          <div class="preset-colors">
            <span class="preset-label">预设：</span>
            <div
              v-for="preset in presetColors"
              :key="preset.color"
              class="preset-color-dot"
              :class="{ active: levelForm.color === preset.color }"
              :style="{ background: preset.color }"
              :title="preset.name"
              @click="levelForm.color = preset.color"
            ></div>
          </div>
        </a-form-item>

        <!-- 等级效果预览 -->
        <a-form-item label="效果预览">
          <div class="level-preview">
            <div
              class="level-preview-badge"
              :style="{ background: levelForm.color }"
            >
              {{ levelForm.levelValue || '?' }}
            </div>
            <span :style="{ color: levelForm.color, fontWeight: 600, fontSize: '14px' }">
              {{ levelForm.levelName || '等级名称' }}
            </span>
            <span style="color: #8C8C8C; margin-left: 8px">
              {{ levelForm.levelCode || 'L?' }}
            </span>
          </div>
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="levelDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="levelSaving" @click="saveLevel">
            {{ levelDrawerMode === 'add' ? '创建等级' : '保存修改' }}
          </a-button>
        </a-space>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined,
  SearchOutlined, ReloadOutlined, SafetyOutlined,
  CheckCircleOutlined, CloseCircleOutlined
} from '@ant-design/icons-vue'
import {
  pageClassification as pageClassificationApi,
  listClassification as listClassificationApi,
  saveClassification as saveClassificationApi,
  updateClassification as updateClassificationApi,
  deleteClassification as deleteClassificationApi,
  enableClassification as enableClassificationApi,
  disableClassification as disableClassificationApi,
  pageLevel as pageLevelApi,
  listLevel as listLevelApi,
  saveLevel as saveLevelApi,
  updateLevel as updateLevelApi,
  deleteLevel as deleteLevelApi,
  getSecurityStats,
  type SecClassificationVO,
  type SecClassificationSaveDTO,
  type SecLevelVO,
  type SecLevelSaveDTO
} from '@/api/security'

// ============================================================
// 公共状态
// ============================================================
const activeTab = ref('classification')

// 统计数据
const stats = reactive({
  totalClassification: 0,
  totalLevel: 0,
  enabledClassification: 0,
  pendingReview: 0
})

// 等级选项列表（供分类表单选择）
const levelList = ref<{ levelCode: string; levelName: string; color: string }[]>([])

// ============================================================
// 分类管理
// ============================================================
const classLoading = ref(false)
const classData = ref<SecClassificationVO[]>([])
const classSearch = ref('')
const classEnabled = ref<number | undefined>()

// 抽屉
const classDrawerVisible = ref(false)
const classDrawerMode = ref<'add' | 'edit'>('add')
const classSaving = ref(false)
const classFormRef = ref<FormInstance>()
const classFormStatus = ref(true)

const classForm = reactive<SecClassificationSaveDTO>({
  id: undefined,
  classCode: '',
  className: '',
  classDesc: '',
  sensitivityLevel: undefined,
  status: 1,
  classOrder: 0
})

const classFormRules = {
  classCode: [{ required: true, message: '请输入分类编码' }],
  className: [{ required: true, message: '请输入分类名称' }, { min: 2, message: '分类名称至少2个字符' }]
}

const classPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
})

const classColumns = [
  { title: '分类名称', key: 'className', width: 240, ellipsis: true },
  { title: '关联等级', key: 'sensitivityLevel', width: 100, align: 'center' },
  { title: '状态', key: 'enabled', width: 90, align: 'center' },
  { title: '排序', dataIndex: 'classOrder', width: 70, align: 'center' },
  { title: '规则数', dataIndex: 'ruleCount', width: 80, align: 'center' },
  { title: '字段数', dataIndex: 'sensitiveFieldCount', width: 80, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

async function loadClassification() {
  classLoading.value = true
  try {
    const res = await pageClassificationApi({
      pageNum: classPagination.current,
      pageSize: classPagination.pageSize,
      className: classSearch.value || undefined,
      classCode: classSearch.value || undefined,
      enabled: classEnabled.value
    })
    // request 拦截器已解包为 Result，payload 在 res.data（勿用 res.data.data）
    if (res.success !== false) {
      classData.value = res.data?.records || []
      classPagination.total = res.data?.total || 0
    }
  } catch {
    message.error('加载数据分类失败')
  } finally {
    classLoading.value = false
  }
}

function handleClassTableChange(pag: any) {
  classPagination.current = pag.current
  classPagination.pageSize = pag.pageSize
  loadClassification()
}

function resetClassFilters() {
  classSearch.value = ''
  classEnabled.value = undefined
  classPagination.current = 1
  loadClassification()
}

// 防抖搜索
let searchTimer: any = null
function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    classPagination.current = 1
    loadClassification()
  }, 300)
}

function openClassDrawer(mode: 'add' | 'edit', record?: SecClassificationVO) {
  classDrawerMode.value = mode
  if (mode === 'edit' && record) {
    Object.assign(classForm, {
      id: record.id,
      classCode: record.classCode,
      className: record.className,
      classDesc: record.classDesc,
      sensitivityLevel: record.sensitivityLevel,
      status: record.status,
      classOrder: record.classOrder || 0
    })
    classFormStatus.value = record.status === 1
  } else {
    Object.assign(classForm, {
      id: undefined,
      classCode: '',
      className: '',
      classDesc: '',
      sensitivityLevel: undefined,
      status: 1,
      classOrder: 0
    })
    classFormStatus.value = true
  }
  classDrawerVisible.value = true
}

async function saveClass() {
  try {
    await classFormRef.value?.validate()
  } catch {
    return
  }
  classSaving.value = true
  try {
    classForm.status = classFormStatus.value ? 1 : 0
    if (classDrawerMode.value === 'add') {
      await saveClassificationApi(classForm)
      message.success('分类创建成功')
    } else {
      await updateClassificationApi(classForm.id!, classForm)
      message.success('分类更新成功')
    }
    classDrawerVisible.value = false
    loadClassification()
    loadStats()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    classSaving.value = false
  }
}

async function deleteClass(id: number) {
  try {
    await deleteClassificationApi(id)
    message.success('删除成功')
    loadClassification()
    loadStats()
  } catch {
    message.error('删除失败')
  }
}

async function toggleClass(record: SecClassificationVO) {
  try {
    if (record.status === 1) {
      await disableClassificationApi(record.id!)
    } else {
      await enableClassificationApi(record.id!)
    }
    loadClassification()
    loadStats()
  } catch {
    message.error('操作失败')
  }
}

// ============================================================
// 等级管理
// ============================================================
const levelLoading = ref(false)
const levelData = ref<SecLevelVO[]>([])
const levelSearch = ref('')
const levelDrawerVisible = ref(false)
const levelDrawerMode = ref<'add' | 'edit'>('add')
const levelSaving = ref(false)
const levelFormRef = ref<FormInstance>()

const levelForm = reactive<SecLevelSaveDTO>({
  id: undefined,
  levelCode: '',
  levelName: '',
  levelValue: 1,
  levelDesc: '',
  color: '#52C41A'
})

const levelFormRules = {
  levelCode: [{ required: true, message: '请输入等级编码' }],
  levelName: [{ required: true, message: '请输入等级名称' }],
  levelValue: [{ required: true, message: '请输入等级数值' }]
}

const levelPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
})

const levelColumns = [
  { title: '等级', key: 'levelValue', width: 80, align: 'center' },
  { title: '等级名称', key: 'levelName', width: 200 },
  { title: '等级描述', key: 'levelDesc', width: 220, ellipsis: true },
  { title: '颜色', key: 'color', width: 160 },
  { title: '字段数', key: 'fieldCount', width: 80, align: 'center' },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

// 预设颜色
const presetColors = [
  { color: '#52C41A', name: '绿色 - L1公开' },
  { color: '#1677FF', name: '蓝色 - L2内部' },
  { color: '#FA8C16', name: '橙色 - L3敏感' },
  { color: '#F5222D', name: '红色 - L4机密' },
  { color: '#722ED1', name: '紫色' },
  { color: '#13C2C2', name: '青色' }
]

async function loadLevel() {
  levelLoading.value = true
  try {
    const res = await pageLevelApi({
      pageNum: levelPagination.current,
      pageSize: levelPagination.pageSize
    })
    if (res.success !== false) {
      levelData.value = res.data?.records || []
      levelPagination.total = res.data?.total || 0
    }
  } catch {
    message.error('加载敏感等级失败')
  } finally {
    levelLoading.value = false
  }
}

async function loadLevelOptions() {
  try {
    const res = await listLevelApi()
    if (res.success !== false) {
      levelList.value = (res.data || []).map(l => ({
        levelCode: l.levelCode || '',
        levelName: l.levelName || '',
        color: l.color || '#1677FF'
      }))
    }
  } catch {
    // silent
  }
}

function handleLevelTableChange(pag: any) {
  levelPagination.current = pag.current
  levelPagination.pageSize = pag.pageSize
  loadLevel()
}

let levelSearchTimer: any = null
function debouncedLevelSearch() {
  clearTimeout(levelSearchTimer)
  levelSearchTimer = setTimeout(() => {
    levelPagination.current = 1
    loadLevel()
  }, 300)
}

function openLevelDrawer(mode: 'add' | 'edit', record?: SecLevelVO) {
  levelDrawerMode.value = mode
  if (mode === 'edit' && record) {
    Object.assign(levelForm, {
      id: record.id,
      levelCode: record.levelCode,
      levelName: record.levelName,
      levelValue: record.levelValue || 1,
      levelDesc: record.levelDesc || '',
      color: record.color || '#52C41A'
    })
  } else {
    Object.assign(levelForm, {
      id: undefined,
      levelCode: '',
      levelName: '',
      levelValue: 1,
      levelDesc: '',
      color: '#52C41A'
    })
  }
  levelDrawerVisible.value = true
}

async function saveLevel() {
  try {
    await levelFormRef.value?.validate()
  } catch {
    return
  }
  levelSaving.value = true
  try {
    if (levelDrawerMode.value === 'add') {
      await saveLevelApi(levelForm)
      message.success('等级创建成功')
    } else {
      await updateLevelApi(levelForm.id!, levelForm)
      message.success('等级更新成功')
    }
    levelDrawerVisible.value = false
    loadLevel()
    loadLevelOptions()
    loadStats()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    levelSaving.value = false
  }
}

async function deleteLevel(id: number) {
  try {
    await deleteLevelApi(id)
    message.success('删除成功')
    loadLevel()
    loadLevelOptions()
    loadStats()
  } catch {
    message.error('删除失败')
  }
}

// ============================================================
// 辅助方法
// ============================================================
function getLevelColor(level?: string) {
  const map: Record<string, string> = {
    'L1': '#52C41A', 'L2': '#1677FF',
    'L3': '#FA8C16', 'L4': '#F5222D',
    'HIGHLY_SENSITIVE': '#F5222D', 'SENSITIVE': '#FA8C16',
    'INTERNAL': '#1677FF', 'NORMAL': '#52C41A'
  }
  return map[level || ''] || '#8C8C8C'
}

async function loadStats() {
  try {
    const res = await getSecurityStats()
    if (res.success !== false) {
      const s = res.data
      stats.totalClassification = s?.totalClassificationCount || 0
      stats.totalLevel = s?.totalLevelCount || 0
      stats.enabledClassification = (s?.totalClassificationCount || 0) - (s?.disabledClassificationCount || 0)
      stats.pendingReview = s?.pendingReviewCount || 0
    }
  } catch {
    // silent
  }
}

function onTabChange(tab: string) {
  if (tab === 'classification') {
    if (classData.value.length === 0) {
      loadClassification()
    }
  } else if (tab === 'level') {
    if (levelData.value.length === 0) {
      loadLevel()
    }
  }
}

// ============================================================
// 初始化
// ============================================================
onMounted(() => {
  loadStats()
  loadClassification()
  loadLevelOptions()
})
</script>

<style lang="less" scoped>
/* ========== 布局 ========== */
.page-container {
  padding: 24px;
  min-height: 100%;
  background: #F5F7FA;
}
.classification-page { }

/* ========== 页面标题区 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .header-left { display: flex; align-items: center; gap: 12px; }
  .header-icon { flex-shrink: 0; }
  .page-title { margin: 0; font-size: 22px; font-weight: 700; color: #1F1F1F; line-height: 1.2; }
  .page-subtitle { margin: 4px 0 0; font-size: 13px; color: #8C8C8C; }
}

/* ========== 统计卡片 ========== */
.stat-row { margin-bottom: 20px; }
.stat-card {
  border-radius: 8px;
  padding: 24px;
  color: #fff;
  min-height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: default;
  &:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,0.15); }
}
.stat-value { font-size: 36px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 14px; opacity: 0.9; margin-top: 6px; }

/* ========== 主内容卡片 ========== */
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* ========== 工具栏 ========== */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }

/* ========== 表格 ========== */
.class-name-cell, .level-name-cell { display: flex; flex-direction: column; gap: 2px; }
.level-badge {
  width: 32px; height: 32px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; font-weight: 700; margin: 0 auto;
  box-shadow: 0 2px 6px rgba(0,0,0,0.15); cursor: default;
}

/* ========== 抽屉 ========== */
.drawer-footer {
  display: flex; justify-content: flex-end; gap: 8px; padding: 16px 0;
  border-top: 1px solid #f0f0f0; position: absolute; bottom: 0; left: 0; right: 0;
}
.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; line-height: 1.5; }

/* ========== 等级颜色选择器 ========== */
.color-picker-row { display: flex; align-items: center; gap: 8px; }
.color-preview { width: 32px; height: 32px; border-radius: 6px; flex-shrink: 0; border: 1px solid rgba(0,0,0,0.06); }
.color-input-wrapper { flex-shrink: 0; }
.native-color-picker { width: 32px; height: 32px; border: 1px solid #d9d9d9; border-radius: 6px; cursor: pointer; padding: 2px; background: #fff; }
.preset-colors { display: flex; align-items: center; gap: 6px; margin-top: 10px; flex-wrap: wrap; }
.preset-label { font-size: 12px; color: #8C8C8C; flex-shrink: 0; }
.preset-color-dot { width: 20px; height: 20px; border-radius: 4px; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.15); }
.preset-color-dot:hover { transform: scale(1.2); border-color: rgba(0,0,0,0.3); }
.preset-color-dot.active { border-color: #fff; box-shadow: 0 0 0 2px #1677FF; transform: scale(1.15); }
.level-preview { display: flex; align-items: center; gap: 12px; padding: 12px 16px; background: #fafafa; border-radius: 8px; border: 1px solid #f0f0f0; }
.level-preview-badge { width: 40px; height: 40px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: 700; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }

/* ========== 空状态 ========== */
.table-empty { padding: 48px 0; text-align: center; }
.empty-icon { font-size: 48px; color: #d9d9d9; margin-bottom: 12px; display: block; }
.empty-title { font-size: 16px; color: #595959; margin-bottom: 8px; }
.empty-desc { font-size: 13px; color: #8C8C8C; margin-bottom: 16px; }
</style>
