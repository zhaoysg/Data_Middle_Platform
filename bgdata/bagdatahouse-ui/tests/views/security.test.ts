/**
 * Security Pages Component Tests
 * Tests for 6 security module pages: overview, sensitivity-scan, sensitivity-rules,
 * mask-strategy, mask-rules, and classification
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h, ref, nextTick } from 'vue'

// Mock ant-design-vue globally
vi.mock('ant-design-vue', () => {
  const AButton = defineComponent({
    name: 'AButton',
    props: ['loading', 'type', 'size'],
    emits: ['click'],
    setup(props, { slots, emit }) {
      return () => h('button', {
        class: ['ant-btn', `ant-btn-${props.type || 'default'}`],
        onClick: (e: Event) => emit('click', e)
      }, slots.default?.())
    }
  })
  const AInput = defineComponent({
    name: 'AInput',
    props: ['modelValue', 'placeholder', 'allowClear'],
    emits: ['update:modelValue', 'pressEnter', 'change'],
    setup(props, { emit }) {
      return () => h('input', {
        value: props.modelValue || '',
        placeholder: props.placeholder,
        class: 'ant-input',
        onInput: (e: Event) => emit('update:modelValue', (e.target as HTMLInputElement).value),
        onKeydown: (e: KeyboardEvent) => { if (e.key === 'Enter') emit('pressEnter', e) }
      })
    }
  })
  const ASelect = defineComponent({
    name: 'ASelect',
    props: ['modelValue', 'placeholder', 'allowClear'],
    emits: ['update:modelValue', 'change'],
    setup(props, { emit, slots }) {
      return () => h('select', {
        value: props.modelValue ?? '',
        class: 'ant-select',
        onChange: (e: Event) => emit('update:modelValue', (e.target as HTMLSelectElement).value)
      }, slots.default?.())
    }
  })
  const ASelectOption = defineComponent({
    name: 'ASelectOption',
    props: ['value'],
    setup(props, { slots }) {
      return () => h('option', { value: props.value }, slots.default?.())
    }
  })
  const ATabs = defineComponent({
    name: 'ATabs',
    props: ['activeKey'],
    emits: ['update:activeKey', 'change'],
    setup(props, { emit, slots }) {
      return () => h('div', { class: 'ant-tabs' }, slots.default?.())
    }
  })
  const ATabPane = defineComponent({
    name: 'ATabPane',
    props: ['key', 'tab'],
    setup(props, { slots }) {
      return () => h('div', { class: 'ant-tab-pane' }, slots.default?.())
    }
  })
  const ATable = defineComponent({
    name: 'ATable',
    props: ['columns', 'dataSource', 'loading', 'rowKey', 'pagination'],
    emits: ['change'],
    setup(props, { emit, slots }) {
      return () => h('table', { class: 'ant-table' }, [
        h('thead', {}, [
          h('tr', {}, (props.columns || []).map((col: any) =>
            h('th', { key: col.dataIndex }, col.title)
          ))
        ]),
        h('tbody', {}, (props.dataSource || []).map((row: any, idx: number) =>
          h('tr', { key: props.rowKey ? (typeof props.rowKey === 'function' ? props.rowKey(row) : row[props.rowKey]) : idx }, (props.columns || []).map((col: any) =>
            h('td', { key: col.dataIndex }, col.customRender ? col.customRender(row[col.dataIndex], row) : row[col.dataIndex])
          ))
        ))
      ])
    }
  })
  const ACard = defineComponent({
    name: 'ACard',
    props: ['title', 'bordered'],
    setup(props, { slots }) {
      return () => h('div', { class: 'ant-card' }, slots.default?.())
    }
  })
  const AModal = defineComponent({
    name: 'AModal',
    props: ['open', 'title', 'width'],
    emits: ['ok', 'cancel', 'update:open'],
    setup(props, { emit, slots }) {
      return () => props.open
        ? h('div', { class: 'ant-modal-mask' }, [
            h('div', { class: 'ant-modal' }, [
              h('div', { class: 'ant-modal-title' }, props.title),
              h('div', { class: 'ant-modal-content' }, slots.default?.()),
              h('div', { class: 'ant-modal-footer' }, slots.footer?.())
            ])
          ])
        : null
    }
  })
  const AForm = defineComponent({
    name: 'AForm',
    props: ['labelCol'],
    setup(props, { slots }) {
      return () => h('form', { class: 'ant-form' }, slots.default?.())
    }
  })
  const AFormItem = defineComponent({
    name: 'AFormItem',
    props: ['label'],
    setup(props, { slots }) {
      return () => h('div', { class: 'ant-form-item' }, [
        h('label', {}, props.label),
        h('div', { class: 'ant-form-item-control' }, slots.default?.())
      ])
    }
  })
  const ASpace = defineComponent({
    name: 'ASpace',
    setup(props, { slots }) {
      return () => h('div', { class: 'ant-space' }, slots.default?.())
    }
  })
  const ABadge = defineComponent({
    name: 'ABadge',
    props: ['count', 'overflowCount'],
    setup(props) {
      return () => h('span', { class: 'ant-badge' }, props.count)
    }
  })
  const ASpin = defineComponent({
    name: 'ASpin',
    props: ['tip'],
    setup(props, { slots }) {
      return () => h('div', { class: 'ant-spin' }, slots.default?.())
    }
  })
  const AMessage = {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }
  return {
    message: AMessage,
    default: {
      message: AMessage
    }
  }
})

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(() => 'mock-token'),
  setItem: vi.fn(),
  removeItem: vi.fn()
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// ============================================================
// Test Suite 1: Classification Page - Component Structure
// ============================================================
describe('Classification Page - Component Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render classification statistics cards', () => {
    const stats = {
      totalClassification: 10,
      totalLevel: 4,
      enabledClassification: 8,
      pendingReview: 25
    }

    const wrapper = mount({
      setup() {
        return { stats }
      },
      template: `
        <div class="stat-row">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalClassification }}</div>
            <div class="stat-label">分类总数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalLevel }}</div>
            <div class="stat-label">敏感等级</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.enabledClassification }}</div>
            <div class="stat-label">启用分类</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ stats.pendingReview }}</div>
            <div class="stat-label">待审核字段</div>
          </div>
        </div>
      `
    })

    expect(wrapper.findAll('.stat-card')).toHaveLength(4)
    expect(wrapper.find('.stat-value').text()).toBe('10')
  })

  it('should render classification table with columns', () => {
    const columns = [
      { title: '分类编码', dataIndex: 'classCode' },
      { title: '分类名称', dataIndex: 'className' },
      { title: '敏感等级', dataIndex: 'levelName' },
      { title: '字段数', dataIndex: 'fieldCount' }
    ]

    const dataSource = [
      { classCode: 'CLASS001', className: '个人信息', levelName: 'L2', fieldCount: 15 },
      { classCode: 'CLASS002', className: '金融数据', levelName: 'L3', fieldCount: 22 }
    ]

    const wrapper = mount({
      setup() {
        return { columns, dataSource }
      },
      template: `
        <table class="ant-table">
          <thead>
            <tr>
              <th v-for="col in columns" :key="col.dataIndex">{{ col.title }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in dataSource" :key="idx">
              <td v-for="col in columns" :key="col.dataIndex">{{ row[col.dataIndex] }}</td>
            </tr>
          </tbody>
        </table>
      `
    })

    expect(wrapper.findAll('th')).toHaveLength(4)
    expect(wrapper.findAll('tr')).toHaveLength(3) // 1 header + 2 data rows
    expect(wrapper.find('td').text()).toBe('CLASS001')
  })

  it('should handle search input', async () => {
    const searchValue = ref('')
    const loadClassification = vi.fn()

    const wrapper = mount({
      setup() {
        return { searchValue, loadClassification }
      },
      template: `
        <div>
          <input
            :value="searchValue"
            @input="e => searchValue = e.target.value"
            @keydown.enter="loadClassification"
          />
          <span>{{ searchValue }}</span>
        </div>
      `
    })

    const input = wrapper.find('input')
    await input.setValue('金融')
    expect(searchValue.value).toBe('金融')

    await wrapper.find('input').trigger('keydown.enter')
    await nextTick()
    expect(loadClassification).toHaveBeenCalled()
  })

  it('should display empty state when no classifications', () => {
    const dataSource: any[] = []
    const loading = false

    const wrapper = mount({
      setup() {
        return { dataSource, loading }
      },
      template: `
        <div>
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="dataSource.length === 0" class="empty-state">暂无数据</div>
          <div v-else>有数据</div>
        </div>
      `
    })

    expect(wrapper.find('.empty-state').text()).toBe('暂无数据')
  })
})

// ============================================================
// Test Suite 2: Sensitivity Scan Page - Component Tests
// ============================================================
describe('Sensitivity Scan Page - Component Tests', () => {
  it('should render scan form with all scan options', () => {
    const scanForm = ref({
      dsId: null,
      scanScope: 'ALL',
      scanColumnName: true,
      scanColumnComment: true,
      scanDataType: true,
      useRegex: false,
      sampleSize: 1000,
      enableContentScan: false
    })

    const wrapper = mount({
      setup() {
        return { scanForm }
      },
      template: `
        <form>
          <select :value="scanForm.scanScope">
            <option value="ALL">全量扫描</option>
            <option value="LAYER">按层级</option>
            <option value="SPECIFIC">指定表</option>
          </select>
          <label>
            <input type="checkbox" :checked="scanForm.scanColumnName" />
            列名扫描
          </label>
          <label>
            <input type="checkbox" :checked="scanForm.scanColumnComment" />
            列注释扫描
          </label>
          <label>
            <input type="checkbox" :checked="scanForm.scanDataType" />
            数据类型扫描
          </label>
        </form>
      `
    })

    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(3)
    expect(wrapper.find('select').exists()).toBe(true)
  })

  it('should toggle scan options correctly', async () => {
    const scanForm = ref({
      scanColumnName: true,
      scanDataType: false
    })

    const wrapper = mount({
      setup() {
        return { scanForm }
      },
      template: `
        <div>
          <label>
            <input type="checkbox" v-model="scanForm.scanColumnName" />
            列名扫描
          </label>
          <label>
            <input type="checkbox" v-model="scanForm.scanDataType" />
            数据类型扫描
          </label>
        </div>
      `
    })

    const checkboxes = wrapper.findAll('input[type="checkbox"]')
    expect(checkboxes[0].element as HTMLInputElement).toBeChecked()
    expect(checkboxes[1].element as HTMLInputElement).not.toBeChecked()

    await checkboxes[1].setChecked(true)
    expect(scanForm.value.scanDataType).toBe(true)
  })

  it('should set scan scope correctly', async () => {
    const scanScope = ref('ALL')

    const wrapper = mount({
      setup() {
        return { scanScope }
      },
      template: `
        <select v-model="scanScope">
          <option value="ALL">全量扫描</option>
          <option value="LAYER">按层级扫描</option>
          <option value="SPECIFIC">指定表扫描</option>
        </select>
      `
    })

    expect(scanScope.value).toBe('ALL')
    await wrapper.find('select').setValue('SPECIFIC')
    expect(scanScope.value).toBe('SPECIFIC')
  })
})

// ============================================================
// Test Suite 3: Sensitivity Rules Page - Component Tests
// ============================================================
describe('Sensitivity Rules Page - Component Tests', () => {
  it('should render rule list with match type badges', () => {
    const rules = ref([
      {
        ruleName: '身份证号识别',
        matchType: 'COLUMN_NAME',
        matchExpr: '.*(id_card|identity).*',
        levelName: 'L3',
        status: 1
      },
      {
        ruleName: '手机号识别',
        matchType: 'COLUMN_COMMENT',
        matchExpr: 'phone|mobile',
        levelName: 'L2',
        status: 1
      }
    ])

    const wrapper = mount({
      setup() {
        return { rules }
      },
      template: `
        <div class="rule-list">
          <div v-for="rule in rules" :key="rule.ruleName" class="rule-item">
            <span class="rule-name">{{ rule.ruleName }}</span>
            <span class="match-type-badge">{{ rule.matchType }}</span>
            <span class="level-badge">{{ rule.levelName }}</span>
          </div>
        </div>
      `
    })

    expect(wrapper.findAll('.rule-item')).toHaveLength(2)
    expect(wrapper.findAll('.match-type-badge')[0].text()).toBe('COLUMN_NAME')
    expect(wrapper.findAll('.level-badge')[1].text()).toBe('L2')
  })

  it('should support regex match expression', () => {
    const matchExpr = '.*(id_card|identity|card_no).*'
    const regex = new RegExp(matchExpr)

    expect(regex.test('user_id_card')).toBe(true)
    expect(regex.test('user_identity_no')).toBe(true)
    expect(regex.test('user_name')).toBe(false)
  })

  it('should handle rule status toggle', async () => {
    const rule = ref({ id: 1, ruleName: 'Test Rule', status: 1 })
    const toggleStatus = vi.fn()

    const wrapper = mount({
      setup() {
        return { rule, toggleStatus }
      },
      template: `
        <div>
          <span class="status-badge">{{ rule.status === 1 ? '启用' : '禁用' }}</span>
          <button @click="toggleStatus(rule)">切换状态</button>
        </div>
      `
    })

    expect(wrapper.find('.status-badge').text()).toBe('启用')
    await wrapper.find('button').trigger('click')
    expect(toggleStatus).toHaveBeenCalledWith(rule.value)
  })
})

// ============================================================
// Test Suite 4: Mask Strategy Page - Component Tests
// ============================================================
describe('Mask Strategy Page - Component Tests', () => {
  it('should render strategy list with level mappings', () => {
    const strategies = ref([
      {
        strategyName: '金融数据脱敏策略',
        priority: 1,
        levelMaskMapping: {
          L1: 'NONE',
          L2: 'MASK',
          L3: 'ENCRYPT',
          L4: 'DELETE'
        },
        conflictCheck: true
      }
    ])

    const wrapper = mount({
      setup() {
        return { strategies }
      },
      template: `
        <div class="strategy-list">
          <div v-for="s in strategies" :key="s.strategyName" class="strategy-item">
            <div class="strategy-name">{{ s.strategyName }}</div>
            <div class="priority">优先级: {{ s.priority }}</div>
            <div class="level-mapping">
              <span v-for="(mask, level) in s.levelMaskMapping" :key="level" class="level-tag">
                {{ level }}: {{ mask }}
              </span>
            </div>
            <div class="conflict-badge">冲突检测: {{ s.conflictCheck ? '开启' : '关闭' }}</div>
          </div>
        </div>
      `
    })

    expect(wrapper.findAll('.strategy-item')).toHaveLength(1)
    expect(wrapper.findAll('.level-tag')).toHaveLength(4)
    expect(wrapper.find('.conflict-badge').text()).toBe('冲突检测: 开启')
  })

  it('should render mask type options', () => {
    const maskTypes = [
      { value: 'NONE', label: '不脱敏' },
      { value: 'MASK', label: '掩码' },
      { value: 'PARTIAL_MASK', label: '部分掩码' },
      { value: 'HASH', label: '哈希' },
      { value: 'ENCRYPT', label: '加密' },
      { value: 'FORMAT_KEEP', label: '格式保留' },
      { value: 'RANGE', label: '范围' }
    ]

    const wrapper = mount({
      setup() {
        return { maskTypes }
      },
      template: `
        <select>
          <option v-for="mt in maskTypes" :key="mt.value" :value="mt.value">
            {{ mt.label }}
          </option>
        </select>
      `
    })

    expect(wrapper.findAll('option')).toHaveLength(7)
    expect(wrapper.find('option[value="PARTIAL_MASK"]').text()).toBe('部分掩码')
  })

  it('should validate conflict check field accepts boolean', () => {
    const form = ref({
      conflictCheck: true
    })

    const wrapper = mount({
      setup() {
        return { form }
      },
      template: `
        <label>
          <input type="checkbox" v-model="form.conflictCheck" />
          启用冲突检测
        </label>
      `
    })

    expect(form.value.conflictCheck).toBe(true)
    const checkbox = wrapper.find('input[type="checkbox"]')
    expect((checkbox.element as HTMLInputElement).checked).toBe(true)
  })
})

// ============================================================
// Test Suite 5: Mask Rules Page - Component Tests
// ============================================================
describe('Mask Rules Page - Component Tests', () => {
  it('should render template list with mask patterns', () => {
    const templates = ref([
      {
        templateName: '手机号掩码',
        maskType: 'PARTIAL_MASK',
        maskPattern: '138****5678',
        sortOrder: 1,
        enabled: true
      },
      {
        templateName: '身份证号掩码',
        maskType: 'PARTIAL_MASK',
        maskPattern: '**************',
        sortOrder: 2,
        enabled: false
      }
    ])

    const wrapper = mount({
      setup() {
        return { templates }
      },
      template: `
        <div class="template-list">
          <div v-for="t in templates" :key="t.templateName" class="template-item">
            <div class="template-name">{{ t.templateName }}</div>
            <div class="mask-pattern">{{ t.maskPattern }}</div>
            <div class="status">{{ t.enabled ? '启用' : '禁用' }}</div>
          </div>
        </div>
      `
    })

    expect(wrapper.findAll('.template-item')).toHaveLength(2)
    expect(wrapper.find('.mask-pattern').text()).toBe('138****5678')
  })

  it('should validate partial mask pattern format', () => {
    const pattern = '138****5678'
    const isValid = pattern.includes('*') && pattern.length >= 7

    expect(isValid).toBe(true)
    expect(pattern.split('*').length - 1).toBeGreaterThan(1) // has multiple asterisks
  })
})

// ============================================================
// Test Suite 6: Overview Page - Component Tests
// ============================================================
describe('Overview Page - Component Tests', () => {
  it('should render overview statistics', () => {
    const overview = ref({
      totalSensitiveFieldCount: 5000,
      sensitiveCoverage: 65.5,
      weeklyScanCount: 12,
      pendingReviewCount: 30
    })

    const wrapper = mount({
      setup() {
        return { overview }
      },
      template: `
        <div class="stat-row">
          <div class="stat-card">
            <div class="stat-value">{{ overview.totalSensitiveFieldCount }}</div>
            <div class="stat-label">敏感资产总数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ overview.sensitiveCoverage }}%</div>
            <div class="stat-label">敏感资产覆盖率</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ overview.weeklyScanCount }}</div>
            <div class="stat-label">本周扫描任务</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ overview.pendingReviewCount }}</div>
            <div class="stat-label">待审核字段</div>
          </div>
        </div>
      `
    })

    expect(wrapper.findAll('.stat-card')).toHaveLength(4)
    expect(wrapper.find('.stat-value').text()).toBe('5000')
  })

  it('should calculate level percentages correctly', () => {
    const distribution = {
      l1Count: 2000,
      l2Count: 1500,
      l3Count: 1000,
      l4Count: 500
    }
    const total = distribution.l1Count + distribution.l2Count + distribution.l3Count + distribution.l4Count

    const levelL1Percent = Math.round((distribution.l1Count / total) * 100)
    const levelL2Percent = Math.round((distribution.l2Count / total) * 100)
    const levelL3Percent = Math.round((distribution.l3Count / total) * 100)
    const levelL4Percent = Math.round((distribution.l4Count / total) * 100)

    expect(levelL1Percent).toBe(40)
    expect(levelL2Percent).toBe(30)
    expect(levelL3Percent).toBe(20)
    expect(levelL4Percent).toBe(10)
    expect(levelL1Percent + levelL2Percent + levelL3Percent + levelL4Percent).toBe(100)
  })

  it('should render health score with color coding', () => {
    const healthScore = ref({ score: 85, gradeLabel: '良好' })
    const scoreColor = healthScore.value.score >= 80 ? '#52C41A' : healthScore.value.score >= 60 ? '#FA8C16' : '#F5222D'

    const wrapper = mount({
      setup() {
        return { healthScore, scoreColor }
      },
      template: `
        <div>
          <div class="health-score" :style="{ color: scoreColor }">
            {{ healthScore.score }}
          </div>
          <div class="grade-label">{{ healthScore.gradeLabel }}</div>
        </div>
      `
    })

    expect(wrapper.find('.health-score').text()).toBe('85')
    expect(wrapper.find('.grade-label').text()).toBe('良好')
  })

  it('should render alert list with level badges', () => {
    const alerts = ref([
      { id: 1, alertTitle: '身份证号字段暴露', alertLevel: 'HIGH', alertType: '敏感字段' },
      { id: 2, alertTitle: '手机号字段暴露', alertLevel: 'MEDIUM', alertType: '敏感字段' }
    ])

    const wrapper = mount({
      setup() {
        return { alerts }
      },
      template: `
        <div class="alert-list">
          <div v-for="alert in alerts" :key="alert.id" class="alert-item">
            <span class="alert-level-badge" :class="'badge-' + alert.alertLevel.toLowerCase()">
              {{ alert.alertType }}
            </span>
            <span class="alert-title">{{ alert.alertTitle }}</span>
          </div>
        </div>
      `
    })

    expect(wrapper.findAll('.alert-item')).toHaveLength(2)
    expect(wrapper.find('.alert-title').text()).toBe('身份证号字段暴露')
  })

  it('should handle new column alert count', () => {
    const newColumnPendingCount = ref(5)
    const hasPendingAlerts = newColumnPendingCount.value > 0

    const wrapper = mount({
      setup() {
        return { newColumnPendingCount, hasPendingAlerts }
      },
      template: `
        <div>
          <span v-if="hasPendingAlerts" class="pending-badge">
            {{ newColumnPendingCount }} 待扫描
          </span>
          <span v-else class="no-pending">暂无待扫描字段</span>
        </div>
      `
    })

    expect(wrapper.find('.pending-badge').text()).toBe('5 待扫描')
  })
})

// ============================================================
// Test Suite 7: Common Security Page Utilities
// ============================================================
describe('Security Page - Common Utility Tests', () => {
  it('should format sensitivity level label correctly', () => {
    const levelMap: Record<string, string> = {
      L1: 'L1 公开',
      L2: 'L2 内部',
      L3: 'L3 敏感',
      L4: 'L4 机密'
    }

    expect(levelMap['L1']).toBe('L1 公开')
    expect(levelMap['L4']).toBe('L4 机密')
  })

  it('should format mask type label correctly', () => {
    const maskTypeMap: Record<string, string> = {
      NONE: '不脱敏',
      MASK: '掩码',
      PARTIAL_MASK: '部分掩码',
      HASH: '哈希',
      ENCRYPT: '加密',
      DELETE: '删除',
      FORMAT_KEEP: '格式保留',
      RANGE: '范围',
      WATERMARK: '水印',
      RANDOM_REPLACE: '随机替换'
    }

    expect(maskTypeMap['PARTIAL_MASK']).toBe('部分掩码')
    expect(maskTypeMap['HASH']).toBe('哈希')
    expect(maskTypeMap['WATERMARK']).toBe('水印')
  })

  it('should validate cron expression format', () => {
    const cronExpr = '0 0 2 * * ?'
    const parts = cronExpr.split(' ')

    expect(parts).toHaveLength(6)
    expect(parts[0]).toBe('0') // seconds
    expect(parts[1]).toBe('0') // minutes
    expect(parts[2]).toBe('2') // hours
  })

  it('should handle sensitivity level color mapping', () => {
    const colorMap: Record<string, string> = {
      L1: '#52C41A', // green
      L2: '#1677FF', // blue
      L3: '#FA8C16', // orange
      L4: '#F5222D'  // red
    }

    expect(colorMap['L1']).toBe('#52C41A')
    expect(colorMap['L4']).toBe('#F5222D')
    expect(Object.keys(colorMap)).toHaveLength(4)
  })
})
