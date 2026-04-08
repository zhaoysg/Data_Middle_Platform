<script setup lang="ts">
import type { FormInstance } from 'ant-design-vue';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Space,
  message,
} from 'ant-design-vue';
import {
  CodeOutlined,
  ControlOutlined,
  FileTextOutlined,
  ProfileOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue';

import {
  secMaskTemplateAdd,
  secMaskTemplateInfo,
  secMaskTemplateUpdate,
} from '#/api/metadata/security/mask-template';

import {
  applyMask,
  previewPlaceholder,
  sampleInputForType,
} from './mask-preview';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formRef = ref<FormInstance>();
const formValues = ref<Record<string, any>>({});
const previewInput = ref('');

const maskTypeOptions = [
  { label: '不脱敏 (NONE)', value: 'NONE' },
  { label: '掩码遮蔽 (MASK)', value: 'MASK' },
  { label: '完全隐藏 (HIDE)', value: 'HIDE' },
  { label: '加密 (ENCRYPT)', value: 'ENCRYPT' },
  { label: '删除 (DELETE)', value: 'DELETE' },
  { label: '哈希 (HASH)', value: 'HASH' },
  { label: '保留格式加密 (FORMAT_KEEP)', value: 'FORMAT_KEEP' },
  { label: '区间化 (RANGE)', value: 'RANGE' },
  { label: '水印 (WATERMARK)', value: 'WATERMARK' },
  { label: '打乱 (SHUFFLE)', value: 'SHUFFLE' },
  { label: '自定义 (CUSTOM)', value: 'CUSTOM' },
];

const positionOptions = [
  { label: '全部遮蔽', value: 'ALL' },
  { label: '头部遮蔽', value: 'HEAD' },
  { label: '尾部遮蔽', value: 'TAIL' },
  { label: '中间遮蔽（默认）', value: 'CENTER' },
];

const formRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' as const }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' as const }],
  templateType: [{ required: true, message: '请选择脱敏类型', trigger: 'change' as const }],
};

const title = computed(() => {
  return recordId.value ? '编辑脱敏模板' : '新增脱敏模板';
});

const showMaskDetail = computed(() => {
  const t = formValues.value.templateType;
  return t === 'MASK' || t === 'HIDE';
});

const previewResult = computed(() => {
  const t = formValues.value.templateType;
  if (!previewInput.value || !t) {
    return '';
  }
  return applyMask(
    previewInput.value,
    t,
    formValues.value.maskPattern,
    formValues.value.maskChar,
    formValues.value.maskHeadKeep,
    formValues.value.maskTailKeep,
    formValues.value.maskPosition,
  );
});

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
      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await secMaskTemplateInfo(id);
        formValues.value = {
          ...Object.fromEntries(
            Object.entries({ ...info })
              .filter(([, v]) => v !== undefined && v !== null)
              .map(([k, v]) => [k, v === 'undefined' ? '' : v]),
          ),
          maskChar: info.maskChar ?? '*',
          maskPosition: info.maskPosition ?? 'CENTER',
          maskHeadKeep: info.maskHeadKeep ?? 3,
          maskTailKeep: info.maskTailKeep ?? 4,
        };
        previewInput.value = '';
      } else {
        recordId.value = undefined;
        formValues.value = {
          enabled: '0',
          builtin: '0',
          maskChar: '*',
          maskPosition: 'CENTER',
          maskHeadKeep: 3,
          maskTailKeep: 4,
          templateType: undefined,
          templateName: '',
          templateCode: '',
          templateDesc: '',
          maskPattern: '',
          maskExpr: '',
        };
        previewInput.value = '';
      }
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

watch(
  () => formValues.value.templateType,
  () => {
    previewInput.value = '';
  },
);

function handleClosed() {
  recordId.value = undefined;
  formValues.value = {};
  previewInput.value = '';
  formRef.value?.resetFields();
}

function fillSamplePreview() {
  previewInput.value = sampleInputForType(formValues.value.templateType);
}

function clearPreview() {
  previewInput.value = '';
}

async function handleSubmit() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  drawerApi.lock(true);
  try {
    if (recordId.value) {
      await secMaskTemplateUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await secMaskTemplateAdd(formValues.value);
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
</script>

<template>
  <BasicDrawer :title="title" class="mask-template-drawer w-[736px]">
    <Form
      ref="formRef"
      :model="formValues"
      :rules="formRules"
      class="mt-1 flex flex-col gap-5"
      layout="vertical"
    >
      <Card
        :bordered="false"
        class="border-border/70 shadow-none ring-1 ring-border/60"
        size="small"
      >
        <template #title>
          <span class="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
            <ProfileOutlined class="text-primary text-base" />
            基础信息
          </span>
        </template>
        <div class="grid gap-x-4 gap-y-0 sm:grid-cols-2">
          <Form.Item class="mb-4 sm:col-span-2" label="模板名称" name="templateName" required>
            <Input
              v-model:value="formValues.templateName"
              placeholder="请输入模板名称，如：手机号中间脱敏"
              :maxlength="50"
              show-count
            />
          </Form.Item>
          <Form.Item class="mb-4" label="模板编码" name="templateCode" required>
            <Input
              v-model:value="formValues.templateCode"
              placeholder="唯一编码，如：PHONE_MASK"
              :disabled="!!recordId"
              :maxlength="50"
              show-count
            />
          </Form.Item>
          <Form.Item class="mb-4" label="脱敏类型" name="templateType" required>
            <Select
              v-model:value="formValues.templateType"
              :options="maskTypeOptions"
              placeholder="请选择脱敏类型"
              allow-clear
            />
          </Form.Item>
        </div>
      </Card>

      <Card
        v-if="showMaskDetail"
        :bordered="false"
        class="border-border/70 shadow-none ring-1 ring-border/60"
        size="small"
      >
        <template #title>
          <span class="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
            <ControlOutlined class="text-primary text-base" />
            掩码参数
          </span>
        </template>
        <p class="text-muted-foreground mb-4 text-xs leading-relaxed">
          根据遮蔽位置与保留位数控制可见片段；下方预览可即时校验效果。
        </p>
        <Form.Item class="mb-4" label="掩码字符" name="maskChar">
          <div class="flex flex-wrap items-center gap-3">
            <Input
              v-model:value="formValues.maskChar"
              class="w-[88px] font-mono text-center"
              placeholder="*"
              :maxlength="1"
            />
            <span class="text-muted-foreground text-sm">用于替换被遮蔽位置的字符</span>
          </div>
        </Form.Item>
        <Form.Item class="mb-4" label="遮蔽位置" name="maskPosition">
          <Select
            v-model:value="formValues.maskPosition"
            :options="positionOptions"
            placeholder="请选择遮蔽位置"
          />
        </Form.Item>
        <div class="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
          <Form.Item class="mb-0 sm:mb-4" label="保留头部" name="maskHeadKeep">
            <InputNumber
              v-model:value="formValues.maskHeadKeep"
              :min="0"
              :max="20"
              class="w-full max-w-[200px]"
              placeholder="前 N 位"
            />
          </Form.Item>
          <Form.Item class="mb-0" label="保留尾部" name="maskTailKeep">
            <InputNumber
              v-model:value="formValues.maskTailKeep"
              :min="0"
              :max="20"
              class="w-full max-w-[200px]"
              placeholder="后 N 位"
            />
          </Form.Item>
        </div>
      </Card>

      <Card
        :bordered="false"
        class="border-border/70 shadow-none ring-1 ring-border/60"
        size="small"
      >
        <template #title>
          <span class="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
            <CodeOutlined class="text-primary text-base" />
            高级与执行层
          </span>
        </template>
        <Form.Item class="mb-4" label="掩码正则" name="maskPattern">
          <Input.TextArea
            v-model:value="formValues.maskPattern"
            placeholder="可选，如: (\\d{3})\\d{4}(\\d{4}) → $1****$2"
            :auto-size="{ minRows: 3, maxRows: 8 }"
            :maxlength="500"
            class="font-mono text-sm"
            show-count
          />
          <Alert
            class="mt-2 text-xs leading-relaxed"
            message="用于高级替换的正则或规则说明。预览仍以当前类型与保留位为准；实际正则由服务端或执行引擎解析。"
            show-icon
            type="info"
          />
        </Form.Item>
        <Form.Item class="mb-0" label="SQL 脱敏表达式" name="maskExpr">
          <Input.TextArea
            v-model:value="formValues.maskExpr"
            placeholder="可选，如: CONCAT(LEFT({col}, 3), '****', RIGHT({col}, 4))"
            :auto-size="{ minRows: 3, maxRows: 8 }"
            :maxlength="500"
            class="font-mono text-sm"
            show-count
          />
        </Form.Item>
      </Card>

      <Card
        :bordered="false"
        class="border-border/70 shadow-none ring-1 ring-border/60"
        size="small"
      >
        <template #title>
          <span class="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
            <FileTextOutlined class="text-primary text-base" />
            说明与状态
          </span>
        </template>
        <Form.Item class="mb-4" label="模板描述" name="templateDesc">
          <Input.TextArea
            v-model:value="formValues.templateDesc"
            placeholder="说明适用场景、合规要求或管理规范"
            :auto-size="{ minRows: 2, maxRows: 6 }"
            :maxlength="500"
            show-count
          />
        </Form.Item>
        <Form.Item class="mb-0" label="状态" name="enabled">
          <Radio.Group v-model:value="formValues.enabled" button-style="solid">
            <Radio.Button value="0">启用</Radio.Button>
            <Radio.Button value="1">停用</Radio.Button>
          </Radio.Group>
        </Form.Item>
      </Card>
    </Form>

    <Card
      :bordered="false"
      class="border-border/80 from-primary/5 mt-5 shadow-none ring-1 ring-border/70 bg-gradient-to-br to-transparent"
      size="small"
    >
      <template #title>
        <span class="inline-flex items-center gap-2 text-sm font-semibold text-foreground">
          <ThunderboltOutlined class="text-amber-500" />
          脱敏预览
        </span>
      </template>
      <p class="text-muted-foreground mb-4 text-xs">
        输入样例数据，快速核对当前模板下的前端预览效果。
      </p>
      <Form class="mb-0" layout="vertical">
        <Form.Item class="mb-3" label="测试输入">
          <Input
            v-model:value="previewInput"
            :placeholder="previewPlaceholder(formValues.templateType)"
            allow-clear
          />
        </Form.Item>
        <Form.Item class="mb-3" label="预览结果">
          <div
            class="bg-background/80 border-border min-h-[44px] rounded-md border border-dashed px-3 py-2.5 font-mono text-sm tracking-wide"
            :class="previewResult ? 'text-foreground' : 'text-muted-foreground'"
          >
            {{ previewResult || '填写测试输入后显示预览' }}
          </div>
        </Form.Item>
        <Space wrap>
          <Button @click="clearPreview">清空</Button>
          <Button type="primary" @click="fillSamplePreview">
            <template #icon>
              <ThunderboltOutlined />
            </template>
            填入示例
          </Button>
        </Space>
      </Form>
    </Card>
  </BasicDrawer>
</template>
