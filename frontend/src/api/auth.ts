import { apiClient } from '@/api/client'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '@/types'

export const authApi = {
  register: (data: RegisterRequest) => apiClient.post<AuthResponse>('/auth/register', data).then((r) => r.data),
  login: (data: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  me: () => apiClient.get<User>('/auth/me').then((r) => r.data),
}
