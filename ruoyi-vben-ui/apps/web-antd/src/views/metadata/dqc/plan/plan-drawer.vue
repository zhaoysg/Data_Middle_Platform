<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, Select, Textarea, message } from 'ant-design-vue';

import { datasourceEnabled } from '#/api/system/datasource';
import { dqcPlanAdd, dqcPlanInfo, dqcPlanUpdate } from '#/api/metadata/dqc/plan';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const datasourceOptions = ref<{ label: string; value: number }[]>([]);

const scanModeOptions = [
  { label: '包含', value: 'INCLUDE' },
  { label: '排除', value: 'EXCLUDE' },
  { label: '正则', value: 'REGEX' },
];
const triggerTypeOptions = [
  { label: '手动', value: 'MANUAL' },
  { label: '定时', value: 'SCHEDULE' },
  { label: 'API调用', value: 'API' },
];
const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑质检方案' : '新增质检方案';
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
      const dsList = await datasourceEnabled();
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: item.dsName,
        value: item.dsId,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcPlanInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          status: 'DRAFT',
          triggerType: 'MANUAL',
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
      await dqcPlanUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await dqcPlanAdd(formValues.value);
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
          :rows="2"
        />
      </Form.Item>
      <Form.Item label="数据源" name="dsId">
        <Select
          v-model:value="formValues.dsId"
          :options="datasourceOptions"
          placeholder="请选择数据源"
        />
      </Form.Item>
      <Form.Item label="扫描模式" name="scanMode">
        <Select
          v-model:value="formValues.scanMode"
          :options="scanModeOptions"
          placeholder="请选择扫描模式"
        />
      </Form.Item>
      <Form.Item label="表名模式" name="tablePattern">
        <Input
          v-model:value="formValues.tablePattern"
          placeholder="如: dim_*, fact_* 或 *order*"
        />
      </Form.Item>
      <Form.Item label="触发方式" name="triggerType">
        <Select
          v-model:value="formValues.triggerType"
          :options="triggerTypeOptions"
          placeholder="请选择触发方式"
        />
      </Form.Item>
      <Form.Item label="Cron表达式" name="cronExpr">
        <Input v-model:value="formValues.cronExpr" placeholder="请输入Cron表达式，如: 0 0 2 * * ?" />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
