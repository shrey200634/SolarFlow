import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { LogOut, Menu, Sun, X } from 'lucide-react'
import { useAuth } from '../features/auth/auth-context'
import { Button } from '../components/common/Button'
import type { NavItem } from './nav'

const roleLabel = { HOMEOWNER: 'Homeowner', SALES: 'Sales', ADMIN: 'Admin' } as const

function SidebarContent({ nav, onNavigate }: { nav: NavItem[]; onNavigate?: () => void }) {
  const { user, logout } = useAuth()
  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 items-center gap-2 border-b border-line px-4">
        <span className="flex h-7 w-7 items-center justify-center rounded-md bg-brand text-white">
          <Sun size={16} />
        </span>
        <span className="font-semibold">SolarFlow</span>
      </div>

      <nav className="flex-1 space-y-0.5 p-2">
        {nav.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={onNavigate}
            className={({ isActive }: { isActive: boolean }) =>
              `flex items-center gap-2.5 rounded-md px-3 py-2 text-sm ${
                isActive ? 'bg-brand-soft font-medium text-brand' : 'text-muted hover:bg-canvas hover:text-ink'
              }`
            }
          >
            <Icon size={16} /> {label}
          </NavLink>
        ))}
      </nav>

      {user && (
        <div className="border-t border-line p-3">
          <p className="truncate text-sm font-medium">{user.fullName}</p>
          <p className="truncate text-xs text-muted">{user.email} · {roleLabel[user.role]}</p>
          <Button variant="secondary" className="mt-3 w-full" onClick={logout}>
            <LogOut size={14} /> Sign out
          </Button>
        </div>
      )}
    </div>
  )
}

export function AppShell({ nav }: { nav: NavItem[] }) {
  const [open, setOpen] = useState(false)

  return (
    <div className="min-h-full">
      <aside className="fixed inset-y-0 left-0 hidden w-60 border-r border-line bg-surface lg:block">
        <SidebarContent nav={nav} />
      </aside>

      <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-line bg-surface px-4 lg:hidden">
        <button onClick={() => setOpen(true)} aria-label="Open menu" className="rounded p-1.5 hover:bg-canvas">
          <Menu size={18} />
        </button>
        <span className="font-semibold">SolarFlow</span>
      </header>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-ink/40" onClick={() => setOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-64 bg-surface shadow-lg">
            <button onClick={() => setOpen(false)} aria-label="Close menu" className="absolute right-2 top-3 rounded p-1.5 hover:bg-canvas">
              <X size={16} />
            </button>
            <SidebarContent nav={nav} onNavigate={() => setOpen(false)} />
          </aside>
        </div>
      )}

      <main className="lg:pl-60">
        <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:py-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}