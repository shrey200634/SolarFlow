import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/auth-context'
import { canVisit, homePathFor } from '../features/auth/roles'
import type { Role } from '../types/api'

/** Signed-in users only, optionally limited to certain roles. */
export function RequireAuth({ roles }: { roles?: Role[] }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homePathFor(user.role)} replace />
  }
  return <Outlet />
}

/** Login and register. Once signed in, go back to where the user came from, or their home. */
export function GuestOnly() {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Outlet />

  const from = (location.state as { from?: string } | null)?.from
  const target = from && canVisit(user.role, from) ? from : homePathFor(user.role)
  return <Navigate to={target} replace />
}

export function RoleRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />
}