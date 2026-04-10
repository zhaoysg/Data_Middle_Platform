<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import {
  Card,
  Descriptions,
  DescriptionsItem,
  Divider,
  Form,
  Input,
  Select,
  Typography,
  message,
} from 'ant-design-vue';
import { DatabaseOutlined, FieldTimeOutlined, FileTextOutlined } from '@ant-design/icons-vue';

import { metadataDomainOptions } from '#/api/metadata/domain';
import { metadataLayerOptions } from '#/api/metadata/layer';
import {
  metadataTableInfo,
  metadataTableTagOptions,
  metadataTableUpdate,
} from '#/api/metadata/table';
import { secLevelList } from '#/api/metadata/security/level';
import { datasourceEnabled } from '#/api/system/datasource';

const { Title } = Typography;

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const isEdit = ref(false);

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const layerOptions = ref<{ label: string; value: string }[]>([]);
const domainOptions = ref<{ label: string; value: string }[]>([]);
const tagOptions = ref<{ label: string; value: string }[]>([]);
/** 表级敏感等级：优先使用安全模块配置的等级编码 */
const sensitivityOptions = ref<{ label: string; value: string }[]>([]);

const title = computed(() => {
  return isEdit.value ? '编辑元数据表' : '新增元数据表';
});

const dsDisplayLabel = computed(() => {
  const id = formValues.value.dsId;
  const hit = datasourceOptions.value.find((d) => d.value === id);
  return hit?.label ?? '—';
});

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  onClosed: handleClosed,
  onConfirm: handleSubmit,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }

    drawerApi.drawerLoading(true);
    try {
      await loadOptions();

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        isEdit.value = true;
        const info = await metadataTableInfo(id);
        formValues.value = normalizeFormValues(info);
        tagOptions.value = mergeTagOptions([
          ...tagOptions.value.map((item) => item.value),
          ...parseTags(info.tags),
        ]);
      } else {
        recordId.value = undefined;
        isEdit.value = false;
        formValues.value = { status: 'ACTIVE', tags: [] };
      }
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

function unwrapList(raw: unknown): any[] {
  if (Array.isArray(raw)) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'rows' in raw && Array.isArray((raw as { rows: unknown }).rows)) {
    return (raw as { rows: any[] }).rows;
  }
  return [];
}

async function loadOptions() {
  const [datasources, layers, domains, tags, levelsPage] = await Promise.all([
    datasourceEnabled(),
    metadataLayerOptions().catch(() => []),
    metadataDomainOptions().catch(() => []),
    metadataTableTagOptions().catch(() => []),
    secLevelList({ pageNum: 1, pageSize: 500 }).catch(() => null),
  ]);
  datasourceOptions.value = datasources.map((ds: any) => ({
    label: ds.dsName,
    value: ds.dsId,
  }));

  const layerList = unwrapList(layers);
  layerOptions.value = layerList
    .map((layer: any) => ({
      label: layer.layerCode ? `${layer.layerCode} — ${layer.layerName ?? ''}` : (layer.layerName ?? ''),
      value: layer.layerCode,
    }))
    .filter((o) => o.value);

  const domainList = unwrapList(domains);
  domainOptions.value = domainList
    .map((domain: any) => ({
      label: domain.domainCode ? `${domain.domainCode} — ${domain.domainName}` : domain.domainName,
      value: domain.domainName ?? domain.domainCode,
    }))
    .filter((o) => o.value);

  tagOptions.value = mergeTagOptions(tags || []);

  const levelRows = levelsPage?.rows ? levelsPage.rows : unwrapList(levelsPage);
  sensitivityOptions.value =
    levelRows.length > 0
      ? levelRows.map((lv: any) => ({
          label: lv.levelCode ? `${lv.levelCode} — ${lv.levelName ?? ''}` : (lv.levelName ?? ''),
          value: lv.levelCode,
        }))
      : [
          { label: 'NORMAL — 普通', value: 'NORMAL' },
          { label: 'INNER — 内部', value: 'INNER' },
          { label: 'SENSITIVE — 敏感', value: 'SENSITIVE' },
          { label: 'HIGHLY_SENSITIVE — 高度敏感', value: 'HIGHLY_SENSITIVE' },
        ];
}

async function handleSubmit() {
  if (!recordId.value) {
    message.warning('新增表请通过扫描功能自动入库');
    return;
  }

  drawerApi.lock(true);
  try {
    await metadataTableUpdate({
      id: recordId.value,
      ...formValues.value,
      dataLayer: formValues.value.dataLayer ?? '',
      dataDomain: formValues.value.dataDomain ?? '',
      sensitivityLevel: formValues.value.sensitivityLevel ?? '',
      tags: formatTags(formValues.value.tags),
    });
    message.success('保存成功');
    emit('reload');
    drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    drawerApi.lock(false);
  }
}

function handleClosed() {
  recordId.value = undefined;
  isEdit.value = false;
  formValues.value = {};
}

function normalizeFormValues(info: Record<string, any>) {
  return {
    ...info,
    tags: parseTags(info.tags),
  };
}

function parseTags(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((tag) => String(tag).trim()).filter(Boolean);
  }
  if (typeof value !== 'string') {
    return [];
  }
  return value
    .split(/[,\n，]/)
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function formatTags(value: unknown): string {
  const tags = parseTags(value);
  return tags.join(',');
}

function mergeTagOptions(tags: string[]) {
  return [...new Set(tags.map((tag) => tag.trim()).filter(Boolean))]
    .sort((left, right) => left.localeCompare(right, 'zh-CN'))
    .map((tag) => ({
      label: tag,
      value: tag,
    }));
}

const rowCountDisplay = computed(() => {
  const val = formValues.value.rowCount;
  if (val == null) {
    return '—';
  }
  return Number(val).toLocaleString();
});

const lastUpdateDisplay = computed(() => {
  const val = formValues.value.sourceUpdateTime;
  if (!val) {
    return '—';
  }
  return String(val).substring(0, 16).replace('T', ' ');
});

const storageDisplay = computed(() => {
  const val = formValues.value.storageBytes;
  if (!val) {
    return '—';
  }
  const bytes = Number(val);
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`;
});
</script>

<template>
  <BasicDrawer :title="title" class="metadata-table-drawer w-[600px]">
    <!-- 快速指标 -->
    <Card
      v-if="isEdit"
      :bordered="false"
      size="small"
      class="mb-4 shadow-sm"
      :body-style="{ padding: '14px 16px', background: 'linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%)' }"
    >
      <div class="flex flex-wrap gap-8">
        <div class="flex items-start gap-3">
          <DatabaseOutlined class="mt-0.5 text-lg text-[#2563eb]" />
          <div>
            <div class="text-xs font-medium uppercase tracking-wide text-slate-500">数据行数</div>
            <div class="text-lg font-semibold text-slate-800">{{ rowCountDisplay }}</div>
          </div>
        </div>
        <div class="flex items-start gap-3">
          <FileTextOutlined class="mt-0.5 text-lg text-[#16a34a]" />
          <div>
            <div class="text-xs font-medium uppercase tracking-wide text-slate-500">存储大小</div>
            <div class="text-lg font-semibold text-slate-800">{{ storageDisplay }}</div>
          </div>
        </div>
        <div class="flex items-start gap-3">
          <FieldTimeOutlined class="mt-0.5 text-lg text-[#ea580c]" />
          <div>
            <div class="text-xs font-medium uppercase tracking-wide text-slate-500">源表最后更新</div>
            <div class="text-base font-semibold text-slate-800">{{ lastUpdateDisplay }}</div>
          </div>
        </div>
      </div>
    </Card>

    <Title v-if="isEdit" :level="5" class="!mb-3 !mt-0 text-slate-700">
      技术元数据（同步自数据源，只读）
    </Title>
    <Descriptions
      v-if="isEdit"
      bordered
      size="small"
      :column="1"
      class="mb-5 metadata-tech-desc"
    >
      <DescriptionsItem label="数据源">
        {{ dsDisplayLabel }}
      </DescriptionsItem>
      <DescriptionsItem label="物理表名">
        <span class="font-mono text-slate-800">{{ formValues.tableName || '—' }}</span>
      </DescriptionsItem>
    </Descriptions>

    <Divider v-if="isEdit" orientation="left" class="!my-4">
      <span class="text-slate-600 font-medium">业务元数据（可编辑）</span>
    </Divider>

    <Form
      :model="formValues"
      layout="vertical"
      class="metadata-business-form"
      :label-col="{ style: { fontWeight: 600 } }"
    >
      <template v-if="!isEdit">
        <Form.Item label="数据源" name="dsId" :rules="[{ required: true, message: '请选择数据源' }]">
          <Select
            v-model:value="formValues.dsId"
            :options="datasourceOptions"
            placeholder="请选择数据源"
          />
        </Form.Item>
        <Form.Item label="表名" name="tableName" :rules="[{ required: true, message: '请输入表名' }]">
          <Input v-model:value="formValues.tableName" placeholder="物理表名" />
        </Form.Item>
      </template>

      <Form.Item label="表别名" name="tableAlias" extra="用于目录与检索展示的业务名称">
        <Input v-model:value="formValues.tableAlias" placeholder="请输入表别名" :maxlength="200" />
      </Form.Item>
      <Form.Item label="表注释 / 业务说明" name="tableComment" extra="可补充口径、负责人、使用场景等">
        <Input.TextArea
          v-model:value="formValues.tableComment"
          placeholder="请输入表注释或业务说明"
          :rows="3"
          show-count
          :maxlength="2000"
        />
      </Form.Item>
      <Form.Item label="数仓分层" name="dataLayer" extra="来自「数仓分层」主数据">
        <Select
          v-model:value="formValues.dataLayer"
          :options="layerOptions"
          placeholder="请选择分层"
          allow-clear
          show-search
          option-filter-prop="label"
        />
      </Form.Item>
      <Form.Item label="数据域" name="dataDomain" extra="来自「数据域」主数据">
        <Select
          v-model:value="formValues.dataDomain"
          :allow-clear="true"
          :options="domainOptions"
          option-filter-prop="label"
          placeholder="请选择数据域"
          show-search
        />
      </Form.Item>
      <Form.Item label="敏感等级" name="sensitivityLevel" extra="来自「敏感等级」配置；与表级安全策略一致">
        <Select
          v-model:value="formValues.sensitivityLevel"
          :options="sensitivityOptions"
          placeholder="请选择敏感等级"
          allow-clear
          show-search
          option-filter-prop="label"
        />
      </Form.Item>
      <Form.Item label="标签" name="tags" extra="多选，可用于检索与分类">
        <Select
          v-model:value="formValues.tags"
          mode="multiple"
          :allow-clear="true"
          :options="tagOptions"
          option-filter-prop="label"
          placeholder="请选择或输入标签"
          show-search
        />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>

<style scoped>
.metadata-table-drawer :deep(.ant-form-item-label > label) {
  color: #334155;
  font-weight: 600;
}
.metadata-tech-desc :deep(.ant-descriptions-item-label) {
  width: 120px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc !important;
}
.metadata-business-form :deep(.ant-form-item-extra) {
  font-size: 12px;
  color: #94a3b8;
}
</style>
