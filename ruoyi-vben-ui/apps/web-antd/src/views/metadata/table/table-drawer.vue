<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Select, message } from 'ant-design-vue';

import { metadataDomainOptions } from '#/api/metadata/domain';
import { metadataLayerOptions } from '#/api/metadata/layer';
import {
  metadataTableInfo,
  metadataTableTagOptions,
  metadataTableUpdate,
} from '#/api/metadata/table';
import { datasourceEnabled } from '#/api/system/datasource';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const isEdit = ref(false);

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const layerOptions = ref<{ label: string; value: string }[]>([]);
const domainOptions = ref<{ label: string; value: string }[]>([]);
const tagOptions = ref<{ label: string; value: string }[]>([]);
const sensitivityOptions = [
  { label: '普通', value: 'NORMAL' },
  { label: '内部', value: 'INNER' },
  { label: '敏感', value: 'SENSITIVE' },
  { label: '高度敏感', value: 'HIGHLY_SENSITIVE' },
];

const title = computed(() => {
  return isEdit.value ? '编辑元数据表' : '新增元数据表';
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

async function loadOptions() {
  const [datasources, layers, domains, tags] = await Promise.all([
    datasourceEnabled(),
    metadataLayerOptions(),
    metadataDomainOptions(),
    metadataTableTagOptions(),
  ]);
  datasourceOptions.value = datasources.map((ds: any) => ({
    label: ds.dsName,
    value: ds.dsId,
  }));
  layerOptions.value = layers.map((layer: any) => ({
    label: `${layer.layerCode} - ${layer.layerName}`,
    value: layer.layerCode,
  }));
  domainOptions.value = domains.map((domain: any) => ({
    label: domain.domainCode ? `${domain.domainCode} - ${domain.domainName}` : domain.domainName,
    value: domain.domainName,
  }));
  tagOptions.value = mergeTagOptions(tags || []);
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
</script>

<template>
  <BasicDrawer :title="title" class="w-[500px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="数据源" name="dsId">
        <Select
          v-model:value="formValues.dsId"
          :disabled="isEdit"
          :options="datasourceOptions"
          placeholder="请选择数据源"
        />
      </Form.Item>
      <Form.Item label="表名" name="tableName">
        <Input
          v-model:value="formValues.tableName"
          :disabled="isEdit"
          placeholder="请输入物理表名"
        />
      </Form.Item>
      <Form.Item label="表别名" name="tableAlias">
        <Input v-model:value="formValues.tableAlias" placeholder="请输入表别名" />
      </Form.Item>
      <Form.Item label="表注释" name="tableComment">
        <Input.TextArea
          v-model:value="formValues.tableComment"
          placeholder="请输入表注释"
          :rows="2"
        />
      </Form.Item>
      <Form.Item label="数仓分层" name="dataLayer">
        <Select
          v-model:value="formValues.dataLayer"
          :options="layerOptions"
          placeholder="请选择分层"
        />
      </Form.Item>
      <Form.Item label="数据域" name="dataDomain">
        <Select
          v-model:value="formValues.dataDomain"
          :allow-clear="true"
          :options="domainOptions"
          option-filter-prop="label"
          placeholder="请选择数据域"
          show-search
        />
      </Form.Item>
      <Form.Item label="敏感等级" name="sensitivityLevel">
        <Select
          v-model:value="formValues.sensitivityLevel"
          :options="sensitivityOptions"
          placeholder="请选择敏感等级"
        />
      </Form.Item>
      <Form.Item label="标签" name="tags">
        <Select
          v-model:value="formValues.tags"
          mode="multiple"
          :allow-clear="true"
          :options="tagOptions"
          option-filter-prop="label"
          placeholder="请选择标签"
          show-search
        />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
