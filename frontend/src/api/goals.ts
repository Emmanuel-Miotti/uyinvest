import { apiClient } from '@/api/client'
import type { Goal, GoalRequest } from '@/types'

export const goalsApi = {
  list: () => apiClient.get<Goal[]>('/goals').then((r) => r.data),
  get: (id: string) => apiClient.get<Goal>(`/goals/${id}`).then((r) => r.data),
  create: (data: GoalRequest) => apiClient.post<Goal>('/goals', data).then((r) => r.data),
  update: (id: string, data: GoalRequest) => apiClient.put<Goal>(`/goals/${id}`, data).then((r) => r.data),
  remove: (id: string) => apiClient.delete(`/goals/${id}`).then(() => undefined),
}
