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