/**
 * 与后端 SecColumnSensitivityServiceImpl#matchRule 保持一致的规则表达式解析与本地测试。
 * 扩展字段（priority、description、suggestionMaskType）存放在 rule_expr JSON 中，扫描逻辑忽略未知键。
 */
export interface RuleExprPayload {
  pattern?: string;
  columnNames?: string[];
  dataTypes?: string[];
  priority?: number;
  description?: string;
  suggestionMaskType?: string;
  raw?: string;
}

export function parseRuleExpr(raw: string | undefined | null): RuleExprPayload {
  if (raw == null || !String(raw).trim()) {
    return {};
  }
  try {
    return JSON.parse(String(raw)) as RuleExprPayload;
  } catch {
    return {};
  }
}

/** 列表/抽屉展示的匹配表达式文案 */
export function displayMatchExpr(ruleType: string | undefined, raw: string | undefined): string {
  const p = parseRuleExpr(raw);
  if (ruleType === 'COLUMN_NAME' && p.columnNames?.length) {
    return p.columnNames.join(', ');
  }
  if (ruleType === 'REGEX' && p.pattern) {
    return p.pattern;
  }
  if (ruleType === 'DATA_TYPE' && p.dataTypes?.length) {
    return p.dataTypes.join(', ');
  }
  if (p.raw) {
    return p.raw;
  }
  return raw?.trim() ? raw : '';
}

export function getPriorityFromExpr(raw: string | undefined): number {
  const p = parseRuleExpr(raw);
  const n = p.priority;
  return typeof n === 'number' && !Number.isNaN(n) ? n : 100;
}

export function splitKeywords(input: string): string[] {
  return input
    .split(/[,，\n]/g)
    .map((s) => s.trim())
    .filter(Boolean);
}

export function buildRuleExpr(
  ruleType: string | undefined,
  matchText: string,
  extra: {
    priority?: number;
    description?: string;
    suggestionMaskType?: string;
  },
): string {
  const priority = extra.priority ?? 100;
  const base: RuleExprPayload = {
    priority,
    description: extra.description?.trim() || undefined,
    suggestionMaskType: extra.suggestionMaskType || undefined,
  };

  if (ruleType === 'COLUMN_NAME') {
    return JSON.stringify({
      ...base,
      columnNames: splitKeywords(matchText),
    });
  }
  if (ruleType === 'REGEX') {
    return JSON.stringify({
      ...base,
      pattern: matchText.trim(),
    });
  }
  if (ruleType === 'DATA_TYPE') {
    return JSON.stringify({
      ...base,
      dataTypes: splitKeywords(matchText),
    });
  }
  return JSON.stringify({
    ...base,
    raw: matchText,
  });
}

/** 模拟后端匹配（列名小写、正则全匹配） */
export function matchRuleLocal(
  ruleType: string | undefined,
  ruleExpr: string | undefined,
  testColumnName: string,
  testDataType: string,
): boolean {
  const expr = ruleExpr?.trim();
  if (!expr || !ruleType) {
    return false;
  }

  const colName = testColumnName.trim().toLowerCase();
  const dataType = testDataType.trim();

  try {
    if (ruleType === 'COLUMN_NAME') {
      const json = parseRuleExpr(expr);
      const columnNames = json.columnNames;
      if (!columnNames?.length) {
        return false;
      }
      for (const name of columnNames) {
        const pattern = String(name).toLowerCase();
        if (colName.includes(pattern) || pattern.includes(colName)) {
          return true;
        }
      }
      return false;
    }
    if (ruleType === 'REGEX') {
      const json = parseRuleExpr(expr);
      const pattern = json.pattern;
      if (!pattern?.trim()) {
        return false;
      }
      const re = new RegExp(pattern);
      return re.test(colName);
    }
    if (ruleType === 'DATA_TYPE') {
      const json = parseRuleExpr(expr);
      const dataTypes = json.dataTypes;
      if (!dataTypes?.length || !dataType) {
        return false;
      }
      const lowerDt = dataType.toLowerCase();
      for (const type of dataTypes) {
        if (lowerDt.includes(String(type).toLowerCase())) {
          return true;
        }
      }
      return false;
    }
  } catch {
    return false;
  }
  return false;
}

export const RULE_TYPE_LABEL: Record<string, string> = {
  REGEX: '正则匹配（列名）',
  COLUMN_NAME: '列名匹配',
  DATA_TYPE: '数据类型',
  CUSTOM: '自定义',
};
