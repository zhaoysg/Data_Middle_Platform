<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import {
  Alert,
  Descriptions,
  Form,
  Input,
  Radio,
  Select,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import {
  CheckCircleOutlined,
  QuestionCircleOutlined,
  InfoCircleOutlined,
  BulbOutlined,
} from '@ant-design/icons-vue';

import {
  secSensitivityRuleAdd,
  secSensitivityRuleInfo,
  secSensitivityRuleUpdate,
} from '#/api/metadata/security/rules';
import { secLevelList } from '#/api/metadata/security/level';
import { secClassificationList } from '#/api/metadata/security/classification';
import WizardDrawer from '#/components/metadata/WizardDrawer.vue';

const emit = defineEmits<{ reload: [] }>();

/** 与 bgdata 质检方案向导一致：主标题 + 副标题 */
const steps = [
  { title: '基本信息', description: '规则名称与编码' },
  { title: '匹配配置', description: '类型与表达式' },
  { title: '目标映射', description: '敏感等级与数据分类' },
  { title: '预览保存', description: '确认并创建' },
];

const recordId = ref<number>();
const currentStep = ref(0);
const submitting = ref(false);
const wizardRef = ref<InstanceType<typeof WizardDrawer>>();

const formValues = ref<Record<string, any>>({});
const levelOptions = ref<{ label: string; value: string }[]>([]);
const classificationOptions = ref<{ label: string; value: string }[]>([]);
const levelLoading = ref(false);
const classificationLoading = ref(false);

const enabledOptions = [
  { label: '启用', value: '1' },
  { label: '停用', value: '0' },
];

const builtinOptions = [
  { label: '否', value: '0' },
  { label: '是（内置）', value: '1' },
];

const ruleTypeOptions = [
  { label: '正则匹配', value: 'REGEX', desc: '通过正则表达式匹配字段名或内容' },
  { label: '字段名匹配', value: 'COLUMN_NAME', desc: '精确或模糊匹配字段名称' },
  { label: '数据类型', value: 'DATA_TYPE', desc: '按列数据类型（varchar、int 等）匹配' },
  { label: '自定义', value: 'CUSTOM', desc: '使用自定义规则脚本' },
];

const ruleTypeTagColor: Record<string, string> = {
  REGEX: 'blue',
  COLUMN_NAME: 'cyan',
  DATA_TYPE: 'purple',
  CUSTOM: 'orange',
};

const ruleExprExamples: Record<string, string> = {
  REGEX: '(?i)(.*name|.*phone|.*email|.*card)',
  COLUMN_NAME: 'password,secret,token,private',
  DATA_TYPE: 'varchar,text,blob',
  CUSTOM: '',
};

const ruleExprPlaceholder: Record<string, string> = {
  REGEX: '请输入正则表达式，如: (?i)(.*name|.*phone)',
  COLUMN_NAME: '请输入字段名，多个用逗号分隔，如: password,secret',
  DATA_TYPE: '请输入数据类型，多个用逗号分隔，如: varchar,text',
  CUSTOM: '请输入自定义规则表达式',
};

const title = computed(() =>
  recordId.value ? '编辑敏感识别规则' : '新建敏感识别规则',
);
const isEdit = computed(() => !!recordId.value);

const selectedRuleType = computed(() => formValues.value.ruleType || '');
const currentExprExample = computed(
  () => ruleExprExamples[selectedRuleType.value] || '',
);
const currentPlaceholder = computed(
  () => ruleExprPlaceholder[selectedRuleType.value] || '请输入规则表达式',
);
const currentTypeTagColor = computed(
  () => ruleTypeTagColor[selectedRuleType.value] || 'default',
);

const autoRuleCode = computed(() => {
  const name = (formValues.value.ruleName || '').trim();
  if (!name) return '';
  return `RULE_${name
    .replace(/[^a-zA-Z0-9\u4e00-\u9fa5]/g, '_')
    .toUpperCase()
    .slice(0, 20)}`;
});

const levelNameMap = computed(() => {
  const m: Record<string, string> = {};
  for (const opt of levelOptions.value) {
    m[opt.value] = opt.label;
  }
  return m;
});

const classNameMap = computed(() => {
  const m: Record<string, string> = {};
  for (const opt of classificationOptions.value) {
    m[opt.value] = opt.label;
  }
  return m;
});

async function loadLevelOptions() {
  levelLoading.value = true;
  try {
    const res = await secLevelList({ pageNum: 1, pageSize: 200 });
    const rows = (res as any)?.rows || [];
    levelOptions.value = rows.map((item: any) => ({
      label: `${item.levelCode || ''} - ${item.levelName || ''}`,
      value: item.levelCode || item.id,
    }));
  } catch {
    levelOptions.value = [];
  } finally {
    levelLoading.value = false;
  }
}

async function loadClassificationOptions() {
  classificationLoading.value = true;
  try {
    const res = await secClassificationList({ pageNum: 1, pageSize: 200 });
    const rows = (res as any)?.rows || [];
    classificationOptions.value = rows.map((item: any) => ({
      label: `${item.classCode || ''} - ${item.className || ''}`,
      value: item.classCode || item.id,
    }));
  } catch {
    classificationOptions.value = [];
  } finally {
    classificationLoading.value = false;
  }
}

watch(
  () => formValues.value.ruleType,
  () => {
    if (!formValues.value.ruleExpr) {
      formValues.value.ruleExpr = '';
    }
  },
);

watch(
  () => formValues.value.ruleName,
  (name) => {
    if (!isEdit.value && name !== undefined && !formValues.value._nameTouched) {
      formValues.value.ruleCode = autoRuleCode.value;
    }
  },
);

const [BasicDrawer, drawerApi] = useVbenDrawer({
  destroyOnClose: true,
  footer: false,
  onClosed: handleClosed,
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    drawerApi.drawerLoading(true);
    currentStep.value = 0;
    try {
      // 并行加载等级和分类选项
      await Promise.all([
        loadLevelOptions(),
        loadClassificationOptions(),
      ]);

      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await secSensitivityRuleInfo(id);
        formValues.value = { ...info, _nameTouched: true };
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          builtin: '0',
          ruleType: 'REGEX',
          _nameTouched: false,
        };
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
}

function validateStep(step: number): boolean {
  if (step === 0) {
    if (!(formValues.value.ruleName || '').trim()) {
      message.warning('请输入规则名称');
      return false;
    }
    if (!(formValues.value.ruleCode || '').trim()) {
      message.warning('请输入规则编码');
      return false;
    }
    return true;
  }
  if (step === 1) {
    if (!formValues.value.ruleType) {
      message.warning('请选择规则类型');
      return false;
    }
    if (
      formValues.value.ruleType !== 'CUSTOM' &&
      !(formValues.value.ruleExpr || '').trim()
    ) {
      message.warning('请输入规则表达式');
      return false;
    }
    return true;
  }
  if (step === 2) {
    if (!formValues.value.targetLevelCode) {
      message.warning('请选择目标敏感等级');
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
  for (let i = 0; i < 3; i++) {
    if (!validateStep(i)) {
      currentStep.value = i;
      return;
    }
  }
  submitting.value = true;
  wizardRef.value?.setSubmitting?.(true);
  drawerApi.lock(true);
  try {
    const payload = {
      ruleName: formValues.value.ruleName,
      ruleCode: formValues.value.ruleCode,
      ruleType: formValues.value.ruleType,
      ruleExpr: formValues.value.ruleExpr || '',
      targetLevelCode: formValues.value.targetLevelCode,
      targetClassCode: formValues.value.targetClassCode || '',
      enabled: formValues.value.enabled === '0' ? '1' : '0',
      builtin: formValues.value.builtin,
    };
    if (recordId.value) {
      await secSensitivityRuleUpdate({ id: recordId.value, ...payload });
      message.success('保存成功');
    } else {
      await secSensitivityRuleAdd(payload);
      message.success('创建成功');
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

function markNameTouched() {
  formValues.value._nameTouched = true;
}
</script>

<template>
  <BasicDrawer
    :title="title"
    class="sec-rule-wizard w-[min(860px,98vw)]"
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
      <!-- Step 0: 基本信息 -->
      <template #step-0>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">填写规则基本信息</div>
            <div class="panel-desc">为敏感识别规则命名并设置基本参数</div>
          </div>
          <Form :model="formValues" layout="vertical" class="panel-form">
            <Form.Item label="规则名称" required>
              <Input
                v-model:value="formValues.ruleName"
                placeholder="请输入规则名称，如：姓名字段识别"
                maxlength="50"
                show-count
                @input="markNameTouched"
              />
              <div class="form-tip">
                建议命名规范：描述匹配目标，如「姓名字段识别」「手机号字段识别」
              </div>
            </Form.Item>

            <Form.Item label="规则编码" required>
              <Input
                v-model:value="formValues.ruleCode"
                placeholder="请输入唯一编码，如：RULE_NAME_FIELD"
                maxlength="64"
                show-count
              />
              <div v-if="autoRuleCode && !isEdit" class="form-tip">
                自动生成：
                <Tag color="processing">{{ autoRuleCode }}</Tag>
                ，可直接修改
              </div>
            </Form.Item>

            <Form.Item label="是否内置">
              <Radio.Group v-model:value="formValues.builtin" :options="builtinOptions" />
              <div class="form-tip warning">
                <InfoCircleOutlined />
                内置规则不可删除，仅可修改启用状态
              </div>
            </Form.Item>

            <Form.Item label="状态">
              <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
            </Form.Item>
          </Form>
        </div>
      </template>

      <!-- Step 1: 匹配配置 -->
      <template #step-1>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">配置匹配规则</div>
            <div class="panel-desc">选择识别方式并填写匹配表达式</div>
          </div>
          <Form :model="formValues" layout="vertical" class="panel-form">
            <Form.Item label="规则类型" required>
              <Select
                v-model:value="formValues.ruleType"
                placeholder="请选择规则类型"
                @change="
                  () => {
                    formValues.ruleExpr = ruleExprExamples[formValues.ruleType] || '';
                  }
                "
              >
                <Select.Option
                  v-for="opt in ruleTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  <div class="rule-type-option">
                    <Tag :color="ruleTypeTagColor[opt.value]">{{ opt.label }}</Tag>
                    <span class="rule-type-desc">{{ opt.desc }}</span>
                  </div>
                </Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              v-if="formValues.ruleType && formValues.ruleType !== 'CUSTOM'"
              :label="formValues.ruleType === 'REGEX' ? '正则表达式' : formValues.ruleType === 'COLUMN_NAME' ? '字段名匹配' : '数据类型匹配'"
              required
            >
              <Input.TextArea
                v-model:value="formValues.ruleExpr"
                :placeholder="currentPlaceholder"
                :rows="4"
                :maxlength="1000"
                show-count
              />
              <div class="form-tip">
                <BulbOutlined />
                <span v-if="formValues.ruleType === 'REGEX'">
                  正则支持忽略大小写 (?i)，示例：<code>(?i)(.*phone|.*email)</code>
                </span>
                <span v-else-if="formValues.ruleType === 'COLUMN_NAME'">
                  多个字段名用英文逗号分隔，支持模糊匹配
                </span>
                <span v-else>
                  多个类型用英文逗号分隔，匹配字段的 dataType
                </span>
              </div>
            </Form.Item>

            <Form.Item
              v-if="formValues.ruleType === 'CUSTOM'"
              label="自定义规则表达式"
            >
              <Input.TextArea
                v-model:value="formValues.ruleExpr"
                placeholder="请输入自定义规则脚本或表达式"
                :rows="4"
                :maxlength="2000"
                show-count
              />
            </Form.Item>

            <Alert
              v-if="currentExprExample && formValues.ruleType !== 'CUSTOM'"
              :message="`表达式示例（可复制后修改）`"
              :description="currentExprExample"
              type="info"
              show-icon
              class="mt-4"
            >
              <template #action>
                <a
                  class="copy-btn"
                  @click="
                    () => {
                      navigator.clipboard.writeText(currentExprExample);
                      formValues.ruleExpr = currentExprExample;
                      message.success('已复制并填入');
                    }
                  "
                >复制使用</a>
              </template>
            </Alert>
          </Form>
        </div>
      </template>

      <!-- Step 2: 目标映射 -->
      <template #step-2>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">配置识别结果</div>
            <div class="panel-desc">设置匹配成功后的敏感等级和数据分类</div>
          </div>
          <Form :model="formValues" layout="vertical" class="panel-form">
            <Form.Item label="目标敏感等级" required>
              <Select
                v-model:value="formValues.targetLevelCode"
                placeholder="请选择敏感等级"
                show-search
                :loading="levelLoading"
                :filter-option="
                  (input, option) =>
                    String(option?.label ?? '')
                      .toLowerCase()
                      .includes(input.toLowerCase())
                "
              >
                <Select.Option
                  v-for="opt in levelOptions"
                  :key="opt.value"
                  :value="opt.value"
                  :label="opt.label"
                >
                  <Tag color="red">{{ opt.label }}</Tag>
                </Select.Option>
              </Select>
              <div v-if="levelOptions.length === 0" class="form-tip warning">
                <InfoCircleOutlined />
                暂无可用敏感等级，请先去「敏感等级管理」中添加
              </div>
            </Form.Item>

            <Form.Item label="目标数据分类" >
              <Select
                v-model:value="formValues.targetClassCode"
                placeholder="可选项，留空则不归类"
                show-search
                allow-clear
                :loading="classificationLoading"
                :filter-option="
                  (input, option) =>
                    String(option?.label ?? '')
                      .toLowerCase()
                      .includes(input.toLowerCase())
                "
              >
                <Select.Option
                  v-for="opt in classificationOptions"
                  :key="opt.value"
                  :value="opt.value"
                  :label="opt.label"
                >
                  {{ opt.label }}
                </Select.Option>
              </Select>
              <div class="form-tip">
                <InfoCircleOutlined />
                数据分类用于进一步归类敏感字段，可不选
              </div>
            </Form.Item>

            <Alert
              v-if="formValues.targetLevelCode"
              :message="`当前配置预览`"
              type="success"
              show-icon
              class="mt-4"
            >
              <template #description>
                <div class="preview-summary">
                  <span>
                    匹配方式：
                    <Tag :color="currentTypeTagColor">
                      {{ ruleTypeOptions.find((t) => t.value === formValues.ruleType)?.label || '-' }}
                    </Tag>
                  </span>
                  <span>
                    识别等级：
                    <Tag color="red">{{ levelNameMap[formValues.targetLevelCode] || formValues.targetLevelCode }}</Tag>
                  </span>
                  <span v-if="formValues.targetClassCode">
                    数据分类：
                    {{ classNameMap[formValues.targetClassCode] || formValues.targetClassCode }}
                  </span>
                </div>
              </template>
            </Alert>
          </Form>
        </div>
      </template>

      <!-- Step 3: 预览保存 -->
      <template #step-3>
        <div class="step-panel">
          <div class="panel-head">
            <div class="panel-title">确认配置</div>
            <div class="panel-desc">请仔细核对以下配置，确认无误后保存</div>
          </div>
          <Descriptions
            :column="1"
            bordered
            size="small"
            class="preview-descriptions"
          >
            <Descriptions.Item label="规则名称">
              <b>{{ formValues.ruleName || '-' }}</b>
            </Descriptions.Item>
            <Descriptions.Item label="规则编码">
              <code>{{ formValues.ruleCode || '-' }}</code>
            </Descriptions.Item>
            <Descriptions.Item label="规则类型">
              <Tag :color="currentTypeTagColor">
                {{ ruleTypeOptions.find((t) => t.value === formValues.ruleType)?.label || '-' }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="规则表达式">
              <div class="expr-preview">
                <Input.TextArea
                  :value="formValues.ruleExpr || '(未填写)'"
                  :rows="3"
                  read-only
                  :class="{ 'text-gray-400': !formValues.ruleExpr }"
                />
              </div>
            </Descriptions.Item>
            <Descriptions.Item label="目标敏感等级">
              <Tag color="red">
                {{ levelNameMap[formValues.targetLevelCode] || formValues.targetLevelCode || '(未选择)' }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="目标数据分类">
              {{ formValues.targetClassCode ? (classNameMap[formValues.targetClassCode] || formValues.targetClassCode) : '(未选择)' }}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag :color="formValues.enabled === '0' ? 'green' : 'default'">
                {{ formValues.enabled === '0' ? '启用' : '停用' }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="是否内置">
              <Tag :color="formValues.builtin === '1' ? 'purple' : 'default'">
                {{ formValues.builtin === '1' ? '是（内置规则）' : '否（自定义规则）' }}
              </Tag>
            </Descriptions.Item>
          </Descriptions>
        </div>
      </template>
    </WizardDrawer>
  </BasicDrawer>
</template>

<style scoped>
.step-panel {
  padding: 8px 4px;
}
.panel-head {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f1f1f;
  margin-bottom: 4px;
}
.panel-desc {
  font-size: 13px;
  color: #8c8c8c;
}
.panel-form {
  max-width: 560px;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
  line-height: 1.5;
}
.form-tip.warning {
  color: #fa8c16;
}
.form-tip code {
  background: #f5f5f5;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 12px;
  color: #d46b08;
}
.rule-type-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rule-type-desc {
  font-size: 12px;
  color: #8c8c8c;
}
.copy-btn {
  font-size: 12px;
  cursor: pointer;
  color: #1677ff;
}
.preview-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.preview-summary span {
  font-size: 13px;
}
.preview-descriptions {
  max-width: 600px;
}
.expr-preview :deep(textarea) {
  resize: none;
}
.text-gray-400 {
  color: #d9d9d9;
}
</style>
