import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Sun } from 'lucide-react'

import { projectsApi } from '../api/projects.api'
import { parseError } from '../api/client'
import { Card } from '../components/common/Card'
import { Spinner } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { formatDate } from '../features/leads/labels'
import { projectStageLabel } from '../features/projects/labels'
import { ProjectTimeline } from '../features/projects/ProjectTimeline'
import type { ProjectStatus } from '../types/api'

/** Public, no login. Shows only what the backend's minimal DTO returns. */
export default function PublicStatusPage() {
  const { token } = useParams<{ token: string }>()
  const status = useQuery({
    queryKey: ['public-status', token],
    queryFn: () => projectsApi.publicStatus(token!),
    enabled: Boolean(token),
    retry: false,
    refetchInterval: 60_000,
  })

  const reachedAt: Partial<Record<ProjectStatus, string>> = {}
  status.data?.timeline.forEach((t) => { reachedAt[t.status] = t.at })

  return (
    <div className="min-h-full bg-canvas px-4 py-10">
      <div className="mx-auto max-w-lg">
        <div className="mb-6 flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-md bg-brand text-white"><Sun size={18} /></span>
          <span className="text-lg font-semibold">SolarFlow</span>
        </div>

        {status.isPending && <Spinner label="Loading status..." />}

        {status.isError && (
          <Card className="p-5">
            <Alert tone={parseError(status.error).status === 404 ? 'warning' : 'error'} title="Status not available">
              {parseError(status.error).status === 404
                ? 'This link is invalid or has been turned off. Contact your SolarFlow representative for a new one.'
                : parseError(status.error).message}
            </Alert>
          </Card>
        )}

        {status.data && (
          <Card className="p-5 sm:p-6">
            <p className="text-sm text-muted">Your rooftop solar installation</p>
            <h1 className="mt-1 text-2xl font-semibold">{status.data.capacityKw} kW system</h1>
            <p className="mt-3 rounded-md bg-brand-soft px-3 py-2 text-sm text-brand">
              <span className="font-medium">{projectStageLabel[status.data.currentStatus].label}.</span>{' '}
              {projectStageLabel[status.data.currentStatus].hint}
            </p>

            <div className="mt-6">
              <ProjectTimeline current={status.data.currentStatus} reachedAt={reachedAt} />
            </div>

            <p className="mt-6 border-t border-line pt-3 text-xs text-muted">
              Started {formatDate(status.data.startedAt)} · last updated {formatDate(status.data.lastUpdatedAt)}.
              This page refreshes automatically.
            </p>
          </Card>
        )}
      </div>
    </div>
  )
}