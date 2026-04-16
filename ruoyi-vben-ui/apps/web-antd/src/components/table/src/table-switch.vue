<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { $t } from '@vben/locales';

import { Modal, Switch } from 'ant-design-vue';
import { isFunction } from 'lodash-es';

type CheckedType = boolean | number | string;

interface Props {
  checkedText?: string;
  unCheckedText?: string;
  checkedValue?: CheckedType;
  unCheckedValue?: CheckedType;
  disabled?: boolean;
  api: () => PromiseLike<void>;
  confirm?: boolean;
  confirmText?: (checked: CheckedType) => string;
}

const props = withDefaults(defineProps<Props>(), {
  checkedText: undefined,
  unCheckedText: undefined,
  checkedValue: '1',
  unCheckedValue: '0',
  confirm: false,
  confirmText: undefined,
});

const emit = defineEmits<{ reload: [] }>();

const checkedTextComputed = computed(() => {
  return props.checkedText ?? $t('pages.common.enable');
});

const unCheckedTextComputed = computed(() => {
  return props.unCheckedText ?? $t('pages.common.disable');
});

const currentChecked = defineModel<CheckedType>('value', {
  default: false,
});

const loading = ref(false);

// 本地临时状态：切换时先更新 UI，API 失败 / 取消时回滚
const localChecked = ref<CheckedType>('0');

watch(
  () => props.value,
  (v) => {
    localChecked.value = v;
  },
  { immediate: true },
);

function handleChange(newValue: CheckedType) {
  const { checkedValue, unCheckedValue } = props;
  const lastStatus = newValue === checkedValue ? unCheckedValue : checkedValue;

  localChecked.value = newValue;

  if (props.confirm) {
    confirmUpdate(newValue, lastStatus);
    return;
  }

  doUpdate(lastStatus, newValue);
}

async function doUpdate(lastStatus: CheckedType, newValue: CheckedType) {
  try {
    loading.value = true;
    const { api } = props;
    await api();
    emit('reload');
    emit('update:value', newValue);
  } catch {
    localChecked.value = lastStatus;
  } finally {
    loading.value = false;
  }
}

function confirmUpdate(checked: CheckedType, lastStatus: CheckedType) {
  const content = isFunction(props.confirmText)
    ? props.confirmText(checked)
    : `确认要更新状态吗？`;

  Modal.confirm({
    title: '提示',
    content,
    centered: true,
    onOk: async () => {
      await doUpdate(lastStatus, checked);
    },
    onCancel: () => {
      localChecked.value = lastStatus;
    },
  });
}
</script>

<style scoped>
.table-switch-wrapper {
  display: inline-block;
  position: relative;
  z-index: 9999;
}
</style>

<template>
  <div class="table-switch-wrapper">
    <Switch
      v-bind="$attrs"
      :loading="loading"
      :disabled="disabled"
      :checked="localChecked"
      :checked-children="checkedTextComputed"
      :checked-value="checkedValue"
      :un-checked-children="unCheckedTextComputed"
      :un-checked-value="unCheckedValue"
      @change="handleChange"
    />
  </div>
</template>
