import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { leadsApi } from '../api/leads.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Badge } from '../components/common/Badge'
import { Button } from '../components/common/Button'
import { EmptyState } from '../components/common/EmptyState'
import { Skeleton } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { TextField, SelectField } from '../components/forms/fields'
import {
  formatDate,
  formatINR,
  leadStatuses,
  ownershipLabel,
  staffStatus,
} from '../features/leads/labels'
import type { LeadStatus } from '../types/api'

const PAGE_SIZE = 20

export default function StaffHome() {
  const [status, setStatus] = useState<LeadStatus | ''>('')
  const [pincode, setPincode] = useState('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)

  const stats = useQuery({ queryKey: ['lead-stats'], queryFn: leadsApi.stats })

  const leads = useQuery({
    queryKey: ['leads', 'staff', { status, pincode, q, page }],
    queryFn: () =>
      leadsApi.list({
        status: status || undefined,
        pincode: pincode.length === 6 ? pincode : undefined,
        q: q.trim() || undefined,
        page,
        size: PAGE_SIZE,
      }),
    placeholderData: keepPreviousData,
  })

  const resetPage = () => setPage(0)

  return (
    <>
      <PageHeader title="Leads" description="Every enquiry, newest first. Click a row to open it." />

      {/* Status counts: click one to filter */}
      <div className="mb-5 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => { setStatus(''); resetPage() }}
          className={`rounded-md border px-3 py-1.5 text-sm ${status === '' ? 'border-brand bg-brand text-white' : 'border-line bg-surface hover:bg-canvas'}`}
        >
          All{stats.data ? ` · ${stats.data.total}` : ''}
        </button>
        {leadStatuses.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => { setStatus(s); resetPage() }}
            className={`rounded-md border px-3 py-1.5 text-sm ${status === s ? 'border-brand bg-brand text-white' : 'border-line bg-surface hover:bg-canvas'}`}
          >
            {staffStatus[s].label} · {stats.data?.byStatus[s] ?? 0}
          </button>
        ))}
      </div>

      <Card className="mb-5 p-4">
        <div className="grid gap-4 sm:grid-cols-3">
          <TextField
            label="Search"
            placeholder="Name, email or phone"
            value={q}
            onChange={(e) => { setQ(e.target.value); resetPage() }}
          />
          <TextField
            label="Pincode"
            inputMode="numeric"
            maxLength={6}
            placeholder="6 digits"
            value={pincode}
            onChange={(e) => { setPincode(e.target.value.replace(/\D/g, '')); resetPage() }}
          />
          <SelectField
            label="Status"
            value={status}
            onChange={(e) => { setStatus(e.target.value as LeadStatus | ''); resetPage() }}
          >
            <option value="">All statuses</option>
            {leadStatuses.map((s) => (
              <option key={s} value={s}>{staffStatus[s].label}</option>
            ))}
          </SelectField>
        </div>
      </Card>

      {leads.isPending && (
        <div className="space-y-2">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
        </div>
      )}

      {leads.isError && (
        <Card className="p-4">
          <Alert tone="error" title="Could not load leads">{parseError(leads.error).message}</Alert>
          <Button variant="secondary" className="mt-3" onClick={() => leads.refetch()} loading={leads.isFetching}>
            Try again
          </Button>
        </Card>
      )}

      {leads.data && leads.data.content.length === 0 && (
        <Card><EmptyState title="No leads match" description="Try a different filter or clear the search." /></Card>
      )}

      {leads.data && leads.data.content.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b border-line text-muted">
              <tr>
                <th className="px-4 py-3 font-medium">Customer</th>
                <th className="px-4 py-3 font-medium">Pincode</th>
                <th className="px-4 py-3 font-medium">Property</th>
                <th className="px-4 py-3 font-medium">Bill / month</th>
                <th className="px-4 py-3 font-medium">Score</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {leads.data.content.map((lead) => {
                const s = staffStatus[lead.status]
                return (
                  <tr key={lead.id} className="hover:bg-canvas">
                    <td className="px-4 py-3">
                      <Link to={`/staff/leads/${lead.id}`} className="font-medium text-brand hover:underline">
                        {lead.ownerName}
                      </Link>
                      <p className="text-xs text-muted">{lead.phone}</p>
                    </td>
                    <td className="px-4 py-3">{lead.pincode}</td>
                    <td className="px-4 py-3">{ownershipLabel[lead.ownership]}</td>
                    <td className="px-4 py-3">{formatINR(lead.declaredMonthlyBill)}</td>
                    <td className="px-4 py-3">{lead.qualificationScore ?? '—'}</td>
                    <td className="px-4 py-3">
                      <Badge tone={s.tone}>{s.label}</Badge>
                      {lead.duplicate && <span className="ml-2"><Badge tone="warning">Duplicate?</Badge></span>}
                    </td>
                    <td className="px-4 py-3 text-muted">{formatDate(lead.createdAt)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>

          <div className="flex items-center justify-between border-t border-line px-4 py-3 text-sm">
            <span className="text-muted">
              Page {leads.data.page + 1} of {Math.max(leads.data.totalPages, 1)} · {leads.data.totalElements} leads
            </span>
            <div className="flex gap-2">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
              <Button variant="secondary" disabled={page + 1 >= leads.data.totalPages} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </div>
          </div>
        </Card>
      )}
    </>
  )
}