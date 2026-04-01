<script setup lang="ts">
import { ref, computed } from 'vue';
import { Steps, Button, Space } from 'ant-design-vue';
import { Drawer } from 'ant-design-vue';

interface Step {
  title: string;
  description?: string;
}

interface Props {
  title?: string;
  steps?: Step[];
  width?: number | string;
  loading?: boolean;
  closable?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: '向导',
  width: 720,
  loading: false,
  closable: true,
});

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'finish', val: any): void;
  (e: 'step-change', val: number): void;
}>();

const visible = defineModel<boolean>('visible', { default: false });
const currentStep = ref(0);
const submitting = ref(false);

const totalSteps = computed(() => props.steps?.length || 0);
const isFirstStep = computed(() => currentStep.value === 0);
const isLastStep = computed(() => currentStep.value === totalSteps.value - 1);

function handleNext() {
  if (currentStep.value < totalSteps.value - 1) {
    currentStep.value++;
    emit('step-change', currentStep.value);
  }
}

function handlePrev() {
  if (currentStep.value > 0) {
    currentStep.value--;
    emit('step-change', currentStep.value);
  }
}

async function handleFinish(data: any) {
  submitting.value = true;
  try {
    emit('finish', data);
    visible.value = false;
    currentStep.value = 0;
  } finally {
    submitting.value = false;
  }
}

function handleClose() {
  visible.value = false;
  currentStep.value = 0;
}

defineExpose({ currentStep, setStep: (s: number) => (currentStep.value = s) });
</script>

<template>
  <Drawer
    v-model:open="visible"
    :title="title"
    :width="width"
    :closable="closable"
    :maskClosable="!loading"
    :destroyOnClose="true"
    @close="handleClose"
  >
    <div class="flex flex-col h-full">
      <Steps :current="currentStep" size="small" class="mb-6" :items="steps" />

      <div class="flex-1 overflow-auto min-h-0">
        <slot :name="'step-' + currentStep" />
      </div>

      <div class="flex justify-end gap-3 pt-4 border-t mt-4">
        <Space>
          <Button v-if="!isFirstStep" :disabled="loading" @click="handlePrev">
            上一步
          </Button>
          <Button v-if="!isLastStep" type="primary" :disabled="loading" @click="handleNext">
            下一步
          </Button>
          <Button
            v-if="isLastStep"
            type="primary"
            :loading="submitting || loading"
            @click="emit('finish', $event)"
          >
            保存
          </Button>
          <Button :disabled="submitting || loading" @click="handleClose">
            取消
          </Button>
        </Space>
      </div>
    </div>
  </Drawer>
</template>
