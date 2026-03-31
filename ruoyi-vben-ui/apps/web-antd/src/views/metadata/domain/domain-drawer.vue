<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { Form, Input, Radio, Select, TreeSelect, message } from 'ant-design-vue';

import {
  metadataDomainAdd,
  metadataDomainInfo,
  metadataDomainUpdate,
} from '#/api/metadata/domain';
import { getDeptTree, listUserByDeptId } from '#/api/system/user';

const emit = defineEmits<{ reload: [] }>();

const recordId = ref<number>();
const formValues = ref<Record<string, any>>({});
const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];
const deptOptions = ref<Record<string, any>[]>([]);
const ownerOptions = ref<{ label: string; value: number }[]>([]);

const title = computed(() => {
  return recordId.value ? '编辑数据域' : '新增数据域';
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
      await loadDeptOptions();
      const { id } = drawerApi.getData() as { id?: number };
      if (id) {
        recordId.value = id;
        const info = await metadataDomainInfo(id);
        formValues.value = { ...info };
        if (info.deptId) {
          await loadUsersByDept(info.deptId);
        }
      } else {
        recordId.value = undefined;
        formValues.value = { status: '0' };
        ownerOptions.value = [];
      }
    } finally {
      drawerApi.drawerLoading(false);
    }
  },
});

async function loadDeptOptions() {
  const tree = await getDeptTree();
  deptOptions.value = mapDeptTree(tree || []);
}

async function loadUsersByDept(deptId?: number) {
  ownerOptions.value = [];
  if (!deptId) {
    formValues.value.ownerId = undefined;
    return;
  }
  const users = await listUserByDeptId(deptId);
  ownerOptions.value = (users || []).map((user) => ({
    label: `${user.nickName} (${user.userName})`,
    value: Number(user.userId),
  }));
}

async function handleSubmit() {
  drawerApi.lock(true);
  try {
    if (recordId.value) {
      await metadataDomainUpdate({ id: recordId.value, ...formValues.value });
    } else {
      await metadataDomainAdd(formValues.value);
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
  ownerOptions.value = [];
}

async function handleDeptChange(deptId?: number) {
  await loadUsersByDept(deptId);
}

function mapDeptTree(tree: any[]): Record<string, any>[] {
  return tree.map((item) => ({
    label: item.label,
    value: item.id,
    children: item.children ? mapDeptTree(item.children) : undefined,
  }));
}
</script>

<template>
  <BasicDrawer :title="title" class="w-[500px]">
    <Form :model="formValues" layout="vertical" class="mt-4">
      <Form.Item label="数据域名称" name="domainName">
        <Input v-model:value="formValues.domainName" placeholder="请输入数据域名称" />
      </Form.Item>
      <Form.Item label="数据域编码" name="domainCode">
        <Input v-model:value="formValues.domainCode" placeholder="请输入数据域编码" />
      </Form.Item>
      <Form.Item label="数据域描述" name="domainDesc">
        <Input.TextArea
          v-model:value="formValues.domainDesc"
          placeholder="请输入描述"
          :rows="3"
        />
      </Form.Item>
      <Form.Item label="所属部门" name="deptId">
        <TreeSelect
          v-model:value="formValues.deptId"
          :allow-clear="true"
          :tree-data="deptOptions"
          placeholder="请选择所属部门"
          style="width: 100%"
          @change="handleDeptChange"
        />
      </Form.Item>
      <Form.Item label="负责人" name="ownerId">
        <Select
          v-model:value="formValues.ownerId"
          :allow-clear="true"
          :disabled="!formValues.deptId"
          :options="ownerOptions"
          placeholder="请选择负责人"
        />
      </Form.Item>
      <Form.Item label="状态" name="status">
        <Radio.Group v-model:value="formValues.status" :options="statusOptions" />
      </Form.Item>
      <Form.Item label="备注" name="remark">
        <Input.TextArea
          v-model:value="formValues.remark"
          placeholder="请输入备注"
          :rows="2"
        />
      </Form.Item>
    </Form>
  </BasicDrawer>
</template>
