import { api } from './client'
import type { Quotation } from '../types/api'

export const quotationsApi = {
  // Newest first. A homeowner only ever gets their own lead's quotations.
  listForLead: (leadId: string) =>
    api.get<Quotation[]>(`/leads/${leadId}/quotations`).then((r) => r.data),

  accept: (quotationId: string) =>
    api.post<Quotation>(`/quotations/${quotationId}/accept`).then((r) => r.data),

  pdf: (quotationId: string) =>
    api.get<Blob>(`/quotations/${quotationId}/pdf`, { responseType: 'blob' }).then((r) => r.data),
}