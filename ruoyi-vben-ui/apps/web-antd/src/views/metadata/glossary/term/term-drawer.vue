<script setup lang="ts">
import { useVbenDrawer } from '@vben/common-ui';
import { Radio, RadioGroup } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import {
  glossaryTermAdd,
  glossaryTermUpdate,
} from '#/api/metadata/glossary';

const emit = defineEmits<{ reload: [] }>();

const isEdit = ref(false);
const formValues = reactive<any>({});

const [Drawer, drawerApi] = useVbenDrawer({
  onClosed: () => {
    Object.keys(formValues).forEach((k) => delete formValues[k]);
    isEdit.value = false;
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<Record<string, any>>();
    Object.keys(formValues).forEach((k) => delete formValues[k]);
    if (data?.id) {
      Object.assign(formValues, data);
      isEdit.value = true;
      return;
    }
    isEdit.value = false;
    Object.assign(formValues, {
      categoryId: data?.categoryId,
      status: 'DRAFT',
    });
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
    <a-form layout="vertical">
      <a-form-item label="术语名称" prop="termName">
        <a-input v-model:value="formValues.termName" placeholder="请输入术语名称" />
      </a-form-item>
      <a-form-item label="术语别名" prop="termAlias">
        <a-input v-model:value="formValues.termAlias" placeholder="请输入术语别名" />
      </a-form-item>
      <a-form-item label="所属分类" prop="categoryId">
        <a-input-number
          v-model:value="formValues.categoryId"
          class="w-full"
          placeholder="请输入分类ID"
        />
      </a-form-item>
      <a-form-item label="关键词" prop="keyword">
        <a-input v-model:value="formValues.keyword" placeholder="请输入关键词（多个用逗号分隔）" />
      </a-form-item>
      <a-form-item label="术语描述" prop="termDesc">
        <a-textarea
          v-model:value="formValues.termDesc"
          :rows="3"
          placeholder="请输入术语描述"
        />
      </a-form-item>
      <a-form-item label="业务定义" prop="bizDefinition">
        <a-textarea
          v-model:value="formValues.bizDefinition"
          :rows="3"
          placeholder="请输入业务定义"
        />
      </a-form-item>
      <a-form-item label="状态" prop="status">
        <RadioGroup v-model:value="formValues.status">
          <Radio value="DRAFT">草稿</Radio>
          <Radio value="PUBLISHED">已发布</Radio>
        </RadioGroup>
      </a-form-item>
    </a-form>
  </Drawer>
</template>
