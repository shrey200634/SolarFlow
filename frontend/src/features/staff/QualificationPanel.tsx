import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, CheckCircle2, RefreshCw, XCircle } from 'lucide-react'

import { qualificationApi } from '../../api/qualification.api'
import { parseError } from '../../api/client'
import { Card } from '../../components/common/Card'
import { Badge } from '../../components/common/Badge'
import { Button } from '../../components/common/Button'
import { Spinner } from '../../components/common/Loading'
import { Alert } from '../../components/feedback/Alert'
import { useToast } from '../../components/feedback/toast-context'
import { formatDate } from '../leads/labels'
import type { NextAction } from '../../types/api'

const nextActionLabel: Record<NextAction, { label: string; tone: 'success' | 'warning' }> = {
  SURVEY_RECOMMENDED: { label: 'Survey recommended', tone: 'success' },
  SALES_CALL_PENDING: { label: 'Sales call first', tone: 'warning' },
}

export function QualificationPanel({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient()
  const toast = useToast()

  const result = useQuery({
    queryKey: ['qualification', leadId],
    queryFn: () => qualificationApi.latest(leadId),
    retry: false,
  })

  const run = useMutation({
    mutationFn: () => qualificationApi.run(leadId),
    onSuccess: async () => {
      toast.success('Qualification updated.')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['qualification', leadId] }),
        queryClient.invalidateQueries({ queryKey: ['recommendations', leadId] }),
        queryClient.invalidateQueries({ queryKey: ['lead', leadId] }),
        queryClient.invalidateQueries({ queryKey: ['leads'] }),
        queryClient.invalidateQueries({ queryKey: ['lead-stats'] }),
      ])
    },
    onError: (e) => toast.error(parseError(e).message),
  })

  const notRunYet = result.isError && parseError(result.error).status === 404
  const data = result.data

  return (
    <Card className="p-4 sm:p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-semibold">Qualification</h2>
        <Button variant="secondary" loading={run.isPending} onClick={() => run.mutate()}>
          <RefreshCw size={14} /> {data ? 'Re-run' : 'Run qualification'}
        </Button>
      </div>

      {result.isPending && <Spinner label="Loading qualification..." />}

      {notRunYet && (
        <p className="mt-3 text-sm text-muted">
          Not qualified yet. It runs automatically when the customer confirms the bill, or you can run it now.
        </p>
      )}

      {result.isError && !notRunYet && (
        <div className="mt-3">
          <Alert tone="error" title="Could not load qualification">{parseError(result.error).message}</Alert>
        </div>
      )}

      {data && (
        <>
          <div className="mt-4 flex flex-wrap items-center gap-4">
            <p className="text-4xl font-semibold">
              {data.score}<span className="text-base font-normal text-muted"> / 100</span>
            </p>
            <Badge tone={nextActionLabel[data.nextAction].tone}>{nextActionLabel[data.nextAction].label}</Badge>
          </div>

          <ul className="mt-4 divide-y divide-line text-sm">
            {data.rules.map((r) => (
              <li key={r.code} className="flex items-start gap-2 py-2">
                {r.passed
                  ? <CheckCircle2 size={16} className="mt-0.5 shrink-0 text-success" />
                  : <XCircle size={16} className="mt-0.5 shrink-0 text-muted" />}
                <span className="flex-1">{r.text}</span>
                <span className="shrink-0 font-medium">{r.points}/{r.maxPoints}</span>
              </li>
            ))}
          </ul>

          {data.gates.some((g) => g.triggered) && (
            <div className="mt-4 space-y-2">
              {data.gates.filter((g) => g.triggered).map((g) => (
                <div key={g.code} className="flex gap-2 rounded-md border border-warning/30 bg-warning-soft px-3 py-2 text-sm text-warning">
                  <AlertTriangle size={16} className="mt-0.5 shrink-0" />
                  <span>{g.text}</span>
                </div>
              ))}
            </div>
          )}

          <p className="mt-4 text-xs text-muted">
            Rule set {data.ruleVersion} · run on {formatDate(data.createdAt)}. A low score never rejects a lead.
          </p>
        </>
      )}
    </Card>
  )
}