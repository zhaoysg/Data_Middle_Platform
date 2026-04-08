<script setup lang="ts">
import { useVbenDrawer } from '@vben/common-ui';
import { Radio, RadioGroup } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import {
  glossaryMappingAdd,
  glossaryMappingUpdate,
} from '#/api/metadata/glossary';

const emit = defineEmits<{ reload: [] }>();

const isEdit = ref(false);

const formValues = reactive<any>({
  confidence: 100,
  mappingType: 'CONTAINS',
  assetType: 'TABLE',
});

const [Drawer, drawerApi] = useVbenDrawer({
  onClosed: () => {
    Object.keys(formValues).forEach((k) => delete formValues[k]);
    formValues.confidence = 100;
    formValues.mappingType = 'CONTAINS';
    formValues.assetType = 'TABLE';
    isEdit.value = false;
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<{ id?: number }>();
    if (data?.id) {
      Object.assign(formValues, data);
      isEdit.value = true;
    } else {
      isEdit.value = false;
    }
  },
  async onConfirm() {
    const data = drawerApi.getData<{ id?: number }>();
    if (isEdit.value && data?.id) {
      await glossaryMappingUpdate({ id: data.id, ...formValues });
    } else {
      await glossaryMappingAdd({ ...formValues });
    }
    emit('reload');
    drawerApi.close();
  },
});
</script>

<template>
  <Drawer :title="isEdit ? '编辑映射' : '新增映射'">
    <a-form layout="vertical">
      <a-form-item label="术语ID" prop="termId">
        <a-input-number
          v-model:value="formValues.termId"
          :min="1"
          placeholder="请输入术语ID"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="术语名称" prop="termName">
        <a-input v-model:value="formValues.termName" placeholder="请输入术语名称（可选）" />
      </a-form-item>
      <a-form-item label="数据源ID" prop="dsId">
        <a-input-number
          v-model:value="formValues.dsId"
          :min="1"
          placeholder="请输入数据源ID"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="资产类型" prop="assetType">
        <RadioGroup v-model:value="formValues.assetType">
          <Radio value="TABLE">表</Radio>
          <Radio value="COLUMN">列</Radio>
        </RadioGroup>
      </a-form-item>
      <a-form-item label="表名" prop="tableName">
        <a-input v-model:value="formValues.tableName" placeholder="请输入表名" />
      </a-form-item>
      <a-form-item label="列名" prop="columnName">
        <a-input
          v-model:value="formValues.columnName"
          placeholder="请输入列名（资产类型为列时必填）"
        />
      </a-form-item>
      <a-form-item label="映射类型" prop="mappingType">
        <RadioGroup v-model:value="formValues.mappingType">
          <Radio value="CONTAINS">包含</Radio>
          <Radio value="DEFINES">定义</Radio>
          <Radio value="REFERENCES">引用</Radio>
        </RadioGroup>
      </a-form-item>
      <a-form-item label="置信度" prop="confidence">
        <a-input-number
          v-model:value="formValues.confidence"
          :min="0"
          :max="100"
          :step="10"
          class="w-full"
        />
      </a-form-item>
      <a-form-item label="备注" prop="remark">
        <a-textarea v-model:value="formValues.remark" :rows="2" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </Drawer>
</template>
