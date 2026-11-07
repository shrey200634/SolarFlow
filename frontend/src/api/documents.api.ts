import { api } from './client'

export type DocumentStatus =
  | 'UPLOADED'
  | 'EXTRACTING'
  | 'EXTRACTED'
  | 'LOW_QUALITY'
  | 'FAILED'
  | 'CONFIRMED'

export interface LeadDocument {
  id: string
  leadId: string
  contentType: string
  sizeBytes: number
  status: DocumentStatus
  createdAt: string
}

/** Each extracted field is { value, confidence }, each confirmed field is { value, source }. */
export interface ExtractedField {
  value: string | number | null
  confidence?: number | null
}

export interface ConfirmedField {
  value: string | number | null
  source: 'AI' | 'CUSTOMER' | 'SALES'
}

export type BillFieldKey = 'consumerNumber' | 'tariff' | 'sanctionedLoadKw' | 'monthlyUnits'

export interface Extraction {
  documentId: string
  status: DocumentStatus
  provider: string | null
  extractedFields: Partial<Record<BillFieldKey, ExtractedField>>
  confidence: Record<string, unknown>
  confirmedFields: Partial<Record<BillFieldKey, ConfirmedField>>
  errorCode: string | null
}

/** Send only the fields the person changed. Omitted fields keep the AI value. */
export interface ConfirmBillRequest {
  consumerNumber?: string
  tariff?: string
  sanctionedLoadKw?: number
  monthlyUnits?: number
}

export const documentsApi = {
  upload: (leadId: string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)

    return api
      .post<LeadDocument>(`/leads/${leadId}/documents`, formData)
      .then((response) => response.data)
  },

  list: (leadId: string) =>
    api.get<LeadDocument[]>(`/leads/${leadId}/documents`).then((response) => response.data),

  extraction: (documentId: string) =>
    api.get<Extraction>(`/documents/${documentId}/extraction`).then((r) => r.data),

  confirm: (documentId: string, body: ConfirmBillRequest) =>
    api.post<Extraction>(`/documents/${documentId}/extraction/confirm`, body).then((r) => r.data),

  file: (documentId: string) =>
    api.get<Blob>(`/documents/${documentId}/file`, { responseType: 'blob' }).then((r) => r.data),
}