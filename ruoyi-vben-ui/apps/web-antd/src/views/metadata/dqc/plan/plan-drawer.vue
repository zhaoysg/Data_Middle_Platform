<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Alert, Form, Input, Radio, Select, message } from 'ant-design-vue';

import { metadataDomainOptions } from '#/api/metadata/domain';
import { metadataLayerOptions } from '#/api/metadata/layer';
import { dqcPlanAdd, dqcPlanInfo, dqcPlanUpdate } from '#/api/metadata/dqc/plan';
import { datasourceEnabled, datasourceTables } from '#/api/system/datasource';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const layerOptions = ref<{ label: string; value: string }[]>([]);
const domainOptions = ref<{ label: string; value: string }[]>([]);
const tableOptions = ref<{ label: string; value: string }[]>([]);
const tableLoading = ref(false);
const initializingLinkage = ref(false);

const bindTypeOptions = [
  { label: '指定表', value: 'TABLE' },
  { label: '表名模式', value: 'PATTERN' },
  { label: '数据域', value: 'DOMAIN' },
  { label: '数仓分层', value: 'LAYER' },
];
const triggerTypeOptions = [
  { label: '手动', value: 'MANUAL' },
  { label: '定时', value: 'SCHEDULE' },
  { label: 'API调用', value: 'API' },
];
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已停用', value: 'DISABLED' },
];
const switchOptions = [
  { label: '是', value: '1' },
  { label: '否', value: '0' },
];

const title = computed(() => {
  return recordId.value ? '编辑质检方案' : '新增质检方案';
});

const isTableBinding = computed(() => ['TABLE', 'PATTERN'].includes(formValues.value.bindType || ''));
const isDomainBinding = computed(() => formValues.value.bindType === 'DOMAIN');
const isLayerBinding = computed(() => formValues.value.bindType === 'LAYER');

function filterOption(input: string, option?: { label?: string }) {
  return String(option?.label ?? '')
    .toLowerCase()
    .includes(input.toLowerCase());
}

async function loadTableOptions(dsId?: number, selectedValue?: string) {
  if (!dsId) {
    tableOptions.value = [];
    return;
  }
  tableLoading.value = true;
  try {
    const tables = await datasourceTables(dsId);
    tableOptions.value = (tables || []).map((item) => ({
      label: item,
      value: item,
    }));
    if (selectedValue && !tableOptions.value.some((item) => item.value === selectedValue)) {
      tableOptions.value.unshift({
        label: selectedValue,
        value: selectedValue,
      });
    }
  } finally {
    tableLoading.value = false;
  }
}

function parseBindValue(raw?: string): Record<string, any> {
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return { raw };
  }
}

function encodeBindValue() {
  if (isTableBinding.value) {
    return JSON.stringify({
      dsId: formValues.value.bindDsId,
      tablePattern: formValues.value.bindTablePattern,
    });
  }
  if (isDomainBinding.value) {
    return JSON.stringify({
      domainCode: formValues.value.bindDomainCode,
    });
  }
  if (isLayerBinding.value) {
    return JSON.stringify({
      layerCode: formValues.value.layerCode,
    });
  }
  return formValues.value.bindValue || '';
}

watch(
  () => formValues.value.bindDsId,
  async (dsId, previousDsId) => {
    if (initializingLinkage.value || dsId === previousDsId) {
      return;
    }
    formValues.value.bindTablePattern = undefined;
    await loadTableOptions(dsId);
  },
);

watch(
  () => formValues.value.bindType,
  (bindType, previousBindType) => {
    if (initializingLinkage.value || bindType === previousBindType) {
      return;
    }
    formValues.value.bindValue = undefined;
    formValues.value.bindDsId = undefined;
    formValues.value.bindTablePattern = undefined;
    formValues.value.bindDomainCode = undefined;
    if (bindType !== 'LAYER') {
      formValues.value.layerCode = undefined;
    }
    tableOptions.value = [];
  },
);

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
      const [dsList, layers, domains] = await Promise.all([
        datasourceEnabled(),
        metadataLayerOptions(),
        metadataDomainOptions(),
      ]);
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: item.dsCode ? `${item.dsName} (${item.dsCode})` : item.dsName,
        value: item.dsId,
      }));
      layerOptions.value = (layers || []).map((item: any) => ({
        label: `${item.layerName} (${item.layerCode})`,
        value: item.layerCode,
      }));
      domainOptions.value = (domains || []).map((item: any) => ({
        label: `${item.domainName}${item.domainCode ? ` (${item.domainCode})` : ''}`,
        value: item.domainCode || item.id,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcPlanInfo(id);
        const bindValue: Record<string, any> = parseBindValue(info.bindValue);
        initializingLinkage.value = true;
        formValues.value = {
          ...info,
          bindDsId: bindValue.dsId,
          bindTablePattern: bindValue.tablePattern,
          bindDomainCode: bindValue.domainCode,
        };
        if (bindValue.dsId) {
          await loadTableOptions(bindValue.dsId, bindValue.tablePattern);
        }
        initializingLinkage.value = false;
      } else {
        recordId.value = undefined;
        formValues.value = {
          bindType: 'TABLE',
          status: 'DRAFT',
          triggerType: 'MANUAL',
          alertOnFailure: '1',
          autoBlock: '0',
        };
        tableOptions.value = [];
      }
    } finally {
      initializingLinkage.value = false;
      drawerApi.drawerLoading(false);
    }
  },
});

async function handleSubmit() {
  drawerApi.lock(true);
  try {
    const payload: Record<string, any> = {
      ...formValues.value,
      bindValue: encodeBindValue(),
    };
    delete payload.bindDsId;
    delete payload.bindTablePattern;
    delete payload.bindDomainCode;

    if (recordId.value) {
      await dqcPlanUpdate({ id: recordId.value, ...payload });
    } else {
      await dqcPlanAdd(payload);
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
  tableOptions.value = [];
  initializingLinkage.value = false;
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[720px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="方案名称" name="planName">
        <Input v-model:value="formValues.planName" placeholder="请输入方案名称" />
      </Form.Item>
      <Form.Item label="方案编码" name="planCode">
        <Input v-model:value="formValues.planCode" placeholder="请输入方案编码" />
      </Form.Item>
      <Form.Item label="方案描述" name="planDesc">
        <Input.TextArea
          v-model:value="formValues.planDesc"
          placeholder="请输入方案描述"
          :rows="3"
        />
      </Form.Item>

      <Alert
        class="mb-4"
        show-icon
        type="info"
        message="方案绑定说明"
        description="方案本身只负责圈定检查范围和触发方式，真正执行哪些规则由方案-规则绑定关系决定。"
      />

      <div class="grid gap-4 md:grid-cols-2">
        <Form.Item label="绑定类型" name="bindType">
          <Select
            v-model:value="formValues.bindType"
            :options="bindTypeOptions"
            placeholder="请选择绑定类型"
          />
        </Form.Item>
        <Form.Item label="触发方式" name="triggerType">
          <Select
            v-model:value="formValues.triggerType"
            :options="triggerTypeOptions"
            placeholder="请选择触发方式"
          />
        </Form.Item>
      </div>

      <template v-if="isTableBinding">
        <div class="grid gap-4 md:grid-cols-2">
          <Form.Item label="绑定数据源" name="bindDsId">
            <Select
              v-model:value="formValues.bindDsId"
              :options="datasourceOptions"
              :filter-option="filterOption"
              option-filter-prop="label"
              placeholder="请选择数据源"
              show-search
            />
          </Form.Item>
          <Form.Item
            :label="formValues.bindType === 'TABLE' ? '指定表' : '表名模式'"
            name="bindTablePattern"
          >
            <Select
              v-if="formValues.bindType === 'TABLE'"
              v-model:value="formValues.bindTablePattern"
              :disabled="!formValues.bindDsId"
              :filter-option="filterOption"
              :loading="tableLoading"
              :options="tableOptions"
              option-filter-prop="label"
              placeholder="请先选择数据源"
              show-search
            />
            <Input
              v-else
              v-model:value="formValues.bindTablePattern"
              placeholder="请输入表名模式，例如 ods_*"
            />
          </Form.Item>
        </div>
      </template>

      <template v-if="isDomainBinding">
        <Form.Item label="绑定数据域" name="bindDomainCode">
          <Select
            v-model:value="formValues.bindDomainCode"
            :options="domainOptions"
            :filter-option="filterOption"
            option-filter-prop="label"
            placeholder="请选择数据域"
            show-search
          />
        </Form.Item>
      </template>

      <template v-if="isLayerBinding">
        <Form.Item label="绑定分层" name="layerCode">
          <Select
            v-model:value="formValues.layerCode"
            :options="layerOptions"
            :filter-option="filterOption"
            option-filter-prop="label"
            placeholder="请选择数仓分层"
            show-search
          />
        </Form.Item>
      </template>

      <div class="grid gap-4 md:grid-cols-3">
        <Form.Item label="Cron表达式" name="triggerCron">
          <Input
            v-model:value="formValues.triggerCron"
            placeholder="定时触发时填写，例如 0 0 2 * * ?"
          />
        </Form.Item>
        <Form.Item label="失败告警" name="alertOnFailure">
          <Radio.Group v-model:value="formValues.alertOnFailure" :options="switchOptions" />
        </Form.Item>
        <Form.Item label="失败阻断" name="autoBlock">
          <Radio.Group v-model:value="formValues.autoBlock" :options="switchOptions" />
        </Form.Item>
      </div>

      <Form.Item label="状态" name="status">
        <Radio.Group v-model:value="formValues.status" :options="statusOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
