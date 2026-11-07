import { api } from './client'
import type { QualificationResult, SolarRecommendation } from '../types/api'

export const qualificationApi = {
  latest: (leadId: string) =>
    api.get<QualificationResult>(`/leads/${leadId}/qualification`).then((r) => r.data),

  run: (leadId: string) =>
    api.post<QualificationResult>(`/leads/${leadId}/qualification`).then((r) => r.data),

  recommendations: (leadId: string) =>
    api.get<SolarRecommendation>(`/leads/${leadId}/recommendations`).then((r) => r.data),
}