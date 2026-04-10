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
  Switch,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import {
  CheckCircleOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  QuestionCircleOutlined,
  SyncOutlined,
  UserOutlined,
} from '@ant-design/icons-vue';

import type { Datasource } from '#/api/system/datasource/model';
import type { DqcRuleDef, DqcRuleTemplate, MetadataTable, MetadataColumn } from '#/api/metadata/model';
import { datasourceEnabled } from '#/api/system/datasource';
import { dqcTemplateInfo, dqcTemplateList } from '#/api/metadata/dqc/template';
import {
  dqcRuleAdd,
  dqcRuleInfo,
  dqcRuleUpdate,
  dqcGetTableColumns,
} from '#/api/metadata/dqc/rule';
import { metadataTableList } from '#/api/metadata/table';

import WizardDrawer from '#/components/metadata/WizardDrawer.vue';

const dimensionOptions = [
  { label: '完整性', value: 'COMPLETENESS' },
  { label: '唯一性', value: 'UNIQUENESS' },
  { label: '准确性', value: 'ACCURACY' },
  { label: '一致性', value: 'CONSISTENCY' },
  { label: '及时性', value: 'TIMELINESS' },
  { label: '有效性', value: 'VALIDITY' },
];

const strongRuleHelpItems = [
  '失败时立即阻塞下游任务执行',
  '触发告警通知',
  '影响数据推送和数据应用',
  '适用于：主键唯一性、重要字段非空',
];

const weakRuleHelpItems = [
  '失败时仅记录日志和告警',
  '不阻塞任务执行',
  '纳入质量评分统计',
  '适用于：数据分布检查、趋势监测',
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
/** 多选质量维度 */
const dimensionChecked = ref<string[]>([]);

/** 元数据表下拉选项（从元数据系统选择） */
const metadataTableOptions = ref<{ label: string; value: number; record: MetadataTable }[]>([]);
const metadataTableLoading = ref(false);
const selectedMetadataTable = ref<MetadataTable | null>(null);

/** 元数据字段下拉选项 */
const metadataColumnOptions = ref<{ label: string; value: number; record: MetadataColumn }[]>([]);
const metadataColumnLoading = ref(false);
const selectedMetadataColumn = ref<MetadataColumn | null>(null);

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

/** 状态：Switch 与后端 0=启用 / 1=停用 互转 */
const enabledSwitch = computed({
  get: () => formValues.value.enabled === '0',
  set: (v: boolean) => {
    formValues.value.enabled = v ? '0' : '1';
  },
});

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

      // 加载元数据表列表
      await loadMetadataTables();

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
        // 如果有元数据表ID，加载对应的字段列表
        if (info.tableId) {
          await onMetadataTableChange(info.tableId);
          if (info.columnId) {
            onMetadataColumnChange(info.columnId);
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
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

watch(dimensionChecked, (arr) => {
  formValues.value.dimensions = arr.length ? arr.join(',') : undefined;
});

/** 加载元数据表列表（用于从元数据选择） */
async function loadMetadataTables() {
  metadataTableLoading.value = true;
  metadataTableOptions.value = [];
  try {
    const res = await metadataTableList({ status: 'ACTIVE', pageNum: 1, pageSize: 500 }) as any;
    const rows = res?.rows || res || [];
    metadataTableOptions.value = rows.map((t: MetadataTable) => ({
      label: `${t.tableAlias || t.tableName}${t.tableComment ? ` (${t.tableComment})` : ''}`,
      value: t.id as number,
      record: t,
    }));
  } catch {
    metadataTableOptions.value = [];
  } finally {
    metadataTableLoading.value = false;
  }
}

/** 当元数据表选择变化时，加载该表的字段列表 */
async function onMetadataTableChange(tableId: number | undefined) {
  if (!tableId) {
    selectedMetadataTable.value = null;
    metadataColumnOptions.value = [];
    selectedMetadataColumn.value = null;
    // 清除关联的字段值
    formValues.value.columnId = undefined;
    formValues.value.targetColumn = undefined;
    formValues.value.tableId = undefined;
    return;
  }

  const option = metadataTableOptions.value.find((o) => o.value === tableId);
  selectedMetadataTable.value = option?.record || null;
  formValues.value.tableId = tableId;
  formValues.value.targetTable = selectedMetadataTable.value?.tableName;

  // 自动获取数据源ID（如果尚未选择）
  if (!formValues.value.targetDsId && selectedMetadataTable.value?.dsId) {
    formValues.value.targetDsId = selectedMetadataTable.value.dsId;
  }

  // 加载字段列表
  metadataColumnLoading.value = true;
  metadataColumnOptions.value = [];
  selectedMetadataColumn.value = null;
  formValues.value.columnId = undefined;
  formValues.value.targetColumn = undefined;
  try {
    const columns = (await dqcGetTableColumns(tableId)) as MetadataColumn[] || [];
    metadataColumnOptions.value = columns.map((c: MetadataColumn) => ({
      label: `${c.columnAlias || c.columnName}${c.columnComment ? ` - ${c.columnComment}` : ''}`,
      value: c.id as number,
      record: c,
    }));
  } catch {
    metadataColumnOptions.value = [];
  } finally {
    metadataColumnLoading.value = false;
  }
}

/** 当元数据字段选择变化时 */
function onMetadataColumnChange(columnId: number | undefined) {
  if (!columnId) {
    selectedMetadataColumn.value = null;
    formValues.value.targetColumn = undefined;
    return;
  }
  const option = metadataColumnOptions.value.find((o) => o.value === columnId);
  selectedMetadataColumn.value = option?.record || null;
  formValues.value.columnId = columnId;
  formValues.value.targetColumn = selectedMetadataColumn.value?.columnName;
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
  }
  if (step === 1) {
    if (!formValues.value.tableId) {
      message.warning('请从元数据中选择目标表，用于表达式预览与保存时的执行绑定信息');
      return false;
    }
    if (!formValues.value.targetDsId) {
      message.warning('无法确定数据源，请重新选择目标表或先在基本信息中选择目标数据源');
      return false;
    }
    if (formValues.value.applyLevel === 'COLUMN' && !formValues.value.columnId) {
      message.warning('字段级规则请从元数据中选择目标字段');
      return false;
    }
    if (!formValues.value.ruleExpr?.trim()) {
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
          <Form.Item label="目标数据源">
            <Select
              v-model:value="formValues.targetDsId"
              :options="
                datasourceList.map((d) => ({
                  label: [d.dsName, d.dsType, d.dataLayer].filter(Boolean).join(' · '),
                  value: d.dsId,
                }))
              "
              placeholder="可选；下一步从元数据选表后会自动带出对应数据源"
              allow-clear
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
          <div class="mb-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
            <div class="text-blue-700 text-sm">
              <InfoCircleOutlined class="mr-1" />
              <strong>说明：</strong>请从元数据选择表/字段，用于表达式中的
              <code>${table}</code>、<code>${column}</code> 替换预览，并与保存后的规则执行解析一致（非列表展示字段）。
            </div>
          </div>

          <Form.Item label="目标表（元数据选择）" required>
            <Select
              v-model:value="formValues.tableId"
              :options="metadataTableOptions"
              :loading="metadataTableLoading"
              placeholder="请从元数据中选择目标表"
              show-search
              allow-clear
              option-filter-prop="label"
              @change="(v: any) => onMetadataTableChange(v)"
            >
              <template #suffixIcon>
                <SyncOutlined v-if="metadataTableLoading" spin />
              </template>
            </Select>
            <div v-if="selectedMetadataTable" class="mt-2 text-xs text-gray-500">
              <Tag color="blue">{{ selectedMetadataTable.tableName }}</Tag>
              <span v-if="selectedMetadataTable.tableAlias">{{ selectedMetadataTable.tableAlias }}</span>
              <span v-if="selectedMetadataTable.dsName" class="ml-2 text-gray-400">
                · {{ selectedMetadataTable.dsName }}
              </span>
            </div>
          </Form.Item>

          <Form.Item
            v-if="metadataColumnOptions.length > 0 || selectedMetadataTable"
            label="目标字段（元数据选择）"
          >
            <Select
              v-model:value="formValues.columnId"
              :options="metadataColumnOptions"
              :loading="metadataColumnLoading"
              placeholder="请从元数据中选择目标字段（字段级规则必填）"
              show-search
              allow-clear
              option-filter-prop="label"
              @change="(v: any) => onMetadataColumnChange(v)"
            >
              <template #suffixIcon>
                <SyncOutlined v-if="metadataColumnLoading" spin />
              </template>
            </Select>
            <div v-if="selectedMetadataColumn" class="mt-2 text-xs text-gray-500">
              <Tag color="cyan">{{ selectedMetadataColumn.columnName }}</Tag>
              <span v-if="selectedMetadataColumn.columnAlias">{{ selectedMetadataColumn.columnAlias }}</span>
              <span v-if="selectedMetadataColumn.dataType" class="ml-2 text-gray-400">
                · {{ selectedMetadataColumn.dataType }}
              </span>
            </div>
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
        <Form :model="formValues" layout="vertical" class="dqc-rule-step3-form">
          <Form.Item label="质量维度" required>
            <div class="dqc-dimension-panel">
              <Checkbox.Group v-model:value="dimensionChecked" class="w-full">
                <div class="dqc-dimension-grid">
                  <div
                    v-for="opt in dimensionOptions"
                    :key="opt.value"
                    class="dqc-dimension-cell"
                    :class="{
                      'dqc-dimension-cell--checked': dimensionChecked.includes(opt.value),
                    }"
                  >
                    <Checkbox :value="opt.value" class="dqc-dimension-checkbox">
                      {{ opt.label }}
                    </Checkbox>
                  </div>
                </div>
              </Checkbox.Group>
              <div class="dqc-dimension-hint">至少选择一个质量维度</div>
            </div>
          </Form.Item>

          <div class="dqc-rule-strength-panel">
            <div class="dqc-rule-strength-panel-title">规则强度与告警</div>

          <Form.Item label="规则强度">
            <Radio.Group v-model:value="formValues.ruleStrength" class="w-full">
              <div class="flex flex-col gap-3">
                <Radio value="STRONG" class="!items-start rule-radio-strong">
                  <div>
                    <span class="text-red-600 font-medium inline-flex items-center gap-0.5">
                      强规则
                      <Tooltip
                        placement="topLeft"
                        :overlay-inner-style="{ maxWidth: '300px' }"
                      >
                        <template #title>
                          <ul class="rule-strength-tooltip-list">
                            <li v-for="(line, i) in strongRuleHelpItems" :key="i">{{ line }}</li>
                          </ul>
                        </template>
                        <span
                          class="rule-strength-help-trigger"
                          tabindex="0"
                          @click.stop.prevent
                        >
                          <QuestionCircleOutlined />
                        </span>
                      </Tooltip>
                    </span>
                    <div class="text-gray-500 text-xs mt-0.5">
                      失败时阻塞任务执行 + 立即告警
                    </div>
                  </div>
                </Radio>
                <Radio value="WEAK" class="!items-start rule-radio-weak">
                  <div>
                    <span class="text-orange-600 font-medium inline-flex items-center gap-0.5">
                      弱规则
                      <Tooltip
                        placement="topLeft"
                        :overlay-inner-style="{ maxWidth: '300px' }"
                      >
                        <template #title>
                          <ul class="rule-strength-tooltip-list">
                            <li v-for="(line, i) in weakRuleHelpItems" :key="i">{{ line }}</li>
                          </ul>
                        </template>
                        <span
                          class="rule-strength-help-trigger"
                          tabindex="0"
                          @click.stop.prevent
                        >
                          <QuestionCircleOutlined />
                        </span>
                      </Tooltip>
                    </span>
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

          <div class="flex items-center gap-3">
              <Switch
                :checked="formValues.enabled === '0'"
                disabled
              />
              <span class="text-sm text-gray-700">{{
                formValues.enabled === '0' ? '启用' : '停用'
              }}</span>
            </div>
          </div>
        </Form>
      </template>

      <!-- Step 3 预览保存 -->
      <template #step-3>
        <div class="rule-preview-wrap">
          <div class="text-sm text-gray-500 mb-3">请确认以下配置，确认无误后点击保存。</div>
          <div class="preview-section">
            <div class="preview-title">
              <CheckCircleOutlined class="text-emerald-600" />
              规则配置总览
            </div>

            <div class="detail-title">基本信息</div>
            <Descriptions bordered :column="2" size="small" class="rule-preview-desc mb-4">
              <Descriptions.Item label="规则名称">{{ formValues.ruleName || '—' }}</Descriptions.Item>
              <Descriptions.Item label="规则编码">
                <code class="rule-code-inline">{{ formValues.ruleCode || '—' }}</code>
              </Descriptions.Item>
              <Descriptions.Item label="规则类型">
                <Tag color="processing">{{ ruleTypeLabel }}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="关联模板">
                {{
                  templateRows.find((t) => t.id === formValues.templateId)?.templateName ||
                  selectedTemplate?.templateName ||
                  '—'
                }}
              </Descriptions.Item>
              <Descriptions.Item label="适用级别">
                <Tag color="default">{{ applyLevelLabel }}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <div class="flex flex-wrap items-center gap-2">
                  <Switch :checked="formValues.enabled === '0'" disabled />
                  <span class="text-sm">{{
                    formValues.enabled === '0' ? '启用' : '停用'
                  }}</span>
                </div>
              </Descriptions.Item>
            </Descriptions>

            <div class="detail-title">数据目标</div>
            <Descriptions bordered :column="2" size="small" class="rule-preview-desc mb-4">
              <Descriptions.Item label="目标数据源">
                {{ selectedDs?.dsName || '—' }}
              </Descriptions.Item>
              <Descriptions.Item label="数仓层标签">
                <Tag v-if="selectedDs?.dataLayer" color="blue">{{ selectedDs.dataLayer }}</Tag>
                <span v-else class="text-gray-400">—</span>
              </Descriptions.Item>
              <Descriptions.Item label="目标表">
                <code v-if="formValues.targetTable" class="rule-code-inline">{{ formValues.targetTable }}</code>
                <span v-else class="text-gray-400">—</span>
              </Descriptions.Item>
              <Descriptions.Item label="目标列">
                {{ formValues.targetColumn?.trim() ? formValues.targetColumn : '—' }}
              </Descriptions.Item>
            </Descriptions>

            <div class="detail-title">表达式与阈值</div>
            <Descriptions bordered :column="1" size="small" class="rule-preview-desc mb-4">
              <Descriptions.Item label="规则表达式">
                <pre class="rule-expr-preview m-0">{{ formValues.ruleExpr || '—' }}</pre>
              </Descriptions.Item>
              <Descriptions.Item label="阈值范围">{{ thresholdPreview }}</Descriptions.Item>
              <Descriptions.Item label="波动阈值(%)">
                {{ formValues.fluctuationThreshold ?? '—' }}
              </Descriptions.Item>
            </Descriptions>

            <div class="detail-title">质量与告警</div>
            <Descriptions bordered :column="2" size="small" class="rule-preview-desc">
              <Descriptions.Item label="质量维度" :span="2">
                <template v-if="dimensionChecked.length">
                  <Tag
                    v-for="d in dimensionChecked"
                    :key="d"
                    color="cyan"
                    class="mb-1 mr-1"
                  >
                    {{ dimensionOptions.find((o) => o.value === d)?.label || d }}
                  </Tag>
                </template>
                <span v-else class="text-gray-400">—</span>
              </Descriptions.Item>
              <Descriptions.Item label="规则强度">
                <Tag :color="formValues.ruleStrength === 'STRONG' ? 'red' : 'orange'">{{
                  strengthLabel
                }}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="错误级别">
                <Tag :color="errorLevelMeta[formValues.errorLevel || '']?.color || 'default'">
                  {{ errorLevelLabel }}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="告警接收人" :span="2">
                {{ formValues.alertReceivers || '—' }}
              </Descriptions.Item>
              <Descriptions.Item label="排序权重">{{ formValues.sortOrder ?? 0 }}</Descriptions.Item>
            </Descriptions>
          </div>
        </div>
      </template>
    </WizardDrawer>
  </BasicDrawer>
</template>

<style scoped>
.rule-wizard-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}
.dqc-rule-step3-form :deep(.ant-form-item) {
  margin-bottom: 18px;
}
.dqc-dimension-panel {
  padding: 14px 14px 12px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  background: linear-gradient(165deg, #fafafa 0%, #fff 40%);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.dqc-dimension-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
@media (min-width: 640px) {
  .dqc-dimension-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
.dqc-dimension-cell {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  background: #fff;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    box-shadow 0.2s ease;
}
.dqc-dimension-cell:hover {
  border-color: #bae0ff;
  box-shadow: 0 1px 4px rgba(22, 119, 255, 0.08);
}
.dqc-dimension-cell--checked {
  border-color: #1677ff;
  background: #e6f4ff;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.12);
}
.dqc-dimension-cell :deep(.dqc-dimension-checkbox) {
  width: 100%;
}
.dqc-dimension-cell :deep(.ant-checkbox-wrapper) {
  width: 100%;
  align-items: center;
  font-size: 14px;
  color: #262626;
}
.dqc-dimension-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}
.rule-strength-help-trigger {
  display: inline-flex;
  align-items: center;
  color: #8c8c8c;
  font-size: 13px;
  line-height: 1;
  cursor: help;
  outline: none;
}
.rule-strength-help-trigger:hover,
.rule-strength-help-trigger:focus-visible {
  color: #595959;
}
.rule-strength-tooltip-list {
  margin: 0;
  padding-left: 1.1em;
  text-align: left;
  font-size: 12px;
  line-height: 1.55;
}
.rule-strength-tooltip-list li + li {
  margin-top: 4px;
}
.dqc-rule-strength-panel {
  margin-top: 8px;
  padding: 16px 18px 4px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fafafa 0%, #fff 56px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.dqc-rule-strength-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.rule-preview-wrap {
  padding-right: 4px;
}
.preview-section {
  overflow-x: hidden;
}
.preview-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #434343;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #1677ff;
}
.rule-preview-desc :deep(.ant-descriptions-item-label) {
  min-width: 96px;
  max-width: 160px;
  width: 28%;
  font-weight: 500;
  color: #595959;
  white-space: nowrap;
  writing-mode: horizontal-tb;
}
.rule-preview-desc :deep(.ant-descriptions-item-content) {
  word-break: break-word;
}
.rule-code-inline {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  color: #262626;
}
.rule-expr-preview {
  display: block;
  max-height: 180px;
  overflow: auto;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.5;
  color: #262626;
  background: #f7f7f9;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
