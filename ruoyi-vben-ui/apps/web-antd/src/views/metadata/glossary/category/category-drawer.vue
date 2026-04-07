<script setup lang="ts">
import { useVbenDrawer } from '@vben/common-ui';
import { ElFormItem, ElInput } from 'element-plus';
import { Radio, RadioGroup } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import {
  glossaryCategoryAdd,
  glossaryCategoryUpdate,
  glossaryCategoryOptions,
} from '#/api/metadata/glossary';

const emit = defineEmits<{ reload: [] }>();

const isEdit = ref(false);

const formRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
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
    const data = drawerApi.getData<{ id?: number; parentId?: number }>();
    if (isEdit.value && data?.id) {
      await glossaryCategoryUpdate({ id: data.id, ...formValues });
    } else {
      await glossaryCategoryAdd({ ...data, ...formValues });
    }
    emit('reload');
    drawerApi.close();
  },
});
</script>

<template>
  <Drawer :title="isEdit ? '编辑分类' : '新增分类'">
    <ElFormItem label="上级分类" prop="parentId">
      <ElInput v-model.number="formValues.parentId" placeholder="请选择上级分类（不选则为顶级）" />
    </ElFormItem>
    <ElFormItem label="分类名称" prop="categoryName" :rules="formRules.categoryName">
      <ElInput v-model="formValues.categoryName" placeholder="请输入分类名称" />
    </ElFormItem>
    <ElFormItem label="分类编码" prop="categoryCode" :rules="formRules.categoryCode">
      <ElInput v-model="formValues.categoryCode" placeholder="请输入分类编码" />
    </ElFormItem>
    <ElFormItem label="排序" prop="sortOrder">
      <ElInput v-model.number="formValues.sortOrder" type="number" placeholder="数值越小越靠前" />
    </ElFormItem>
    <ElFormItem label="状态" prop="status">
      <RadioGroup v-model="formValues.status">
        <Radio value="0">正常</Radio>
        <Radio value="1">停用</Radio>
      </RadioGroup>
    </ElFormItem>
    <ElFormItem label="备注" prop="remark">
      <ElInput v-model="formValues.remark" type="textarea" :rows="3" placeholder="请输入备注" />
    </ElFormItem>
  </Drawer>
</template>
