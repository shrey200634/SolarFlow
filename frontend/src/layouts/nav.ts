import { LayoutDashboard, FileText, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
}

// Only routes that exist. Each phase adds its own entries.
export const homeownerNav: NavItem[] = [{ to: '/app', label: 'My applications', icon: FileText, end: true }]
export const staffNav: NavItem[] = [{ to: '/staff', label: 'Overview', icon: LayoutDashboard, end: true }]