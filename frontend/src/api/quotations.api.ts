import { api } from './client'
import type { Quotation } from '../types/api'

export const quotationsApi = {
  listForLead: (leadId: string) =>
    api.get<Quotation[]>(`/leads/${leadId}/quotations`).then((r) => r.data),

  // Staff only. selectedCapacityKw must equal one of the sizing options exactly.
  create: (leadId: string, selectedCapacityKw: number) =>
    api.post<Quotation>(`/leads/${leadId}/quotations`, { selectedCapacityKw }).then((r) => r.data),

  accept: (quotationId: string) =>
    api.post<Quotation>(`/quotations/${quotationId}/accept`).then((r) => r.data),

  pdf: (quotationId: string) =>
    api.get<Blob>(`/quotations/${quotationId}/pdf`, { responseType: 'blob' }).then((r) => r.data),
}