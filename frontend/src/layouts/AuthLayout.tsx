import type { ReactNode } from 'react'
import { Sun } from 'lucide-react'
import { Card } from '../components/common/Card'

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex min-h-full items-center justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex items-center justify-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand text-white">
            <Sun size={18} />
          </span>
          <span className="text-lg font-semibold">SolarFlow</span>
        </div>
        <Card className="p-6">
          <h1 className="mb-5 text-lg font-semibold">{title}</h1>
          {children}
        </Card>
      </div>
    </div>
  )
}