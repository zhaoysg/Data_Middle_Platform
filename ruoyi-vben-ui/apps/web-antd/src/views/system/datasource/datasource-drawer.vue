<script setup lang="ts">
import type { Datasource } from '#/api/system/datasource/model';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
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
  schema: drawerSchema({
    onGenerateCode: handleGenerateCode,
  }),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6 gap-y-2',
});

const lastAutoCode = ref<string | null>(null);

function generateDatasourceCode(dsType: string) {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  const timestamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  const random = Math.floor(Math.random() * 9000) + 1000;
  return `${dsType.toUpperCase()}_${timestamp}_${random}`;
}

watch(
  () => formApi.form?.values?.dsType as string | undefined,
  (dsType) => {
    if (!dsType) {
      return;
    }
    const currentCode = formApi.form?.values?.dsCode as string | undefined;
    if (!currentCode || currentCode === lastAutoCode.value) {
      const generated = generateDatasourceCode(dsType);
      lastAutoCode.value = generated;
      formApi.setFieldValue('dsCode', generated);
    }
  },
);

watch(
  () => formApi.form?.values?.dsCode as string | undefined,
  (value) => {
    if (value && value !== lastAutoCode.value) {
      lastAutoCode.value = null;
    }
  },
);

async function customFormValueGetter() {
  return await defaultFormValueGetter(formApi)();
}

function handleGenerateCode() {
  const dsType = formApi.form?.values?.dsType as string | undefined;
  if (!dsType) {
    message.warning('请先选择数据源类型');
    return;
  }
  const generated = generateDatasourceCode(dsType);
  lastAutoCode.value = generated;
  formApi.setFieldValue('dsCode', generated);
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

  if (typeof data.dataSource === 'string') {
    const dataSource = data.dataSource.trim();
    if (dataSource) {
      data.dataSource = dataSource;
    } else {
      delete data.dataSource;
    }
  }

  if (!data.dsFlag) {
    data.dsFlag = '0';
  }

  return data;
}

const { onBeforeClose, markInitialized, resetInitialized } = useBeforeCloseDiff({
  initializedGetter: customFormValueGetter,
  currentGetter: customFormValueGetter,
});

const [BasicModal, modalApi] = useVbenModal({
  fullscreenButton: false,
  onBeforeClose,
  onClosed: handleClosed,
  onConfirm: handleConfirm,
  destroyOnClose: true,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return null;
    }
    modalApi.modalLoading(true);

    lastAutoCode.value = null;
    const { id } = modalApi.getData() as { id?: number | string };
    isUpdate.value = !!id;

    if (isUpdate.value && id) {
      const record = await datasourceInfo(id);
      await formApi.setValues({
        dsFlag: '0',
        status: '0',
        ...record,
      });
    } else {
      await formApi.setValues({
        dsFlag: '0',
        status: '0',
      });
    }

    await markInitialized();
    modalApi.modalLoading(false);
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
  modalApi.lock(true);
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
    modalApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    modalApi.lock(false);
  }
}

async function handleClosed() {
  await formApi.resetForm();
  resetInitialized();
  lastAutoCode.value = null;
}
</script>

<template>
  <BasicModal :close-on-click-modal="false" :title="title" class="w-[760px]">
    <BasicForm />
    <template #append-footer>
      <a-button
        v-access:code="['system:ds:test']"
        @click="handleTestConnection"
      >
        测试连接
      </a-button>
    </template>
  </BasicModal>
</template>
