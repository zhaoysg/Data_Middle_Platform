<script setup lang="ts">
import { useVbenDrawer } from '@vben/common-ui';
import { ElFormItem, ElInput, ElInputNumber, ElSelect, ElOption } from 'element-plus';
import { Radio, RadioGroup } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import {
  glossaryMappingAdd,
  glossaryMappingUpdate,
} from '#/api/metadata/glossary';

const emit = defineEmits<{ reload: [] }>();

const isEdit = ref(false);

const formRules = {
  termId: [{ required: true, message: '请输入术语ID', trigger: 'blur' }],
  dsId: [{ required: true, message: '请输入数据源ID', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
};

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
    if (isOpen) {
      const data = drawerApi.getData<{ id?: number }>();
      if (data?.id) {
        Object.assign(formValues, data);
        isEdit.value = true;
      } else {
        isEdit.value = false;
      }
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
    <ElFormItem label="术语ID" prop="termId" :rules="formRules.termId">
      <ElInputNumber
        v-model="formValues.termId"
        :min="1"
        placeholder="请输入术语ID"
        class="w-full"
      />
    </ElFormItem>
    <ElFormItem label="术语名称" prop="termName">
      <ElInput v-model="formValues.termName" placeholder="请输入术语名称（可选）" />
    </ElFormItem>
    <ElFormItem label="数据源ID" prop="dsId" :rules="formRules.dsId">
      <ElInputNumber
        v-model="formValues.dsId"
        :min="1"
        placeholder="请输入数据源ID"
        class="w-full"
      />
    </ElFormItem>
    <ElFormItem label="资产类型" prop="assetType">
      <RadioGroup v-model="formValues.assetType">
        <Radio value="TABLE">表</Radio>
        <Radio value="COLUMN">列</Radio>
      </RadioGroup>
    </ElFormItem>
    <ElFormItem label="表名" prop="tableName" :rules="formRules.tableName">
      <ElInput v-model="formValues.tableName" placeholder="请输入表名" />
    </ElFormItem>
    <ElFormItem label="列名" prop="columnName">
      <ElInput v-model="formValues.columnName" placeholder="请输入列名（资产类型为列时必填）" />
    </ElFormItem>
    <ElFormItem label="映射类型" prop="mappingType">
      <RadioGroup v-model="formValues.mappingType">
        <Radio value="CONTAINS">包含</Radio>
        <Radio value="DEFINES">定义</Radio>
        <Radio value="REFERENCES">引用</Radio>
      </RadioGroup>
    </ElFormItem>
    <ElFormItem label="置信度" prop="confidence">
      <ElInputNumber
        v-model="formValues.confidence"
        :min="0"
        :max="100"
        :step="10"
        class="w-full"
      />
    </ElFormItem>
    <ElFormItem label="备注" prop="remark">
      <ElInput v-model="formValues.remark" type="textarea" :rows="2" placeholder="请输入备注" />
    </ElFormItem>
  </Drawer>
</template>
