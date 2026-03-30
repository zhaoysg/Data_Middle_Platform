<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#roleGrad)"/>
            <circle cx="9" cy="9" r="2.5" stroke="white" stroke-width="1.5"/>
            <circle cx="15" cy="9" r="2.5" stroke="white" stroke-width="1.5"/>
            <path d="M5 17c0-2.21 1.79-4 4-4h6c2.21 0 4 1.79 4 4" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <defs>
              <linearGradient id="roleGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#EB8C00"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">角色管理</h1>
          <p class="page-subtitle">管理系统角色，支持角色创建、编辑、删除、权限分配</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="openCreateDrawer">
          <template #icon><PlusOutlined /></template>
          新增角色
        </a-button>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="filters.roleName"
          placeholder="搜索角色名称"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-input
          v-model:value="filters.roleCode"
          placeholder="搜索角色编码"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filters.status"
          placeholder="角色状态"
          style="width: 120px"
          allowClear
        >
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
        <a-button @click="resetFilters">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </a-space>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="(record: any) => record.id"
      :scroll="{ x: 900 }"
      @change="handleTableChange"
      style="margin-top: 16px"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge
            :status="record.status === 1 ? 'success' : 'error'"
            :text="record.status === 1 ? '启用' : '禁用'"
          />
        </template>
        <template v-else-if="column.key === 'roleType'">
          <a-tag :color="record.roleType === 'SYSTEM' ? 'blue' : 'purple'">
            {{ record.roleType === 'SYSTEM' ? '系统角色' : '自定义' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'dataScope'">
          {{ getDataScopeText(record.dataScope) }}
        </template>
        <template v-else-if="column.key === 'createTime'">
          <span v-if="record.createTime">{{ record.createTime }}</span>
          <span v-else style="color: #999">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="openEditDrawer(record)" style="color: #1677ff">编辑</a>
            <a-divider type="vertical" />
            <a
              v-if="record.status === 1"
              @click="handleToggleStatus(record)"
              style="color: #fa8c16"
            >禁用</a>
            <a
              v-else
              @click="handleToggleStatus(record)"
              style="color: #52c41a"
            >启用</a>
            <a-divider type="vertical" />
            <a @click="openMenuDrawer(record)" style="color: #722ed1">分配权限</a>
            <a-divider type="vertical" />
            <a-popconfirm
              :title="`确定要删除角色「${record.roleName}」吗？`"
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

    <!-- 新增/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerTitle"
      width="480"
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
        <a-form-item label="角色名称" name="roleName">
          <a-input
            v-model:value="formData.roleName"
            placeholder="请输入角色名称"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item label="角色编码" name="roleCode">
          <a-input
            v-model:value="formData.roleCode"
            placeholder="请输入角色编码，如 ROLE_ADMIN"
            maxlength="50"
            :disabled="isEdit"
          />
        </a-form-item>
        <a-form-item label="角色类型" name="roleType">
          <a-select v-model:value="formData.roleType" placeholder="请选择角色类型">
            <a-select-option value="CUSTOM">自定义角色</a-select-option>
            <a-select-option value="SYSTEM">系统角色</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据权限" name="dataScope">
          <a-select v-model:value="formData.dataScope" placeholder="请选择数据权限范围">
            <a-select-option value="CUSTOM">自定义（仅本人数据）</a-select-option>
            <a-select-option value="DEPT">本部门（所在部门数据）</a-select-option>
            <a-select-option value="ALL">全部（所有数据）</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排序号" name="sortOrder">
          <a-input-number
            v-model:value="formData.sortOrder"
            :min="0"
            :max="9999"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="请输入备注信息"
            :rows="3"
            maxlength="500"
          />
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

    <!-- 分配权限抽屉 -->
    <a-drawer
      v-model:open="menuDrawerVisible"
      title="分配菜单权限"
      width="520"
      :closable="true"
      @close="closeMenuDrawer"
    >
      <div class="menu-assign-tip">勾选该角色可访问的菜单权限</div>
      <a-tree
        v-model:checkedKeys="checkedMenuKeys"
        :tree-data="menuTreeData"
        :replace-fields="{ title: 'menuName', key: 'id' }"
        checkable
        :selectable="false"
        style="background: #fafafa; border-radius: 4px; padding: 8px"
      />
      <template #footer>
        <a-space>
          <a-button @click="closeMenuDrawer">取消</a-button>
          <a-button type="primary" :loading="menuSubmitLoading" @click="handleMenuSubmit">
            保存权限
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
  SearchOutlined, PlusOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import {
  getRolePage,
  createRole,
  updateRole,
  deleteRole,
  updateRoleStatus,
  getRoleMenus,
  assignRoleMenus,
  getMenuTree,
  type SysRole
} from '@/api/system'

const loading = ref(false)
const dataSource = ref<SysRole[]>([])
const submitLoading = ref(false)
const drawerVisible = ref(false)
const isEdit = ref(false)
const drawerTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')

// 分配权限相关
const menuDrawerVisible = ref(false)
const menuSubmitLoading = ref(false)
const currentRoleId = ref<number | null>(null)
const checkedMenuKeys = ref<number[]>([])
const menuTreeData = ref<Record<string, any>[]>([])

const filters = reactive({
  roleName: '',
  roleCode: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns: TableColumnType[] = [
  {
    title: '角色名称',
    dataIndex: 'roleName',
    key: 'roleName',
    width: 150
  },
  {
    title: '角色编码',
    dataIndex: 'roleCode',
    key: 'roleCode',
    width: 180,
    ellipsis: true
  },
  {
    title: '角色类型',
    key: 'roleType',
    width: 100
  },
  {
    title: '数据权限',
    key: 'dataScope',
    width: 140
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
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 170
  },
  {
    title: '操作',
    key: 'action',
    width: 260,
    fixed: 'right'
  }
]

const formRef = ref()
const formData = reactive<Record<string, any>>({
  roleName: '',
  roleCode: '',
  roleType: 'CUSTOM',
  dataScope: 'CUSTOM',
  sortOrder: 0,
  status: 1,
  remark: ''
})

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

function getDataScopeText(scope: string): string {
  const map: Record<string, string> = {
    'CUSTOM': '自定义',
    'DEPT': '本部门',
    'ALL': '全部'
  }
  return map[scope] || scope || '-'
}

function resetFilters() {
  filters.roleName = ''
  filters.roleCode = ''
  filters.status = undefined
  pagination.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRolePage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      roleName: filters.roleName || undefined,
      roleCode: filters.roleCode || undefined,
      status: filters.status
    })
    dataSource.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadMenuTree() {
  try {
    const res = await getMenuTree()
    menuTreeData.value = buildTree(res.data || [])
  } catch {
    // ignore
  }
}

function buildTree(menus: any[]): Record<string, any>[] {
  const map = new Map<number, Record<string, any>>()
  const roots: Record<string, any>[] = []

  menus.forEach(menu => {
    map.set(menu.id, {
      ...menu,
      key: menu.id,
      title: menu.menuName,
      children: []
    })
  })

  menus.forEach(menu => {
    const node = map.get(menu.id)!
    if (menu.parentId && menu.parentId !== 0 && map.has(menu.parentId)) {
      map.get(menu.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

function handleTableChange(pag: Record<string, any>) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadData()
}

function openCreateDrawer() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    roleName: '',
    roleCode: '',
    roleType: 'CUSTOM',
    dataScope: 'CUSTOM',
    sortOrder: 0,
    status: 1,
    remark: ''
  })
  drawerVisible.value = true
}

function openEditDrawer(record: SysRole) {
  isEdit.value = true
  Object.assign(formData, {
    id: record.id,
    roleName: record.roleName,
    roleCode: record.roleCode,
    roleType: record.roleType || 'CUSTOM',
    dataScope: record.dataScope || 'CUSTOM',
    sortOrder: record.sortOrder || 0,
    status: record.status || 1,
    remark: record.remark || ''
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
      roleName: formData.roleName,
      roleCode: formData.roleCode,
      roleType: formData.roleType,
      dataScope: formData.dataScope,
      sortOrder: formData.sortOrder,
      status: formData.status,
      remark: formData.remark
    }
    if (isEdit.value) {
      await updateRole(formData.id, payload)
      message.success('修改成功')
    } else {
      await createRole(payload)
      message.success('创建成功')
    }
    closeDrawer()
    loadData()
  } catch {
    // error handled
  } finally {
    submitLoading.value = false
  }
}

async function handleToggleStatus(record: SysRole) {
  const newStatus = record.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await updateRoleStatus(record.id!, newStatus)
    message.success(`${action}成功`)
    loadData()
  } catch {
    // error handled
  }
}

async function handleDelete(record: SysRole) {
  try {
    await deleteRole(record.id!)
    message.success('删除成功')
    loadData()
  } catch {
    // error handled
  }
}

async function openMenuDrawer(record: SysRole) {
  currentRoleId.value = record.id!
  menuSubmitLoading.value = false
  // 加载菜单树
  await loadMenuTree()
  // 加载该角色已有的菜单
  try {
    const res = await getRoleMenus(record.id!)
    checkedMenuKeys.value = res.data || []
  } catch {
    checkedMenuKeys.value = []
  }
  menuDrawerVisible.value = true
}

function closeMenuDrawer() {
  menuDrawerVisible.value = false
  currentRoleId.value = null
  checkedMenuKeys.value = []
}

async function handleMenuSubmit() {
  if (!currentRoleId.value) return
  menuSubmitLoading.value = true
  try {
    await assignRoleMenus(currentRoleId.value, checkedMenuKeys.value)
    message.success('权限分配成功')
    closeMenuDrawer()
  } catch {
    // error handled
  } finally {
    menuSubmitLoading.value = false
  }
}

onMounted(() => {
  loadData()
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
}
.filter-bar {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 0;
}
.menu-assign-tip {
  color: #8c8c8c;
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
