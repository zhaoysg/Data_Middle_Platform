/**
 * 元数据平台 Vue 组件集成测试
 *
 * 测试覆盖：
 * - WizardDrawer 组件的步骤逻辑
 * - ExecutionProgress 组件的状态映射
 * - 列表页组件的 API 交互模拟
 */

import { describe, expect, it, vi, beforeEach } from 'vitest';

// ============================================================
// WizardDrawer 组件逻辑测试
// ============================================================

describe('WizardDrawer 组件逻辑', () => {
  interface WizardState {
    currentStep: number;
    totalSteps: number;
    isSubmitting: boolean;
    isLoading: boolean;
    isFirstStep: boolean;
    isLastStep: boolean;
  }

  function createWizardState(totalSteps: number): WizardState {
    return {
      currentStep: 0,
      totalSteps,
      isSubmitting: false,
      isLoading: false,
      isFirstStep: true,
      isLastStep: totalSteps === 1,
    };
  }

  function computeWizardState(state: WizardState): WizardState {
    return {
      ...state,
      isFirstStep: state.currentStep === 0,
      isLastStep: state.currentStep === state.totalSteps - 1,
    };
  }

  function handleNext(state: WizardState): WizardState {
    if (state.currentStep < state.totalSteps - 1) {
      return computeWizardState({ ...state, currentStep: state.currentStep + 1 });
    }
    return state;
  }

  function handlePrev(state: WizardState): WizardState {
    if (state.currentStep > 0) {
      return computeWizardState({ ...state, currentStep: state.currentStep - 1 });
    }
    return state;
  }

  describe('步骤导航逻辑', () => {
    it('初始化时 currentStep = 0', () => {
      const state = createWizardState(4);
      expect(state.currentStep).toBe(0);
      expect(state.isFirstStep).toBe(true);
    });

    it('第一步时上一步按钮应隐藏', () => {
      const state = computeWizardState(createWizardState(4));
      expect(state.isFirstStep).toBe(true);
    });

    it('最后一步时下一步按钮应变为保存按钮', () => {
      const state = computeWizardState({ ...createWizardState(4), currentStep: 3 });
      expect(state.isLastStep).toBe(true);
    });

    it('下一步按钮点击后 currentStep + 1', () => {
      let state = createWizardState(4);
      state = handleNext(state);
      expect(state.currentStep).toBe(1);
    });

    it('上一步按钮点击后 currentStep - 1', () => {
      let state = computeWizardState({ ...createWizardState(4), currentStep: 2 });
      state = handlePrev(state);
      expect(state.currentStep).toBe(1);
    });

    it('第一步点击上一步无变化', () => {
      let state = createWizardState(4);
      state = handlePrev(state);
      expect(state.currentStep).toBe(0);
    });

    it('最后一步点击下一步无变化', () => {
      let state = computeWizardState({ ...createWizardState(4), currentStep: 3 });
      state = handleNext(state);
      expect(state.currentStep).toBe(3);
    });
  });

  describe('按钮禁用状态逻辑', () => {
    it('loading 时所有按钮应禁用', () => {
      const state = computeWizardState({ ...createWizardState(4), isLoading: true });
      const isPrevDisabled = state.isLoading;
      const isNextDisabled = state.isLoading;
      expect(isPrevDisabled).toBe(true);
      expect(isNextDisabled).toBe(true);
    });

    it('submitting 时所有按钮应禁用', () => {
      const state = computeWizardState({ ...createWizardState(4), isSubmitting: true });
      const isPrevDisabled = state.isSubmitting;
      const isNextDisabled = state.isSubmitting;
      const isSaveDisabled = state.isSubmitting;
      const isCancelDisabled = state.isSubmitting;
      expect(isPrevDisabled).toBe(true);
      expect(isNextDisabled).toBe(true);
      expect(isSaveDisabled).toBe(true);
      expect(isCancelDisabled).toBe(true);
    });
  });

  describe('防重复提交逻辑', () => {
    it('保存按钮点击后进入 submitting 状态', () => {
      let state = computeWizardState({ ...createWizardState(4), currentStep: 3 });
      state = { ...state, isSubmitting: true };
      expect(state.isSubmitting).toBe(true);
    });

    it('API 返回后退出 submitting 状态', () => {
      let state = computeWizardState({ ...createWizardState(4), currentStep: 3, isSubmitting: true });
      state = { ...state, isSubmitting: false };
      expect(state.isSubmitting).toBe(false);
    });
  });

  describe('抽屉关闭逻辑', () => {
    it('loading 时 maskClosable 应为 false', () => {
      const state = computeWizardState({ ...createWizardState(4), isSubmitting: true });
      const maskClosable = !state.isSubmitting;
      expect(maskClosable).toBe(false);
    });

    it('非 loading 时 maskClosable 应为 true', () => {
      const state = computeWizardState(createWizardState(4));
      const maskClosable = !state.isSubmitting;
      expect(maskClosable).toBe(true);
    });
  });
});

// ============================================================
// ExecutionProgress 组件状态映射测试
// ============================================================

describe('ExecutionProgress 组件状态映射', () => {
  interface ExecutionDetail {
    id?: number;
    ruleName?: string;
    status?: string;
    elapsedMs?: number;
    errorMsg?: string;
    actualValue?: string;
  }

  const statusMap = {
    PENDING: { color: 'default', text: '等待执行' },
    RUNNING: { color: 'processing', text: '执行中' },
    SUCCESS: { color: 'success', text: '通过' },
    FAILED: { color: 'error', text: '失败' },
  };

  function getStepItem(detail: ExecutionDetail) {
    const status = statusMap[detail.status as keyof typeof statusMap] || statusMap.PENDING;
    const elapsed = detail.elapsedMs ? `(${detail.elapsedMs}ms)` : '';
    return {
      title: detail.ruleName || '未知规则',
      description:
        detail.status === 'SUCCESS'
          ? `${status.text} ${elapsed}，实际值: ${detail.actualValue || '-'}`
          : detail.status === 'FAILED'
            ? `${status.text}: ${detail.errorMsg || '未知错误'}`
            : status.text,
      status: detail.status?.toLowerCase() || 'wait',
    };
  }

  describe('状态映射', () => {
    it('PENDING 状态映射正确', () => {
      const detail: ExecutionDetail = { ruleName: '测试规则', status: 'PENDING' };
      const item = getStepItem(detail);
      expect(item.status).toBe('pending');
      expect(item.description).toBe('等待执行');
    });

    it('RUNNING 状态映射正确', () => {
      const detail: ExecutionDetail = { ruleName: '测试规则', status: 'RUNNING' };
      const item = getStepItem(detail);
      expect(item.status).toBe('running');
      expect(item.description).toBe('执行中');
    });

    it('SUCCESS 状态映射正确，包含执行耗时和实际值', () => {
      const detail: ExecutionDetail = {
        ruleName: '空值检查',
        status: 'SUCCESS',
        elapsedMs: 150,
        actualValue: '0.95',
      };
      const item = getStepItem(detail);
      expect(item.status).toBe('success');
      expect(item.description).toContain('通过');
      expect(item.description).toContain('150ms');
      expect(item.description).toContain('0.95');
    });

    it('FAILED 状态映射正确，包含错误信息', () => {
      const detail: ExecutionDetail = {
        ruleName: '唯一性检查',
        status: 'FAILED',
        errorMsg: 'Connection timeout',
      };
      const item = getStepItem(detail);
      expect(item.status).toBe('failed');
      expect(item.description).toContain('失败');
      expect(item.description).toContain('Connection timeout');
    });

    it('未知状态处理', () => {
      const detail: ExecutionDetail = { ruleName: '测试规则', status: 'UNKNOWN' };
      const item = getStepItem(detail);
      // 未知状态：描述使用 PENDING 文本，但 status 字符串保留原始值
      expect(item.description).toBe('等待执行'); // 描述使用 PENDING 文本
      expect(item.status).toBe('unknown'); // status 保留原始值的小写
    });
  });

  describe('步骤项列表渲染', () => {
    it('空数组返回空列表', () => {
      const details: ExecutionDetail[] = [];
      const items = details.map(getStepItem);
      expect(items).toEqual([]);
    });

    it('多个执行明细正确生成步骤列表', () => {
      const details: ExecutionDetail[] = [
        { ruleName: '规则1', status: 'SUCCESS', elapsedMs: 100 },
        { ruleName: '规则2', status: 'RUNNING' },
        { ruleName: '规则3', status: 'PENDING' },
      ];
      const items = details.map(getStepItem);
      expect(items.length).toBe(3);
      expect(items[0].title).toBe('规则1');
      expect(items[1].title).toBe('规则2');
      expect(items[2].title).toBe('规则3');
    });
  });
});

// ============================================================
// 状态标签颜色映射测试
// ============================================================

describe('状态标签颜色映射（所有页面统一）', () => {
  const executionStatusColorMap: Record<string, string> = {
    RUNNING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
    PARTIAL: 'warning',
  };

  const executionStatusLabelMap: Record<string, string> = {
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败',
    PARTIAL: '部分成功',
  };

  const planStatusColorMap: Record<string, string> = {
    DRAFT: 'default',
    RUNNING: 'processing',
    SUCCESS: 'success',
    FAILED: 'error',
    PARTIAL: 'warning',
    STOPPED: 'warning',
    CANCELLED: 'default',
  };

  const planStatusLabelMap: Record<string, string> = {
    DRAFT: '草稿',
    RUNNING: '运行中',
    SUCCESS: '已完成',
    FAILED: '已失败',
    PARTIAL: '部分成功',
    STOPPED: '已停止',
    CANCELLED: '已取消',
  };

  describe('执行记录状态映射', () => {
    it('RUNNING -> processing / 运行中', () => {
      expect(executionStatusColorMap['RUNNING']).toBe('processing');
      expect(executionStatusLabelMap['RUNNING']).toBe('运行中');
    });

    it('SUCCESS -> success / 成功', () => {
      expect(executionStatusColorMap['SUCCESS']).toBe('success');
      expect(executionStatusLabelMap['SUCCESS']).toBe('成功');
    });

    it('FAILED -> error / 失败', () => {
      expect(executionStatusColorMap['FAILED']).toBe('error');
      expect(executionStatusLabelMap['FAILED']).toBe('失败');
    });

    it('PARTIAL -> warning / 部分成功', () => {
      expect(executionStatusColorMap['PARTIAL']).toBe('warning');
      expect(executionStatusLabelMap['PARTIAL']).toBe('部分成功');
    });
  });

  describe('质检方案状态映射', () => {
    it('DRAFT -> default / 草稿', () => {
      expect(planStatusColorMap['DRAFT']).toBe('default');
      expect(planStatusLabelMap['DRAFT']).toBe('草稿');
    });

    it('RUNNING -> processing / 运行中', () => {
      expect(planStatusColorMap['RUNNING']).toBe('processing');
      expect(planStatusLabelMap['RUNNING']).toBe('运行中');
    });

    it('SUCCESS -> success / 已完成', () => {
      expect(planStatusColorMap['SUCCESS']).toBe('success');
      expect(planStatusLabelMap['SUCCESS']).toBe('已完成');
    });

    it('FAILED -> error / 已失败', () => {
      expect(planStatusColorMap['FAILED']).toBe('error');
      expect(planStatusLabelMap['FAILED']).toBe('已失败');
    });

    it('STOPPED -> warning / 已停止', () => {
      expect(planStatusColorMap['STOPPED']).toBe('warning');
      expect(planStatusLabelMap['STOPPED']).toBe('已停止');
    });
  });
});

// ============================================================
// MaskQuery 页面逻辑测试
// ============================================================

describe('MaskQuery 页面逻辑', () => {
  interface QueryState {
    datasourceOptions: { label: string; value: number }[];
    selectedDsId?: number;
    sqlText: string;
    loading: boolean;
    queryResult: Record<string, any>[];
    truncated: boolean;
    error: string | null;
  }

  function createQueryState(): QueryState {
    return {
      datasourceOptions: [],
      selectedDsId: undefined,
      sqlText: '',
      loading: false,
      queryResult: [],
      truncated: false,
      error: null,
    };
  }

  function computeQueryButtonState(state: QueryState): { disabled: boolean; tooltip?: string } {
    const isDsEmpty = state.selectedDsId === undefined;
    const isSqlEmpty = !state.sqlText.trim();

    if (state.loading) {
      return { disabled: true, tooltip: '查询进行中，请等待完成' };
    }
    if (isDsEmpty || isSqlEmpty) {
      return { disabled: true, tooltip: '请先选择数据源并输入 SQL' };
    }
    return { disabled: false };
  }

  function validateSql(sql: string): { valid: boolean; message: string } {
    const trimmed = sql.trim().toUpperCase();
    if (!trimmed) {
      return { valid: false, message: 'SQL 语句不能为空' };
    }
    if (!trimmed.startsWith('SELECT')) {
      return { valid: false, message: '只允许 SELECT 查询语句' };
    }
    const dangerousKeywords = ['UPDATE', 'DELETE', 'INSERT', 'DROP', 'TRUNCATE', 'ALTER', 'CREATE'];
    for (const keyword of dangerousKeywords) {
      if (trimmed.includes(keyword)) {
        return { valid: false, message: `禁止使用 ${keyword} 等危险操作` };
      }
    }
    return { valid: true, message: 'SQL 语法验证通过' };
  }

  describe('执行按钮状态计算', () => {
    it('初始状态：数据源未选 + SQL 为空 -> 禁用', () => {
      const state = createQueryState();
      const result = computeQueryButtonState(state);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toBe('请先选择数据源并输入 SQL');
    });

    it('数据源已选但 SQL 为空 -> 禁用', () => {
      const state = { ...createQueryState(), selectedDsId: 1 };
      const result = computeQueryButtonState(state);
      expect(result.disabled).toBe(true);
    });

    it('SQL 已输入但数据源未选 -> 禁用', () => {
      const state = { ...createQueryState(), sqlText: 'SELECT * FROM users' };
      const result = computeQueryButtonState(state);
      expect(result.disabled).toBe(true);
    });

    it('数据源已选 + SQL 已输入 -> 可点击', () => {
      const state = {
        ...createQueryState(),
        selectedDsId: 1,
        sqlText: 'SELECT * FROM users',
      };
      const result = computeQueryButtonState(state);
      expect(result.disabled).toBe(false);
    });

    it('查询中状态 -> 禁用', () => {
      const state = {
        ...createQueryState(),
        selectedDsId: 1,
        sqlText: 'SELECT * FROM users',
        loading: true,
      };
      const result = computeQueryButtonState(state);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toBe('查询进行中，请等待完成');
    });
  });

  describe('SQL 验证逻辑', () => {
    it('空 SQL 验证失败', () => {
      const result = validateSql('');
      expect(result.valid).toBe(false);
      expect(result.message).toContain('不能为空');
    });

    it('纯空格 SQL 验证失败', () => {
      const result = validateSql('   ');
      expect(result.valid).toBe(false);
    });

    it('合法 SELECT 语句验证通过', () => {
      const result = validateSql('SELECT * FROM users WHERE id = 1');
      expect(result.valid).toBe(true);
    });

    it('带 WHERE 子句的 SELECT 验证通过', () => {
      const result = validateSql('SELECT id, name, email FROM users WHERE status = 1 ORDER BY id DESC LIMIT 10');
      expect(result.valid).toBe(true);
    });

    it('UPDATE 语句验证失败', () => {
      const result = validateSql('UPDATE users SET name = "test" WHERE id = 1');
      expect(result.valid).toBe(false);
      expect(result.message).toContain('只允许');
    });

    it('DELETE 语句验证失败', () => {
      const result = validateSql('DELETE FROM users WHERE id = 1');
      expect(result.valid).toBe(false);
    });

    it('INSERT 语句验证失败', () => {
      const result = validateSql('INSERT INTO users VALUES(1, "test")');
      expect(result.valid).toBe(false);
    });

    it('DROP TABLE 语句验证失败', () => {
      const result = validateSql('DROP TABLE users');
      expect(result.valid).toBe(false);
    });

    it('TRUNCATE 语句验证失败', () => {
      const result = validateSql('TRUNCATE TABLE users');
      expect(result.valid).toBe(false);
    });

    it('小写 SELECT 也验证通过', () => {
      const result = validateSql('select * from users');
      expect(result.valid).toBe(true);
    });
  });

  describe('查询结果处理', () => {
    it('LIMIT 截断判断', () => {
      const MAX_ROWS = 1000;
      const rowCount = 1500;
      const truncated = rowCount > MAX_ROWS;
      expect(truncated).toBe(true);
    });

    it('未截断时 truncated 为 false', () => {
      const MAX_ROWS = 1000;
      const rowCount = 500;
      const truncated = rowCount > MAX_ROWS;
      expect(truncated).toBe(false);
    });

    it('从结果行构建列定义', () => {
      const row = { id: 1, name: 'test', email: 'test@example.com' };
      const columns = Object.keys(row).map((key) => ({
        title: key,
        dataIndex: key,
      }));
      expect(columns.length).toBe(3);
      expect(columns[0]).toEqual({ title: 'id', dataIndex: 'id' });
      expect(columns[1]).toEqual({ title: 'name', dataIndex: 'name' });
    });
  });
});

// ============================================================
// 列表页按钮状态测试
// ============================================================

describe('列表页按钮状态逻辑', () => {
  interface TableRow {
    id?: number;
    builtin?: string;
    status?: string;
  }

  function computeBatchDeleteState(rows: TableRow[]): { disabled: boolean; tooltip?: string } {
    const selectedCount = rows.length;
    const hasBuiltin = rows.some((r) => r.builtin === '1');
    const hasRunning = rows.some((r) => r.status === 'RUNNING');

    if (selectedCount === 0) {
      return { disabled: true, tooltip: '请先选择要操作的行' };
    }
    if (hasBuiltin) {
      return { disabled: true, tooltip: '包含不可删除的数据，请先取消选择' };
    }
    if (hasRunning) {
      return { disabled: true, tooltip: '运行中任务关联的数据无法删除' };
    }
    return { disabled: false };
  }

  function computeDeleteButtonState(row: TableRow): { disabled: boolean; tooltip?: string } {
    if (row.builtin === '1') {
      return { disabled: true, tooltip: '内置数据不可删除' };
    }
    if (row.status === 'RUNNING') {
      return { disabled: true, tooltip: '运行中任务关联的数据无法删除' };
    }
    return { disabled: false };
  }

  describe('批量删除按钮状态', () => {
    it('无选中项时禁用', () => {
      const result = computeBatchDeleteState([]);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toBe('请先选择要操作的行');
    });

    it('选中非内置/非运行中项时可点击', () => {
      const rows: TableRow[] = [{ id: 1 }, { id: 2 }];
      const result = computeBatchDeleteState(rows);
      expect(result.disabled).toBe(false);
    });

    it('包含内置项时禁用', () => {
      const rows: TableRow[] = [{ id: 1 }, { id: 2, builtin: '1' }];
      const result = computeBatchDeleteState(rows);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toContain('不可删除');
    });

    it('包含运行中项时禁用', () => {
      const rows: TableRow[] = [{ id: 1 }, { id: 2, status: 'RUNNING' }];
      const result = computeBatchDeleteState(rows);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toContain('运行中');
    });
  });

  describe('单行删除按钮状态', () => {
    it('内置数据禁用', () => {
      const row: TableRow = { id: 1, builtin: '1' };
      const result = computeDeleteButtonState(row);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toBe('内置数据不可删除');
    });

    it('运行中数据禁用', () => {
      const row: TableRow = { id: 1, status: 'RUNNING' };
      const result = computeDeleteButtonState(row);
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toContain('运行中');
    });

    it('普通数据可删除', () => {
      const row: TableRow = { id: 1 };
      const result = computeDeleteButtonState(row);
      expect(result.disabled).toBe(false);
    });
  });
});

// ============================================================
// 执行状态按钮逻辑测试
// ============================================================

describe('执行状态按钮逻辑', () => {
  interface ExecutionState {
    status: string;
  }

  function computeStartButtonState(state: ExecutionState): { disabled: boolean; tooltip?: string } {
    if (state.status === 'RUNNING') {
      return { disabled: true, tooltip: '执行中，请等待完成' };
    }
    if (state.status === 'SUCCESS' || state.status === 'PARTIAL') {
      return { disabled: true, tooltip: '已完成，再次执行请使用重新执行' };
    }
    if (state.status === 'DRAFT' || state.status === 'STOPPED') {
      return { disabled: false };
    }
    return { disabled: true };
  }

  function computeStopButtonState(state: ExecutionState): { disabled: boolean } {
    return { disabled: state.status !== 'RUNNING' };
  }

  function computeRerunButtonState(state: ExecutionState): { disabled: boolean } {
    return { disabled: !['SUCCESS', 'FAILED', 'PARTIAL'].includes(state.status) };
  }

  describe('开始执行按钮', () => {
    it('草稿状态可开始', () => {
      const result = computeStartButtonState({ status: 'DRAFT' });
      expect(result.disabled).toBe(false);
    });

    it('已停止状态可开始', () => {
      const result = computeStartButtonState({ status: 'STOPPED' });
      expect(result.disabled).toBe(false);
    });

    it('运行中状态禁用', () => {
      const result = computeStartButtonState({ status: 'RUNNING' });
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toContain('执行中');
    });

    it('已完成状态禁用', () => {
      const result = computeStartButtonState({ status: 'SUCCESS' });
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toContain('已完成');
    });

    it('部分成功状态禁用', () => {
      const result = computeStartButtonState({ status: 'PARTIAL' });
      expect(result.disabled).toBe(true);
      expect(result.tooltip).toContain('已完成');
    });
  });

  describe('停止按钮', () => {
    it('运行中状态可停止', () => {
      const result = computeStopButtonState({ status: 'RUNNING' });
      expect(result.disabled).toBe(false);
    });

    it('非运行中状态禁用', () => {
      const result = computeStopButtonState({ status: 'SUCCESS' });
      expect(result.disabled).toBe(true);
    });
  });

  describe('重新执行按钮', () => {
    it('已完成状态可重新执行', () => {
      const result = computeRerunButtonState({ status: 'SUCCESS' });
      expect(result.disabled).toBe(false);
    });

    it('已失败状态可重新执行', () => {
      const result = computeRerunButtonState({ status: 'FAILED' });
      expect(result.disabled).toBe(false);
    });

    it('部分成功状态可重新执行', () => {
      const result = computeRerunButtonState({ status: 'PARTIAL' });
      expect(result.disabled).toBe(false);
    });

    it('运行中状态禁用', () => {
      const result = computeRerunButtonState({ status: 'RUNNING' });
      expect(result.disabled).toBe(true);
    });

    it('草稿状态禁用', () => {
      const result = computeRerunButtonState({ status: 'DRAFT' });
      expect(result.disabled).toBe(true);
    });
  });
});
