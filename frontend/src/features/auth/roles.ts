import type { Role } from '../../types/api'

export function homePathFor(role: Role): string {
  return role === 'HOMEOWNER' ? '/app' : '/staff'
}

/** Which URL prefixes each role may open. ADMIN also gets the staff area (backend allows it). */
const allowed: Record<Role, string[]> = {
  HOMEOWNER: ['/app'],
  SALES: ['/staff'],
  ADMIN: ['/staff', '/admin'],
}

export function canVisit(role: Role, path: string): boolean {
  return allowed[role].some((p) => path === p || path.startsWith(p + '/'))
}