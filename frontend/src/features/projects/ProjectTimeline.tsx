import { Check } from 'lucide-react'
import type { ProjectStatus } from '../../types/api'
import { formatDateTime, projectStageLabel, projectStages } from './labels'

interface Props {
  current: ProjectStatus
  /** When each stage was reached (from history / public timeline). */
  reachedAt: Partial<Record<ProjectStatus, string>>
  /** Optional staff notes per stage. */
  notes?: Partial<Record<ProjectStatus, string | null>>
  showHints?: boolean
}

/** Vertical stepper used by staff, homeowner and the public status page. */
export function ProjectTimeline({ current, reachedAt, notes, showHints }: Props) {
  const currentIdx = projectStages.indexOf(current)
  return (
    <ol className="relative">
      {projectStages.map((stage, i) => {
        const done = i <= currentIdx
        const isCurrent = i === currentIdx
        const last = i === projectStages.length - 1
        return (
          <li key={stage} className="relative flex gap-3 pb-6 last:pb-0">
            {!last && (
              <span
                aria-hidden
                className={`absolute left-[11px] top-6 h-[calc(100%-1.5rem)] w-0.5 ${i < currentIdx ? 'bg-brand' : 'bg-line'}`}
              />
            )}
            <span
              className={`relative z-10 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 text-xs font-semibold ${
                done ? 'border-brand bg-brand text-white' : 'border-line bg-surface text-muted'
              } ${isCurrent ? 'ring-4 ring-brand-soft' : ''}`}
            >
              {done ? <Check size={13} strokeWidth={3} /> : i + 1}
            </span>
            <div className="min-w-0 pt-0.5">
              <p className={`text-sm ${done ? 'font-medium' : 'text-muted'}`}>
                {projectStageLabel[stage].label}
                {isCurrent && <span className="ml-2 text-xs font-normal text-brand">Current</span>}
              </p>
              {reachedAt[stage] && <p className="text-xs text-muted">{formatDateTime(reachedAt[stage]!)}</p>}
              {showHints && isCurrent && <p className="mt-1 text-sm text-muted">{projectStageLabel[stage].hint}</p>}
              {notes?.[stage] && (
                <p className="mt-1 rounded border border-line bg-canvas px-2 py-1 text-xs">{notes[stage]}</p>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}