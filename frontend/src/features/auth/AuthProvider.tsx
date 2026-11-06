import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '../../api/auth.api'
import { SESSION_EXPIRED_EVENT } from '../../api/client'
import { sessionStore } from '../../utils/session'
import type { RegisterRequest, User } from '../../types/api'
import { AuthContext, type AuthValue } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(() => sessionStore.get()?.user ?? null)
  const [sessionExpired, setSessionExpired] = useState(false)

  const expire = useCallback(() => {
    sessionStore.clear()
    queryClient.clear()
    setUser(null)
    setSessionExpired(true)
  }, [queryClient])

  // 401 from any request
  useEffect(() => {
    window.addEventListener(SESSION_EXPIRED_EVENT, expire)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expire)
  }, [expire])

  // Sign out when the token's expiry time passes, even if the user is idle.
  useEffect(() => {
    if (!user) return
    const session = sessionStore.get()
    if (!session) return
    const timer = setTimeout(expire, Math.max(session.expiresAt - Date.now(), 0))
    return () => clearTimeout(timer)
  }, [user, expire])

  const login = useCallback<AuthValue['login']>(async (email, password) => {
    const res = await authApi.login({ email, password })
    sessionStore.set({
      token: res.accessToken,
      expiresAt: Date.now() + res.expiresInSeconds * 1000,
      user: res.user,
    })
    setSessionExpired(false)
    setUser(res.user)
    return res.user
  }, [])

  const register = useCallback(
    async (input: RegisterRequest) => {
      await authApi.register(input)
      return login(input.email, input.password)
    },
    [login],
  )

  const logout = useCallback(() => {
    sessionStore.clear()
    queryClient.clear()
    setSessionExpired(false)
    setUser(null)
  }, [queryClient])

  const value = useMemo(
    () => ({ user, sessionExpired, login, register, logout }),
    [user, sessionExpired, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}