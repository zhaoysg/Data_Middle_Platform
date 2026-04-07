<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Select, message } from 'ant-design-vue';

import { datasourceEnabled } from '#/api/system/datasource';
import { dqcTemplateList } from '#/api/metadata/dqc/template';
import { dqcRuleAdd, dqcRuleInfo, dqcRuleUpdate } from '#/api/metadata/dqc/rule';

import WizardDrawer from '#/components/metadata/WizardDrawer.vue';

const dimensionOptions = [
  { label: '完整性', value: 'COMPLETENESS' },
  { label: '唯一性', value: 'UNIQUENESS' },
  { label: '准确性', value: 'ACCURACY' },
  { label: '一致性', value: 'CONSISTENCY' },
  { label: '时效性', value: 'TIMELINESS' },
  { label: '有效性', value: 'VALIDITY' },
  { label: '自定义', value: 'CUSTOM' },
];

const errorLevelOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];

const ruleStrengthOptions = [
  { label: '强约束', value: 'STRONG' },
  { label: '弱约束', value: 'WEAK' },
];

const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const steps = [
  { title: '基本信息' },
  { title: '目标配置' },
  { title: '参数与强度' },
];

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const currentStep = ref(0);
const submitting = ref(false);
const wizardRef = ref<InstanceType<typeof WizardDrawer>>();

const formValues = ref<Record<string, any>>({
  enabled: '0',
  builtin: '0',
});

const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const templateOptions = ref<{ label: string; value: number }[]>([]);

const title = computed(() => {
  return recordId.value ? '编辑规则' : '新增规则';
});

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  footer: false,
  onClosed: handleClosed,
  onCancel: () => drawerApi.close(),
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }

    drawerApi.drawerLoading(true);
    try {
      const [dsList, templateList] = await Promise.all([
        datasourceEnabled(),
        dqcTemplateList(),
      ]);
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: item.dsName,
        value: item.dsId,
      }));
      templateOptions.value = ((templateList as any)?.rows || []).map((item: any) => ({
        label: item.templateName,
        value: item.id,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcRuleInfo(id);
        formValues.value = { ...info };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          builtin: '0',
        };
      }
      currentStep.value = 0;
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

async function handleSubmit() {
  if (!validateStep(currentStep.value)) {
    return;
  }
  drawerApi.lock(true);
  submitting.value = true;
  try {
    if (recordId.value) {
      await dqcRuleUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await dqcRuleAdd(formValues.value);
    }
    message.success('保存成功');
    emit('reload');
    drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    drawerApi.lock(false);
    submitting.value = false;
  }
}

function handleClosed() {
  recordId.value = undefined;
  formValues.value = { enabled: '0', builtin: '0' };
  currentStep.value = 0;
}

const step0Valid = computed(() => {
  const f = formValues.value;
  return f.ruleName && f.ruleCode && f.templateId;
});

const step1Valid = computed(() => {
  const f = formValues.value;
  return f.targetDsId && f.targetTable;
});

function validateStep(step: number): boolean {
  if (step === 0) {
    if (!step0Valid.value) {
      message.warning('请完善基本信息（规则名称、规则编码、关联模板）');
      return false;
    }
  }
  if (step === 1) {
    if (!step1Valid.value) {
      message.warning('请完善目标配置（数据源、目标表）');
      return false;
    }
  }
  return true;
}

function handleWizardNext() {
  if (!validateStep(currentStep.value)) return;
  wizardRef.value?.nextStep();
}

const dimensionLabel = computed(
  () => dimensionOptions.find((o) => o.value === formValues.value.dimension)?.label || '-',
);
const datasourceLabel = computed(
  () => datasourceOptions.value.find((o) => o.value === formValues.value.targetDsId)?.label || '-',
);
const templateLabel = computed(
  () => templateOptions.value.find((o) => o.value === formValues.value.templateId)?.label || '-',
);
const enabledLabel = computed(
  () => enabledOptions.find((o) => o.value === formValues.value.enabled)?.label || '-',
);
const errorLevelLabel = computed(
  () => errorLevelOptions.find((o) => o.value === formValues.value.errorLevel)?.label || '-',
);
const strengthLabel = computed(
  () => ruleStrengthOptions.find((o) => o.value === formValues.value.ruleStrength)?.label || '-',
);
</script>

<template>
  <BasicDrawer :title="title" class="w-[720px]" :loading="submitting">
    <WizardDrawer
      ref="wizardRef"
      :steps="steps"
      v-model:currentStep="currentStep"
      :loading="submitting"
      @finish="handleSubmit"
      @next="handleWizardNext"
    >
      <!-- Step 0: 基本信息 -->
      <template #step-0>
        <Form :model="formValues" layout="vertical">
          <Form.Item label="规则名称" required>
            <Input v-model:value="formValues.ruleName" placeholder="请输入规则名称" />
          </Form.Item>
          <Form.Item label="规则编码" required>
            <Input v-model:value="formValues.ruleCode" placeholder="请输入规则编码" />
          </Form.Item>
          <Form.Item label="规则描述">
            <Input.TextArea
              v-model:value="formValues.ruleDesc"
              placeholder="请输入规则描述"
              :rows="3"
            />
          </Form.Item>
          <Form.Item label="关联模板" required>
            <Select
              v-model:value="formValues.templateId"
              :options="templateOptions"
              placeholder="请选择关联模板，选择后将自动填充维度等信息"
            />
          </Form.Item>
          <Form.Item label="质量维度">
            <Select
              v-model:value="formValues.dimension"
              :options="dimensionOptions"
              placeholder="请选择质量维度"
            />
          </Form.Item>
        </Form>
      </template>

      <!-- Step 1: 目标配置 -->
      <template #step-1>
        <div class="space-y-4">
          <div
            class="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-700 leading-relaxed"
          >
            <p class="font-medium mb-1">配置规则质检的目标对象：</p>
            <ul class="list-disc list-inside space-y-1">
              <li><code class="bg-blue-100 px-1 rounded">目标表</code> — 质检规则应用于哪张表</li>
              <li><code class="bg-blue-100 px-1 rounded">目标字段</code> — 可选，指定特定字段（如留空则对该表全字段执行）</li>
            </ul>
          </div>
          <Form.Item label="数据源" required>
            <Select
              v-model:value="formValues.targetDsId"
              :options="datasourceOptions"
              placeholder="请选择数据源"
            />
          </Form.Item>
          <Form.Item label="目标表" required>
            <Input v-model:value="formValues.targetTable" placeholder="请输入目标表名，如: dim_user" />
          </Form.Item>
          <Form.Item label="目标字段">
            <Input
              v-model:value="formValues.targetColumn"
              placeholder="请输入目标字段名，留空则对该表所有字段执行"
            />
          </Form.Item>
        </div>
      </template>

      <!-- Step 2: 参数与强度 -->
      <template #step-2>
        <div class="space-y-4">
          <Form.Item label="规则参数">
            <Input.TextArea
              v-model:value="formValues.ruleParams"
              placeholder='请输入规则参数JSON，如: {"maxRate": 0.05, "minValue": 0}'
              :rows="3"
            />
          </Form.Item>
          <div class="grid grid-cols-2 gap-4">
            <Form.Item label="错误级别">
              <Select
                v-model:value="formValues.errorLevel"
                :options="errorLevelOptions"
                placeholder="请选择错误级别"
              />
            </Form.Item>
            <Form.Item label="规则强度">
              <Select
                v-model:value="formValues.ruleStrength"
                :options="ruleStrengthOptions"
                placeholder="请选择规则强度"
              />
            </Form.Item>
          </div>
          <Form.Item label="状态">
            <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
          </Form.Item>

          <!-- Preview -->
          <div class="mt-6 border-t pt-4">
            <div class="text-sm font-medium text-gray-600 mb-3">配置总览</div>
            <div class="grid grid-cols-2 gap-y-2 text-sm bg-gray-50 rounded-lg p-4">
              <div><span class="text-gray-500">规则名称：</span>{{ formValues.ruleName || '-' }}</div>
              <div><span class="text-gray-500">规则编码：</span>{{ formValues.ruleCode || '-' }}</div>
              <div><span class="text-gray-500">关联模板：</span>{{ templateLabel }}</div>
              <div><span class="text-gray-500">质量维度：</span>{{ dimensionLabel }}</div>
              <div><span class="text-gray-500">数据源：</span>{{ datasourceLabel }}</div>
              <div><span class="text-gray-500">目标表：</span>{{ formValues.targetTable || '-' }}</div>
              <div><span class="text-gray-500">目标字段：</span>{{ formValues.targetColumn || '-' }}</div>
              <div><span class="text-gray-500">错误级别：</span>{{ errorLevelLabel }}</div>
              <div><span class="text-gray-500">规则强度：</span>{{ strengthLabel }}</div>
              <div><span class="text-gray-500">状态：</span>{{ enabledLabel }}</div>
            </div>
          </div>
        </div>
      </template>
    </WizardDrawer>
  </BasicDrawer>
</template>
