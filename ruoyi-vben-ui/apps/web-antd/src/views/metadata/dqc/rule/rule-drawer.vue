<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import {
  Checkbox,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Tag,
  message,
} from 'ant-design-vue';
import {
  BulbOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  SyncOutlined,
  UserOutlined,
} from '@ant-design/icons-vue';

import type { Datasource } from '#/api/system/datasource/model';
import type { DqcRuleDef, DqcRuleTemplate } from '#/api/metadata/model';
import { datasourceEnabled, datasourceTables } from '#/api/system/datasource';
import { dqcTemplateInfo, dqcTemplateList } from '#/api/metadata/dqc/template';
import { dqcRuleAdd, dqcRuleInfo, dqcRuleUpdate } from '#/api/metadata/dqc/rule';

import WizardDrawer from '#/components/metadata/WizardDrawer.vue';

const dimensionOptions = [
  { label: '完整性', value: 'COMPLETENESS' },
  { label: '唯一性', value: 'UNIQUENESS' },
  { label: '准确性', value: 'ACCURACY' },
  { label: '一致性', value: 'CONSISTENCY' },
  { label: '及时性', value: 'TIMELINESS' },
  { label: '有效性', value: 'VALIDITY' },
];

const ruleTypeOptions = [
  { label: '空值检查', value: 'NULL_CHECK' },
  { label: '唯一性', value: 'UNIQUE' },
  { label: '正则匹配', value: 'REGEX' },
  { label: '阈值检查', value: 'THRESHOLD' },
  { label: '波动检查', value: 'FLUCTUATION' },
  { label: '跨字段', value: 'CROSS_FIELD' },
  { label: '自定义SQL', value: 'CUSTOM_SQL' },
  { label: '更新时效', value: 'UPDATE_TIMELINESS' },
];

const applyLevelOptions = [
  { label: '表级', value: 'TABLE' },
  { label: '字段级', value: 'COLUMN' },
  { label: '跨字段', value: 'CROSS_FIELD' },
  { label: '跨表', value: 'CROSS_TABLE' },
];

const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const steps = [
  { title: '基本信息' },
  { title: '配置规则' },
  { title: '质量维度与强度' },
  { title: '预览保存' },
];

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const currentStep = ref(0);
const submitting = ref(false);
const wizardRef = ref<InstanceType<typeof WizardDrawer>>();

const formValues = ref<Record<string, any>>({
  enabled: '0',
  ruleStrength: 'WEAK',
  errorLevel: 'MEDIUM',
  sortOrder: 0,
});

const datasourceList = ref<Datasource[]>([]);
const templateRows = ref<DqcRuleTemplate[]>([]);
const selectedTemplate = ref<DqcRuleTemplate | null>(null);
const tableOptions = ref<{ label: string; value: string }[]>([]);
const tablesLoading = ref(false);
/** 多选质量维度 */
const dimensionChecked = ref<string[]>([]);

const title = computed(() => (recordId.value ? '编辑规则' : '新建规则'));

const ruleTypeLabel = computed(
  () => ruleTypeOptions.find((o) => o.value === formValues.value.ruleType)?.label || formValues.value.ruleType || '-',
);

const applyLevelLabel = computed(
  () => applyLevelOptions.find((o) => o.value === formValues.value.applyLevel)?.label || '-',
);

const selectedDs = computed(() =>
  datasourceList.value.find((d) => d.dsId === formValues.value.targetDsId),
);

const templateSelectOptions = computed(() => {
  const rt = formValues.value.ruleType as string | undefined;
  const rows = rt
    ? templateRows.value.filter((t) => t.ruleType === rt)
    : templateRows.value;
  return rows.map((t) => ({
    label: `${t.templateName || ''}${t.ruleType ? ` (${ruleTypeOptions.find((o) => o.value === t.ruleType)?.label || t.ruleType})` : ''}`,
    value: t.id,
  }));
});

const exprPreview = computed(() => {
  let s = formValues.value.ruleExpr || '';
  const table = formValues.value.targetTable || '${table}';
  const col = formValues.value.targetColumn || '${column}';
  s = s.replaceAll('${table}', table).replaceAll('${column}', col);
  return s || '-';
});

const thresholdPreview = computed(() => {
  const a = formValues.value.thresholdMin;
  const b = formValues.value.thresholdMax;
  if (a == null && b == null) return '- ~ -';
  return `${a ?? '-'} ~ ${b ?? '-'}`;
});

const strengthLabel = computed(() =>
  formValues.value.ruleStrength === 'STRONG' ? '强规则' : '弱规则',
);

const errorLevelMeta: Record<string, { label: string; color: string }> = {
  LOW: { label: '低', color: 'green' },
  MEDIUM: { label: '中', color: 'orange' },
  HIGH: { label: '高', color: 'red' },
  CRITICAL: { label: '危急', color: 'purple' },
};

const errorLevelLabel = computed(
  () => errorLevelMeta[formValues.value.errorLevel || '']?.label || formValues.value.errorLevel || '-',
);

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  footer: false,
  onClosed: handleClosed,
  onCancel: () => drawerApi.close(),
  async onOpenChange(isOpen) {
    if (!isOpen) return;

    drawerApi.drawerLoading(true);
    try {
      const [dsList, tplRes] = await Promise.all([datasourceEnabled(), dqcTemplateList()]);
      datasourceList.value = dsList || [];
      templateRows.value = ((tplRes as any)?.rows || tplRes || []) as DqcRuleTemplate[];

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = (await dqcRuleInfo(id)) as DqcRuleDef;
        formValues.value = {
          enabled: '0',
          ruleStrength: 'WEAK',
          errorLevel: 'MEDIUM',
          sortOrder: 0,
          ...info,
        };
        dimensionChecked.value = (info.dimensions || '')
          .split(',')
          .map((x: string) => x.trim())
          .filter(Boolean);
        if (info.templateId) {
          try {
            selectedTemplate.value = await dqcTemplateInfo(info.templateId);
          } catch {
            selectedTemplate.value = null;
          }
        }
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          ruleStrength: 'WEAK',
          errorLevel: 'MEDIUM',
          sortOrder: 0,
        };
        dimensionChecked.value = [];
        selectedTemplate.value = null;
      }
      currentStep.value = 0;
      await loadTablesForDs(formValues.value.targetDsId);
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

watch(
  () => formValues.value.targetDsId,
  (dsId) => {
    loadTablesForDs(dsId);
  },
);

watch(dimensionChecked, (arr) => {
  formValues.value.dimensions = arr.length ? arr.join(',') : undefined;
});

async function loadTablesForDs(dsId?: number) {
  tableOptions.value = [];
  if (!dsId) return;
  tablesLoading.value = true;
  try {
    const names = await datasourceTables(dsId);
    tableOptions.value = (names || []).map((n) => ({ label: n, value: n }));
  } catch {
    tableOptions.value = [];
  } finally {
    tablesLoading.value = false;
  }
}

async function onRuleTypeChange() {
  formValues.value.templateId = undefined;
  selectedTemplate.value = null;
}

async function onTemplatePick(tplId?: number | null) {
  if (tplId == null) {
    selectedTemplate.value = null;
    return;
  }
  try {
    const t = await dqcTemplateInfo(tplId);
    selectedTemplate.value = t;
    if (t.ruleType) formValues.value.ruleType = t.ruleType;
    if (t.applyLevel) formValues.value.applyLevel = t.applyLevel;
    if (t.defaultExpr) formValues.value.ruleExpr = t.defaultExpr;
    if (t.dimension && dimensionChecked.value.length === 0) {
      dimensionChecked.value = [t.dimension];
    }
  } catch {
    selectedTemplate.value = null;
  }
}

function autoGenRuleCode() {
  const raw = (formValues.value.ruleName || 'RULE').replace(/\s+/g, '_');
  const suffix = Math.random().toString(36).substring(2, 10).toUpperCase();
  formValues.value.ruleCode = `RULE_${suffix}`;
}

async function handleSubmit() {
  if (!validateStep(3)) return;
  drawerApi.lock(true);
  submitting.value = true;
  try {
    const payload = { ...formValues.value };
    if (recordId.value) {
      await dqcRuleUpdate({ id: recordId.value, ...payload });
    } else {
      await dqcRuleAdd(payload);
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
  formValues.value = { enabled: '0', ruleStrength: 'WEAK', errorLevel: 'MEDIUM', sortOrder: 0 };
  dimensionChecked.value = [];
  selectedTemplate.value = null;
  currentStep.value = 0;
}

function validateStep(step: number): boolean {
  const f = formValues.value;
  if (step === 0) {
    if (!f.ruleName?.trim()) {
      message.warning('请填写规则名称');
      return false;
    }
    if (!f.ruleType) {
      message.warning('请选择规则类型');
      return false;
    }
    if (!f.ruleCode?.trim()) {
      message.warning('请填写或自动生成规则编码');
      return false;
    }
    if (!f.templateId) {
      message.warning('请选择关联模板');
      return false;
    }
    if (!f.applyLevel) {
      message.warning('请选择适用级别');
      return false;
    }
    if (!f.targetDsId) {
      message.warning('请选择目标数据源');
      return false;
    }
  }
  if (step === 1) {
    if (!f.targetTable?.trim()) {
      message.warning('请选择或填写目标表名');
      return false;
    }
    if (!f.ruleExpr?.trim()) {
      message.warning('请填写规则表达式');
      return false;
    }
  }
  if (step === 2) {
    if (!dimensionChecked.value.length) {
      message.warning('请至少选择一个质量维度');
      return false;
    }
    if (!f.errorLevel) {
      message.warning('请选择错误级别');
      return false;
    }
    if (!f.ruleStrength) {
      message.warning('请选择规则强度');
      return false;
    }
  }
  return true;
}

function handleWizardNext() {
  if (!validateStep(currentStep.value)) return;
  wizardRef.value?.nextStep();
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[820px]" :loading="submitting">
    <WizardDrawer
      ref="wizardRef"
      step-variant="horizontal"
      footer-align="start"
      :steps="steps"
      v-model:current-step="currentStep"
      :loading="submitting"
      @finish="handleSubmit"
      @next="handleWizardNext"
    >
      <!-- Step 0 基本信息 -->
      <template #step-0>
        <Form
          :model="formValues"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          label-align="right"
          class="rule-wizard-form"
        >
          <Form.Item label="规则名称" required>
            <Input v-model:value="formValues.ruleName" placeholder="请输入规则名称" />
          </Form.Item>
          <Form.Item label="规则类型" required>
            <Select
              v-model:value="formValues.ruleType"
              :options="ruleTypeOptions"
              placeholder="请选择规则类型"
              allow-clear
              @change="onRuleTypeChange"
            />
          </Form.Item>
          <Form.Item label="规则编码" required>
            <div class="flex gap-2 items-center">
              <Input
                v-model:value="formValues.ruleCode"
                class="flex-1"
                placeholder="请输入规则编码"
              />
              <a class="text-primary whitespace-nowrap flex items-center gap-1" @click="autoGenRuleCode">
                <SyncOutlined />
                自动生成
              </a>
            </div>
          </Form.Item>
          <Form.Item label="关联模板">
            <Select
              v-model:value="formValues.templateId"
              :options="templateSelectOptions"
              placeholder="请选择关联模板"
              allow-clear
              show-search
              option-filter-prop="label"
              @change="(v: any) => onTemplatePick(v)"
            />
          </Form.Item>
          <Form.Item label="适用级别" required>
            <Select
              v-model:value="formValues.applyLevel"
              :options="applyLevelOptions"
              placeholder="请选择适用级别"
            />
          </Form.Item>
          <Form.Item label="目标数据源" required>
            <Select
              v-model:value="formValues.targetDsId"
              :options="
                datasourceList.map((d) => ({
                  label: [d.dsName, d.dsType, d.dataLayer].filter(Boolean).join(' · '),
                  value: d.dsId,
                }))
              "
              placeholder="请选择目标数据源"
              show-search
              option-filter-prop="label"
            />
            <div v-if="selectedDs" class="mt-2 flex flex-wrap items-center gap-2 text-sm">
              <Tag v-if="selectedDs.dsType" color="blue">{{ selectedDs.dsType }}</Tag>
              <span>{{ selectedDs.dsName }}</span>
              <Tag v-if="selectedDs.dataLayer">{{ selectedDs.dataLayer }}</Tag>
            </div>
            <div v-if="selectedDs?.dataLayer" class="text-gray-400 text-xs mt-1">
              数仓层标签由所选数据源自动带出: {{ selectedDs.dataLayer }}
            </div>
          </Form.Item>
        </Form>

        <div
          v-if="selectedTemplate"
          class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50/80 p-4 text-sm"
        >
          <div class="font-medium text-emerald-800 mb-2 flex items-center gap-1">
            <InfoCircleOutlined />
            模板说明
          </div>
          <div class="space-y-2 text-gray-700">
            <div>
              <span class="text-gray-500">模板类型：</span>
              <Tag color="magenta">{{ ruleTypeLabel }}</Tag>
            </div>
            <div>
              <span class="text-gray-500">质量维度：</span>
              <Tag color="cyan">
                {{
                  dimensionOptions.find((o) => o.value === selectedTemplate.dimension)?.label ||
                  selectedTemplate.dimension ||
                  '-'
                }}
              </Tag>
            </div>
            <div>
              <span class="text-gray-500">模板描述：</span>
              {{ selectedTemplate.templateDesc || '-' }}
            </div>
            <div v-if="selectedTemplate.defaultExpr">
              <span class="text-gray-500">默认表达式：</span>
              <pre
                class="mt-1 rounded bg-neutral-900 text-amber-100 p-3 text-xs overflow-x-auto"
              >{{ selectedTemplate.defaultExpr }}</pre>
            </div>
          </div>
        </div>
      </template>

      <!-- Step 1 配置规则 -->
      <template #step-1>
        <Form :model="formValues" layout="vertical">
          <Form.Item label="目标表名" required>
            <Select
              v-model:value="formValues.targetTable"
              :options="tableOptions"
              :loading="tablesLoading"
              placeholder="请选择目标表"
              show-search
              allow-clear
              option-filter-prop="label"
            />
            <div class="text-gray-400 text-xs mt-1">若下拉无数据，请确认已选数据源且元数据已同步</div>
          </Form.Item>
          <Form.Item label="目标列名">
            <Input v-model:value="formValues.targetColumn" placeholder="字段级规则时填写，表级可留空" />
          </Form.Item>
          <Form.Item label="规则表达式" required>
            <Input.TextArea
              v-model:value="formValues.ruleExpr"
              placeholder="支持 ${table}、${column} 等占位符"
              :rows="5"
            />
            <div class="text-gray-400 text-xs mt-1">
              支持变量: ${table}、${column}、${column_a}、${column_b}、${source_table}、${target_table}、${threshold}
            </div>
          </Form.Item>
          <Form.Item label="阈值配置">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <div class="text-gray-600 text-sm mb-1">最小值</div>
                <InputNumber
                  v-model:value="formValues.thresholdMin"
                  class="w-full"
                  placeholder="阈值最小值"
                />
              </div>
              <div>
                <div class="text-gray-600 text-sm mb-1">最大值</div>
                <InputNumber
                  v-model:value="formValues.thresholdMax"
                  class="w-full"
                  placeholder="阈值最大值"
                />
              </div>
            </div>
          </Form.Item>
          <Form.Item label="波动阈值(%)">
            <div class="flex items-center gap-2 max-w-md">
              <span class="text-gray-500 select-none">%</span>
              <InputNumber
                v-model:value="formValues.fluctuationThreshold"
                class="flex-1"
                :min="0"
                :max="100"
                placeholder="百分比"
              />
            </div>
          </Form.Item>
        </Form>

        <div class="mt-4">
          <div class="text-emerald-600 font-medium mb-2 flex items-center gap-1">
            <EyeOutlined />
            表达式预览
          </div>
          <pre
            class="rounded-lg bg-neutral-900 text-amber-100 p-4 text-sm overflow-x-auto min-h-[3rem]"
          >{{ exprPreview }}</pre>
        </div>
      </template>

      <!-- Step 2 质量维度与强度 -->
      <template #step-2>
        <Form :model="formValues" layout="vertical">
          <Form.Item label="质量维度" required>
            <Checkbox.Group v-model:value="dimensionChecked" class="w-full">
              <div class="grid grid-cols-3 gap-3">
                <Checkbox v-for="opt in dimensionOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </Checkbox>
              </div>
            </Checkbox.Group>
            <div class="text-gray-400 text-xs mt-1">至少选择一个质量维度</div>
          </Form.Item>

          <div class="text-center font-semibold text-gray-800 my-4">规则强度与告警</div>

          <Form.Item label="规则强度">
            <Radio.Group v-model:value="formValues.ruleStrength" class="w-full">
              <div class="flex flex-col gap-3">
                <Radio value="STRONG" class="!items-start rule-radio-strong">
                  <div>
                    <span class="text-red-600 font-medium">强规则</span>
                    <div class="text-gray-500 text-xs mt-0.5">
                      失败时阻塞任务执行 + 立即告警
                    </div>
                  </div>
                </Radio>
                <Radio value="WEAK" class="!items-start rule-radio-weak">
                  <div>
                    <span class="text-orange-600 font-medium">弱规则</span>
                    <div class="text-gray-500 text-xs mt-0.5">失败时仅记录，不阻塞任务</div>
                  </div>
                </Radio>
              </div>
            </Radio.Group>
          </Form.Item>

          <Form.Item label="错误级别" required>
            <Radio.Group v-model:value="formValues.errorLevel" class="w-full">
              <div class="grid grid-cols-2 gap-3">
                <Radio value="LOW" class="!items-start">
                  <div>
                    <Tag color="green">低</Tag>
                    <span class="text-gray-500 text-xs ml-1">影响轻微</span>
                  </div>
                </Radio>
                <Radio value="MEDIUM" class="!items-start">
                  <div>
                    <Tag color="orange">中</Tag>
                    <span class="text-gray-500 text-xs ml-1">影响一般</span>
                  </div>
                </Radio>
                <Radio value="HIGH" class="!items-start">
                  <div>
                    <Tag color="red">高</Tag>
                    <span class="text-gray-500 text-xs ml-1">影响严重</span>
                  </div>
                </Radio>
                <Radio value="CRITICAL" class="!items-start">
                  <div>
                    <Tag color="purple">危急</Tag>
                    <span class="text-gray-500 text-xs ml-1">核心功能受损</span>
                  </div>
                </Radio>
              </div>
            </Radio.Group>
          </Form.Item>

          <Form.Item label="告警接收人">
            <Input v-model:value="formValues.alertReceivers" placeholder="输入接收人，多人以逗号分隔">
              <template #prefix>
                <UserOutlined class="text-gray-400" />
              </template>
            </Input>
            <div class="text-gray-400 text-xs mt-1">留空则使用系统默认告警配置</div>
          </Form.Item>

          <Form.Item label="排序权重">
            <InputNumber v-model:value="formValues.sortOrder" class="w-full max-w-xs" :min="0" />
            <div class="text-gray-400 text-xs mt-1">数值越小排序越靠前</div>
          </Form.Item>

          <Form.Item label="状态">
            <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
          </Form.Item>
        </Form>

        <div class="mt-4 rounded-lg border border-sky-200 bg-sky-50/90 p-4 text-sm">
          <div class="font-medium text-sky-900 mb-3 flex items-center gap-1">
            <BulbOutlined />
            规则强度说明
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div class="rounded border border-red-100 overflow-hidden bg-white">
              <div class="bg-red-50 text-red-700 font-medium px-3 py-2 text-xs">强规则</div>
              <ul class="list-disc list-inside p-3 text-gray-600 text-xs space-y-1">
                <li>失败时立即阻塞下游任务执行</li>
                <li>触发告警通知</li>
                <li>影响数据推送和数据应用</li>
                <li>适用于：主键唯一性、重要字段非空</li>
              </ul>
            </div>
            <div class="rounded border border-orange-100 overflow-hidden bg-white">
              <div class="bg-orange-50 text-orange-700 font-medium px-3 py-2 text-xs">弱规则</div>
              <ul class="list-disc list-inside p-3 text-gray-600 text-xs space-y-1">
                <li>失败时仅记录日志和告警</li>
                <li>不阻塞任务执行</li>
                <li>纳入质量评分统计</li>
                <li>适用于：数据分布检查、趋势监测</li>
              </ul>
            </div>
          </div>
        </div>
      </template>

      <!-- Step 3 预览保存 -->
      <template #step-3>
        <div class="text-base font-semibold text-gray-800 mb-4">规则配置总览</div>
        <Descriptions bordered :column="1" size="small" class="rule-preview-desc">
          <Descriptions.Item label="规则名称">{{ formValues.ruleName || '-' }}</Descriptions.Item>
          <Descriptions.Item label="规则编码">{{ formValues.ruleCode || '-' }}</Descriptions.Item>
          <Descriptions.Item label="规则类型">
            <Tag color="magenta">{{ ruleTypeLabel }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="关联模板">
            {{
              templateRows.find((t) => t.id === formValues.templateId)?.templateName ||
              selectedTemplate?.templateName ||
              '-'
            }}
          </Descriptions.Item>
          <Descriptions.Item label="适用级别">
            <Tag>{{ applyLevelLabel }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="目标数据源">
            {{ selectedDs?.dsName || '-' }}
          </Descriptions.Item>
          <Descriptions.Item label="数仓层标签">
            <Tag v-if="selectedDs?.dataLayer" color="blue">{{ selectedDs.dataLayer }}</Tag>
            <span v-else>-</span>
          </Descriptions.Item>
          <Descriptions.Item label="目标表">{{ formValues.targetTable || '-' }}</Descriptions.Item>
          <Descriptions.Item label="目标列">{{ formValues.targetColumn || '-' }}</Descriptions.Item>
          <Descriptions.Item label="规则表达式">
            <pre class="bg-neutral-100 rounded p-2 text-xs overflow-x-auto m-0">{{
              formValues.ruleExpr || '-'
            }}</pre>
          </Descriptions.Item>
          <Descriptions.Item label="阈值范围">{{ thresholdPreview }}</Descriptions.Item>
          <Descriptions.Item label="质量维度">
            <template v-if="dimensionChecked.length">
              <Tag v-for="d in dimensionChecked" :key="d" color="cyan" class="mb-1 mr-1">
                {{ dimensionOptions.find((o) => o.value === d)?.label || d }}
              </Tag>
            </template>
            <span v-else>-</span>
          </Descriptions.Item>
          <Descriptions.Item label="规则强度">
            <Tag color="orange">{{ strengthLabel }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="错误级别">
            <Tag :color="errorLevelMeta[formValues.errorLevel || '']?.color || 'default'">
              {{ errorLevelLabel }}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="波动阈值">
            {{ formValues.fluctuationThreshold ?? '-' }}
          </Descriptions.Item>
          <Descriptions.Item label="告警接收人">{{ formValues.alertReceivers || '-' }}</Descriptions.Item>
          <Descriptions.Item label="排序权重">{{ formValues.sortOrder ?? 0 }}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {{ enabledOptions.find((o) => o.value === formValues.enabled)?.label || '-' }}
          </Descriptions.Item>
        </Descriptions>
      </template>
    </WizardDrawer>
  </BasicDrawer>
</template>

<style scoped>
.rule-wizard-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}
.rule-preview-desc :deep(.ant-descriptions-item-label) {
  width: 140px;
  font-weight: 500;
}
</style>
