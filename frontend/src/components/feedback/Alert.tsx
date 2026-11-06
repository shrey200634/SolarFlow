import type { ReactNode } from 'react'
import { AlertCircle, AlertTriangle, CheckCircle2, Info } from 'lucide-react'

type Tone = 'error' | 'warning' | 'success' | 'info'

const tones: Record<Tone, { box: string; icon: typeof Info }> = {
  error: { box: 'border-danger/30 bg-danger-soft text-danger', icon: AlertCircle },
  warning: { box: 'border-warning/30 bg-warning-soft text-warning', icon: AlertTriangle },
  success: { box: 'border-success/30 bg-success-soft text-success', icon: CheckCircle2 },
  info: { box: 'border-info/30 bg-info-soft text-info', icon: Info },
}

export function Alert({ tone = 'info', title, children }: { tone?: Tone; title?: string; children?: ReactNode }) {
  const { box, icon: I } = tones[tone]
  return (
    <div role={tone === 'error' ? 'alert' : undefined} className={`flex gap-2 rounded-md border px-3 py-2.5 text-sm ${box}`}>
      <I size={16} className="mt-0.5 shrink-0" />
      <div>
        {title && <p className="font-medium">{title}</p>}
        {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
      </div>
    </div>
  )
}