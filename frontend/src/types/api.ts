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

// ---- Quotations (matches backend quotation.dto.QuotationResponseDTO and QuotationSnapshot) ----
export type QuotationStatus = 'ISSUED' | 'ACCEPTED'

export interface PricingCalculation {
  systemCapacityKw: number
  pricePerKw: number
  grossSystemCost: number
  subsidyAmount: number
  netSystemCost: number
  annualGenerationUnits: number
  electricityTariffPerUnit: number
  estimatedAnnualSavings: number
  estimatedPaybackYears: number | null
  subsidyEstimated: boolean
  calculationDate: string
}

export interface QuotationSnapshot {
  quotationDate: string
  selectedOptionType: string
  selectedCapacityKw: number
  pricing: PricingCalculation
  subsidyDisclaimer: string | null
  pricingDisclaimer: string | null
}

export interface Quotation {
  id: string
  leadId: string
  selectedCapacityKw: number
  selectedOptionType: string
  status: QuotationStatus
  createdAt: string
  acceptedAt: string | null
  snapshot: QuotationSnapshot
}

// ---- Staff: qualification, recommendations (match backend DTOs) ----
export type NextAction = 'SURVEY_RECOMMENDED' | 'SALES_CALL_PENDING'

export interface QualificationReason {
  code: string
  points: number
  maxPoints: number
  passed: boolean
  text: string
}

export interface QualificationGateResult {
  code: string
  triggered: boolean
  text: string
}

export interface QualificationResult {
  id: string
  leadId: string
  score: number
  nextAction: NextAction
  ruleVersion: string
  rules: QualificationReason[]
  gates: QualificationGateResult[]
  createdAt: string
}

export interface SolarOption {
  type: string // RECOMMENDED | ECONOMY | MAXIMUM
  capacityKw: number
  estimatedYearlyGeneration: number
}

export interface SolarRecommendation {
  monthlyUnits: number
  roofAreaSqft: number
  sanctionedLoadKw: number | null
  usageLimitKw: number
  roofLimitKw: number
  recommendedCapacityKw: number
  limitingFactor: string
  viable: boolean
  message: string | null
  options: SolarOption[]
}

export interface LeadStats {
  total: number
  byStatus: Partial<Record<LeadStatus, number>>
}

export interface LeadListParams {
  status?: LeadStatus
  pincode?: string
  q?: string
  assignee?: string
  page?: number
  size?: number
}

// ---- Lead update (staff) ----
export interface UpdateLeadRequest {
  assignedTo?: string
  status?: LeadStatus // backend only allows LOST, or LOST → SALES_CALL_PENDING (reopen)
  ownership?: Ownership
  roofType?: RoofType
  roofAreaSqft?: number
}

export interface StaffUser { id: string; fullName: string; email: string; role: Role }

// ---- Projects ----
export type ProjectStatus = 'ADVANCE_PAID' | 'INSTALLATION_SCHEDULED' | 'INSTALLED' | 'SUBSIDY_CREDITED'

export interface ProjectHistoryEntry {
  fromStatus: ProjectStatus | null
  toStatus: ProjectStatus
  note: string | null // null for homeowners: staff notes stay internal
  changedAt: string
}

export interface Project {
  id: string
  quotationId: string
  leadId: string
  status: ProjectStatus
  capacityKw: number
  netSystemCost: number
  advanceAmount: number
  statusLink: string | null // "/api/v1/status/{token}", null when revoked
  createdAt: string
  updatedAt: string
  history: ProjectHistoryEntry[]
}

export interface PublicProjectStatus {
  currentStatus: ProjectStatus
  capacityKw: number
  startedAt: string
  lastUpdatedAt: string
  timeline: { status: ProjectStatus; at: string }[]
}

// ---- Admin ----
export interface ServiceablePincode { id: string; pincode: string; areaName: string | null; active: boolean; createdAt: string }

export interface PricingRule {
  id: string
  pricePerKw: number
  electricityTariffPerUnit: number
  effectiveFrom: string // "YYYY-MM-DD"
  effectiveTo: string | null
  active: boolean
}
export interface CreatePricingRuleRequest {
  pricePerKw: number
  electricityTariffPerUnit: number
  effectiveFrom: string
  effectiveTo?: string | null
}

export interface SubsidyRule {
  id: string
  subsidyPerKwFirstTwoKw: number
  subsidyForThirdKw: number
  maximumSubsidy: number
  effectiveFrom: string
  effectiveTo: string | null
  active: boolean
  estimated: boolean
}
export interface CreateSubsidyRuleRequest {
  subsidyPerKwFirstTwoKw: number
  subsidyForThirdKw: number
  maximumSubsidy: number
  effectiveFrom: string
  effectiveTo?: string | null
  estimated: boolean
}