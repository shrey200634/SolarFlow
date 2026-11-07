import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, FilePlus2, Hammer } from 'lucide-react'

import { qualificationApi } from '../../api/qualification.api'
import { quotationsApi } from '../../api/quotations.api'
import { projectsApi } from '../../api/projects.api'
import { parseError } from '../../api/client'
import { Card } from '../../components/common/Card'
import { Badge } from '../../components/common/Badge'
import { Button } from '../../components/common/Button'
import { Spinner } from '../../components/common/Loading'
import { TextField } from '../../components/forms/fields'
import { Alert } from '../../components/feedback/Alert'
import { ConfirmDialog } from '../../components/feedback/ConfirmDialog'
import { useToast } from '../../components/feedback/toast-context'
import { formatDate, formatINR } from '../leads/labels'
import { useDownloadQuotation } from '../quotation/useDownloadQuotation'
import { projectStageLabel } from '../projects/labels'
import type { Quotation } from '../../types/api'

const pretty = (s: string) => s.charAt(0) + s.slice(1).toLowerCase().replaceAll('_', ' ')
const inr = (n: number) => formatINR(Math.round(n))

/** Turn an accepted quotation into a project (advance must be > 0 and ≤ net cost). */
function CreateProjectForm({ quotation }: { quotation: Quotation }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const net = quotation.snapshot.pricing.netSystemCost
  const [advance, setAdvance] = useState('')
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: () => projectsApi.create({ quotationId: quotation.id, advanceAmount: Number(advance) }),
    onSuccess: async () => {
      toast.success('Project created. Status link is ready to share.')
      await queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (e) => {
      const p = parseError(e)
      setError(p.fieldErrors.advanceAmount ?? p.message)
    },
  })

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    const n = Number(advance)
    if (!advance || Number.isNaN(n) || n <= 0) return setError('Enter an advance above ₹0')
    if (n > net) return setError(`Advance cannot exceed the net cost of ${inr(net)}`)
    if (!/^\d+(\.\d{1,2})?$/.test(advance)) return setError('Use at most 2 decimal places')
    setError(null)
    create.mutate()
  }

  return (
    <form onSubmit={submit} className="mt-3 rounded-md border border-success/30 bg-success-soft/50 p-3">
      <p className="text-sm font-medium">Customer accepted this quotation. Record the advance to start the project.</p>
      <div className="mt-3 flex flex-wrap items-end gap-2">
        <div className="w-48">
          <TextField
            label="Advance received (₹)"
            inputMode="decimal"
            value={advance}
            onChange={(e) => setAdvance(e.target.value.replace(/[^\d.]/g, ''))}
            error={error ?? undefined}
            hint={`Max ${inr(net)}`}
          />
        </div>
        <Button type="submit" loading={create.isPending} className="mb-[22px]">
          <Hammer size={14} /> Create project
        </Button>
      </div>
    </form>
  )
}

export function QuotationBuilder({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const download = useDownloadQuotation()
  const [selected, setSelected] = useState<number | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)

  const rec = useQuery({
    queryKey: ['recommendations', leadId],
    queryFn: () => qualificationApi.recommendations(leadId),
    retry: false,
  })
  const quotes = useQuery({
    queryKey: ['quotations', leadId],
    queryFn: () => quotationsApi.listForLead(leadId),
  })
  const projects = useQuery({
    queryKey: ['projects', { leadId }],
    queryFn: () => projectsApi.list(leadId),
  })

  const create = useMutation({
    mutationFn: (kw: number) => quotationsApi.create(leadId, kw),
    onSuccess: async () => {
      setConfirmOpen(false)
      setSelected(null)
      toast.success('Quotation issued. The customer can now view and accept it.')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['quotations', leadId] }),
        queryClient.invalidateQueries({ queryKey: ['lead', leadId] }),
        queryClient.invalidateQueries({ queryKey: ['leads'] }),
        queryClient.invalidateQueries({ queryKey: ['lead-stats'] }),
      ])
    },
    onError: (e) => {
      setConfirmOpen(false)
      toast.error(parseError(e).message)
    },
  })

  const list = quotes.data ?? []
  const accepted = list.find((q) => q.status === 'ACCEPTED')
  const project = projects.data?.[0]
  const options = rec.data?.options ?? []
  const canQuote = Boolean(rec.data?.viable) && !accepted

  return (
    <Card className="p-4 sm:p-5">
      <h2 className="font-semibold">Quotation</h2>
      <p className="mt-1 text-sm text-muted">
        Pick a size option. Price, subsidy and payback are calculated by the server from the rules in force today and frozen into the quote.
      </p>

      {rec.isPending && <Spinner label="Loading size options..." />}
      {rec.isError && (
        <div className="mt-3"><Alert tone="info" title="Cannot quote yet">{parseError(rec.error).message}</Alert></div>
      )}
      {rec.data && !rec.data.viable && (
        <div className="mt-3"><Alert tone="warning" title="Not viable for a standard system">{rec.data.message ?? 'Route to a sales call.'}</Alert></div>
      )}
      {accepted && (
        <div className="mt-3"><Alert tone="success" title="Deal closed">A quotation has been accepted, so no new quotations can be issued.</Alert></div>
      )}

      {canQuote && options.length > 0 && (
        <>
          <div className="mt-4 grid gap-3 sm:grid-cols-3" role="radiogroup" aria-label="System size">
            {options.map((o) => {
              const active = selected === Number(o.capacityKw)
              return (
                <button
                  key={o.type}
                  type="button"
                  role="radio"
                  aria-checked={active}
                  onClick={() => setSelected(Number(o.capacityKw))}
                  className={`rounded-lg border p-3 text-left transition-colors ${
                    active ? 'border-brand bg-brand-soft ring-1 ring-brand' : 'border-line bg-surface hover:bg-canvas'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium text-muted">{pretty(o.type)}</span>
                    {o.type === 'RECOMMENDED' && <Badge tone="success">Best fit</Badge>}
                  </div>
                  <p className="mt-1 text-2xl font-semibold">{o.capacityKw} kW</p>
                  <p className="text-xs text-muted">{Math.round(o.estimatedYearlyGeneration).toLocaleString('en-IN')} units / year</p>
                </button>
              )
            })}
          </div>
          <div className="mt-4 flex justify-end">
            <Button disabled={selected === null} onClick={() => setConfirmOpen(true)}>
              <FilePlus2 size={14} /> Issue quotation
            </Button>
          </div>
        </>
      )}

      <h3 className="mt-6 text-sm font-semibold">Issued quotations</h3>
      {quotes.isPending && <Spinner label="Loading quotations..." />}
      {quotes.isError && <div className="mt-2"><Alert tone="error">{parseError(quotes.error).message}</Alert></div>}
      {quotes.data && list.length === 0 && <p className="mt-2 text-sm text-muted">None yet.</p>}

      {list.length > 0 && (
        <div className="mt-2 overflow-x-auto">
          <table className="w-full min-w-[620px] text-left text-sm">
            <thead className="border-b border-line text-muted">
              <tr>
                <th className="py-2 font-medium">Date</th>
                <th className="py-2 font-medium">Size</th>
                <th className="py-2 font-medium">Gross</th>
                <th className="py-2 font-medium">Subsidy</th>
                <th className="py-2 font-medium">Net</th>
                <th className="py-2 font-medium">Payback</th>
                <th className="py-2 font-medium">Status</th>
                <th className="py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {list.map((q) => {
                const p = q.snapshot.pricing
                return (
                  <tr key={q.id}>
                    <td className="py-2">{formatDate(q.createdAt)}</td>
                    <td className="py-2 font-medium">{q.selectedCapacityKw} kW <span className="text-xs text-muted">({pretty(q.selectedOptionType)})</span></td>
                    <td className="py-2">{inr(p.grossSystemCost)}</td>
                    <td className="py-2">
                      − {inr(p.subsidyAmount)}
                      {p.subsidyEstimated && <span className="ml-1 text-xs text-warning">est.</span>}
                    </td>
                    <td className="py-2 font-medium">{inr(p.netSystemCost)}</td>
                    <td className="py-2">{p.estimatedPaybackYears != null ? `${p.estimatedPaybackYears} yrs` : 'n/a'}</td>
                    <td className="py-2"><Badge tone={q.status === 'ACCEPTED' ? 'success' : 'info'}>{q.status === 'ACCEPTED' ? 'Accepted' : 'Issued'}</Badge></td>
                    <td className="py-2 text-right">
                      <Button variant="ghost" onClick={() => download(q)} aria-label="Download PDF"><Download size={14} /></Button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {accepted && projects.data && !project && <CreateProjectForm quotation={accepted} />}
      {project && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-md border border-line bg-canvas px-3 py-2 text-sm">
          <span>
            Project in progress · <span className="font-medium">{projectStageLabel[project.status].label}</span>
          </span>
          <Link to={`/staff/projects/${project.id}`} className="font-medium text-brand hover:underline">Open project</Link>
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title={`Issue a ${selected} kW quotation?`}
        message="The price, subsidy and tariff in force today will be locked into this quotation. Later rule changes will not affect it."
        confirmLabel="Issue quotation"
        loading={create.isPending}
        onConfirm={() => selected !== null && create.mutate(selected)}
        onCancel={() => setConfirmOpen(false)}
      />
    </Card>
  )
}