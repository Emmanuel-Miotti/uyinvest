import { apiClient } from '@/api/client'
import type { Allocation, PerformancePoint, Portfolio, PortfolioRequest, PortfolioSummary } from '@/types'

export const portfoliosApi = {
  list: () => apiClient.get<Portfolio[]>('/portfolios').then((r) => r.data),
  get: (id: string) => apiClient.get<Portfolio>(`/portfolios/${id}`).then((r) => r.data),
  create: (data: PortfolioRequest) => apiClient.post<Portfolio>('/portfolios', data).then((r) => r.data),
  update: (id: string, data: PortfolioRequest) => apiClient.put<Portfolio>(`/portfolios/${id}`, data).then((r) => r.data),
  remove: (id: string) => apiClient.delete(`/portfolios/${id}`).then(() => undefined),
  summary: (id: string) => apiClient.get<PortfolioSummary>(`/portfolios/${id}/summary`).then((r) => r.data),
  allocation: (id: string) => apiClient.get<Allocation[]>(`/portfolios/${id}/allocation`).then((r) => r.data),
  performance: (id: string) => apiClient.get<PerformancePoint[]>(`/portfolios/${id}/performance`).then((r) => r.data),
}
