import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { DictEnum } from '@vben/constants';

import { Tag } from 'ant-design-vue';

import { getDictOptions } from '#/utils/dict';

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
    width: 180,
  },
];

/** 表单抽屉 schema */
export const drawerSchema: FormSchemaGetter = () => [
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
    component: 'Input',
    fieldName: 'dsName',
    label: '数据源名称',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'dsCode',
    label: '数据源编码',
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
    label: '数据库名',
    rules: 'required',
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
    fieldName: 'password',
    label: '密码',
    rules: 'required',
  },
  {
    component: 'Textarea',
    fieldName: 'connectionParams',
    label: '连接参数',
    help: '额外JDBC连接参数，JSON格式，如：{"connectTimeout":5000}',
    formItemClass: 'col-span-2',
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
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
    },
    defaultValue: '0',
    fieldName: 'status',
    label: '状态',
    rules: 'required',
  },
  {
    component: 'Textarea',
    fieldName: 'remark',
    label: '备注',
    formItemClass: 'col-span-2',
  },
];
