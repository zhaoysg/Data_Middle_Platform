<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#domainGrad)"/>
            <rect x="6" y="6" width="5" height="5" rx="1" fill="white"/>
            <rect x="13" y="6" width="5" height="5" rx="1" fill="white" opacity="0.7"/>
            <rect x="6" y="13" width="5" height="5" rx="1" fill="white" opacity="0.7"/>
            <rect x="13" y="13" width="5" height="5" rx="1" fill="white" opacity="0.5"/>
            <defs>
              <linearGradient id="domainGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#13C2C2"/>
                <stop offset="100%" stop-color="#08979C"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">数据域管理</h1>
          <p class="page-subtitle">管理系统数据域，支持数据域的创建、编辑、删除和状态管理</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="openCreateDrawer">
          <template #icon><PlusOutlined /></template>
          新增数据域
        </a-button>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="filters.domainName"
          placeholder="搜索数据域名称"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-input
          v-model:value="filters.domainCode"
          placeholder="搜索数据域编码"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select
          v-model:value="filters.status"
          placeholder="数据域状态"
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
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'domainName'">
            <span style="display: flex; align-items: center; gap: 6px">
              <DatabaseOutlined style="color: #13c2c2; font-size: 16px" />
              <span>{{ record.domainName }}</span>
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : 'error'"
              :text="record.status === 1 ? '启用' : '禁用'"
            />
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
              <a-popconfirm
                :title="`确定要删除数据域「${record.domainName}」吗？`"
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
        <a-form-item label="数据域名称" name="domainName">
          <a-input
            v-model:value="formData.domainName"
            placeholder="请输入数据域名称"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item label="数据域编码" name="domainCode">
          <a-input
            v-model:value="formData.domainCode"
            placeholder="请输入数据域编码"
            maxlength="50"
            :disabled="isEdit"
          />
        </a-form-item>
        <a-form-item label="所属部门" name="deptId">
          <a-tree-select
            v-model:value="formData.deptId"
            placeholder="请选择所属部门"
            :tree-data="deptTreeData"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            allowClear
            tree-default-expand-all
            style="width: 100%"
          />
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
        <a-form-item label="数据域描述" name="domainDesc">
          <a-textarea
            v-model:value="formData.domainDesc"
            placeholder="请输入数据域描述"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType, TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined, PlusOutlined, ReloadOutlined,
  DatabaseOutlined
} from '@ant-design/icons-vue'
import {
  getDataDomainPage,
  getDataDomainDetail,
  createDataDomain,
  updateDataDomain,
  deleteDataDomain,
  updateDataDomainStatus,
  getDeptTree,
  type DqDataDomain,
  type SysDept
} from '@/api/system'

const loading = ref(false)
const dataSource = ref<DqDataDomain[]>([])
const submitLoading = ref(false)
const drawerVisible = ref(false)
const isEdit = ref(false)
const drawerTitle = computed(() => isEdit.value ? '编辑数据域' : '新增数据域')
const deptTreeData = ref<any[]>([])

const filters = reactive({
  domainName: '',
  domainCode: '',
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
    title: '数据域名称',
    dataIndex: 'domainName',
    key: 'domainName',
    width: 180
  },
  {
    title: '数据域编码',
    dataIndex: 'domainCode',
    key: 'domainCode',
    width: 150,
    ellipsis: true
  },
  {
    title: '所属部门',
    dataIndex: 'deptId',
    key: 'deptId',
    width: 120,
    customRender: ({ text }) => {
      return text ? `部门${text}` : '-'
    }
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
    width: 180,
    fixed: 'right'
  }
]

const formRef = ref()
const formData = reactive<Record<string, any>>({
  domainName: '',
  domainCode: '',
  deptId: undefined as number | undefined,
  sortOrder: 0,
  status: 1,
  domainDesc: ''
})

const formRules = {
  domainName: [{ required: true, message: '请输入数据域名称', trigger: 'blur' }],
  domainCode: [{ required: true, message: '请输入数据域编码', trigger: 'blur' }]
}

function resetFilters() {
  filters.domainName = ''
  filters.domainCode = ''
  filters.status = undefined
  pagination.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDataDomainPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      domainName: filters.domainName || undefined,
      domainCode: filters.domainCode || undefined,
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
    deptTreeData.value = buildDeptTree(res.data || [])
  } catch {
    // ignore
  }
}

function buildDeptTree(depts: SysDept[], parentId: number = 0): any[] {
  return depts
    .filter(d => d.parentId === parentId)
    .map(d => ({
      id: d.id,
      deptName: d.deptName,
      children: buildDeptTree(depts, d.id)
    }))
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadData()
}

function openCreateDrawer() {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    domainName: '',
    domainCode: '',
    deptId: undefined,
    sortOrder: 0,
    status: 1,
    domainDesc: ''
  })
  drawerVisible.value = true
}

async function openEditDrawer(record: DqDataDomain) {
  isEdit.value = true
  try {
    const res = await getDataDomainDetail(record.id!)
    Object.assign(formData, {
      id: res.data.id,
      domainName: res.data.domainName || '',
      domainCode: res.data.domainCode || '',
      deptId: res.data.deptId || undefined,
      sortOrder: res.data.sortOrder || 0,
      status: res.data.status || 1,
      domainDesc: res.data.domainDesc || ''
    })
  } catch {
    Object.assign(formData, {
      id: record.id,
      domainName: record.domainName || '',
      domainCode: record.domainCode || '',
      deptId: record.deptId || undefined,
      sortOrder: record.sortOrder || 0,
      status: record.status || 1,
      domainDesc: record.domainDesc || ''
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
      domainName: formData.domainName,
      domainCode: formData.domainCode,
      deptId: formData.deptId,
      sortOrder: formData.sortOrder,
      status: formData.status,
      domainDesc: formData.domainDesc
    }
    if (isEdit.value) {
      await updateDataDomain(formData.id, payload)
      message.success('修改成功')
    } else {
      await createDataDomain(payload)
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

async function handleToggleStatus(record: DqDataDomain) {
  const newStatus = record.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await updateDataDomainStatus(record.id!, newStatus)
    message.success(`${action}成功`)
    loadData()
  } catch {
    // error handled
  }
}

async function handleDelete(record: DqDataDomain) {
  try {
    await deleteDataDomain(record.id!)
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
