import { useQuery } from '@tanstack/react-query'

import { qualificationApi } from '../../api/qualification.api'
import { parseError } from '../../api/client'
import { Card } from '../../components/common/Card'
import { Badge } from '../../components/common/Badge'
import { Spinner } from '../../components/common/Loading'
import { Alert } from '../../components/feedback/Alert'

const num = (n: number | null | undefined) => (n == null ? '—' : Number(n).toLocaleString('en-IN'))
const pretty = (s: string) => s.charAt(0) + s.slice(1).toLowerCase().replaceAll('_', ' ')

export function SizingPanel({ leadId }: { leadId: string }) {
  const rec = useQuery({
    queryKey: ['recommendations', leadId],
    queryFn: () => qualificationApi.recommendations(leadId),
    retry: false,
  })

  return (
    <Card className="p-4 sm:p-5">
      <h2 className="font-semibold">System size options</h2>

      {rec.isPending && <Spinner label="Calculating options..." />}

      {rec.isError && (
        <div className="mt-3">
          <Alert tone="info" title="No options yet">
            {parseError(rec.error).message}
          </Alert>
        </div>
      )}

      {rec.data && (
        <>
          {!rec.data.viable && (
            <div className="mt-3">
              <Alert tone="warning" title="Not viable for a standard system">
                {rec.data.message ?? 'Route this lead to a sales call.'}
              </Alert>
            </div>
          )}

          <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted">Monthly units (confirmed)</dt>
              <dd className="font-medium">{num(rec.data.monthlyUnits)}</dd>
            </div>
            <div>
              <dt className="text-muted">Limited by</dt>
              <dd className="font-medium">{pretty(rec.data.limitingFactor)}</dd>
            </div>
            <div>
              <dt className="text-muted">Usage limit</dt>
              <dd className="font-medium">{num(rec.data.usageLimitKw)} kW</dd>
            </div>
            <div>
              <dt className="text-muted">Roof limit</dt>
              <dd className="font-medium">{num(rec.data.roofLimitKw)} kW</dd>
            </div>
            <div>
              <dt className="text-muted">Sanctioned load</dt>
              <dd className="font-medium">
                {rec.data.sanctionedLoadKw == null ? 'Not known' : `${num(rec.data.sanctionedLoadKw)} kW`}
              </dd>
            </div>
            <div>
              <dt className="text-muted">Recommended</dt>
              <dd className="font-medium">{num(rec.data.recommendedCapacityKw)} kW</dd>
            </div>
          </dl>

          {rec.data.options.length > 0 && (
            <table className="mt-4 w-full text-left text-sm">
              <thead className="border-b border-line text-muted">
                <tr>
                  <th className="py-2 font-medium">Option</th>
                  <th className="py-2 font-medium">Capacity</th>
                  <th className="py-2 font-medium">Yearly generation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {rec.data.options.map((o) => (
                  <tr key={o.type}>
                    <td className="py-2">
                      <Badge tone={o.type === 'RECOMMENDED' ? 'success' : 'neutral'}>{pretty(o.type)}</Badge>
                    </td>
                    <td className="py-2 font-medium">{num(o.capacityKw)} kW</td>
                    <td className="py-2">{num(o.estimatedYearlyGeneration)} units</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </Card>
  )
}