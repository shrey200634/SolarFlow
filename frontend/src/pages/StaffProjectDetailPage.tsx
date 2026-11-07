import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, Link2, Link2Off } from 'lucide-react'

import { projectsApi } from '../api/projects.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Badge } from '../components/common/Badge'
import { Button } from '../components/common/Button'
import { Spinner } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { ConfirmDialog } from '../components/feedback/ConfirmDialog'
import { useToast } from '../components/feedback/toast-context'
import { formatDate, formatINR } from '../features/leads/labels'
import { nextStage, projectStageLabel } from '../features/projects/labels'
import { ProjectTimeline } from '../features/projects/ProjectTimeline'
import { StatusLinkBox } from '../features/projects/StatusLinkBox'
import type { Project, ProjectStatus } from '../types/api'

export default function StaffProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [note, setNote] = useState('')
  const [confirmRevoke, setConfirmRevoke] = useState(false)

  const project = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectsApi.get(projectId!),
    enabled: Boolean(projectId),
  })

  const onSaved = (p: Project) => {
    queryClient.setQueryData(['project', p.id], p)
    queryClient.invalidateQueries({ queryKey: ['projects'] })
  }

  const advance = useMutation({
    mutationFn: (to: ProjectStatus) => projectsApi.advance(projectId!, { status: to, note: note.trim() || undefined }),
    onSuccess: (p) => {
      onSaved(p)
      setNote('')
      toast.success(`Moved to ${projectStageLabel[p.status].label}.`)
    },
    onError: (e) => toast.error(parseError(e).message),
  })

  const link = useMutation({
    mutationFn: (active: boolean) => projectsApi.setStatusLink(projectId!, active),
    onSuccess: (p) => {
      onSaved(p)
      setConfirmRevoke(false)
      toast.success(p.statusLink ? 'Status link turned on.' : 'Status link revoked.')
    },
    onError: (e) => {
      setConfirmRevoke(false)
      toast.error(parseError(e).message)
    },
  })

  if (project.isPending) return <Spinner label="Loading project..." />
  if (project.isError || !project.data) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="Could not load this project">
          {project.isError ? parseError(project.error).message : 'Not found.'}
        </Alert>
        <Link to="/staff/projects" className="mt-4 inline-flex text-sm text-brand hover:underline">Back to projects</Link>
      </Card>
    )
  }

  const p = project.data
  const next = nextStage(p.status)
  const reachedAt: Partial<Record<ProjectStatus, string>> = {}
  const notes: Partial<Record<ProjectStatus, string | null>> = {}
  for (const h of p.history) {
    reachedAt[h.toStatus] = h.changedAt
    notes[h.toStatus] = h.note
  }
  const balance = Number(p.netSystemCost) - Number(p.advanceAmount)

  return (
    <>
      <Link to="/staff/projects" className="mb-5 inline-flex items-center gap-2 text-sm text-muted hover:text-brand">
        <ArrowLeft size={16} /> Back to projects
      </Link>

      <PageHeader
        title={`${p.capacityKw} kW installation`}
        description={`Project #${p.id.slice(0, 8)} · started ${formatDate(p.createdAt)}`}
        actions={
          <div className="flex items-center gap-2">
            <Link to={`/staff/leads/${p.leadId}`} className="text-sm text-brand hover:underline">Open lead</Link>
            <Badge tone={next ? 'info' : 'success'}>{projectStageLabel[p.status].label}</Badge>
          </div>
        }
      />

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        {[
          ['Net system cost', formatINR(Math.round(p.netSystemCost))],
          ['Advance received', formatINR(Math.round(p.advanceAmount))],
          ['Balance due', formatINR(Math.round(Math.max(balance, 0)))],
        ].map(([label, value]) => (
          <Card key={label} className="p-4">
            <p className="text-xs text-muted">{label}</p>
            <p className="mt-0.5 text-lg font-semibold">{value}</p>
          </Card>
        ))}
      </div>

      <div className="grid gap-5 lg:grid-cols-[1fr_1.1fr]">
        <Card className="p-4 sm:p-5">
          <h2 className="mb-4 font-semibold">Progress</h2>
          <ProjectTimeline current={p.status} reachedAt={reachedAt} notes={notes} />
        </Card>

        <div className="space-y-5">
          <Card className="p-4 sm:p-5">
            <h2 className="font-semibold">Move to next stage</h2>
            {next ? (
              <>
                <p className="mt-1 text-sm text-muted">
                  Stages only move forward, one at a time. Next: <span className="font-medium text-ink">{projectStageLabel[next].label}</span>
                </p>
                <label htmlFor="note" className="mt-4 mb-1 block text-sm font-medium">Internal note (optional)</label>
                <textarea
                  id="note"
                  rows={3}
                  maxLength={500}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="e.g. Installation booked for 14 Nov with team B"
                  className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm focus:outline-2 focus:outline-brand"
                />
                <div className="mt-1 flex items-center justify-between">
                  <p className="text-xs text-muted">Staff only. Never shown to the customer. {note.length}/500</p>
                  <Button loading={advance.isPending} onClick={() => advance.mutate(next)}>
                    Mark {projectStageLabel[next].label.toLowerCase()} <ArrowRight size={14} />
                  </Button>
                </div>
              </>
            ) : (
              <div className="mt-3"><Alert tone="success" title="Project complete">Subsidy has been credited. No further stages.</Alert></div>
            )}
          </Card>

          <Card className="p-4 sm:p-5">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-semibold">Customer status link</h2>
              {p.statusLink ? (
                <Button variant="ghost" className="text-danger" onClick={() => setConfirmRevoke(true)}><Link2Off size={14} /> Revoke</Button>
              ) : (
                <Button variant="secondary" loading={link.isPending} onClick={() => link.mutate(true)}><Link2 size={14} /> Turn on</Button>
              )}
            </div>
            <p className="mt-1 mb-3 text-sm text-muted">
              Anyone with this link sees only the stage and dates. No name, address, consumer number, bill or prices.
            </p>
            <StatusLinkBox statusLink={p.statusLink} />
          </Card>
        </div>
      </div>

      <ConfirmDialog
        open={confirmRevoke}
        danger
        title="Revoke the status link?"
        message="The link will stop working immediately. Turning it back on restores the same link."
        confirmLabel="Revoke link"
        loading={link.isPending}
        onConfirm={() => link.mutate(false)}
        onCancel={() => setConfirmRevoke(false)}
      />
    </>
  )
}