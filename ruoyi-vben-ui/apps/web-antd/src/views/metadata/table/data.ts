import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const layerColorMap: Record<string, string> = {
  ODS: '#909399',
  DWD: '#409EFF',
  DWS: '#67C23A',
  ADS: '#E6A23C',
};

export const sensitivityColorMap: Record<string, string> = {
  NORMAL: 'green',
  INNER: 'blue',
  SENSITIVE: 'orange',
  HIGHLY_SENSITIVE: 'red',
};

export const sensitivityLabelMap: Record<string, string> = {
  NORMAL: '普通',
  INNER: '内部',
  SENSITIVE: '敏感',
  HIGHLY_SENSITIVE: '高度敏感',
};

export const layerOptions = [
  { label: 'ODS（操作数据层）', value: 'ODS' },
  { label: 'DWD（明细数据层）', value: 'DWD' },
  { label: 'DWS（汇总数据层）', value: 'DWS' },
  { label: 'ADS（应用数据层）', value: 'ADS' },
];

export const querySchema: FormSchemaGetter = () => [
  {
    component: 'Input',
    fieldName: 'keyword',
    label: '关键字',
    componentProps: {
      placeholder: '表名/别名/注释',
      allowClear: true,
    },
  },
  {
    component: 'Select',
    fieldName: 'dsId',
    label: '数据源',
    componentProps: {
      options: [],
      placeholder: '请选择数据源',
      allowClear: true,
    },
  },
  {
    component: 'Select',
    fieldName: 'dataLayer',
    label: '数仓分层',
    componentProps: {
      options: layerOptions,
      placeholder: '请选择分层',
      allowClear: true,
    },
  },
  {
    component: 'Input',
    fieldName: 'dataDomain',
    label: '数据域',
    componentProps: {
      placeholder: '请输入数据域',
      allowClear: true,
    },
  },
  {
    component: 'Select',
    fieldName: 'status',
    label: '状态',
    componentProps: {
      options: [
        { label: '活跃', value: 'ACTIVE' },
        { label: '归档', value: 'ARCHIVED' },
        { label: '废弃', value: 'DEPRECATED' },
      ],
      placeholder: '请选择状态',
      allowClear: true,
    },
  },
];

export const columns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 50, fixed: 'left' },
  { title: '表名', field: 'tableName', minWidth: 180, fixed: 'left' },
  {
    title: '表别名',
    field: 'tableAlias',
    minWidth: 150,
    slots: { default: 'tableAlias' },
  },
  { title: '注释', field: 'tableComment', minWidth: 150, showOverflow: true },
  {
    title: '分层',
    field: 'dataLayer',
    width: 100,
    slots: { default: 'dataLayer' },
  },
  { title: '数据域', field: 'dataDomain', width: 120 },
  {
    title: '敏感等级',
    field: 'sensitivityLevel',
    width: 110,
    slots: { default: 'sensitivityLevel' },
  },
  {
    title: '行数',
    field: 'rowCount',
    width: 100,
    formatter: ({ cellValue }) => {
      return cellValue ? Number(cellValue).toLocaleString() : '-';
    },
  },
  {
    title: '最后扫描',
    field: 'lastScanTime',
    width: 160,
    formatter: ({ cellValue }) => {
      return cellValue
        ? String(cellValue).substring(0, 19).replace('T', ' ')
        : '-';
    },
  },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 200,
  },
];
