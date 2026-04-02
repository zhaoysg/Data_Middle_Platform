<script setup lang="ts">
import type { DqcRuleTemplate } from '#/api/metadata/model';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Alert, Divider, Form, Input, InputNumber, Radio, Select, message } from 'ant-design-vue';

import { datasourceColumns, datasourceEnabled, datasourceTables } from '#/api/system/datasource';
import { dqcTemplateList } from '#/api/metadata/dqc/template';
import { dqcRuleAdd, dqcRuleInfo, dqcRuleUpdate } from '#/api/metadata/dqc/rule';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const datasourceOptions = ref<{ label: string; value: number }[]>([]);
const templateOptions = ref<{ label: string; value: number }[]>([]);
const templateRecords = ref<DqcRuleTemplate[]>([]);
const tableOptions = ref<{ label: string; value: string }[]>([]);
const columnOptions = ref<{ label: string; value: string }[]>([]);
const tableLoading = ref(false);
const columnLoading = ref(false);
const initializingLinkage = ref(false);

const dimensionOptions = [
  { label: '完整性', value: 'COMPLETENESS' },
  { label: '唯一性', value: 'UNIQUENESS' },
  { label: '准确性', value: 'ACCURACY' },
  { label: '一致性', value: 'CONSISTENCY' },
  { label: '时效性', value: 'TIMELINESS' },
  { label: '有效性', value: 'VALIDITY' },
  { label: '自定义', value: 'CUSTOM' },
];
const ruleTypeOptions = [
  { label: '空值检查', value: 'NULL_CHECK' },
  { label: '唯一性检查', value: 'UNIQUE' },
  { label: '正则匹配', value: 'REGEX' },
  { label: '阈值检查', value: 'THRESHOLD' },
  { label: '波动检查', value: 'FLUCTUATION' },
  { label: '自定义SQL', value: 'CUSTOM_SQL' },
];
const applyLevelOptions = [
  { label: '表级', value: 'TABLE' },
  { label: '字段级', value: 'COLUMN' },
  { label: '跨字段', value: 'CROSS_FIELD' },
  { label: '跨表', value: 'CROSS_TABLE' },
];
const errorLevelOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];
const enabledOptions = [
  { label: '启用', value: '1' },
  { label: '停用', value: '0' },
];

const title = computed(() => {
  return recordId.value ? '编辑规则' : '新增规则';
});

const selectedTemplate = computed(() => {
  return templateRecords.value.find((item) => item.id === formValues.value.templateId);
});

const templateParamHints = computed(() => parseParamSpec(selectedTemplate.value?.paramSpec));
const thresholdKeys = computed(() => Object.keys(parseJsonObject(selectedTemplate.value?.thresholdJson)));
const currentRuleType = computed(() => formValues.value.ruleType || selectedTemplate.value?.ruleType);
const currentApplyLevel = computed(() => formValues.value.applyLevel || selectedTemplate.value?.applyLevel);

const showRegexPattern = computed(() => {
  return (
    currentRuleType.value === 'REGEX' ||
    thresholdKeys.value.some((key) => key.toLowerCase().includes('pattern')) ||
    templateParamHints.value.some((item) => item.toLowerCase().includes('pattern')) ||
    Boolean(formValues.value.regexPattern)
  );
});

const showThresholdMin = computed(() => {
  return (
    currentRuleType.value === 'THRESHOLD' ||
    thresholdKeys.value.some((key) => key.toLowerCase().includes('min')) ||
    Boolean(formValues.value.thresholdMin)
  );
});

const showThresholdMax = computed(() => {
  return (
    currentRuleType.value === 'THRESHOLD' ||
    thresholdKeys.value.some((key) => key.toLowerCase().includes('max')) ||
    Boolean(formValues.value.thresholdMax)
  );
});

const showFluctuationThreshold = computed(() => {
  return (
    currentRuleType.value === 'FLUCTUATION' ||
    thresholdKeys.value.some((key) => key.toLowerCase().includes('fluct')) ||
    Boolean(formValues.value.fluctuationThreshold)
  );
});

const showCompareFields = computed(() => {
  return ['CROSS_FIELD', 'CROSS_TABLE'].includes(currentApplyLevel.value || '');
});

function filterOption(input: string, option?: { label?: string }) {
  return String(option?.label ?? '')
    .toLowerCase()
    .includes(input.toLowerCase());
}

function parseJsonObject(raw?: string) {
  if (!raw || typeof raw !== 'string') {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function parseParamSpec(raw?: string) {
  if (!raw || typeof raw !== 'string') {
    return [] as string[];
  }
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed
        .map((item) => item?.name || item?.label || item?.field || '')
        .filter(Boolean);
    }
    if (parsed && typeof parsed === 'object') {
      return Object.keys(parsed);
    }
  } catch {
    return [];
  }
  return [];
}

function readNumber(raw: unknown) {
  if (raw === null || raw === undefined || raw === '') {
    return undefined;
  }
  const parsed = Number(raw);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function applyThresholdDefaults(raw?: string, force = false) {
  const values = parseJsonObject(raw);
  const minValue =
    values.thresholdMin ??
    values.threshold_min ??
    values.minValue ??
    values.min_value ??
    values.minThreshold ??
    values.min_threshold;
  const maxValue =
    values.thresholdMax ??
    values.threshold_max ??
    values.maxValue ??
    values.max_value ??
    values.maxThreshold ??
    values.max_threshold;
  const fluctuationValue =
    values.fluctuationThreshold ??
    values.fluctuation_threshold ??
    values.thresholdPct ??
    values.threshold_pct ??
    values.maxFluctuation ??
    values.max_fluctuation;
  const patternValue = values.pattern ?? values.regex;

  if (force || formValues.value.thresholdMin == null || formValues.value.thresholdMin === '') {
    formValues.value.thresholdMin = readNumber(minValue);
  }
  if (force || formValues.value.thresholdMax == null || formValues.value.thresholdMax === '') {
    formValues.value.thresholdMax = readNumber(maxValue);
  }
  if (
    force ||
    formValues.value.fluctuationThreshold == null ||
    formValues.value.fluctuationThreshold === ''
  ) {
    formValues.value.fluctuationThreshold = readNumber(fluctuationValue);
  }
  if ((force || !formValues.value.regexPattern) && patternValue) {
    formValues.value.regexPattern = String(patternValue);
  }
}

function applyTemplateDefaults(template?: DqcRuleTemplate, force = false) {
  if (!template) {
    return;
  }
  if (force || !formValues.value.ruleType) {
    formValues.value.ruleType = template.ruleType;
  }
  if (force || !formValues.value.applyLevel) {
    formValues.value.applyLevel = template.applyLevel;
  }
  if (force || !formValues.value.dimension) {
    formValues.value.dimension = template.dimension;
  }
  if (force || !formValues.value.ruleExpr) {
    formValues.value.ruleExpr = template.defaultExpr;
  }
  applyThresholdDefaults(template.thresholdJson, force);
}

async function loadTableOptions(dsId?: number, selectedTable?: string) {
  if (!dsId) {
    tableOptions.value = [];
    return;
  }
  tableLoading.value = true;
  try {
    const tables = await datasourceTables(dsId);
    tableOptions.value = (tables || []).map((item) => ({
      label: item,
      value: item,
    }));
    if (selectedTable && !tableOptions.value.some((item) => item.value === selectedTable)) {
      tableOptions.value.unshift({
        label: selectedTable,
        value: selectedTable,
      });
    }
  } finally {
    tableLoading.value = false;
  }
}

async function loadColumnOptions(dsId?: number, tableName?: string, selectedColumn?: string) {
  if (!dsId || !tableName) {
    columnOptions.value = [];
    return;
  }
  columnLoading.value = true;
  try {
    const columns = await datasourceColumns(dsId, tableName);
    columnOptions.value = (columns || []).map((item: any) => ({
      label: item.dataType ? `${item.columnName} (${item.dataType})` : item.columnName,
      value: item.columnName,
    }));
    if (selectedColumn && !columnOptions.value.some((item) => item.value === selectedColumn)) {
      columnOptions.value.unshift({
        label: selectedColumn,
        value: selectedColumn,
      });
    }
  } finally {
    columnLoading.value = false;
  }
}

watch(
  () => formValues.value.targetDsId,
  async (dsId, previousDsId) => {
    if (initializingLinkage.value || dsId === previousDsId) {
      return;
    }
    formValues.value.targetTable = undefined;
    formValues.value.targetColumn = undefined;
    columnOptions.value = [];
    await loadTableOptions(dsId);
  },
);

watch(
  () => formValues.value.targetTable,
  async (tableName, previousTable) => {
    if (initializingLinkage.value || tableName === previousTable) {
      return;
    }
    formValues.value.targetColumn = undefined;
    await loadColumnOptions(formValues.value.targetDsId, tableName);
  },
);

watch(
  () => formValues.value.templateId,
  (templateId, previousTemplateId) => {
    if (initializingLinkage.value || templateId === previousTemplateId) {
      return;
    }
    applyTemplateDefaults(selectedTemplate.value);
  },
);

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  onClosed: handleClosed,
  onConfirm: handleSubmit,
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }

    drawerApi.drawerLoading(true);
    try {
      const dsList = await datasourceEnabled();
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: item.dsCode ? `${item.dsName} (${item.dsCode})` : item.dsName,
        value: item.dsId,
      }));

      const templateList = await dqcTemplateList({ pageNum: 1, pageSize: 200 });
      templateRecords.value = (templateList as any)?.rows || [];
      templateOptions.value = templateRecords.value.map((item) => ({
        label: item.templateName || '',
        value: item.id!,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcRuleInfo(id);
        initializingLinkage.value = true;
        formValues.value = {
          ...info,
          dimension: info.dimension || info.dimensions,
        };
        await loadTableOptions(info.targetDsId, info.targetTable);
        await loadColumnOptions(info.targetDsId, info.targetTable, info.targetColumn);
        initializingLinkage.value = false;
      } else {
        recordId.value = undefined;
        formValues.value = {
          errorLevel: 'MEDIUM',
          enabled: '1',
          sortOrder: 0,
        };
        tableOptions.value = [];
        columnOptions.value = [];
      }
    } finally {
      initializingLinkage.value = false;
      drawerApi.drawerLoading(false);
    }
  },
});

function buildPayload() {
  const payload: Record<string, any> = {
    ...formValues.value,
    dimensions: formValues.value.dimension || formValues.value.dimensions,
  };
  if (!payload.ruleType) {
    payload.ruleType = selectedTemplate.value?.ruleType;
  }
  if (!payload.applyLevel) {
    payload.applyLevel = selectedTemplate.value?.applyLevel;
  }
  if (!payload.ruleExpr) {
    payload.ruleExpr = selectedTemplate.value?.defaultExpr;
  }
  delete payload.dimension;
  delete payload.ruleParams;
  delete payload.dsName;
  delete payload.targetDsName;
  return payload;
}

async function handleSubmit() {
  drawerApi.lock(true);
  try {
    const payload = buildPayload();
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
  }
}

function handleClosed() {
  recordId.value = undefined;
  formValues.value = {};
  tableOptions.value = [];
  columnOptions.value = [];
  initializingLinkage.value = false;
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[760px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="规则名称" name="ruleName">
        <Input v-model:value="formValues.ruleName" placeholder="请输入规则名称" />
      </Form.Item>
      <Form.Item label="规则编码" name="ruleCode">
        <Input v-model:value="formValues.ruleCode" placeholder="请输入规则编码" />
      </Form.Item>
      <Form.Item label="关联模板" name="templateId">
        <Select
          v-model:value="formValues.templateId"
          :options="templateOptions"
          placeholder="请选择关联模板"
          show-search
        />
      </Form.Item>

      <Alert
        v-if="selectedTemplate"
        class="mb-4"
        show-icon
        type="info"
        :message="`已选择模板：${selectedTemplate.templateName}`"
        :description="selectedTemplate.templateDesc || '选择模板后会自动带出规则类型、适用级别、默认表达式和常用阈值。'"
      />

      <Divider orientation="left">基础配置</Divider>

      <div class="grid gap-4 md:grid-cols-2">
        <Form.Item label="规则类型" name="ruleType">
          <Select
            v-model:value="formValues.ruleType"
            :options="ruleTypeOptions"
            placeholder="请选择规则类型"
          />
        </Form.Item>
        <Form.Item label="适用级别" name="applyLevel">
          <Select
            v-model:value="formValues.applyLevel"
            :options="applyLevelOptions"
            placeholder="请选择适用级别"
          />
        </Form.Item>
        <Form.Item label="质量维度" name="dimension">
          <Select
            v-model:value="formValues.dimension"
            :options="dimensionOptions"
            placeholder="请选择质量维度"
          />
        </Form.Item>
        <Form.Item label="错误级别" name="errorLevel">
          <Select
            v-model:value="formValues.errorLevel"
            :options="errorLevelOptions"
            placeholder="请选择错误级别"
          />
        </Form.Item>
      </div>

      <div class="grid gap-4 md:grid-cols-2">
        <Form.Item label="数据源" name="targetDsId">
          <Select
            v-model:value="formValues.targetDsId"
            :options="datasourceOptions"
            :filter-option="filterOption"
            option-filter-prop="label"
            placeholder="请选择数据源"
            show-search
          />
        </Form.Item>
        <Form.Item label="目标表" name="targetTable">
          <Select
            v-model:value="formValues.targetTable"
            :disabled="!formValues.targetDsId"
            :filter-option="filterOption"
            :loading="tableLoading"
            :options="tableOptions"
            option-filter-prop="label"
            placeholder="请先选择数据源"
            show-search
          />
        </Form.Item>
      </div>

      <div class="grid gap-4 md:grid-cols-2">
        <Form.Item label="目标字段" name="targetColumn">
          <Select
            v-model:value="formValues.targetColumn"
            :disabled="!formValues.targetTable"
            :filter-option="filterOption"
            :loading="columnLoading"
            :options="columnOptions"
            option-filter-prop="label"
            placeholder="请先选择目标表"
            show-search
          />
        </Form.Item>
        <Form.Item label="排序" name="sortOrder">
          <InputNumber v-model:value="formValues.sortOrder" class="w-full" :min="0" />
        </Form.Item>
      </div>

      <Divider orientation="left">高级参数</Divider>

      <div v-if="templateParamHints.length" class="mb-4 rounded-md bg-gray-50 p-3 text-sm text-gray-600">
        模板参数提示：
        <span>{{ templateParamHints.join(' / ') }}</span>
      </div>

      <Form.Item label="规则表达式 / SQL" name="ruleExpr">
        <Input.TextArea
          v-model:value="formValues.ruleExpr"
          placeholder="支持使用模板默认表达式，也可以按实际规则场景微调。"
          :rows="4"
        />
      </Form.Item>

      <div class="grid gap-4 md:grid-cols-3">
        <Form.Item v-if="showThresholdMin" label="最小阈值" name="thresholdMin">
          <InputNumber v-model:value="formValues.thresholdMin" class="w-full" />
        </Form.Item>
        <Form.Item v-if="showThresholdMax" label="最大阈值" name="thresholdMax">
          <InputNumber v-model:value="formValues.thresholdMax" class="w-full" />
        </Form.Item>
        <Form.Item
          v-if="showFluctuationThreshold"
          label="波动阈值(%)"
          name="fluctuationThreshold"
        >
          <InputNumber
            v-model:value="formValues.fluctuationThreshold"
            class="w-full"
            :min="0"
            :max="100"
          />
        </Form.Item>
      </div>

      <Form.Item v-if="showRegexPattern" label="正则表达式" name="regexPattern">
        <Input
          v-model:value="formValues.regexPattern"
          placeholder="请输入正则表达式，例如 ^[A-Z0-9_]+$"
        />
      </Form.Item>

      <template v-if="showCompareFields">
        <div class="grid gap-4 md:grid-cols-3">
          <Form.Item label="对比数据源" name="compareDsId">
            <Select
              v-model:value="formValues.compareDsId"
              :options="datasourceOptions"
              :filter-option="filterOption"
              option-filter-prop="label"
              placeholder="可选"
              show-search
            />
          </Form.Item>
          <Form.Item label="对比表" name="compareTable">
            <Input v-model:value="formValues.compareTable" placeholder="请输入对比表名" />
          </Form.Item>
          <Form.Item label="对比字段" name="compareColumn">
            <Input v-model:value="formValues.compareColumn" placeholder="请输入对比字段名" />
          </Form.Item>
        </div>
      </template>

      <Form.Item label="告警接收人" name="alertReceivers">
        <Input
          v-model:value="formValues.alertReceivers"
          placeholder="多个接收人用逗号分隔"
        />
      </Form.Item>

      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
