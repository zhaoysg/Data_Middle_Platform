<script setup lang="ts">
import { useVbenDrawer } from '@vben/common-ui';
import { Radio, RadioGroup } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import {
  glossaryCategoryAdd,
  glossaryCategoryUpdate,
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
      parentId: data?.parentId,
      sortOrder: 0,
      status: '0',
    });
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
    <a-form layout="vertical">
      <a-form-item label="上级分类" prop="parentId">
        <a-input v-model:value="formValues.parentId" placeholder="请选择上级分类（不选则为顶级）" />
      </a-form-item>
      <a-form-item label="分类名称" prop="categoryName">
        <a-input v-model:value="formValues.categoryName" placeholder="请输入分类名称" />
      </a-form-item>
      <a-form-item label="分类编码" prop="categoryCode">
        <a-input v-model:value="formValues.categoryCode" placeholder="请输入分类编码" />
      </a-form-item>
      <a-form-item label="排序" prop="sortOrder">
        <a-input-number
          v-model:value="formValues.sortOrder"
          class="w-full"
          placeholder="数值越小越靠前"
        />
      </a-form-item>
      <a-form-item label="状态" prop="status">
        <RadioGroup v-model:value="formValues.status">
          <Radio value="0">正常</Radio>
          <Radio value="1">停用</Radio>
        </RadioGroup>
      </a-form-item>
      <a-form-item label="备注" prop="remark">
        <a-textarea
          v-model:value="formValues.remark"
          :rows="3"
          placeholder="请输入备注"
        />
      </a-form-item>
    </a-form>
  </Drawer>
</template>
