<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Textarea, message } from 'ant-design-vue';

import {
  secMaskTemplateAdd,
  secMaskTemplateInfo,
  secMaskTemplateUpdate,
} from '#/api/metadata/security/mask-template';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});

const enabledOptions = [
  { label: '启用', value: '1' },
  { label: '停用', value: '0' },
];

const title = computed(() => {
  return recordId.value ? '编辑脱敏模板' : '新增脱敏模板';
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
        const info = await secMaskTemplateInfo(id);
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
    if (recordId.value) {
      await secMaskTemplateUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secMaskTemplateAdd(formValues.value);
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
      <Form.Item label="模板名称" name="templateName">
        <Input v-model:value="formValues.templateName" placeholder="请输入模板名称" />
      </Form.Item>
      <Form.Item label="模板编码" name="templateCode">
        <Input v-model:value="formValues.templateCode" placeholder="请输入模板编码" />
      </Form.Item>
      <Form.Item label="模板类型" name="templateType">
        <Radio.Group v-model:value="formValues.templateType">
          <Radio value="ENCRYPT">加密</Radio>
          <Radio value="MASK">掩码</Radio>
          <Radio value="HIDE">隐藏</Radio>
          <Radio value="DELETE">删除</Radio>
          <Radio value="SHUFFLE">打乱</Radio>
          <Radio value="CUSTOM">自定义</Radio>
        </Radio.Group>
      </Form.Item>
      <Form.Item label="脱敏表达式" name="maskExpr">
        <Textarea
          v-model:value="formValues.maskExpr"
          placeholder="请输入脱敏表达式，如: SUBSTR({col}, 0, 3) || '****'"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="模板描述" name="templateDesc">
        <Input.TextArea
          v-model:value="formValues.templateDesc"
          placeholder="请输入模板描述"
          :rows="2"
        />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
