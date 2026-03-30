<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#menuGrad)"/>
            <path d="M4 6h16M4 10h10M4 14h12M4 18h8" stroke="white" stroke-width="1.8" stroke-linecap="round"/>
            <defs>
              <linearGradient id="menuGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#52C41A"/>
                <stop offset="100%" stop-color="#237804"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">菜单管理</h1>
          <p class="page-subtitle">管理系统菜单，支持菜单树展示、新增、编辑、删除</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="handleExpand">
          <template #icon><ExpandAltOutlined /></template>
          {{ expanded ? '收起全部' : '展开全部' }}
        </a-button>
        <a-button type="primary" @click="openCreateDrawer(0)">
          <template #icon><PlusOutlined /></template>
          新增菜单
        </a-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        :row-key="(record: any) => record.id"
        :default-expand-all-rows="expanded"
        :expandRowByClick="false"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuName'">
            <span style="display: flex; align-items: center; gap: 6px">
              <component :is="getIconComponent(record.icon)" v-if="record.icon" style="font-size: 16px; color: #1677ff" />
              <span>{{ record.menuName }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'menuType'">
            <a-tag :color="getMenuTypeColor(record.menuType)">
              {{ getMenuTypeText(record.menuType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'visible'">
            <a-badge
              :status="record.visible === 1 ? 'success' : 'error'"
              :text="record.visible === 1 ? '显示' : '隐藏'"
            />
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : 'error'"
              :text="record.status === 1 ? '启用' : '禁用'"
            />
          </template>
          <template v-else-if="column.key === 'cached'">
            <a-tag :color="record.cached === 1 ? 'green' : 'default'">
              {{ record.cached === 1 ? '缓存' : '不缓存' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openCreateDrawer(record.id)" style="color: #1677ff">新增子菜单</a>
              <a-divider type="vertical" />
              <a @click="openEditDrawer(record)" style="color: #1677ff">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                :title="`确定要删除菜单「${record.menuName}」吗？`"
                ok-text="确认删除"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a style="color: #ff4d4f">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerTitle"
      width="520"
      :closable="true"
      @close="closeDrawer"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 17 }"
      >
        <a-form-item label="上级菜单" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            placeholder="请选择上级菜单（不选则为顶级）"
            :tree-data="menuSelectTree"
            :field-names="{ label: 'menuName', value: 'id', children: 'children' }"
            allowClear
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="菜单类型" name="menuType">
          <a-radio-group v-model:value="formData.menuType">
            <a-radio value="CATALOG">目录</a-radio>
            <a-radio value="MENU">菜单</a-radio>
            <a-radio value="BUTTON">按钮</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="菜单名称" name="menuName">
          <a-input v-model:value="formData.menuName" placeholder="请输入菜单名称" maxlength="50" />
        </a-form-item>
        <a-form-item label="菜单编码" name="menuCode">
          <a-input v-model:value="formData.menuCode" placeholder="请输入菜单编码" maxlength="50" />
        </a-form-item>
        <a-form-item v-if="formData.menuType !== 'BUTTON'" label="路由路径" name="path">
          <a-input v-model:value="formData.path" placeholder="请输入路由路径" maxlength="200" />
        </a-form-item>
        <a-form-item v-if="formData.menuType === 'MENU'" label="组件路径" name="component">
          <a-input v-model:value="formData.component" placeholder="请输入组件路径，如 views/xxx/index" maxlength="255" />
        </a-form-item>
        <a-form-item v-if="formData.menuType !== 'BUTTON'" label="菜单图标" name="icon">
          <a-input v-model:value="formData.icon" placeholder="请输入图标名称，如 DashboardOutlined" maxlength="100" />
        </a-form-item>
        <a-form-item label="权限标识" name="perms">
          <a-input v-model:value="formData.perms" placeholder="请输入权限标识，如 system:user:list" maxlength="100" />
        </a-form-item>
        <a-form-item label="排序号" name="sortOrder">
          <a-input-number v-model:value="formData.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="formData.menuType !== 'BUTTON'" label="是否显示" name="visible">
          <a-radio-group v-model:value="formData.visible">
            <a-radio :value="1">显示</a-radio>
            <a-radio :value="0">隐藏</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="formData.menuType === 'MENU'" label="是否缓存" name="cached">
          <a-radio-group v-model:value="formData.cached">
            <a-radio :value="1">缓存</a-radio>
            <a-radio :value="0">不缓存</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="closeDrawer">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '确认创建' }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import {
  PlusOutlined, ExpandAltOutlined,
  DashboardOutlined, UserOutlined, TeamOutlined, SettingOutlined,
  SafetyCertificateOutlined, FundOutlined, AccountBookOutlined, AlertOutlined,
  TableOutlined, DatabaseOutlined, FolderOutlined, FileTextOutlined,
  LockOutlined, BellOutlined, EyeOutlined, ExperimentOutlined,
  MenuOutlined, ControlOutlined, ApartmentOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import {
  getMenuTree,
  getMenuDetail,
  createMenu,
  updateMenu,
  deleteMenu,
  type SysMenu
} from '@/api/system'
import { useMenuStore } from '@/stores/menu'

const loading = ref(false)
const dataSource = ref<SysMenu[]>([])
const submitLoading = ref(false)
const drawerVisible = ref(false)
const isEdit = ref(false)
const drawerTitle = computed(() => isEdit.value ? '编辑菜单' : '新增菜单')
const expanded = ref(true)
const menuSelectTree = ref<any[]>([])
const menuStore = useMenuStore()

const columns: TableColumnType[] = [
  {
    title: '菜单名称',
    dataIndex: 'menuName',
    key: 'menuName',
    width: 200
  },
  {
    title: '菜单类型',
    key: 'menuType',
    width: 100
  },
  {
    title: '路由路径',
    dataIndex: 'path',
    key: 'path',
    width: 180,
    ellipsis: true
  },
  {
    title: '权限标识',
    dataIndex: 'perms',
    key: 'perms',
    width: 160,
    ellipsis: true
  },
  {
    title: '显示',
    key: 'visible',
    width: 80
  },
  {
    title: '缓存',
    key: 'cached',
    width: 80
  },
  {
    title: '排序',
    dataIndex: 'sortOrder',
    key: 'sortOrder',
    width: 70
  },
  {
    title: '状态',
    key: 'status',
    width: 80
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

const formRef = ref()
const formData = reactive<Record<string, any>>({
  parentId: undefined as number | undefined,
  menuType: 'MENU',
  menuName: '',
  menuCode: '',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  visible: 1,
  cached: 0,
  status: 1,
  perms: ''
})

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

function getMenuTypeText(type: string): string {
  const map: Record<string, string> = {
    'CATALOG': '目录',
    'MENU': '菜单',
    'BUTTON': '按钮'
  }
  return map[type] || type
}

function getMenuTypeColor(type: string): string {
  const map: Record<string, string> = {
    'CATALOG': 'blue',
    'MENU': 'green',
    'BUTTON': 'orange'
  }
  return map[type] || 'default'
}

function getIconComponent(iconName: string | undefined) {
  if (!iconName) return null
  const iconMap: Record<string, any> = {
    'dashboard': DashboardOutlined,
    'DashboardOutlined': DashboardOutlined,
    'user': UserOutlined,
    'UserOutlined': UserOutlined,
    'team': TeamOutlined,
    'TeamOutlined': TeamOutlined,
    'setting': SettingOutlined,
    'SettingOutlined': SettingOutlined,
    'SafetyCertificateOutlined': SafetyCertificateOutlined,
    'FundOutlined': FundOutlined,
    'AccountBookOutlined': AccountBookOutlined,
    'AlertOutlined': AlertOutlined,
    'table': TableOutlined,
    'TableOutlined': TableOutlined,
    'database': DatabaseOutlined,
    'DatabaseOutlined': DatabaseOutlined,
    'folder': FolderOutlined,
    'FolderOutlined': FolderOutlined,
    'file-text': FileTextOutlined,
    'FileTextOutlined': FileTextOutlined,
    'lock': LockOutlined,
    'LockOutlined': LockOutlined,
    'bell': BellOutlined,
    'BellOutlined': BellOutlined,
    'eye': EyeOutlined,
    'EyeOutlined': EyeOutlined,
    'experiment': ExperimentOutlined,
    'ExperimentOutlined': ExperimentOutlined,
    'menu': MenuOutlined,
    'MenuOutlined': MenuOutlined,
    'control': ControlOutlined,
    'ControlOutlined': ControlOutlined,
    'apartment': ApartmentOutlined,
    'ApartmentOutlined': ApartmentOutlined,
    'appstore': AppstoreOutlined,
    'AppstoreOutlined': AppstoreOutlined
  }
  return iconMap[iconName] || MenuOutlined
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMenuTree()
    dataSource.value = res.data || []
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadMenuSelectTree() {
  try {
    const res = await getMenuTree()
    menuSelectTree.value = buildSelectTree(res.data || [])
  } catch {
    // ignore
  }
}

function buildSelectTree(menus: any[], parentId: number = 0): any[] {
  return menus
    .filter(m => m.parentId === parentId)
    .map(m => ({
      id: m.id,
      menuName: m.menuName,
      children: buildSelectTree(menus, m.id)
    }))
}

function handleExpand() {
  expanded.value = !expanded.value
}

function openCreateDrawer(parentId: number | undefined) {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    parentId: parentId || undefined,
    menuType: 'MENU',
    menuName: '',
    menuCode: '',
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    visible: 1,
    cached: 0,
    status: 1,
    perms: ''
  })
  drawerVisible.value = true
}

function openEditDrawer(record: SysMenu) {
  isEdit.value = true
  Object.assign(formData, {
    id: record.id,
    parentId: record.parentId && record.parentId !== 0 ? record.parentId : undefined,
    menuType: record.menuType || 'MENU',
    menuName: record.menuName || '',
    menuCode: record.menuCode || '',
    path: record.path || '',
    component: record.component || '',
    icon: record.icon || '',
    sortOrder: record.sortOrder || 0,
    visible: record.visible !== undefined ? record.visible : 1,
    cached: record.cached !== undefined ? record.cached : 0,
    status: record.status || 1,
    perms: record.perms || ''
  })
  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    const payload: Record<string, any> = {
      parentId: formData.parentId || 0,
      menuType: formData.menuType,
      menuName: formData.menuName,
      menuCode: formData.menuCode,
      path: formData.path,
      component: formData.component,
      icon: formData.icon,
      sortOrder: formData.sortOrder,
      visible: formData.visible,
      cached: formData.cached,
      status: formData.status,
      perms: formData.perms
    }
    if (isEdit.value) {
      await updateMenu(formData.id, payload)
      message.success('修改成功')
    } else {
      await createMenu(payload)
      message.success('创建成功')
    }
    // 通知侧边栏刷新菜单
    menuStore.incrementVersion()
    closeDrawer()
    loadData()
  } catch {
    // error handled
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(record: SysMenu) {
  try {
    await deleteMenu(record.id!)
    message.success('删除成功')
    // 通知侧边栏刷新菜单
    menuStore.incrementVersion()
    loadData()
  } catch {
    // error handled
  }
}

onMounted(() => {
  loadData()
  loadMenuSelectTree()
})
</script>

<style lang="less" scoped>
.page-container {
  padding: 24px;
  min-height: 100%;
  background: #f5f5f5;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .header-icon {
    display: flex;
    align-items: center;
  }
  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #1f1f1f;
    margin: 0;
    line-height: 1.2;
  }
  .page-subtitle {
    font-size: 13px;
    color: #8c8c8c;
    margin: 4px 0 0;
  }
  .header-right {
    display: flex;
    gap: 8px;
  }
}
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
</style>
