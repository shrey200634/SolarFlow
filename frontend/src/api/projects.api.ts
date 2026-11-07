import { api } from './client'
import type { Project, ProjectStatus, PublicProjectStatus } from '../types/api'

export const projectsApi = {
  // Staff get all (latest 100); homeowners only their own. Optional lead filter.
  list: (leadId?: string) =>
    api.get<Project[]>('/projects', { params: leadId ? { leadId } : {} }).then((r) => r.data),

  get: (id: string) => api.get<Project>(`/projects/${id}`).then((r) => r.data),

  create: (body: { quotationId: string; advanceAmount: number }) =>
    api.post<Project>('/projects', body).then((r) => r.data),

  // Forward-only: backend rejects anything other than the next stage with 409.
  advance: (id: string, body: { status: ProjectStatus; note?: string }) =>
    api.post<Project>(`/projects/${id}/status`, body).then((r) => r.data),

  setStatusLink: (id: string, active: boolean) =>
    api.patch<Project>(`/projects/${id}/status-link`, { active }).then((r) => r.data),

  // Public, no login. The token is the only key.
  publicStatus: (token: string) =>
    api.get<PublicProjectStatus>(`/status/${encodeURIComponent(token)}`).then((r) => r.data),
}