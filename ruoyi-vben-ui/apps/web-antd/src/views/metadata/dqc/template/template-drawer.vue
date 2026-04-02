<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Select, Textarea, message } from 'ant-design-vue';

import {
  dqcTemplateAdd,
  dqcTemplateInfo,
  dqcTemplateUpdate,
} from '#/api/metadata/dqc/template';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const dimensionOptions = [
  { label: '完整性', value: 'COMPLETENESS' },
  { label: '唯一性', value: 'UNIQUENESS' },
  { label: '准确性', value: 'ACCURACY' },
  { label: '一致性', value: 'CONSISTENCY' },
  { label: '时效性', value: 'TIMELINESS' },
  { label: '有效性', value: 'VALIDITY' },
  { label: '自定义', value: 'CUSTOM' },
];
const ruleTypeOptions = [
  { label: '空值检查', value: 'NULL_CHECK' },
  { label: '唯一性', value: 'UNIQUE' },
  { label: '正则匹配', value: 'REGEX' },
  { label: '阈值检查', value: 'THRESHOLD' },
  { label: '波动检查', value: 'FLUCTUATION' },
  { label: '自定义SQL', value: 'CUSTOM_SQL' },
];
const applyLevelOptions = [
  { label: '表级', value: 'TABLE' },
  { label: '字段级', value: 'COLUMN' },
  { label: '跨字段', value: 'CROSS_FIELD' },
  { label: '跨表', value: 'CROSS_TABLE' },
];
const enabledOptions = [
  { label: '启用', value: '1' },
  { label: '停用', value: '0' },
];

const title = computed(() => {
  return recordId.value ? '编辑规则模板' : '新增规则模板';
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
      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcTemplateInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '1',
          builtin: '0',
        };
      }
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

async function handleSubmit() {
  drawerApi.lock(true);
  try {
    formatJsonField('thresholdJson');
    formatJsonField('paramSpec');
    if (recordId.value) {
      await dqcTemplateUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await dqcTemplateAdd(formValues.value);
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
}

function formatJsonField(field: 'paramSpec' | 'thresholdJson') {
  const value = formValues.value[field];
  if (!value || typeof value !== 'string') {
    return;
  }
  try {
    formValues.value[field] = JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    // Keep the user's original content if it is not valid JSON yet.
  }
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[600px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="模板名称" name="templateName">
        <Input v-model:value="formValues.templateName" placeholder="请输入模板名称" />
      </Form.Item>
      <Form.Item label="模板编码" name="templateCode">
        <Input v-model:value="formValues.templateCode" placeholder="请输入模板编码" />
      </Form.Item>
      <Form.Item label="模板描述" name="templateDesc">
        <Input.TextArea
          v-model:value="formValues.templateDesc"
          placeholder="请输入模板描述"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="质量维度" name="dimension">
        <Select
          v-model:value="formValues.dimension"
          :options="dimensionOptions"
          placeholder="请选择质量维度"
        />
      </Form.Item>
      <Form.Item label="规则类型" name="ruleType">
        <Select
          v-model:value="formValues.ruleType"
          :options="ruleTypeOptions"
          placeholder="请选择规则类型"
        />
      </Form.Item>
      <Form.Item label="适用级别" name="applyLevel">
        <Select
          v-model:value="formValues.applyLevel"
          :options="applyLevelOptions"
          placeholder="请选择适用级别"
        />
      </Form.Item>
      <Form.Item label="默认表达式" name="defaultExpr">
        <Input.TextArea
          v-model:value="formValues.defaultExpr"
          placeholder="请输入默认表达式"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="阈值JSON" name="thresholdJson">
        <div class="mb-2 flex justify-end">
          <a-button size="small" type="link" @click="formatJsonField('thresholdJson')">
            格式化 JSON
          </a-button>
        </div>
        <Textarea
          v-model:value="formValues.thresholdJson"
          placeholder="请输入阈值JSON，如: {&quot;min&quot;: 0, &quot;max&quot;: 100}"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="参数规格" name="paramSpec">
        <div class="mb-2 flex justify-end">
          <a-button size="small" type="link" @click="formatJsonField('paramSpec')">
            格式化 JSON
          </a-button>
        </div>
        <Input.TextArea
          v-model:value="formValues.paramSpec"
          placeholder="请输入参数规格，例如 [{&quot;name&quot;:&quot;column&quot;,&quot;required&quot;:true}]"
          :rows="4"
        />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
