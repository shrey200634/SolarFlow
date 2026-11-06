import { createContext, useContext } from 'react'
import type { RegisterRequest, User } from '../../types/api'

export interface AuthValue {
  user: User | null
  sessionExpired: boolean
  login: (email: string, password: string) => Promise<User>
  register: (input: RegisterRequest) => Promise<User>
  logout: () => void
}

export const AuthContext = createContext<AuthValue | null>(null)

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}