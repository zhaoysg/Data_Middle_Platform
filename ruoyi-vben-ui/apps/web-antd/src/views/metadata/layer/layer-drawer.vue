<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, InputNumber, Radio, message } from 'ant-design-vue';

import {
  metadataLayerAdd,
  metadataLayerInfo,
  metadataLayerUpdate,
} from '#/api/metadata/layer';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() => {
  return recordId.value ? '编辑数仓分层' : '新增数仓分层';
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
        const info = await metadataLayerInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          layerColor: '#1890ff',
          sortOrder: 0,
          status: '0',
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
      await metadataLayerUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await metadataLayerAdd(formValues.value);
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
      <Form.Item label="分层编码" name="layerCode">
        <Input v-model:value="formValues.layerCode" placeholder="如 ODS、DWD、DWS、ADS" />
      </Form.Item>
      <Form.Item label="分层名称" name="layerName">
        <Input v-model:value="formValues.layerName" placeholder="请输入分层名称" />
      </Form.Item>
      <Form.Item label="分层描述" name="layerDesc">
        <Input.TextArea
          v-model:value="formValues.layerDesc"
          placeholder="请输入分层描述"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="颜色" name="layerColor">
        <Input v-model:value="formValues.layerColor" placeholder="如 #409EFF" />
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
          :rows="2"
        />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
