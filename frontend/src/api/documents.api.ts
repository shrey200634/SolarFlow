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
}