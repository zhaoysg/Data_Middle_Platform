<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#assetGrad)"/>
            <path d="M4 7H20M4 12H20M4 17H10" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <defs>
              <linearGradient id="assetGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#9254DE"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据资产目录</h1>
          <p class="page-subtitle">构建业务域 → 数据域 → 数据专辑的多层级资产目录体系，支持跨域收录资产</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="openAddDrawer('BUSINESS_DOMAIN')">
          <template #icon><PlusOutlined /></template>
          新建目录
        </a-button>
        <a-button @click="loadTree">
          <template #icon><ReloadOutlined /></template>
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

    <!-- 主内容区：左侧目录树 + 右侧资产列表 -->
    <a-row :gutter="16">
      <!-- 左侧目录树 -->
      <a-col :span="6">
        <div class="tree-card">
          <div class="tree-card-header">
            <span class="tree-title">目录结构</span>
            <a-space>
              <a-tooltip title="刷新">
                <a-button type="text" size="small" @click="loadTree">
                  <template #icon><ReloadOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </div>

          <!-- 目录类型筛选 -->
          <div class="tree-filter">
            <a-radio-group v-model:value="treeFilterType" size="small" button-style="solid" @change="loadTree">
              <a-radio-button value="">全部</a-radio-button>
              <a-radio-button value="BUSINESS_DOMAIN">业务域</a-radio-button>
              <a-radio-button value="DATA_DOMAIN">数据域</a-radio-button>
              <a-radio-button value="ALBUM">数据专辑</a-radio-button>
            </a-radio-group>
          </div>

          <!-- 目录树 -->
          <div class="tree-body">
            <a-spin :spinning="treeLoading">
              <a-tree
                v-if="treeData.length > 0"
                :tree-data="treeData"
                :selected-keys="selectedKeys"
                :expanded-keys="expandedKeys"
                :auto-expand-parent="autoExpandParent"
                :show-icon="true"
                :show-line="{ showLeafIcon: false }"
                :field-names="{ key: 'id', title: 'catalogName', children: 'children' }"
                @select="handleTreeSelect"
                @expand="handleTreeExpand"
              >
                <template #icon>
                  <FolderOutlined style="color: #1677FF" />
                </template>
                <template #switcherIcon>
                  <CaretDownOutlined />
                </template>
                <template #title="node">
                  <div class="tree-node-content">
                    <span class="node-name">{{ node.catalogName }}</span>
                    <span class="node-count" v-if="node.itemCount">{{ node.itemCount }}</span>
                    <a-dropdown :trigger="['click']" @click.stop>
                      <a-button type="text" size="small" class="node-action" @click.stop>
                        <template #icon><MoreOutlined /></template>
                      </a-button>
                      <template #overlay>
                        <a-menu @click="(e: any) => handleNodeAction(e.key, node)">
                          <a-menu-item key="add-child" v-if="node.catalogType !== 'ALBUM'">
                            <PlusOutlined /> 添加子目录
                          </a-menu-item>
                          <a-menu-item key="add-asset">
                            <AppstoreAddOutlined /> 收录资产
                          </a-menu-item>
                          <a-menu-item key="edit">
                            <EditOutlined /> 编辑
                          </a-menu-item>
                          <a-menu-item key="move">
                            <SwapOutlined /> 移动
                          </a-menu-item>
                          <a-menu-divider />
                          <a-menu-item key="delete" style="color: #FF4D4F">
                            <DeleteOutlined /> 删除
                          </a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </div>
                </template>
              </a-tree>

              <!-- 空状态 -->
              <div v-else-if="!treeLoading" class="tree-empty">
                <InboxOutlined class="empty-icon" />
                <div class="empty-text">暂无目录</div>
                <a-button type="link" @click="openAddDrawer('BUSINESS_DOMAIN')">立即创建</a-button>
              </div>
            </a-spin>
          </div>
        </div>
      </a-col>

      <!-- 右侧资产列表 -->
      <a-col :span="18">
        <div class="table-card">
          <!-- 资产列表标题 -->
          <div class="asset-header">
            <div class="asset-title">
              <span v-if="currentCatalog">{{ currentCatalog.catalogName }}</span>
              <span v-else class="text-muted">请从左侧选择目录</span>
              <a-tag v-if="currentCatalog" :color="getCatalogTypeColor(currentCatalog.catalogType)">
                {{ getCatalogTypeLabel(currentCatalog.catalogType) }}
              </a-tag>
            </div>
            <div class="asset-actions" v-if="currentCatalog">
              <a-space>
                <a-button size="small" @click="openAddAssetDrawer">
                  <template #icon><AppstoreAddOutlined /></template>
                  收录资产
                </a-button>
                <a-button size="small" @click="refreshCatalogCount">
                  <template #icon><ReloadOutlined /></template>
                  刷新数量
                </a-button>
              </a-space>
            </div>
          </div>

          <!-- 筛选区 -->
          <div class="filter-bar" v-if="currentCatalog">
            <a-space wrap>
              <a-input
                v-model:value="assetSearchKeyword"
                placeholder="搜索资产"
                style="width: 180px"
                allowClear
                @pressEnter="loadAssets"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </a-space>
          </div>

          <!-- 资产表格 -->
          <a-table
            v-if="currentCatalog"
            :columns="assetColumns"
            :data-source="assetData"
            :loading="assetLoading"
            :pagination="assetPagination"
            :row-key="(record: any) => `${record.metadataId}-${record.dsId}`"
            @change="handleAssetTableChange"
            :scroll="{ x: 1200 }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'tableName'">
                <div class="table-name-cell">
                  <FileOutlined class="table-icon" />
                  <div>
                    <div class="table-name-text">{{ record.tableName }}</div>
                    <div class="table-alias-text" v-if="record.tableAlias">{{ record.tableAlias }}</div>
                  </div>
                </div>
              </template>

              <template v-if="column.key === 'dsName'">
                <a-tag>{{ record.dsName }}</a-tag>
                <span class="ds-type-tag">{{ record.dsType }}</span>
              </template>

              <template v-if="column.key === 'dataLayer'">
                <a-tag :color="getLayerColor(record.dataLayer)">
                  {{ record.dataLayer || '-' }}
                </a-tag>
              </template>

              <template v-if="column.key === 'rowCount'">
                <span class="mono-text">{{ formatNumber(record.rowCount) }}</span>
              </template>

              <template v-if="column.key === 'sensitivityLevel'">
                <a-tag :color="getSensitivityColor(record.sensitivityLevel)">
                  {{ getSensitivityLabel(record.sensitivityLevel) }}
                </a-tag>
              </template>

              <template v-if="column.key === 'action'">
                <a-space>
                  <a-tooltip title="查看详情">
                    <a-button type="text" size="small" @click="openAssetDetail(record)">
                      <template #icon><EyeOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm
                    title="确定从目录中移除该资产？"
                    ok-text="确定"
                    cancel-text="取消"
                    @confirm="removeAsset(record)"
                  >
                    <a-tooltip title="移除">
                      <a-button type="text" size="small" danger>
                        <template #icon><CloseOutlined /></template>
                      </a-button>
                    </a-tooltip>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>

            <template #emptyText>
              <div class="empty-state">
                <InboxOutlined class="empty-icon" />
                <div class="empty-title">暂无收录资产</div>
                <div class="empty-desc">从元数据中收录数据表到当前目录</div>
                <a-button type="primary" @click="openAddAssetDrawer">
                  <template #icon><AppstoreAddOutlined /></template>
                  收录资产
                </a-button>
              </div>
            </template>
          </a-table>

          <!-- 未选择目录时的提示 -->
          <div v-if="!currentCatalog" class="no-catalog-tip">
            <InboxOutlined style="font-size: 48px; color: #d9d9d9" />
            <div style="margin-top: 16px; color: #8C8C8C">请从左侧选择目录以查看收录的资产</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 新增/编辑目录抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerMode === 'add' ? '新建目录' : '编辑目录'"
      :width="520"
      @close="drawerVisible = false"
    >
      <a-form
        ref="formRef"
        :model="form"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="formRules"
      >
        <a-form-item label="上级目录" name="parentId">
          <a-tree-select
            v-model:value="form.parentId"
            :tree-data="flatTreeData"
            :field-names="{ label: 'catalogName', value: 'id', children: 'children' }"
            placeholder="选择上级目录，留空则为顶级目录"
            allow-clear
            tree-default-expand-all
            :disabled="drawerMode === 'edit'"
          />
        </a-form-item>

        <a-form-item label="目录类型" name="catalogType">
          <a-select
            v-model:value="form.catalogType"
            placeholder="选择目录类型"
            :disabled="drawerMode === 'edit'"
          >
            <a-select-option value="BUSINESS_DOMAIN">
              <span><TagsOutlined /> 业务域</span>
            </a-select-option>
            <a-select-option value="DATA_DOMAIN">
              <span><FolderOutlined /> 数据域</span>
            </a-select-option>
            <a-select-option value="ALBUM">
              <span><FolderOpenOutlined /> 数据专辑</span>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="目录名称" name="catalogName">
          <a-input v-model:value="form.catalogName" placeholder="输入目录名称" :maxlength="100" show-count />
        </a-form-item>

        <a-form-item label="目录编码" name="catalogCode" v-if="drawerMode === 'add'">
          <a-input v-model:value="form.catalogCode" placeholder="输入目录编码，唯一标识" :maxlength="50" show-count />
          <div class="form-tip">建议使用英文或拼音，如 "order_domain"</div>
        </a-form-item>

        <a-form-item label="目录描述" name="catalogDesc">
          <a-textarea v-model:value="form.catalogDesc" placeholder="简要描述目录的用途和范围" :rows="3" :maxlength="500" show-count />
        </a-form-item>

        <!-- 数据专辑专属 -->
        <template v-if="form.catalogType === 'ALBUM'">
          <a-form-item label="专辑说明" name="albumDesc">
            <a-textarea v-model:value="form.albumDesc" placeholder="专辑的详细说明，可包含数据内容介绍" :rows="4" :maxlength="2000" show-count />
          </a-form-item>
          <a-form-item label="封面图片" name="coverImage">
            <a-input v-model:value="form.coverImage" placeholder="输入封面图片 URL" />
            <div class="form-tip">支持图片 URL 或上传后获得的地址</div>
          </a-form-item>
        </template>

        <a-form-item label="排序号" name="sortOrder">
          <a-input-number v-model:value="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
          <div class="form-tip">数字越小排序越靠前</div>
        </a-form-item>

        <a-form-item label="可见性" name="visible">
          <a-radio-group v-model:value="form.visible">
            <a-radio :value="1">公开</a-radio>
            <a-radio :value="0">私有</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio value="ACTIVE">启用</a-radio>
            <a-radio value="INACTIVE">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 收录资产抽屉 -->
    <a-drawer
      v-model:open="assetDrawerVisible"
      title="收录资产"
      :width="720"
      @close="assetDrawerVisible = false"
    >
      <div class="asset-picker">
        <div class="picker-header">
          <a-space wrap>
            <a-select
              v-model:value="pickerDsId"
              placeholder="选择数据源"
              style="width: 180px"
              allowClear
              :loading="dsLoading"
              @change="loadPickerTables"
            >
              <a-select-option v-for="ds in dataSourceList" :key="ds.id" :value="ds.id">
                {{ ds.dsName }} ({{ ds.dsType }})
              </a-select-option>
            </a-select>
            <a-input
              v-model:value="pickerKeyword"
              placeholder="搜索表名/中文名"
              style="width: 160px"
              allowClear
              @pressEnter="loadPickerTables"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-button @click="loadPickerTables">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
          </a-space>
        </div>

        <div class="picker-info" v-if="currentCatalog">
          <span>收录到：</span>
          <a-tag :color="getCatalogTypeColor(currentCatalog.catalogType)">
            {{ currentCatalog.catalogName }}
          </a-tag>
        </div>

        <a-transfer
          v-model:target-keys="selectedMetadataIds"
          :data-source="transferData"
          :titles="['可选资产', '已选资产']"
          :render="(item: any) => item.title"
          :row-key="(item: any) => item.key"
          :list-style="{ width: '280px', height: '400px' }"
          show-search
          :filter-option="(input: string, item: any) => item.title.toLowerCase().indexOf(input.toLowerCase()) >= 0"
          @change="handleTransferChange"
        />
      </div>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="assetDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="addingAssets" @click="handleAddAssets" :disabled="selectedMetadataIds.length === 0">
            确认收录 ({{ selectedMetadataIds.length }})
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- 资产详情抽屉 -->
    <a-drawer
      v-model:open="assetDetailVisible"
      title="资产详情"
      :width="600"
      @close="assetDetailVisible = false"
    >
      <div v-if="assetDetail" class="detail-content">
        <a-descriptions :column="2" size="small" bordered title="基本信息">
          <a-descriptions-item label="表名">{{ assetDetail.tableName }}</a-descriptions-item>
          <a-descriptions-item label="中文名">{{ assetDetail.tableAlias || '-' }}</a-descriptions-item>
          <a-descriptions-item label="所属数据源" :span="2">
            <a-tag>{{ assetDetail.dsName }}</a-tag>
            <span class="ds-type-tag">{{ assetDetail.dsType }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="数据层">
            <a-tag :color="getLayerColor(assetDetail.dataLayer)">{{ assetDetail.dataLayer || '-' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="数据域">{{ assetDetail.dataDomain || '-' }}</a-descriptions-item>
          <a-descriptions-item label="行数">{{ formatNumber(assetDetail.rowCount) }}</a-descriptions-item>
          <a-descriptions-item label="存储大小">{{ formatBytes(assetDetail.storageBytes) }}</a-descriptions-item>
          <a-descriptions-item label="敏感等级">
            <a-tag :color="getSensitivityColor(assetDetail.sensitivityLevel)">
              {{ getSensitivityLabel(assetDetail.sensitivityLevel) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="表注释" :span="2">{{ assetDetail.tableComment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="最后修改">{{ assetDetail.lastModifiedAt || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收录时间">{{ assetDetail.createTime }}</a-descriptions-item>
        </a-descriptions>

        <div class="detail-actions">
          <a-button type="link" @click="goToMetadata">
            <template #icon><ArrowRightOutlined /></template>
            查看元数据详情
          </a-button>
        </div>
      </div>
    </a-drawer>

    <!-- 移动目录抽屉 -->
    <a-drawer
      v-model:open="moveDrawerVisible"
      title="移动目录"
      :width="400"
      @close="moveDrawerVisible = false"
    >
      <div class="move-tip">将目录移动到其他位置</div>
      <div class="move-source" v-if="moveTargetCatalog">
        <span class="move-label">当前目录：</span>
        <a-tag>{{ moveTargetCatalog.catalogName }}</a-tag>
      </div>
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="移动到">
          <a-tree-select
            v-model:value="moveParentId"
            :tree-data="flatTreeDataForMove"
            :field-names="{ label: 'catalogName', value: 'id', children: 'children' }"
            placeholder="选择目标父目录，移到根目录请选择空"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>
      </a-form>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="moveDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="moving" @click="handleMove">确认移动</a-button>
        </a-space>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TreeProps } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SearchOutlined, FolderOutlined,
  InboxOutlined, FileOutlined, EyeOutlined, EditOutlined,
  DeleteOutlined, MoreOutlined, AppstoreAddOutlined, SwapOutlined,
  FolderOpenOutlined, TagsOutlined, CloseOutlined, CaretDownOutlined,
  ArrowRightOutlined
} from '@ant-design/icons-vue'
import { assetCatalogApi } from '@/api/asset'
import type { AssetCatalogTreeVO, CatalogAssetVO, AssetCatalogSaveDTO } from '@/api/asset'
import { metadataApi } from '@/api/gov'
import { dataSourceApi } from '@/api/dqc'
import { useRouter } from 'vue-router'

const router = useRouter()

// ============ 状态 ============
const treeLoading = ref(false)
const saving = ref(false)
const addingAssets = ref(false)
const moving = ref(false)
const dsLoading = ref(false)

// ============ 目录树 ============
const treeData = ref<AssetCatalogTreeVO[]>([])
const selectedKeys = ref<number[]>([])
const expandedKeys = ref<number[]>([])
const autoExpandParent = ref(true)
const currentCatalog = ref<AssetCatalogTreeVO | null>(null)
const treeFilterType = ref('')

// ============ 资产列表 ============
const assetLoading = ref(false)
const assetData = ref<CatalogAssetVO[]>([])
const assetPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const assetSearchKeyword = ref('')

// ============ 抽屉 ============
const drawerVisible = ref(false)
const drawerMode = ref<'add' | 'edit'>('add')
const drawerParentType = ref<string | undefined>()
const formRef = ref<FormInstance>()

const form = reactive<AssetCatalogSaveDTO>({
  parentId: undefined,
  catalogType: undefined,
  catalogName: '',
  catalogCode: '',
  catalogDesc: '',
  albumDesc: '',
  coverImage: '',
  sortOrder: 0,
  visible: 1,
  status: 'ACTIVE'
})

const formRules = {
  catalogType: [{ required: true, message: '请选择目录类型' }],
  catalogName: [{ required: true, message: '请输入目录名称' }],
  catalogCode: [{ required: true, message: '请输入目录编码' }]
}

// ============ 收录资产 ============
const assetDrawerVisible = ref(false)
const assetDetailVisible = ref(false)
const pickerDsId = ref<number | undefined>()
const pickerKeyword = ref('')
const selectedMetadataIds = ref<string[]>([])
const transferData = ref<any[]>([])
const dataSourceList = ref<any[]>([])
const assetDetail = ref<CatalogAssetVO | null>(null)

// ============ 移动目录 ============
const moveDrawerVisible = ref(false)
const moveTargetCatalog = ref<AssetCatalogTreeVO | null>(null)
const moveParentId = ref<number | undefined>()

// ============ 统计数据 ============
const statCards = ref([
  { label: '总目录数', value: 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' },
  { label: '业务域', value: 0, bg: 'linear-gradient(135deg, #1677FF 0%, #00B4DB 100%)' },
  { label: '数据域', value: 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '数据专辑', value: 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' }
])

const layerOptions = [
  { label: 'ODS（原始层）', value: 'ODS' },
  { label: 'DWD（明细层）', value: 'DWD' },
  { label: 'DWS（汇总层）', value: 'DWS' },
  { label: 'ADS（应用层）', value: 'ADS' }
]

// ============ 资产表格列 ============
const assetColumns = [
  { title: '表名', key: 'tableName', width: 220, ellipsis: true },
  { title: '所属数据源', key: 'dsName', width: 180 },
  { title: '数据层', key: 'dataLayer', width: 100 },
  { title: '行数', key: 'rowCount', width: 120, align: 'right' },
  { title: '敏感等级', key: 'sensitivityLevel', width: 110 },
  { title: '最后修改', dataIndex: 'lastModifiedAt', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

// ============ 目录树扁平化（用于选择上级） ============
const flatTreeData = computed(() => {
  const result: AssetCatalogTreeVO[] = []
  function flatten(nodes: AssetCatalogTreeVO[], excludeId?: number) {
    for (const node of nodes) {
      if (node.id !== excludeId) {
        result.push({ ...node, children: undefined })
        if (node.children?.length) {
          flatten(node.children, excludeId)
        }
      }
    }
  }
  flatten(treeData.value, currentCatalog.value?.id)
  return result
})

const flatTreeDataForMove = computed(() => {
  const result: AssetCatalogTreeVO[] = []
  function flatten(nodes: AssetCatalogTreeVO[], excludeId?: number) {
    for (const node of nodes) {
      if (node.id !== excludeId) {
        result.push({ ...node, children: undefined })
        if (node.children?.length) {
          flatten(node.children, excludeId)
        }
      }
    }
  }
  flatten(treeData.value, moveTargetCatalog.value?.id)
  return result
})

// ============ 加载目录树 ============
async function loadTree() {
  treeLoading.value = true
  try {
    const params: any = {}
    if (treeFilterType.value) params.catalogType = treeFilterType.value
    const res = await assetCatalogApi.getTree(params)
    if (res.data?.success !== false) {
      treeData.value = res.data || []
      // 默认展开第一层
      expandedKeys.value = treeData.value.filter(n => n.id).map(n => n.id!)
    }
  } catch {
    message.error('加载目录树失败')
  } finally {
    treeLoading.value = false
  }
}

// ============ 加载统计数据 ============
async function loadStats() {
  try {
    const res = await assetCatalogApi.getStats()
    if (res.data?.success !== false) {
      const s = res.data?.data
      statCards.value[0].value = s?.totalCount || 0
      statCards.value[1].value = s?.businessDomainCount || 0
      statCards.value[2].value = s?.dataDomainCount || 0
      statCards.value[3].value = s?.albumCount || 0
    }
  } catch { /* ignore */ }
}

// ============ 加载资产列表 ============
async function loadAssets() {
  if (!currentCatalog.value?.id) return
  assetLoading.value = true
  try {
    const res = await assetCatalogApi.getAssets(currentCatalog.value.id, {
      pageNum: assetPagination.current,
      pageSize: assetPagination.pageSize
    })
    if (res.data?.success !== false) {
      assetData.value = res.data?.records || []
      assetPagination.total = res.data?.total || 0
    }
  } catch {
    message.error('加载资产列表失败')
  } finally {
    assetLoading.value = false
  }
}

// ============ 加载数据源（收录资产用） ============
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

// ============ 加载可选资产（穿梭框） ============
async function loadPickerTables() {
  if (!pickerDsId.value) {
    transferData.value = []
    return
  }
  try {
    const res = await metadataApi.page({
      pageNum: 1,
      pageSize: 500,
      dsId: pickerDsId.value,
      tableName: pickerKeyword.value || undefined,
      status: 'ACTIVE'
    })
    if (res.data?.success !== false) {
      const records = res.data?.records || []
      transferData.value = records.map((r: any) => ({
        key: `${r.id}-${r.dsId}`,
        title: `${r.tableName}${r.tableAlias ? ' (' + r.tableAlias + ')' : ''}`,
        description: `${r.dataLayer || ''} | ${r.tableComment || ''}`,
        disabled: false
      }))
    }
  } catch { /* ignore */ }
}

// ============ 树操作 ============
function handleTreeSelect(keys: any[]) {
  const id = keys[0]
  if (!id) {
    currentCatalog.value = null
    selectedKeys.value = []
    assetData.value = []
    assetPagination.total = 0
    return
  }
  selectedKeys.value = [id]
  // 找到当前选中的节点
  function findNode(nodes: AssetCatalogTreeVO[]): AssetCatalogTreeVO | null {
    for (const node of nodes) {
      if (node.id === id) return node
      if (node.children?.length) {
        const found = findNode(node.children)
        if (found) return found
      }
    }
    return null
  }
  currentCatalog.value = findNode(treeData.value)
  assetPagination.current = 1
  loadAssets()
}

function handleTreeExpand(keys: number[]) {
  expandedKeys.value = keys
}

function handleNodeAction(key: string, node: any) {
  const catalog = node as AssetCatalogTreeVO
  switch (key) {
    case 'add-child':
      openAddDrawer(catalog.catalogType === 'BUSINESS_DOMAIN' ? 'DATA_DOMAIN' : 'ALBUM', catalog.id)
      break
    case 'add-asset':
      selectedKeys.value = [catalog.id!]
      currentCatalog.value = catalog
      openAddAssetDrawer()
      break
    case 'edit':
      openEditDrawer(catalog)
      break
    case 'move':
      openMoveDrawer(catalog)
      break
    case 'delete':
      handleDeleteCatalog(catalog)
      break
  }
}

// ============ 打开新增/编辑抽屉 ============
function openAddDrawer(type: string, parentId?: number) {
  drawerMode.value = 'add'
  drawerParentType.value = type
  Object.assign(form, {
    id: undefined,
    parentId: parentId || undefined,
    catalogType: type as any,
    catalogName: '',
    catalogCode: '',
    catalogDesc: '',
    albumDesc: '',
    coverImage: '',
    sortOrder: 0,
    visible: 1,
    status: 'ACTIVE'
  })
  drawerVisible.value = true
}

function openEditDrawer(catalog: AssetCatalogTreeVO) {
  drawerMode.value = 'edit'
  Object.assign(form, {
    id: catalog.id,
    parentId: catalog.parentId,
    catalogType: catalog.catalogType,
    catalogName: catalog.catalogName,
    catalogCode: catalog.catalogCode,
    catalogDesc: catalog.catalogDesc,
    albumDesc: catalog.albumDesc,
    coverImage: catalog.coverImage,
    sortOrder: catalog.sortOrder || 0,
    visible: catalog.visible,
    status: catalog.status
  })
  drawerVisible.value = true
}

function openAddAssetDrawer() {
  if (!currentCatalog.value?.id) {
    message.warning('请先选择目录')
    return
  }
  selectedMetadataIds.value = []
  pickerDsId.value = undefined
  pickerKeyword.value = ''
  transferData.value = []
  assetDrawerVisible.value = true
}

function openMoveDrawer(catalog: AssetCatalogTreeVO) {
  moveTargetCatalog.value = catalog
  moveParentId.value = catalog.parentId
  moveDrawerVisible.value = true
}

function openAssetDetail(record: CatalogAssetVO) {
  assetDetail.value = record
  assetDetailVisible.value = true
}

// ============ 保存目录 ============
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (drawerMode.value === 'add') {
      const res = await assetCatalogApi.create(form as AssetCatalogSaveDTO)
      if (res.data?.success !== false) {
        message.success('创建成功')
        drawerVisible.value = false
        loadTree()
        loadStats()
      }
    } else {
      const res = await assetCatalogApi.update(form.id!, form as AssetCatalogSaveDTO)
      if (res.data?.success !== false) {
        message.success('保存成功')
        drawerVisible.value = false
        loadTree()
        loadStats()
      }
    }
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ============ 删除目录 ============
async function handleDeleteCatalog(catalog: AssetCatalogTreeVO) {
  try {
    await Modal.confirm({
      title: '确认删除',
      content: `确定删除目录「${catalog.catalogName}」？${catalog.itemCount ? `该目录下有 ${catalog.itemCount} 个资产，将一并移除关联关系。` : ''} 子目录也会被一并删除。`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true }
    })
  } catch { return }
  try {
    const res = await assetCatalogApi.delete(catalog.id!)
    if (res.data?.success !== false) {
      message.success('删除成功')
      if (currentCatalog.value?.id === catalog.id) {
        currentCatalog.value = null
        assetData.value = []
        assetPagination.total = 0
      }
      loadTree()
      loadStats()
    }
  } catch { message.error('删除失败') }
}

// ============ 收录资产 ============
function handleTransferChange(targetKeys: string[]) {
  selectedMetadataIds.value = targetKeys
}

async function handleAddAssets() {
  if (!currentCatalog.value?.id || selectedMetadataIds.value.length === 0) return
  addingAssets.value = true
  try {
    const metadataIds = selectedMetadataIds.value.map(k => parseInt(k.split('-')[0]))
    const res = await assetCatalogApi.addAssets(currentCatalog.value.id, metadataIds)
    if (res.data?.success !== false) {
      message.success(`成功收录 ${metadataIds.length} 个资产`)
      assetDrawerVisible.value = false
      loadAssets()
      loadTree()
      loadStats()
    }
  } catch {
    message.error('收录失败')
  } finally {
    addingAssets.value = false
  }
}

// ============ 移除资产 ============
async function removeAsset(record: CatalogAssetVO) {
  if (!currentCatalog.value?.id) return
  try {
    const res = await assetCatalogApi.removeAssets(currentCatalog.value.id, [record.metadataId!])
    if (res.data?.success !== false) {
      message.success('已移除')
      loadAssets()
      loadTree()
      loadStats()
    }
  } catch {
    message.error('移除失败')
  }
}

// ============ 刷新目录数量 ============
async function refreshCatalogCount() {
  if (!currentCatalog.value?.id) return
  try {
    await assetCatalogApi.refreshCount(currentCatalog.value.id)
    message.success('已刷新')
    loadTree()
    loadStats()
  } catch {
    message.error('刷新失败')
  }
}

// ============ 移动目录 ============
async function handleMove() {
  if (!moveTargetCatalog.value?.id) return
  moving.value = true
  try {
    const res = await assetCatalogApi.move(moveTargetCatalog.value.id, moveParentId || 0)
    if (res.data?.success !== false) {
      message.success('移动成功')
      moveDrawerVisible.value = false
      loadTree()
    }
  } catch {
    message.error('移动失败')
  } finally {
    moving.value = false
  }
}

// ============ 查看元数据详情 ============
function goToMetadata() {
  if (assetDetail.value?.metadataId) {
    router.push(`/governance/metadata?id=${assetDetail.value.metadataId}`)
  }
}

// ============ 表格分页 ============
function handleAssetTableChange(pagination: any) {
  assetPagination.current = pagination.current
  assetPagination.pageSize = pagination.pageSize
  loadAssets()
}

// ============ 辅助方法 ============
function getCatalogTypeColor(type?: string) {
  const map: Record<string, string> = {
    BUSINESS_DOMAIN: '#1677FF',
    DATA_DOMAIN: '#52C41A',
    ALBUM: '#722ED1'
  }
  return map[type || ''] || '#8C8C8C'
}

function getCatalogTypeLabel(type?: string) {
  const map: Record<string, string> = {
    BUSINESS_DOMAIN: '业务域',
    DATA_DOMAIN: '数据域',
    ALBUM: '数据专辑'
  }
  return map[type || ''] || type || '-'
}

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
  loadTree()
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
.header-right { display: flex; gap: 8px; }

.stat-mini-card {
  border-radius: 8px;
  padding: 16px 20px;
  color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.mini-value { font-size: 24px; font-weight: 700; }
.mini-label { font-size: 12px; opacity: 0.9; margin-top: 4px; }

.tree-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
  height: calc(100vh - 280px);
  display: flex;
  flex-direction: column;
}
.tree-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.tree-title { font-weight: 600; font-size: 14px; }
.tree-filter {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}
.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: #8C8C8C;
}
.empty-icon { font-size: 40px; color: #d9d9d9; margin-bottom: 8px; }
.empty-text { font-size: 13px; margin-bottom: 8px; }

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}
.node-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.node-count {
  background: #f0f0f0;
  color: #8C8C8C;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  line-height: 18px;
}
.node-action { padding: 0 4px !important; opacity: 0; transition: opacity 0.2s; }
:deep(.ant-tree-node-content-wrapper) { width: 100%; }
:deep(.ant-tree-node-content-wrapper:hover .node-action) { opacity: 1; }

.asset-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}
.asset-title { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 15px; }
.asset-actions { display: flex; gap: 8px; }

.filter-bar { margin-bottom: 12px; }

.table-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 16px;
  height: calc(100vh - 280px);
  display: flex;
  flex-direction: column;
}

.no-catalog-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 300px;
}

.table-name-cell { display: flex; align-items: flex-start; gap: 8px; }
.table-icon { color: #722ED1; margin-top: 2px; }
.table-name-text { font-weight: 500; font-family: 'JetBrains Mono', monospace; font-size: 13px; }
.table-alias-text { font-size: 12px; color: #8C8C8C; margin-top: 2px; }
.ds-type-tag {
  background: #f0f0f0;
  color: #8C8C8C;
  border-radius: 4px;
  padding: 0 4px;
  font-size: 11px;
  margin-left: 4px;
}
.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.empty-state {
  text-align: center;
  padding: 40px 20px;
}
.empty-icon { font-size: 48px; color: #d9d9d9; margin-bottom: 16px; }
.empty-title { font-size: 16px; color: #1F1F1F; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #8C8C8C; margin-bottom: 16px; }

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

.asset-picker { display: flex; flex-direction: column; gap: 12px; }
.picker-header { display: flex; flex-wrap: wrap; gap: 8px; }
.picker-info { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #8C8C8C; }

.detail-content { padding-bottom: 60px; }
.detail-actions { margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }

.move-tip { font-size: 13px; color: #8C8C8C; margin-bottom: 12px; }
.move-source { display: flex; align-items: center; gap: 4px; margin-bottom: 16px; }
.move-label { font-size: 13px; color: #8C8C8C; }

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }

.text-muted { color: #8C8C8C; }
</style>
