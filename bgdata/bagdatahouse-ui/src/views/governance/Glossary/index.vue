<template>
  <div class="page-container">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="6" fill="url(#glossaryGrad)"/>
            <path d="M8 7h8M8 12h8M8 17h5" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <path d="M19 12v5a2 2 0 01-2 2H7a2 2 0 01-2-2v-5" stroke="white" stroke-width="1.5" stroke-linecap="round" opacity="0.6"/>
            <defs>
              <linearGradient id="glossaryGrad" x1="0" y1="0" x2="24" y2="24">
                <stop offset="0%" stop-color="#722ED1"/>
                <stop offset="100%" stop-color="#9254DE"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="page-title">业务术语库</h1>
          <p class="page-subtitle">统一企业数据语言，管理业务术语定义，支持字段映射与搜索推荐</p>
        </div>
      </div>
      <div class="header-right">
        <!-- 全局搜索框 -->
        <a-input-search
          v-model:value="globalSearchKeyword"
          placeholder="搜索术语名称/编码/英文名/别名..."
          style="width: 260px; margin-right: 8px"
          allowClear
          @search="handleGlobalSearch"
          @input="handleSearchInput"
        >
          <template #prefix><SearchOutlined style="color: #8C8C8C" /></template>
        </a-input-search>
        <a-button @click="openImportModal">
          <template #icon><UploadOutlined /></template>
          批量导入
        </a-button>
        <a-button @click="handleExport">
          <template #icon><DownloadOutlined /></template>
          导出
        </a-button>
        <a-button type="primary" @click="openAddTermDrawer">
          <template #icon><PlusOutlined /></template>
          新建术语
        </a-button>
      </div>
    </div>

    <!-- 全局搜索结果悬浮面板 -->
    <div v-if="globalSearchVisible && globalSearchResults.length > 0" class="global-search-panel">
      <div class="search-panel-header">
        <span class="search-result-label">搜索结果 ({{ globalSearchResults.length }})</span>
        <a-button type="text" size="small" @click="globalSearchVisible = false">
          <template #icon><CloseOutlined /></template>
        </a-button>
      </div>
      <div class="search-result-list">
        <div
          v-for="item in globalSearchResults"
          :key="item.id"
          class="search-result-item"
          @click="openDetailDrawer(item)"
        >
          <div class="result-name">{{ item.termName }}</div>
          <div class="result-meta">
            <a-tag v-if="item.categoryName" color="purple" size="small">{{ item.categoryName }}</a-tag>
            <a-tag v-if="item.status" :color="item.status === 'PUBLISHED' ? 'green' : 'orange'" size="small">
              {{ item.statusLabel }}
            </a-tag>
            <code class="result-code">{{ item.termCode }}</code>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区：左侧分类树 + 右侧内容 -->
    <div class="content-wrapper">
      <!-- 左侧分类树 -->
      <div class="tree-panel">
        <div class="panel-header">
          <span class="panel-title">术语分类</span>
          <a-space size="small">
            <a-tooltip title="新增顶级分类">
              <a-button type="text" size="small" @click="openAddCategoryDrawer(0)">
                <template #icon><FolderAddOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="刷新分类">
              <a-button type="text" size="small" @click="loadCategoryTree">
                <template #icon><ReloadOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </div>

        <!-- 分类搜索 -->
        <div class="tree-search">
          <a-input v-model:value="categorySearchKeyword" placeholder="搜索分类..." size="small" allowClear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </div>

        <!-- 分类树 -->
        <div class="tree-content">
          <a-spin :spinning="categoryLoading">
            <a-tree
              v-if="categoryTree.length > 0"
              :tree-data="categoryTree"
              :selectedKeys="selectedCategoryKeys"
              :expandedKeys="expandedCategoryKeys"
              :auto-expand-parent="autoExpandParent"
              :show-icon="true"
              @select="handleCategorySelect"
              @expand="handleCategoryExpand"
              @rightClick="handleCategoryRightClick"
            >
              <template #icon><FolderOutlined /></template>
              <template #title="node">
                <div class="tree-node-wrapper">
                  <span class="tree-node-title">{{ node.title }}</span>
                  <span class="tree-node-count" v-if="node.termCount > 0">{{ node.termCount }}</span>
                </div>
              </template>
            </a-tree>
            <div v-else class="tree-empty">
              <FolderOutlined style="font-size: 40px; color: #d9d9d9; margin-bottom: 12px" />
              <div style="font-size: 13px; color: #8C8C8C">暂无分类</div>
              <a-button type="link" size="small" @click="openAddCategoryDrawer(0)">新建顶级分类</a-button>
            </div>
          </a-spin>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="list-panel">
        <!-- 标签页切换：术语列表 / 映射审批 -->
        <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
          <a-tab-pane key="terms" tab="术语管理">
            <!-- 统计卡片 -->
            <div class="stat-mini-row">
              <div class="stat-mini-card stat-purple" v-for="stat in termStatCards" :key="stat.label">
                <div class="mini-value">{{ stat.value }}</div>
                <div class="mini-label">{{ stat.label }}</div>
              </div>
            </div>

            <!-- 筛选区 -->
            <div class="filter-card">
              <div class="filter-bar">
                <a-space wrap>
                  <a-input
                    v-model:value="searchKeyword"
                    placeholder="搜索术语名称/编码/英文名/别名"
                    style="width: 220px"
                    allowClear
                    @pressEnter="loadTerms"
                  >
                    <template #prefix><SearchOutlined /></template>
                  </a-input>
                  <a-select
                    v-model:value="filterStatus"
                    placeholder="状态"
                    style="width: 120px"
                    allowClear
                    @change="loadTerms"
                  >
                    <a-select-option value="DRAFT">草稿</a-select-option>
                    <a-select-option value="PUBLISHED">已发布</a-select-option>
                    <a-select-option value="DEPRECATED">已废弃</a-select-option>
                  </a-select>
                  <a-select
                    v-model:value="filterDataType"
                    placeholder="数据类型"
                    style="width: 120px"
                    allowClear
                    @change="loadTerms"
                  >
                    <a-select-option value="STRING">字符串</a-select-option>
                    <a-select-option value="NUMBER">数值</a-select-option>
                    <a-select-option value="DATE">日期</a-select-option>
                    <a-select-option value="BOOLEAN">布尔</a-select-option>
                  </a-select>
                  <a-button @click="resetFilters">
                    <template #icon><ReloadOutlined /></template>
                    重置
                  </a-button>
                </a-space>
              </div>
            </div>

            <!-- 术语表格 -->
            <div class="table-card">
              <a-table
                :columns="columns"
                :data-source="tableData"
                :loading="tableLoading"
                :pagination="pagination"
                :row-key="(record: any) => record.id"
                @change="handleTableChange"
                :scroll="{ x: 1600 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'termName'">
                    <div class="term-name-cell">
                      <div class="name-text">{{ record.termName }}</div>
                      <div class="code-text">{{ record.termCode }}</div>
                    </div>
                  </template>

                  <template v-if="column.key === 'termAlias'">
                    <div class="alias-cell">
                      <span v-if="record.termNameEn" class="alias-item en">{{ record.termNameEn }}</span>
                      <span v-if="record.termAlias" class="alias-item alias">{{ record.termAlias }}</span>
                      <span v-if="!record.termNameEn && !record.termAlias" class="no-data">-</span>
                    </div>
                  </template>

                  <template v-if="column.key === 'category'">
                    <a-tag v-if="record.categoryName" color="purple">{{ record.categoryName }}</a-tag>
                    <span v-else class="no-data">-</span>
                  </template>

                  <template v-if="column.key === 'dataType'">
                    <a-tag :color="getDataTypeColor(record.dataType)">
                      {{ getDataTypeLabel(record.dataType) }}
                    </a-tag>
                  </template>

                  <template v-if="column.key === 'sensitivityLevel'">
                    <a-tag :color="getSensitivityColor(record.sensitivityLevel)">
                      {{ record.sensitivityLevelLabel || record.sensitivityLevel }}
                    </a-tag>
                  </template>

                  <template v-if="column.key === 'status'">
                    <a-badge
                      :status="getStatusBadge(record.status)"
                      :text="record.statusLabel || record.status"
                    />
                  </template>

                  <template v-if="column.key === 'enabled'">
                    <a-switch
                      :checked="record.enabled === 1"
                      size="small"
                      @change="(checked: boolean) => handleToggleTerm(record, checked)"
                    />
                  </template>

                  <template v-if="column.key === 'action'">
                    <a-space>
                      <a-tooltip title="查看详情">
                        <a-button type="text" size="small" @click="openDetailDrawer(record)">
                          <template #icon><EyeOutlined /></template>
                        </a-button>
                      </a-tooltip>
                      <a-tooltip title="编辑">
                        <a-button type="text" size="small" @click="openEditTermDrawer(record)">
                          <template #icon><EditOutlined /></template>
                        </a-button>
                      </a-tooltip>
                      <a-dropdown :trigger="['click']">
                        <a-button type="text" size="small">
                          <template #icon><MoreOutlined /></template>
                        </a-button>
                        <template #overlay>
                          <a-menu @click="(e: any) => handleTermMenuClick(e.key, record)">
                            <a-menu-item key="publish" v-if="record.status === 'DRAFT'">
                              <CheckCircleOutlined /> 发布
                            </a-menu-item>
                            <a-menu-item key="deprecate" v-if="record.status === 'PUBLISHED'">
                              <StopOutlined /> 废弃
                            </a-menu-item>
                            <a-menu-item key="copy">
                              <CopyOutlined /> 复制
                            </a-menu-item>
                            <a-menu-divider />
                            <a-menu-item key="mapping">
                              <LinkOutlined /> 字段映射
                            </a-menu-item>
                            <a-menu-divider />
                            <a-menu-item key="delete" style="color: #FF4D4F">
                              <DeleteOutlined /> 删除
                            </a-menu-item>
                          </a-menu>
                        </template>
                      </a-dropdown>
                    </a-space>
                  </template>
                </template>

                <template #emptyText>
                  <div class="empty-state">
                    <FileTextOutlined class="empty-icon" />
                    <div class="empty-title">暂无术语</div>
                    <div class="empty-desc">在左侧选择分类，或直接点击"新建术语"开始录入业务术语</div>
                    <a-button type="primary" @click="openAddTermDrawer">
                      <template #icon><PlusOutlined /></template>
                      新建术语
                    </a-button>
                  </div>
                </template>
              </a-table>
            </div>
          </a-tab-pane>

          <!-- 映射审批 Tab -->
          <a-tab-pane key="mapping-approval">
            <template #tab>
              <span>
                映射审批
                <a-badge
                  v-if="pendingMappingCount > 0"
                  :count="pendingMappingCount"
                  :overflow-count="99"
                  :number-style="{ backgroundColor: '#FF4D4F', fontSize: '10px', minWidth: '16px', height: '16px', lineHeight: '16px' }"
                  style="margin-left: 4px"
                />
              </span>
            </template>

            <!-- 映射审批统计卡片 -->
            <div class="stat-mini-row" style="margin-top: 0">
              <div class="stat-mini-card" style="background: linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)">
                <div class="mini-value">{{ pendingMappingCount }}</div>
                <div class="mini-label">待审批</div>
              </div>
              <div class="stat-mini-card" style="background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%)">
                <div class="mini-value">{{ approvedMappingCount }}</div>
                <div class="mini-label">已审批</div>
              </div>
              <div class="stat-mini-card" style="background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%)">
                <div class="mini-value">{{ rejectedMappingCount }}</div>
                <div class="mini-label">已驳回</div>
              </div>
              <div class="stat-mini-card" style="background: linear-gradient(135deg, #1677FF 0%, #69B1FF 100%)">
                <div class="mini-value">{{ mappingStatsData.total || 0 }}</div>
                <div class="mini-label">映射总数</div>
              </div>
            </div>

            <!-- 映射筛选区 -->
            <div class="filter-card">
              <div class="filter-bar">
                <a-space wrap>
                  <a-input
                    v-model:value="mappingSearchKeyword"
                    placeholder="搜索术语名称"
                    style="width: 200px"
                    allowClear
                    @pressEnter="loadMappingList"
                  >
                    <template #prefix><SearchOutlined /></template>
                  </a-input>
                  <a-select
                    v-model:value="mappingFilterStatus"
                    placeholder="映射状态"
                    style="width: 120px"
                    allowClear
                    @change="loadMappingList"
                  >
                    <a-select-option value="PENDING">待审批</a-select-option>
                    <a-select-option value="APPROVED">已审批</a-select-option>
                    <a-select-option value="REJECTED">已驳回</a-select-option>
                  </a-select>
                  <a-button @click="loadMappingList">
                    <template #icon><ReloadOutlined /></template>
                    刷新
                  </a-button>
                </a-space>
                <div style="margin-left: auto">
                  <a-space>
                    <a-button
                      v-if="selectedMappingRowKeys.length > 0"
                      type="primary"
                      size="small"
                      @click="handleBatchApprove"
                    >
                      批量通过 ({{ selectedMappingRowKeys.length }})
                    </a-button>
                    <a-button
                      v-if="selectedMappingRowKeys.length > 0"
                      danger
                      size="small"
                      @click="handleBatchReject"
                    >
                      批量驳回
                    </a-button>
                  </a-space>
                </div>
              </div>
            </div>

            <!-- 映射审批表格 -->
            <div class="table-card">
              <a-table
                :columns="mappingColumns"
                :data-source="mappingTableData"
                :loading="mappingTableLoading"
                :pagination="mappingPagination"
                :row-key="(record: any) => record.id"
                :row-selection="mappingRowSelection"
                @change="handleMappingTableChange"
                :scroll="{ x: 1400 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'termName'">
                    <div>
                      <div style="font-weight: 500">{{ record.termName || '-' }}</div>
                      <div class="code-text" style="font-size: 11px">{{ record.termCode || '' }}</div>
                    </div>
                  </template>

                  <template v-if="column.key === 'field'">
                    <code>{{ record.tableName }}.{{ record.columnName }}</code>
                  </template>

                  <template v-if="column.key === 'mappingType'">
                    <a-tag>{{ record.mappingTypeLabel || record.mappingType }}</a-tag>
                  </template>

                  <template v-if="column.key === 'confidence'">
                    <a-progress
                      v-if="record.confidence"
                      :percent="Number(record.confidence)"
                      :format="(p: number) => p + '%'"
                      size="small"
                      :stroke-color="getConfidenceColor(Number(record.confidence))"
                    />
                    <span v-else class="no-data">-</span>
                  </template>

                  <template v-if="column.key === 'status'">
                    <a-tag :color="getMappingStatusColor(record.status)">
                      {{ record.statusLabel || record.status }}
                    </a-tag>
                    <div v-if="record.rejectReason" style="font-size: 11px; color: #FF4D4F; margin-top: 2px">
                      {{ record.rejectReason }}
                    </div>
                  </template>

                  <template v-if="column.key === 'action'">
                    <a-space v-if="record.status === 'PENDING'">
                      <a-tooltip title="通过">
                        <a-button type="text" size="small" style="color: #52C41A" @click="handleApproveOne(record)">
                          <template #icon><CheckOutlined /></template>
                        </a-button>
                      </a-tooltip>
                      <a-tooltip title="驳回">
                        <a-button type="text" size="small" style="color: #FF4D4F" @click="handleRejectOne(record)">
                          <template #icon><CloseOutlined /></template>
                        </a-button>
                      </a-tooltip>
                    </a-space>
                    <span v-else class="no-data">-</span>
                  </template>
                </template>

                <template #emptyText>
                  <div class="empty-state">
                    <LinkOutlined class="empty-icon" />
                    <div class="empty-title">暂无映射记录</div>
                    <div class="empty-desc">在术语详情中关联元数据字段即可创建映射关系</div>
                  </div>
                </template>
              </a-table>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>

    <!-- ============================================================ -->
    <!-- 新增/编辑术语抽屉 -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="termDrawerVisible"
      :title="termDrawerMode === 'add' ? '新建术语' : '编辑术语'"
      :width="720"
      @close="termDrawerVisible = false"
    >
      <a-form
        ref="termFormRef"
        :model="termForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="termFormRules"
      >
        <!-- 步骤1: 基本信息 -->
        <a-divider orientation="left">基本信息</a-divider>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="术语名称" name="termName">
              <a-input v-model:value="termForm.termName" placeholder="输入术语名称（中文）" :maxlength="100" show-count />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="术语编码" name="termCode">
              <a-input v-model:value="termForm.termCode" placeholder="输入术语编码，唯一标识" :maxlength="50" show-count :disabled="termDrawerMode === 'edit'" />
              <div class="form-tip">建议使用英文大写下划线，如 CUSTOMER_ID</div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="英文名" name="termNameEn">
              <a-input v-model:value="termForm.termNameEn" placeholder="英文名称（可选）" :maxlength="100" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="别名" name="termAlias">
              <a-input v-model:value="termForm.termAlias" placeholder="别名/简称，多个用逗号分隔" :maxlength="200" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="所属分类" name="categoryId">
              <a-tree-select
                v-model:value="termForm.categoryId"
                :tree-data="categoryTreeForSelect"
                placeholder="选择术语所属分类"
                allowClear
                tree-default-expand-all
                :field-names="{ label: 'title', value: 'key' }"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="业务域" name="bizDomain">
              <a-input v-model:value="termForm.bizDomain" placeholder="输入业务域，如 客户域" :maxlength="50" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 步骤2: 业务定义 -->
        <a-divider orientation="left">业务定义</a-divider>

        <a-form-item label="术语定义" name="definition" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea
            v-model:value="termForm.definition"
            placeholder="填写术语的业务定义和解释，说明该术语的业务含义和使用场景"
            :rows="4"
            :maxlength="2000"
            show-count
          />
        </a-form-item>

        <a-form-item label="计算公式" name="formula" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea
            v-model:value="termForm.formula"
            placeholder="如有计算公式，填写在此（如 COUNT(DISTINCT user_id) WHERE login_date = today）"
            :rows="3"
            :maxlength="1000"
            show-count
            style="font-family: 'JetBrains Mono', monospace"
          />
        </a-form-item>

        <!-- 步骤3: 技术属性 -->
        <a-divider orientation="left">技术属性</a-divider>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="数据类型" name="dataType">
              <a-select v-model:value="termForm.dataType" placeholder="选择数据类型">
                <a-select-option value="STRING">字符串 (STRING)</a-select-option>
                <a-select-option value="NUMBER">数值 (NUMBER)</a-select-option>
                <a-select-option value="DATE">日期 (DATE)</a-select-option>
                <a-select-option value="BOOLEAN">布尔 (BOOLEAN)</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="单位" name="unit">
              <a-input v-model:value="termForm.unit" placeholder="如 元、个、%" :maxlength="30" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="示例值" name="exampleValue">
              <a-input v-model:value="termForm.exampleValue" placeholder="填写典型示例值" :maxlength="200" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="敏感等级" name="sensitivityLevel">
              <a-select v-model:value="termForm.sensitivityLevel" placeholder="选择敏感等级">
                <a-select-option value="PUBLIC">公开 (PUBLIC)</a-select-option>
                <a-select-option value="INTERNAL">内部 (INTERNAL)</a-select-option>
                <a-select-option value="CONFIDENTIAL">保密 (CONFIDENTIAL)</a-select-option>
                <a-select-option value="SECRET">机密 (SECRET)</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="termForm.status" placeholder="选择状态">
                <a-select-option value="DRAFT">草稿</a-select-option>
                <a-select-option value="PUBLISHED">已发布</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 步骤4: 字段映射（在术语编辑时可见） -->
        <a-divider orientation="left">
          关联字段映射
          <a-button type="link" size="small" @click="openMappingDrawerFromTerm" style="padding: 0 4px">
            <template #icon><PlusOutlined /></template>
            添加映射
          </a-button>
        </a-divider>

        <div class="mapping-list-in-form">
          <a-table
            v-if="termForm.mappings && termForm.mappings.length > 0"
            :columns="mappingInlineColumns"
            :data-source="termForm.mappings"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'field'">
                <code>{{ record.tableName }}.{{ record.columnName }}</code>
              </template>
              <template v-if="column.key === 'mappingType'">
                <a-tag>{{ record.mappingTypeLabel || record.mappingType }}</a-tag>
              </template>
              <template v-if="column.key === 'confidence'">
                {{ record.confidence ? record.confidence + '%' : '-' }}
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="text" size="small" style="color: #FF4D4F" @click="removeMappingFromTerm(record)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
          <div v-else class="no-mapping-tip-inline">
            <LinkOutlined />
            <span>暂无关联字段映射，点击上方"添加映射"开始关联</span>
          </div>
        </div>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="termDrawerVisible = false">取消</a-button>
          <a-button @click="termFormRef && termFormRef.resetFields()">重置</a-button>
        </a-space>
        <a-space>
          <a-button @click="saveTermAndContinue(termFormRef)">
            保存草稿
          </a-button>
          <a-button type="primary" :loading="termSaving" @click="saveTermAndClose(termFormRef)">
            保存并关闭
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- ============================================================ -->
    <!-- 术语详情抽屉 -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      title="术语详情"
      :width="720"
      @close="detailDrawerVisible = false"
    >
      <a-descriptions :column="2" bordered size="small" v-if="currentTerm">
        <a-descriptions-item label="术语编码" :span="2">
          <code class="code-inline">{{ currentTerm.termCode }}</code>
        </a-descriptions-item>
        <a-descriptions-item label="术语名称">{{ currentTerm.termName }}</a-descriptions-item>
        <a-descriptions-item label="英文名">{{ currentTerm.termNameEn || '-' }}</a-descriptions-item>
        <a-descriptions-item label="别名">{{ currentTerm.termAlias || '-' }}</a-descriptions-item>
        <a-descriptions-item label="所属分类">
          <a-tag v-if="currentTerm.categoryName" color="purple">{{ currentTerm.categoryName }}</a-tag>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="业务域">{{ currentTerm.bizDomain || '-' }}</a-descriptions-item>
        <a-descriptions-item label="数据类型">
          <a-tag :color="getDataTypeColor(currentTerm.dataType)">{{ getDataTypeLabel(currentTerm.dataType) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="单位">{{ currentTerm.unit || '-' }}</a-descriptions-item>
        <a-descriptions-item label="示例值">{{ currentTerm.exampleValue || '-' }}</a-descriptions-item>
        <a-descriptions-item label="敏感等级">
          <a-tag :color="getSensitivityColor(currentTerm.sensitivityLevel)">
            {{ currentTerm.sensitivityLevelLabel || currentTerm.sensitivityLevel || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-badge :status="getStatusBadge(currentTerm.status)" :text="currentTerm.statusLabel || currentTerm.status" />
        </a-descriptions-item>
        <a-descriptions-item label="是否启用">
          <a-tag :color="currentTerm.enabled === 1 ? 'green' : 'red'">
            {{ currentTerm.enabled === 1 ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentTerm.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="发布时间">{{ currentTerm.publishedTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="术语定义" :span="2">
          <div class="definition-content" v-if="currentTerm.definition">{{ currentTerm.definition }}</div>
          <span v-else class="no-data">-</span>
        </a-descriptions-item>
        <a-descriptions-item label="计算公式" :span="2">
          <code v-if="currentTerm.formula" class="code-block">{{ currentTerm.formula }}</code>
          <span v-else class="no-data">-</span>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 字段映射列表 -->
      <a-divider>
        关联字段映射 ({{ currentTerm?.mappings?.length || 0 }})
        <a-button type="link" size="small" @click="openMappingDrawerFromDetail" style="padding: 0 4px">
          <template #icon><PlusOutlined /></template>
          添加映射
        </a-button>
      </a-divider>
      <a-table
        v-if="currentTerm?.mappings?.length && currentTerm.mappings.length > 0"
        :columns="mappingColumns"
        :data-source="currentTerm.mappings || []"
        :pagination="false"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dsName'">
            {{ record.dsName || '-' }}
          </template>
          <template v-if="column.key === 'field'">
            <code>{{ record.tableName }}.{{ record.columnName }}</code>
          </template>
          <template v-if="column.key === 'mappingType'">
            <a-tag>{{ record.mappingTypeLabel || record.mappingType }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getMappingStatusColor(record.status)">{{ record.statusLabel || record.status }}</a-tag>
          </template>
        </template>
      </a-table>
      <div v-else class="no-mapping-tip">
        <LinkOutlined style="font-size: 32px; color: #d9d9d9" />
        <div>暂无字段映射</div>
        <a-button type="link" @click="openMappingDrawerFromDetail">立即添加</a-button>
      </div>
    </a-drawer>

    <!-- ============================================================ -->
    <!-- 字段映射管理抽屉 -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="mappingDrawerVisible"
      :title="mappingDrawerMode === 'add' ? '添加字段映射' : '编辑字段映射'"
      :width="640"
      @close="mappingDrawerVisible = false"
    >
      <!-- 选择数据源 + 表 -->
      <a-form
        ref="mappingFormRef"
        :model="mappingForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="mappingFormRules"
      >
        <a-form-item label="选择数据源" name="dsId">
          <a-select
            v-model:value="mappingForm.dsId"
            placeholder="请先选择数据源"
            @change="handleDsChange"
            showSearch
            :options="dsOptions"
            :field-names="{ label: 'dsName', value: 'id' }"
          />
        </a-form-item>

        <a-form-item label="选择表" name="tableName">
          <a-select
            v-model:value="mappingForm.tableName"
            placeholder="请先选择数据源"
            @change="handleTableChangeForMapping"
            :disabled="!mappingForm.dsId"
            showSearch
            :options="tableNameOptions"
          />
        </a-form-item>

        <a-form-item label="选择字段" name="columnName">
          <a-select
            v-model:value="mappingForm.columnName"
            placeholder="请先选择表"
            :disabled="!mappingForm.tableName"
            showSearch
            :options="columnNameOptions"
          />
        </a-form-item>

        <a-form-item label="映射类型" name="mappingType">
          <a-select v-model:value="mappingForm.mappingType" placeholder="选择映射类型">
            <a-select-option value="DIRECT">直接映射</a-select-option>
            <a-select-option value="TRANSFORM">转换映射</a-select-option>
            <a-select-option value="AGGREGATE">聚合映射</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="映射说明" name="mappingDesc">
          <a-textarea v-model:value="mappingForm.mappingDesc" placeholder="描述该字段与术语的映射关系" :rows="3" :maxlength="500" show-count />
        </a-form-item>

        <a-form-item label="匹配置信度" name="confidence">
          <a-input-number
            v-model:value="mappingForm.confidence"
            :min="0"
            :max="100"
            style="width: 100%"
            placeholder="0-100，表示匹配程度"
          />
          <div class="form-tip">置信度 0-100，可根据自动匹配置信度调整</div>
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="mappingDrawerVisible = false">取消</a-button>
        </a-space>
        <a-space>
          <a-button type="primary" :loading="mappingSaving" @click="saveMapping(mappingFormRef)">
            确定添加
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- ============================================================ -->
    <!-- 新增/编辑分类抽屉 -->
    <!-- ============================================================ -->
    <a-drawer
      v-model:open="categoryDrawerVisible"
      :title="categoryDrawerMode === 'add' ? '新建分类' : '编辑分类'"
      :width="480"
      @close="categoryDrawerVisible = false"
    >
      <a-form
        ref="categoryFormRef"
        :model="categoryForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="categoryFormRules"
      >
        <a-form-item label="上级分类" name="parentId">
          <a-tree-select
            v-model:value="categoryForm.parentId"
            :tree-data="categoryTreeForSelect"
            placeholder="选择上级分类（留空为顶级分类）"
            allowClear
            tree-default-expand-all
            :field-names="{ label: 'title', value: 'key' }"
          />
        </a-form-item>

        <a-form-item label="分类名称" name="categoryName">
          <a-input v-model:value="categoryForm.categoryName" placeholder="输入分类名称" :maxlength="100" show-count />
        </a-form-item>

        <a-form-item label="分类编码" name="categoryCode">
          <a-input v-model:value="categoryForm.categoryCode" placeholder="输入分类编码，唯一标识" :maxlength="50" show-count :disabled="categoryDrawerMode === 'edit'" />
          <div class="form-tip">建议使用英文大写下划线，如 CUSTOMER_DOMAIN</div>
        </a-form-item>

        <a-form-item label="分类描述" name="categoryDesc">
          <a-textarea v-model:value="categoryForm.categoryDesc" placeholder="简要描述分类的业务范围" :rows="3" :maxlength="500" show-count />
        </a-form-item>

        <a-form-item label="排序号" name="sortOrder">
          <a-input-number v-model:value="categoryForm.sortOrder" :min="0" :max="9999" style="width: 100%" />
          <div class="form-tip">数字越小排序越靠前</div>
        </a-form-item>
      </a-form>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="categoryDrawerVisible = false">取消</a-button>
        </a-space>
        <a-space>
          <a-button type="primary" :loading="categorySaving" @click="saveCategory(categoryFormRef)">
            确定
          </a-button>
        </a-space>
      </div>
    </a-drawer>

    <!-- ============================================================ -->
    <!-- 批量导入弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="importModalVisible"
      title="批量导入术语"
      :width="520"
      @ok="handleImportOk"
      @cancel="importModalVisible = false"
      :confirmLoading="importLoading"
      okText="开始导入"
      cancelText="取消"
    >
      <div class="import-modal-content">
        <!-- 步骤说明 -->
        <a-steps :current="importStep" size="small" style="margin-bottom: 24px">
          <a-step title="下载模板" />
          <a-step title="填写数据" />
          <a-step title="上传导入" />
        </a-steps>

        <div v-if="importStep === 0" class="import-step-content">
          <div class="import-tip-box">
            <InfoCircleOutlined style="color: #1677FF; font-size: 18px; margin-right: 8px" />
            <div>
              <div style="font-weight: 600; margin-bottom: 8px">操作步骤</div>
              <div style="font-size: 13px; color: #595959; line-height: 1.8">
                1. 点击下方按钮下载 Excel 导入模板<br>
                2. 按模板格式填写术语数据<br>
                3. 上传填写好的 Excel 文件完成导入
              </div>
            </div>
          </div>

          <a-button type="primary" block style="margin-top: 20px" @click="downloadTemplate">
            <template #icon><DownloadOutlined /></template>
            下载导入模板
          </a-button>
        </div>

        <div v-if="importStep === 1" class="import-step-content">
          <a-alert
            message="模板填写说明"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          >
            <template #description>
              <ul style="margin: 8px 0; padding-left: 20px; font-size: 13px; color: #595959; line-height: 2">
                <li>带 <span style="color: #FF4D4F">*</span> 的列为必填项</li>
                <li>术语编码不能与现有数据重复</li>
                <li>状态填写: DRAFT（草稿）/ PUBLISHED（已发布）</li>
                <li>数据类型填写: STRING / NUMBER / DATE / BOOLEAN</li>
                <li>敏感等级填写: PUBLIC / INTERNAL / CONFIDENTIAL / SECRET</li>
                <li>一次最多导入 500 条数据</li>
              </ul>
            </template>
          </a-alert>

          <a-table
            :columns="templateColumns"
            :data-source="templatePreviewData"
            :pagination="false"
            size="small"
            bordered
            style="margin-bottom: 16px"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'required'">
                <span v-if="record.required" style="color: #FF4D4F">*</span>
                <span v-else>-</span>
              </template>
              <template v-if="column.key === 'example'">
                <code style="font-size: 11px">{{ record.example }}</code>
              </template>
            </template>
          </a-table>

          <a-button type="link" @click="downloadTemplate">
            <template #icon><DownloadOutlined /></template>
            下载完整模板（含示例）
          </a-button>
        </div>

        <div v-if="importStep === 2" class="import-step-content">
          <a-upload
           -dragger
            v-model:file-list="importFileList"
            name="file"
            :multiple="false"
            accept=".xlsx,.xls"
            :before-upload="beforeUpload"
            :max-count="1"
            @remove="handleRemoveFile"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">点击或拖拽 Excel 文件到此区域上传</p>
            <p class="ant-upload-hint">支持 .xlsx 和 .xls 格式，单文件不超过 5MB</p>
          </a-upload>

          <!-- 导入结果预览 -->
          <div v-if="importResult" class="import-result">
            <a-alert
              :message="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
              :type="importResult.failCount > 0 ? 'warning' : 'success'"
              show-icon
              style="margin-top: 16px"
            />
            <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top: 12px">
              <div style="font-weight: 600; font-size: 13px; margin-bottom: 8px">失败记录：</div>
              <div
                v-for="(err, idx) in importResult.errors.slice(0, 10)"
                :key="idx"
                style="font-size: 12px; color: #FF4D4F; margin-bottom: 4px; padding: 4px 8px; background: #fff2f0; border-radius: 4px"
              >
                第 {{ err.row }} 行: {{ err.message }}
              </div>
              <div v-if="importResult.errors.length > 10" style="font-size: 12px; color: #8C8C8C; margin-top: 4px">
                还有 {{ importResult.errors.length - 10 }} 条错误未显示...
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <a-space>
          <a-button v-if="importStep > 0" @click="importStep--">上一步</a-button>
          <a-button v-if="importStep < 2" type="primary" @click="importStep++">下一步</a-button>
          <a-button v-if="importStep === 2" type="primary" :disabled="!selectedImportFile" :loading="importLoading" @click="handleImportFile">
            <template #icon><UploadOutlined /></template>
            确认导入
          </a-button>
        </a-space>
      </template>
    </a-modal>

    <!-- ============================================================ -->
    <!-- 驳回原因弹窗 -->
    <!-- ============================================================ -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="驳回映射"
      :width="480"
      @ok="confirmReject"
      @cancel="rejectModalVisible = false"
      :confirmLoading="rejectLoading"
      okText="确认驳回"
      cancelText="取消"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="驳回原因">
          <a-textarea
            v-model:value="rejectReason"
            placeholder="请填写驳回原因，以便创建者了解需要修改的内容"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分类右键菜单 -->
    <div v-if="categoryContextMenu.visible" :style="{ position: 'fixed', left: categoryContextMenu.x + 'px', top: categoryContextMenu.y + 'px', zIndex: 1000 }">
      <a-menu @click="(e: any) => handleCategoryMenuClick(e.key)">
        <a-menu-item key="addChild">
          <FolderAddOutlined /> 新增子分类
        </a-menu-item>
        <a-menu-item key="edit">
          <EditOutlined /> 编辑分类
        </a-menu-item>
        <a-menu-item key="move">
          <SwapOutlined /> 移动分类
        </a-menu-item>
        <a-menu-divider />
        <a-menu-item key="delete" style="color: #FF4D4F">
          <DeleteOutlined /> 删除分类
        </a-menu-item>
      </a-menu>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  MoreOutlined,
  DeleteOutlined,
  CopyOutlined,
  CheckCircleOutlined,
  StopOutlined,
  LinkOutlined,
  FolderOutlined,
  FolderAddOutlined,
  SwapOutlined,
  FileTextOutlined,
  DownloadOutlined,
  UploadOutlined,
  InboxOutlined,
  InfoCircleOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  glossaryCategoryApi,
  glossaryTermApi,
  glossaryMappingApi,
  type GlossaryCategoryTreeVO,
  type GlossaryCategoryVO,
  type GlossaryTermVO,
  type GlossaryTermDetailVO,
  type GlossaryCategorySaveDTO,
  type GlossaryTermSaveDTO,
  type GlossaryMappingVO,
  type GlossaryMappingDTO,
  type TermStatus,
  type DataType,
  type SensitivityLevel,
  type MappingStatus,
} from '@/api/glossary'
import { dataSourceApi } from '@/api/dqc'
import { metadataApi, type Metadata, type MetadataColumn } from '@/api/gov'
import type { Page } from '@/api/glossary'

// ============================================================
// 状态定义
// ============================================================

// 标签页
const activeTab = ref('terms')

// 分类相关
const categoryLoading = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryKeys = ref<string[]>([])
const expandedCategoryKeys = ref<string[]>([])
const autoExpandParent = ref(true)
const categorySearchKeyword = ref('')
const categoryDrawerVisible = ref(false)
const categoryDrawerMode = ref<'add' | 'edit'>('add')
const categoryFormRef = ref<FormInstance>()
const categorySaving = ref(false)
const currentEditCategoryId = ref<number | null>(null)
const categoryContextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  currentCategory: null as GlossaryCategoryTreeVO | null,
})

// 术语相关
const tableLoading = ref(false)
const tableData = ref<GlossaryTermVO[]>([])
const termDrawerVisible = ref(false)
const termDrawerMode = ref<'add' | 'edit'>('add')
const termFormRef = ref<FormInstance>()
const termSaving = ref(false)
const detailDrawerVisible = ref(false)
const currentTerm = ref<GlossaryTermDetailVO | null>(null)
const currentEditTermId = ref<number | null>(null)

// 全局搜索
const globalSearchKeyword = ref('')
const globalSearchResults = ref<GlossaryTermVO[]>([])
const globalSearchVisible = ref(false)
let globalSearchTimer: ReturnType<typeof setTimeout> | null = null

// 映射相关
const mappingDrawerVisible = ref(false)
const mappingDrawerMode = ref<'add' | 'edit'>('add')
const mappingFormRef = ref<FormInstance>()
const mappingSaving = ref(false)
const mappingForm = reactive<GlossaryMappingDTO>({
  dsId: undefined,
  tableName: '',
  columnName: '',
  mappingType: 'DIRECT',
  mappingDesc: '',
  confidence: undefined,
})
const dsOptions = ref<any[]>([])
const tableNameOptions = ref<any[]>([])
const columnNameOptions = ref<any[]>([])
const currentMappingTermId = ref<number | null>(null)

// 映射审批相关
const mappingTableLoading = ref(false)
const mappingTableData = ref<GlossaryMappingVO[]>([])
const selectedMappingRowKeys = ref<string[]>([])
const mappingSearchKeyword = ref('')
const mappingFilterStatus = ref<MappingStatus | undefined>()
const pendingMappingCount = ref(0)
const approvedMappingCount = ref(0)
const rejectedMappingCount = ref(0)
const mappingStatsData = reactive({ total: 0 })

// 映射驳回
const rejectModalVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const currentRejectRecord = ref<GlossaryMappingVO | null>(null)

// 批量导入相关
const importModalVisible = ref(false)
const importStep = ref(0)
const importFileList = ref<any[]>([])
const selectedImportFile = ref<File | null>(null)
const importLoading = ref(false)
const importResult = ref<{ successCount: number; failCount: number; errors?: Array<{ row: number; message: string }> } | null>(null)

// 筛选相关
const searchKeyword = ref('')
const filterStatus = ref<TermStatus | undefined>()
const filterDataType = ref<DataType | undefined>()

// 表单数据
const categoryForm = reactive<GlossaryCategorySaveDTO>({
  id: undefined,
  parentId: undefined,
  categoryName: '',
  categoryCode: '',
  categoryDesc: '',
  sortOrder: 0,
  status: 1,
  createUser: undefined,
})

const termForm = reactive<GlossaryTermSaveDTO>({
  id: undefined,
  termCode: '',
  termName: '',
  termNameEn: '',
  termAlias: '',
  categoryId: undefined,
  bizDomain: '',
  definition: '',
  formula: '',
  dataType: undefined,
  unit: '',
  exampleValue: '',
  sensitivityLevel: undefined,
  status: 'DRAFT',
  ownerId: undefined,
  deptId: undefined,
  sortOrder: 0,
  enabled: 1,
  createUser: undefined,
  mappings: [],
})

// ============================================================
// 表格列定义
// ============================================================

const columns: TableColumnType<any>[] = [
  { title: '术语名称', key: 'termName', width: 200, fixed: 'left' },
  { title: '英文名 / 别名', key: 'termAlias', width: 180 },
  { title: '所属分类', key: 'category', width: 140 },
  { title: '业务域', dataIndex: 'bizDomain', width: 120 },
  { title: '数据类型', key: 'dataType', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 80 },
  { title: '敏感等级', key: 'sensitivityLevel', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '启用', key: 'enabled', width: 70 },
  { title: '映射数', dataIndex: 'mappingCount', width: 80, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

const mappingColumns: TableColumnType<any>[] = [
  { title: '术语名称', key: 'termName', width: 180 },
  { title: '数据源', key: 'dsName', width: 140 },
  { title: '表.字段', key: 'field', width: 200 },
  { title: '映射类型', key: 'mappingType', width: 100 },
  { title: '置信度', key: 'confidence', width: 120 },
  { title: '说明', dataIndex: 'mappingDesc', ellipsis: true },
  { title: '状态', key: 'status', width: 120 },
  { title: '创建人', dataIndex: 'createUserName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 80 },
]

const mappingInlineColumns: TableColumnType<any>[] = [
  { title: '数据源ID', dataIndex: 'dsId', width: 100 },
  { title: '表.字段', key: 'field', width: 200 },
  { title: '映射类型', key: 'mappingType', width: 100 },
  { title: '置信度', key: 'confidence', width: 80 },
  { title: '操作', key: 'action', width: 60 },
]

const mappingRowSelection = computed(() => ({
  selectedRowKeys: selectedMappingRowKeys.value,
  onChange: (keys: string[]) => {
    selectedMappingRowKeys.value = keys
  },
}))

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const mappingPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const templateColumns: TableColumnType<any>[] = [
  { title: '字段', dataIndex: 'field', width: 120 },
  { title: '必填', key: 'required', width: 60, align: 'center' },
  { title: '说明', dataIndex: 'desc' },
  { title: '示例', key: 'example', width: 140 },
]

const templatePreviewData = ref([
  { field: 'termCode', required: true, desc: '术语编码，唯一标识', example: 'CUSTOMER_ID' },
  { field: 'termName', required: true, desc: '术语名称（中文）', example: '客户ID' },
  { field: 'termNameEn', required: false, desc: '英文名', example: 'Customer ID' },
  { field: 'termAlias', required: false, desc: '别名，多个用逗号分隔', example: 'cust_id,customer_id' },
  { field: 'categoryName', required: false, desc: '所属分类名称', example: '客户基本信息' },
  { field: 'bizDomain', required: false, desc: '业务域', example: '客户域' },
  { field: 'definition', required: false, desc: '术语定义', example: '在系统中唯一标识一个客户的编号' },
  { field: 'formula', required: false, desc: '计算公式', example: '雪花算法ID' },
  { field: 'dataType', required: false, desc: '数据类型 STRING/NUMBER/DATE/BOOLEAN', example: 'NUMBER' },
  { field: 'unit', required: false, desc: '单位', example: '个' },
  { field: 'exampleValue', required: false, desc: '示例值', example: '6871947673600000001' },
  { field: 'sensitivityLevel', required: false, desc: '敏感等级 PUBLIC/INTERNAL/CONFIDENTIAL/SECRET', example: 'PUBLIC' },
  { field: 'status', required: true, desc: '状态 DRAFT/PUBLISHED', example: 'PUBLISHED' },
])

// ============================================================
// 统计卡片
// ============================================================

const termStatCards = computed(() => [
  { label: '术语总数', value: statsData.totalTermCount || 0, bg: 'linear-gradient(135deg, #722ED1 0%, #9254DE 100%)' },
  { label: '已发布', value: statsData.publishedTermCount || 0, bg: 'linear-gradient(135deg, #52C41A 0%, #73D13D 100%)' },
  { label: '草稿', value: statsData.draftTermCount || 0, bg: 'linear-gradient(135deg, #FAAD14 0%, #FFC53D 100%)' },
  { label: '已废弃', value: statsData.deprecatedTermCount || 0, bg: 'linear-gradient(135deg, #8C8C8C 0%, #A6A6A6 100%)' },
])

const statsData = reactive({
  totalTermCount: 0,
  publishedTermCount: 0,
  draftTermCount: 0,
  deprecatedTermCount: 0,
  totalMappingCount: 0,
  pendingMappingCount: 0,
})

// ============================================================
// 表单验证规则
// ============================================================

const categoryFormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
}

const termFormRules = {
  termName: [{ required: true, message: '请输入术语名称', trigger: 'blur' }],
  termCode: [{ required: true, message: '请输入术语编码', trigger: 'blur' }],
}

const mappingFormRules = {
  dsId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  tableName: [{ required: true, message: '请选择表', trigger: 'change' }],
  columnName: [{ required: true, message: '请选择字段', trigger: 'change' }],
  mappingType: [{ required: true, message: '请选择映射类型', trigger: 'change' }],
}

// ============================================================
// 全局搜索
// ============================================================

function handleGlobalSearch(value: string) {
  if (!value.trim()) {
    globalSearchVisible.value = false
    return
  }
  performGlobalSearch(value)
}

function handleSearchInput() {
  if (globalSearchTimer) clearTimeout(globalSearchTimer)
  if (!globalSearchKeyword.value.trim()) {
    globalSearchVisible.value = false
    return
  }
  globalSearchTimer = setTimeout(() => {
    performGlobalSearch(globalSearchKeyword.value)
  }, 300)
}

async function performGlobalSearch(keyword: string) {
  try {
    const res = await glossaryTermApi.search(keyword)
    if (res.success && res.data) {
      globalSearchResults.value = res.data.slice(0, 8)
      globalSearchVisible.value = globalSearchResults.value.length > 0
    }
  } catch (e) {
    console.error('搜索失败', e)
  }
}

// ============================================================
// 分类树相关
// ============================================================

const categoryTreeForSelect = computed(() => {
  return categoryTree.value.map((node: any) => ({
    key: String(node.key),
    title: node.title,
    children: node.children ? node.children.map((child: any) => ({
      key: String(child.key),
      title: child.title,
      children: child.children,
    })) : [],
  }))
})

function convertTreeToNodes(treeData: GlossaryCategoryTreeVO[], selectedId?: number): any[] {
  return treeData.map((item) => ({
    key: String(item.id),
    title: item.categoryName,
    termCount: item.termCount || 0,
    parentId: item.parentId,
    status: item.status,
    ...item,
    children: item.children && item.children.length > 0
      ? convertTreeToNodes(item.children, selectedId)
      : undefined,
  }))
}

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res = await glossaryCategoryApi.getFullTree()
    if (res.success && res.data) {
      categoryTree.value = convertTreeToNodes(res.data)
      expandedCategoryKeys.value = categoryTree.value.map((n: any) => String(n.key))
    }
  } catch (e) {
    console.error('加载分类树失败', e)
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: string[]) {
  selectedCategoryKeys.value = keys
  if (keys.length > 0) {
    loadTerms()
  }
}

function handleCategoryExpand(keys: string[]) {
  expandedCategoryKeys.value = keys
}

function handleCategoryRightClick({ event, node }: any) {
  event.preventDefault()
  event.stopPropagation()
  categoryContextMenu.visible = true
  categoryContextMenu.x = event.clientX
  categoryContextMenu.y = event.clientY
  categoryContextMenu.currentCategory = node
}

function handleCategoryMenuClick(key: any) {
  const actionKey = String(key)
  categoryContextMenu.visible = false
  const category = categoryContextMenu.currentCategory
  if (!category) return

  switch (actionKey) {
    case 'addChild':
      openAddCategoryDrawer(category.id)
      break
    case 'edit':
      openEditCategoryDrawer(category)
      break
    case 'move':
      openMoveCategoryDrawer(category)
      break
    case 'delete':
      confirmDeleteCategory(category)
      break
  }
}

function openAddCategoryDrawer(parentId: number) {
  categoryDrawerMode.value = 'add'
  currentEditCategoryId.value = null
  categoryFormRef.value?.resetFields()
  Object.assign(categoryForm, {
    id: undefined,
    parentId: parentId || undefined,
    categoryName: '',
    categoryCode: '',
    categoryDesc: '',
    sortOrder: 0,
    status: 1,
    createUser: undefined,
  })
  categoryDrawerVisible.value = true
}

function openEditCategoryDrawer(category: GlossaryCategoryTreeVO) {
  categoryDrawerMode.value = 'edit'
  currentEditCategoryId.value = category.id ?? null
  Object.assign(categoryForm, {
    id: category.id,
    parentId: category.parentId === 0 ? undefined : category.parentId,
    categoryName: category.categoryName,
    categoryCode: category.categoryCode,
    categoryDesc: category.categoryDesc,
    sortOrder: category.sortOrder || 0,
    status: category.status,
    createUser: undefined,
  })
  categoryDrawerVisible.value = true
}

async function openMoveCategoryDrawer(category: GlossaryCategoryTreeVO) {
  try {
    const res = await glossaryCategoryApi.getFullTree()
    if (!res.success || !res.data) return
    const tree = res.data
    function excludeDescendants(node: any, excludeId: number): any[] {
      if (node.id === excludeId) return []
      return [{
        ...node,
        children: node.children ? node.children.flatMap((c: any) => excludeDescendants(c, excludeId)) : [],
      }]
    }
    const filteredTree = tree.flatMap((t: any) => excludeDescendants(t, category.id ?? 0))
    const options = buildMoveTreeOptions(filteredTree, 0)
    Modal.confirm({
      title: '移动分类',
      content: `将"${category.categoryName}"移动到：`,
      okText: '确定',
      cancelText: '取消',
    })
  } catch (e) {
    console.error('移动分类失败', e)
  }
}

function buildMoveTreeOptions(tree: any[], level: number): any[] {
  return tree.map((node: any) => ({
    value: node.id,
    label: '　'.repeat(level) + node.categoryName,
    children: node.children ? buildMoveTreeOptions(node.children, level + 1) : [],
  }))
}

async function confirmDeleteCategory(category: GlossaryCategoryTreeVO) {
  const termCount = category.termCount || 0
  const childCount = category.children?.length || 0
  let content = `确定要删除分类"${category.categoryName}"吗？`
  if (childCount > 0) {
    content += `\n该分类下有 ${childCount} 个子分类，所有子分类及其关联的术语都将被删除。`
  } else if (termCount > 0) {
    content += `\n该分类下有 ${termCount} 个术语，这些术语都将被删除。`
  }

  Modal.confirm({
    title: '确认删除',
    content,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await glossaryCategoryApi.delete(category.id!)
        if (res.success) {
          message.success('删除成功')
          await loadCategoryTree()
          loadTerms()
        }
      } catch (e) {
        console.error('删除分类失败', e)
      }
    },
  })
}

async function saveCategory(form: FormInstance | undefined) {
  if (!form) return
  try {
    await form.validate()
    categorySaving.value = true
    let res
    if (categoryDrawerMode.value === 'add') {
      res = await glossaryCategoryApi.create(categoryForm)
    } else {
      res = await glossaryCategoryApi.update(currentEditCategoryId.value!, categoryForm)
    }
    if (res.success) {
      message.success(categoryDrawerMode.value === 'add' ? '新增成功' : '更新成功')
      categoryDrawerVisible.value = false
      await loadCategoryTree()
    }
  } catch (e) {
    console.error('保存分类失败', e)
  } finally {
    categorySaving.value = false
  }
}

// ============================================================
// 术语相关
// ============================================================

async function loadTerms() {
  tableLoading.value = true
  try {
    const categoryId = selectedCategoryKeys.value[0] ? Number(selectedCategoryKeys.value[0]) : undefined
    const res = await glossaryTermApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      termName: searchKeyword.value || undefined,
      status: filterStatus.value,
      dataType: filterDataType.value,
      categoryId,
    })
    if (res.success && res.data) {
      const pageData = res.data
      tableData.value = pageData.records || []
      pagination.total = pageData.total || 0
    }
  } catch (e) {
    console.error('加载术语列表失败', e)
  } finally {
    tableLoading.value = false
  }
}

async function loadStats() {
  try {
    const res = await glossaryTermApi.getStats()
    if (res.success && res.data) {
      const s = res.data
      statsData.totalTermCount = s.totalTermCount ?? 0
      statsData.publishedTermCount = s.publishedTermCount ?? 0
      statsData.draftTermCount = s.draftTermCount ?? 0
      statsData.deprecatedTermCount = s.deprecatedTermCount ?? 0
      statsData.totalMappingCount = s.totalMappingCount ?? 0
      statsData.pendingMappingCount = s.pendingMappingCount ?? 0
      pendingMappingCount.value = s.pendingMappingCount ?? 0
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadTerms()
}

function resetFilters() {
  searchKeyword.value = ''
  filterStatus.value = undefined
  filterDataType.value = undefined
  pagination.current = 1
  loadTerms()
}

function openAddTermDrawer() {
  termDrawerMode.value = 'add'
  currentEditTermId.value = null
  termFormRef.value?.resetFields()
  Object.assign(termForm, {
    id: undefined,
    termCode: '',
    termName: '',
    termNameEn: '',
    termAlias: '',
    categoryId: selectedCategoryKeys.value[0] ? Number(selectedCategoryKeys.value[0]) : undefined,
    bizDomain: '',
    definition: '',
    formula: '',
    dataType: undefined,
    unit: '',
    exampleValue: '',
    sensitivityLevel: undefined,
    status: 'DRAFT',
    ownerId: undefined,
    deptId: undefined,
    sortOrder: 0,
    enabled: 1,
    createUser: undefined,
    mappings: [],
  })
  termDrawerVisible.value = true
}

function openEditTermDrawer(record: GlossaryTermVO) {
  termDrawerMode.value = 'edit'
  currentEditTermId.value = record.id ?? null
  Object.assign(termForm, {
    id: record.id,
    termCode: record.termCode,
    termName: record.termName,
    termNameEn: record.termNameEn || '',
    termAlias: record.termAlias || '',
    categoryId: record.categoryId,
    bizDomain: record.bizDomain || '',
    definition: record.definition || '',
    formula: '',
    dataType: record.dataType,
    unit: record.unit || '',
    exampleValue: record.exampleValue || '',
    sensitivityLevel: record.sensitivityLevel,
    status: record.status,
    ownerId: record.ownerId,
    deptId: record.deptId,
    sortOrder: record.sortOrder || 0,
    enabled: record.enabled,
    createUser: record.createUser,
    mappings: [],
  })
  // 加载已有映射
  loadMappingsForEdit(record.id!)
  termDrawerVisible.value = true
}

async function loadMappingsForEdit(termId: number) {
  try {
    const res = await glossaryMappingApi.getByTermId(termId)
    if (res.success && res.data) {
      termForm.mappings = res.data.map((m: any) => ({
        id: m.id,
        dsId: m.dsId,
        tableName: m.tableName,
        columnName: m.columnName,
        mappingType: m.mappingType,
        mappingTypeLabel: m.mappingTypeLabel,
        mappingDesc: m.mappingDesc,
        confidence: m.confidence,
        status: m.status,
      }))
    }
  } catch (e) {
    console.error('加载映射失败', e)
  }
}

async function openDetailDrawer(record: GlossaryTermVO) {
  try {
    const res = await glossaryTermApi.getDetail(record.id!)
    if (res.success && res.data) {
      currentTerm.value = res.data
      detailDrawerVisible.value = true
    }
  } catch (e) {
    console.error('加载术语详情失败', e)
  }
}

async function saveTermAndClose(form: FormInstance | undefined) {
  if (!form) return
  try {
    await form.validate()
    termSaving.value = true
    let res
    if (termDrawerMode.value === 'add') {
      res = await glossaryTermApi.create(termForm)
    } else {
      res = await glossaryTermApi.update(currentEditTermId.value!, termForm)
    }
    if (res.success) {
      message.success(termDrawerMode.value === 'add' ? '新增成功' : '更新成功')
      termDrawerVisible.value = false
      await loadTerms()
      await loadStats()
      await loadCategoryTree()
    }
  } catch (e) {
    console.error('保存术语失败', e)
  } finally {
    termSaving.value = false
  }
}

async function saveTermAndContinue(form: FormInstance | undefined) {
  if (!form) return
  try {
    await form.validate()
    termSaving.value = true
    let res
    if (termDrawerMode.value === 'add') {
      res = await glossaryTermApi.create({ ...termForm, status: 'DRAFT' })
    } else {
      res = await glossaryTermApi.update(currentEditTermId.value!, { ...termForm, status: 'DRAFT' })
    }
    if (res.success) {
      message.success('保存成功')
      termFormRef.value?.resetFields()
      Object.assign(termForm, {
        id: undefined,
        termCode: '',
        termName: '',
        termNameEn: '',
        termAlias: '',
        categoryId: termForm.categoryId,
        bizDomain: '',
        definition: '',
        formula: '',
        dataType: undefined,
        unit: '',
        exampleValue: '',
        sensitivityLevel: undefined,
        status: 'DRAFT',
        enabled: 1,
        mappings: [],
      })
      await loadTerms()
      await loadStats()
      await loadCategoryTree()
    }
  } catch (e) {
    console.error('保存术语失败', e)
  } finally {
    termSaving.value = false
  }
}

async function handleTermAction(key: string, record: GlossaryTermVO) {
  handleTermMenuClick(key, record)
}

async function handleTermMenuClick(key: any, record: GlossaryTermVO) {
  const actionKey = String(key)
  switch (actionKey) {
    case 'publish':
      await glossaryTermApi.publish(record.id!)
      message.success('发布成功')
      loadTerms()
      loadStats()
      break
    case 'deprecate':
      Modal.confirm({
        title: '确认废弃',
        content: `确定要废弃术语"${record.termName}"吗？废弃后可重新启用。`,
        okText: '废弃',
        okType: 'danger',
        cancelText: '取消',
        async onOk() {
          await glossaryTermApi.deprecate(record.id!)
          message.success('已废弃')
          loadTerms()
          loadStats()
        },
      })
      break
    case 'copy':
      await glossaryTermApi.copy(record.id!)
      message.success('复制成功')
      loadTerms()
      loadStats()
      break
    case 'mapping':
      // 打开映射管理
      openMappingDrawerForTerm(record.id!)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除术语"${record.termName}"吗？该操作不可恢复。`,
        okText: '删除',
        okType: 'danger',
        cancelText: '取消',
        async onOk() {
          await glossaryTermApi.delete(record.id!)
          message.success('删除成功')
          loadTerms()
          loadStats()
          await loadCategoryTree()
        },
      })
      break
  }
}

async function handleToggleTerm(record: GlossaryTermVO, checked: boolean) {
  try {
    if (checked) {
      await glossaryTermApi.enable(record.id!)
    } else {
      await glossaryTermApi.disable(record.id!)
    }
    message.success(checked ? '已启用' : '已禁用')
    loadTerms()
  } catch (e) {
    console.error('切换状态失败', e)
  }
}

// ============================================================
// 字段映射管理
// ============================================================

function openMappingDrawerForTerm(termId: number) {
  currentMappingTermId.value = termId
  mappingDrawerMode.value = 'add'
  mappingFormRef.value?.resetFields()
  Object.assign(mappingForm, {
    dsId: undefined,
    tableName: '',
    columnName: '',
    mappingType: 'DIRECT',
    mappingDesc: '',
    confidence: undefined,
  })
  tableNameOptions.value = []
  columnNameOptions.value = []
  loadDataSourcesForMapping()
  mappingDrawerVisible.value = true
}

function openMappingDrawerFromTerm() {
  if (currentEditTermId.value) {
    openMappingDrawerForTerm(currentEditTermId.value)
  }
}

function openMappingDrawerFromDetail() {
  if (currentTerm.value?.id) {
    openMappingDrawerForTerm(currentTerm.value.id)
  }
}

async function loadDataSourcesForMapping() {
  try {
    const res = await dataSourceApi.listEnabled()
    if (res.success && res.data) {
      dsOptions.value = res.data
    }
  } catch (e) {
    console.error('加载数据源失败', e)
  }
}

async function handleDsChange(dsId: number) {
  mappingForm.tableName = ''
  mappingForm.columnName = ''
  tableNameOptions.value = []
  columnNameOptions.value = []
  if (!dsId) return
  try {
    const res = await metadataApi.page({ pageNum: 1, pageSize: 1000, dsId })
    if (res.success && res.data) {
      const page = res.data as any
      tableNameOptions.value = (page.records || []).map((m: Metadata) => ({
        value: m.tableName,
        label: m.tableName + (m.tableAlias ? ` (${m.tableAlias})` : ''),
      }))
    }
  } catch (e) {
    console.error('加载表列表失败', e)
  }
}

async function handleTableChangeForMapping(tableName: string) {
  mappingForm.columnName = ''
  columnNameOptions.value = []
  if (!tableName || !mappingForm.dsId) return
  try {
    const res = await metadataApi.getByDsIdAndTable(mappingForm.dsId, tableName)
    if (res.success && res.data) {
      const metadata: Metadata = res.data
      const colRes = await metadataApi.getColumns(metadata.id!)
      if (colRes.data?.success && colRes.data.data) {
        columnNameOptions.value = (colRes.data.data as MetadataColumn[]).map((c) => ({
          value: c.columnName,
          label: `${c.columnName} ${c.columnComment ? `(${c.columnComment})` : ''} [${c.dataType}]`,
        }))
      }
    }
  } catch (e) {
    console.error('加载字段列表失败', e)
  }
}

async function saveMapping(form: FormInstance | undefined) {
  if (!form) return
  if (!currentMappingTermId.value) return
  try {
    await form.validate()
    mappingSaving.value = true
    const dto: GlossaryMappingDTO = {
      termId: currentMappingTermId.value,
      dsId: mappingForm.dsId,
      tableName: mappingForm.tableName,
      columnName: mappingForm.columnName,
      mappingType: mappingForm.mappingType,
      mappingDesc: mappingForm.mappingDesc,
      confidence: mappingForm.confidence as any,
      status: 'PENDING',
    }
    const res = await glossaryMappingApi.create(dto)
    if (res.success) {
      message.success('映射添加成功')
      mappingDrawerVisible.value = false
      // 刷新映射列表
      if (detailDrawerVisible.value && currentTerm.value?.id) {
        await loadMappingsForEdit(currentTerm.value.id)
        // 重新加载详情
        const detailRes = await glossaryTermApi.getDetail(currentTerm.value.id)
        if (detailRes.success && detailRes.data) {
          currentTerm.value = detailRes.data
        }
      }
      await loadStats()
      await loadMappingList()
    }
  } catch (e) {
    console.error('保存映射失败', e)
  } finally {
    mappingSaving.value = false
  }
}

function removeMappingFromTerm(record: any) {
  const idx = termForm.mappings.findIndex((m: any) => m.id === record.id)
  if (idx > -1) {
    termForm.mappings.splice(idx, 1)
  }
}

// ============================================================
// 映射审批
// ============================================================

function handleTabChange(key: string) {
  if (key === 'mapping-approval') {
    loadMappingList()
    loadMappingStats()
  }
}

async function loadMappingList() {
  mappingTableLoading.value = true
  try {
    const res = await glossaryMappingApi.page({
      pageNum: mappingPagination.current,
      pageSize: mappingPagination.pageSize,
      termName: mappingSearchKeyword.value || undefined,
      status: mappingFilterStatus.value,
    })
    if (res.success && res.data) {
      const pageData = res.data as any
      mappingTableData.value = pageData.records || []
      mappingPagination.total = pageData.total || 0
    }
  } catch (e) {
    console.error('加载映射列表失败', e)
  } finally {
    mappingTableLoading.value = false
  }
}

async function loadMappingStats() {
  try {
    const res = await glossaryMappingApi.list({ status: undefined })
    if (res.success && res.data) {
      const all = res.data
      mappingStatsData.total = all.length
      pendingMappingCount.value = all.filter((m: any) => m.status === 'PENDING').length
      approvedMappingCount.value = all.filter((m: any) => m.status === 'APPROVED').length
      rejectedMappingCount.value = all.filter((m: any) => m.status === 'REJECTED').length
    }
  } catch (e) {
    console.error('加载映射统计失败', e)
  }
}

function handleMappingTableChange(pag: any) {
  mappingPagination.current = pag.current
  mappingPagination.pageSize = pag.pageSize
  loadMappingList()
}

async function handleApproveOne(record: GlossaryMappingVO) {
  try {
    await glossaryMappingApi.approve(record.id!)
    message.success('审批通过')
    loadMappingList()
    loadMappingStats()
  } catch (e) {
    console.error('审批失败', e)
  }
}

function handleRejectOne(record: GlossaryMappingVO) {
  currentRejectRecord.value = record
  rejectReason.value = ''
  rejectModalVisible.value = true
}

async function confirmReject() {
  if (!currentRejectRecord.value) return
  try {
    rejectLoading.value = true
    await glossaryMappingApi.reject(currentRejectRecord.value.id!, rejectReason.value)
    message.success('已驳回')
    rejectModalVisible.value = false
    loadMappingList()
    loadMappingStats()
  } catch (e) {
    console.error('驳回失败', e)
  } finally {
    rejectLoading.value = false
  }
}

async function handleBatchApprove() {
  if (selectedMappingRowKeys.value.length === 0) return
  try {
    const res = await glossaryMappingApi.batchApprove(selectedMappingRowKeys.value.map(Number))
    if (res.success) {
      const result = res.data
      message.success(`批量审批完成：成功 ${result?.successCount || 0} 条`)
      selectedMappingRowKeys.value = []
      loadMappingList()
      loadMappingStats()
    }
  } catch (e) {
    console.error('批量审批失败', e)
  }
}

function handleBatchReject() {
  if (selectedMappingRowKeys.value.length === 0) return
  Modal.confirm({
    title: '批量驳回',
    content: `确定要驳回选中的 ${selectedMappingRowKeys.value.length} 条映射记录吗？`,
    okText: '确认驳回',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await glossaryMappingApi.batchReject(
          selectedMappingRowKeys.value.map(Number),
          '批量驳回'
        )
        if (res.success) {
          const result = res.data
          message.success(`批量驳回完成：成功 ${result?.successCount || 0} 条`)
          selectedMappingRowKeys.value = []
          loadMappingList()
          loadMappingStats()
        }
      } catch (e) {
        console.error('批量驳回失败', e)
      }
    },
  })
}

// ============================================================
// 批量导入/导出
// ============================================================

function openImportModal() {
  importStep.value = 0
  importFileList.value = []
  selectedImportFile.value = null
  importResult.value = null
  importModalVisible.value = true
}

function downloadTemplate() {
  // 触发下载导入模板文件
  // 后端需要实现模板下载接口 /api/gov/glossary-term/template
  glossaryTermApi.downloadTemplate().then((res: any) => {
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '术语导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    message.success('模板下载成功')
  }).catch(() => {
    message.error('模板下载失败，请稍后重试')
  })
}

function beforeUpload(file: File) {
  selectedImportFile.value = file
  return false // 阻止自动上传
}

function handleRemoveFile() {
  selectedImportFile.value = null
  importResult.value = null
}

async function handleImportFile() {
  if (!selectedImportFile.value) return
  importLoading.value = true
  try {
    const res: any = await glossaryTermApi.importExcel(selectedImportFile.value)
    if (res.success) {
      importResult.value = res.data
      if (res.data.failCount === 0) {
        message.success('导入成功！')
      } else {
        message.warning(`导入完成，${res.data.failCount} 条失败`)
      }
      await loadTerms()
      await loadStats()
    } else {
      message.error(res.data?.message || '导入失败')
    }
  } catch (e: any) {
    console.error('导入失败', e)
    message.error(e?.message || '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

function handleImportOk() {
  if (importStep.value === 2 && selectedImportFile.value) {
    handleImportFile()
  } else if (importStep.value < 2) {
    importStep.value++
  }
}

function handleExport() {
  const categoryId = selectedCategoryKeys.value[0] ? Number(selectedCategoryKeys.value[0]) : undefined
  glossaryTermApi.exportExcel({
    categoryId,
    status: filterStatus.value,
    sensitivityLevel: undefined,
  }).then((res: any) => {
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `术语库导出_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  }).catch(() => {
    message.error('导出失败，请稍后重试')
  })
}

// ============================================================
// 辅助方法
// ============================================================

function getDataTypeColor(dataType: string | undefined) {
  const map: Record<string, string> = {
    STRING: 'blue',
    NUMBER: 'cyan',
    DATE: 'orange',
    BOOLEAN: 'purple',
  }
  return map[dataType || ''] || 'default'
}

function getDataTypeLabel(dataType: string | undefined) {
  const map: Record<string, string> = {
    STRING: '字符串',
    NUMBER: '数值',
    DATE: '日期',
    BOOLEAN: '布尔',
  }
  return map[dataType || ''] || dataType || '-'
}

function getSensitivityColor(level: string | undefined) {
  const map: Record<string, string> = {
    PUBLIC: 'green',
    INTERNAL: 'blue',
    CONFIDENTIAL: 'orange',
    SECRET: 'red',
  }
  return map[level || ''] || 'default'
}

function getStatusBadge(status: string | undefined) {
  const map: Record<string, 'success' | 'warning' | 'error' | 'default' | 'processing'> = {
    PUBLISHED: 'success',
    DRAFT: 'warning',
    DEPRECATED: 'default',
  }
  return map[status || ''] || 'default'
}

function getMappingStatusColor(status: string) {
  const map: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
  }
  return map[status] || 'default'
}

function getConfidenceColor(confidence: number) {
  if (confidence >= 80) return '#52C41A'
  if (confidence >= 60) return '#FAAD14'
  return '#FF4D4F'
}

// ============================================================
// 生命周期
// ============================================================

onMounted(async () => {
  await loadCategoryTree()
  await loadTerms()
  await loadStats()

  // 点击其他地方关闭右键菜单和全局搜索
  document.addEventListener('click', (e) => {
    categoryContextMenu.visible = false
    const target = e.target as HTMLElement
    if (!target.closest('.global-search-panel') && !target.closest('.ant-input-search')) {
      globalSearchVisible.value = false
    }
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('click', () => {})
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { flex-shrink: 0; }
.header-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.page-title { font-size: 20px; font-weight: 700; color: #1F1F1F; margin: 0 0 4px; }
.page-subtitle { font-size: 13px; color: #8C8C8C; margin: 0; }

.content-wrapper {
  display: flex;
  gap: 16px;
  height: calc(100vh - 240px);
  min-height: 500px;
}

.tree-panel {
  width: 280px;
  flex-shrink: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.panel-title { font-weight: 600; font-size: 14px; color: #1F1F1F; }

.tree-search {
  padding: 8px 12px;
  border-bottom: 1px solid #f5f5f5;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-node-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.tree-node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-node-count {
  margin-left: 8px;
  font-size: 11px;
  color: #722ED1;
  background: #f0e6ff;
  padding: 0 6px;
  border-radius: 10px;
  flex-shrink: 0;
}

.tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.list-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-mini-row {
  display: flex;
  gap: 12px;
}
.stat-mini-card {
  flex: 1;
  border-radius: 8px;
  padding: 16px 20px;
  color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.stat-purple {
  background: linear-gradient(135deg, #722ED1 0%, #9254DE 100%);
}
.mini-value { font-size: 24px; font-weight: 700; }
.mini-label { font-size: 12px; opacity: 0.9; margin-top: 4px; }

.filter-card {
  background: white;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.filter-bar { margin-bottom: 0; }

.table-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  flex: 1;
  overflow: auto;
}

.term-name-cell { display: flex; flex-direction: column; gap: 2px; }
.name-text { font-weight: 500; }
.code-text { font-size: 12px; color: #8C8C8C; font-family: 'JetBrains Mono', monospace; }

.alias-cell { display: flex; flex-direction: column; gap: 2px; }
.alias-item { font-size: 12px; }
.alias-item.en { color: #1677FF; }
.alias-item.alias { color: #8C8C8C; }

.no-data { color: #d9d9d9; }

.empty-state { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 64px; color: #d9d9d9; margin-bottom: 16px; }
.empty-title { font-size: 16px; color: #1F1F1F; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #8C8C8C; margin-bottom: 24px; }

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: white;
  display: flex;
  justify-content: space-between;
}

.form-tip { font-size: 12px; color: #8C8C8C; margin-top: 4px; }

.definition-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #1F1F1F;
}

.code-inline {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.code-block {
  display: block;
  background: #1C1F2E;
  color: #E8F4FF;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  white-space: pre-wrap;
}

.no-mapping-tip {
  text-align: center;
  padding: 30px;
  color: #8C8C8C;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.mapping-list-in-form {
  padding: 0 16px;
}

.no-mapping-tip-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
  color: #8C8C8C;
  font-size: 13px;
}

/* 全局搜索结果面板 */
.global-search-panel {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  width: 560px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.15);
  z-index: 999;
  max-height: 480px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.search-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-label {
  font-size: 13px;
  font-weight: 600;
  color: #1F1F1F;
}

.search-result-list {
  overflow-y: auto;
  max-height: 400px;
}

.search-result-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}

.search-result-item:hover {
  background: #fafafa;
}

.result-name {
  font-weight: 500;
  font-size: 14px;
  color: #1F1F1F;
  margin-bottom: 4px;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.result-code {
  font-size: 11px;
  color: #8C8C8C;
  font-family: 'JetBrains Mono', monospace;
}

/* 导入弹窗 */
.import-modal-content {
  padding: 8px 0;
}

.import-step-content {
  padding: 8px 0;
}

.import-tip-box {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: #f0f7ff;
  border-radius: 8px;
  border: 1px solid #adc6ff;
}

.import-result {
  margin-top: 8px;
}
</style>
