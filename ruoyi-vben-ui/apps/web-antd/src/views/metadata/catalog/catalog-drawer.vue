<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, Select, TreeSelect, message } from 'ant-design-vue';

import {
  metadataCatalogAdd,
  metadataCatalogInfo,
  metadataCatalogOptions,
  metadataCatalogUpdate,
} from '#/api/metadata/catalog';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const catalogTypeOptions = [
  { label: '业务域', value: 'BUSINESS_DOMAIN' },
  { label: '数据域', value: 'DATA_DOMAIN' },
  { label: '专辑', value: 'ALBUM' },
];
const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];
const catalogTreeOptions = ref<Record<string, any>[]>([]);
const isCatalogCodeTouched = ref(false);

const title = computed(() => {
  return recordId.value ? '编辑资产目录' : '新增资产目录';
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
      await loadCatalogOptions();
      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await metadataCatalogInfo(id);
        formValues.value = { ...info };
        isCatalogCodeTouched.value = true;
      } else {
        recordId.value = undefined;
        formValues.value = {
          catalogType: 'BUSINESS_DOMAIN',
          sortOrder: 0,
          status: '0',
        };
        isCatalogCodeTouched.value = false;
      }
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

async function loadCatalogOptions() {
  const catalogs = await metadataCatalogOptions();
  catalogTreeOptions.value = mapTreeOptions(catalogs || []);
}

async function handleSubmit() {
  if (!formValues.value.catalogCode?.trim()) {
    formValues.value.catalogCode = generateCatalogCode(
      formValues.value.catalogName,
      formValues.value.catalogType,
    );
  }
  drawerApi.lock(true);
  try {
    if (recordId.value) {
      await metadataCatalogUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await metadataCatalogAdd(formValues.value);
    }
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
  formValues.value = {};
  isCatalogCodeTouched.value = false;
}

function mapTreeOptions(catalogs: any[]): Record<string, any>[] {
  return catalogs.map((item) => ({
    label: item.catalogName,
    value: item.id,
    children: item.children ? mapTreeOptions(item.children) : undefined,
  }));
}

watch(
  () => [formValues.value.catalogName, formValues.value.catalogType],
  () => {
    if (recordId.value || isCatalogCodeTouched.value) {
      return;
    }
    formValues.value.catalogCode = generateCatalogCode(
      formValues.value.catalogName,
      formValues.value.catalogType,
    );
  },
);

function handleCatalogCodeInput(value?: string) {
  formValues.value.catalogCode = value;
  isCatalogCodeTouched.value = true;
}

function handleCatalogCodeBlur() {
  if (formValues.value.catalogCode?.trim()) {
    return;
  }
  isCatalogCodeTouched.value = false;
  formValues.value.catalogCode = generateCatalogCode(
    formValues.value.catalogName,
    formValues.value.catalogType,
  );
}

function generateCatalogCode(catalogName?: string, catalogType?: string) {
  const prefixMap: Record<string, string> = {
    BUSINESS_DOMAIN: 'BUS',
    DATA_DOMAIN: 'DOM',
    ALBUM: 'ALB',
  };
  const prefix = prefixMap[catalogType || ''] || 'CAT';
  const normalizedName = normalizeCatalogSegment(catalogName);
  return normalizedName ? `${prefix}_${normalizedName}`.slice(0, 50) : '';
}

function normalizeCatalogSegment(value?: string) {
  const text = value?.trim();
  if (!text) {
    return '';
  }
  const asciiSegment = text
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Za-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .replace(/_+/g, '_')
    .toUpperCase();
  if (asciiSegment) {
    return asciiSegment;
  }
  return Array.from(text)
    .slice(0, 6)
    .map((char) => `U${char.codePointAt(0)?.toString(16).toUpperCase()}`)
    .join('_');
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[500px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="目录名称" name="catalogName">
        <Input v-model:value="formValues.catalogName" placeholder="请输入目录名称" />
      </Form.Item>
      <Form.Item label="目录编码" name="catalogCode">
        <Input
          :value="formValues.catalogCode"
          placeholder="根据目录名称自动生成，可手动调整"
          @blur="handleCatalogCodeBlur"
          @update:value="handleCatalogCodeInput"
        />
      </Form.Item>
      <Form.Item label="目录类型" name="catalogType">
        <Select
          v-model:value="formValues.catalogType"
          :options="catalogTypeOptions"
          placeholder="请选择目录类型"
        />
      </Form.Item>
      <Form.Item label="父目录" name="parentId">
        <TreeSelect
          v-model:value="formValues.parentId"
          :allow-clear="true"
          :tree-data="catalogTreeOptions"
          placeholder="不选则为顶级目录"
          style="width: 100%"
        />
      </Form.Item>
      <Form.Item label="排序" name="sortOrder">
        <InputNumber
          v-model:value="formValues.sortOrder"
          :min="0"
          style="width: 100%"
        />
      </Form.Item>
      <Form.Item label="状态" name="status">
        <Radio.Group v-model:value="formValues.status" :options="statusOptions" />
      </Form.Item>
      <Form.Item label="备注" name="remark">
        <Input.TextArea
          v-model:value="formValues.remark"
          placeholder="请输入备注"
          :rows="3"
        />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
