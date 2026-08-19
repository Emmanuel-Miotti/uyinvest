import { apiClient } from '@/api/client'
import type { Dividend, DividendRequest, DividendSummary } from '@/types'

export interface DividendFilters {
  assetId?: string
  from?: string
  to?: string
}

export const dividendsApi = {
  list: (portfolioId: string, filters: DividendFilters = {}) =>
    apiClient.get<Dividend[]>(`/portfolios/${portfolioId}/dividends`, { params: filters }).then((r) => r.data),
  create: (portfolioId: string, data: DividendRequest) =>
    apiClient.post<Dividend>(`/portfolios/${portfolioId}/dividends`, data).then((r) => r.data),
  summary: (portfolioId: string) =>
    apiClient.get<DividendSummary>(`/portfolios/${portfolioId}/dividends/summary`).then((r) => r.data),
}
