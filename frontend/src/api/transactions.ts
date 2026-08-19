import { apiClient } from '@/api/client'
import type { Transaction, TransactionRequest } from '@/types'

export const transactionsApi = {
  list: (portfolioId: string) =>
    apiClient.get<Transaction[]>(`/portfolios/${portfolioId}/transactions`).then((r) => r.data),
  create: (portfolioId: string, data: TransactionRequest) =>
    apiClient.post<Transaction>(`/portfolios/${portfolioId}/transactions`, data).then((r) => r.data),
}
