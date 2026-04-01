<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, Select, Textarea, message } from 'ant-design-vue';

import { datasourceEnabled } from '#/api/system/datasource';
import { dqcTemplateList } from '#/api/metadata/dqc/template';
import { dqcRuleAdd, dqcRuleInfo, dqcRuleUpdate } from '#/api/metadata/dqc/rule';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const templateOptions = ref<{ label: string; value: number }[]>([]);

const dimensionOptions = [
  { label: '完整性', value: 'COMPLETENESS' },
  { label: '唯一性', value: 'UNIQUENESS' },
  { label: '准确性', value: 'ACCURACY' },
  { label: '一致性', value: 'CONSISTENCY' },
  { label: '时效性', value: 'TIMELINESS' },
  { label: '有效性', value: 'VALIDITY' },
  { label: '自定义', value: 'CUSTOM' },
];
const errorLevelOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];
const ruleStrengthOptions = [
  { label: '强约束', value: 'STRONG' },
  { label: '弱约束', value: 'WEAK' },
];
const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑规则' : '新增规则';
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
      // Load datasource options
      const dsList = await datasourceEnabled();
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: item.dsName,
        value: item.dsId,
      }));

      // Load template options
      const templateList = await dqcTemplateList();
      templateOptions.value = ((templateList as any)?.rows || []).map((item: any) => ({
        label: item.templateName,
        value: item.id,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcRuleInfo(id);
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
      await dqcRuleUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await dqcRuleAdd(formValues.value);
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
  <BasicDrawer :title="title" class="w-[650px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="规则名称" name="ruleName">
        <Input v-model:value="formValues.ruleName" placeholder="请输入规则名称" />
      </Form.Item>
      <Form.Item label="规则编码" name="ruleCode">
        <Input v-model:value="formValues.ruleCode" placeholder="请输入规则编码" />
      </Form.Item>
      <Form.Item label="规则描述" name="ruleDesc">
        <Input.TextArea
          v-model:value="formValues.ruleDesc"
          placeholder="请输入规则描述"
          :rows="2"
        />
      </Form.Item>
      <Form.Item label="关联模板" name="templateId">
        <Select
          v-model:value="formValues.templateId"
          :options="templateOptions"
          placeholder="请选择关联模板"
        />
      </Form.Item>
      <Form.Item label="质量维度" name="dimension">
        <Select
          v-model:value="formValues.dimension"
          :options="dimensionOptions"
          placeholder="请选择质量维度"
        />
      </Form.Item>
      <Form.Item label="数据源" name="targetDsId">
        <Select
          v-model:value="formValues.targetDsId"
          :options="datasourceOptions"
          placeholder="请选择数据源"
        />
      </Form.Item>
      <Form.Item label="目标表" name="targetTable">
        <Input v-model:value="formValues.targetTable" placeholder="请输入目标表名" />
      </Form.Item>
      <Form.Item label="目标字段" name="targetColumn">
        <Input v-model:value="formValues.targetColumn" placeholder="请输入目标字段名" />
      </Form.Item>
      <Form.Item label="规则参数" name="ruleParams">
        <Textarea
          v-model:value="formValues.ruleParams"
          placeholder="请输入规则参数JSON"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="错误级别" name="errorLevel">
        <Select
          v-model:value="formValues.errorLevel"
          :options="errorLevelOptions"
          placeholder="请选择错误级别"
        />
      </Form.Item>
      <Form.Item label="规则强度" name="ruleStrength">
        <Select
          v-model:value="formValues.ruleStrength"
          :options="ruleStrengthOptions"
          placeholder="请选择规则强度"
        />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
