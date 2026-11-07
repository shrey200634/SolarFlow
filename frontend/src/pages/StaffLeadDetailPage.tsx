import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

import { leadsApi } from '../api/leads.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Badge } from '../components/common/Badge'
import { Spinner } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { QualificationPanel } from '../features/staff/QualificationPanel'
import { SizingPanel } from '../features/staff/SizingPanel'
import { QuotationBuilder } from '../features/staff/QuotationBuilder'
import { LeadEditPanel } from '../features/staff/LeadEditPanel'
import {
  formatDate,
  formatINR,
  ownershipLabel,
  roofTypeLabel,
  staffStatus,
} from '../features/leads/labels'

function Item({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <dt className="text-muted">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}

export default function StaffLeadDetailPage() {
  const { leadId } = useParams<{ leadId: string }>()

  const leadQuery = useQuery({
    queryKey: ['lead', leadId],
    queryFn: () => leadsApi.get(leadId!),
    enabled: Boolean(leadId),
  })

  if (leadQuery.isPending) return <Spinner label="Loading lead..." />

  if (leadQuery.isError || !leadQuery.data) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="Could not load this lead">
          {leadQuery.isError ? parseError(leadQuery.error).message : 'Lead not found.'}
        </Alert>
        <Link to="/staff" className="mt-4 inline-flex text-sm text-brand hover:underline">Back to leads</Link>
      </Card>
    )
  }

  const lead = leadQuery.data
  const s = staffStatus[lead.status]

  return (
    <>
      <Link to="/staff" className="mb-5 inline-flex items-center gap-2 text-sm text-muted hover:text-brand">
        <ArrowLeft size={16} /> Back to leads
      </Link>

      <PageHeader
        title={lead.ownerName}
        description={`Enquiry created ${formatDate(lead.createdAt)}`}
        actions={<Badge tone={s.tone}>{s.label}</Badge>}
      />

      {lead.duplicate && (
        <div className="mb-5">
          <Alert tone="warning" title="Possible duplicate">
            Another enquiry with similar details exists. Check before contacting the customer.
          </Alert>
        </div>
      )}

      <Card className="mb-5 p-4 sm:p-5">
        <dl className="grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
          <Item label="Email">{lead.ownerEmail}</Item>
          <Item label="Phone"><a href={`tel:${lead.phone}`} className="text-brand hover:underline">{lead.phone}</a></Item>
          <Item label="Pincode">{lead.pincode}</Item>
          <Item label="Property">{ownershipLabel[lead.ownership]}</Item>
          <Item label="Roof">{roofTypeLabel[lead.roofType]} · {lead.roofAreaSqft} sq ft</Item>
          <Item label="Declared monthly bill">{formatINR(lead.declaredMonthlyBill)}</Item>
          <Item label="Assigned to">{lead.assignedToName ?? 'Unassigned'}</Item>
        </dl>
      </Card>

      <div className="grid gap-5 lg:grid-cols-2">
        <QualificationPanel leadId={lead.id} />
        <SizingPanel leadId={lead.id} />
      </div>

      <div className="mt-5">
        <QuotationBuilder leadId={lead.id} />
      </div>

      <div className="mt-5">
        {/* key: resets the form fields whenever the saved lead changes */}
        <LeadEditPanel
          key={`${lead.id}-${lead.status}-${lead.roofAreaSqft}-${lead.roofType}-${lead.ownership}-${lead.assignedToId}`}
          lead={lead}
        />
      </div>
    </>
  )
}