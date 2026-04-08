<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ApiOutlined,
  BarChartOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  DatabaseOutlined,
  EditOutlined,
  ExperimentOutlined,
  FileOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue';
import {
  Alert,
  Button,
  Checkbox,
  Descriptions,
  Form,
  Input,
  InputNumber,
  message,
  Radio,
  Select,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  dprofileTaskAdd,
  dprofileTaskInfo,
  dprofileTaskStart,
  dprofileTaskUpdate,
} from '#/api/metadata/dprofile/task';
import { metadataTableList } from '#/api/metadata/table';
import {
  datasourceColumns,
  datasourceEnabled,
  datasourceSchemas,
  datasourceTables,
} from '#/api/system/datasource';
import WizardDrawer from '#/components/metadata/WizardDrawer.vue';

const emit = defineEmits<{ reload: [] }>();

const steps = [
  { title: '探查配置', description: '选择数据源、目标表与探查级别' },
  { title: '基本信息', description: '填写任务名称与描述' },
  { title: '触发配置', description: '设置执行触发方式' },
  { title: '确认创建', description: '确认配置并提交' },
];

const profileLevelOptions = [
  {
    value: 'BASIC',
    name: '仅表级',
    desc: '快速获取基本信息，资源消耗低',
    icon: DatabaseOutlined,
    items: ['行数', '列数', '存储大小'],
  },
  {
    value: 'DETAILED',
    name: '表+列级',
    desc: '包含基础统计和分布分析',
    icon: BarChartOutlined,
    items: ['表级统计', '空值率', '唯一值', '高频值'],
  },
  {
    value: 'FULL',
    name: '完整探查',
    desc: '全量统计+异常检测',
    icon: ExperimentOutlined,
    items: ['完整统计', '直方图', '3σ异常检测', 'IQR异常检测'],
  },
];

const triggerTypeOptions = [
  {
    value: 'MANUAL',
    name: '手动触发',
    desc: '随时手动执行，适合临时探查',
    icon: EditOutlined,
    iconBg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)',
  },
  {
    value: 'SCHEDULE',
    name: '定时触发',
    desc: '按 Cron 自动执行，适合周期性监控',
    icon: ClockCircleOutlined,
    iconBg: 'linear-gradient(135deg, #1677FF 0%, #69C0FF 100%)',
  },
  {
    value: 'API',
    name: 'API 调用',
    desc: '由接口或流水线触发执行',
    icon: ApiOutlined,
    iconBg: 'linear-gradient(135deg, #13C2C2 0%, #36CFC9 100%)',
  },
];

const cronPresets = [
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天中午12点', value: '0 0 12 * * ?' },
  { label: '每周一', value: '0 0 2 ? * MON' },
  { label: '每月1号', value: '0 0 2 1 * ?' },
];

const recordId = ref<number>();
const currentStep = ref(0);
const submitting = ref(false);
const wizardRef = ref<InstanceType<typeof WizardDrawer>>();

const formValues = ref<Record<string, any>>({});
const datasourceOptions = ref<{ label: string; raw: any; value: number }[]>([]);
const pgSchema = ref<string>();
const schemaList = ref<string[]>([]);
const schemaLoading = ref(false);
const tablesLoading = ref(false);
const metadataMerged = ref<
  {
    columnCount?: number;
    tableAlias?: string;
    tableComment?: string;
    tableName: string;
  }[]
>([]);
const columnPreviewLoading = ref(false);
const columnPreview = ref<
  {
    checked?: boolean;
    columnComment?: string;
    columnName: string;
    dataType?: string;
  }[]
>([]);
const selectedTargetColumns = ref<Set<string>>(new Set());

const taskNameTouched = ref(false);
const executeAfterCreate = ref(true);

const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const title = computed(() =>
  recordId.value ? '编辑探查任务' : '新增探查任务',
);
const isEdit = computed(() => !!recordId.value);

const selectedDs = computed(
  () =>
    datasourceOptions.value.find((d) => d.value === formValues.value.dsId)?.raw,
);

const isPostgresDs = computed(
  () => (selectedDs.value?.dsType || '').toUpperCase() === 'POSTGRESQL',
);

const filteredTables = computed(() => {
  const kw = (formValues.value._tableFilter || '').toLowerCase();
  if (!kw) return metadataMerged.value;
  return metadataMerged.value.filter(
    (t) =>
      (t.tableName || '').toLowerCase().includes(kw) ||
      (t.tableAlias || '').toLowerCase().includes(kw),
  );
});

const autoTaskName = computed(() => {
  const table = (formValues.value.tablePattern || '').trim();
  if (!table) return '';
  const layer = selectedDs.value?.dataLayer
    ? `${selectedDs.value.dataLayer}_`
    : '';
  return `探查_${layer}${table}`;
});

function getDsTypeColor(dsType: string) {
  const map: Record<string, string> = {
    MYSQL: 'blue',
    SQLSERVER: 'red',
    ORACLE: 'orange',
    POSTGRESQL: 'cyan',
    TIDB: 'green',
  };
  return map[(dsType || '').toUpperCase()] || 'default';
}

function getProfileLevelLabel(level?: string) {
  const m: Record<string, string> = {
    BASIC: '仅表级',
    DETAILED: '表+列级',
    FULL: '完整探查',
  };
  return m[level || ''] || level || '-';
}

function getProfileLevelColor(level?: string) {
  const m: Record<string, string> = {
    BASIC: 'blue',
    DETAILED: 'purple',
    FULL: 'magenta',
  };
  return m[level || ''] || 'default';
}

function getTriggerLabel(t?: string) {
  const m: Record<string, string> = {
    MANUAL: '手动触发',
    SCHEDULE: '定时触发',
    API: 'API 调用',
  };
  return m[t || ''] || t || '-';
}

function getTriggerColor(t?: string) {
  const m: Record<string, string> = {
    MANUAL: 'green',
    SCHEDULE: 'blue',
    API: 'cyan',
  };
  return m[t || ''] || 'default';
}

async function loadSchemas(dsId: number) {
  schemaList.value = [];
  pgSchema.value = undefined;
  if (!isPostgresDs.value) return;
  schemaLoading.value = true;
  try {
    schemaList.value = (await datasourceSchemas(dsId)) || [];
    if (schemaList.value.length === 1) {
      pgSchema.value = schemaList.value[0];
    }
  } catch {
    schemaList.value = [];
  } finally {
    schemaLoading.value = false;
  }
}

async function loadTablesForDs(dsId: number) {
  metadataMerged.value = [];
  tablesLoading.value = true;
  try {
    const [metaRes, live] = await Promise.all([
      metadataTableList({ dsId, pageNum: 1, pageSize: 2000 } as any).catch(
        () => ({
          rows: [] as any[],
        }),
      ),
      datasourceTables(dsId, pgSchema.value).catch(() => [] as string[]),
    ]);
    const metaRows = metaRes?.rows || [];
    const map = new Map<string, (typeof metadataMerged.value)[0]>();
    for (const r of metaRows) {
      if (!r.tableName) continue;
      map.set(r.tableName, {
        tableName: r.tableName,
        tableAlias: r.tableAlias,
        tableComment: r.tableComment,
        columnCount: r.columnCount,
      });
    }
    for (const name of live || []) {
      if (!name || map.has(name)) continue;
      map.set(name, { tableName: name });
    }
    metadataMerged.value = [...map.values()].toSorted((a, b) =>
      (a.tableName || '').localeCompare(b.tableName || ''),
    );
  } finally {
    tablesLoading.value = false;
  }
}

async function loadColumnPreview() {
  columnPreview.value = [];
  selectedTargetColumns.value = new Set();
  const dsId = formValues.value.dsId;
  const table = (formValues.value.tablePattern || '').trim();
  if (!dsId || !table || formValues.value.profileLevel === 'BASIC') return;
  columnPreviewLoading.value = true;
  try {
    const cols = await datasourceColumns(dsId, table, pgSchema.value);
    const raw = (cols || []) as any[];
    // 合并已选列（编辑场景切换表后不清空上次选择）
    const existing = new Set(selectedTargetColumns.value);
    columnPreview.value = raw.map((c: any) => ({
      columnName: c.columnName || c.COLUMN_NAME || c.name || '-',
      dataType: c.dataType || c.DATA_TYPE || c.typeName,
      columnComment: c.columnComment || c.column_comment || c.remarks,
      checked: existing.has(c.columnName || c.COLUMN_NAME || c.name || '-'),
    }));
    if (existing.size > 0) {
      selectedTargetColumns.value = new Set(
        columnPreview.value.filter((c) => c.checked).map((c) => c.columnName),
      );
    }
  } catch {
    columnPreview.value = [];
  } finally {
    columnPreviewLoading.value = false;
  }
}

watch(
  () => [
    formValues.value.tablePattern,
    formValues.value.profileLevel,
    formValues.value.dsId,
  ],
  () => {
    loadColumnPreview();
  },
);

watch(
  () => formValues.value.tablePattern,
  (t) => {
    if (taskNameTouched.value || isEdit.value) return;
    if ((t || '').trim()) {
      formValues.value.taskName = autoTaskName.value;
    }
  },
);

watch(
  () => formValues.value.dsId,
  async (id) => {
    metadataMerged.value = [];
    pgSchema.value = undefined;
    schemaList.value = [];
    if (!id) return;
    await loadSchemas(id);
    await loadTablesForDs(id);
  },
);

watch(
  () => pgSchema.value,
  async () => {
    if (!formValues.value.dsId) return;
    await loadTablesForDs(formValues.value.dsId);
  },
);

const columnPreviewColumns = [
  {
    title: '',
    key: '_check',
    width: 40,
    align: 'center' as const,
  },
  { title: '列名', dataIndex: 'columnName', key: 'columnName', width: 220 },
  { title: '类型', dataIndex: 'dataType', key: 'dataType', width: 140 },
  {
    title: '注释',
    dataIndex: 'columnComment',
    key: 'columnComment',
    ellipsis: true,
  },
];

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  onClosed: handleClosed,
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    drawerApi.drawerLoading(true);
    try {
      const dsList = await datasourceEnabled();
      datasourceOptions.value = (dsList || []).map((item: any) => ({
        label: item.dsName,
        value: item.dsId,
        raw: item,
      }));

      const { id } = drawerApi.getData() as { id?: number };
      currentStep.value = 0;
      taskNameTouched.value = false;
      executeAfterCreate.value = true;
      if (id) {
        recordId.value = id;
        const info = await dprofileTaskInfo(id);
        formValues.value = Object.fromEntries(
          Object.entries({ ...info })
            .filter(([, v]) => v !== undefined && v !== null)
            .map(([k, v]) => [k, v === 'undefined' ? '' : v]),
        ) as any;
        formValues.value._tableFilter = '';
        taskNameTouched.value = true;
        // 回显已选列
        selectedTargetColumns.value = new Set(
          (info.targetColumns || '')
            .split(',')
            .map((s: string) => s.trim())
            .filter(Boolean),
        );
        if (info.dsId) {
          await loadSchemas(info.dsId);
          await loadTablesForDs(info.dsId);
        }
        await loadColumnPreview();
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          profileLevel: 'DETAILED',
          triggerType: 'MANUAL',
          cronExpr: '',
          tablePattern: '',
          _tableFilter: '',
        };
        pgSchema.value = undefined;
        schemaList.value = [];
        metadataMerged.value = [];
        columnPreview.value = [];
        selectedTargetColumns.value = new Set();
      }
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

function handleClosed() {
  recordId.value = undefined;
  formValues.value = {};
  currentStep.value = 0;
  pgSchema.value = undefined;
  schemaList.value = [];
  metadataMerged.value = [];
  columnPreview.value = [];
  selectedTargetColumns.value = new Set();
}

function validateStep(step: number): boolean {
  if (step === 0) {
    if (!formValues.value.dsId) {
      message.warning('请选择目标数据源');
      return false;
    }
    if (isPostgresDs.value && !pgSchema.value) {
      message.warning('PostgreSQL 请先选择 Schema');
      return false;
    }
    if (!(formValues.value.tablePattern || '').trim()) {
      message.warning('请选择或输入目标表名 / 表名模式');
      return false;
    }
    if (!formValues.value.profileLevel) {
      message.warning('请选择探查级别');
      return false;
    }
    return true;
  }
  if (step === 1) {
    if (!(formValues.value.taskName || '').trim()) {
      message.warning('请输入任务名称');
      return false;
    }
    return true;
  }
  if (step === 2) {
    if (
      formValues.value.triggerType === 'SCHEDULE' &&
      !(formValues.value.cronExpr || '').trim()
    ) {
      message.warning('定时触发请填写 Cron 表达式');
      return false;
    }
    return true;
  }
  return true;
}

function handleWizardNext() {
  if (!validateStep(currentStep.value)) return;
  wizardRef.value?.nextStep();
}

async function handleSubmit() {
  if (!validateStep(0) || !validateStep(1) || !validateStep(2)) {
    currentStep.value = 0;
    return;
  }
  submitting.value = true;
  wizardRef.value?.setSubmitting?.(true);
  drawerApi.lock(true);
  try {
    const payload = {
      taskName: formValues.value.taskName,
      taskCode: formValues.value.taskCode,
      taskDesc: formValues.value.taskDesc,
      dsId: formValues.value.dsId,
      profileLevel: formValues.value.profileLevel,
      tablePattern: (formValues.value.tablePattern || '').trim(),
      targetColumns:
        selectedTargetColumns.value.size > 0
          ? [...selectedTargetColumns.value].join(',')
          : '',
      triggerType: formValues.value.triggerType,
      cronExpr: formValues.value.cronExpr,
      enabled: formValues.value.enabled,
    };
    if (recordId.value) {
      await dprofileTaskUpdate({ id: recordId.value, ...payload });
      message.success('保存成功');
    } else {
      const newId = await dprofileTaskAdd(payload);
      message.success('创建成功');
      if (executeAfterCreate.value && newId !== undefined && newId !== null) {
        try {
          await dprofileTaskStart(newId as number);
        } catch (error) {
          console.error(error);
          message.warning('任务已创建，但立即执行失败，可在列表中手动启动');
        }
      }
    }
    emit('reload');
    drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    submitting.value = false;
    wizardRef.value?.setSubmitting?.(false);
    drawerApi.lock(false);
  }
}

function markTaskNameEdited() {
  taskNameTouched.value = true;
}

function selectAllColumns() {
  selectedTargetColumns.value = new Set(
    columnPreview.value.map((c) => c.columnName),
  );
}

function clearSelectedColumns() {
  selectedTargetColumns.value = new Set();
}

function onColumnCheckChange(columnName: string, checked: boolean) {
  const s = new Set(selectedTargetColumns.value);
  if (checked) {
    s.add(columnName);
  } else {
    s.delete(columnName);
  }
  selectedTargetColumns.value = s;
}

const selectedColumnCount = computed(() => selectedTargetColumns.value.size);

const targetColumnDisplay = computed(() => {
  const s = selectedTargetColumns.value;
  if (s.size === 0) return '全部列';
  const arr = [...s];
  if (arr.length <= 3) return arr.join(', ');
  return `${arr.slice(0, 3).join(', ')} 等${arr.length}列`;
});
</script>

<template>
  <BasicDrawer
    :title="title"
    class="dprofile-task-wizard w-[min(920px,98vw)]"
    :footer="false"
  >
    <WizardDrawer
      ref="wizardRef"
      step-variant="horizontal"
      :steps="steps"
      v-model:current-step="currentStep"
      :loading="submitting"
      @next="handleWizardNext"
      @finish="handleSubmit"
    >
      <template #step-0>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">配置探查范围</div>
            <div class="panel-desc">选择目标数据源、表以及探查级别</div>
          </div>
          <Form :model="formValues" layout="vertical" class="panel-form">
            <Form.Item label="目标数据源" required>
              <Select
                v-model:value="formValues.dsId"
                placeholder="请选择数据源"
                show-search
                :filter-option="
                  (input, option) =>
                    String(option?.label ?? '')
                      .toLowerCase()
                      .includes(input.toLowerCase())
                "
              >
                <Select.Option
                  v-for="opt in datasourceOptions"
                  :key="opt.value"
                  :value="opt.value"
                  :label="opt.label"
                >
                  <div class="ds-option">
                    <span class="ds-option-name">{{ opt.label }}</span>
                    <Tag
                      v-if="opt.raw?.dsType"
                      size="small"
                      :color="getDsTypeColor(opt.raw.dsType)"
                    >
                      {{ opt.raw.dsType }}
                    </Tag>
                    <Tag v-if="opt.raw?.dataLayer" size="small" color="purple">
                      {{ opt.raw.dataLayer }}
                    </Tag>
                  </div>
                </Select.Option>
              </Select>
              <div class="form-tip">
                {{
                  formValues.dsId
                    ? `已选择数据源，共 ${metadataMerged.length} 张表（元数据与库表合并）`
                    : '请从下拉列表中选择已配置的数据源'
                }}
              </div>
            </Form.Item>

            <Form.Item v-if="isPostgresDs" label="Schema" required>
              <Select
                v-model:value="pgSchema"
                placeholder="请选择 schema，再加载目标表"
                :loading="schemaLoading"
                :options="schemaList.map((s) => ({ label: s, value: s }))"
                allow-clear
              />
              <div
                v-if="
                  schemaList.length === 0 && !schemaLoading && formValues.dsId
                "
                class="form-tip"
              >
                未找到 schema，请确认数据源配置
              </div>
            </Form.Item>

            <Form.Item label="目标表" required>
              <Select
                v-if="metadataMerged.length > 0"
                v-model:value="formValues.tablePattern"
                show-search
                :loading="tablesLoading"
                placeholder="从列表选择，或输入关键字筛选；也支持通配符如 dim_*"
                :filter-option="false"
                @search="(v: string) => (formValues._tableFilter = v)"
              >
                <Select.Option
                  v-for="t in filteredTables.slice(0, 500)"
                  :key="t.tableName"
                  :value="t.tableName"
                >
                  <div class="table-option">
                    <FileOutlined class="table-opt-icon" />
                    <span>{{ t.tableName }}</span>
                    <!-- prettier-ignore -->
                    <span v-if="t.tableAlias" class="table-alias">{{ t.tableAlias }}</span>
                    <!-- prettier-ignore -->
                    <span v-if="t.columnCount != null" class="column-count">{{ t.columnCount }} 列</span>
                  </div>
                </Select.Option>
              </Select>
              <Input
                v-else
                v-model:value="formValues.tablePattern"
                placeholder="未拉到表清单时可直接输入表名或模式（如 dim_*），失焦后可加载字段预览"
              >
                <template #suffix>
                  <Tooltip
                    title="表名须与库中一致；也可使用 * ? 通配符匹配多张表"
                  >
                    <QuestionCircleOutlined class="tip-icon" />
                  </Tooltip>
                </template>
              </Input>
              <div class="form-tip">
                <span v-if="formValues.tablePattern">
                  当前表名 / 模式：<code>{{ formValues.tablePattern }}</code>
                </span>
                <!-- prettier-ignore -->
                <span v-else-if="!formValues.dsId">请先选择数据源，再选择目标表</span>
                <!-- prettier-ignore -->
                <span v-else-if="isPostgresDs && !pgSchema">请先选择 Schema</span>
                <span v-else-if="tablesLoading">正在加载表清单…</span>
                <span v-else>请从下拉选择，或直接输入完整表名 / 通配模式</span>
              </div>
            </Form.Item>

            <Form.Item label="探查级别" required>
              <div class="level-grid">
                <div
                  v-for="level in profileLevelOptions"
                  :key="level.value"
                  class="level-card"
                  :class="{
                    'level-card--active':
                      formValues.profileLevel === level.value,
                  }"
                  @click="formValues.profileLevel = level.value"
                >
                  <div class="level-icon">
                    <component :is="level.icon" />
                  </div>
                  <div class="level-body">
                    <div class="level-name">{{ level.name }}</div>
                    <div class="level-desc">{{ level.desc }}</div>
                    <div class="level-tags">
                      <Tag
                        v-for="item in level.items"
                        :key="item"
                        class="level-tag"
                      >
                        {{ item }}
                      </Tag>
                    </div>
                  </div>
                  <CheckCircleFilled
                    v-if="formValues.profileLevel === level.value"
                    class="level-check"
                  />
                </div>
              </div>
            </Form.Item>

            <Form.Item
              v-if="formValues.profileLevel !== 'BASIC'"
              label="指定探查列"
            >
              <div v-if="columnPreviewLoading" class="form-tip">
                正在加载字段列表…
              </div>
              <template v-else-if="columnPreview.length > 0">
                <Table
                  size="small"
                  :columns="columnPreviewColumns"
                  :data-source="columnPreview"
                  :pagination="false"
                  :scroll="{ y: 240 }"
                  row-key="columnName"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === '_check'">
                      <Checkbox
                        :checked="selectedTargetColumns.has(record.columnName)"
                        @change="
                          (e: Event) =>
                            onColumnCheckChange(
                              record.columnName,
                              (e.target as HTMLInputElement).checked,
                            )
                        "
                      />
                    </template>
                  </template>
                </Table>
                <div class="col-opts">
                  <Button type="link" size="small" @click="selectAllColumns">
                    全选
                  </Button>
                  <Button
                    type="link"
                    size="small"
                    @click="clearSelectedColumns"
                  >
                    清空
                  </Button>
                  <span class="col-count">
                    已选 <strong>{{ selectedColumnCount }}</strong> 列（共
                    {{ columnPreview.length }} 列）
                  </span>
                </div>
                <div class="form-tip" style="margin-top: 6px">
                  勾选要探查的列（留空表示探查全部列）。指定列后，任务执行时仅分析选中的列。
                </div>
              </template>
              <div v-else class="form-tip">
                {{
                  formValues.tablePattern
                    ? '未能加载字段列表，请确认表名或数据源权限'
                    : '选择目标表后将显示可勾选的字段列表'
                }}
              </div>
            </Form.Item>
          </Form>
        </div>
      </template>

      <template #step-1>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">填写基本信息</div>
            <div class="panel-desc">设置探查任务的基本标识信息</div>
          </div>
          <Form :model="formValues" layout="vertical" class="panel-form">
            <Form.Item label="任务名称" required>
              <Input
                v-model:value="formValues.taskName"
                placeholder="上一步已选表时可自动生成；也可改为业务化名称"
                :maxlength="100"
                show-count
                @focus="markTaskNameEdited"
                @change="markTaskNameEdited"
              />
            </Form.Item>
            <Form.Item label="任务编码">
              <Input
                v-model:value="formValues.taskCode"
                placeholder="留空则由系统在保存时自动生成"
                :maxlength="50"
                :disabled="isEdit"
              />
              <div class="form-tip">
                编辑模式下编码只读；新建时后端默认生成唯一编码
              </div>
            </Form.Item>
            <Form.Item label="任务描述">
              <Input.TextArea
                v-model:value="formValues.taskDesc"
                placeholder="选填，简要描述该任务的用途和探查范围"
                :rows="3"
                :maxlength="500"
                show-count
              />
            </Form.Item>
          </Form>
          <Alert message="任务命名建议" type="info" show-icon class="mt-2">
            <template #description>
              <ul class="tip-ul">
                <li>
                  建议格式：<code>数据层_表名_探查频率</code>，如
                  <code>ODS_dim_orders_daily</code>
                </li>
                <li>同一数据源下任务名称宜保持可读、可区分</li>
              </ul>
            </template>
          </Alert>
        </div>
      </template>

      <template #step-2>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">配置触发方式</div>
            <div class="panel-desc">设置任务的执行触发方式和调度规则</div>
          </div>
          <Form :model="formValues" layout="vertical" class="panel-form">
            <Form.Item label="触发方式">
              <div class="trigger-grid">
                <div
                  v-for="opt in triggerTypeOptions"
                  :key="opt.value"
                  class="trigger-card"
                  :class="{
                    'trigger-card--active':
                      formValues.triggerType === opt.value,
                  }"
                  @click="formValues.triggerType = opt.value"
                >
                  <div class="trigger-icon" :style="{ background: opt.iconBg }">
                    <component :is="opt.icon" />
                  </div>
                  <div class="trigger-text">
                    <div class="trigger-name">{{ opt.name }}</div>
                    <div class="trigger-desc">{{ opt.desc }}</div>
                  </div>
                </div>
              </div>
            </Form.Item>

            <Form.Item
              v-if="formValues.triggerType === 'SCHEDULE'"
              label="Cron 表达式"
              required
            >
              <Input
                v-model:value="formValues.cronExpr"
                placeholder="如 0 0 2 * * ?（每天凌晨2点）"
              />
              <div class="cron-presets">
                <Tag
                  v-for="p in cronPresets"
                  :key="p.value"
                  class="cron-preset"
                  :class="{
                    'cron-preset--on': formValues.cronExpr === p.value,
                  }"
                  @click="formValues.cronExpr = p.value"
                >
                  {{ p.label }}
                </Tag>
              </div>
            </Form.Item>

            <Form.Item label="超时时间（分钟）">
              <InputNumber
                :min="1"
                :max="1440"
                :default-value="60"
                disabled
                style="width: 200px"
              />
              <div class="form-tip">
                与 bgdata
                一致预留；当前后端未持久化该字段，执行超时由服务默认策略控制
              </div>
            </Form.Item>

            <Form.Item label="任务开关" name="enabled">
              <Radio.Group
                v-model:value="formValues.enabled"
                :options="enabledOptions"
              />
            </Form.Item>
          </Form>

          <Alert
            v-if="formValues.triggerType === 'MANUAL'"
            type="info"
            show-icon
            class="mt-2"
          >
            <template #message>手动触发说明</template>
            <template #description>
              任务不会自动执行，可在列表中点击「启动」手动运行（将按当前配置探查匹配到的表）。
            </template>
          </Alert>
          <Alert
            v-if="formValues.triggerType === 'SCHEDULE'"
            type="info"
            show-icon
            class="mt-2"
          >
            <template #message>定时触发说明</template>
            <template #description>
              保存 Cron 后，需确保调度模块已接入该表达式；当前表单会写入任务的
              Cron 字段。
            </template>
          </Alert>
        </div>
      </template>

      <template #step-3>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">确认配置</div>
            <div class="panel-desc">请确认以下信息，确认无误后保存</div>
          </div>
          <Descriptions bordered size="small" :column="2" class="confirm-desc">
            <Descriptions.Item label="任务名称" :span="2">
              <strong>{{ formValues.taskName || '-' }}</strong>
              <Tag v-if="isEdit" color="blue" class="ml-2">编辑</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="任务编码">
              {{ formValues.taskCode || '（保存时自动生成）' }}
            </Descriptions.Item>
            <Descriptions.Item label="任务描述">
              {{ formValues.taskDesc || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="目标数据源" :span="2">
              {{
                datasourceOptions.find((d) => d.value === formValues.dsId)
                  ?.label || '-'
              }}
            </Descriptions.Item>
            <Descriptions.Item label="目标表 / 模式" :span="2">
              <code>{{ formValues.tablePattern || '-' }}</code>
            </Descriptions.Item>
            <Descriptions.Item label="Schema" v-if="isPostgresDs" :span="2">
              {{ pgSchema || '-' }}
            </Descriptions.Item>
            <Descriptions.Item label="探查级别">
              <Tag :color="getProfileLevelColor(formValues.profileLevel)">
                {{ getProfileLevelLabel(formValues.profileLevel) }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="指定列">
              {{ targetColumnDisplay }}
            </Descriptions.Item>
            <Descriptions.Item label="触发方式">
              <Tag :color="getTriggerColor(formValues.triggerType)">
                {{ getTriggerLabel(formValues.triggerType) }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="启用状态">
              {{ formValues.enabled === '0' ? '启用' : '停用' }}
            </Descriptions.Item>
            <Descriptions.Item
              v-if="formValues.triggerType === 'SCHEDULE'"
              label="Cron"
              :span="2"
            >
              <code>{{ formValues.cronExpr || '-' }}</code>
            </Descriptions.Item>
          </Descriptions>

          <Checkbox
            v-if="!isEdit"
            v-model:checked="executeAfterCreate"
            class="exec-once"
          >
            <span class="exec-once-title">创建后立即执行一次探查</span>
            <div class="exec-once-desc">
              保存成功后自动调用启动接口（与 bgdata 行为对齐）
            </div>
          </Checkbox>
        </div>
      </template>
    </WizardDrawer>
  </BasicDrawer>
</template>

<style scoped>
.step-panel {
  padding: 4px 0 8px;
}
.panel-head {
  margin-bottom: 16px;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
}
.panel-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 4px;
}
.panel-form {
  max-width: 100%;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
  line-height: 1.5;
}
.ds-option {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ds-option-name {
  flex: 1;
  min-width: 0;
}
.table-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.table-opt-icon {
  color: #722ed1;
}
.table-alias {
  color: #8c8c8c;
  font-size: 12px;
}
.column-count {
  margin-left: auto;
  font-size: 12px;
  color: #1677ff;
}
.tip-icon {
  color: #8c8c8c;
}
.level-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.level-card {
  position: relative;
  border: 2px solid #f0f0f0;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.level-card:hover {
  border-color: #d3adf7;
}
.level-card--active {
  border-color: #722ed1;
  box-shadow: 0 0 0 1px rgba(114, 46, 209, 0.12);
}
.level-icon {
  font-size: 22px;
  color: #722ed1;
}
.level-body {
  flex: 1;
  min-width: 0;
}
.level-name {
  font-weight: 600;
  font-size: 14px;
}
.level-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
.level-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.level-tag {
  margin: 0;
  font-size: 11px;
}
.level-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #722ed1;
  font-size: 18px;
}
.trigger-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.trigger-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 2px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.trigger-card:hover {
  border-color: #91caff;
}
.trigger-card--active {
  border-color: #1677ff;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.12);
}
.trigger-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
}
.trigger-text {
  min-width: 0;
}
.trigger-name {
  font-weight: 600;
}
.trigger-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
.cron-presets {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cron-preset {
  cursor: pointer;
  margin: 0;
}
.cron-preset--on {
  color: #1677ff;
  border-color: #1677ff;
}
.tip-ul {
  margin: 0;
  padding-left: 18px;
}
.confirm-desc {
  margin-bottom: 16px;
}
.exec-once {
  display: flex;
  align-items: flex-start;
  margin-top: 16px;
}
.exec-once-title {
  font-weight: 500;
}
.exec-once-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
</style>
