import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { markRaw } from 'vue';

import { DictEnum } from '@vben/constants';

import { Button, Tag } from 'ant-design-vue';

import { getDictOptions } from '#/utils/dict';

import ConnectionParamsEditor from './connection-params-editor.vue';

/** 数据源类型选项 */
export const datasourceTypeOptions = [
  { label: 'MySQL', value: 'MYSQL', color: 'blue' },
  { label: 'TiDB', value: 'TIDB', color: 'cyan' },
  { label: 'PostgreSQL', value: 'POSTGRESQL', color: 'green' },
  { label: 'SQL Server', value: 'SQLSERVER', color: 'orange' },
  { label: 'Oracle', value: 'ORACLE', color: 'red' },
];

/** 是否需要显示 Schema 配置 */
export function supportsSchema(dsType?: string) {
  return dsType?.toUpperCase() === 'POSTGRESQL';
}

/** 数仓层选项 */
export const dataLayerOptions = [
  { label: 'ODS（原始层）', value: 'ODS' },
  { label: 'DWD（明细层）', value: 'DWD' },
  { label: 'DWS（汇总层）', value: 'DWS' },
  { label: 'ADS（应用层）', value: 'ADS' },
];

/** 数据来源选项 */
export const dataSourceOptions = [
  { label: 'K3DC', value: 'K3DC' },
  { label: 'K3HW', value: 'K3HW' },
  { label: 'K1', value: 'K1' },
  { label: 'K2', value: 'K2' },
  { label: 'OTHER', value: 'OTHER' },
];

/** 数据源标识选项 */
export const datasourceFlagOptions = [
  { label: '内部', value: '0', color: 'green' },
  { label: '外部', value: '1', color: 'orange' },
];

/** 查询表单 */
export const querySchema: FormSchemaGetter = () => [
  {
    component: 'Input',
    fieldName: 'dsName',
    label: '数据源名称',
  },
  {
    component: 'Input',
    fieldName: 'dsCode',
    label: '数据源编码',
  },
  {
    component: 'Select',
    componentProps: {
      options: datasourceTypeOptions,
    },
    fieldName: 'dsType',
    label: '数据源类型',
  },
  {
    component: 'Select',
    componentProps: {
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
    },
    fieldName: 'status',
    label: '状态',
  },
  {
    component: 'Select',
    componentProps: {
      options: dataLayerOptions,
    },
    fieldName: 'dataLayer',
    label: '数仓层',
  },
  {
    component: 'Select',
    componentProps: {
      options: dataSourceOptions,
    },
    fieldName: 'dataSource',
    label: '数据来源',
  },
  {
    component: 'Select',
    componentProps: {
      options: datasourceFlagOptions.map(({ label, value }) => ({ label, value })),
    },
    fieldName: 'dsFlag',
    label: '数据源标识',
  },
];

/** 表格列定义 */
export const columns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60 },
  {
    title: '数据源名称',
    field: 'dsName',
    minWidth: 160,
  },
  {
    title: '数据源编码',
    field: 'dsCode',
    minWidth: 140,
    slots: {
      default: ({ row }) => {
        return <Tag color="processing">{row.dsCode}</Tag>;
      },
    },
  },
  {
    title: '数据源类型',
    field: 'dsType',
    width: 130,
    slots: {
      default: ({ row }) => {
        const found = datasourceTypeOptions.find((item) => item.value === row.dsType);
        if (found) {
          return <Tag color={found.color}>{found.label}</Tag>;
        }
        return <Tag>{row.dsType}</Tag>;
      },
    },
  },
  {
    title: '数据来源',
    field: 'dataSource',
    width: 120,
    slots: {
      default: ({ row }) => {
        if (row.dataSource) {
          return <Tag color="geekblue">{row.dataSource}</Tag>;
        }
        return <span>-</span>;
      },
    },
  },
  {
    title: '数据源标识',
    field: 'dsFlag',
    width: 110,
    slots: {
      default: ({ row }) => {
        const found = datasourceFlagOptions.find((item) => item.value === row.dsFlag);
        if (found) {
          return <Tag color={found.color}>{found.label}</Tag>;
        }
        return <span>-</span>;
      },
    },
  },
  {
    title: '主机',
    field: 'host',
    minWidth: 140,
  },
  {
    title: '端口',
    field: 'port',
    width: 80,
  },
  {
    title: '数据库',
    field: 'databaseName',
    minWidth: 120,
  },
  {
    title: '数仓层',
    field: 'dataLayer',
    width: 120,
    slots: {
      default: ({ row }) => {
        if (row.dataLayer) {
          return <Tag>{row.dataLayer}</Tag>;
        }
        return <span>-</span>;
      },
    },
  },
  {
    title: '状态',
    field: 'status',
    width: 90,
    slots: { default: 'status' },
  },
  {
    title: '创建时间',
    field: 'createTime',
    width: 180,
  },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 220,
  },
];

interface DrawerSchemaOptions {
  onGenerateCode?: () => void;
}

/** 表单弹窗 schema */
export function drawerSchema(
  options: DrawerSchemaOptions = {},
): ReturnType<FormSchemaGetter> {
  const { onGenerateCode } = options;

  return [
  {
    component: 'Input',
    dependencies: {
      show: () => false,
      triggerFields: [''],
    },
    fieldName: 'dsId',
    label: '数据源ID',
  },
  {
    component: 'Divider',
    componentProps: {
      orientation: 'center',
    },
    fieldName: 'divider-basic',
    formItemClass: 'col-span-2',
    hideLabel: true,
    renderComponentContent: () => ({
      default: () => '基础信息',
    }),
  },
  {
    component: 'Input',
    fieldName: 'dsName',
    label: '数据源名称',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'dsCode',
    help: '留空时按类型自动生成全局唯一编码',
    label: '数据源编码',
    renderComponentContent: () => ({
      addonAfter: () => (
        <Button size="small" type="link" onClick={() => onGenerateCode?.()}>
          自动生成
        </Button>
      ),
    }),
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: datasourceTypeOptions,
    },
    fieldName: 'dsType',
    label: '数据源类型',
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      options: dataLayerOptions,
    },
    fieldName: 'dataLayer',
    label: '数仓层',
  },
  {
    component: 'Select',
    componentProps: {
      options: dataSourceOptions,
      showSearch: true,
    },
    fieldName: 'dataSource',
    label: '数据来源',
  },
  {
    component: 'RadioGroup',
    componentProps: {
      buttonStyle: 'solid',
      options: datasourceFlagOptions.map(({ label, value }) => ({ label, value })),
      optionType: 'button',
    },
    defaultValue: '0',
    fieldName: 'dsFlag',
    label: '数据源标识',
  },
  {
    component: 'Select',
    componentProps: {
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
    },
    defaultValue: '0',
    fieldName: 'status',
    label: '状态',
    rules: 'required',
  },
  {
    component: 'Divider',
    componentProps: {
      orientation: 'center',
    },
    fieldName: 'divider-connection',
    formItemClass: 'col-span-2',
    hideLabel: true,
    renderComponentContent: () => ({
      default: () => '连接信息',
    }),
  },
  {
    component: 'Input',
    fieldName: 'host',
    label: '主机地址',
    rules: 'required',
  },
  {
    component: 'InputNumber',
    fieldName: 'port',
    label: '端口',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'databaseName',
    help: '可不填，按数据源类型确定是否需要',
    label: '数据库名',
  },
  {
    component: 'Input',
    dependencies: {
      show: (values) => supportsSchema(values.dsType),
      triggerFields: ['dsType'],
    },
    fieldName: 'schemaName',
    label: 'Schema名称',
    help: '仅 PostgreSQL 等需要指定 Schema',
  },
  {
    component: 'Input',
    fieldName: 'username',
    label: '用户名',
    rules: 'required',
  },
  {
    component: 'InputPassword',
    componentProps: {
      visibilityToggle: true,
    },
    fieldName: 'password',
    label: '密码',
    rules: 'required',
  },
  {
    component: 'Divider',
    componentProps: {
      orientation: 'center',
    },
    fieldName: 'divider-advanced',
    formItemClass: 'col-span-2',
    hideLabel: true,
    renderComponentContent: () => ({
      default: () => '高级参数',
    }),
  },
  {
    component: markRaw(ConnectionParamsEditor),
    fieldName: 'connectionParams',
    help: '支持按行 key=value、querystring 或 JSON，保存时统一转为 key=value 多行格式',
    label: '高级参数',
    formItemClass: 'col-span-2 items-start',
  },
  {
    component: 'Textarea',
    fieldName: 'remark',
    label: '备注',
    formItemClass: 'col-span-2',
  },
  ];
}
