<template>
  <div class="mask-strategy-page">
    <!-- ========== 页面标题区 ========== -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#msGrad)"/>
            <path d="M8 12l3 3 5-5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="12" cy="12" r="9" stroke="white" stroke-width="1.5"/>
            <defs>
              <linearGradient id="msGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#FA541C"/>
                <stop offset="100%" stop-color="#FFD591"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">脱敏策略管理</h1>
          <p class="page-subtitle">配置敏感等级到脱敏类型的映射规则，支持场景分组、白名单配置及策略冲突检测</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="loadData" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="openDrawer('add')">
          <template #icon><PlusOutlined /></template>
          新增策略
        </a-button>
      </div>
    </div>

    <!-- ========== 统计卡片行 ========== -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #FA541C 0%, #FFD591 100%)">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">策略总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-value">{{ stats.enabled || 0 }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #1677FF 0%, #69B1FF 100%)">
          <div class="stat-value">{{ stats.sceneCount || 0 }}</div>
          <div class="stat-label">应用场景</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6" :lg="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)">
          <div class="stat-value">{{ stats.whitelistCount || 0 }}</div>
          <div class="stat-label">白名单总数</div>
        </div>
      </a-col>
    </a-row>

    <!-- ========== 主内容卡片 ========== -->
    <div class="table-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <a-select
            v-model:value="filterSceneType"
            placeholder="应用场景"
            style="width: 200px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option v-for="s in sceneTypeOptions" :key="s.value" :value="s.value">
              <span class="scene-option">
                <span class="scene-dot" :style="{ background: s.color }"></span>
                {{ s.label }}
              </span>
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterStatus"
            placeholder="启用状态"
            style="width: 120px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option :value="1">已启用</a-select-option>
            <a-select-option :value="0">已禁用</a-select-option>
          </a-select>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </div>
        <div class="toolbar-right">
          <a-button @click="handleConflictDetect" :loading="conflictLoading">
            <template #icon><AlertOutlined /></template>
            冲突检测
          </a-button>
        </div>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="paginationConfig"
        :scroll="{ x: 1300 }"
        row-key="id"
        @change="handleTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <!-- 策略名称 -->
          <template v-if="column.key === 'strategyName'">
            <div class="strategy-name-cell">
              <div class="strategy-name-row">
                <a-tooltip :title="record.strategyCode || ''">
                  <span class="strategy-name-text">{{ record.strategyName }}</span>
                </a-tooltip>
              </div>
              <div class="strategy-code-text">{{ record.strategyCode }}</div>
            </div>
          </template>

          <!-- 应用场景 -->
          <template v-if="column.key === 'sceneType'">
            <a-tag :color="getSceneColor(record.sceneType)" style="font-weight: 500">
              {{ record.sceneTypeLabel || record.sceneType || '-' }}
            </a-tag>
          </template>

          <!-- 等级→脱敏映射预览 -->
          <template v-if="column.key === 'levelMapping'">
            <div class="level-mapping-cell">
              <a-tooltip v-if="parseLevelMapping(record.levelMaskMapping).length > 0">
                <template #title>
                  <div v-for="m in parseLevelMapping(record.levelMaskMapping)" :key="m.level" style="margin-bottom: 4px">
                    <a-tag :color="getLevelColor(m.level)" style="margin-right: 4px">{{ m.level }}</a-tag>
                    → {{ m.maskType || '不脱敏' }}
                  </div>
                </template>
                <a-tag v-for="m in parseLevelMapping(record.levelMaskMapping).slice(0, 3)" :key="m.level"
                  :color="getLevelColor(m.level)" style="margin-bottom: 2px; font-size: 11px">
                  {{ m.level }}→{{ m.maskType || 'NONE' }}
                </a-tag>
                <span v-if="parseLevelMapping(record.levelMaskMapping).length > 3" style="color: #8C8C8C; font-size: 11px">
                  +{{ parseLevelMapping(record.levelMaskMapping).length - 3 }}
                </span>
              </a-tooltip>
              <span v-else style="color: #d9d9d9">—</span>
            </div>
          </template>

          <!-- 白名单数 -->
          <template v-if="column.key === 'whitelistCount'">
            <span style="color: #722ED1; font-weight: 500">{{ record.whitelistCount || 0 }}</span>
          </template>

          <!-- 优先级 -->
          <template v-if="column.key === 'priority'">
            <span style="color: #8C8C8C">P{{ record.priority || 0 }}</span>
          </template>

          <!-- 启用状态 -->
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'ENABLED' ? 'success' : 'default'"
              :text="record.statusLabel || (record.status === 'ENABLED' ? '已启用' : '已禁用')"
            />
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-tooltip title="编辑">
                <a-button type="text" size="small" @click="openDrawer('edit', record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="白名单">
                <a-button type="text" size="small" @click="openWhitelistDrawer(record)">
                  <template #icon><TeamOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm title="确定删除该策略？" @confirm="handleDelete(record)">
                <a-tooltip title="删除">
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <div class="table-empty">
            <SafetyCertificateOutlined class="empty-icon" />
            <div class="empty-title">暂无脱敏策略</div>
            <div class="empty-desc">点击「新增策略」创建第一个脱敏策略</div>
            <a-button type="primary" @click="openDrawer('add')">新增策略</a-button>
          </div>
        </template>
      </a-table>
    </div>

    <!-- ========== 新增/编辑策略抽屉 ========== -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="drawerMode === 'add' ? '新增脱敏策略' : '编辑脱敏策略'"
      :width="680"
      :mask-closable="false"
      @close="drawerVisible = false"
    >
      <template v-if="drawerVisible">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          layout="horizontal"
        >
          <a-form-item label="策略名称" name="strategyName">
            <a-input
              v-model:value="formData.strategyName"
              placeholder="请输入策略名称，如：开发环境展示策略"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="策略编码" name="strategyCode">
            <a-input
              v-model:value="formData.strategyCode"
              placeholder="唯一标识，如：DEV_SHOW_STRATEGY"
              :disabled="drawerMode === 'edit'"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="应用场景" name="sceneType">
            <a-select
              v-model:value="formData.sceneType"
              placeholder="请选择应用场景"
            >
              <a-select-option v-for="s in sceneTypeOptions" :key="s.value" :value="s.value">
                <span class="scene-option">
                  <span class="scene-dot" :style="{ background: s.color }"></span>
                  {{ s.label }}
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="策略描述" name="description">
            <a-textarea
              v-model:value="formData.description"
              placeholder="简要描述该策略的适用范围和管理要求"
              :rows="3"
              :maxlength="200"
              show-count
            />
          </a-form-item>

          <a-form-item label="优先级" name="priority">
            <a-input-number
              v-model:value="formData.priority"
              :min="1"
              :max="999"
              style="width: 100%"
              placeholder="数值越小优先级越高，默认100"
            />
          </a-form-item>

          <a-form-item label="冲突检测" name="conflictCheck">
            <a-switch v-model:checked="formConflictCheck" checked-children="启用" un-checked-children="禁用" />
            <div class="form-tip">启用后系统将自动检测同一字段在多个策略中的冲突规则</div>
          </a-form-item>
        </a-form>

        <!-- ========== 等级→脱敏类型映射配置 ========== -->
        <a-divider orientation="left">
          <span class="divider-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="#FA541C" stroke-width="2"/>
              <path d="M12 8v4M12 16h.01" stroke="#FA541C" stroke-width="2" stroke-linecap="round"/>
            </svg>
            敏感等级 → 脱敏类型映射
          </span>
        </a-divider>

        <div class="level-mapping-panel">
          <div class="mapping-tip">
            配置不同敏感等级字段在当前场景下的脱敏方式。不配置则默认不脱敏。
          </div>

          <div
            v-for="(rule, index) in formLevelRules"
            :key="index"
            class="mapping-rule-item"
          >
            <div class="mapping-rule-header">
              <div class="level-badge" :style="{ background: getLevelColor(rule.levelCode) }">
                {{ rule.levelCode || '?' }}
              </div>
              <span class="level-label">{{ getLevelLabel(rule.levelCode) }}</span>
            </div>
            <div class="mapping-rule-body">
              <a-select
                v-model="rule.maskType"
                placeholder="选择脱敏类型"
                style="width: 160px"
              >
                <a-select-option value="">不脱敏 (NONE)</a-select-option>
                <a-select-option value="MASK">掩码遮蔽 (MASK)</a-select-option>
                <a-select-option value="HIDE">完全隐藏 (HIDE)</a-select-option>
                <a-select-option value="ENCRYPT">加密 (ENCRYPT)</a-select-option>
                <a-select-option value="DELETE">删除 (DELETE)</a-select-option>
                <a-select-option value="HASH">哈希脱敏 (HASH)</a-select-option>
                <a-select-option value="FORMAT_KEEP">保留格式加密</a-select-option>
                <a-select-option value="RANGE">区间化 (RANGE)</a-select-option>
              </a-select>
              <a-input
                v-model="rule.maskPattern"
                placeholder="可选：脱敏正则"
                style="width: 200px"
                allowClear
              />
              <a-tag :color="getMaskTypeColor(rule.maskType)" style="margin-left: 8px; min-width: 70px; text-align: center">
                {{ getMaskTypeLabel(rule.maskType) }}
              </a-tag>
            </div>
          </div>

          <!-- 预览示例 -->
          <div class="mapping-preview">
            <div class="preview-title">映射预览</div>
            <div class="preview-table">
              <div class="preview-row preview-header">
                <div class="preview-cell">敏感等级</div>
                <div class="preview-cell">示例输入</div>
                <div class="preview-cell">脱敏结果</div>
              </div>
              <div v-for="rule in formLevelRules" :key="rule.levelCode" class="preview-row">
                <div class="preview-cell">
                  <a-tag :color="getLevelColor(rule.levelCode)">{{ rule.levelCode }}</a-tag>
                </div>
                <div class="preview-cell preview-input">{{ getLevelSample(rule.levelCode) }}</div>
                <div class="preview-cell preview-result">{{ applyPreviewMask(getLevelSample(rule.levelCode), rule.maskType, rule.maskPattern) }}</div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ drawerMode === 'add' ? '创建策略' : '保存修改' }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- ========== 白名单管理抽屉 ========== -->
    <a-drawer
      v-model:open="whitelistDrawerVisible"
      :title="`白名单管理 — ${currentStrategy?.strategyName || ''}`"
      :width="720"
      :mask-closable="false"
      @close="whitelistDrawerVisible = false"
    >
      <template v-if="whitelistDrawerVisible && currentStrategy">
        <!-- 策略基本信息 -->
        <div class="strategy-summary">
          <div class="summary-tags">
            <a-tag :color="getSceneColor(currentStrategy.sceneType)">{{ currentStrategy.sceneTypeLabel }}</a-tag>
            <a-badge
              :status="currentStrategy.status === 'ENABLED' ? 'success' : 'default'"
              :text="currentStrategy.statusLabel"
            />
          </div>
          <div class="summary-desc" v-if="currentStrategy.description">
            {{ currentStrategy.description }}
          </div>
        </div>

        <!-- 白名单工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <a-select
              v-model:value="wlFilterType"
              placeholder="实体类型"
              style="width: 120px"
              allowClear
              @change="loadWhitelist"
            >
              <a-select-option value="USER">用户</a-select-option>
              <a-select-option value="ROLE">角色</a-select-option>
            </a-select>
            <a-select
              v-model:value="wlFilterStatus"
              placeholder="状态"
              style="width: 120px"
              allowClear
              @change="loadWhitelist"
            >
              <a-select-option value="APPROVED">已批准</a-select-option>
              <a-select-option value="PENDING">待审批</a-select-option>
            </a-select>
          </div>
          <div class="toolbar-right">
            <a-button type="primary" @click="openWlDrawer()">
              <template #icon><PlusOutlined /></template>
              添加白名单
            </a-button>
          </div>
        </div>

        <!-- 白名单列表 -->
        <a-table
          :columns="wlColumns"
          :data-source="whitelistData"
          :loading="wlLoading"
          :pagination="wlPaginationConfig"
          row-key="id"
          @change="handleWlTableChange"
          size="small"
          style="margin-top: 8px"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'entityType'">
              <a-tag :color="record.entityType === 'USER' ? 'blue' : 'purple'">
                {{ record.entityType === 'USER' ? '用户' : '角色' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'entityName'">
              <span style="font-weight: 500">{{ record.entityName || '-' }}</span>
            </template>
            <template v-if="column.key === 'validity'">
              <div class="validity-cell">
                <span v-if="record.validFrom" style="color: #8C8C8C; font-size: 12px">
                  {{ formatDate(record.validFrom) }}
                </span>
                <span v-if="record.validFrom && record.validTo" style="color: #8C8C8C"> ~ </span>
                <span v-if="record.validTo" style="color: #8C8C8C; font-size: 12px">
                  {{ formatDate(record.validTo) }}
                </span>
                <span v-if="!record.validFrom && !record.validTo" style="color: #d9d9d9">永久有效</span>
              </div>
            </template>
            <template v-if="column.key === 'status'">
              <a-badge
                :status="getWlStatusBadge(record.status)"
                :text="record.statusLabel || record.status"
              />
            </template>
            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-tooltip title="编辑">
                  <a-button type="text" size="small" @click="openWlDrawer(record)">
                    <template #icon><EditOutlined /></template>
                  </a-button>
                </a-tooltip>
                <a-popconfirm title="确定删除该白名单？" @confirm="handleWlDelete(record)">
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="whitelistDrawerVisible = false">关闭</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- ========== 新增/编辑白名单抽屉 ========== -->
    <a-drawer
      v-model:open="wlFormDrawerVisible"
      :title="wlEditMode === 'add' ? '添加白名单' : '编辑白名单'"
      :width="480"
      :mask-closable="false"
      @close="wlFormDrawerVisible = false"
    >
      <template v-if="wlFormDrawerVisible">
        <a-form
          ref="wlFormRef"
          :model="wlFormData"
          :rules="wlFormRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          layout="horizontal"
        >
          <a-form-item label="实体类型" name="entityType">
            <a-select v-model:value="wlFormData.entityType" placeholder="请选择">
              <a-select-option value="USER">用户 (USER)</a-select-option>
              <a-select-option value="ROLE">角色 (ROLE)</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="实体名称" name="entityName">
            <a-input
              v-model:value="wlFormData.entityName"
              placeholder="请输入用户姓名或角色名称"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="生效时间" name="validFrom">
            <a-date-picker
              v-model:value="wlFormData.validFrom"
              style="width: 100%"
              show-time
              format="YYYY-MM-DD HH:mm"
              placeholder="留空表示立即生效"
            />
          </a-form-item>

          <a-form-item label="失效时间" name="validTo">
            <a-date-picker
              v-model:value="wlFormData.validTo"
              style="width: 100%"
              show-time
              format="YYYY-MM-DD HH:mm"
              placeholder="留空表示永久有效"
            />
          </a-form-item>

          <a-form-item label="申请原因" name="reason">
            <a-textarea
              v-model:value="wlFormData.reason"
              placeholder="请输入申请原因"
              :rows="3"
              :maxlength="200"
              show-count
            />
          </a-form-item>
        </a-form>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="wlFormDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="wlSubmitLoading" @click="handleWlSubmit">
            {{ wlEditMode === 'add' ? '添加' : '保存' }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- ========== 冲突检测结果弹窗 ========== -->
    <a-modal
      v-model:open="conflictModalVisible"
      title="策略冲突检测结果"
      :footer="null"
      :width="600"
    >
      <template v-if="conflictResults.length === 0">
        <a-result
          status="success"
          title="未检测到冲突"
          sub-title="当前所有脱敏策略均无冲突，请放心使用"
        />
      </template>
      <template v-else>
        <a-alert type="warning" show-icon style="margin-bottom: 16px">
          <template #message>检测到 {{ conflictResults.length }} 个策略冲突</template>
          <template #description>以下字段在多个策略中配置了不同的脱敏规则，请手动确认优先级</template>
        </a-alert>
        <div class="conflict-list">
          <div v-for="(conflict, idx) in conflictResults" :key="idx" class="conflict-item">
            <div class="conflict-field">
              <code>{{ conflict }}</code>
            </div>
            <div class="conflict-desc">该字段在多个策略中脱敏规则不一致</div>
          </div>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
  TeamOutlined, AlertOutlined, SafetyCertificateOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  pageMaskStrategy,
  getMaskStrategyById,
  saveMaskStrategy,
  updateMaskStrategy,
  deleteMaskStrategy,
  enableMaskStrategy,
  disableMaskStrategy,
  detectStrategyConflicts,
  pageMaskWhitelist,
  saveMaskWhitelist,
  updateMaskWhitelist,
  deleteMaskWhitelist,
  type SecMaskStrategyVO,
  type SecMaskStrategySaveDTO,
  type SecMaskWhitelistVO,
  type SecMaskWhitelistSaveDTO
} from '@/api/security'

// ============ 场景类型选项 ============
const sceneTypeOptions = [
  { value: 'DEVELOP_SHOW', label: '数据开发界面展示', color: '#1677FF' },
  { value: 'DATA_MAP_SHOW', label: '数据地图/元数据预览', color: '#13C2C2' },
  { value: 'ANALYSIS_QUERY', label: '数据分析 SQL 查询', color: '#722ED1' },
  { value: 'EXPORT_RESULT', label: '导出结果', color: '#FA541C' },
  { value: 'PRINT_REPORT', label: '报表打印', color: '#FA8C16' }
]

function getSceneColor(sceneType?: string) {
  const found = sceneTypeOptions.find(s => s.value === sceneType)
  return found ? found.color : '#8C8C8C'
}

// ============ 敏感等级配置 ============
const LEVEL_OPTIONS = [
  { levelCode: 'L4', levelName: '机密', color: '#F5222D', sample: '13812345678' },
  { levelCode: 'L3', levelName: '敏感', color: '#FA8C16', sample: 'test@example.com' },
  { levelCode: 'L2', levelName: '内部', color: '#1677FF', sample: '192.168.1.1' },
  { levelCode: 'L1', levelName: '公开', color: '#52C41A', sample: '公开内容' }
]

function getLevelColor(level?: string) {
  const found = LEVEL_OPTIONS.find(l => l.levelCode === level)
  return found ? found.color : '#8C8C8C'
}

function getLevelLabel(level?: string) {
  const found = LEVEL_OPTIONS.find(l => l.levelCode === level)
  return found ? `${found.levelName}级` : '未知等级'
}

function getLevelSample(level?: string) {
  const found = LEVEL_OPTIONS.find(l => l.levelCode === level)
  return found ? found.sample : ''
}

// ============ 脱敏类型工具 ============
function getMaskTypeColor(maskType?: string) {
  const map: Record<string, string> = {
    NONE: '#8C8C8C', MASK: '#FAAD14', HIDE: '#722ED1',
    ENCRYPT: '#F5222D', DELETE: '#F5222D', HASH: '#13C2C2',
    RANDOM_REPLACE: '#1677FF', RANGE: '#FA8C16', WATERMARK: '#722ED1', FORMAT_KEEP: '#1677FF'
  }
  return map[maskType || ''] || 'default'
}

function getMaskTypeLabel(maskType?: string) {
  const map: Record<string, string> = {
    NONE: '不脱敏', MASK: '掩码', HIDE: '隐藏',
    ENCRYPT: '加密', DELETE: '删除', HASH: '哈希', RANGE: '区间化', FORMAT_KEEP: '格式加密'
  }
  return map[maskType || ''] || '未配置'
}

function applyPreviewMask(input: string, maskType?: string, _pattern?: string): string {
  if (!input) return '-'
  switch (maskType) {
    case 'NONE': return input
    case 'MASK': return input.slice(0, 3) + '****' + input.slice(-4)
    case 'HIDE': return '***'
    case 'DELETE': return '[已删除]'
    case 'ENCRYPT': {
      return input.split('').map(c => (c.charCodeAt(0) ^ 42).toString(16).padStart(2, '0')).join('').slice(0, 16)
    }
    case 'HASH': return 'A1B2C3D4'
    case 'FORMAT_KEEP': return '1**2**3**'
    case 'RANGE': return '5,000-10,000'
    default: return input
  }
}

// ============ 等级映射解析 ============
interface LevelRule { level: string; levelCode: string; levelName: string; maskType: string; maskTypeLabel: string; maskPattern: string }

function parseLevelMapping(jsonStr?: string): LevelRule[] {
  if (!jsonStr) return []
  try {
    const parsed = JSON.parse(jsonStr) as Array<{ levelCode: string; maskType: string; maskPattern?: string }>
    return parsed.map(p => ({
      levelCode: p.levelCode || '',
      levelName: getLevelLabel(p.levelCode),
      maskType: p.maskType || '',
      maskTypeLabel: getMaskTypeLabel(p.maskType),
      maskPattern: p.maskPattern || ''
    }))
  } catch {
    return []
  }
}

// ============ 统计数据 ============
const stats = reactive({
  total: 0,
  enabled: 0,
  sceneCount: 0,
  whitelistCount: 0
})

// ============ 状态 ============
const loading = ref(false)
const tableData = ref<SecMaskStrategyVO[]>([])
const filterSceneType = ref<string | undefined>()
const filterStatus = ref<number | undefined>()
const conflictLoading = ref(false)
const conflictModalVisible = ref(false)
const conflictResults = ref<string[]>([])

// 分页
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const paginationConfig = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

// 表格列
const columns = [
  { title: '策略名称', key: 'strategyName', width: 220 },
  { title: '应用场景', key: 'sceneType', width: 180, align: 'center' as const },
  { title: '等级映射', key: 'levelMapping', width: 240 },
  { title: '白名单数', key: 'whitelistCount', width: 90, align: 'center' as const },
  { title: '优先级', key: 'priority', width: 80, align: 'center' as const },
  { title: '状态', key: 'status', width: 100, align: 'center' as const },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' as const }
]

// ============ 策略抽屉 ============
const drawerVisible = ref(false)
const drawerMode = ref<'add' | 'edit'>('add')
const editingId = ref<number | undefined>()
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formConflictCheck = ref(false)

interface LevelRuleForm { levelCode: string; levelName: string; maskType: string; maskPattern: string }

const formData = reactive<SecMaskStrategySaveDTO>({
  id: undefined,
  strategyName: '',
  strategyCode: '',
  sceneType: undefined as any,
  description: '',
  priority: 100,
  conflictCheck: false,
  levelRules: []
})

const formLevelRules = ref<LevelRuleForm[]>(
  LEVEL_OPTIONS.map(l => ({ levelCode: l.levelCode, levelName: l.levelName, maskType: '', maskPattern: '' }))
)

const formRules = {
  strategyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  strategyCode: [{ required: true, message: '请输入策略编码', trigger: 'blur' }],
  sceneType: [{ required: true, message: '请选择应用场景', trigger: 'change' }]
}

// ============ 白名单抽屉 ============
const whitelistDrawerVisible = ref(false)
const currentStrategy = ref<SecMaskStrategyVO | null>(null)
const wlLoading = ref(false)
const wlData = ref<SecMaskWhitelistVO[]>([])
const wlFilterType = ref<string | undefined>()
const wlFilterStatus = ref<string | undefined>()
const wlPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const wlPaginationConfig = computed(() => ({
  current: wlPagination.current, pageSize: wlPagination.pageSize,
  total: wlPagination.total, showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

const wlColumns = [
  { title: '类型', key: 'entityType', width: 80 },
  { title: '实体名称', key: 'entityName', width: 160 },
  { title: '有效期', key: 'validity', width: 220 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'createUserName', key: 'createUserName', width: 100 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const }
]

const wlFormDrawerVisible = ref(false)
const wlEditMode = ref<'add' | 'edit'>('add')
const wlEditingId = ref<number | undefined>()
const wlSubmitLoading = ref(false)
const wlFormRef = ref<FormInstance>()

const wlFormData = reactive<{
  entityType: string
  entityName: string
  validFrom: any
  validTo: any
  reason: string
}>({
  entityType: 'USER',
  entityName: '',
  validFrom: null,
  validTo: null,
  reason: ''
})

const wlFormRules = {
  entityType: [{ required: true, message: '请选择实体类型', trigger: 'change' }],
  entityName: [{ required: true, message: '请输入实体名称', trigger: 'blur' }]
}

function getWlStatusBadge(status?: string): 'success' | 'processing' | 'error' | 'default' {
  const map: Record<string, 'success' | 'processing' | 'error' | 'default'> = {
    APPROVED: 'success', PENDING: 'processing', REJECTED: 'error'
  }
  return map[status || ''] || 'default'
}

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  try { return dayjs(dateStr).format('YYYY-MM-DD HH:mm') } catch { return dateStr }
}

// ============ 加载策略列表 ============
async function loadData() {
  loading.value = true
  try {
    const res = await pageMaskStrategy({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      sceneType: filterSceneType.value,
      status: filterStatus.value === undefined ? undefined : filterStatus.value === 1 ? 'ENABLED' : 'DISABLED'
    })

    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      tableData.value = records
      pagination.total = res.data.total ?? 0

      // 统计
      stats.total = res.data.total ?? 0
      stats.enabled = records.filter(r => r.status === 'ENABLED').length
      stats.sceneCount = new Set(records.map(r => r.sceneType).filter(Boolean)).size
      stats.whitelistCount = records.reduce((sum, r) => sum + (r.whitelistCount || 0), 0)
    } else {
      tableData.value = []
      pagination.total = 0
    }
  } catch (error) {
    message.error('加载策略数据失败')
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  filterSceneType.value = undefined
  filterStatus.value = undefined
  pagination.current = 1
  loadData()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadData()
}

// ============ 策略增删改 ============
function openDrawer(mode: 'add' | 'edit', record?: SecMaskStrategyVO) {
  drawerMode.value = mode
  if (mode === 'edit' && record) {
    editingId.value = record.id
    Object.assign(formData, {
      id: record.id,
      strategyName: record.strategyName,
      strategyCode: record.strategyCode,
      sceneType: record.sceneType,
      description: record.description,
      priority: record.priority || 100,
      conflictCheck: record.conflictCheck
    })
    formConflictCheck.value = record.conflictCheck === 1
    loadLevelRulesFromRecord(record)
  } else {
    editingId.value = undefined
    Object.assign(formData, {
      id: undefined,
      strategyName: '',
      strategyCode: '',
      sceneType: undefined as any,
      description: '',
      priority: 100,
      conflictCheck: 0
    })
    formConflictCheck.value = false
    formLevelRules.value = LEVEL_OPTIONS.map(l => ({ levelCode: l.levelCode, levelName: l.levelName, maskType: '', maskPattern: '' }))
  }
  drawerVisible.value = true
}

function loadLevelRulesFromRecord(record: SecMaskStrategyVO) {
  const parsed = parseLevelMapping(record.levelMaskMapping)
  formLevelRules.value = LEVEL_OPTIONS.map(l => {
    const found = parsed.find(p => p.levelCode === l.levelCode)
    return {
      levelCode: l.levelCode,
      levelName: l.levelName,
      maskType: found?.maskType || '',
      maskPattern: found?.maskPattern || ''
    }
  })
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch { return }

  submitLoading.value = true
  try {
    // 组装 levelMaskMapping JSON
    const levelMaskMapping = JSON.stringify(
      formLevelRules.value
        .filter(r => r.maskType) // 只保存有配置的等级
        .map(r => ({ levelCode: r.levelCode, maskType: r.maskType, maskPattern: r.maskPattern || undefined }))
    )

    const payload: SecMaskStrategySaveDTO = {
      ...formData,
      levelMaskMapping,
      conflictCheck: formConflictCheck.value
    }

    if (drawerMode.value === 'add') {
      const res = await saveMaskStrategy(payload)
      if (res.code === 200) {
        message.success('策略创建成功')
        drawerVisible.value = false
        loadData()
      } else {
        message.error(res.message || '创建失败')
      }
    } else {
      const res = await updateMaskStrategy(editingId.value!, payload)
      if (res.code === 200) {
        message.success('策略更新成功')
        drawerVisible.value = false
        loadData()
      } else {
        message.error(res.message || '更新失败')
      }
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(record: SecMaskStrategyVO) {
  if (!record.id) return
  try {
    const res = await deleteMaskStrategy(record.id)
    if (res.code === 200) {
      message.success('删除成功')
      loadData()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
}

async function handleConflictDetect() {
  conflictLoading.value = true
  try {
    const res = await detectStrategyConflicts()
    if (res.code === 200) {
      conflictResults.value = res.data || []
      conflictModalVisible.value = true
    } else {
      message.error(res.message || '检测失败')
    }
  } catch {
    message.error('冲突检测失败')
  } finally {
    conflictLoading.value = false
  }
}

// ============ 白名单 ============
async function openWhitelistDrawer(record: SecMaskStrategyVO) {
  currentStrategy.value = record
  wlPagination.current = 1
  wlFilterType.value = undefined
  wlFilterStatus.value = undefined
  whitelistDrawerVisible.value = true
  await loadWhitelist()
}

async function loadWhitelist() {
  if (!currentStrategy.value?.id) return
  wlLoading.value = true
  try {
    const res = await pageMaskWhitelist({
      pageNum: wlPagination.current,
      pageSize: wlPagination.pageSize,
      strategyId: currentStrategy.value.id,
      entityType: wlFilterType.value,
      status: wlFilterStatus.value
    })
    if (res.code === 200 && res.data) {
      wlData.value = res.data.records || []
      wlPagination.total = res.data.total ?? 0
    } else {
      wlData.value = []
    }
  } catch {
    wlData.value = []
  } finally {
    wlLoading.value = false
  }
}

function handleWlTableChange(pag: any) {
  wlPagination.current = pag.current || 1
  wlPagination.pageSize = pag.pageSize || 10
  loadWhitelist()
}

function openWlDrawer(record?: SecMaskWhitelistVO) {
  if (record) {
    wlEditMode.value = 'edit'
    wlEditingId.value = record.id
    Object.assign(wlFormData, {
      entityType: record.entityType || 'USER',
      entityName: record.entityName || '',
      validFrom: record.validFrom ? dayjs(record.validFrom) : null,
      validTo: record.validTo ? dayjs(record.validTo) : null,
      reason: record.reason || ''
    })
  } else {
    wlEditMode.value = 'add'
    wlEditingId.value = undefined
    Object.assign(wlFormData, {
      entityType: 'USER',
      entityName: '',
      validFrom: null,
      validTo: null,
      reason: ''
    })
  }
  wlFormDrawerVisible.value = true
}

async function handleWlSubmit() {
  try {
    await wlFormRef.value?.validate()
  } catch { return }

  wlSubmitLoading.value = true
  try {
    const payload: SecMaskWhitelistSaveDTO = {
      id: wlEditingId.value,
      strategyId: currentStrategy.value?.id,
      entityType: wlFormData.entityType,
      entityName: wlFormData.entityName,
      validFrom: wlFormData.validFrom ? wlFormData.validFrom.format('YYYY-MM-DD HH:mm:ss') : undefined,
      validTo: wlFormData.validTo ? wlFormData.validTo.format('YYYY-MM-DD HH:mm:ss') : undefined,
      description: wlFormData.reason || undefined,
      status: 1
    }

    const res = wlEditMode.value === 'add'
      ? await saveMaskWhitelist(payload)
      : await updateMaskWhitelist(wlEditingId.value!, payload)

    if (res.code === 200) {
      message.success(wlEditMode.value === 'add' ? '白名单添加成功' : '白名单更新成功')
      wlFormDrawerVisible.value = false
      loadWhitelist()
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    wlSubmitLoading.value = false
  }
}

async function handleWlDelete(record: SecMaskWhitelistVO) {
  if (!record.id) return
  try {
    const res = await deleteMaskWhitelist(record.id)
    if (res.code === 200) {
      message.success('删除成功')
      loadWhitelist()
    } else {
      message.error(res.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadData()
})
</script>

<style lang="less" scoped>
.mask-strategy-page { padding: 0; }

.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; background: linear-gradient(135deg, #FA541C10 0%, #FFD59110 100%);
  border: 1px solid #FA541C20; border-radius: 12px; padding: 20px 24px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1F1F1F; line-height: 1.3; }
.page-subtitle { margin: 4px 0 0; font-size: 13px; color: #8C8C8C; }
.header-right { display: flex; align-items: center; gap: 8px; }

.stat-row { margin-bottom: 16px; }
.stat-card {
  border-radius: 12px; padding: 16px; color: #fff; min-height: 88px;
  display: flex; flex-direction: column; justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: transform 0.2s, box-shadow 0.2s; cursor: default;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; opacity: 0.88; margin-top: 4px; }

.table-card { background: #fff; border-radius: 10px; padding: 16px; box-shadow: 0 1px 2px rgba(0,0,0,0.03); }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.toolbar-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }

.strategy-name-cell { display: flex; flex-direction: column; gap: 2px; }
.strategy-name-row { display: flex; align-items: center; flex-wrap: wrap; gap: 2px; }
.strategy-name-text { font-weight: 600; color: #1F1F1F; font-size: 13px; }
.strategy-code-text { font-size: 11px; color: #8C8C8C; font-family: 'JetBrains Mono', monospace; }
.level-mapping-cell { display: flex; flex-direction: column; gap: 2px; }

.scene-option { display: flex; align-items: center; gap: 8px; }
.scene-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; line-height: 1.5; }

.divider-title { font-weight: 600; font-size: 14px; color: #FA541C; display: flex; align-items: center; gap: 6px; }

/* 等级映射面板 */
.level-mapping-panel {
  border: 1px solid #F0F0F0; border-radius: 8px; padding: 16px;
  background: #FAFAFA; margin-top: -8px;
}

.mapping-tip { font-size: 12px; color: #8C8C8C; margin-bottom: 16px; }

.mapping-rule-item {
  display: flex; align-items: center; gap: 12px; margin-bottom: 12px;
  padding: 10px 12px; background: #fff; border: 1px solid #E8E8E8; border-radius: 6px;
}

.mapping-rule-header { display: flex; align-items: center; gap: 8px; min-width: 120px; }
.level-badge {
  width: 32px; height: 32px; border-radius: 50%; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700;
}
.level-label { font-size: 12px; color: #595959; }

.mapping-rule-body { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* 预览 */
.mapping-preview { margin-top: 16px; padding-top: 16px; border-top: 1px dashed #E8E8E8; }
.preview-title { font-size: 12px; font-weight: 600; color: #595959; margin-bottom: 8px; }
.preview-table { border: 1px solid #E8E8E8; border-radius: 6px; overflow: hidden; }
.preview-row { display: flex; border-bottom: 1px solid #F0F0F0; }
.preview-row:last-child { border-bottom: none; }
.preview-row.preview-header { background: #FAFAFA; }
.preview-cell { flex: 1; padding: 6px 10px; font-size: 12px; color: #595959; }
.preview-cell.preview-input { color: #8C8C8C; font-family: 'JetBrains Mono', monospace; }
.preview-cell.preview-result { color: #722ED1; font-family: 'JetBrains Mono', monospace; font-weight: 500; }

/* 策略摘要 */
.strategy-summary {
  padding: 12px 16px; background: linear-gradient(135deg, #FA541C10 0%, #fff 100%);
  border: 1px solid #FA541C20; border-radius: 8px; margin-bottom: 16px;
}
.summary-tags { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.summary-desc { font-size: 13px; color: #8C8C8C; }

.validity-cell { display: flex; align-items: center; gap: 2px; font-size: 12px; }

/* 冲突列表 */
.conflict-list { max-height: 400px; overflow-y: auto; }
.conflict-item { padding: 12px; border-bottom: 1px solid #F0F0F0; }
.conflict-item:last-child { border-bottom: none; }
.conflict-field code { font-size: 13px; color: #722ED1; background: #722ED10A; padding: 2px 6px; border-radius: 4px; }
.conflict-desc { font-size: 12px; color: #8C8C8C; margin-top: 4px; }

.table-empty { padding: 48px 0; text-align: center; }
.empty-icon { font-size: 48px; color: #d9d9d9; margin-bottom: 12px; display: block; }
.empty-title { font-size: 16px; color: #595959; margin-bottom: 8px; }
.empty-desc { font-size: 13px; color: #8C8C8C; margin-bottom: 16px; }
</style>
