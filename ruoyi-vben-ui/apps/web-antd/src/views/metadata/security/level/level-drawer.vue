<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, message } from 'ant-design-vue';

import {
  secLevelAdd,
  secLevelInfo,
  secLevelUpdate,
} from '#/api/metadata/security/level';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑敏感等级' : '新增敏感等级';
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
        const info = await secLevelInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          sortOrder: 0,
          levelValue: 1,
          color: '#1677ff',
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
      await secLevelUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secLevelAdd(formValues.value);
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
  <BasicDrawer :title="title" class="w-[520px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="等级名称" name="levelName">
        <Input v-model:value="formValues.levelName" placeholder="请输入敏感等级名称" />
      </Form.Item>
      <Form.Item label="等级编码" name="levelCode">
        <Input v-model:value="formValues.levelCode" placeholder="请输入等级编码，如 L1" />
      </Form.Item>
      <Form.Item label="等级值" name="levelValue">
        <InputNumber v-model:value="formValues.levelValue" :min="1" :max="99" style="width: 100%" />
      </Form.Item>
      <Form.Item label="展示颜色" name="color">
        <Input v-model:value="formValues.color" placeholder="请输入颜色值，如 #1677ff" />
      </Form.Item>
      <Form.Item label="等级描述" name="levelDesc">
        <Input.TextArea
          v-model:value="formValues.levelDesc"
          placeholder="请输入等级描述"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="排序" name="sortOrder">
        <InputNumber v-model:value="formValues.sortOrder" :min="0" style="width: 100%" />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
