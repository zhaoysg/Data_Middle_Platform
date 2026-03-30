<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#userGrad)"/>
            <circle cx="12" cy="9" r="3" stroke="white" stroke-width="1.5"/>
            <path d="M5 19c0-3.866 3.134-7 7-7s7 3.134 7 7" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <defs>
              <linearGradient id="userGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#1677FF"/>
                <stop offset="100%" stop-color="#00B4DB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">用户管理</h1>
          <p class="page-subtitle">管理系统用户，支持增删改查、状态管理、密码重置</p>
        </div>
      </div>
      <div class="header-right">
        <a-button type="primary" @click="openCreateDrawer">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <a-space wrap>
        <a-input
          v-model:value="filters.username"
          placeholder="搜索用户名"
          style="width: 180px"
          allowClear
          @pressEnter="loadData"
          @change="loadData"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-input
          v-model:value="filters.nickname"
          placeholder="搜索昵称"
          style="width: 160px"
          allowClear
          @pressEnter="loadData"
          @change="loadData"
        />
        <a-select
          v-model:value="filters.status"
          placeholder="用户状态"
          style="width: 120px"
          allowClear
          @change="loadData"
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
        <template v-else-if="column.key === 'avatar'">
          <a-avatar v-if="record.avatar" :src="record.avatar" :size="32" />
          <a-avatar v-else :size="32" style="background: #1677ff">
            {{ (record.nickname || record.username || 'U').charAt(0).toUpperCase() }}
          </a-avatar>
        </template>
        <template v-else-if="column.key === 'lastLoginTime'">
          <span v-if="record.lastLoginTime">{{ record.lastLoginTime }}</span>
          <span v-else style="color: #999">从未登录</span>
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
            <a @click="handleResetPwd(record)" style="color: #722ed1">重置密码</a>
            <a-divider type="vertical" />
            <a-popconfirm
              :title="`确定要删除用户「${record.nickname || record.username}」吗？`"
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
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="formData.username"
            placeholder="请输入用户名"
            :disabled="isEdit"
            maxlength="50"
          />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="密码" name="password">
          <a-input-password
            v-model:value="formData.password"
            placeholder="请输入密码，默认 admin123"
            maxlength="100"
          />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="formData.nickname" placeholder="请输入昵称" maxlength="50" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱" maxlength="100" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入手机号" maxlength="20" />
        </a-form-item>
        <a-form-item label="部门" name="deptId">
          <a-select
            v-model:value="formData.deptId"
            placeholder="请选择部门"
            allowClear
            style="width: 100%"
          >
            <a-select-option v-for="d in deptOptions" :key="d.value" :value="d.value">
              {{ d.label }}
            </a-select-option>
          </a-select>
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
import { message, Modal } from 'ant-design-vue'
import type { TableColumnType, TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined, PlusOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  updateUserStatus,
  resetUserPassword,
  getUserOptions,
  getDeptOptions,
  type SysUser
} from '@/api/system'

const loading = ref(false)
const dataSource = ref<SysUser[]>([])
const submitLoading = ref(false)
const drawerVisible = ref(false)
const isEdit = ref(false)
const drawerTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')

const filters = reactive({
  username: '',
  nickname: '',
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
    title: '头像',
    key: 'avatar',
    width: 70,
    fixed: 'left'
  },
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
    width: 120
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    key: 'nickname',
    width: 120
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    key: 'email',
    width: 180,
    ellipsis: true
  },
  {
    title: '手机号',
    dataIndex: 'phone',
    key: 'phone',
    width: 130
  },
  {
    title: '状态',
    key: 'status',
    width: 80
  },
  {
    title: '最后登录',
    key: 'lastLoginTime',
    width: 160
  },
  {
    title: '操作',
    key: 'action',
    width: 280,
    fixed: 'right'
  }
]

const formRef = ref()
const formData = reactive<Record<string, any>>({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  deptId: undefined,
  status: 1
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const deptOptions = ref<{ value: number; label: string }[]>([])

function resetFilters() {
  filters.username = ''
  filters.nickname = ''
  filters.status = undefined
  pagination.current = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      username: filters.username || undefined,
      nickname: filters.nickname || undefined,
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

async function loadDeptOptions() {
  try {
    const res = await getDeptOptions()
    deptOptions.value = (res.data || []).map((item: any) => ({
      value: item.value,
      label: item.label
    }))
  } catch {
    // ignore
  }
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
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    deptId: undefined,
    status: 1
  })
  drawerVisible.value = true
}

function openEditDrawer(record: SysUser) {
  isEdit.value = true
  Object.assign(formData, {
    id: record.id,
    username: record.username,
    password: '',
    nickname: record.nickname || '',
    email: record.email || '',
    phone: record.phone || '',
    deptId: record.deptId,
    status: record.status || 1
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
      username: formData.username,
      nickname: formData.nickname,
      email: formData.email,
      phone: formData.phone,
      deptId: formData.deptId,
      status: formData.status
    }
    if (!isEdit.value && formData.password) {
      payload.password = formData.password
    }
    if (isEdit.value) {
      await updateUser(formData.id, payload)
      message.success('修改成功')
    } else {
      await createUser(payload)
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

async function handleToggleStatus(record: SysUser) {
  const newStatus = record.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await updateUserStatus(record.id!, newStatus)
    message.success(`${action}成功`)
    loadData()
  } catch {
    // error handled
  }
}

function handleResetPwd(record: SysUser) {
  Modal.confirm({
    title: '重置密码',
    content: `确定要重置用户「${record.nickname || record.username}」的密码吗？默认密码为 admin123`,
    okText: '确认重置',
    okType: 'danger',
    async onOk() {
      try {
        await resetUserPassword(record.id!)
        message.success('密码重置成功，默认密码为 admin123')
      } catch {
        // error handled
      }
    }
  })
}

async function handleDelete(record: SysUser) {
  try {
    await deleteUser(record.id!)
    message.success('删除成功')
    loadData()
  } catch {
    // error handled
  }
}

onMounted(() => {
  loadData()
  loadDeptOptions()
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
</style>
