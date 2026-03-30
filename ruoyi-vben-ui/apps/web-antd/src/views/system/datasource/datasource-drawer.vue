<script setup lang="ts">
import type { Datasource } from '#/api/system/datasource/model';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { $t } from '@vben/locales';
import { cloneDeep } from '@vben/utils';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  datasourceAdd,
  datasourceInfo,
  datasourceTest,
  datasourceUpdate,
} from '#/api/system/datasource';
import { defaultFormValueGetter, useBeforeCloseDiff } from '#/utils/popup';

import { drawerSchema, supportsSchema } from './data';

const emit = defineEmits<{ reload: [] }>();

const isUpdate = ref(false);
const title = computed(() => {
  return isUpdate.value ? $t('pages.common.edit') : $t('pages.common.add');
});

const [BasicForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    formItemClass: 'col-span-1',
  },
  layout: 'vertical',
  schema: drawerSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-4',
});

async function customFormValueGetter() {
  return await defaultFormValueGetter(formApi)();
}

function normalizeDatasourcePayload(data: Partial<Datasource>) {
  if (!supportsSchema(data.dsType)) {
    delete data.schemaName;
  } else if (typeof data.schemaName === 'string') {
    const schemaName = data.schemaName.trim();
    if (schemaName) {
      data.schemaName = schemaName;
    } else {
      delete data.schemaName;
    }
  }

  if (typeof data.connectionParams === 'string') {
    const connectionParams = data.connectionParams.trim();
    if (connectionParams) {
      data.connectionParams = connectionParams;
    } else {
      delete data.connectionParams;
    }
  }

  return data;
}

const { onBeforeClose, markInitialized, resetInitialized } = useBeforeCloseDiff({
  initializedGetter: customFormValueGetter,
  currentGetter: customFormValueGetter,
});

const [BasicDrawer, drawerApi] = useVbenDrawer({
  onBeforeClose,
  onClosed: handleClosed,
  onConfirm: handleConfirm,
  destroyOnClose: true,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return null;
    }
    drawerApi.drawerLoading(true);

    const { id } = drawerApi.getData() as { id?: number | string };
    isUpdate.value = !!id;

    if (isUpdate.value && id) {
      const record = await datasourceInfo(id);
      await formApi.setValues(record);
    }

    await markInitialized();
    drawerApi.drawerLoading(false);
  },
});

/** 测试连接 */
async function handleTestConnection() {
  const { valid } = await formApi.validate();
  if (!valid) {
    return;
  }
  const data = cloneDeep(await formApi.getValues());
  // 去掉 dsId，因为测试不需要
  delete data.dsId;
  normalizeDatasourcePayload(data);

  const hide = message.loading('正在测试连接...');
  try {
    const result = await datasourceTest(data);
    hide();
    if (result.success) {
      message.success(`连接成功！版本：${result.databaseVersion || '未知'}，耗时：${result.elapsedMs}ms`);
    } else {
      message.error(`连接失败：${result.message}`);
    }
  } catch (error: any) {
    hide();
    message.error(`连接异常：${error?.message || '未知错误'}`);
  }
}

async function handleConfirm() {
  const { valid } = await formApi.validate();
  if (!valid) {
    return;
  }
  drawerApi.lock(true);
  try {
    const data = cloneDeep(await formApi.getValues());
    normalizeDatasourcePayload(data);
    if (isUpdate.value) {
      await datasourceUpdate(data);
    } else {
      await datasourceAdd(data);
    }
    emit('reload');
    resetInitialized();
    drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    drawerApi.lock(false);
  }
}

async function handleClosed() {
  await formApi.resetForm();
  resetInitialized();
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[800px]">
    <BasicForm />
    <template #append-footer>
      <a-button
        v-access:code="['system:ds:test']"
        @click="handleTestConnection"
      >
        测试连接
      </a-button>
    </template>
  </BasicDrawer>
</template>
