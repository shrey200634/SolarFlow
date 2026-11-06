import { api } from './client'
import type { LoginResponse, RegisterRequest, User } from '../types/api'

export const authApi = {
  login: (body: { email: string; password: string }) =>
    api.post<LoginResponse>('/auth/login', body).then((r) => r.data),

  register: (body: RegisterRequest) => api.post<User>('/auth/register', body).then((r) => r.data),

  me: () => api.get<User>('/users/me').then((r) => r.data),
}