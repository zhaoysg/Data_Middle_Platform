<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#deptGrad)"/>
            <path d="M12 7v10M7 12h10" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="12" r="3" stroke="white" stroke-width="1.5"/>
            <defs>
              <linearGradient id="deptGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#1890FF"/>
                <stop offset="100%" stop-color="#096DD9"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">部门管理</h1>
          <p class="page-subtitle">管理系统部门组织架构，支持部门树展示、新增、编辑、删除</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="handleExpand">
          <template #icon><ExpandAltOutlined /></template>
          {{ expanded ? '收起全部' : '展开全部' }}
        </a-button>
        <a-button type="primary" @click="openCreateDrawer(0)">
          <template #icon><PlusOutlined /></template>
          新增部门
        </a-button>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="filters.deptName"
          placeholder="搜索部门名称"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-input
          v-model:value="filters.deptCode"
          placeholder="搜索部门编码"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filters.status"
          placeholder="部门状态"
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
    <div class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="(record: any) => record.id"
        :default-expand-all-rows="expanded"
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'deptName'">
            <span style="display: flex; align-items: center; gap: 6px">
              <ApartmentOutlined style="color: #1890ff; font-size: 16px" />
              <span>{{ record.deptName }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : 'error'"
              :text="record.status === 1 ? '启用' : '禁用'"
            />
          </template>
          <template v-else-if="column.key === 'deptType'">
            <a-tag :color="record.deptType === 'HEADQUARTERS' ? 'blue' : 'green'">
              {{ getDeptTypeText(record.deptType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span v-if="record.createTime">{{ record.createTime }}</span>
            <span v-else style="color: #999">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openCreateDrawer(record.id)" style="color: #1677ff">新增子部门</a>
              <a-divider type="vertical" />
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
              <a-popconfirm
                :title="`确定要删除部门「${record.deptName}」吗？`"
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
        <a-form-item label="上级部门" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            placeholder="请选择上级部门（不选则为顶级）"
            :tree-data="deptSelectTree"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            allowClear
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="部门名称" name="deptName">
          <a-input
            v-model:value="formData.deptName"
            placeholder="请输入部门名称"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item label="部门编码" name="deptCode">
          <a-input
            v-model:value="formData.deptCode"
            placeholder="请输入部门编码"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item label="部门类型" name="deptType">
          <a-select v-model:value="formData.deptType" placeholder="请选择部门类型">
            <a-select-option value="HEADQUARTERS">总部</a-select-option>
            <a-select-option value="BRANCH">分部</a-select-option>
            <a-select-option value="DEPARTMENT">部门</a-select-option>
            <a-select-option value="TEAM">小组</a-select-option>
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
        <a-form-item label="联系电话" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入联系电话" maxlength="20" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱" maxlength="100" />
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
import type { TableColumnType, TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined, PlusOutlined, ReloadOutlined, ExpandAltOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'
import {
  getDeptPage,
  getDeptTree,
  getDeptDetail,
  createDept,
  updateDept,
  deleteDept,
  updateDeptStatus,
  type SysDept
} from '@/api/system'

const loading = ref(false)
const dataSource = ref<SysDept[]>([])
const submitLoading = ref(false)
const drawerVisible = ref(false)
const isEdit = ref(false)
const drawerTitle = computed(() => isEdit.value ? '编辑部门' : '新增部门')
const expanded = ref(true)
const deptSelectTree = ref<any[]>([])

const filters = reactive({
  deptName: '',
  deptCode: '',
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
    title: '部门名称',
    dataIndex: 'deptName',
    key: 'deptName',
    width: 200
  },
  {
    title: '部门编码',
    dataIndex: 'deptCode',
    key: 'deptCode',
    width: 150,
    ellipsis: true
  },
  {
    title: '部门类型',
    key: 'deptType',
    width: 100
  },
  {
    title: '联系电话',
    dataIndex: 'phone',
    key: 'phone',
    width: 130
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    key: 'email',
    width: 180,
    ellipsis: true
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
    width: 240,
    fixed: 'right'
  }
]

const formRef = ref()
const formData = reactive<Record<string, any>>({
  parentId: undefined as number | undefined,
  deptName: '',
  deptCode: '',
  deptType: 'DEPARTMENT',
  sortOrder: 0,
  phone: '',
  email: '',
  status: 1
})

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

function getDeptTypeText(type: string | undefined): string {
  const map: Record<string, string> = {
    'HEADQUARTERS': '总部',
    'BRANCH': '分部',
    'DEPARTMENT': '部门',
    'TEAM': '小组'
  }
  return map[type || ''] || type || '-'
}

function resetFilters() {
  filters.deptName = ''
  filters.deptCode = ''
  filters.status = undefined
  pagination.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDeptPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      deptName: filters.deptName || undefined,
      deptCode: filters.deptCode || undefined,
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

async function loadDeptTree() {
  try {
    const res = await getDeptTree()
    deptSelectTree.value = buildSelectTree(res.data || [])
  } catch {
    // ignore
  }
}

function buildSelectTree(depts: any[], parentId: number = 0): any[] {
  return depts
    .filter(d => d.parentId === parentId)
    .map(d => ({
      id: d.id,
      deptName: d.deptName,
      children: buildSelectTree(depts, d.id)
    }))
}

function handleExpand() {
  expanded.value = !expanded.value
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadData()
}

function openCreateDrawer(parentId: number | undefined) {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    parentId: parentId || undefined,
    deptName: '',
    deptCode: '',
    deptType: 'DEPARTMENT',
    sortOrder: 0,
    phone: '',
    email: '',
    status: 1
  })
  drawerVisible.value = true
}

async function openEditDrawer(record: SysDept) {
  isEdit.value = true
  try {
    const res = await getDeptDetail(record.id!)
    Object.assign(formData, {
      id: res.data.id,
      parentId: res.data.parentId && res.data.parentId !== 0 ? res.data.parentId : undefined,
      deptName: res.data.deptName || '',
      deptCode: res.data.deptCode || '',
      deptType: res.data.deptType || 'DEPARTMENT',
      sortOrder: res.data.sortOrder || 0,
      phone: res.data.phone || '',
      email: res.data.email || '',
      status: res.data.status || 1
    })
  } catch {
    // use record data as fallback
    Object.assign(formData, {
      id: record.id,
      parentId: record.parentId && record.parentId !== 0 ? record.parentId : undefined,
      deptName: record.deptName || '',
      deptCode: record.deptCode || '',
      deptType: record.deptType || 'DEPARTMENT',
      sortOrder: record.sortOrder || 0,
      phone: record.phone || '',
      email: record.email || '',
      status: record.status || 1
    })
  }
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
      deptName: formData.deptName,
      deptCode: formData.deptCode,
      deptType: formData.deptType,
      sortOrder: formData.sortOrder,
      phone: formData.phone,
      email: formData.email,
      status: formData.status
    }
    if (isEdit.value) {
      await updateDept(formData.id, payload)
      message.success('修改成功')
    } else {
      await createDept(payload)
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

async function handleToggleStatus(record: SysDept) {
  const newStatus = record.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await updateDeptStatus(record.id!, newStatus)
    message.success(`${action}成功`)
    loadData()
  } catch {
    // error handled
  }
}

async function handleDelete(record: SysDept) {
  try {
    await deleteDept(record.id!)
    message.success('删除成功')
    loadData()
  } catch {
    // error handled
  }
}

onMounted(() => {
  loadData()
  loadDeptTree()
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
.filter-bar {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 0;
}
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
</style>
