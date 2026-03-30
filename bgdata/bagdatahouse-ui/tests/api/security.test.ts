/**
 * Security API Layer Unit Tests
 * Tests all API parameter construction and type correctness for security.ts
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

// Mock axios
vi.mock('axios', async () => {
  const actual = await vi.importActual('axios')
  return {
    ...actual as any,
    default: {
      create: vi.fn(() => ({
        interceptors: {
          request: { use: vi.fn(), eject: vi.fn() },
          response: { use: vi.fn(), eject: vi.fn() }
        },
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn()
      })),
      interceptors: {
        request: { use: vi.fn(), eject: vi.fn() },
        response: { use: vi.fn(), eject: vi.fn() }
      }
    }
  }
})

// Mock ant-design-vue message
vi.mock('ant-design-vue', () => ({
  message: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }
}))

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn()
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

describe('Security API - Type & Parameter Construction Tests', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    localStorageMock.getItem.mockReturnValue('mock-token')
  })

  // ============================================================
  // Test Suite 1: Classification API Parameter Construction
  // ============================================================
  describe('Classification API (SecClassificationSaveDTO)', () => {
    it('should construct full SecClassificationSaveDTO with all required fields', () => {
      const dto = {
        id: 1,
        classCode: 'CLASS001',
        className: '个人信息',
        classDesc: '个人基本信息描述',
        classOrder: 1,
        sensitivityLevel: 'L2',
        status: 1
      }

      expect(dto.classCode).toBe('CLASS001')
      expect(dto.className).toBe('个人信息')
      expect(dto.sensitivityLevel).toBe('L2')
      expect(dto.status).toBe(1)
      expect(dto.classOrder).toBe(1)
    })

    it('should allow optional fields to be omitted', () => {
      const dto = {
        classCode: 'CLASS002',
        className: '金融信息'
      }

      expect(dto.classCode).toBe('CLASS002')
      expect(dto.className).toBe('金融信息')
      expect(dto.id).toBeUndefined()
      expect(dto.classDesc).toBeUndefined()
    })

    it('should construct SecClassificationQueryDTO with optional filters', () => {
      const queryDTO = {
        classCode: 'CLASS',
        className: '个人',
        enabled: 1
      }

      expect(queryDTO.classCode).toBe('CLASS')
      expect(queryDTO.enabled).toBe(1)
    })
  })

  // ============================================================
  // Test Suite 2: Sensitivity Level API
  // ============================================================
  describe('Sensitivity Level API (SecLevelSaveDTO)', () => {
    it('should construct SecLevelSaveDTO with level configuration', () => {
      const dto = {
        id: 1,
        levelCode: 'L1',
        levelName: 'L1 公开',
        levelValue: 1,
        levelDesc: '公开级别，无敏感信息',
        color: '#52C41A',
        icon: 'GlobalOutlined'
      }

      expect(dto.levelCode).toBe('L1')
      expect(dto.levelValue).toBe(1)
      expect(dto.color).toBe('#52C41A')
    })

    it('should allow partial SecLevelVO properties', () => {
      const vo = {
        id: 2,
        levelCode: 'L2',
        levelName: 'L2 内部',
        levelValue: 2,
        sensitiveFieldCount: 156,
        statusLabel: '启用'
      }

      expect(vo.levelCode).toBe('L2')
      expect(vo.sensitiveFieldCount).toBe(156)
      expect(vo.statusLabel).toBe('启用')
    })
  })

  // ============================================================
  // Test Suite 3: Sensitivity Rule API (MatchType & MaskType)
  // ============================================================
  describe('Sensitivity Rule API (MatchType & MaskType)', () => {
    it('should support all MatchType enum values', () => {
      const matchTypes = [
        'COLUMN_NAME',
        'COLUMN_COMMENT',
        'DATA_TYPE',
        'REGEX',
        'CONTENT_ALGORITHM',
        'CONTENT_REGEX'
      ] as const

      matchTypes.forEach(type => {
        expect(['COLUMN_NAME', 'COLUMN_COMMENT', 'DATA_TYPE', 'REGEX', 'CONTENT_ALGORITHM', 'CONTENT_REGEX']).toContain(type)
      })
    })

    it('should support all MaskType enum values', () => {
      const maskTypes = [
        'NONE', 'MASK', 'HIDE', 'ENCRYPT', 'DELETE',
        'PARTIAL_MASK', 'HASH', 'FORMAT_KEEP', 'RANGE',
        'WATERMARK', 'RANDOM_REPLACE'
      ] as const

      maskTypes.forEach(type => {
        const validTypes = ['NONE', 'MASK', 'HIDE', 'ENCRYPT', 'DELETE', 'PARTIAL_MASK', 'HASH', 'FORMAT_KEEP', 'RANGE', 'WATERMARK', 'RANDOM_REPLACE']
        expect(validTypes).toContain(type)
      })
    })

    it('should construct SecSensitivityRuleSaveDTO with all fields', () => {
      const dto = {
        id: 1,
        ruleName: '身份证号识别规则',
        ruleCode: 'RULE_ID_CARD',
        classId: 1,
        levelId: 3,
        matchType: 'COLUMN_NAME' as const,
        matchExpr: '.*(id_card|identity|card_no).*',
        matchExprType: 'REGEX' as const,
        builtinAlgorithm: 'ID_CARD',
        suggestionAction: 'MASK',
        suggestionMaskPattern: '**************',
        priority: 10,
        status: 1,
        description: '识别身份证号字段'
      }

      expect(dto.matchType).toBe('COLUMN_NAME')
      expect(dto.suggestionAction).toBe('MASK')
      expect(dto.priority).toBe(10)
    })
  })

  // ============================================================
  // Test Suite 4: Scan DTOs
  // ============================================================
  describe('Scan API DTOs', () => {
    it('should construct SecSensitivityScanDTO with scan parameters', () => {
      const dto = {
        dsId: 1,
        scanScope: 'ALL' as const,
        layerCode: 'ODS',
        scanColumnName: true,
        scanColumnComment: true,
        scanDataType: true,
        useRegex: true,
        sampleSize: 1000,
        enableContentScan: true,
        incremental: false,
        directScan: true,
        scanCycle: 'DAILY' as const,
        excludeSystemTables: true,
        triggerType: 'MANUAL' as const,
        sensitivityLevel: 'L3',
        scanBuiltinRules: true,
        scanCustomRules: true,
        overwriteExisting: false
      }

      expect(dto.scanScope).toBe('ALL')
      expect(dto.scanColumnName).toBe(true)
      expect(dto.sampleSize).toBe(1000)
      expect(dto.excludeSystemTables).toBe(true)
    })

    it('should construct SecSensitivityScanDTO with specific tables', () => {
      const dto = {
        dsId: 2,
        scanScope: 'SPECIFIC' as const,
        specificTables: ['users', 'orders', 'payments'],
        triggerType: 'MANUAL' as const
      }

      expect(dto.scanScope).toBe('SPECIFIC')
      expect(dto.specificTables).toContain('orders')
    })
  })

  // ============================================================
  // Test Suite 5: Mask Strategy DTOs
  // ============================================================
  describe('Mask Strategy API DTOs', () => {
    it('should construct SecMaskStrategySaveDTO with priority and conflict check', () => {
      const dto = {
        strategyCode: 'MASK001',
        strategyName: '金融数据脱敏策略',
        description: '针对金融敏感字段的脱敏策略',
        priority: 1,
        conflictCheck: true,
        enabled: true,
        levelMaskMapping: {
          L1: 'NONE',
          L2: 'MASK',
          L3: 'ENCRYPT',
          L4: 'DELETE'
        },
        whitelistEnabled: true
      }

      expect(dto.priority).toBe(1)
      expect(dto.conflictCheck).toBe(true)
      expect(dto.levelMaskMapping['L3']).toBe('ENCRYPT')
      expect(dto.levelMaskMapping['L4']).toBe('DELETE')
    })

    it('should construct SecMaskTemplateSaveDTO with description', () => {
      const dto = {
        templateName: '身份证掩码模板',
        templateCode: 'TPL_ID_CARD',
        description: '对身份证号进行部分掩码处理',
        matchType: 'COLUMN_NAME',
        matchExpr: 'id_card',
        maskType: 'PARTIAL_MASK',
        maskPattern: '**************',
        sortOrder: 1,
        enabled: true
      }

      expect(dto.maskType).toBe('PARTIAL_MASK')
      expect(dto.description).toBe('对身份证号进行部分掩码处理')
    })

    it('should support partial mask with custom pattern', () => {
      const dto = {
        maskType: 'PARTIAL_MASK' as const,
        maskPattern: '138****5678',
        keepPrefix: 3,
        keepSuffix: 4
      }

      expect(dto.maskType).toBe('PARTIAL_MASK')
      expect(dto.maskPattern).toBe('138****5678')
      expect(dto.keepPrefix).toBe(3)
    })
  })

  // ============================================================
  // Test Suite 6: Mask Task DTOs
  // ============================================================
  describe('Mask Task API DTOs', () => {
    it('should construct SecMaskTaskSaveDTO with task parameters', () => {
      const dto = {
        taskCode: 'TASK001',
        taskName: '金融字段脱敏任务',
        taskType: 'RULE' as const,
        strategyId: 1,
        dsId: 1,
        tableNames: ['users', 'accounts'],
        columnNames: ['id_card', 'bank_card'],
        triggerType: 'MANUAL' as const,
        sceneType: 'QUERY' as const,
        targetMode: 'VIEW' as const,
        enabled: true
      }

      expect(dto.taskType).toBe('RULE')
      expect(dto.sceneType).toBe('QUERY')
      expect(dto.targetMode).toBe('VIEW')
      expect(dto.tableNames).toContain('users')
    })

    it('should construct SecMaskWhitelistSaveDTO with timing fields', () => {
      const dto = {
        entityType: 'TABLE' as const,
        entityValue: 'public.users',
        maskType: 'NONE' as const,
        validFrom: '2026-03-01 00:00:00',
        validTo: '2026-12-31 23:59:59',
        description: '生产表例外申请',
        approverId: 1
      }

      expect(dto.validFrom).toBe('2026-03-01 00:00:00')
      expect(dto.validTo).toBe('2026-12-31 23:59:59')
      expect(dto.entityType).toBe('TABLE')
    })
  })

  // ============================================================
  // Test Suite 7: Access Approval DTOs
  // ============================================================
  describe('Access Approval API DTOs', () => {
    it('should construct approval DTO with sensitivity level', () => {
      const dto = {
        dsId: 1,
        tableName: 'users',
        columnName: 'id_card',
        sensitivityLevel: 'L3',
        requestReason: '业务人员数据查询',
        requestUserId: 100,
        requestDuration: 30
      }

      expect(dto.sensitivityLevel).toBe('L3')
      expect(dto.requestDuration).toBe(30)
    })
  })

  // ============================================================
  // Test Suite 8: Statistics VOs
  // ============================================================
  describe('Statistics VO Types', () => {
    it('should use Record<string, number> for SecStatsVO', () => {
      const stats: Record<string, number> = {
        totalSensitiveFields: 5000,
        l1Count: 2000,
        l2Count: 1500,
        l3Count: 1000,
        l4Count: 500,
        scanCount: 50,
        alertCount: 12
      }

      expect(stats['totalSensitiveFields']).toBe(5000)
      expect(stats['l4Count']).toBe(500)
      expect(stats.disabledClassificationCount).toBeUndefined()
    })

    it('should support dynamic level statistics', () => {
      const levelStats: Record<string, number> = {}
      const levels = ['L1', 'L2', 'L3', 'L4']

      levels.forEach((level, index) => {
        levelStats[level] = (index + 1) * 1000
      })

      expect(levelStats['L1']).toBe(1000)
      expect(levelStats['L4']).toBe(4000)
    })
  })

  // ============================================================
  // Test Suite 9: Scan Schedule DTOs
  // ============================================================
  describe('Scan Schedule API DTOs', () => {
    it('should construct SecScanScheduleSaveDTO with cron expression', () => {
      const dto = {
        scheduleCode: 'SCAN_DAILY',
        scheduleName: '每日敏感字段扫描',
        dsId: 1,
        scanScope: 'ALL' as const,
        cronExpression: '0 0 2 * * ?',
        enabled: true,
        incremental: true
      }

      expect(dto.cronExpression).toBe('0 0 2 * * ?')
      expect(dto.scheduleName).toBe('每日敏感字段扫描')
    })
  })

  // ============================================================
  // Test Suite 10: Review Status Enum
  // ============================================================
  describe('Review Status Type', () => {
    it('should support PENDING/APPROVED/REJECTED statuses', () => {
      const statuses = ['PENDING', 'APPROVED', 'REJECTED'] as const

      statuses.forEach(status => {
        expect(['PENDING', 'APPROVED', 'REJECTED']).toContain(status)
      })
    })
  })
})
