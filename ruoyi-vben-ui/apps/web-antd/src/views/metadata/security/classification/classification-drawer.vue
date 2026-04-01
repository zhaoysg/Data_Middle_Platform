<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, message } from 'ant-design-vue';

import {
  secClassificationAdd,
  secClassificationInfo,
  secClassificationUpdate,
} from '#/api/metadata/security/classification';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑数据分类' : '新增数据分类';
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
        const info = await secClassificationInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          sortOrder: 0,
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
      await secClassificationUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secClassificationAdd(formValues.value);
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
  <BasicDrawer :title="title" class="w-[500px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="分类名称" name="className">
        <Input v-model:value="formValues.className" placeholder="请输入分类名称" />
      </Form.Item>
      <Form.Item label="分类编码" name="classCode">
        <Input v-model:value="formValues.classCode" placeholder="请输入分类编码" />
      </Form.Item>
      <Form.Item label="分类描述" name="classDesc">
        <Input.TextArea
          v-model:value="formValues.classDesc"
          placeholder="请输入分类描述"
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
