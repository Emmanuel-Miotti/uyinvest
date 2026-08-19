import { apiClient } from '@/api/client'
import type { Asset, AssetRequest, AssetType, Page } from '@/types'

export interface AssetSearchParams {
  type?: AssetType
  search?: string
  page?: number
  size?: number
  sort?: string
}

export const assetsApi = {
  search: (params: AssetSearchParams) => apiClient.get<Page<Asset>>('/assets', { params }).then((r) => r.data),
  get: (id: string) => apiClient.get<Asset>(`/assets/${id}`).then((r) => r.data),
  create: (data: AssetRequest) => apiClient.post<Asset>('/assets', data).then((r) => r.data),
  update: (id: string, data: AssetRequest) => apiClient.put<Asset>(`/assets/${id}`, data).then((r) => r.data),
}
