/**
 * 前端脱敏预览（与 bgdata 脱敏规则管理页逻辑对齐，仅供配置页试算，非服务端真实脱敏）
 */
export function applyMask(
  input: string,
  templateType?: string,
  _pattern?: string,
  maskChar?: string,
  headKeep?: number,
  tailKeep?: number,
  position?: string,
): string {
  if (!input) {
    return '';
  }

  const char = maskChar || '*';

  switch (templateType) {
    case 'NONE': {
      return input;
    }
    case 'MASK':
    case 'HIDE': {
      if (position === 'ALL') {
        return char.repeat(input.length);
      }
      if (position === 'HEAD') {
        const h = headKeep ?? 0;
        return input.slice(0, h) + char.repeat(Math.max(0, input.length - h));
      }
      if (position === 'TAIL') {
        const t = tailKeep ?? 0;
        return char.repeat(Math.max(0, input.length - t)) + input.slice(-t);
      }
      const h = headKeep ?? 3;
      const t = tailKeep ?? 4;
      if (input.length <= h + t) {
        return char.repeat(input.length);
      }
      return (
        input.slice(0, h) + char.repeat(input.length - h - t) + input.slice(-t)
      );
    }
    case 'DELETE': {
      return '[已删除]';
    }
    case 'ENCRYPT': {
      return input
        .split('')
        .map((c) =>
          (c.charCodeAt(0) ^ 42).toString(16).padStart(2, '0'),
        )
        .join('');
    }
    case 'HASH': {
      let hash = 0;
      for (let i = 0; i < input.length; i++) {
        hash = (hash << 5) - hash + input.charCodeAt(i);
        hash |= 0;
      }
      return Math.abs(hash).toString(16).toUpperCase().padStart(8, '0');
    }
    case 'FORMAT_KEEP': {
      return input
        .split('')
        .map((c) => {
          if (/[a-z]/i.test(c)) {
            return String.fromCharCode(65 + Math.floor(Math.random() * 26));
          }
          if (/\d/.test(c)) {
            return String(Math.floor(Math.random() * 10));
          }
          return c;
        })
        .join('');
    }
    case 'RANGE': {
      const num = Number.parseFloat(input);
      if (Number.isNaN(num)) {
        return input;
      }
      if (num < 1000) {
        return '< 1,000';
      }
      if (num < 10_000) {
        return '1,000 - 10,000';
      }
      if (num < 100_000) {
        return '10,000 - 100,000';
      }
      if (num < 1_000_000) {
        return '100,000 - 1,000,000';
      }
      return '> 1,000,000';
    }
    case 'WATERMARK': {
      return `${input}【水印】`;
    }
    case 'SHUFFLE':
    case 'CUSTOM': {
      return `[${templateType}] ${input}`;
    }
    default: {
      return input;
    }
  }
}

export function maskTypeColor(templateType?: string): string {
  const colorMap: Record<string, string> = {
    NONE: '#8C8C8C',
    MASK: '#FAAD14',
    HIDE: '#722ED1',
    ENCRYPT: '#F5222D',
    DELETE: '#F5222D',
    HASH: '#13C2C2',
    FORMAT_KEEP: '#1677FF',
    RANGE: '#FA8C16',
    WATERMARK: '#722ED1',
    SHUFFLE: '#2F54EB',
    CUSTOM: '#595959',
  };
  return colorMap[templateType || ''] || 'default';
}

const SAMPLE_BY_TYPE: Record<string, string> = {
  NONE: '13812345678',
  MASK: '13812345678',
  HIDE: '13812345678',
  ENCRYPT: 'HelloWorld',
  DELETE: '13812345678',
  HASH: '110101199003074515',
  FORMAT_KEEP: '13812345678',
  RANGE: '50000',
  WATERMARK: '13812345678',
  SHUFFLE: 'abc123',
  CUSTOM: 'sample',
};

export function previewPlaceholder(templateType?: string): string {
  const samples: Record<string, string> = {
    NONE: '任意文本，示例: Hello',
    MASK: '手机号，示例: 13812345678',
    HIDE: '邮箱，示例: user@example.com',
    ENCRYPT: '任意文本，示例: HelloWorld',
    DELETE: '任意内容，示例: 敏感数据',
    HASH: '身份证，示例: 110101199003074515',
    FORMAT_KEEP: '手机号，示例: 13812345678',
    RANGE: '金额，示例: 50000',
    SHUFFLE: '任意字符串',
    CUSTOM: '自定义规则试算',
  };
  return samples[templateType || ''] || '请输入测试数据';
}

export function sampleInputForType(templateType?: string): string {
  if (!templateType) {
    return '13812345678';
  }
  return SAMPLE_BY_TYPE[templateType] ?? '13812345678';
}
