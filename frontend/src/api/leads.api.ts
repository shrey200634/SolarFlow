import { api } from './client'
import type { Lead, LeadStatus, PageResponse } from '../types/api'

export const leadsApi = {
  // Homeowners get only their own leads; staff get all (the backend decides).
  list: (params: { status?: LeadStatus; page?: number; size?: number } = {}) =>
    api.get<PageResponse<Lead>>('/leads', { params }).then((r) => r.data),

  get: (id: string) => api.get<Lead>(`/leads/${id}`).then((r) => r.data),
}