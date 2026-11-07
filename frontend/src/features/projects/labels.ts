import type { ProjectStatus } from '../../types/api'

export const projectStages: ProjectStatus[] = [
  'ADVANCE_PAID',
  'INSTALLATION_SCHEDULED',
  'INSTALLED',
  'SUBSIDY_CREDITED',
]

export const projectStageLabel: Record<ProjectStatus, { label: string; hint: string }> = {
  ADVANCE_PAID: { label: 'Advance paid', hint: 'Payment received. Materials are being arranged.' },
  INSTALLATION_SCHEDULED: { label: 'Installation scheduled', hint: 'Our team has a date for your installation.' },
  INSTALLED: { label: 'Installed', hint: 'Your system is installed. Subsidy paperwork is in progress.' },
  SUBSIDY_CREDITED: { label: 'Subsidy credited', hint: 'Subsidy has been credited. Project complete.' },
}

/** Mirrors ProjectStatus.next() on the backend. */
export function nextStage(s: ProjectStatus): ProjectStatus | null {
  const i = projectStages.indexOf(s)
  return i >= 0 && i + 1 < projectStages.length ? projectStages[i + 1] : null
}

/** Backend returns an API path ("/api/v1/status/{token}"); customers need the frontend page. */
export function publicStatusUrl(statusLink: string | null): string | null {
  if (!statusLink) return null
  const token = statusLink.split('/').filter(Boolean).pop()
  return token ? `${window.location.origin}/status/${token}` : null
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit',
  })
}

/** For LocalDate strings ("2026-11-07"): avoid timezone shifts from new Date(). */
export function formatDay(day: string | null): string {
  if (!day) return 'Open-ended'
  const [y, m, d] = day.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function todayISO(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Is a dated rule in force today? (inclusive, open-ended end allowed) */
export function inForceToday(from: string, to: string | null): boolean {
  const t = todayISO()
  return from <= t && (to === null || t <= to)
}