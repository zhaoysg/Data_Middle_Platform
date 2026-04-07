<script setup lang="ts">
import { useVbenDrawer } from '@vben/common-ui';
import { ElFormItem, ElInput } from 'element-plus';
import { Radio, RadioGroup } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import {
  glossaryTermAdd,
  glossaryTermUpdate,
} from '#/api/metadata/glossary';

const emit = defineEmits<{ reload: [] }>();

const isEdit = ref(false);

const formRules = {
  termName: [{ required: true, message: '请输入术语名称', trigger: 'blur' }],
};

const formValues = reactive<any>({});

const [Drawer, drawerApi] = useVbenDrawer({
  onClosed: () => {
    Object.keys(formValues).forEach((k) => delete formValues[k]);
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
      await glossaryTermUpdate({ id: data.id, ...formValues });
    } else {
      await glossaryTermAdd({ ...formValues });
    }
    emit('reload');
    drawerApi.close();
  },
});
</script>

<template>
  <Drawer :title="isEdit ? '编辑术语' : '新增术语'">
    <ElFormItem label="术语名称" prop="termName" :rules="formRules.termName">
      <ElInput v-model="formValues.termName" placeholder="请输入术语名称" />
    </ElFormItem>
    <ElFormItem label="术语别名" prop="termAlias">
      <ElInput v-model="formValues.termAlias" placeholder="请输入术语别名" />
    </ElFormItem>
    <ElFormItem label="所属分类" prop="categoryId">
      <ElInput v-model.number="formValues.categoryId" type="number" placeholder="请输入分类ID" />
    </ElFormItem>
    <ElFormItem label="关键词" prop="keyword">
      <ElInput v-model="formValues.keyword" placeholder="请输入关键词（多个用逗号分隔）" />
    </ElFormItem>
    <ElFormItem label="术语描述" prop="termDesc">
      <ElInput v-model="formValues.termDesc" type="textarea" :rows="3" placeholder="请输入术语描述" />
    </ElFormItem>
    <ElFormItem label="业务定义" prop="bizDefinition">
      <ElInput v-model="formValues.bizDefinition" type="textarea" :rows="3" placeholder="请输入业务定义" />
    </ElFormItem>
    <ElFormItem label="状态" prop="status">
      <RadioGroup v-model="formValues.status">
        <Radio value="DRAFT">草稿</Radio>
        <Radio value="PUBLISHED">已发布</Radio>
      </RadioGroup>
    </ElFormItem>
  </Drawer>
</template>
