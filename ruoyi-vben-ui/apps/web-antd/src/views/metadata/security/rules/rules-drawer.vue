<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Select, Textarea, message } from 'ant-design-vue';

import {
  secSensitivityRuleAdd,
  secSensitivityRuleInfo,
  secSensitivityRuleUpdate,
} from '#/api/metadata/security/rules';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});

const ruleTypeOptions = [
  { label: '正则匹配', value: 'REGEX' },
  { label: '字段名匹配', value: 'COLUMN_NAME' },
  { label: '数据类型', value: 'DATA_TYPE' },
  { label: '自定义', value: 'CUSTOM' },
];
const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑敏感识别规则' : '新增敏感识别规则';
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
        const info = await secSensitivityRuleInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
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
    if (recordId.value) {
      await secSensitivityRuleUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secSensitivityRuleAdd(formValues.value);
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
</script>

<template>
  <BasicDrawer :title="title" class="w-[600px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="规则名称" name="ruleName">
        <Input v-model:value="formValues.ruleName" placeholder="请输入规则名称" />
      </Form.Item>
      <Form.Item label="规则编码" name="ruleCode">
        <Input v-model:value="formValues.ruleCode" placeholder="请输入规则编码" />
      </Form.Item>
      <Form.Item label="规则类型" name="ruleType">
        <Select
          v-model:value="formValues.ruleType"
          :options="ruleTypeOptions"
          placeholder="请选择规则类型"
        />
      </Form.Item>
      <Form.Item label="规则表达式" name="ruleExpr">
        <Textarea
          v-model:value="formValues.ruleExpr"
          placeholder="请输入规则表达式，如: (?i)(.*name|.*phone|.*email)"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="目标敏感等级" name="targetLevelCode">
        <Input v-model:value="formValues.targetLevelCode" placeholder="如: HIGH, MEDIUM, LOW" />
      </Form.Item>
      <Form.Item label="目标数据分类" name="targetClassCode">
        <Input v-model:value="formValues.targetClassCode" placeholder="请输入目标数据分类编码" />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
