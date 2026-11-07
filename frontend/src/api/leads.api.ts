import { api } from './client'
import type {
  CreateLeadRequest, Lead, LeadListParams, LeadStats, PageResponse, StaffUser, UpdateLeadRequest,
} from '../types/api'

export const leadsApi = {
  list: (params: LeadListParams = {}) =>
    api.get<PageResponse<Lead>>('/leads', { params }).then((r) => r.data),
  get: (id: string) => api.get<Lead>(`/leads/${id}`).then((r) => r.data),
  create: (body: CreateLeadRequest) => api.post<Lead>('/leads', body).then((r) => r.data),
  // Staff only: roof correction after survey (R6), assignment, mark lost / reopen.
  update: (id: string, body: UpdateLeadRequest) => api.patch<Lead>(`/leads/${id}`, body).then((r) => r.data),
  stats: () => api.get<LeadStats>('/leads/stats').then((r) => r.data),
  staffUsers: () => api.get<StaffUser[]>('/users/staff').then((r) => r.data),
}