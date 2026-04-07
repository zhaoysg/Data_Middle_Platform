<script setup lang="ts">
import { computed, ref } from 'vue';
import { Steps, Button, Space } from 'ant-design-vue';

export interface WizardStep {
  title: string;
  description?: string;
}

export interface WizardDrawerProps {
  steps?: WizardStep[];
  loading?: boolean;
}

const props = withDefaults(defineProps<WizardDrawerProps>(), {
  loading: false,
});

const emit = defineEmits<{
  'update:currentStep': [val: number];
  'next': [];
  'finish': [val: any];
  'step-change': [val: number];
}>();

const currentStep = defineModel<number>({ default: 0 });
const submitting = ref(false);

const totalSteps = computed(() => props.steps?.length || 0);
const isFirstStep = computed(() => currentStep.value === 0);
const isLastStep = computed(() => currentStep.value === totalSteps.value - 1);

function setStep(s: number) {
  if (s >= 0 && s < totalSteps.value) {
    currentStep.value = s;
    emit('step-change', currentStep.value);
  }
}

function nextStep() {
  if (currentStep.value < totalSteps.value - 1) {
    currentStep.value++;
    emit('step-change', currentStep.value);
  }
}

function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--;
    emit('step-change', currentStep.value);
  }
}

function handleNext() {
  emit('next');
}

function handleFinish(data: any) {
  submitting.value = true;
  try {
    emit('finish', data);
  } finally {
    submitting.value = false;
  }
}

defineExpose({
  setStep,
  nextStep,
  prevStep,
  setSubmitting: (v: boolean) => (submitting.value = v),
  resetStep: () => (currentStep.value = 0),
});
</script>

<template>
  <div class="flex flex-col h-full wizard-body">
    <Steps
      v-if="steps && steps.length > 0"
      :current="currentStep"
      size="small"
      class="mb-6 wizard-steps"
      :items="steps"
    />

    <div class="flex-1 overflow-auto min-h-0 wizard-content">
      <slot :name="'step-' + currentStep" />
    </div>

    <div class="flex justify-end gap-3 pt-4 border-t mt-4 wizard-footer">
      <Space>
        <Button v-if="!isFirstStep" :disabled="loading || submitting" @click="prevStep">
          上一步
        </Button>
        <Button v-if="!isLastStep" type="primary" :disabled="loading || submitting" @click="handleNext">
          下一步
        </Button>
        <Button
          v-if="isLastStep"
          type="primary"
          :loading="submitting || loading"
          @click="handleFinish($event)"
        >
          保存
        </Button>
      </Space>
    </div>
  </div>
</template>

<style scoped>
.wizard-body {
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.wizard-steps {
  flex-shrink: 0;
}
.wizard-content {
  flex: 1;
  min-height: 0;
}
.wizard-footer {
  flex-shrink: 0;
}
</style>
