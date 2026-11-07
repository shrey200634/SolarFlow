import { api } from './client'
import type {
  CreateLeadRequest,
  Lead,
  LeadListParams,
  LeadStats,
  PageResponse,
} from '../types/api'

export const leadsApi = {
  list: (params: LeadListParams = {}) =>
    api.get<PageResponse<Lead>>('/leads', { params }).then((r) => r.data),

  get: (id: string) => api.get<Lead>(`/leads/${id}`).then((r) => r.data),

  create: (body: CreateLeadRequest) => api.post<Lead>('/leads', body).then((r) => r.data),

  stats: () => api.get<LeadStats>('/leads/stats').then((r) => r.data),
}