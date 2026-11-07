import { LayoutDashboard, FileText, PlusCircle, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
}

// Only routes that exist. Each phase adds its own entries.
export const homeownerNav: NavItem[] = [
  { to: '/app', label: 'My applications', icon: FileText, end: true },
  { to: '/app/new', label: 'New enquiry', icon: PlusCircle },
]

export const staffNav: NavItem[] = [{ to: '/staff', label: 'Leads', icon: LayoutDashboard, end: true }]