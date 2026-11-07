export type Role = 'HOMEOWNER' | 'SALES' | 'ADMIN'

export interface User {
  id: string
  fullName: string
  email: string
  role: Role
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export interface RegisterRequest {
  fullName: string
  email: string
  phone?: string
  password: string
  consent: boolean
}

// Matches backend common/ApiError
export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  fieldErrors: Record<string, string> | null
}
// ---- Leads (matches backend lead.controller.LeadResponseDTO and common.PageResponse) ----
export type Ownership = 'OWNED' | 'RENTED' | 'LEASED'
export type RoofType = 'RCC_FLAT' | 'TIN_SHEET' | 'TILED' | 'ASBESTOS' | 'OTHER'
export type LeadStatus =
  | 'NEW'
  | 'BILL_UPLOADED'
  | 'BILL_CONFIRMED'
  | 'QUALIFIED'
  | 'SALES_CALL_PENDING'
  | 'SURVEY_RECOMMENDED'
  | 'QUOTED'
  | 'ACCEPTED'
  | 'LOST'

export interface Lead {
  id: string
  ownerId: string
  ownerName: string
  ownerEmail: string
  assignedToId: string | null
  assignedToName: string | null
  ownership: Ownership
  roofType: RoofType
  roofAreaSqft: number
  declaredMonthlyBill: number
  pincode: string
  phone: string
  status: LeadStatus
  duplicate: boolean
  qualificationScore: number | null // staff only: never show the score to homeowners
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateLeadRequest {
  ownership: Ownership
  roofType: RoofType
  roofAreaSqft: number
  declaredMonthlyBill: number
  pincode: string
  phone: string
}

export interface PincodeAvailability {
  pincode: string
  serviceable: boolean
}