<script setup lang="ts">
import type { MetadataColumn, MetadataTable, SecColumnSensitivity } from '#/api/metadata/model';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { EditOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue';
import {
  Button,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  metadataColumnAlias,
  metadataColumnUpdate,
  metadataTableColumns,
} from '#/api/metadata/table';
import {
  secColumnSensitivityConfirm,
  secColumnSensitivityList,
  secColumnSensitivityUpdate,
} from '#/api/metadata/security/sensitivity';
import { secLevelList } from '#/api/metadata/security/level';

const columnsData = ref<MetadataColumn[]>([]);
const currentTable = ref<MetadataTable>();
const editingColumnId = ref<number | null>(null);
const editingAlias = ref('');

// 敏感字段快捷编辑
const sensitivityModalVisible = ref(false);
const sensitivityModalLoading = ref(false);
const sensitivityRow = ref<MetadataColumn & { sensitivityRecord?: SecColumnSensitivity }>();
const sensitivityOptions = ref<{ label: string; value: string }[]>([]);

function unwrapList(raw: unknown): any[] {
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'rows' in raw && Array.isArray((raw as { rows: unknown }).rows)) {
    return (raw as { rows: any[] }).rows;
  }
  return [];
}

async function loadSensitivityLevelOptions() {
  try {
    const page = await secLevelList({ pageNum: 1, pageSize: 500 });
    const rows = page?.rows ? page.rows : unwrapList(page);
    sensitivityOptions.value =
      rows.length > 0
        ? rows.map((lv: any) => ({
            label: lv.levelCode ? `${lv.levelCode} — ${lv.levelName ?? ''}` : (lv.levelName ?? ''),
            value: lv.levelCode,
          }))
        : [
            { label: 'NORMAL — 普通', value: 'NORMAL' },
            { label: 'INNER — 内部', value: 'INNER' },
            { label: 'SENSITIVE — 敏感', value: 'SENSITIVE' },
            { label: 'HIGHLY_SENSITIVE — 高度敏感', value: 'HIGHLY_SENSITIVE' },
          ];
  } catch {
    sensitivityOptions.value = [
      { label: 'NORMAL — 普通', value: 'NORMAL' },
      { label: 'INNER — 内部', value: 'INNER' },
      { label: 'SENSITIVE — 敏感', value: 'SENSITIVE' },
      { label: 'HIGHLY_SENSITIVE — 高度敏感', value: 'HIGHLY_SENSITIVE' },
    ];
  }
}
const confirmForm = ref({
  levelCode: '',
  confirmed: false,
});

// 字段完整编辑弹窗
const columnEditModalVisible = ref(false);
const columnEditModalLoading = ref(false);
const columnEditForm = ref({
  id: null as number | null,
  columnAlias: '',
  columnComment: '',
  sensitivityLevel: '',
});
// 敏感记录映射: key = `${dsId}-${tableName}-${columnName}`
const sensitivityMap = ref<Record<string, SecColumnSensitivity>>({});

const title = computed(() => {
  const name = currentTable.value?.tableName;
  const domain = currentTable.value?.dataDomain;
  const layer = currentTable.value?.dataLayer;
  const parts = [name];
  if (layer) parts.push(`[${layer}]`);
  if (domain) parts.push(`(${domain})`);
  return parts.join(' ');
});

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  footer: false,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }

    const data = drawerApi.getData() as {
      columns?: MetadataColumn[];
      table?: MetadataTable;
    };
    columnsData.value = [...(data.columns || [])];
    currentTable.value = data.table;
    editingColumnId.value = null;
    editingAlias.value = '';
    sensitivityMap.value = {};

    await loadSensitivityLevelOptions();

    // 加载敏感字段记录
    if (data.table?.dsId && data.table?.tableName) {
      await loadSensitivityRecords(data.table.dsId, data.table.tableName);
    }
  },
  onClosed: handleClosed,
});

async function loadSensitivityRecords(dsId: number, tableName: string) {
  try {
    const result = await secColumnSensitivityList({
      dsId,
      tableName,
      pageNum: 1,
      pageSize: 999,
    });
    const rows = result?.rows || result || [];
    sensitivityMap.value = {};
    for (const r of rows) {
      const key = `${r.dsId}-${r.tableName}-${r.columnName}`;
      sensitivityMap.value[key] = r;
    }
  } catch {
    // ignore
  }
}

function getSensitivityInfo(
  record: MetadataColumn,
): { color: string; label: string; level: string; confirmed: boolean; maskType: string } {
  const key = `${record.dsId}-${record.tableName}-${record.columnName}`;
  const info = sensitivityMap.value[key];
  if (!info) {
    return { color: 'default', label: '未识别', level: '', confirmed: false, maskType: '' };
  }
  const colorMap: Record<string, string> = {
    RED: 'red',
    ORANGE: 'orange',
    YELLOW: 'gold',
    GREEN: 'green',
    NORMAL: 'green',
    INNER: 'blue',
    SENSITIVE: 'orange',
    HIGHLY_SENSITIVE: 'red',
  };
  const labelMap: Record<string, string> = {
    NORMAL: '普通',
    INNER: '内部',
    SENSITIVE: '敏感',
    HIGHLY_SENSITIVE: '高度敏感',
  };
  const level = info.levelCode || info.levelName || '';
  return {
    color: colorMap[info.levelCode || ''] || colorMap[info.levelName || ''] || 'default',
    label: labelMap[level] || level || '未识别',
    level,
    confirmed: info.confirmed === '1',
    maskType: info.maskType || '',
  };
}

function getMaskSuggestion(record: MetadataColumn): string {
  const key = `${record.dsId}-${record.tableName}-${record.columnName}`;
  const info = sensitivityMap.value[key];
  if (!info?.maskType) return '-';
  const maskLabelMap: Record<string, string> = {
    MASK: '遮蔽',
    HASH: '哈希',
    ENCRYPT: '加密',
    DELETE: '删除',
    NONE: '无',
    HIDE: '隐藏',
    RANGE: '范围',
    WATERMARK: '水印',
    SHUFFLE: '打乱',
    CUSTOM: '自定义',
  };
  return maskLabelMap[info.maskType] || info.maskType;
}

function getConfidence(record: MetadataColumn): string {
  const key = `${record.dsId}-${record.tableName}-${record.columnName}`;
  const info = sensitivityMap.value[key];
  if (!info?.confidence) return '-';
  return `${info.confidence}%`;
}

function openSensitivityModal(record: MetadataColumn) {
  const key = `${record.dsId}-${record.tableName}-${record.columnName}`;
  const info = sensitivityMap.value[key];
  sensitivityRow.value = { ...record, sensitivityRecord: info };
  confirmForm.value = {
    levelCode: info?.levelCode || '',
    confirmed: info?.confirmed === '1',
  };
  sensitivityModalVisible.value = true;
}

async function handleConfirmSensitivity() {
  if (!sensitivityRow.value) return;
  const record = sensitivityRow.value;
  const key = `${record.dsId}-${record.tableName}-${record.columnName}`;
  const existing = sensitivityMap.value[key];

  sensitivityModalLoading.value = true;
  try {
    if (existing?.id) {
      // 更新已有记录
      await secColumnSensitivityUpdate({
        id: existing.id,
        levelCode: confirmForm.value.levelCode,
        confirmed: confirmForm.value.confirmed ? '1' : '0',
      });
    } else {
      // 新增记录（需要调用 add，但当前只暴露了 update）
      // 对于未识别的字段，需要通过后端新增接口
      // 这里先提示用户去敏感字段管理页面添加
      message.warning('该字段尚未被识别为敏感字段，请先在"敏感字段扫描"中扫描识别');
      sensitivityModalLoading.value = false;
      return;
    }

    message.success('保存成功');
    // 刷新敏感记录映射
    if (currentTable.value?.dsId && currentTable.value?.tableName) {
      await loadSensitivityRecords(currentTable.value.dsId, currentTable.value.tableName);
    }
    sensitivityModalVisible.value = false;
  } catch {
    message.error('保存失败');
  } finally {
    sensitivityModalLoading.value = false;
  }
}

async function handleQuickConfirm(record: MetadataColumn) {
  const key = `${record.dsId}-${record.tableName}-${record.columnName}`;
  const existing = sensitivityMap.value[key];
  if (!existing?.id) {
    message.warning('该字段尚未被识别为敏感字段');
    return;
  }
  try {
    await secColumnSensitivityConfirm(existing.id, true);
    message.success('已确认为敏感字段');
    if (currentTable.value?.dsId && currentTable.value?.tableName) {
      await loadSensitivityRecords(currentTable.value.dsId, currentTable.value.tableName);
    }
  } catch {
    message.error('确认失败');
  }
}

function openColumnEditModal(column: MetadataColumn) {
  columnEditForm.value = {
    id: column.id ?? null,
    columnAlias: column.columnAlias || '',
    columnComment: column.columnComment || '',
    sensitivityLevel: column.sensitivityLevel || '',
  };
  columnEditModalVisible.value = true;
}

async function handleSaveColumn() {
  if (!columnEditForm.value.id) return;
  columnEditModalLoading.value = true;
  try {
    await metadataColumnUpdate(columnEditForm.value.id, {
      columnAlias: columnEditForm.value.columnAlias,
      columnComment: columnEditForm.value.columnComment,
      sensitivityLevel: columnEditForm.value.sensitivityLevel,
    });
    message.success('保存成功');

    const index = columnsData.value.findIndex((item) => item.id === columnEditForm.value.id);
    if (index >= 0) {
      columnsData.value[index] = {
        ...columnsData.value[index],
        columnAlias: columnEditForm.value.columnAlias,
        columnComment: columnEditForm.value.columnComment,
        sensitivityLevel: columnEditForm.value.sensitivityLevel,
      };
    }
    columnEditModalVisible.value = false;
  } catch {
    message.error('保存失败');
  } finally {
    columnEditModalLoading.value = false;
  }
}

function startEditAlias(column: MetadataColumn) {
  editingColumnId.value = column.id ?? null;
  editingAlias.value = column.columnAlias || '';
}

async function saveAlias(column: MetadataColumn) {
  try {
    await metadataColumnAlias(column.id!, editingAlias.value);
    message.success('别名保存成功');

    const index = columnsData.value.findIndex((item) => item.id === column.id);
    if (index >= 0) {
      columnsData.value[index] = {
        ...columnsData.value[index],
        columnAlias: editingAlias.value,
      };
    }

    editingColumnId.value = null;
    editingAlias.value = '';
  } catch {
    message.error('保存失败');
  }
}

function cancelEdit() {
  editingColumnId.value = null;
  editingAlias.value = '';
}

function handleClosed() {
  columnsData.value = [];
  currentTable.value = undefined;
  editingColumnId.value = null;
  editingAlias.value = '';
  sensitivityMap.value = {};
}

const tableColumns = [
  { title: '序号', key: 'sortOrder', dataIndex: 'sortOrder', width: 60, align: 'center' as const },
  { title: '字段名', key: 'columnName', dataIndex: 'columnName', width: 150, ellipsis: true },
  { title: '别名', key: 'columnAlias', dataIndex: 'columnAlias', width: 168 },
  { title: '注释', key: 'columnComment', dataIndex: 'columnComment', width: 160, ellipsis: true },
  { title: '数据类型', key: 'dataType', dataIndex: 'dataType', width: 110 },
  { title: '可空', key: 'isNullable', dataIndex: 'isNullable', width: 56, align: 'center' as const },
  { title: '键', key: 'columnKey', dataIndex: 'columnKey', width: 52, align: 'center' as const },
  { title: '主键', key: 'isPrimaryKey', dataIndex: 'isPrimaryKey', width: 56, align: 'center' as const },
  { title: '敏感', key: '__sensitivity__', dataIndex: '__sensitivity__', width: 72, align: 'center' as const },
  { title: '敏感等级', key: '__level__', dataIndex: '__level__', width: 108 },
  { title: '脱敏建议', key: '__mask__', dataIndex: '__mask__', width: 92, align: 'center' as const },
  { title: '确认', key: '__confirmed__', dataIndex: '__confirmed__', width: 64, align: 'center' as const },
  { title: '操作', key: '__action__', dataIndex: '__action__', width: 108, fixed: 'right' as const },
];
</script>

<template>
  <BasicDrawer :title="title" class="w-[1280px]">
    <!-- 敏感字段统计条 -->
    <div
      v-if="Object.keys(sensitivityMap).length > 0"
      class="flex items-center gap-4 px-4 py-2 mb-2 rounded"
      style="background: #f0f5ff; border: 1px solid #adc6ff;"
    >
      <span style="font-size: 13px; color: #3b5998; font-weight: 500;">
        敏感字段统计：
      </span>
      <Tag color="red">
        高度敏感 {{ Object.values(sensitivityMap).filter((r) => r.levelCode === 'HIGHLY_SENSITIVE').length }}
      </Tag>
      <Tag color="orange">
        敏感 {{ Object.values(sensitivityMap).filter((r) => r.levelCode === 'SENSITIVE').length }}
      </Tag>
      <Tag color="blue">
        内部 {{ Object.values(sensitivityMap).filter((r) => r.levelCode === 'INNER').length }}
      </Tag>
      <Tag color="green">
        普通 {{ Object.values(sensitivityMap).filter((r) => r.levelCode === 'NORMAL').length }}
      </Tag>
      <Tag color="default">
        待确认 {{ Object.values(sensitivityMap).filter((r) => r.confirmed === '0').length }}
      </Tag>
      <span style="font-size: 12px; color: #8c8c8c; margin-left: auto;">
        提示：点击敏感等级列可直接编辑
      </span>
    </div>

    <Table
      :columns="tableColumns"
      :data-source="columnsData"
      :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
      :row-key="(record: MetadataColumn) => record.id ?? record.columnName ?? 0"
      :scroll="{ x: 1280 }"
      size="small"
      :row-class-name="(record: MetadataColumn) => {
        const info = getSensitivityInfo(record);
        return info.level && !info.confirmed ? 'sensitivity-unconfirmed' : '';
      }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'columnAlias'">
          <template v-if="editingColumnId === record.id">
            <Input
              v-model:value="editingAlias"
              size="small"
              style="width: 120px; margin-right: 4px;"
              @press-enter="saveAlias(record)"
              @keyup.esc="cancelEdit"
            />
            <Button type="link" size="small" @click="saveAlias(record)">保存</Button>
            <Button type="link" size="small" @click="cancelEdit">取消</Button>
          </template>
          <template v-else>
            <span style="margin-right: 4px;">{{ record.columnAlias || '—' }}</span>
            <EditOutlined
              style="color: #1890ff; cursor: pointer; font-size: 12px;"
              @click="openColumnEditModal(record)"
            />
          </template>
        </template>
        <template v-else-if="column.key === 'isPrimaryKey'">
          <Tag v-if="record.isPrimaryKey" color="blue" style="margin: 0;">PK</Tag>
          <span v-else>—</span>
        </template>
        <template v-else-if="column.key === '__sensitivity__'">
          <template v-if="getSensitivityInfo(record).level">
            <Tag
              :color="getSensitivityInfo(record).confirmed ? 'success' : 'warning'"
              style="margin: 0; font-size: 11px;"
            >
              {{ getSensitivityInfo(record).confirmed ? '已确' : '待确' }}
            </Tag>
          </template>
          <span v-else style="color: #d9d9d9;">—</span>
        </template>
        <template v-else-if="column.key === '__level__'">
          <div class="flex items-center gap-1">
            <Tag
              :color="getSensitivityInfo(record).color"
              style="margin: 0; cursor: pointer;"
              @click="openSensitivityModal(record)"
            >
              {{ getSensitivityInfo(record).label }}
            </Tag>
            <EditOutlined
              style="color: #1890ff; cursor: pointer; font-size: 11px;"
              @click="openSensitivityModal(record)"
            />
          </div>
        </template>
        <template v-else-if="column.key === '__mask__'">
          <span style="font-size: 12px;">{{ getMaskSuggestion(record) }}</span>
        </template>
        <template v-else-if="column.key === '__confirmed__'">
          <CheckCircleOutlined
            v-if="getSensitivityInfo(record).confirmed"
            style="color: #52c41a; font-size: 16px;"
          />
          <Tooltip v-else-if="getSensitivityInfo(record).level" title="点击快速确认">
            <CloseCircleOutlined
              style="color: #fa8c16; font-size: 16px; cursor: pointer;"
              @click="handleQuickConfirm(record)"
            />
          </Tooltip>
          <span v-else style="color: #d9d9d9;">—</span>
        </template>
        <template v-else-if="column.key === '__action__'">
          <Space size="small">
            <Button type="link" size="small" @click="startEditAlias(record)">行内别名</Button>
            <Button type="link" size="small" @click="openColumnEditModal(record)">编辑</Button>
            <Button type="link" size="small" @click="openSensitivityModal(record)">
              {{ getSensitivityInfo(record).level ? '敏感' : '标记' }}
            </Button>
          </Space>
        </template>
      </template>
    </Table>
  </BasicDrawer>

  <!-- 敏感字段快捷编辑弹窗 -->
  <Modal
    v-model:open="sensitivityModalVisible"
    title="敏感等级编辑"
    :confirm-loading="sensitivityModalLoading"
    ok-text="保存"
    cancel-text="取消"
    @ok="handleConfirmSensitivity"
    @cancel="sensitivityModalVisible = false"
  >
    <div v-if="sensitivityRow" class="sensitivity-edit-form">
      <div class="field-info-row">
        <span style="color: #8c8c8c; font-size: 13px;">字段信息：</span>
        <Tag color="blue">{{ sensitivityRow.columnName }}</Tag>
        <Tag>{{ sensitivityRow.dataType }}</Tag>
        <span style="color: #8c8c8c; font-size: 13px;">{{ sensitivityRow.columnComment || '无注释' }}</span>
      </div>

      <div style="margin-top: 16px;">
        <div style="margin-bottom: 8px; font-weight: 500;">敏感等级</div>
        <Select
          v-model:value="confirmForm.levelCode"
          :options="sensitivityOptions"
          placeholder="请选择敏感等级"
          style="width: 100%;"
        />
      </div>

      <div style="margin-top: 12px;">
        <div style="margin-bottom: 8px; font-weight: 500;">确认状态</div>
        <Space>
          <Tag :color="confirmForm.confirmed ? 'success' : 'warning'">
            {{ confirmForm.confirmed ? '已确认（无需再次确认）' : '待确认' }}
          </Tag>
          <Button
            v-if="!confirmForm.confirmed"
            type="link"
            size="small"
            @click="confirmForm.confirmed = true"
          >
            标记为已确认
          </Button>
        </Space>
      </div>

      <!-- 如果有敏感记录，显示更多信息 -->
      <div v-if="sensitivityRow.sensitivityRecord" style="margin-top: 16px; padding: 12px; background: #fafafa; border-radius: 6px;">
        <div style="font-size: 12px; color: #8c8c8c; margin-bottom: 8px;">识别详情</div>
        <Space wrap>
          <Tag v-if="sensitivityRow.sensitivityRecord.identifiedBy === 'AUTO'" color="purple">自动识别</Tag>
          <Tag v-else color="cyan">手动标记</Tag>
          <span style="font-size: 13px;">置信度：<strong>{{ sensitivityRow.sensitivityRecord.confidence || '-' }}%</strong></span>
          <span v-if="sensitivityRow.sensitivityRecord.matchRuleName" style="font-size: 13px;">
            匹配规则：<Tag size="small">{{ sensitivityRow.sensitivityRecord.matchRuleName }}</Tag>
          </span>
          <span v-if="sensitivityRow.sensitivityRecord.maskType" style="font-size: 13px;">
            脱敏方式：{{ getMaskSuggestion(sensitivityRow) }}
          </span>
        </Space>
      </div>
    </div>
  </Modal>

  <!-- 字段完整编辑弹窗 -->
  <Modal
    v-model:open="columnEditModalVisible"
    title="编辑字段"
    :confirm-loading="columnEditModalLoading"
    ok-text="保存"
    cancel-text="取消"
    @ok="handleSaveColumn"
    @cancel="columnEditModalVisible = false"
  >
    <Form layout="vertical" :model="columnEditForm">
      <Form.Item label="字段别名" name="columnAlias">
        <Input
          v-model:value="columnEditForm.columnAlias"
          placeholder="请输入字段别名"
          :maxlength="200"
        />
      </Form.Item>
      <Form.Item label="字段注释" name="columnComment">
        <Input.TextArea
          v-model:value="columnEditForm.columnComment"
          placeholder="请输入字段注释"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="敏感等级" name="sensitivityLevel">
        <Select
          v-model:value="columnEditForm.sensitivityLevel"
          :options="sensitivityOptions"
          placeholder="请选择敏感等级（可选）"
          allow-clear
        />
      </Form.Item>
    </Form>
  </Modal>
</template>

<style scoped>
.sensitivity-edit-form {
  padding: 4px 0;
}

.field-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 6px;
}
</style>

<style>
/* 待确认敏感字段行高亮 */
.sensitivity-unconfirmed td {
  background: #fffbe6 !important;
}
</style>
