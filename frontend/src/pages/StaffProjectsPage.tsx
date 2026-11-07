import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'

import { projectsApi } from '../api/projects.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Badge } from '../components/common/Badge'
import { Button } from '../components/common/Button'
import { EmptyState } from '../components/common/EmptyState'
import { Skeleton } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { formatDate, formatINR } from '../features/leads/labels'
import { projectStageLabel, projectStages } from '../features/projects/labels'
import type { ProjectStatus } from '../types/api'

const DAY = 86_400_000

export default function StaffProjectsPage() {
  const [stage, setStage] = useState<ProjectStatus | ''>('')
  const [now] = useState(() => Date.now()) // fixed per visit, for the idle-days badge
  const projects = useQuery({ queryKey: ['projects', 'all'], queryFn: () => projectsApi.list() })

  const all = projects.data ?? []
  const rows = stage ? all.filter((p) => p.status === stage) : all
  const count = (s: ProjectStatus) => all.filter((p) => p.status === s).length

  return (
    <>
      <PageHeader title="Projects" description="Accepted quotations moving to installation and subsidy credit. Latest 100." />

      <div className="mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {projectStages.map((s, i) => (
          <button
            key={s}
            type="button"
            onClick={() => setStage(stage === s ? '' : s)}
            className={`rounded-lg border p-3 text-left ${stage === s ? 'border-brand bg-brand-soft' : 'border-line bg-surface hover:bg-canvas'}`}
          >
            <p className="text-xs text-muted">Stage {i + 1}</p>
            <p className="font-medium">{projectStageLabel[s].label}</p>
            <p className="mt-1 text-2xl font-semibold">{projects.data ? count(s) : '–'}</p>
          </button>
        ))}
      </div>

      {projects.isPending && <div className="space-y-2"><Skeleton className="h-12 w-full" /><Skeleton className="h-12 w-full" /></div>}
      {projects.isError && (
        <Card className="p-4">
          <Alert tone="error" title="Could not load projects">{parseError(projects.error).message}</Alert>
          <Button variant="secondary" className="mt-3" onClick={() => projects.refetch()}>Try again</Button>
        </Card>
      )}
      {projects.data && rows.length === 0 && (
        <Card><EmptyState title={stage ? 'No projects at this stage' : 'No projects yet'} description="A project is created from a lead once the customer accepts a quotation." /></Card>
      )}

      {rows.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-line text-muted">
              <tr>
                <th className="px-4 py-3 font-medium">Project</th>
                <th className="px-4 py-3 font-medium">Size</th>
                <th className="px-4 py-3 font-medium">Net cost</th>
                <th className="px-4 py-3 font-medium">Advance</th>
                <th className="px-4 py-3 font-medium">Stage</th>
                <th className="px-4 py-3 font-medium">Last update</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {rows.map((p) => {
                const idle = Math.floor((now - new Date(p.updatedAt).getTime()) / DAY)
                const stuck = p.status !== 'SUBSIDY_CREDITED' && idle >= 14
                return (
                  <tr key={p.id} className="hover:bg-canvas">
                    <td className="px-4 py-3">
                      <Link to={`/staff/projects/${p.id}`} className="font-medium text-brand hover:underline">
                        #{p.id.slice(0, 8)}
                      </Link>
                      <p className="text-xs text-muted">Started {formatDate(p.createdAt)}</p>
                    </td>
                    <td className="px-4 py-3">{p.capacityKw} kW</td>
                    <td className="px-4 py-3">{formatINR(Math.round(p.netSystemCost))}</td>
                    <td className="px-4 py-3">{formatINR(Math.round(p.advanceAmount))}</td>
                    <td className="px-4 py-3">
                      <Badge tone={p.status === 'SUBSIDY_CREDITED' ? 'success' : 'info'}>{projectStageLabel[p.status].label}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-muted">{formatDate(p.updatedAt)}</span>
                      {stuck && <span className="ml-2"><Badge tone="warning">{idle}d idle</Badge></span>}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}