import { FileText, Hammer, LayoutDashboard, MapPin, PlusCircle, Receipt, type LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
}

export const homeownerNav: NavItem[] = [
  { to: '/app', label: 'My applications', icon: FileText, end: true },
  { to: '/app/new', label: 'New enquiry', icon: PlusCircle },
]

export const staffNav: NavItem[] = [
  { to: '/staff', label: 'Leads', icon: LayoutDashboard, end: true },
  { to: '/staff/projects', label: 'Projects', icon: Hammer },
]

// ADMIN sees the sales screens plus configuration.
export const adminNav: NavItem[] = [
  ...staffNav,
  { to: '/admin/pincodes', label: 'Pincodes', icon: MapPin },
  { to: '/admin/rules', label: 'Pricing & subsidy', icon: Receipt },
]