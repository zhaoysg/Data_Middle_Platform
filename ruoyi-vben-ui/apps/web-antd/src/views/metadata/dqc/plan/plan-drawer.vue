<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Select, message, Modal, Table, Tag, Space } from 'ant-design-vue';
import {
  SearchOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue';

import {
  datasourceEnabled,
  datasourceTables,
  datasourceColumns,
  datasourceSchemas,
} from '#/api/system/datasource';
import {
  dqcPlanAdd,
  dqcPlanBindRules,
  dqcPlanBoundRules,
  dqcPlanInfo,
  dqcPlanPublish,
  dqcPlanUpdate,
} from '#/api/metadata/dqc/plan';
import { dqcRuleList } from '#/api/metadata/dqc/rule';

import WizardDrawer from '#/components/metadata/WizardDrawer.vue';

const triggerTypeOptions = [
  { label: '手动触发', value: 'MANUAL' },
  { label: '定时触发', value: 'SCHEDULE' },
  { label: 'API调用', value: 'API' },
];

const triggerTypeLabelMap: Record<string, string> = {
  MANUAL: '手动触发',
  SCHEDULE: '定时触发',
  API: 'API调用',
};

const sensitivityLevelOptions = [
  { label: '🔴 L4 机密', value: 'L4' },
  { label: '🟠 L3 敏感', value: 'L3' },
  { label: '🔵 L2 内部', value: 'L2' },
  { label: '🟢 L1 公开', value: 'L1' },
];

const sensitivityTextColor: Record<string, string> = {
  L4: '#FF4D4F',
  L3: '#FA8C16',
  L2: '#1677FF',
  L1: '#52C41A',
};

const sensitivityBgColor: Record<string, string> = {
  L4: '#fff1f0',
  L3: '#fff7e6',
  L2: '#e6f4ff',
  L1: '#f6ffed',
};

const errorLevelOptions = [
  { label: '高告警', value: 'HIGH' },
  { label: '中告警', value: 'MEDIUM' },
  { label: '低告警', value: 'LOW' },
];

const errorLevelColor: Record<string, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'blue',
};

const ruleStrengthColor: Record<string, string> = {
  STRONG: 'red',
  WEAK: 'orange',
};

const cronExamples = [
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每天8:30', value: '0 30 8 * * ?' },
  { label: '工作日上午9点', value: '0 0 9 ? * MON-FRI' },
  { label: '每30分钟', value: '0 0/30 * * * ?' },
  { label: '每周一凌晨2点', value: '0 0 2 ? * 1' },
];

const steps = [
  { title: '基本信息' },
  { title: '绑定表范围' },
  { title: '字段与规则' },
  { title: '调度配置' },
  { title: '预览保存' },
];

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const currentStep = ref(0);
const submitting = ref(false);
const wizardRef = ref<InstanceType<typeof WizardDrawer>>();

const formValues = ref<Record<string, any>>({
  status: 'DRAFT',
  triggerType: 'MANUAL',
  alertOnFailure: '1',
  alertOnSuccess: '0',
  autoBlock: '0',
});

// ---- 数据源相关 ----
const datasourceOptions = ref<{ label: string; value: number; dsType?: string }[]>([]);
const bindDsId = ref<number>();
const dsType = ref<string>();
const tableLoading = ref(false);
const availableTables = ref<string[]>([]);
const availableTableSet = ref<Set<string>>(new Set());

// ---- Schema 相关（PostgreSQL） ----
const schemaList = ref<string[]>([]);
const schemaLoading = ref(false);
const bindSchema = ref('');

// ---- 已选表 & 字段 ----
// 按 tableName 索引，每项含 columns[] 和 boundRules[]
interface TableBinding {
  tableName: string;
  columns: string[];
  // key: columnName, value: 绑定的规则列表
  columnRules: Record<string, any[]>;
}

const tableBindings = ref<TableBinding[]>([]);

// ---- Computed ----
const isPostgresBind = computed(() => {
  const ds = datasourceOptions.value.find((d) => d.value === bindDsId.value);
  return ds?.dsType?.toUpperCase()?.includes('POSTGRESQL') ?? false;
});

const schemaOptions = computed(() =>
  schemaList.value.map((s) => ({ label: s, value: s })),
);

const tableSelectPlaceholder = computed(() => {
  if (!bindDsId.value) return '请先选择数据源';
  if (isPostgresBind.value && !bindSchema.value) return '请先选择 Schema';
  return '搜索或选择表名（可多选）';
});

// ---- 字段+规则扁平行 ----
interface FlatRow {
  key: string;
  tableName: string;
  columnName: string;
  rules: any[];
}

// ---- 规则选择器 ----
const ruleSelectorVisible = ref(false);
const ruleSelectorTable = ref<string>();
const ruleSelectorColumn = ref<string>();
const ruleSelectorLoading = ref(false);
const allRules = ref<any[]>([]);
const selectedRuleIds = ref<number[]>([]);
const ruleSelectorLoading2 = ref(false);

// ---- API规则列表参数 ----
const ruleFilterKeyword = ref<string>();

const title = computed(() => (recordId.value ? '编辑质检方案' : '新增质检方案'));

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  footer: false,
  onClosed: handleClosed,
  onCancel: () => drawerApi.close(),
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    drawerApi.drawerLoading(true);
    try {
      // 加载数据源列表
      const dsList = await datasourceEnabled();
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: `${item.dsName || item.datasourceName || ''}`,
        value: item.dsId || item.id,
        dsType: item.dsType,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcPlanInfo(id);
        formValues.value = { ...info };
        bindDsId.value = info.dsId;

        // 解析 bindValue JSON，填充绑定配置
        if (info.bindValue) {
          try {
            const bv = JSON.parse(info.bindValue);
            if (bv.dsId) bindDsId.value = Number(bv.dsId);
            if (bv.schema) bindSchema.value = bv.schema;

            if (bv.dsId) {
              const ds = datasourceOptions.value.find((d) => d.value === Number(bv.dsId));
              if (ds) dsType.value = ds.dsType;

              // PostgreSQL 加载 Schema
              if (isPostgresBind.value) {
                schemaLoading.value = true;
                try {
                  const schemas = await datasourceSchemas(Number(bv.dsId));
                  schemaList.value = schemas?.data || schemas || [];
                } catch (_) {}
                finally { schemaLoading.value = false; }
              }
            }

            if (bv.tables && Array.isArray(bv.tables)) {
              availableTables.value = bv.tables;
              availableTableSet.value = new Set(bv.tables);
              for (const tbl of bv.tables) {
                await loadTableColumns(tbl);
              }
            }
            if (bv.bindSensitivityLevel) {
              formValues.value.bindSensitivityLevel = bv.bindSensitivityLevel;
            }
          } catch (_) {}
        }

        // 加载已绑定的规则
        if (id) {
          await loadBoundRules(id);
        }
      } else {
        recordId.value = undefined;
        formValues.value = {
          status: 'DRAFT',
          triggerType: 'MANUAL',
          alertOnFailure: '1',
          alertOnSuccess: '0',
          autoBlock: '0',
          bindSensitivityLevel: '',
        };
        tableBindings.value = [];
      }
      currentStep.value = 0;
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

// ---- 加载已绑定规则到 tableBindings ----
async function loadBoundRules(planId: number) {
  try {
    const bound: any[] = await dqcPlanBoundRules(planId);
    for (const b of bound || []) {
      // 优先用 targetTable/targetColumn 回显
      let binding = tableBindings.value.find((tb) => tb.tableName === (b.targetTable || b.tableName));
      if (binding) {
        const col = b.targetColumn || b.columnName;
        if (col && !binding.columnRules[col]) {
          binding.columnRules[col] = [];
        }
        if (col) {
          binding.columnRules[col].push({
            id: b.ruleId,
            ruleId: b.ruleId,
            ruleName: b.ruleName || `规则${b.ruleId}`,
            ruleType: b.ruleType,
            ruleStrength: b.ruleStrength,
            targetTable: b.targetTable,
            targetColumn: col,
            enabled: b.enabled,
          });
        }
      }
    }
  } catch (_) {}
}

// ---- 数据源切换，加载表列表和 Schema ----
async function onDsChange(dsId: number) {
  bindDsId.value = dsId;
  bindSchema.value = '';
  schemaList.value = [];
  tableBindings.value = [];
  availableTables.value = [];
  availableTableSet.value = new Set();
  if (!dsId) return;

  const ds = datasourceOptions.value.find((d) => d.value === dsId);
  dsType.value = ds?.dsType;

  // PostgreSQL 加载 Schema 列表
  if (ds?.dsType?.toUpperCase()?.includes('POSTGRESQL')) {
    schemaLoading.value = true;
    try {
      const schemas = await datasourceSchemas(dsId);
      schemaList.value = schemas?.data || schemas || [];
    } catch (_) {}
    finally { schemaLoading.value = false; }
  }

  tableLoading.value = true;
  try {
    const tables = await datasourceTables(dsId);
    availableTables.value = tables || [];
    availableTableSet.value = new Set(tables || []);
  } catch (_) {
    availableTables.value = [];
  } finally {
    tableLoading.value = false;
  }
}

// ---- Schema 切换，重新加载表列表 ----
async function onSchemaChange(schema: string) {
  bindSchema.value = schema;
  tableBindings.value = [];
  availableTables.value = [];
  availableTableSet.value = new Set();
  if (!bindDsId.value) return;
  tableLoading.value = true;
  try {
    const tables = await datasourceTables(bindDsId.value, schema);
    availableTables.value = tables || [];
    availableTableSet.value = new Set(tables || []);
  } catch (_) {
    availableTables.value = [];
  } finally {
    tableLoading.value = false;
  }
}

// ---- 加载某张表的字段 ----
async function loadTableColumns(tableName: string) {
  if (!bindDsId.value) return;
  try {
    const cols = await datasourceColumns(bindDsId.value, tableName, bindSchema.value || undefined);
    const colNames = (cols || []).map((c: any) => c.columnName || c.name || c);
    let binding = tableBindings.value.find((tb) => tb.tableName === tableName);
    if (!binding) {
      binding = { tableName, columns: colNames, columnRules: {} };
      tableBindings.value.push(binding);
    } else {
      binding.columns = colNames;
    }
  } catch (_) {
    let binding = tableBindings.value.find((tb) => tb.tableName === tableName);
    if (!binding) {
      binding = { tableName, columns: [], columnRules: {} };
      tableBindings.value.push(binding);
    }
  }
}

// ---- 选择表：加入 tableBindings 并加载字段 ----
async function onTableSelectChange(selected: string[]) {
  // 新增的表
  const added = selected.filter((t) => !availableTableSet.value.has(t));
  // 移除的表
  const removed = [...availableTableSet.value].filter((t) => !selected.includes(t));

  for (const t of added) {
    await loadTableColumns(t);
  }
  availableTableSet.value = new Set(selected);
  availableTables.value = selected;
  // 删除移除的表的 binding
  tableBindings.value = tableBindings.value.filter((tb) => selected.includes(tb.tableName));
}

// ---- 扁平行 ----
const flattenedRows = computed((): FlatRow[] => {
  const rows: FlatRow[] = [];
  for (const tb of tableBindings.value) {
    for (const col of tb.columns) {
      rows.push({
        key: `${tb.tableName}__${col}`,
        tableName: tb.tableName,
        columnName: col,
        rules: tb.columnRules[col] || [],
      });
    }
  }
  return rows;
});

const selectedColumnCount = computed(() =>
  tableBindings.value.reduce((sum, tb) => sum + (tb.columns?.length || 0), 0),
);

const totalBoundRules = computed(() =>
  tableBindings.value.reduce((sum, tb) => {
    return sum + Object.values(tb.columnRules).reduce((s, rules) => s + (rules?.length || 0), 0);
  }, 0),
);

// ---- 打开规则选择器 ----
async function openRuleSelector(tableName: string, columnName: string) {
  ruleSelectorTable.value = tableName;
  ruleSelectorColumn.value = columnName;
  ruleSelectorLoading.value = true;
  selectedRuleIds.value = [];

  // 获取当前列已绑定的规则 ID
  const binding = tableBindings.value.find((tb) => tb.tableName === tableName);
  if (binding?.columnRules[columnName]) {
    selectedRuleIds.value = binding.columnRules[columnName].map((r: any) => r.ruleId);
  }

  try {
    const res = await dqcRuleList({
      pageNum: 1,
      pageSize: 200,
      enabled: '0',
    });
    allRules.value = res?.rows || res || [];
  } catch (_) {
    allRules.value = [];
  } finally {
    ruleSelectorLoading.value = false;
  }
  ruleSelectorVisible.value = true;
}

// ---- 确认规则绑定 ----
function confirmRuleBinding() {
  if (!ruleSelectorTable.value || !ruleSelectorColumn.value) return;
  const binding = tableBindings.value.find((tb) => tb.tableName === ruleSelectorTable.value);
  if (!binding) return;

  // 用选中 ID 过滤 allRules 中对应的规则
  const chosen = allRules.value.filter((r: any) => selectedRuleIds.value.includes(r.id));
  binding.columnRules[ruleSelectorColumn.value!] = chosen;
  ruleSelectorVisible.value = false;
}

// ---- 移除某字段绑定的单个规则 ----
function removeRule(tableName: string, columnName: string, ruleId: number) {
  const binding = tableBindings.value.find((tb) => tb.tableName === tableName);
  if (!binding) return;
  binding.columnRules[columnName] = (binding.columnRules[columnName] || []).filter(
    (r: any) => r.id !== ruleId,
  );
}

// ---- 提交 ----
async function handleSubmit() {
  drawerApi.lock(true);
  submitting.value = true;
  try {
    const payload = buildPayload();

    if (recordId.value) {
      await dqcPlanUpdate({ id: recordId.value, ...payload });
    } else {
      const newId = await dqcPlanAdd(payload);
      if (typeof newId === 'number' || typeof newId === 'string') {
        recordId.value = Number(newId);
      }
    }

    // 绑定规则（无论新增还是编辑都重新绑定）
    if (recordId.value) {
      await syncRuleBindings(recordId.value);
      // 如果是 DRAFT 状态，提供发布选项
      if (formValues.value.status === 'DRAFT') {
        Modal.confirm({
          title: '方案已保存',
          content: '是否立即发布方案？发布后方可执行。',
          okText: '发布',
          cancelText: '稍后',
          onOk: async () => {
            await dqcPlanPublish(recordId.value!);
            emit('reload');
            drawerApi.close();
          },
          onCancel: () => {
            emit('reload');
            drawerApi.close();
          },
        });
        return;
      }
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

// ---- 同步规则绑定到后端 ----
async function syncRuleBindings(planId: number) {
  const rules: any[] = [];
  let order = 1;
  for (const tb of tableBindings.value) {
    for (const col of tb.columns) {
      const colRules = tb.columnRules[col] || [];
      for (const r of colRules) {
        rules.push({
          ruleId: r.id || r.ruleId,
          ruleOrder: order++,
          enabled: r.enabled !== false,
          skipOnFailure: false,
          targetTable: tb.tableName,
          targetColumn: col,
        });
      }
    }
  }
  // 服务器端会先删除旧关联再插入新关联
  if (rules.length > 0) {
    await dqcPlanBindRules(planId, rules);
  }
}

// ---- 构建 bindValue JSON（v2 格式）----
function buildBindValue(): string {
  return JSON.stringify({
    v: 2,
    dsId: bindDsId.value,
    schema: bindSchema.value || undefined,
    tables: availableTables.value || [],
    bindSensitivityLevel: formValues.value.bindSensitivityLevel || undefined,
    // 以下字段由前端通过规则绑定API单独管理
  });
}

// ---- 构建 payload ----
function buildPayload() {
  const f = formValues.value;
  const ds = datasourceOptions.value.find((d) => d.value === bindDsId.value);

  return {
    planName: f.planName,
    planCode: f.planCode,
    planDesc: f.planDesc,
    bindType: f.bindSensitivityLevel ? 'SENSITIVITY_LEVEL' : 'TABLE',
    bindValue: buildBindValue(),
    dsId: bindDsId.value,
    layerCode: (ds as any)?.dataLayer || f.layerCode || '',
    triggerType: f.triggerType,
    triggerCron: f.triggerType === 'SCHEDULE' ? f.triggerCron : null,
    alertOnFailure: f.alertOnFailure,
    alertOnSuccess: f.alertOnSuccess,
    autoBlock: f.autoBlock,
    status: f.status,
    ruleCount: totalBoundRules.value,
    tableCount: availableTables.value.length,
  };
}

// ---- 关闭重置 ----
function handleClosed() {
  recordId.value = undefined;
  formValues.value = {
    status: 'DRAFT',
    triggerType: 'MANUAL',
    alertOnFailure: '1',
    alertOnSuccess: '0',
    autoBlock: '0',
    bindSensitivityLevel: '',
  };
  tableBindings.value = [];
  availableTables.value = [];
  availableTableSet.value = new Set();
  bindDsId.value = undefined;
  bindSchema.value = '';
  schemaList.value = [];
  currentStep.value = 0;
}

// ---- 步骤校验 ----
const step0Valid = computed(() => {
  const f = formValues.value;
  return f.planName && f.planCode;
});

const step1Valid = computed(() => {
  if (!bindDsId.value) return false;
  if (isPostgresBind.value && !bindSchema.value) return false;
  return availableTables.value.length > 0;
});

const step3Valid = computed(() => {
  if (formValues.value.triggerType === 'SCHEDULE') {
    return !!formValues.value.triggerCron;
  }
  return true;
});

function validateStep(step: number): boolean {
  if (step === 0) {
    if (!step0Valid.value) {
      message.warning('请完善基本信息（方案名称、方案编码）');
      return false;
    }
  }
  if (step === 1) {
    if (!step1Valid.value) {
      if (!bindDsId.value) {
        message.warning('请选择数据源');
      } else if (isPostgresBind.value && !bindSchema.value) {
        message.warning('PostgreSQL 数据源必须选择 Schema');
      } else {
        message.warning('请至少选择一张表');
      }
      return false;
    }
  }
  if (step === 3) {
    if (!step3Valid.value) {
      message.warning('请填写 Cron 表达式');
      return false;
    }
  }
  return true;
}

function handleWizardNext() {
  if (!validateStep(currentStep.value)) return;
  // 进入字段步骤时，确保已选表的字段都已加载
  if (currentStep.value === 1 && availableTables.value.length > 0) {
    for (const t of availableTables.value) {
      if (!tableBindings.value.find((tb) => tb.tableName === t)) {
        loadTableColumns(t);
      }
    }
  }
  wizardRef.value?.nextStep();
}

// ---- 预览 ----
const previewRuleCount = computed(() => totalBoundRules.value);
const previewTableCount = computed(() => availableTables.value.length);
const previewColumnCount = computed(() => selectedColumnCount.value);

const triggerTypeLabel = computed(
  () => triggerTypeLabelMap[formValues.value.triggerType || ''] || '-',
);
const dsLabel = computed(
  () => datasourceOptions.value.find((d) => d.value === bindDsId.value)?.label || '-',
);

// ---- 规则过滤 ----
const ruleFilterOptions = computed(() => {
  let list = allRules.value as any[];
  if (ruleFilterKeyword.value) {
    const kw = ruleFilterKeyword.value.toLowerCase();
    list = list.filter(
      (r) =>
        (r.ruleName || '').toLowerCase().includes(kw) ||
        (r.ruleCode || '').toLowerCase().includes(kw),
    );
  }
  return list;
});

</script>

<template>
  <BasicDrawer :title="title" class="w-[1100px]" :loading="submitting">
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
          <Form.Item label="方案名称" required>
            <Input v-model:value="formValues.planName" placeholder="请输入方案名称" />
          </Form.Item>
          <Form.Item label="方案编码" required>
            <Input
              v-model:value="formValues.planCode"
              placeholder="请输入唯一编码"
              :disabled="!!recordId"
            />
          </Form.Item>
          <Form.Item label="方案描述">
            <Input.TextArea
              v-model:value="formValues.planDesc"
              placeholder="请输入方案描述"
              :rows="3"
            />
          </Form.Item>
          <Form.Item label="状态">
            <Radio.Group v-model:value="formValues.status">
              <Radio value="DRAFT">草稿</Radio>
              <Radio value="PUBLISHED">已发布</Radio>
            </Radio.Group>
          </Form.Item>
        </Form>
      </template>

      <!-- Step 1: 绑定表范围 -->
      <template #step-1>
        <div class="space-y-4">
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-700">
            <InfoCircleOutlined class="mr-1" />
            选择<strong>数据源</strong>后，再选择要质检的<strong>目标表</strong>。
            PostgreSQL 数据源请先选择 <strong>Schema</strong> 再选表。
          </div>

          <div class="grid grid-cols-2 gap-4">
            <Form.Item label="数据源" required class="mb-0">
              <Select
                v-model:value="bindDsId"
                :options="datasourceOptions"
                placeholder="请选择数据源"
                show-search
                :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
                @change="onDsChange"
              />
            </Form.Item>
            <Form.Item v-if="isPostgresBind" label="Schema" required class="mb-0">
              <Select
                v-model:value="bindSchema"
                :options="schemaOptions"
                placeholder="请选择 Schema"
                show-search
                :loading="schemaLoading"
                :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
                :disabled="!bindDsId"
                @change="onSchemaChange"
              />
            </Form.Item>
            <Form.Item v-else label="质检数据层" class="mb-0">
              <Input v-model:value="formValues.layerCode" placeholder="如: ODS, DWD, DWS" />
            </Form.Item>
          </div>

          <!-- 敏感等级绑定（可选） -->
          <div class="border border-gray-200 rounded-lg p-4 bg-gray-50">
            <div class="text-sm font-medium text-gray-700 mb-3">敏感等级绑定（可选）</div>
            <p class="text-xs text-gray-500 mb-3">
              选择后将只为指定敏感等级的字段绑定质检规则（需先在「数据安全 → 敏感字段识别」完成扫描）。
            </p>
            <div class="flex gap-3 flex-wrap">
              <label
                v-for="opt in sensitivityLevelOptions"
                :key="opt.value"
                class="flex items-center gap-2 px-3 py-2 rounded-lg border cursor-pointer transition-all text-sm"
                :class="
                  formValues.bindSensitivityLevel === opt.value
                    ? 'border-current font-medium'
                    : 'border-gray-200 hover:border-gray-300'
                "
                :style="
                  formValues.bindSensitivityLevel === opt.value
                    ? {
                        borderColor: sensitivityTextColor[opt.value],
                        color: sensitivityTextColor[opt.value],
                        backgroundColor: sensitivityBgColor[opt.value],
                      }
                    : {}
                "
              >
                <input
                  type="radio"
                  :value="opt.value"
                  v-model="formValues.bindSensitivityLevel"
                  class="hidden"
                />
                {{ opt.label }}
              </label>
            </div>
          </div>

          <Form.Item label="目标表（多选）" required>
            <Select
              v-model:value="availableTables"
              mode="multiple"
              :placeholder="tableSelectPlaceholder"
              :loading="tableLoading"
              show-search
              :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
              :disabled="!bindDsId || (isPostgresBind && !bindSchema)"
              option-label-prop="label"
              @change="onTableSelectChange"
            >
              <Select.Option
                v-for="t in availableTables"
                :key="t"
                :value="t"
                :label="t"
              >
                <div class="flex justify-between items-center">
                  <span>{{ t }}</span>
                  <Tag v-if="tableBindings.find((tb) => tb.tableName === t)" size="small" color="green">
                    {{ tableBindings.find((tb) => tb.tableName === t)?.columns?.length || 0 }} 列
                  </Tag>
                </div>
              </Select.Option>
            </Select>
            <div class="text-xs text-gray-500 mt-1">
              已选择
              <strong>{{ availableTables.length }}</strong>
              张表，共
              <strong>{{ selectedColumnCount }}</strong>
              个字段（进入下一步后可绑定规则）
            </div>
          </Form.Item>
        </div>
      </template>

      <!-- Step 2: 字段与规则 -->
      <template #step-2>
        <div class="space-y-4">
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-700">
            <InfoCircleOutlined class="mr-1" />
            以下列出所有已选表的字段，点击「绑定规则」为字段关联质检规则。
          </div>

          <div v-if="tableBindings.length === 0" class="text-center text-gray-400 py-12">
            暂无字段，请返回上一步选择数据源和表
          </div>

          <Table
            v-else
            :data-source="flattenedRows"
            :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` }"
            size="middle"
            bordered
            :scroll="{ x: 900, y: 500 }"
            :row-key="(r: FlatRow) => r.key"
          >
            <Table.Column title="#" width="50" align="center" fixed="left">
              <template #default="{ index }">{{ index + 1 }}</template>
            </Table.Column>
            <Table.Column title="表名" dataIndex="tableName" width="200" fixed="left" ellipsis />
            <Table.Column title="字段名" dataIndex="columnName" width="160" ellipsis />
            <Table.Column title="已绑定规则" min-width="280">
              <template #default="{ record }">
                <Space wrap size="small">
                  <Tag
                    v-for="r in record.rules"
                    :key="r.id"
                    :color="r.ruleStrength === 'STRONG' ? 'red' : 'orange'"
                    closable
                    @close="removeRule(record.tableName, record.columnName, r.id)"
                  >
                    {{ r.ruleName }}
                  </Tag>
                  <span v-if="!record.rules || record.rules.length === 0" class="text-gray-400 text-xs">
                    未绑定
                  </span>
                </Space>
              </template>
            </Table.Column>
            <Table.Column title="规则数" width="80" align="center">
              <template #default="{ record }">
                <span
                  v-if="record.rules && record.rules.length > 0"
                  class="inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-medium"
                  :class="
                    record.rules.length > 2
                      ? 'bg-red-100 text-red-600'
                      : 'bg-orange-100 text-orange-600'
                  "
                >
                  {{ record.rules.length }}
                </span>
                <span v-else class="text-gray-300">—</span>
              </template>
            </Table.Column>
            <Table.Column title="操作" width="140" align="center" fixed="right">
              <template #default="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="openRuleSelector(record.tableName, record.columnName)"
                >
                  {{ record.rules && record.rules.length > 0 ? '编辑规则' : '绑定规则' }}
                </a-button>
              </template>
            </Table.Column>
          </Table>
        </div>
      </template>

      <!-- Step 3: 调度配置 -->
      <template #step-3>
        <div class="space-y-4">
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-700">
            <p class="mb-2 font-medium">Cron 表达式示例：</p>
            <ul class="list-disc list-inside space-y-1">
              <li
                v-for="ex in cronExamples"
                :key="ex.value"
                class="cursor-pointer hover:text-blue-900"
                @click="formValues.triggerCron = ex.value"
              >
                {{ ex.label }}：
                <code class="bg-blue-100 px-1 rounded">{{ ex.value }}</code>
              </li>
            </ul>
          </div>

          <Form :model="formValues" layout="vertical">
            <Form.Item label="触发方式">
              <Radio.Group v-model:value="formValues.triggerType" :options="triggerTypeOptions" />
            </Form.Item>

            <Form.Item
              v-if="formValues.triggerType === 'SCHEDULE'"
              label="Cron 表达式"
              required
            >
              <Input
                v-model:value="formValues.triggerCron"
                placeholder="如: 0 0 2 * * ?"
              />
            </Form.Item>

            <a-divider>告警配置</a-divider>

            <div class="grid grid-cols-2 gap-4">
              <Form.Item label="执行成功通知" class="mb-0">
                <Radio.Group v-model:value="formValues.alertOnSuccess">
                  <Radio value="1">通知</Radio>
                  <Radio value="0">不通知</Radio>
                </Radio.Group>
              </Form.Item>
              <Form.Item label="执行失败通知" class="mb-0">
                <Radio.Group v-model:value="formValues.alertOnFailure">
                  <Radio value="1">通知</Radio>
                  <Radio value="0">不通知</Radio>
                </Radio.Group>
              </Form.Item>
            </div>

            <Form.Item label="强规则失败" class="mt-4">
              <Radio.Group v-model:value="formValues.autoBlock">
                <Radio value="1">阻塞任务</Radio>
                <Radio value="0">仅记录</Radio>
              </Radio.Group>
              <div class="text-xs text-gray-500 mt-1">
                强规则失败时是否阻塞下游任务执行
              </div>
            </Form.Item>
          </Form>
        </div>
      </template>

      <!-- Step 4: 预览保存 -->
      <template #step-4>
        <div class="space-y-4">
          <div class="text-sm text-gray-500 mb-2">请确认以下配置，确认无误后点击保存。</div>
          <div class="bg-gray-50 rounded-lg p-4 space-y-4">
            <div>
              <div class="text-base font-medium text-gray-800 mb-2">基本信息</div>
              <div class="grid grid-cols-2 gap-y-2 text-sm">
                <div>
                  <span class="text-gray-500">方案名称：</span>
                  <span>{{ formValues.planName || '-' }}</span>
                </div>
                <div>
                  <span class="text-gray-500">方案编码：</span>
                  <code class="bg-gray-200 px-1 rounded">{{ formValues.planCode || '-' }}</code>
                </div>
                <div>
                  <span class="text-gray-500">状态：</span>
                  <Tag :color="formValues.status === 'PUBLISHED' ? 'green' : 'default'">
                    {{ formValues.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                  </Tag>
                </div>
                <div>
                  <span class="text-gray-500">数据源：</span>
                  <span>{{ dsLabel }}</span>
                </div>
                <div class="col-span-2">
                  <span class="text-gray-500">描述：</span>
                  <span>{{ formValues.planDesc || '-' }}</span>
                </div>
              </div>
            </div>

            <div>
              <div class="text-base font-medium text-gray-800 mb-2">绑定范围</div>
              <div class="grid grid-cols-2 gap-y-2 text-sm">
                <div>
                  <span class="text-gray-500">数据源ID：</span>
                  <span>{{ bindDsId || '-' }}</span>
                </div>
                <div>
                  <span class="text-gray-500">涉及表：</span>
                  <span>{{ previewTableCount }} 张</span>
                </div>
                <div>
                  <span class="text-gray-500">涉及字段：</span>
                  <span>{{ previewColumnCount }} 个</span>
                </div>
                <div v-if="formValues.bindSensitivityLevel">
                  <span class="text-gray-500">敏感等级：</span>
                  <Tag
                    :style="{
                      color: sensitivityTextColor[formValues.bindSensitivityLevel],
                      backgroundColor: sensitivityBgColor[formValues.bindSensitivityLevel],
                    }"
                  >
                    {{ formValues.bindSensitivityLevel }}
                  </Tag>
                </div>
              </div>
              <div v-if="availableTables.length > 0" class="mt-2">
                <Tag v-for="t in availableTables.slice(0, 10)" :key="t" size="small" class="mb-1">
                  {{ t }}
                </Tag>
                <Tag v-if="availableTables.length > 10" size="small">
                  +{{ availableTables.length - 10 }} 更多
                </Tag>
              </div>
            </div>

            <div>
              <div class="text-base font-medium text-gray-800 mb-2">调度配置</div>
              <div class="grid grid-cols-2 gap-y-2 text-sm">
                <div>
                  <span class="text-gray-500">触发方式：</span>
                  <Tag color="blue">{{ triggerTypeLabel }}</Tag>
                </div>
                <div v-if="formValues.triggerType === 'SCHEDULE'">
                  <span class="text-gray-500">Cron：</span>
                  <code class="bg-gray-200 px-1 rounded">{{ formValues.triggerCron || '-' }}</code>
                </div>
                <div>
                  <span class="text-gray-500">失败通知：</span>
                  <span>{{ formValues.alertOnFailure === '1' ? '是' : '否' }}</span>
                </div>
                <div>
                  <span class="text-gray-500">强规则阻塞：</span>
                  <span>{{ formValues.autoBlock === '1' ? '是' : '否' }}</span>
                </div>
              </div>
            </div>

            <div>
              <div class="text-base font-medium text-gray-800 mb-2">
                规则绑定
                <span class="text-sm font-normal text-gray-500 ml-2">
                  共 {{ previewRuleCount }} 条规则
                </span>
              </div>
              <div
                v-if="previewRuleCount === 0"
                class="text-sm text-orange-500 bg-orange-50 rounded px-3 py-2"
              >
                尚未绑定任何规则，建议在「字段与规则」步骤中绑定质检规则
              </div>
              <div v-else class="space-y-1">
                <div
                  v-for="tb in tableBindings"
                  :key="tb.tableName"
                  class="text-sm"
                >
                  <div class="font-medium text-gray-700">{{ tb.tableName }}</div>
                  <div class="ml-3 flex flex-wrap gap-1 mt-1">
                    <Tag
                      v-for="col in tb.columns"
                      :key="col"
                      size="small"
                      :color="(tb.columnRules[col]?.length || 0) > 0 ? 'green' : 'default'"
                    >
                      {{ col }}
                      <span v-if="tb.columnRules[col]?.length > 0" class="ml-1 text-xs">
                        ({{ tb.columnRules[col].length }})
                      </span>
                    </Tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </WizardDrawer>

    <!-- 规则选择器弹窗 -->
    <Modal
      v-model:open="ruleSelectorVisible"
      title="绑定质检规则"
      width="900px"
      ok-text="确认绑定"
      cancel-text="取消"
      @ok="confirmRuleBinding"
    >
      <div class="mb-4">
        <div class="text-sm text-gray-600 mb-2">
          当前字段：
          <code class="bg-gray-100 px-1 rounded">{{ ruleSelectorTable }}</code>
          .
          <code class="bg-gray-100 px-1 rounded">{{ ruleSelectorColumn }}</code>
        </div>
        <Input
          v-model:value="ruleFilterKeyword"
          placeholder="搜索规则名称或编码"
          allow-clear
          @input="ruleFilterKeyword = ruleFilterKeyword"
        >
          <template #prefix><SearchOutlined /></template>
        </Input>
      </div>

      <Table
        :loading="ruleSelectorLoading"
        :data-source="ruleFilterOptions"
        :pagination="{ pageSize: 10, showSizeChanger: false, showTotal: (total: number) => `共 ${total} 条` }"
        :row-selection="{
          selectedRowKeys: selectedRuleIds,
          onChange: (keys: any[]) => { selectedRuleIds = keys; },
        }"
        :row-key="(r: any) => r.id"
        size="small"
        :scroll="{ y: 400 }"
      >
        <Table.Column title="规则名称" dataIndex="ruleName" width="200" ellipsis />
        <Table.Column title="规则编码" dataIndex="ruleCode" width="150" ellipsis />
        <Table.Column title="规则类型" dataIndex="ruleType" width="100" />
        <Table.Column title="适用级别" dataIndex="applyLevel" width="90" />
        <Table.Column title="告警级别" dataIndex="errorLevel" width="90">
          <template #default="{ text }">
            <Tag v-if="text" :color="errorLevelColor[text]">{{ text }}</Tag>
          </template>
        </Table.Column>
        <Table.Column title="规则强度" dataIndex="ruleStrength" width="90">
          <template #default="{ text }">
            <Tag v-if="text" :color="ruleStrengthColor[text]">{{ text }}</Tag>
          </template>
        </Table.Column>
        <Table.Column title="描述" dataIndex="ruleDesc" ellipsis />
      </Table>
    </Modal>
  </BasicDrawer>
</template>
