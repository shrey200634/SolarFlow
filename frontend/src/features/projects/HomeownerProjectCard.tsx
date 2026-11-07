import { useQuery } from '@tanstack/react-query'
import { projectsApi } from '../../api/projects.api'
import { Card } from '../../components/common/Card'
import { formatINR } from '../leads/labels'
import { ProjectTimeline } from './ProjectTimeline'
import { StatusLinkBox } from './StatusLinkBox'
import type { ProjectStatus } from '../../types/api'

/** Shown on the homeowner's application page once the quotation is accepted and staff create the project. */
export function HomeownerProjectCard({ leadId }: { leadId: string }) {
  const projects = useQuery({ queryKey: ['projects', { leadId }], queryFn: () => projectsApi.list(leadId) })
  const p = projects.data?.[0]

  if (projects.data && !p) {
    return (
      <Card className="mb-5 p-4 sm:p-5">
        <h2 className="font-semibold">Installation</h2>
        <p className="mt-1 text-sm text-muted">Our team will contact you to collect the advance and schedule installation.</p>
      </Card>
    )
  }
  if (!p) return null

  const reachedAt: Partial<Record<ProjectStatus, string>> = {}
  p.history.forEach((h) => { reachedAt[h.toStatus] = h.changedAt })

  return (
    <Card className="mb-5 p-4 sm:p-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="font-semibold">Installation progress</h2>
        <span className="text-sm text-muted">
          {p.capacityKw} kW · advance {formatINR(Math.round(p.advanceAmount))} of {formatINR(Math.round(p.netSystemCost))}
        </span>
      </div>
      <div className="mt-4"><ProjectTimeline current={p.status} reachedAt={reachedAt} showHints /></div>
      {p.statusLink && (
        <div className="mt-5 border-t border-line pt-4">
          <p className="mb-2 text-sm font-medium">Share progress with family</p>
          <StatusLinkBox statusLink={p.statusLink} />
        </div>
      )}
    </Card>
  )
}