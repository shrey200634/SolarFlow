import { api } from './client'
import type {
  CreatePricingRuleRequest,
  CreateSubsidyRuleRequest,
  PricingRule,
  ServiceablePincode,
  SubsidyRule,
} from '../types/api'

// All /admin endpoints are ADMIN only. Rows are never deleted, only deactivated.
export const adminApi = {
  pincodes: () => api.get<ServiceablePincode[]>('/admin/pincodes').then((r) => r.data),
  addPincode: (body: { pincode: string; areaName?: string }) =>
    api.post<ServiceablePincode>('/admin/pincodes', body).then((r) => r.data),
  setPincodeActive: (id: string, active: boolean) =>
    api.patch<ServiceablePincode>(`/admin/pincodes/${id}`, { active }).then((r) => r.data),

  pricingRules: () => api.get<PricingRule[]>('/admin/pricing-rules').then((r) => r.data),
  addPricingRule: (body: CreatePricingRuleRequest) =>
    api.post<PricingRule>('/admin/pricing-rules', body).then((r) => r.data),
  setPricingRuleActive: (id: string, active: boolean) =>
    api.patch<PricingRule>(`/admin/pricing-rules/${id}`, { active }).then((r) => r.data),

  subsidyRules: () => api.get<SubsidyRule[]>('/admin/subsidy-rules').then((r) => r.data),
  addSubsidyRule: (body: CreateSubsidyRuleRequest) =>
    api.post<SubsidyRule>('/admin/subsidy-rules', body).then((r) => r.data),
  setSubsidyRuleActive: (id: string, active: boolean) =>
    api.patch<SubsidyRule>(`/admin/subsidy-rules/${id}`, { active }).then((r) => r.data),
}