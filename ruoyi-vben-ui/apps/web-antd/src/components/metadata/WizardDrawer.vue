<script setup lang="ts">
import { computed, ref } from 'vue';
import { Steps, Button, Space } from 'ant-design-vue';
import { CheckOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue';

export interface WizardStep {
  title: string;
  description?: string;
}

export interface WizardDrawerProps {
  steps?: WizardStep[];
  loading?: boolean;
  /** ant: 默认 Ant Steps；horizontal: bgdata 质检方案横向步骤条（含副标题与连线） */
  stepVariant?: 'ant' | 'horizontal';
  /** 底部主按钮对齐：规则定义等设计稿为左侧 */
  footerAlign?: 'start' | 'end';
}

const props = withDefaults(defineProps<WizardDrawerProps>(), {
  loading: false,
  stepVariant: 'ant',
  footerAlign: 'end',
});

const emit = defineEmits<{
  'update:currentStep': [val: number];
  next: [];
  finish: [val: any];
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

function onHorizontalStepClick(index: number) {
  if (currentStep.value > index) {
    currentStep.value = index;
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
      v-if="stepVariant === 'ant' && steps && steps.length > 0"
      :current="currentStep"
      size="small"
      class="mb-6 wizard-steps-ant"
      :items="steps"
    />

    <div
      v-else-if="stepVariant === 'horizontal' && steps && steps.length > 0"
      class="wizard-steps-horizontal"
    >
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="wizard-step-h"
        :class="{
          'step-h-active': currentStep === index,
          'step-h-done': currentStep > index,
          'step-h-pending': currentStep < index,
        }"
        @click="onHorizontalStepClick(index)"
      >
        <div class="step-h-circle">
          <CheckOutlined v-if="currentStep > index" />
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="step-h-info">
          <div class="step-h-title">{{ step.title }}</div>
          <div v-if="step.description" class="step-h-desc">{{ step.description }}</div>
        </div>
        <div v-if="index < steps.length - 1" class="step-h-connector">
          <div
            class="connector-h-line"
            :class="{
              'connector-h-done': currentStep > index,
              'connector-h-active': currentStep === index,
            }"
          ></div>
        </div>
      </div>
    </div>

    <div class="flex-1 overflow-auto min-h-0 wizard-content">
      <slot :name="'step-' + currentStep" />
    </div>

    <div
      class="wizard-footer"
      :class="{
        'wizard-footer-horizontal': stepVariant === 'horizontal',
        'wizard-footer--start': footerAlign === 'start',
      }"
    >
      <Space>
        <Button v-if="!isFirstStep" :disabled="loading || submitting" @click="prevStep">
          <span class="inline-flex items-center gap-1">
            <LeftOutlined />
            上一步
          </span>
        </Button>
        <Button
          v-if="!isLastStep"
          type="primary"
          :disabled="loading || submitting"
          @click="handleNext"
        >
          <span class="inline-flex items-center gap-1">
            下一步
            <RightOutlined />
          </span>
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
.wizard-steps-ant {
  flex-shrink: 0;
}
.wizard-content {
  flex: 1;
  min-height: 0;
}
.wizard-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.wizard-footer--start {
  justify-content: flex-start;
}
.wizard-footer-horizontal {
  margin-top: 0;
  padding-top: 16px;
  box-shadow: 0 -6px 16px rgba(0, 0, 0, 0.06);
  background: #fff;
}

/* —— bgdata 横向步骤条 —— */
.wizard-steps-horizontal {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
  margin-bottom: 20px;
  padding: 0 4px;
  gap: 0;
}
.wizard-step-h {
  display: flex;
  align-items: center;
  flex: 1 1 0;
  min-width: 0;
  cursor: default;
}
.step-h-done {
  cursor: pointer;
}
.step-h-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  transition:
    transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.35s ease,
    background 0.25s ease,
    color 0.25s ease;
}
.step-h-info {
  margin-left: 8px;
  min-width: 0;
  flex: 0 1 auto;
  max-width: 11rem;
}
.step-h-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.25s ease;
}
.step-h-desc {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}
.step-h-connector {
  flex: 1 1 0;
  min-width: 8px;
  align-self: center;
  padding: 0 4px 0 8px;
  display: flex;
  align-items: center;
  height: 32px;
}
.connector-h-line {
  width: 100%;
  height: 3px;
  border-radius: 2px;
  background: #d9d9d9;
  transition: background 0.45s ease, box-shadow 0.45s ease;
  position: relative;
  overflow: hidden;
}
.connector-h-done {
  background: linear-gradient(90deg, #69b1ff 0%, #1677ff 50%, #0958d9 100%);
  background-size: 200% 100%;
  animation: plan-wizard-connector-flow 2.2s ease-in-out infinite;
}
.connector-h-active:not(.connector-h-done) {
  background: linear-gradient(90deg, #e6f4ff 0%, #1677ff 45%, #69b1ff 100%);
  background-size: 220% 100%;
  animation: plan-wizard-connector-flow 1.6s ease-in-out infinite;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.15);
}
.step-h-active .step-h-circle {
  background: #1677ff;
  color: #fff;
  box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.22);
  animation: plan-wizard-step-pulse 2s ease-in-out infinite;
}
.step-h-active .step-h-title {
  color: #1677ff;
}
.step-h-done .step-h-circle {
  background: #52c41a;
  color: #fff;
}
.step-h-pending .step-h-circle {
  background: #f0f0f0;
  color: #8c8c8c;
}

@keyframes plan-wizard-connector-flow {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes plan-wizard-step-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.18);
  }
  50% {
    box-shadow: 0 0 0 7px rgba(22, 119, 255, 0.12);
  }
}

@media (prefers-reduced-motion: reduce) {
  .connector-h-line {
    animation: none !important;
  }
  .connector-h-done {
    background: #1677ff;
  }
  .connector-h-active:not(.connector-h-done) {
    background: #91caff;
  }
  .step-h-active .step-h-circle {
    animation: none;
    box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.2);
  }
}
</style>
