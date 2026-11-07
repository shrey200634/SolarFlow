import { api } from './client'
import type { PincodeAvailability } from '../types/api'

export const pincodesApi = {
  check: (pincode: string) =>
    api.get<PincodeAvailability>(`/pincodes/${pincode}/serviceable`).then((r) => r.data),
}