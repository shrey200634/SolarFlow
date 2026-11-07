import { useQuery } from '@tanstack/react-query'
import { leadsApi } from '../api/leads.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Badge } from '../components/common/Badge'
import { Button } from '../components/common/Button'
import { EmptyState } from '../components/common/EmptyState'
import { Skeleton } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { formatDate, homeownerStatus, ownershipLabel, roofTypeLabel } from '../features/leads/labels'

export default function HomeownerHome() {
  const { data, isPending, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['leads', 'mine'],
    queryFn: () => leadsApi.list({ size: 50 }),
  })

  return (
    <>
      <PageHeader title="My applications" description="Your solar enquiries and where each one stands." />

      {isPending && (
        <div className="space-y-3" aria-label="Loading your applications">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      )}

      {isError && (
        <Card className="p-4">
          <Alert tone="error" title="We could not load your applications">
            {parseError(error).message}
          </Alert>
          <Button variant="secondary" className="mt-3" onClick={() => refetch()} loading={isFetching}>
            Try again
          </Button>
        </Card>
      )}

      {data && data.content.length === 0 && (
        <Card>
          <EmptyState
            title="No applications yet"
            description="Once you submit a solar enquiry it appears here, with its progress."
          />
        </Card>
      )}

      {data && data.content.length > 0 && (
        <ul className="space-y-3">
          {data.content.map((lead) => {
            const s = homeownerStatus[lead.status]
            return (
              <li key={lead.id}>
                <Card className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <div>
                    <p className="font-medium">
                      {roofTypeLabel[lead.roofType]} · {lead.roofAreaSqft} sq ft
                    </p>
                    <p className="mt-0.5 text-sm text-muted">
                      {ownershipLabel[lead.ownership]} · Pincode {lead.pincode} · {formatDate(lead.createdAt)}
                    </p>
                    <p className="mt-2 text-sm">{s.hint}</p>
                  </div>
                  <Badge tone={s.tone}>{s.label}</Badge>
                </Card>
              </li>
            )
          })}
        </ul>
      )}
    </>
  )
}