<script setup lang="ts">
import type { FormInstance } from 'ant-design-vue';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import {
  Button,
  Divider,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Space,
  message,
} from 'ant-design-vue';
import { ThunderboltOutlined } from '@ant-design/icons-vue';

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

const enabledOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
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
  <BasicDrawer :title="title" class="w-[720px]">
    <Form
      ref="formRef"
      :model="formValues"
      :rules="formRules"
      layout="vertical"
      class="mt-2"
    >
      <Form.Item label="模板名称" name="templateName" required>
        <Input
          v-model:value="formValues.templateName"
          placeholder="请输入模板名称，如：手机号中间脱敏"
          :maxlength="50"
          show-count
        />
      </Form.Item>
      <Form.Item label="模板编码" name="templateCode" required>
        <Input
          v-model:value="formValues.templateCode"
          placeholder="请输入唯一编码，如：PHONE_MASK"
          :disabled="!!recordId"
          :maxlength="50"
          show-count
        />
      </Form.Item>
      <Form.Item label="脱敏类型" name="templateType" required>
        <Select
          v-model:value="formValues.templateType"
          :options="maskTypeOptions"
          placeholder="请选择脱敏类型"
          allow-clear
        />
      </Form.Item>

      <template v-if="showMaskDetail">
        <Form.Item label="掩码字符" name="maskChar">
          <Input
            v-model:value="formValues.maskChar"
            placeholder="默认 *"
            :maxlength="1"
            style="width: 100px"
          />
          <span class="ml-3 text-muted-foreground text-sm">用于遮蔽的字符</span>
        </Form.Item>
        <Form.Item label="遮蔽位置" name="maskPosition">
          <Select
            v-model:value="formValues.maskPosition"
            :options="positionOptions"
            placeholder="请选择遮蔽位置"
          />
        </Form.Item>
        <Form.Item label="保留头部" name="maskHeadKeep">
          <InputNumber
            v-model:value="formValues.maskHeadKeep"
            :min="0"
            :max="20"
            placeholder="前 N 位"
            class="w-[140px]"
          />
        </Form.Item>
        <Form.Item label="保留尾部" name="maskTailKeep">
          <InputNumber
            v-model:value="formValues.maskTailKeep"
            :min="0"
            :max="20"
            placeholder="后 N 位"
            class="w-[140px]"
          />
        </Form.Item>
      </template>

      <Form.Item label="掩码正则" name="maskPattern">
        <Input.TextArea
          v-model:value="formValues.maskPattern"
          placeholder="可选，如: (\\d{3})\\d{4}(\\d{4}) → $1****$2"
          :rows="2"
          :maxlength="500"
          show-count
        />
        <div class="text-muted-foreground mt-1 text-xs">
          用于高级替换的正则或替换规则说明（预览以类型与保留位为准；正则执行由服务端/引擎实现）
        </div>
      </Form.Item>

      <Form.Item label="SQL 脱敏表达式" name="maskExpr">
        <Input.TextArea
          v-model:value="formValues.maskExpr"
          placeholder="可选，执行层 SQL 片段，如: CONCAT(LEFT({col}, 3), '****', RIGHT({col}, 4))"
          :rows="2"
          :maxlength="500"
          show-count
        />
      </Form.Item>

      <Form.Item label="模板描述" name="templateDesc">
        <Input.TextArea
          v-model:value="formValues.templateDesc"
          placeholder="说明适用场景与管理规范"
          :rows="2"
          :maxlength="500"
          show-count
        />
      </Form.Item>
      <Form.Item label="状态" name="enabled">
        <Radio.Group v-model:value="formValues.enabled" :options="enabledOptions" />
      </Form.Item>
    </Form>

    <Divider orientation="left">
      <span class="inline-flex items-center gap-1 text-sm font-medium">
        <ThunderboltOutlined class="text-orange-500" />
        脱敏预览
      </span>
    </Divider>
    <div class="bg-muted/30 rounded-lg p-4">
      <Form layout="vertical" class="mb-0">
        <Form.Item label="测试输入" class="mb-3">
          <Input
            v-model:value="previewInput"
            :placeholder="previewPlaceholder(formValues.templateType)"
            allow-clear
          />
        </Form.Item>
        <Form.Item label="预览结果" class="mb-2">
          <div
            class="bg-background border-border min-h-[40px] rounded border px-3 py-2 font-mono text-sm"
          >
            {{ previewResult || '预览结果将显示在这里' }}
          </div>
        </Form.Item>
        <Space>
          <Button @click="clearPreview">清空</Button>
          <Button type="primary" @click="fillSamplePreview">
            <template #icon>
              <ThunderboltOutlined />
            </template>
            示例数据
          </Button>
        </Space>
      </Form>
    </div>
  </BasicDrawer>
</template>
