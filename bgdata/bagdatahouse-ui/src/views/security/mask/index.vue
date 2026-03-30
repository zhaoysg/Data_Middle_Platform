<template>
  <div class="security-mask">
    <a-card>
      <template #title>数据脱敏规则</template>
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建规则
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'maskType'">
            <a-tag :color="getMaskTypeColor(record.maskType)">{{ record.maskTypeName }}</a-tag>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-switch :checked="record.enabled" @change="(checked: boolean) => handleToggle(record, checked)" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleEdit(record)">编辑</a>
              <a @click="handleDelete(record)">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑脱敏规则' : '新建脱敏规则'"
      @ok="handleSubmit"
      @cancel="modalVisible = false"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="字段" name="column">
          <a-input v-model:value="formData.column" placeholder="请输入字段名" />
        </a-form-item>
        <a-form-item label="脱敏类型" name="maskType">
          <a-select v-model:value="formData.maskType" placeholder="选择脱敏类型">
            <a-select-option value="HEAD_KEEP">保留前N位</a-select-option>
            <a-select-option value="TAIL_KEEP">保留后N位</a-select-option>
            <a-select-option value="CENTER_MASK">中间脱敏</a-select-option>
            <a-select-option value="REPLACE">替换</a-select-option>
            <a-select-option value="HASH">哈希</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="脱敏示例" name="example">
          <a-input v-model:value="formData.example" placeholder="例如: 138****5678" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { confirmModal } from '@/utils/modal'

interface MaskRecord {
  id: number
  columnName: string
  tableName: string
  maskType: string
  maskTypeName: string
  maskPattern: string
  example: string
  enabled: boolean
  updateTime: string
}

const loading = ref(false)
const modalVisible = ref(false)
const isEdit = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '表名', dataIndex: 'tableName', key: 'tableName' },
  { title: '字段名', dataIndex: 'columnName', key: 'columnName' },
  { title: '脱敏类型', key: 'maskType' },
  { title: '脱敏示例', dataIndex: 'example', key: 'example' },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' },
  { title: '启用', key: 'enabled' },
  { title: '操作', key: 'action', width: 150 }
]

const dataSource = ref<MaskRecord[]>([])
const formData = reactive({
  id: null as number | null,
  column: '',
  maskType: '',
  example: ''
})

function getMaskTypeColor(type: string) {
  const map: Record<string, string> = {
    HEAD_KEEP: 'blue',
    TAIL_KEEP: 'green',
    CENTER_MASK: 'orange',
    REPLACE: 'purple',
    HASH: 'cyan'
  }
  return map[type] || 'default'
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(formData, { id: null, column: '', maskType: '', example: '' })
  modalVisible.value = true
}

function handleEdit(record: MaskRecord) {
  isEdit.value = true
  Object.assign(formData, { id: record.id, column: record.columnName, maskType: record.maskType, example: record.example })
  modalVisible.value = true
}

function handleSubmit() {
  if (!formData.column || !formData.maskType) {
    message.warning('请填写完整信息')
    return
  }
  message.success(isEdit.value ? '规则更新成功' : '规则创建成功')
  modalVisible.value = false
  loadData()
}

async function handleDelete(record: MaskRecord) {
  const confirmed = await confirmModal(`确定删除字段 ${record.columnName} 的脱敏规则吗?`)
  if (confirmed) {
    message.success('删除成功')
    loadData()
  }
}

function handleToggle(record: MaskRecord, checked: boolean) {
  record.enabled = checked
  message.success(`脱敏规则${checked ? '启用' : '禁用'}成功`)
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    dataSource.value = [
      { id: 1, tableName: 'dim_users', columnName: 'id_card', maskType: 'CENTER_MASK', maskTypeName: '中间脱敏', maskPattern: '{HEAD6}********{TAIL4}', example: '110101****011234', enabled: true, updateTime: '2024-01-15 10:00:00' },
      { id: 2, tableName: 'dim_users', columnName: 'user_phone', maskType: 'CENTER_MASK', maskTypeName: '中间脱敏', maskPattern: '{HEAD3}****{TAIL4}', example: '138****5678', enabled: true, updateTime: '2024-01-15 10:00:00' },
      { id: 3, tableName: 'dim_users', columnName: 'email', maskType: 'REPLACE', maskTypeName: '替换', maskPattern: '{HEAD1}***@***.{TAIL3}', example: 'z***@***.com', enabled: true, updateTime: '2024-01-15 10:00:00' }
    ]
    pagination.total = 3
    loading.value = false
  }, 500)
}

onMounted(() => {
  loadData()
})
</script>
