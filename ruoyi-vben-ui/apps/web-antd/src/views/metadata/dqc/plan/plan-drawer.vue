<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import {
  Form,
  Input,
  Radio,
  Select,
  message,
  Modal,
  Table,
  Tag,
  Space,
  Descriptions,
  Popover,
  Switch,
  Divider,
  Tooltip,
} from 'ant-design-vue';
import {
  SearchOutlined,
  BulbOutlined,
  ClockCircleOutlined,
  ScheduleOutlined,
  ApiOutlined,
  QuestionCircleOutlined,
  CheckCircleOutlined,
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

const triggerTypeLabelMap: Record<string, string> = {
  MANUAL: '手动触发',
  SCHEDULE: '定时触发',
  API: 'API 触发',
};

const triggerTypeTagColor: Record<string, string> = {
  MANUAL: 'blue',
  SCHEDULE: 'purple',
  API: 'cyan',
};

/** 敏感等级（无 Emoji，便于对齐与无障碍） */
const sensitivityLevelOptions = [
  { value: 'L4', title: 'L4 机密', hint: '最高敏感', level: 'L4' as const },
  { value: 'L3', title: 'L3 敏感', hint: '高敏感', level: 'L3' as const },
  { value: 'L2', title: 'L2 内部', hint: '内部可见', level: 'L2' as const },
  { value: 'L1', title: 'L1 公开', hint: '可公开', level: 'L1' as const },
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

/** 与 bgdata 质检方案向导一致：主标题 + 副标题 */
const steps = [
  { title: '基本信息', description: '方案名称和描述' },
  { title: '绑定表范围', description: '选择检测目标' },
  { title: '字段与规则', description: '全列展示并绑定质检规则' },
  { title: '调度配置', description: '执行和告警配置' },
  { title: '预览保存', description: '确认并创建' },
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
/** 数据源下全部表名（仅作下拉选项，勿与已选混淆） */
const tableCatalog = ref<string[]>([]);
/** 用户勾选要质检的表 */
const selectedTables = ref<string[]>([]);
const selectedTableSet = ref<Set<string>>(new Set());

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

const title = computed(() => (recordId.value ? '编辑方案' : '新建方案'));

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
        tableCatalog.value = [];
        selectedTables.value = [];
        selectedTableSet.value = new Set();
        tableBindings.value = [];
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

            if (bv.dsId) {
              try {
                const catalog = await datasourceTables(
                  Number(bv.dsId),
                  bv.schema || undefined,
                );
                tableCatalog.value = catalog || [];
              } catch (_) {
                tableCatalog.value = [];
              }
            }
            if (bv.tables && Array.isArray(bv.tables)) {
              selectedTables.value = [...bv.tables];
              selectedTableSet.value = new Set(bv.tables);
              for (const tbl of bv.tables) {
                await loadTableColumns(tbl);
              }
            }
            if (bv.bindSensitivityLevel) {
              formValues.value.bindSensitivityLevel = bv.bindSensitivityLevel;
            }
          } catch (_) {}
        }

        // 无 bindValue / 旧数据：仍根据 dsId 拉表清单，避免编辑时下拉为空
        if (bindDsId.value) {
          const ds = datasourceOptions.value.find((d) => d.value === bindDsId.value);
          if (ds) dsType.value = ds.dsType;
          if (
            tableCatalog.value.length === 0 &&
            (!isPostgresBind.value || bindSchema.value)
          ) {
            try {
              const catalog = await datasourceTables(
                bindDsId.value,
                bindSchema.value || undefined,
              );
              tableCatalog.value = catalog || [];
            } catch (_) {
              tableCatalog.value = [];
            }
          }
          if (isPostgresBind.value && schemaList.value.length === 0) {
            schemaLoading.value = true;
            try {
              const schemas = await datasourceSchemas(bindDsId.value);
              schemaList.value = schemas?.data || schemas || [];
            } catch (_) {}
            finally {
              schemaLoading.value = false;
            }
          }
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
        tableCatalog.value = [];
        selectedTables.value = [];
        selectedTableSet.value = new Set();
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
  tableCatalog.value = [];
  selectedTables.value = [];
  selectedTableSet.value = new Set();
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
    tableCatalog.value = tables || [];
  } catch (_) {
    tableCatalog.value = [];
    message.warning('加载表清单失败，请检查数据源连接与权限');
  } finally {
    tableLoading.value = false;
  }
}

// ---- Schema 切换，重新加载表列表 ----
async function onSchemaChange(schema: string) {
  bindSchema.value = schema;
  tableBindings.value = [];
  tableCatalog.value = [];
  selectedTables.value = [];
  selectedTableSet.value = new Set();
  if (!bindDsId.value) return;
  tableLoading.value = true;
  try {
    const tables = await datasourceTables(bindDsId.value, schema);
    tableCatalog.value = tables || [];
  } catch (_) {
    tableCatalog.value = [];
    message.warning('加载表清单失败，请检查 Schema 或权限');
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
    message.error(`加载表「${tableName}」的字段失败，请检查数据源、元数据同步或账号权限`);
    let binding = tableBindings.value.find((tb) => tb.tableName === tableName);
    if (!binding) {
      binding = { tableName, columns: [], columnRules: {} };
      tableBindings.value.push(binding);
    }
  }
}

// ---- 选择表：加入 tableBindings 并加载字段 ----
async function onTableSelectChange(selected: string[]) {
  const prev = selectedTableSet.value;
  const next = selected || [];
  const nextSet = new Set(next);
  const added = next.filter((t) => !prev.has(t));

  for (const t of added) {
    await loadTableColumns(t);
  }
  selectedTableSet.value = nextSet;
  selectedTables.value = next;
  tableBindings.value = tableBindings.value.filter((tb) => next.includes(tb.tableName));
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
  if (!bindDsId.value) {
    message.warning('请先完成「绑定表范围」并选择数据源');
    return;
  }
  ruleSelectorTable.value = tableName;
  ruleSelectorColumn.value = columnName;
  ruleFilterKeyword.value = '';
  ruleSelectorLoading.value = true;
  selectedRuleIds.value = [];

  // 获取当前列已绑定的规则 ID
  const binding = tableBindings.value.find((tb) => tb.tableName === tableName);
  if (binding?.columnRules[columnName]) {
    selectedRuleIds.value = binding.columnRules[columnName].map(
      (r: any) => r.ruleId ?? r.id,
    );
  }

  try {
    // 必须传 targetDsId：后端无此参数时用「可访问数据源 ID 列表」过滤；该列表在数据权限下可能为空，
    // 会导致 SQL 恒为 id=-1，弹窗无数据。与 bgdata 一致，按当前方案数据源拉取可绑定的规则。
    // ⚠️ 调试：先用原生 fetch 打印原始响应，确认字段值
    const debugUrl = `/system/metadata/dqc/rule/list?pageNum=1&pageSize=10&enabled=0&targetDsId=${bindDsId.value}`;
    console.log('[DQC] 调试 fetch URL:', debugUrl);
    const raw = await fetch(debugUrl, { credentials: 'include' });
    const rawJson = await raw.json();
    console.log('[DQC] 原始响应（完整）:', JSON.stringify(rawJson));
    console.log('[DQC] enabled 字段值示例:', rawJson?.rows?.[0]?.enabled, '| rows 总数:', rawJson?.total);

    const res = await dqcRuleList({
      pageNum: 1,
      pageSize: 999,
      enabled: '0',
      targetDsId: bindDsId.value,
    });
    const rows = res?.rows;
    allRules.value = Array.isArray(rows) ? rows : Array.isArray(res) ? res : [];
    if (allRules.value.length === 0) {
      message.info('当前数据源下无可绑定的规则，请检查规则「目标数据源」是否匹配');
    }
  } catch (e: any) {
    allRules.value = [];
    console.error('[DQC] 加载规则列表异常:', e);
    message.error(e?.message || '加载规则列表失败');
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
    tables: selectedTables.value || [],
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
    tableCount: selectedTables.value.length,
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
  tableCatalog.value = [];
  selectedTables.value = [];
  selectedTableSet.value = new Set();
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
  return selectedTables.value.length > 0;
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

async function handleWizardNext() {
  if (!validateStep(currentStep.value)) return;
  // 进入字段步骤时，确保已选表的字段都已加载
  if (currentStep.value === 1 && selectedTables.value.length > 0) {
    await Promise.all(
      selectedTables.value
        .filter((t) => !tableBindings.value.find((tb) => tb.tableName === t))
        .map((t) => loadTableColumns(t)),
    );
    if (selectedColumnCount.value === 0) {
      message.warning(
        '当前已选表未加载到任何字段，请确认数据源元数据已采集，或检查库表权限后再试。',
      );
    }
  }
  wizardRef.value?.nextStep();
}

// ---- 预览 ----
const previewRuleCount = computed(() => totalBoundRules.value);
const previewTableCount = computed(() => selectedTables.value.length);
const previewColumnCount = computed(() => selectedColumnCount.value);

const strongRuleCount = computed(() => {
  let n = 0;
  for (const tb of tableBindings.value) {
    for (const rules of Object.values(tb.columnRules)) {
      for (const r of rules || []) {
        if ((r as any).ruleStrength === 'STRONG') n++;
      }
    }
  }
  return n;
});

const weakRuleCount = computed(() =>
  Math.max(0, previewRuleCount.value - strongRuleCount.value),
);

const triggerTypeLabel = computed(
  () => triggerTypeLabelMap[formValues.value.triggerType || ''] || '-',
);
const dsLabel = computed(
  () => datasourceOptions.value.find((d) => d.value === bindDsId.value)?.label || '-',
);

function setAlertSuccess(checked: boolean) {
  formValues.value.alertOnSuccess = checked ? '1' : '0';
}
function setAlertFailure(checked: boolean) {
  formValues.value.alertOnFailure = checked ? '1' : '0';
}
function setAutoBlock(checked: boolean) {
  formValues.value.autoBlock = checked ? '1' : '0';
}

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
  <BasicDrawer :title="title" class="plan-wizard-drawer w-[min(1680px,98vw)]" :loading="submitting">
    <WizardDrawer
      ref="wizardRef"
      step-variant="horizontal"
      :steps="steps"
      v-model:currentStep="currentStep"
      :loading="submitting"
      @finish="handleSubmit"
      @next="handleWizardNext"
    >
      <!-- Step 0: 基本信息（bgdata 布局） -->
      <template #step-0>
        <div class="plan-step-guide">
          <BulbOutlined />
          <span>配置方案的基本信息，包括名称、编码和描述</span>
        </div>
        <Form
          :model="formValues"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
        >
          <Form.Item label="方案名称" required>
            <Input
              v-model:value="formValues.planName"
              placeholder="请输入方案名称，建议使用业务可读名称"
            />
          </Form.Item>
          <Form.Item label="方案编码" required>
            <Input
              v-model:value="formValues.planCode"
              placeholder="唯一编码，创建后不可修改"
              :disabled="!!recordId"
            />
          </Form.Item>
          <Form.Item label="方案描述" name="planDesc">
            <Input.TextArea
              v-model:value="formValues.planDesc"
              :rows="3"
              placeholder="描述该方案的作用范围和目标"
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
          <div class="plan-step-guide plan-step-guide-multiline">
            <BulbOutlined />
            <span>
              请选择<strong>数据源</strong>与<strong>目标表</strong>；下一步将同页列出字段并绑定规则（执行 SQL 会带库名/Schema）。
              <strong>PostgreSQL</strong> 数据源必须先选择 <strong>Schema</strong>。
            </span>
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
            <Form.Item v-else class="mb-0">
              <template #label>
                <span>分层标签</span>
                <Tooltip
                  title="与「选哪张表」无关：仅用于方案列表里的展示/筛选（如 ODS、DWD）。若数据源配置里已有分层可留空；非数仓分层场景也可不填。"
                >
                  <QuestionCircleOutlined class="ml-1 text-gray-400 align-middle cursor-help" />
                </Tooltip>
              </template>
              <Input v-model:value="formValues.layerCode" placeholder="可选，如 ODS / DWD / DWS" />
            </Form.Item>
          </div>

          <!-- 敏感等级绑定（可选）：卡片式单选 +「不限制」 -->
          <div class="sens-bind-panel">
            <div class="sens-bind-panel-head">
              <span class="sens-bind-panel-title">敏感等级过滤</span>
              <Tag class="sens-bind-optional-tag">可选</Tag>
            </div>
            <p class="sens-bind-panel-desc">
              开启后，仅对<strong>该等级</strong>的敏感字段重点做规则绑定策略（需先在「数据安全 → 敏感字段识别」完成扫描）。不选表示不按等级过滤。
            </p>
            <div class="sens-level-grid" role="radiogroup" aria-label="敏感等级">
              <button
                type="button"
                class="sens-level-card sens-level-card-none"
                :class="{ 'is-active': !formValues.bindSensitivityLevel }"
                @click="formValues.bindSensitivityLevel = ''"
              >
                <span class="sens-level-card-title">不限制</span>
                <span class="sens-level-card-hint">按已选表的全部字段列示</span>
              </button>
              <button
                v-for="opt in sensitivityLevelOptions"
                :key="opt.value"
                type="button"
                class="sens-level-card"
                :class="{ 'is-active': formValues.bindSensitivityLevel === opt.value }"
                :style="{
                  '--sens-accent': sensitivityTextColor[opt.value],
                  '--sens-bg': sensitivityBgColor[opt.value],
                }"
                @click="formValues.bindSensitivityLevel = opt.value"
              >
                <span class="sens-level-dot" :style="{ background: sensitivityTextColor[opt.value] }" />
                <span class="sens-level-card-title">{{ opt.title }}</span>
                <span class="sens-level-card-hint">{{ opt.hint }}</span>
              </button>
            </div>
          </div>

          <Form.Item label="目标表（多选）" required>
            <Select
              v-model:value="selectedTables"
              mode="multiple"
              :placeholder="tableSelectPlaceholder"
              :loading="tableLoading"
              show-search
              :filter-option="(input: string, option: any) =>
                String(option?.label ?? '')
                  .toLowerCase()
                  .includes(input.toLowerCase())"
              :disabled="!bindDsId || (isPostgresBind && !bindSchema)"
              :max-tag-count="8"
              option-label-prop="label"
              option-filter-prop="label"
              @change="onTableSelectChange"
            >
              <Select.Option
                v-for="t in tableCatalog"
                :key="t"
                :value="t"
                :label="t"
              >
                <div class="flex justify-between items-center gap-2">
                  <span class="truncate">{{ t }}</span>
                  <Tag
                    v-if="selectedTables.includes(t) && tableBindings.find((tb) => tb.tableName === t)"
                    size="small"
                    color="green"
                  >
                    {{ tableBindings.find((tb) => tb.tableName === t)?.columns?.length || 0 }} 列
                  </Tag>
                </div>
              </Select.Option>
            </Select>
            <div class="text-xs text-gray-500 mt-1 space-y-0.5">
              <div>
                库中可选 <strong>{{ tableCatalog.length }}</strong> 张表 · 已勾选
                <strong>{{ selectedTables.length }}</strong> 张 · 已加载字段共
                <strong>{{ selectedColumnCount }}</strong> 列（下一步绑定规则）
              </div>
              <div class="text-gray-400">
                不会默认全选；表很多时请用搜索缩小范围后勾选。
              </div>
            </div>
          </Form.Item>
        </div>
      </template>

      <!-- Step 2: 字段与规则 -->
      <template #step-2>
        <div class="space-y-4">
          <div class="plan-step-guide plan-step-guide-lg">
            <BulbOutlined />
            <span>
              数据源 <strong>{{ dsLabel }}</strong>
              <template v-if="bindSchema"> · Schema：{{ bindSchema }}</template>
              。下列为各表<strong>全部已选字段</strong>；对需要质检的字段点击「绑定规则」关联质检规则。
            </span>
          </div>

          <div v-if="tableBindings.length === 0" class="wizard-empty-hint">
            请先在「绑定表范围」中选择数据源与表
          </div>

          <Table
            v-else
            class="field-rules-table"
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
                  <span
                    v-if="!record.rules || record.rules.length === 0"
                    class="text-muted-field text-xs"
                  >
                    未绑定
                  </span>
                </Space>
              </template>
            </Table.Column>
            <Table.Column title="规则数" width="80" align="center">
              <template #default="{ record }">
                <span
                  v-if="record.rules && record.rules.length > 0"
                  class="field-rule-count-pill"
                  :title="`已绑定 ${record.rules.length} 条规则`"
                >
                  {{ record.rules.length > 99 ? '99+' : record.rules.length }}
                </span>
                <span v-else class="field-rule-count-empty" title="未绑定规则">—</span>
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
          <div
            v-if="tableBindings.length > 0 && flattenedRows.length === 0"
            class="wizard-empty-hint"
          >
            暂无字段，请返回上一步检查表选择或数据源连接
          </div>
        </div>
      </template>

      <!-- Step 3: 调度配置（bgdata：图标单选 + 开关告警） -->
      <template #step-3>
        <div class="space-y-4">
          <div class="plan-step-guide">
            <BulbOutlined />
            <span>配置方案的执行触发方式及告警通知策略</span>
          </div>
          <Form
            :model="formValues"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <Form.Item label="触发方式">
              <Radio.Group v-model:value="formValues.triggerType" class="plan-trigger-group">
                <Radio value="MANUAL" class="plan-trigger-radio">
                  <span class="trigger-option">
                    <span class="trigger-icon-wrap"><ClockCircleOutlined /></span>
                    <span>
                      <strong>手动触发</strong>
                      <span class="trigger-desc">手动点击执行按钮触发检测</span>
                    </span>
                  </span>
                </Radio>
                <Radio value="SCHEDULE" class="plan-trigger-radio">
                  <span class="trigger-option">
                    <span class="trigger-icon-wrap"><ScheduleOutlined /></span>
                    <span>
                      <strong>定时触发</strong>
                      <span class="trigger-desc">按 Cron 表达式定时执行</span>
                    </span>
                  </span>
                </Radio>
                <Radio value="API" class="plan-trigger-radio">
                  <span class="trigger-option">
                    <span class="trigger-icon-wrap"><ApiOutlined /></span>
                    <span>
                      <strong>API 触发</strong>
                      <span class="trigger-desc">通过外部接口调用触发</span>
                    </span>
                  </span>
                </Radio>
              </Radio.Group>
            </Form.Item>

            <Form.Item
              v-if="formValues.triggerType === 'SCHEDULE'"
              label="Cron 表达式"
            >
              <Input v-model:value="formValues.triggerCron" placeholder="0 0 2 * * ?">
                <template #suffix>
                  <Popover title="常用表达式" trigger="click">
                    <template #content>
                      <div class="cron-helper">
                        <div @click="formValues.triggerCron = '0 0 2 * * ?'">
                          每天凌晨 2 点：<code>0 0 2 * * ?</code>
                        </div>
                        <div @click="formValues.triggerCron = '0 30 8 * * ?'">
                          每天 8:30：<code>0 30 8 * * ?</code>
                        </div>
                        <div @click="formValues.triggerCron = '0 0/30 * * * ?'">
                          每 30 分钟：<code>0 0/30 * * * ?</code>
                        </div>
                        <div @click="formValues.triggerCron = '0 0 2 ? * 1'">
                          每周一 2 点：<code>0 0 2 ? * 1</code>
                        </div>
                      </div>
                    </template>
                    <a class="text-primary text-sm cursor-pointer">常用 ▼</a>
                  </Popover>
                </template>
              </Input>
              <div class="field-tip">6 位 Cron：秒 分 时 日 月 周（? 表示不指定）</div>
            </Form.Item>

            <Divider>告警配置</Divider>

            <Form.Item label="执行成功">
              <Switch
                :checked="formValues.alertOnSuccess === '1'"
                @update:checked="setAlertSuccess"
              />
              <span class="switch-label">{{
                formValues.alertOnSuccess === '1' ? '通知' : '不通知'
              }}</span>
            </Form.Item>
            <Form.Item label="执行失败">
              <Switch
                :checked="formValues.alertOnFailure === '1'"
                @update:checked="setAlertFailure"
              />
              <span class="switch-label">{{
                formValues.alertOnFailure === '1' ? '通知' : '不通知'
              }}</span>
            </Form.Item>
            <Form.Item label="强规则失败">
              <Switch
                :checked="formValues.autoBlock === '1'"
                @update:checked="setAutoBlock"
              />
              <span class="switch-label">{{
                formValues.autoBlock === '1' ? '阻塞任务' : '仅记录'
              }}</span>
              <Tooltip title="强规则失败时是否阻塞下游任务执行">
                <span class="plan-help-icon-wrap">
                  <QuestionCircleOutlined class="plan-help-icon" />
                </span>
              </Tooltip>
            </Form.Item>
          </Form>
        </div>
      </template>

      <!-- Step 4: 预览保存（bgdata：总览 + 分区标题 + Descriptions） -->
      <template #step-4>
        <div class="plan-preview-wrap">
          <div class="text-sm text-gray-500 mb-3">
            请确认以下配置，确认无误后点击保存。
          </div>
          <div class="preview-section">
            <div class="preview-title">
              <CheckCircleOutlined />
              方案配置总览
            </div>

            <div class="detail-title">基本信息</div>
            <Descriptions bordered size="small" :column="2" class="mb-4">
              <Descriptions.Item label="方案名称">
                {{ formValues.planName || '—' }}
              </Descriptions.Item>
              <Descriptions.Item label="方案编码">
                <code class="plan-code-inline">{{ formValues.planCode || '—' }}</code>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag :color="formValues.status === 'PUBLISHED' ? 'success' : 'default'">
                  {{ formValues.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="数据源（实例）">
                {{ dsLabel }}
              </Descriptions.Item>
              <Descriptions.Item label="描述" :span="2">
                {{ formValues.planDesc || '—' }}
              </Descriptions.Item>
            </Descriptions>

            <div class="detail-title">绑定范围</div>
            <Descriptions bordered size="small" :column="2" class="mb-2">
              <Descriptions.Item label="数据源ID">
                {{ bindDsId ?? '—' }}
              </Descriptions.Item>
              <Descriptions.Item label="涉及表">
                {{ previewTableCount }} 张
                <span v-if="bindSchema" class="text-gray-400 text-xs ml-1">
                  ，Schema：{{ bindSchema }}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="涉及字段">
                {{ previewColumnCount }} 个
              </Descriptions.Item>
              <Descriptions.Item v-if="formValues.bindSensitivityLevel" label="敏感等级">
                <Tag
                  :style="{
                    color: sensitivityTextColor[formValues.bindSensitivityLevel],
                    backgroundColor: sensitivityBgColor[formValues.bindSensitivityLevel],
                    border: 'none',
                  }"
                >
                  {{ formValues.bindSensitivityLevel }}
                </Tag>
              </Descriptions.Item>
            </Descriptions>
            <div v-if="selectedTables.length > 0" class="preview-table-tags">
              <Tag v-for="t in selectedTables" :key="t" class="preview-entity-tag">
                {{ t }}
              </Tag>
            </div>

            <div class="detail-title">调度配置</div>
            <Descriptions bordered size="small" :column="2" class="mb-4">
              <Descriptions.Item label="触发方式">
                <Tag :color="triggerTypeTagColor[formValues.triggerType || ''] || 'blue'">
                  {{ triggerTypeLabel }}
                </Tag>
                <template v-if="formValues.triggerType === 'SCHEDULE' && formValues.triggerCron">
                  <span class="text-gray-500 text-xs ml-2">
                    Cron：<code class="plan-code-inline">{{ formValues.triggerCron }}</code>
                  </span>
                </template>
              </Descriptions.Item>
              <Descriptions.Item label="告警配置" :span="2">
                成功通知：{{ formValues.alertOnSuccess === '1' ? '是' : '否' }}，失败通知：{{
                  formValues.alertOnFailure === '1' ? '是' : '否'
                }}，强规则阻塞：{{ formValues.autoBlock === '1' ? '是' : '否' }}
              </Descriptions.Item>
            </Descriptions>

            <div class="detail-title">
              规则绑定
              <span class="detail-count">共 {{ previewRuleCount }} 条规则</span>
            </div>
            <div
              v-if="previewRuleCount === 0"
              class="plan-preview-alert"
            >
              尚未绑定任何规则，建议在「字段与规则」步骤中绑定质检规则
            </div>
            <Descriptions v-else bordered size="small" :column="1">
              <Descriptions.Item label="绑定规则">
                {{ previewRuleCount }} 条（强规则 {{ strongRuleCount }}，弱规则 {{ weakRuleCount }}）
              </Descriptions.Item>
            </Descriptions>
            <div v-if="previewRuleCount > 0" class="preview-rule-detail mt-2">
              <div v-for="tb in tableBindings" :key="tb.tableName" class="text-sm mb-2">
                <div class="font-medium text-gray-700">{{ tb.tableName }}</div>
                <div class="ml-1 flex flex-wrap gap-1 mt-1">
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

<style scoped>
/* bgdata 质检方案向导样式对齐 */
.plan-step-guide {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  color: #1677ff;
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.plan-step-guide-multiline {
  align-items: flex-start;
}
.plan-step-guide-lg {
  align-items: flex-start;
  line-height: 1.55;
}
.wizard-empty-hint {
  text-align: center;
  color: #8c8c8c;
  padding: 48px 16px;
  font-size: 14px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
}
.trigger-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
}
.trigger-icon-wrap {
  font-size: 20px;
  color: #1677ff;
}
.trigger-option strong {
  display: block;
  font-size: 14px;
  color: #1f1f1f;
}
.trigger-desc {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  font-weight: normal;
}
.plan-trigger-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.plan-trigger-group :deep(.ant-radio-wrapper) {
  margin-inline-end: 0;
  margin-bottom: 0;
  align-items: flex-start;
  height: auto;
  padding: 4px 0;
}
.field-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
.switch-label {
  margin-left: 10px;
  font-size: 13px;
  color: #595959;
}
.plan-help-icon-wrap {
  margin-left: 8px;
  display: inline-block;
  vertical-align: middle;
  cursor: help;
  color: #999;
}
.plan-help-icon {
  vertical-align: middle;
}
.cron-helper {
  font-size: 13px;
  min-width: 220px;
}
.cron-helper div {
  padding: 6px 0;
  cursor: pointer;
  color: #1677ff;
  border-bottom: 1px solid #f0f0f0;
}
.cron-helper div:last-child {
  border-bottom: none;
}
.cron-helper div:hover {
  color: #0958d9;
}
.cron-helper code {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  background: #f5f5f5;
  padding: 1px 4px;
  border-radius: 3px;
  margin-left: 8px;
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
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-count {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
}
.plan-code-inline {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}
.preview-table-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}
.preview-entity-tag {
  margin: 0;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
}
.plan-preview-alert {
  font-size: 13px;
  color: #d46b08;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
}
.field-rule-count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  font-size: 12px;
  font-weight: 600;
  background: #fff7e6;
  color: #d46b08;
}
.field-rule-count-empty {
  color: #d9d9d9;
}
.text-muted-field {
  color: #8c8c8c;
}
.plan-preview-wrap {
  padding-right: 4px;
}
.text-primary {
  color: #1677ff;
}

/* 敏感等级：面板 + 2×2 卡片，焦点环可键盘操作 */
.sens-bind-panel {
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 18px;
  background: linear-gradient(180deg, #fafafa 0%, #fff 48px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.sens-bind-panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.sens-bind-panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
}
.sens-bind-optional-tag {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  border-radius: 4px;
  color: #595959;
  background: #f5f5f5;
  border: none;
}
.sens-bind-panel-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.55;
  margin: 0 0 14px;
  max-width: 52rem;
}
.sens-level-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
@media (min-width: 900px) {
  .sens-level-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
.sens-level-card {
  position: relative;
  text-align: left;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 12px 12px 12px 14px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
  min-height: 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}
.sens-level-card:hover {
  border-color: #91caff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.08);
}
.sens-level-card:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px #1677ff;
}
.sens-level-card.is-active {
  border-color: var(--sens-accent, #1677ff);
  background: var(--sens-bg, #e6f4ff);
  box-shadow: 0 0 0 1px var(--sens-accent, #1677ff);
}
.sens-level-card-none.is-active {
  border-color: #1677ff;
  background: #e6f4ff;
  box-shadow: 0 0 0 1px #1677ff;
}
.sens-level-dot {
  position: absolute;
  left: 12px;
  top: 14px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.sens-level-card:not(.sens-level-card-none) {
  padding-left: 28px;
}
.sens-level-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.3;
}
.sens-level-card-hint {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.35;
}
.sens-level-card-none .sens-level-card-title {
  color: #1677ff;
}
</style>
