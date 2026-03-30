<template>
  <div class="sensitivity-rules-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#ruleGrad)"/>
            <path d="M12 2L4 6v6c0 5.55 3.84 10.74 8 12 4.16-1.26 8-6.45 8-12V6l-8-4z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M9 12l2 2 4-4" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="ruleGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#B37FEB"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">识别规则管理</h1>
          <p class="page-subtitle">配置和管理敏感字段识别规则，支持内置算法匹配与规则测试预览</p>
        </div>
      </div>
      <div class="header-right">
        <a-button @click="loadData" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%)">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">规则总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #FA541C 0%, #FFBB96 100%)">
          <div class="stat-value">{{ stats.builtin || 0 }}</div>
          <div class="stat-label">内置规则</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #13C2C2 0%, #36CBCB 100%)">
          <div class="stat-value">{{ stats.custom || 0 }}</div>
          <div class="stat-label">自定义规则</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
          <div class="stat-value">{{ stats.enabled || 0 }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #FA8C16 0%, #FFD591 100%)">
          <div class="stat-value">{{ stats.algorithmCount || 0 }}</div>
          <div class="stat-label">内容算法</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #F5222D 0%, #FF7875 100%)">
          <div class="stat-value">{{ stats.regexCount || 0 }}</div>
          <div class="stat-label">正则规则</div>
        </div>
      </a-col>
    </a-row>

    <!-- 主内容卡片 -->
    <div class="table-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <a-input
            v-model:value="filterParams.keyword"
            placeholder="搜索规则名称/编码"
            style="width: 200px"
            allowClear
            @pressEnter="handleSearch"
            @change="debouncedSearch"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-select
            v-model:value="filterParams.matchType"
            placeholder="匹配类型"
            style="width: 140px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option value="COLUMN_NAME">
              <span><TagOutlined style="color: #1677FF" /> 字段名</span>
            </a-select-option>
            <a-select-option value="COLUMN_COMMENT">
              <span><FileTextOutlined style="color: #722ED1" /> 注释匹配</span>
            </a-select-option>
            <a-select-option value="DATA_TYPE">
              <span><DatabaseOutlined style="color: #13C2C2" /> 数据类型</span>
            </a-select-option>
            <a-select-option value="CONTENT_REGEX">
              <span><ThunderboltOutlined style="color: #FA541C" /> 内容正则</span>
            </a-select-option>
            <a-select-option value="CONTENT_ALGORITHM">
              <span><BugOutlined style="color: #FA8C16" /> 内容算法</span>
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterParams.builtin"
            placeholder="内置/自定义"
            style="width: 130px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option :value="1">
              <a-tag color="gold" style="margin-right: 4px">内置</a-tag>
            </a-select-option>
            <a-select-option :value="0">
              <a-tag color="cyan" style="margin-right: 4px">自定义</a-tag>
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterParams.enabled"
            placeholder="启用状态"
            style="width: 110px"
            allowClear
            @change="handleSearch"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </div>
        <div class="toolbar-right">
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增规则
          </a-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="paginationConfig"
        :scroll="{ x: 1300 }"
        :row-key="(r: SecSensitivityRuleVO) => r.id || r.ruleCode"
        @change="handleTableChange"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <!-- 规则名称 -->
          <template v-if="column.key === 'ruleName'">
            <div class="rule-name-cell">
              <div class="rule-name-row">
                <a-tag v-if="record.builtin === 1" color="gold" style="margin-right: 4px; font-size: 11px">内置</a-tag>
                <a-tooltip :title="record.description || record.ruleName">
                  <span class="rule-name-text">{{ record.ruleName }}</span>
                </a-tooltip>
              </div>
              <div class="rule-code-text">{{ record.ruleCode }}</div>
            </div>
          </template>

          <!-- 匹配类型 -->
          <template v-if="column.key === 'matchTypeLabel'">
            <a-tag :color="getMatchTypeColor(record.matchType)" style="font-weight: 500">
              {{ record.matchTypeLabel || record.matchType }}
            </a-tag>
          </template>

          <!-- 匹配表达式 -->
          <template v-if="column.key === 'matchExpr'">
            <div class="expr-cell">
              <a-tooltip :title="record.matchExpr">
                <code class="match-expr">{{ truncateText(record.matchExpr, 35) }}</code>
              </a-tooltip>
              <a-tag v-if="record.builtinAlgorithm" color="orange" style="font-size: 10px; margin-left: 4px">
                {{ record.builtinAlgorithmLabel || record.builtinAlgorithm }}
              </a-tag>
            </div>
          </template>

          <!-- 默认等级 -->
          <template v-if="column.key === 'levelName'">
            <a-tag v-if="record.levelName" :color="record.levelColor || getLevelColor(record.levelName)" style="font-weight: 600">
              {{ record.levelName }}
            </a-tag>
            <span v-else style="color: #d9d9d9">—</span>
          </template>

          <!-- 分类 -->
          <template v-if="column.key === 'className'">
            <span style="color: #595959">{{ record.className || '-' }}</span>
          </template>

          <!-- 优先级 -->
          <template v-if="column.key === 'priority'">
            <span style="color: #8C8C8C; font-size: 13px">P{{ record.priority || 0 }}</span>
          </template>

          <!-- 启用状态 -->
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :loading="(record as any).switchLoading"
              checked-children="启用"
              un-checked-children="禁用"
              size="small"
              @change="(checked: boolean) => handleToggleStatus(record, checked)"
            />
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-tooltip title="测试规则">
                <a-button type="text" size="small" @click="openTestDialog(record)">
                  <template #icon><ThunderboltOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="text" size="small" @click="handleEdit(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-popconfirm
                v-if="record.builtin !== 1"
                title="确定删除该规则？删除后不影响已扫描的敏感字段记录。"
                @confirm="handleDelete(record)"
              >
                <a-tooltip title="删除">
                  <a-button type="text" size="small" danger>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </a-space>
          </template>
        </template>

        <!-- 空状态 -->
        <template #emptyText>
          <div class="table-empty">
            <SafetyOutlined class="empty-icon" />
            <div class="empty-title">暂无识别规则</div>
            <div class="empty-desc">点击「新增规则」创建第一条敏感字段识别规则</div>
            <a-button type="primary" @click="handleAdd">新增规则</a-button>
          </div>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑规则抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑识别规则' : '新增识别规则'"
      :width="640"
      :mask-closable="false"
      @close="handleDrawerClose"
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
          <a-form-item label="规则名称" name="ruleName">
            <a-input
              v-model:value="formData.ruleName"
              placeholder="请输入规则名称，如：手机号识别"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="规则编码" name="ruleCode">
            <a-input
              v-model:value="formData.ruleCode"
              placeholder="唯一标识，如：PHONE_NUMBER_RULE"
              :disabled="isEdit"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="匹配类型" name="matchType">
            <a-select
              v-model:value="formData.matchType"
              placeholder="请选择匹配维度"
              @change="handleMatchTypeChange"
            >
              <a-select-option value="COLUMN_NAME">
                <span><TagOutlined /> 字段名匹配</span>
              </a-select-option>
              <a-select-option value="COLUMN_COMMENT">
                <span><FileTextOutlined /> 注释匹配</span>
              </a-select-option>
              <a-select-option value="DATA_TYPE">
                <span><DatabaseOutlined /> 数据类型匹配</span>
              </a-select-option>
              <a-select-option value="CONTENT_REGEX">
                <span><ThunderboltOutlined /> 内容正则（需要开启内容采样）</span>
              </a-select-option>
              <a-select-option value="CONTENT_ALGORITHM">
                <span><BugOutlined /> 内容算法（内置校验算法）</span>
              </a-select-option>
            </a-select>
          </a-form-item>

          <!-- 内容算法选择（仅匹配类型为 CONTENT_ALGORITHM 时显示） -->
          <a-form-item
            v-if="formData.matchType === 'CONTENT_ALGORITHM'"
            label="内置算法"
            name="builtinAlgorithm"
          >
            <a-select
              v-model:value="formData.builtinAlgorithm"
              placeholder="请选择内置算法"
              @change="handleBuiltinAlgorithmChange"
            >
              <a-select-option value="PHONE_VALIDATE">
                <div class="builtin-algo-option">
                  <span class="algo-name">手机号合法性校验</span>
                  <span class="algo-desc">校验中国手机号格式（1[3-9]开头，11位）</span>
                </div>
              </a-select-option>
              <a-select-option value="IDCARD_VALIDATE">
                <div class="builtin-algo-option">
                  <span class="algo-name">身份证合法性校验</span>
                  <span class="algo-desc">18位身份证 + 校验位算法（加权求和 mod 11）</span>
                </div>
              </a-select-option>
              <a-select-option value="EMAIL_VALIDATE">
                <div class="builtin-algo-option">
                  <span class="algo-name">邮箱格式校验</span>
                  <span class="algo-desc">标准邮箱格式正则（user@domain.com）</span>
                </div>
              </a-select-option>
              <a-select-option value="BANKCARD_VALIDATE">
                <div class="builtin-algo-option">
                  <span class="algo-name">银行卡Luhn校验</span>
                  <span class="algo-desc">15-19位银行卡 + Luhn 算法校验</span>
                </div>
              </a-select-option>
              <a-select-option value="IP_VALIDATE">
                <div class="builtin-algo-option">
                  <span class="algo-name">IP地址校验</span>
                  <span class="algo-desc">IPv4/IPv6 格式合法性校验</span>
                </div>
              </a-select-option>
              <a-select-option value="URL_VALIDATE">
                <div class="builtin-algo-option">
                  <span class="algo-name">URL校验</span>
                  <span class="algo-desc">HTTP/HTTPS URL 格式合法性校验</span>
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>

          <!-- 表达式类型（CONTENT_REGEX 时显示） -->
          <a-form-item
            v-if="formData.matchType === 'CONTENT_REGEX' || formData.matchType === 'COLUMN_NAME' || formData.matchType === 'COLUMN_COMMENT'"
            label="表达式类型"
            name="matchExprType"
          >
            <a-select v-model:value="formData.matchExprType" placeholder="请选择表达式类型">
              <a-select-option value="CONTAINS">包含</a-select-option>
              <a-select-option value="EQUALS">等于</a-select-option>
              <a-select-option value="STARTS_WITH">开头匹配</a-select-option>
              <a-select-option value="REGEX">正则表达式</a-select-option>
            </a-select>
          </a-form-item>

          <!-- 匹配表达式 -->
          <a-form-item
            v-if="formData.matchType !== 'CONTENT_ALGORITHM'"
            label="匹配表达式"
            name="matchExpr"
          >
            <a-textarea
              v-model:value="formData.matchExpr"
              placeholder="请输入匹配表达式"
              :rows="2"
              :maxlength="500"
              show-count
            />
            <div class="form-help">
              <template v-if="formData.matchType === 'COLUMN_NAME'">
                支持多个关键词，用逗号分隔。如：id_card, identity, 身份证, idnumber
              </template>
              <template v-else-if="formData.matchType === 'CONTENT_REGEX'">
                支持正则表达式（需要开启内容级扫描）。如：<code>^1[3-9]\d{9}$</code>
              </template>
              <template v-else-if="formData.matchType === 'COLUMN_COMMENT'">
                支持多个关键词，用逗号分隔。如：手机号, 电话号码, phone_no
              </template>
              <template v-else-if="formData.matchType === 'DATA_TYPE'">
                支持多个类型，用逗号分隔。如：VARCHAR, TEXT, CHAR
              </template>
            </div>
          </a-form-item>

          <a-divider orientation="left">敏感分级配置</a-divider>

          <a-form-item label="所属分类" name="classId">
            <a-select
              v-model:value="formData.classId"
              placeholder="请选择敏感分类"
              show-search
              :filter-option="(input: string, option: any) =>
                option.children?.props?.children?.toLowerCase().includes(input.toLowerCase())"
            >
              <a-select-option v-for="cls in classificationList" :key="cls.id" :value="cls.id">
                {{ cls.className }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="推荐等级" name="levelId">
            <a-select
              v-model:value="formData.levelId"
              placeholder="请选择敏感等级"
            >
              <a-select-option v-for="lv in levelList" :key="lv.id" :value="lv.id">
                <div style="display: flex; align-items: center; gap: 8px">
                  <div
                    style="width: 12px; height: 12px; border-radius: 3px"
                    :style="{ background: lv.color }"
                  ></div>
                  <span>{{ lv.levelCode }} - {{ lv.levelName }}</span>
                </div>
              </a-select-option>
            </a-select>
            <div class="form-help" v-if="formData.levelId">
              推荐等级影响扫描结果的默认分级，审核时可调整
            </div>
          </a-form-item>

          <a-form-item label="推荐脱敏" name="suggestionMaskType">
            <a-select
              v-model:value="formData.suggestionMaskType"
              placeholder="推荐脱敏方式（选填）"
              allowClear
            >
              <a-select-option value="MASK">部分掩码（138****5678）</a-select-option>
              <a-select-option value="HIDE">完全隐藏（***）</a-select-option>
              <a-select-option value="ENCRYPT">加密存储</a-select-option>
              <a-select-option value="DELETE">删除</a-select-option>
              <a-select-option value="HASH">哈希脱敏</a-select-option>
              <a-select-option value="FORMAT_KEEP">保留格式加密</a-select-option>
              <a-select-option value="RANGE">区间化</a-select-option>
            </a-select>
          </a-form-item>

          <a-divider orientation="left">高级配置</a-divider>

          <a-form-item label="优先级" name="priority">
            <a-input-number
              v-model:value="formData.priority"
              :min="1"
              :max="999"
              style="width: 100%"
              placeholder="数值越小优先级越高"
            />
            <div class="form-help">用于多个规则同时匹配时的优先级排序，默认100</div>
          </a-form-item>

          <a-form-item label="是否启用" name="status">
            <a-switch
              v-model:checked="formDataStatus"
              checked-children="启用"
              un-checked-children="禁用"
            />
          </a-form-item>

          <a-form-item label="规则描述" name="description">
            <a-textarea
              v-model:value="formData.description"
              placeholder="请输入规则描述，说明规则的适用范围和管理要求"
              :rows="3"
              :maxlength="200"
              show-count
            />
          </a-form-item>
        </a-form>

        <!-- 规则测试区域 -->
        <a-divider orientation="left">
          <span class="test-title">
            <ThunderboltOutlined style="color: #FA541C; margin-right: 4px" />
            规则测试
          </span>
        </a-divider>

        <div class="test-section">
          <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
            <a-form-item label="测试内容">
              <a-input
                v-model:value="testInput"
                :placeholder="getTestPlaceholder()"
                allowClear
                @pressEnter="handleTestRule"
              />
            </a-form-item>
            <a-form-item label="测试结果">
              <div class="test-result-area">
                <template v-if="testLoading">
                  <LoadingOutlined style="font-size: 16px; color: #8C8C8C" />
                  <span style="color: #8C8C8C; margin-left: 8px">正在测试...</span>
                </template>
                <template v-else-if="testResult === true">
                  <a-result
                    status="success"
                    title="匹配成功"
                    :sub-title="`该内容符合规则「${formData.ruleName}」，推荐等级：${testMatchedLevel || '-'}`"
                    style="padding: 8px 0"
                  >
                    <template #extra>
                      <a-tag v-if="testMatchedLevel" :color="getLevelColor(testMatchedLevel)" style="font-weight: 600">
                        {{ testMatchedLevel }}
                      </a-tag>
                      <a-tag v-if="testMatchedMaskType" :color="getMaskTypeColor(testMatchedMaskType)">
                        {{ testMatchedMaskType }}
                      </a-tag>
                    </template>
                  </a-result>
                </template>
                <template v-else-if="testResult === false">
                  <a-result
                    status="error"
                    title="匹配失败"
                    sub-title="该内容不符合当前规则的匹配条件"
                    style="padding: 8px 0"
                  />
                </template>
                <template v-else>
                  <div class="test-placeholder">
                    <InfoCircleOutlined style="color: #8C8C8C; margin-right: 4px" />
                    输入测试内容后点击「测试匹配」，查看规则匹配结果
                  </div>
                </template>
              </div>
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4 }">
              <a-space>
                <a-button type="primary" :loading="testLoading" @click="handleTestRule">
                  <template #icon><ThunderboltOutlined /></template>
                  测试匹配
                </a-button>
                <a-button @click="clearTest">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>

          <!-- 内置算法测试示例 -->
          <div v-if="formData.matchType === 'CONTENT_ALGORITHM' && formData.builtinAlgorithm" class="algo-test-examples">
            <div class="algo-test-title">算法测试示例</div>
            <div class="algo-test-examples">
              <div
                v-for="(example, idx) in currentAlgorithmExamples"
                :key="idx"
                class="algo-example-item"
                :class="{ 'example-match': testExample(example.value, true) }"
                @click="testInput = example.value; handleTestRule()"
              >
                <code>{{ example.value }}</code>
                <span class="example-label">{{ example.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <a-space>
          <a-button @click="handleDrawerClose">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建规则' }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 独立测试弹窗（从列表页点击测试按钮触发） -->
    <a-modal
      v-model:open="testDialogVisible"
      :title="`测试规则 — ${testRuleRecord?.ruleName || ''}`"
      :width="560"
      :footer="null"
      @cancel="testDialogVisible = false"
    >
      <template v-if="testRuleRecord">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="规则编码">{{ testRuleRecord.ruleCode }}</a-descriptions-item>
          <a-descriptions-item label="匹配类型">
            <a-tag :color="getMatchTypeColor(testRuleRecord.matchType)">
              {{ testRuleRecord.matchTypeLabel || testRuleRecord.matchType }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="推荐等级">
            <a-tag v-if="testRuleRecord.levelName" :color="testRuleRecord.levelColor || getLevelColor(testRuleRecord.levelName)">
              {{ testRuleRecord.levelName }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="匹配表达式" :span="2">
            <code style="font-size: 12px; color: #722ED1">{{ testRuleRecord.matchExpr }}</code>
          </a-descriptions-item>
        </a-descriptions>

        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item label="输入内容">
            <a-input
              v-model:value="testInput"
              placeholder="请输入要测试的字段名或内容"
              allowClear
              @pressEnter="handleTestRule"
            />
          </a-form-item>
          <a-form-item label="测试结果">
            <div class="test-result-area">
              <template v-if="testLoading">
                <LoadingOutlined style="font-size: 16px" />
                <span style="color: #8C8C8C; margin-left: 8px">正在测试...</span>
              </template>
              <template v-else-if="testResult === true">
                <a-alert
                  message="匹配成功"
                  :description="`该内容符合规则，推荐等级：${testMatchedLevel || '-'}`"
                  type="success"
                  show-icon
                />
              </template>
              <template v-else-if="testResult === false">
                <a-alert
                  message="匹配失败"
                  description="该内容不符合当前规则的匹配条件"
                  type="error"
                  show-icon
                />
              </template>
              <template v-else>
                <div style="color: #8C8C8C; font-size: 13px">
                  <InfoCircleOutlined style="margin-right: 4px" />
                  输入测试内容后点击「测试匹配」
                </div>
              </template>
            </div>
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 4 }">
            <a-space>
              <a-button type="primary" :loading="testLoading" @click="handleTestRule">
                <template #icon><ThunderboltOutlined /></template>
                测试匹配
              </a-button>
              <a-button @click="clearTest">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
  SearchOutlined, SafetyOutlined, ThunderboltOutlined,
  TagOutlined, FileTextOutlined, DatabaseOutlined,
  BugOutlined, LoadingOutlined, InfoCircleOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  pageRule,
  saveRule,
  updateRule,
  deleteRule,
  enableRule,
  disableRule,
  listClassification,
  listLevel,
  type SecSensitivityRuleVO,
  type SecSensitivityRuleSaveDTO
} from '@/api/security'
import type { MatchType, ExprType } from '@/api/security'

// ==================== 内置算法配置 ====================
const BUILTIN_ALGORITHMS: Record<string, {
  label: string
  examples: { value: string; label: string }[]
}> = {
  PHONE_VALIDATE: {
    label: '手机号校验',
    examples: [
      { value: '13812345678', label: '正确' },
      { value: '13800138000', label: '正确' },
      { value: '12345678901', label: '错误（开头不正确）' },
      { value: '138123456789', label: '错误（超过11位）' }
    ]
  },
  IDCARD_VALIDATE: {
    label: '身份证校验',
    examples: [
      { value: '110101199003074515', label: '正确' },
      { value: '11010119900307451X', label: '正确（X大写）' },
      { value: '11010119900307451x', label: '正确（x小写）' },
      { value: '123456789012345678', label: '错误（校验位不对）' }
    ]
  },
  EMAIL_VALIDATE: {
    label: '邮箱校验',
    examples: [
      { value: 'user@example.com', label: '正确' },
      { value: 'test.user@company.cn', label: '正确' },
      { value: 'invalid-email', label: '错误' },
      { value: '@nodomain.com', label: '错误' }
    ]
  },
  BANKCARD_VALIDATE: {
    label: '银行卡Luhn校验',
    examples: [
      { value: '6222021234567890123', label: '正确' },
      { value: '6212261234567890128', label: '正确' },
      { value: '1234567890123456789', label: '错误（Luhn校验失败）' }
    ]
  },
  IP_VALIDATE: {
    label: 'IP地址校验',
    examples: [
      { value: '192.168.1.1', label: 'IPv4正确' },
      { value: '10.0.0.255', label: 'IPv4正确' },
      { value: '256.1.1.1', label: '错误（超过255）' },
      { value: '::1', label: 'IPv6正确' }
    ]
  },
  URL_VALIDATE: {
    label: 'URL校验',
    examples: [
      { value: 'https://www.example.com/path', label: '正确' },
      { value: 'http://api.example.com:8080', label: '正确' },
      { value: 'ftp://files.example.com', label: '正确' },
      { value: 'not-a-url', label: '错误' }
    ]
  }
}

// ==================== 状态 ====================
const loading = ref(false)
const submitLoading = ref(false)
const testLoading = ref(false)
const testResult = ref<boolean | null>(null)
const testMatchedLevel = ref('')
const testMatchedMaskType = ref('')

const tableData = ref<SecSensitivityRuleVO[]>([])

const stats = reactive({
  total: 0,
  builtin: 0,
  custom: 0,
  enabled: 0,
  algorithmCount: 0,
  regexCount: 0
})

const paginationConfig = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
})

// 筛选参数
const filterParams = reactive({
  keyword: '',
  matchType: undefined as MatchType | undefined,
  builtin: undefined as number | undefined,
  enabled: undefined as number | undefined
})

// 下拉选项
const classificationList = ref<{ id: number; className: string }[]>([])
const levelList = ref<{ id: number; levelCode: string; levelName: string; color: string }[]>([])

// 抽屉
const drawerVisible = ref(false)
const isEdit = ref(false)
const currentRuleId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const formDataStatus = ref(true)

const formData = reactive<SecSensitivityRuleSaveDTO & { builtinAlgorithm?: string }>({
  ruleName: '',
  ruleCode: '',
  matchType: undefined,
  builtinAlgorithm: undefined,
  matchExprType: 'CONTAINS',
  matchExpr: '',
  classId: undefined,
  levelId: undefined,
  suggestionMaskType: undefined,
  priority: 100,
  status: 1,
  description: ''
})

const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  matchType: [{ required: true, message: '请选择匹配类型', trigger: 'change' }],
  matchExpr: [{ required: true, message: '请输入匹配表达式', trigger: 'blur' }],
  classId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  levelId: [{ required: true, message: '请选择推荐等级', trigger: 'change' }]
}

// 测试相关
const testInput = ref('')
const testDialogVisible = ref(false)
const testRuleRecord = ref<SecSensitivityRuleVO | null>(null)

// ==================== 表格列 ====================
const columns = [
  { title: '规则名称', key: 'ruleName', width: 220 },
  { title: '匹配类型', key: 'matchTypeLabel', width: 130, align: 'center' as const },
  { title: '匹配表达式', key: 'matchExpr', width: 280 },
  { title: '推荐等级', key: 'levelName', width: 100, align: 'center' as const },
  { title: '所属分类', key: 'className', width: 110 },
  { title: '优先级', key: 'priority', width: 80, align: 'center' as const },
  { title: '启用', key: 'status', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 130, fixed: 'right' as const }
]

// ==================== 内置算法示例 ====================
const currentAlgorithmExamples = computed(() => {
  const algo = formData.builtinAlgorithm || testRuleRecord.value?.builtinAlgorithm
  if (!algo || !BUILTIN_ALGORITHMS[algo]) return []
  return BUILTIN_ALGORITHMS[algo].examples
})

// ==================== 工具函数 ====================
function getMatchTypeColor(matchType?: string) {
  const colorMap: Record<string, string> = {
    COLUMN_NAME: '#1677FF',
    COLUMN_COMMENT: '#722ED1',
    DATA_TYPE: '#13C2C2',
    CONTENT_REGEX: '#FA541C',
    CONTENT_ALGORITHM: '#FA8C16',
    REGEX: '#FA541C'
  }
  return colorMap[matchType || ''] || 'default'
}

function getLevelColor(level?: string) {
  const map: Record<string, string> = {
    'L4': '#F5222D', 'L3': '#FA8C16', 'L2': '#1677FF', 'L1': '#52C41A',
    'HIGHLY_SENSITIVE': '#F5222D', 'SENSITIVE': '#FA8C16',
    'INTERNAL': '#1677FF', 'NORMAL': '#52C41A'
  }
  return map[level || ''] || 'default'
}

function getMaskTypeColor(maskType?: string) {
  const map: Record<string, string> = {
    NONE: 'default', MASK: 'blue', HIDE: 'orange',
    ENCRYPT: 'red', DELETE: 'purple', PARTIAL_MASK: 'cyan',
    HASH: 'purple', RANDOM_REPLACE: 'cyan', RANGE: 'orange', FORMAT_KEEP: 'cyan'
  }
  return map[maskType || ''] || 'default'
}

function truncateText(text: string | undefined, maxLength: number = 50): string {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

function getTestPlaceholder(): string {
  if (formData.matchType === 'CONTENT_ALGORITHM') {
    const algo = formData.builtinAlgorithm
    if (algo === 'PHONE_VALIDATE') return '请输入手机号，如：13812345678'
    if (algo === 'IDCARD_VALIDATE') return '请输入身份证号，如：110101199003074515'
    if (algo === 'EMAIL_VALIDATE') return '请输入邮箱地址，如：user@example.com'
    if (algo === 'BANKCARD_VALIDATE') return '请输入银行卡号'
    if (algo === 'IP_VALIDATE') return '请输入IP地址，如：192.168.1.1'
    if (algo === 'URL_VALIDATE') return '请输入URL，如：https://www.example.com'
  }
  switch (formData.matchType) {
    case 'COLUMN_NAME': return '请输入字段名进行测试，如：id_card、phone_no'
    case 'COLUMN_COMMENT': return '请输入字段注释进行测试，如：身份证号'
    case 'CONTENT_REGEX': return '请输入内容进行正则匹配测试'
    case 'DATA_TYPE': return '请输入数据类型进行测试，如：VARCHAR'
    default: return '请输入测试内容'
  }
}

// ==================== 内置算法执行 ====================
function runBuiltinAlgorithm(algorithm: string, input: string): boolean {
  const v = input.trim()
  switch (algorithm) {
    case 'PHONE_VALIDATE': {
      return /^(?:(?:\+|00)86)?1[3-9]\d{9}$/.test(v)
    }
    case 'IDCARD_VALIDATE': {
      if (!/^\d{17}[\dXx]$/.test(v)) return false
      const factors = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
      const checkCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
      let sum = 0
      for (let i = 0; i < 17; i++) {
        sum += parseInt(v[i]) * factors[i]
      }
      const checkChar = checkCodes[sum % 11]
      return v[17].toUpperCase() === checkChar
    }
    case 'EMAIL_VALIDATE': {
      return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(v)
    }
    case 'BANKCARD_VALIDATE': {
      if (!/^\d{15,19}$/.test(v)) return false
      let sum = 0
      let isEven = false
      for (let i = v.length - 1; i >= 0; i--) {
        let digit = parseInt(v[i])
        if (isEven) {
          digit *= 2
          if (digit > 9) digit -= 9
        }
        sum += digit
        isEven = !isEven
      }
      return sum % 10 === 0
    }
    case 'IP_VALIDATE': {
      if (/^(\d{1,3}\.){3}\d{1,3}$/.test(v)) {
        return v.split('.').every(part => parseInt(part) <= 255)
      }
      return /^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$/.test(v)
    }
    case 'URL_VALIDATE': {
      return /^(https?|ftp):\/\/[^\s/$.?#].[^\s]*$/i.test(v)
    }
    default:
      return false
  }
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const res = await pageRule({
      pageNum: paginationConfig.current,
      pageSize: paginationConfig.pageSize,
      matchType: filterParams.matchType,
      enabled: filterParams.enabled
    })

    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      tableData.value = records.map(item => ({ ...item, switchLoading: false }))
      paginationConfig.total = res.data.total || 0

      stats.total = res.data.total || 0
      stats.builtin = records.filter(r => r.builtin === 1).length
      stats.custom = records.filter(r => r.builtin !== 1).length
      stats.enabled = records.filter(r => r.status === 1).length
      stats.algorithmCount = records.filter(r => r.matchType === 'CONTENT_ALGORITHM').length
      stats.regexCount = records.filter(r => r.matchType === 'CONTENT_REGEX' || r.matchType === 'REGEX').length
    }
  } catch (error) {
    message.error('加载规则数据失败')
  } finally {
    loading.value = false
  }
}

async function loadFilterOptions() {
  try {
    const [classRes, levelRes] = await Promise.all([
      listClassification(1),
      listLevel()
    ])
    if (classRes.code === 200 && classRes.data) {
      classificationList.value = classRes.data
    }
    if (levelRes.code === 200 && levelRes.data) {
      levelList.value = levelRes.data
    }
  } catch (error) {
    console.error('加载筛选选项失败:', error)
  }
}

// ==================== 事件处理 ====================
let searchTimer: any = null
function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    paginationConfig.current = 1
    loadData()
  }, 300)
}

function handleSearch() {
  paginationConfig.current = 1
  loadData()
}

function handleReset() {
  filterParams.keyword = ''
  filterParams.matchType = undefined
  filterParams.builtin = undefined
  filterParams.enabled = undefined
  paginationConfig.current = 1
  loadData()
}

function handleTableChange(pag: any) {
  paginationConfig.current = pag.current
  paginationConfig.pageSize = pag.pageSize
  loadData()
}

function handleAdd() {
  isEdit.value = false
  currentRuleId.value = undefined
  resetForm()
  drawerVisible.value = true
}

function handleEdit(record: SecSensitivityRuleVO) {
  isEdit.value = true
  currentRuleId.value = record.id
  Object.assign(formData, {
    ruleName: record.ruleName,
    ruleCode: record.ruleCode,
    matchType: record.matchType as MatchType,
    builtinAlgorithm: (record as any).builtinAlgorithm,
    matchExprType: (record.matchExprType as ExprType) || 'CONTAINS',
    matchExpr: record.matchExpr,
    classId: record.classId,
    levelId: record.levelId,
    suggestionMaskType: (record as any).suggestionMaskType,
    priority: record.priority || 100,
    status: record.status,
    description: record.description
  })
  formDataStatus.value = record.status === 1
  drawerVisible.value = true
}

function handleDrawerClose() {
  drawerVisible.value = false
  resetForm()
  clearTest()
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(formData, {
    ruleName: '',
    ruleCode: '',
    matchType: undefined,
    builtinAlgorithm: undefined,
    matchExprType: 'CONTAINS',
    matchExpr: '',
    classId: undefined,
    levelId: undefined,
    suggestionMaskType: undefined,
    priority: 100,
    status: 1,
    description: ''
  })
  formDataStatus.value = true
}

function handleMatchTypeChange() {
  testResult.value = null
  formData.matchExpr = ''
  formData.builtinAlgorithm = undefined
}

function handleBuiltinAlgorithmChange() {
  testResult.value = null
  // 自动填入算法标签
  const algo = formData.builtinAlgorithm
  if (algo && BUILTIN_ALGORITHMS[algo]) {
    formData.matchExpr = `内置算法: ${BUILTIN_ALGORITHMS[algo].label}`
  }
}

// ==================== 规则测试 ====================
function openTestDialog(record: SecSensitivityRuleVO) {
  testRuleRecord.value = record
  testInput.value = ''
  testResult.value = null
  testMatchedLevel.value = ''
  testMatchedMaskType.value = ''
  testDialogVisible.value = true
}

function clearTest() {
  testInput.value = ''
  testResult.value = null
  testMatchedLevel.value = ''
  testMatchedMaskType.value = ''
}

function testExample(value: string, _validate: boolean): boolean {
  const algo = formData.builtinAlgorithm || testRuleRecord.value?.builtinAlgorithm
  if (!algo) return false
  return runBuiltinAlgorithm(algo, value)
}

async function handleTestRule() {
  if (!testInput.value.trim()) {
    message.warning('请输入测试内容')
    return
  }

  // 前端模拟测试（实际生产应调用后端 /gov/security/rule/test 接口）
  testLoading.value = true
  testResult.value = null

  try {
    const currentAlgo = formData.builtinAlgorithm || testRuleRecord.value?.builtinAlgorithm
    let matched = false

    if (currentAlgo) {
      // 内置算法测试
      matched = runBuiltinAlgorithm(currentAlgo, testInput.value)
    } else {
      // 基于表单/记录中的规则进行前端模拟测试
      const matchType = formData.matchType || testRuleRecord.value?.matchType
      const exprType = (formData.matchExprType || testRuleRecord.value?.matchExprType) as ExprType || 'CONTAINS'
      const matchExpr = formData.matchExpr || testRuleRecord.value?.matchExpr || ''
      const input = testInput.value.trim().toLowerCase()

      if (matchType === 'COLUMN_NAME' || matchType === 'COLUMN_COMMENT' || matchType === 'DATA_TYPE') {
        const keywords = matchExpr.split(',').map(k => k.trim().toLowerCase()).filter(Boolean)
        switch (exprType) {
          case 'CONTAINS': matched = keywords.some(k => input.includes(k)); break
          case 'EQUALS': matched = keywords.some(k => input === k); break
          case 'STARTS_WITH': matched = keywords.some(k => input.startsWith(k)); break
          case 'REGEX':
            try { matched = new RegExp(matchExpr, 'i').test(testInput.value) } catch { matched = false }
            break
          default: matched = keywords.some(k => input.includes(k))
        }
      } else if (matchType === 'CONTENT_REGEX') {
        try { matched = new RegExp(matchExpr).test(testInput.value) } catch { matched = false }
      }
    }

    testResult.value = matched

    if (matched) {
      // 提取推荐等级
      const levelName = formData.levelName || testRuleRecord.value?.levelName || ''
      testMatchedLevel.value = levelName
      testMatchedMaskType.value = formData.suggestionMaskType || (testRuleRecord.value as any)?.suggestionMaskType || ''
    }
  } finally {
    testLoading.value = false
  }
}

// ==================== 提交 ====================
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const submitData: SecSensitivityRuleSaveDTO = {
      ...formData,
      status: formDataStatus.value ? 1 : 0
    }

    let res
    if (isEdit.value && currentRuleId.value) {
      res = await updateRule(currentRuleId.value, submitData)
    } else {
      res = await saveRule(submitData)
    }

    if (res.code === 200) {
      message.success(isEdit.value ? '规则更新成功' : '规则创建成功')
      handleDrawerClose()
      loadData()
    } else {
      message.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(record: SecSensitivityRuleVO) {
  if (!record.id) return
  try {
    const res = await deleteRule(record.id)
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

async function handleToggleStatus(record: SecSensitivityRuleVO, checked: boolean) {
  if (!record.id) return

  const targetRow = tableData.value.find(r => r.id === record.id)
  if (targetRow) {
    (targetRow as any).switchLoading = true
  }

  try {
    const res = checked
      ? await enableRule(record.id)
      : await disableRule(record.id)

    if (res.code === 200) {
      message.success(`${checked ? '启用' : '禁用'}成功`)
      if (targetRow) {
        targetRow.status = checked ? 1 : 0
        stats.enabled += checked ? 1 : -1
      }
    } else {
      message.error(res.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  } finally {
    if (targetRow) {
      (targetRow as any).switchLoading = false
    }
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadFilterOptions()
  loadData()
})
</script>

<style scoped>
.sensitivity-rules-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #722ED110 0%, #B37FEB10 100%);
  border: 1px solid #722ED120;
  border-radius: 12px;
  padding: 20px 24px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1F1F1F;
  line-height: 1.3;
}
.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #8C8C8C;
}
.header-right { display: flex; align-items: center; gap: 8px; }

.stat-row { margin-bottom: 16px; }
.stat-card {
  border-radius: 12px;
  padding: 16px;
  color: #fff;
  min-height: 88px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 12px; opacity: 0.88; margin-top: 4px; }

.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }

.rule-name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.rule-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}
.rule-name-text {
  font-weight: 600;
  color: #1F1F1F;
  font-size: 13px;
  cursor: pointer;
}
.rule-name-text:hover { color: #722ED1; }
.rule-code-text {
  font-size: 11px;
  color: #8C8C8C;
  font-family: 'JetBrains Mono', monospace;
}

.expr-cell {
  display: flex;
  align-items: center;
  gap: 2px;
}
.match-expr {
  font-size: 12px;
  padding: 2px 6px;
  background: #F5F5F5;
  border-radius: 4px;
  color: #722ED1;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.builtin-algo-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.algo-name { font-weight: 600; }
.algo-desc { font-size: 11px; color: #8C8C8C; }

.form-help {
  margin-top: 4px;
  font-size: 12px;
  color: #8C8C8C;
  line-height: 1.5;
}
.form-help code {
  padding: 0 4px;
  background: #F5F5F5;
  border-radius: 3px;
  color: #722ED1;
  font-size: 11px;
}

.test-title {
  font-weight: 600;
  font-size: 14px;
  color: #FA541C;
}

.test-section {
  padding: 16px;
  background: #FAFAFA;
  border-radius: 8px;
  border: 1px solid #F0F0F0;
}

.test-result-area {
  min-height: 60px;
  display: flex;
  align-items: center;
}

.test-placeholder {
  color: #8C8C8C;
  font-size: 13px;
  display: flex;
  align-items: center;
}

.algo-test-examples {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #E8E8E8;
}
.algo-test-title {
  font-size: 13px;
  font-weight: 600;
  color: #595959;
  margin-bottom: 12px;
}
.algo-example-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #E8E8E8;
}
.algo-example-item:hover {
  border-color: #722ED1;
  background: #722ED10A;
}
.algo-example-item code {
  font-size: 12px;
  color: #722ED1;
  font-family: 'JetBrains Mono', monospace;
}
.algo-example-item .example-label {
  font-size: 12px;
  color: #8C8C8C;
}

/* 空状态 */
.table-empty {
  padding: 48px 0;
  text-align: center;
}
.empty-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 12px;
  display: block;
}
.empty-title { font-size: 16px; color: #595959; margin-bottom: 8px; }
.empty-desc { font-size: 13px; color: #8C8C8C; margin-bottom: 16px; }
</style>
