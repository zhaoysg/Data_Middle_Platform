import request from '@/utils/request'
import type { Result } from '@/types'

// ============================================================
// 数据资产目录
// 后端路径: /api/gov/asset-catalog（axios baseURL=/api，前端路径写 /gov/asset-catalog）
// ============================================================

// 目录类型
export type CatalogType = 'BUSINESS_DOMAIN' | 'DATA_DOMAIN' | 'ALBUM'

// 目录树节点
export interface AssetCatalogTreeVO {
  id?: number
  parentId?: number
  catalogType?: CatalogType
  catalogTypeLabel?: string
  catalogName?: string
  catalogCode?: string
  catalogDesc?: string
  coverImage?: string
  itemCount?: number
  accessCount?: number
  ownerId?: number
  ownerName?: string
  sortOrder?: number
  visible?: number
  visibleLabel?: string
  status?: string
  statusLabel?: string
  createUser?: number
  createUserName?: string
  createTime?: string
  updateUser?: number
  updateTime?: string
  albumDesc?: string
  children?: AssetCatalogTreeVO[]
}

// 目录详情
export interface AssetCatalogDetailVO extends AssetCatalogTreeVO {
  metadataIds?: number[]
  assets?: CatalogAssetVO[]
}

// 目录下收录的资产
export interface CatalogAssetVO {
  metadataId?: number
  dsId?: number
  dsName?: string
  dsType?: string
  dataLayer?: string
  dataDomain?: string
  tableName?: string
  tableAlias?: string
  tableComment?: string
  tableType?: string
  rowCount?: number
  storageBytes?: number
  sensitivityLevel?: string
  sensitivityLevelLabel?: string
  lastModifiedAt?: string
  createTime?: string
}

// 资产目录统计
export interface AssetCatalogStatsVO {
  totalCount?: number
  businessDomainCount?: number
  dataDomainCount?: number
  albumCount?: number
  totalAssetCount?: number
  assetCountByType?: Record<string, number>
  assetCountByLayer?: Record<string, number>
}

// 新增/编辑目录
export interface AssetCatalogSaveDTO {
  id?: number
  parentId?: number
  catalogType?: CatalogType
  catalogName?: string
  catalogCode?: string
  catalogDesc?: string
  coverImage?: string
  ownerId?: number
  sortOrder?: number
  visible?: number
  status?: string
  albumDesc?: string
  metadataIds?: number[]
  deptId?: number
  createUser?: number
}

// 查询参数
export interface AssetCatalogQueryDTO {
  catalogType?: CatalogType
  catalogName?: string
  catalogCode?: string
  visible?: number
  status?: string
  ownerId?: number
  deptId?: number
  topLevelOnly?: boolean
}

export const assetCatalogApi = {
  // 目录树
  getTree(params?: AssetCatalogQueryDTO) {
    return request.get<Result<AssetCatalogTreeVO[]>>('/gov/asset-catalog/tree', { params })
  },

  getFullTree(params?: AssetCatalogQueryDTO) {
    return request.get<Result<AssetCatalogTreeVO[]>>('/gov/asset-catalog/tree/full', { params })
  },

  // 分页查询
  page(params: {
    pageNum: number
    pageSize: number
    catalogType?: CatalogType
    catalogName?: string
    catalogCode?: string
    visible?: number
    status?: string
    ownerId?: number
    deptId?: number
    topLevelOnly?: boolean
  }) {
    return request.get<Result>('/gov/asset-catalog/page', { params })
  },

  // 详情
  getById(id: number) {
    return request.get<Result<AssetCatalogDetailVO>>(`/gov/asset-catalog/${id}`)
  },

  getDetail(id: number) {
    return request.get<Result<AssetCatalogDetailVO>>(`/gov/asset-catalog/detail/${id}`)
  },

  // 新增
  create(data: AssetCatalogSaveDTO) {
    return request.post<Result<number>>('/gov/asset-catalog', data)
  },

  // 批量新增
  batchCreate(data: AssetCatalogSaveDTO[]) {
    return request.post<Result>('/gov/asset-catalog/batch', data)
  },

  // 更新
  update(id: number, data: AssetCatalogSaveDTO) {
    return request.put<Result>(`/gov/asset-catalog/${id}`, data)
  },

  // 移动目录
  move(id: number, parentId: number) {
    return request.put<Result>(`/gov/asset-catalog/${id}/move`, null, {
      params: { parentId }
    })
  },

  // 删除
  delete(id: number) {
    return request.delete<Result>(`/gov/asset-catalog/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]) {
    return request.delete<Result>('/gov/asset-catalog/batch', { data: ids })
  },

  // 统计
  getStats() {
    return request.get<Result<AssetCatalogStatsVO>>('/gov/asset-catalog/stats')
  },

  // 刷新目录数量
  refreshCount(id: number) {
    return request.post<Result>(`/gov/asset-catalog/${id}/refresh-count`)
  },

  // 收录资产
  addAssets(catalogId: number, metadataIds: number[]) {
    return request.post<Result>(`/gov/asset-catalog/${catalogId}/assets`, metadataIds)
  },

  // 移除资产
  removeAssets(catalogId: number, metadataIds: number[]) {
    return request.delete<Result>(`/gov/asset-catalog/${catalogId}/assets`, { data: metadataIds })
  },

  // 获取目录下资产列表
  getAssets(catalogId: number, params?: {
    pageNum?: number
    pageSize?: number
  }) {
    return request.get<Result>(`/gov/asset-catalog/${catalogId}/assets`, { params })
  },

  // 获取资产所属目录
  getCatalogsForMetadata(metadataId: number) {
    return request.get<Result<AssetCatalogTreeVO[]>>(`/gov/asset-catalog/metadata/${metadataId}/catalogs`)
  }
}
