export interface Datasource {
  /** 数据源ID */
  dsId?: number;
  /** 数据源名称 */
  dsName?: string;
  /** 数据源编码 */
  dsCode?: string;
  /** 数据源类型：MYSQL / SQLSERVER / ORACLE / TIDB / POSTGRESQL */
  dsType?: string;
  /** 主机地址 */
  host?: string;
  /** 端口 */
  port?: number;
  /** 数据库名 */
  databaseName?: string;
  /** Schema名称 */
  schemaName?: string;
  /** 用户名 */
  username?: string;
  /** 密码 */
  password?: string;
  /** 额外连接参数JSON */
  connectionParams?: string;
  /** 数仓层标记 */
  dataLayer?: string;
  /** 部门ID */
  deptId?: number;
  /** 部门名称 */
  deptName?: string;
  /** 状态（0正常 1停用） */
  status?: string;
  /** 备注 */
  remark?: string;
  /** 创建时间 */
  createTime?: string;
  /** 更新时间 */
  updateTime?: string;
  /** 创建者 */
  createBy?: string;
  /** 更新者 */
  updateBy?: string;
}

export interface ConnectionTestResult {
  /** 是否连接成功 */
  success?: boolean;
  /** 连接消息 */
  message?: string;
  /** 数据库版本 */
  databaseVersion?: string;
  /** 耗时（毫秒） */
  elapsedMs?: number;
  /** 测试时间 */
  testTime?: string;
}
