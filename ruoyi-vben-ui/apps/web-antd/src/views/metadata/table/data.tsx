import type { VbenFormSchema } from '@vben/common-ui';
import type { VxeGridPropTypes } from '#/adapter/vxe-table';

import { computed, h, ref } from 'vue';

import { Tag } from 'ant-design-vue';

import { metadataCatalogTree } from '#/api/metadata/catalog';

export const querySchema = computed((): VbenFormSchema[] => [
  {
    fieldName: 'dsId',
    label: '数据源',
    component: 'ApiSelect',
    componentProps: {
      api: async () => {
        const { datasourceEnabled } = await import('#/api/system/datasource');
        const data = await datasourceEnabled();
        return data.map((item: any) => ({ label: item.dsName, value: item.dsId }));
      },
      placeholder: '请选择数据源',
      clearable: true,
    },
    colProps: { span: 6 },
  },
  {
    fieldName: 'keyword',
    label: '关键字',
    component: 'Input',
    componentProps: { placeholder: '表名/别名/注释', clearable: true },
    colProps: { span: 6 },
  },
  {
    fieldName: 'dataLayer',
    label: '数仓分层',
    component: 'Select',
    componentProps: {
      options: [
        { label: 'ODS', value: 'ODS' },
        { label: 'DWD', value: 'DWD' },
        { label: 'DWS', value: 'DWS' },
        { label: 'ADS', value: 'ADS' },
      ],
      placeholder: '请选择分层',
      clearable: true,
    },
    colProps: { span: 6 },
  },
  {
    fieldName: 'dataDomain',
    label: '数据域',
    component: 'Input',
    componentProps: { placeholder: '请输入数据域', clearable: true },
    colProps: { span: 6 },
  },
  {
    fieldName: 'catalogId',
    label: '资产目录',
    component: 'TreeSelect',
    componentProps: {
      placeholder: '请选择资产目录',
      clearable: true,
      checkable: false,
      loadData: async () => {
        const treeData = await metadataCatalogTree();
        return treeData || [];
      },
      fieldNames: { label: 'catalogName', value: 'id', children: 'children' },
    },
    colProps: { span: 6 },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: [
        { label: '活跃', value: 'ACTIVE' },
        { label: '归档', value: 'ARCHIVED' },
        { label: '废弃', value: 'DEPRECATED' },
      ],
      placeholder: '请选择状态',
      clearable: true,
    },
    colProps: { span: 6 },
  },
]);

const layerColorMap: Record<string, string> = {
  ODS: '#909399',
  DWD: '#409EFF',
  DWS: '#67C23A',
  ADS: '#E6A23C',
};

const sensitivityColorMap: Record<string, string> = {
  NORMAL: 'green',
  INNER: 'blue',
  SENSITIVE: 'orange',
  HIGHLY_SENSITIVE: 'red',
};

export const columns: VxeGridPropTypes.Columns = [
  { type: 'checkbox', width: 50, fixed: 'left' },
  {
    title: '表名',
    field: 'tableName',
    width: 180,
    fixed: 'left',
    sortable: true,
  },
  {
    title: '表别名',
    field: 'tableAlias',
    width: 150,
  },
  {
    title: '表注释',
    field: 'tableComment',
    minWidth: 150,
  },
  {
    title: '数据源',
    field: 'dsName',
    width: 120,
  },
  {
    title: '分层',
    field: 'dataLayer',
    width: 80,
    slots: { default: 'dataLayer' },
  },
  {
    title: '数据域',
    field: 'dataDomain',
    width: 100,
  },
  {
    title: '行数',
    field: 'rowCount',
    width: 100,
    sortable: true,
    formatter: ({ cellValue }) => cellValue ? cellValue.toLocaleString() : '-',
  },
  {
    title: '更新时间',
    field: 'sourceUpdateTime',
    width: 160,
    sortable: true,
    formatter: ({ cellValue }) => cellValue ? cellValue.substring(0, 19) : '-',
  },
  {
    title: '敏感等级',
    field: 'sensitivityLevel',
    width: 100,
    slots: { default: 'sensitivityLevel' },
  },
  {
    title: '状态',
    field: 'status',
    width: 80,
    slots: { default: 'status' },
  },
  {
    title: '最后扫描',
    field: 'lastScanTime',
    width: 160,
    formatter: ({ cellValue }) => cellValue ? cellValue.substring(0, 19) : '-',
  },
  {
    title: '操作',
    field: 'action',
    width: 120,
    fixed: 'right',
    slots: { default: 'action' },
  },
];
