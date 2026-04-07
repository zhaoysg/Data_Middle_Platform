import { describe, expect, it } from 'vitest';

import { applyMask, maskTypeColor, previewPlaceholder, sampleInputForType } from './mask-preview';

describe('mask-preview（与 bgdata 试算逻辑对齐）', () => {
  it('MASK CENTER 保留头尾', () => {
    expect(
      applyMask('13812345678', 'MASK', undefined, '*', 3, 4, 'CENTER'),
    ).toBe('138****5678');
  });

  it('DELETE 返回占位', () => {
    expect(applyMask('secret', 'DELETE')).toBe('[已删除]');
  });

  it('NONE 原样返回', () => {
    expect(applyMask('abc', 'NONE')).toBe('abc');
  });

  it('maskTypeColor 返回已知色', () => {
    expect(maskTypeColor('MASK')).toBe('#FAAD14');
  });

  it('previewPlaceholder 与 sampleInputForType', () => {
    expect(previewPlaceholder('MASK')).toContain('138');
    expect(sampleInputForType('MASK')).toBe('13812345678');
  });
});
