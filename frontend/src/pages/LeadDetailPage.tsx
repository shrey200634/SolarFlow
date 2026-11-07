import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, FileText } from 'lucide-react'

import { leadsApi } from '../api/leads.api'
import { quotationsApi } from '../api/quotations.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Badge } from '../components/common/Badge'
import { Button } from '../components/common/Button'
import { Spinner } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { QuotationCard } from '../features/quotation/QuotationCard'
import { useDownloadQuotation } from '../features/quotation/useDownloadQuotation'
import {
  formatDate,
  formatINR,
  homeownerStatus,
  ownershipLabel,
  roofTypeLabel,
} from '../features/leads/labels'

export default function LeadDetailPage() {
  const { leadId } = useParams<{ leadId: string }>()
  const download = useDownloadQuotation()

  const leadQuery = useQuery({
    queryKey: ['lead', leadId],
    queryFn: () => leadsApi.get(leadId!),
    enabled: Boolean(leadId),
  })

  const quotesQuery = useQuery({
    queryKey: ['quotations', leadId],
    queryFn: () => quotationsApi.listForLead(leadId!),
    enabled: Boolean(leadId),
  })

  if (leadQuery.isPending) return <Spinner label="Loading application..." />

  if (leadQuery.isError || !leadQuery.data) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="Could not load this application">
          {leadQuery.isError ? parseError(leadQuery.error).message : 'Application not found.'}
        </Alert>
        <Link to="/app" className="mt-4 inline-flex text-sm text-brand hover:underline">
          Back to applications
        </Link>
      </Card>
    )
  }

  const lead = leadQuery.data
  const s = homeownerStatus[lead.status]
  const quotes = quotesQuery.data ?? []
  const [current, ...earlier] = quotes
  const alreadyAccepted = quotes.some((q) => q.status === 'ACCEPTED')

  return (
    <>
      <Link to="/app" className="mb-5 inline-flex items-center gap-2 text-sm text-muted hover:text-brand">
        <ArrowLeft size={16} /> Back to applications
      </Link>

      <PageHeader
        title={`${roofTypeLabel[lead.roofType]} · ${lead.roofAreaSqft} sq ft`}
        description={`Enquiry created ${formatDate(lead.createdAt)}`}
        actions={<Badge tone={s.tone}>{s.label}</Badge>}
      />

      <Card className="mb-5 p-4 sm:p-5">
        <p className="text-sm">{s.hint}</p>
        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3">
          <div>
            <dt className="text-muted">Property</dt>
            <dd className="font-medium">{ownershipLabel[lead.ownership]}</dd>
          </div>
          <div>
            <dt className="text-muted">Pincode</dt>
            <dd className="font-medium">{lead.pincode}</dd>
          </div>
          <div>
            <dt className="text-muted">Monthly bill</dt>
            <dd className="font-medium">{formatINR(lead.declaredMonthlyBill)}</dd>
          </div>
        </dl>
        <Link
          to={`/app/${lead.id}/bill`}
          className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-brand hover:underline"
        >
          <FileText size={14} /> Electricity bill
        </Link>
      </Card>

      {quotesQuery.isPending && <Spinner label="Checking for quotations..." />}

      {quotesQuery.isError && (
        <Card className="p-4">
          <Alert tone="error" title="Could not load quotations">
            {parseError(quotesQuery.error).message}
          </Alert>
          <Button variant="secondary" className="mt-3" onClick={() => quotesQuery.refetch()}>
            Try again
          </Button>
        </Card>
      )}

      {current && <QuotationCard quotation={current} canAccept={!alreadyAccepted} />}

      {earlier.length > 0 && (
        <Card className="mt-5 p-4 sm:p-5">
          <h2 className="font-semibold">Earlier quotations</h2>
          <ul className="mt-2 divide-y divide-line">
            {earlier.map((q) => (
              <li key={q.id} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm">
                <span>
                  {q.selectedCapacityKw} kW · {formatDate(q.createdAt)} ·{' '}
                  {formatINR(Math.round(q.snapshot.pricing.netSystemCost))} after subsidy
                </span>
                <Button variant="ghost" onClick={() => download(q)}>
                  Download PDF
                </Button>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  )
}