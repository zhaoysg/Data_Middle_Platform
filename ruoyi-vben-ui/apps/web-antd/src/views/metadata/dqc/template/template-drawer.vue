<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Select, message } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined, SyncOutlined } from '@ant-design/icons-vue';

import {
  dqcTemplateAdd,
  dqcTemplateInfo,
  dqcTemplateUpdate,
} from '#/api/metadata/dqc/template';

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

const ruleTypeOptions = [
  { label: '空值检查', value: 'NULL_CHECK' },
  { label: '唯一性', value: 'UNIQUE' },
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

const paramTypeOptions = [
  { label: '字符串', value: 'STRING' },
  { label: '整数', value: 'INTEGER' },
  { label: '小数', value: 'DECIMAL' },
  { label: '布尔', value: 'BOOLEAN' },
];

// 内置系统占位符（不需要用户配置）
const SYSTEM_PLACEHOLDERS: Record<string, { label: string; desc: string }> = {
  TABLE_NAME: { label: '${TABLE_NAME}', desc: '当前扫描的表名' },
  TABLE_SCHEMA: { label: '${TABLE_SCHEMA}', desc: '所属 schema' },
  COLUMN_NAME: { label: '${COLUMN_NAME}', desc: '当前字段名' },
  dsId: { label: '${dsId}', desc: '数据源ID' },
};

const steps = [
  { title: '基本信息' },
  { title: '规则表达式与参数' },
  { title: '预览保存' },
];

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const currentStep = ref(0);
const submitting = ref(false);
const wizardRef = ref<InstanceType<typeof WizardDrawer>>();

const formValues = ref<Record<string, any>>({
  enabled: '0',
  builtin: '0',
  sortOrder: 0,
  paramSpec: '[]',
});

const paramList = ref<any[]>([]);

/** 从 SQL 表达式中自动识别 ${param.xxx} 动态参数 */
function parseParamsFromExpr(expr: string): string[] {
  if (!expr) return [];
  const regex = /\$\{param\.(\w+)\}/g;
  const found: string[] = [];
  let match;
  while ((match = regex.exec(expr)) !== null) {
    if (!found.includes(match[1])) {
      found.push(match[1]);
    }
  }
  return found;
}

/** 从表达式中提取所有 ${...} 占位符，区分系统占位符和动态参数 */
const allPlaceholders = computed(() => {
  const expr = formValues.value.defaultExpr || '';
  const regex = /\$\{(\w+)\}/g;
  const found: { name: string; type: 'system' | 'param'; label: string; desc: string }[] = [];
  let match;
  while ((match = regex.exec(expr)) !== null) {
    const key = match[1];
    if (found.some((f) => f.name === key)) continue;
    if (SYSTEM_PLACEHOLDERS[key]) {
      found.push({ name: key, type: 'system', ...SYSTEM_PLACEHOLDERS[key] });
    } else if (key !== 'param') {
      found.push({ name: key, type: 'param', label: '${' + key + '}', desc: '动态参数（自动识别）' });
    }
  }
  return found;
});

/** 当前表达式中的动态参数名称列表 */
const exprParamNames = computed(() => parseParamsFromExpr(formValues.value.defaultExpr || ''));

/** 动态参数名称集合（快速判断） */
const exprParamNameSet = computed(() => new Set(exprParamNames.value));

/** 根据表达式自动同步参数列表：
 * - 表达式中有但 paramList 中没有的参数 → 自动新增
 * - paramList 中有但表达式中已不存在的参数 → 标记警告
 * - paramList 中原有手动编辑过的参数 → 保留
 */
function syncParamList() {
  const existing = paramList.value;
  const names = exprParamNames.value;
  const existingNames = new Set(existing.map((p) => p.name));

  // 新增：表达式中新增的参数
  for (const name of names) {
    if (!existingNames.has(name)) {
      existing.push({
        name,
        type: guessParamType(formValues.value.defaultExpr, name),
        defaultValue: guessDefaultValue(name),
        description: '',
        required: false,
      });
    }
  }

  // 删除：表达式中已移除的参数
  paramList.value = existing.filter(
    (p) => !exprParamNameSet.value.has(p.name) || p.description !== '',
  );

  syncParams();
}

/** 根据参数名和表达式猜测参数类型 */
function guessParamType(expr: string, name: string): string {
  const patterns: [RegExp, string][] = [
    [/rate|pct|percent|ratio/i, 'DECIMAL'],
    [/count|total|num|size|length/i, 'INTEGER'],
    [/flag|bool|is|enable/i, 'BOOLEAN'],
  ];
  for (const [regex, type] of patterns) {
    if (regex.test(name)) return type;
  }
  // 检查表达式中该参数附近是否有整数常量
  const re = new RegExp(`\\$\\{param\\.${name}\\}.{0,30}?\\d+\\.?\\d*`, 'i');
  const m = expr.match(re);
  if (m && m[0].includes('.')) return 'DECIMAL';
  if (m) return 'INTEGER';
  return 'STRING';
}

/** 根据参数名猜测默认值 */
function guessDefaultValue(name: string): string {
  const guesses: Record<string, string> = {
    maxRate: '0.05',
    minRate: '0',
    minValue: '0',
    maxValue: '100',
    enumValues: 'A,B,C',
    minLength: '1',
    maxLength: '255',
    maxFluctuationPct: '20',
    maxDupRate: '0.01',
    maxOrphanRate: '0.0',
    minDays: '0',
    maxDays: '30',
    topN: '10',
  };
  return guesses[name] || '';
}

watch(
  () => formValues.value.paramSpec,
  (val) => {
    try {
      paramList.value = val ? JSON.parse(val) : [];
    } catch {
      paramList.value = [];
    }
  },
  { immediate: true },
);

function syncParams() {
  formValues.value.paramSpec = JSON.stringify(paramList.value);
}

function addParam() {
  paramList.value.push({
    name: '',
    type: 'STRING',
    defaultValue: '',
    description: '',
    required: false,
  });
  syncParams();
}

function removeParam(index: number) {
  paramList.value.splice(index, 1);
  syncParams();
}

function updateParamName(index: number, name: string) {
  paramList.value[index].name = name;
  syncParams();
}

const title = computed(() => {
  return recordId.value ? '编辑规则模板' : '新增规则模板';
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
      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await dqcTemplateInfo(id);
        formValues.value = { ...info };
        try {
          paramList.value = info.paramSpec ? JSON.parse(info.paramSpec) : [];
        } catch {
          paramList.value = [];
        }
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          builtin: '0',
          sortOrder: 0,
          paramSpec: '[]',
        };
        paramList.value = [];
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
    const payload = { ...formValues.value };
    if (recordId.value) {
      await dqcTemplateUpdate({ id: recordId.value, ...payload });
    } else {
      await dqcTemplateAdd(payload);
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
  formValues.value = { enabled: '0', builtin: '0', sortOrder: 0, paramSpec: '[]' };
  paramList.value = [];
  currentStep.value = 0;
}

const step0Valid = computed(() => {
  const f = formValues.value;
  return f.templateName && f.templateCode && f.ruleType && f.applyLevel && f.dimension;
});

const step1Valid = computed(() => {
  return formValues.value.defaultExpr;
});

function validateStep(step: number): boolean {
  if (step === 0) {
    if (!step0Valid.value) {
      message.warning('请完善基本信息（模板名称、模板编码、规则类型、适用级别、质量维度）');
      return false;
    }
  }
  if (step === 1) {
    if (!step1Valid.value) {
      message.warning('请填写默认表达式');
      return false;
    }
    const unnamed = paramList.value.filter((p) => !p.name && !p.description);
    if (unnamed.length > 0) {
      message.warning('请填写所有参数的名称');
      return false;
    }
    const dupNames = paramList.value.filter((p, i, arr) => arr.findIndex((q) => q.name === p.name && p.name) !== i);
    if (dupNames.length > 0) {
      message.warning('参数名称不能重复');
      return false;
    }
  }
  return true;
}

function handleWizardNext() {
  if (!validateStep(currentStep.value)) return;
  wizardRef.value?.nextStep();
}

const builtinLabel = computed(() => (formValues.value.builtin === '1' ? '是' : '否'));
const enabledLabel = computed(() => (formValues.value.enabled === '0' ? '启用' : '停用'));
const dimensionLabel = computed(
  () => dimensionOptions.find((o) => o.value === formValues.value.dimension)?.label || '-',
);
const ruleTypeLabel = computed(
  () => ruleTypeOptions.find((o) => o.value === formValues.value.ruleType)?.label || '-',
);
const applyLevelLabel = computed(
  () => applyLevelOptions.find((o) => o.value === formValues.value.applyLevel)?.label || '-',
);

// 预览替换后的表达式
const previewExpr = computed(() => {
  const expr = formValues.value.defaultExpr || '';
  let result = expr;
  for (const p of paramList.value) {
    if (p.name && p.defaultValue) {
      result = result.replace(new RegExp(`\\$\\{param\\.${p.name}\\}`, 'g'), p.defaultValue);
    }
  }
  return result;
});
</script>

<template>
  <BasicDrawer :title="title" class="w-[760px]" :loading="submitting">
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
          <Form.Item label="模板名称" required>
            <Input v-model:value="formValues.templateName" placeholder="请输入模板名称" />
          </Form.Item>
          <Form.Item label="模板编码" required>
            <Input v-model:value="formValues.templateCode" placeholder="请输入模板编码" />
          </Form.Item>
          <Form.Item label="模板描述">
            <Input.TextArea
              v-model:value="formValues.templateDesc"
              placeholder="请输入模板描述"
              :rows="3"
            />
          </Form.Item>
          <div class="grid grid-cols-2 gap-4">
            <Form.Item label="规则类型" required>
              <Select
                v-model:value="formValues.ruleType"
                :options="ruleTypeOptions"
                placeholder="请选择规则类型"
              />
            </Form.Item>
            <Form.Item label="适用级别" required>
              <Select
                v-model:value="formValues.applyLevel"
                :options="applyLevelOptions"
                placeholder="请选择适用级别"
              />
            </Form.Item>
          </div>
          <Form.Item label="质量维度" required>
            <Select
              v-model:value="formValues.dimension"
              :options="dimensionOptions"
              placeholder="请选择质量维度"
            />
          </Form.Item>
        </Form>
      </template>

      <!-- Step 1: 规则表达式 + 参数规格（合并） -->
      <template #step-1>
        <div class="space-y-5">
          <!-- 占位符说明 -->
          <div
            class="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-700"
          >
            <p class="font-medium mb-2">可用占位符：</p>
            <div class="grid grid-cols-2 gap-x-6 gap-y-1 text-xs">
              <div v-for="(p, k) in SYSTEM_PLACEHOLDERS" :key="k">
                <code class="bg-blue-100 px-1 rounded mr-1">{{ p.label }}</code>
                <span class="text-blue-600">{{ p.desc }}</span>
              </div>
              <div class="col-span-2 mt-1 pt-1 border-t border-blue-200">
                <code class="bg-blue-100 px-1 rounded mr-1">${param.参数名}</code>
                <span class="text-blue-600">动态参数 → 输入表达式后点击"识别参数"自动生成规格</span>
              </div>
            </div>
          </div>

          <!-- 表达式输入区 -->
          <Form :model="formValues" layout="vertical">
            <Form.Item label="默认表达式" required>
              <Input.TextArea
                v-model:value="formValues.defaultExpr"
                placeholder="例如: SELECT COUNT(*) FROM ${TABLE_NAME} WHERE ${COLUMN_NAME} IS NULL"
                :rows="4"
                class="font-mono text-xs"
              />
            </Form.Item>

            <!-- 阈值配置 + 识别按钮 -->
            <div class="flex gap-3 items-end">
              <div class="flex-1">
                <Form.Item label="默认阈值" class="mb-0">
                  <Input.TextArea
                    v-model:value="formValues.thresholdJson"
                    placeholder='例如: {"maxRate": 0.05} 或 {"minValue": 0, "maxValue": 100}'
                    :rows="2"
                  />
                </Form.Item>
              </div>
              <a-button type="default" @click="syncParamList" class="mb-1">
                <template #icon><SyncOutlined /></template>
                识别参数
              </a-button>
            </div>
          </Form>

          <!-- 参数规格区 -->
          <div class="border border-gray-200 rounded-lg p-4 bg-gray-50">
            <div class="flex justify-between items-center mb-3">
              <div>
                <span class="text-sm font-medium text-gray-700">参数规格</span>
                <span v-if="paramList.length > 0" class="ml-2 text-xs text-gray-400">
                  （已识别 {{ paramList.length }} 个参数）
                </span>
                <span v-else class="ml-2 text-xs text-gray-400">（暂无参数，输入表达式后点击"识别参数"）</span>
              </div>
              <a-button type="primary" size="small" @click="addParam">
                <template #icon><PlusOutlined /></template>
                手动添加
              </a-button>
            </div>

            <!-- 无参数时的空状态 -->
            <div v-if="paramList.length === 0" class="text-center text-gray-400 py-6 text-sm">
              尚未识别到参数，请在上方填写包含 <code class="bg-gray-200 px-1 rounded">${param.参数名}</code> 的表达式后点击"识别参数"
            </div>

            <!-- 参数卡片列表 -->
            <div v-else class="space-y-3">
              <div
                v-for="(param, index) in paramList"
                :key="index"
                class="bg-white border border-gray-200 rounded-lg p-4"
              >
                <div class="flex items-center gap-2 mb-3">
                  <span class="text-xs font-medium text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                    参数 {{ index + 1 }}
                  </span>
                  <span v-if="exprParamNameSet.has(param.name)" class="text-xs text-green-600 bg-green-50 px-2 py-0.5 rounded">
                    从表达式识别
                  </span>
                  <span v-else class="text-xs text-orange-500 bg-orange-50 px-2 py-0.5 rounded">
                    手动添加
                  </span>
                  <div class="flex-1" />
                  <a-button type="link" danger size="small" @click="removeParam(index)">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </div>
                <div class="grid grid-cols-2 gap-3 mb-3">
                  <Form.Item label="参数名称" class="mb-0">
                    <Input
                      :value="param.name"
                      placeholder="如: maxRate"
                      @update:value="(v: string) => updateParamName(index, v)"
                      @blur="syncParams"
                    />
                  </Form.Item>
                  <Form.Item label="参数类型" class="mb-0">
                    <Select
                      v-model:value="param.type"
                      :options="paramTypeOptions"
                      @change="syncParams"
                    />
                  </Form.Item>
                </div>
                <div class="grid grid-cols-2 gap-3 mb-3">
                  <Form.Item label="默认值" class="mb-0">
                    <Input
                      v-model:value="param.defaultValue"
                      placeholder="默认填充值"
                      @blur="syncParams"
                    />
                  </Form.Item>
                  <Form.Item label="参数说明" class="mb-0">
                    <Input
                      v-model:value="param.description"
                      placeholder="参数用途说明"
                      @blur="syncParams"
                    />
                  </Form.Item>
                </div>
                <div class="flex items-center gap-2">
                  <input
                    type="checkbox"
                    :checked="param.required"
                    @change="(e: Event) => { param.required = (e.target as HTMLInputElement).checked; syncParams(); }"
                    class="accent-blue-500"
                    :id="'param-req-' + index"
                  />
                  <label :for="'param-req-' + index" class="text-sm cursor-pointer">必填参数</label>
                  <span v-if="param.name" class="ml-auto text-xs text-gray-400 font-mono">
                    使用: <code class="bg-gray-100 px-1 rounded">{{ '${param.' + param.name + '}' }}</code>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 表达式预览 -->
          <div v-if="paramList.length > 0 && previewExpr">
            <div class="text-sm font-medium text-gray-700 mb-2">表达式预览（参数替换后）</div>
            <pre
              class="bg-slate-800 text-slate-100 rounded-lg p-4 text-xs font-mono whitespace-pre-wrap overflow-auto max-h-40">{{ previewExpr }}</pre>
          </div>
        </div>
      </template>

      <!-- Step 2: 预览保存 -->
      <template #step-2>
        <div class="space-y-4">
          <div class="text-sm text-gray-500 mb-2">请确认以下配置信息，确认无误后点击保存。</div>
          <Form :model="formValues" layout="vertical" class="bg-gray-50 rounded-lg p-4">
            <div class="text-base font-medium mb-4 text-gray-800">基本信息</div>
            <div class="grid grid-cols-2 gap-y-3">
              <div>
                <span class="text-gray-500">模板名称：</span>
                <span>{{ formValues.templateName || '-' }}</span>
              </div>
              <div>
                <span class="text-gray-500">模板编码：</span>
                <span>{{ formValues.templateCode || '-' }}</span>
              </div>
              <div>
                <span class="text-gray-500">质量维度：</span>
                <span>{{ dimensionLabel }}</span>
              </div>
              <div>
                <span class="text-gray-500">规则类型：</span>
                <span>{{ ruleTypeLabel }}</span>
              </div>
              <div>
                <span class="text-gray-500">适用级别：</span>
                <span>{{ applyLevelLabel }}</span>
              </div>
              <div>
                <span class="text-gray-500">状态：</span>
                <span>{{ enabledLabel }}</span>
              </div>
              <div class="col-span-2">
                <span class="text-gray-500">模板描述：</span>
                <span>{{ formValues.templateDesc || '-' }}</span>
              </div>
            </div>

            <div class="text-base font-medium my-4 text-gray-800">规则表达式</div>
            <div class="space-y-2">
              <div class="text-sm font-medium text-gray-600">默认表达式：</div>
              <pre class="bg-white border border-gray-200 rounded p-3 text-xs text-gray-700 whitespace-pre-wrap overflow-auto max-h-32">{{ formValues.defaultExpr || '-' }}</pre>
              <div v-if="formValues.thresholdJson">
                <div class="text-sm font-medium text-gray-600 mt-2">阈值配置：</div>
                <pre class="bg-white border border-gray-200 rounded p-3 text-xs text-gray-700 whitespace-pre-wrap">{{ formValues.thresholdJson }}</pre>
              </div>
            </div>

            <div v-if="paramList.length > 0" class="text-base font-medium my-4 text-gray-800">
              参数规格（共 {{ paramList.length }} 个）
            </div>
            <div v-if="paramList.length > 0" class="space-y-2">
              <div
                v-for="(param, index) in paramList"
                :key="index"
                class="bg-white border border-gray-200 rounded p-3 text-sm"
              >
                <div class="flex justify-between">
                  <span class="font-medium">
                    <code class="bg-gray-100 px-1 rounded mr-1">{{ '${param.' + param.name + '}' }}</code>
                    {{ param.description || param.name }}
                  </span>
                  <span class="text-gray-400">{{ param.type }}</span>
                </div>
                <div class="text-gray-500 text-xs mt-1">
                  <span>默认值: {{ param.defaultValue || '(无)' }}；</span>
                  <span v-if="param.required" class="text-red-500">必填</span>
                  <span v-else>选填</span>
                </div>
              </div>
            </div>
          </Form>
        </div>
      </template>
    </WizardDrawer>
  </BasicDrawer>
</template>
