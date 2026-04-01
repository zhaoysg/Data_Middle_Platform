<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Select, message } from 'ant-design-vue';

import { datasourceEnabled } from '#/api/system/datasource';
import { secMaskStrategyAdd, secMaskStrategyInfo, secMaskStrategyUpdate } from '#/api/metadata/security/strategy';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const datasourceOptions = ref<{ label: string; value: number }[]>([]);

const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑脱敏策略' : '新增脱敏策略';
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
        const info = await secMaskStrategyInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
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
      await secMaskStrategyUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secMaskStrategyAdd(formValues.value);
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
      <Form.Item label="策略名称" name="strategyName">
        <Input v-model:value="formValues.strategyName" placeholder="请输入策略名称" />
      </Form.Item>
      <Form.Item label="策略编码" name="strategyCode">
        <Input v-model:value="formValues.strategyCode" placeholder="请输入策略编码" />
      </Form.Item>
      <Form.Item label="策略描述" name="strategyDesc">
        <Input.TextArea
          v-model:value="formValues.strategyDesc"
          placeholder="请输入策略描述"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="数据源" name="dsId">
        <Select
          v-model:value="formValues.dsId"
          :options="datasourceOptions"
          placeholder="请选择数据源"
        />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
