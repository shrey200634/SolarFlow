import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, Download } from 'lucide-react'

import { quotationsApi } from '../../api/quotations.api'
import { parseError } from '../../api/client'
import { Card } from '../../components/common/Card'
import { Badge } from '../../components/common/Badge'
import { Button } from '../../components/common/Button'
import { Alert } from '../../components/feedback/Alert'
import { ConfirmDialog } from '../../components/feedback/ConfirmDialog'
import { useToast } from '../../components/feedback/toast-context'
import { formatINR, formatDate } from '../leads/labels'
import { useDownloadQuotation } from './useDownloadQuotation'
import type { Quotation } from '../../types/api'

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-2">
      <dt className="text-muted">{label}</dt>
      <dd className={strong ? 'text-base font-semibold' : 'font-medium'}>{value}</dd>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-line bg-canvas p-3">
      <p className="text-xs text-muted">{label}</p>
      <p className="mt-0.5 text-base font-semibold">{value}</p>
    </div>
  )
}

/** Full breakdown of one quotation. Homeowners can accept it while it is still ISSUED. */
export function QuotationCard({ quotation, canAccept }: { quotation: Quotation; canAccept: boolean }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const download = useDownloadQuotation()
  const [confirming, setConfirming] = useState(false)
  const [downloading, setDownloading] = useState(false)

  const { pricing } = quotation.snapshot
  const accepted = quotation.status === 'ACCEPTED'

  const accept = useMutation({
    mutationFn: () => quotationsApi.accept(quotation.id),
    onSuccess: async () => {
      setConfirming(false)
      toast.success('Quotation accepted. We will contact you about installation.')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['quotations', quotation.leadId] }),
        queryClient.invalidateQueries({ queryKey: ['lead', quotation.leadId] }),
        queryClient.invalidateQueries({ queryKey: ['leads'] }),
      ])
    },
    onError: (e) => {
      setConfirming(false)
      toast.error(parseError(e).message)
    },
  })

  return (
    <Card className="p-4 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold">{quotation.selectedCapacityKw} kW rooftop system</h2>
            <Badge tone={accepted ? 'success' : 'info'}>{accepted ? 'Accepted' : 'Ready to review'}</Badge>
          </div>
          <p className="mt-1 text-sm text-muted">Quotation dated {formatDate(quotation.createdAt)}</p>
        </div>
        <Button
          variant="secondary"
          loading={downloading}
          onClick={async () => {
            setDownloading(true)
            await download(quotation)
            setDownloading(false)
          }}
        >
          <Download size={14} /> Download PDF
        </Button>
      </div>

      {/* The story first: size (title) → cost after subsidy → payback → savings */}
      <div className="mt-5 grid gap-3 sm:grid-cols-3">
        <Stat label="You pay after subsidy" value={formatINR(Math.round(pricing.netSystemCost))} />
        <Stat
          label="Pays for itself in"
          value={pricing.estimatedPaybackYears == null ? 'Not available' : `${pricing.estimatedPaybackYears} years`}
        />
        <Stat label="You save every year" value={formatINR(Math.round(pricing.estimatedAnnualSavings))} />
      </div>

      <dl className="mt-5 divide-y divide-line text-sm">
        <Row label={`System cost (${formatINR(pricing.pricePerKw)} per kW)`} value={formatINR(Math.round(pricing.grossSystemCost))} />
        <Row
          label={pricing.subsidyEstimated ? 'Government subsidy (estimated)' : 'Government subsidy'}
          value={`− ${formatINR(Math.round(pricing.subsidyAmount))}`}
        />
        <Row label="You pay (after subsidy)" value={formatINR(Math.round(pricing.netSystemCost))} strong />
        <Row
          label="Yearly generation (estimated)"
          value={`${Math.round(pricing.annualGenerationUnits).toLocaleString('en-IN')} units`}
        />
      </dl>

      {(quotation.snapshot.subsidyDisclaimer || quotation.snapshot.pricingDisclaimer) && (
        <div className="mt-4 space-y-1 text-xs text-muted">
          {quotation.snapshot.pricingDisclaimer && <p>{quotation.snapshot.pricingDisclaimer}</p>}
          {quotation.snapshot.subsidyDisclaimer && <p>{quotation.snapshot.subsidyDisclaimer}</p>}
        </div>
      )}

      {accepted ? (
        <div className="mt-5">
          <Alert tone="success" title="You accepted this quotation">
            <span className="inline-flex items-center gap-1">
              <CheckCircle2 size={14} />
              {quotation.acceptedAt ? `On ${formatDate(quotation.acceptedAt)}. ` : ''}Our team will contact you to plan the installation.
            </span>
          </Alert>
        </div>
      ) : (
        canAccept && (
          <div className="mt-5 flex justify-end">
            <Button onClick={() => setConfirming(true)}>Accept quotation</Button>
          </div>
        )
      )}

      <ConfirmDialog
        open={confirming}
        title="Accept this quotation?"
        message={`You are accepting the ${quotation.selectedCapacityKw} kW system for ${formatINR(Math.round(pricing.netSystemCost))} after subsidy. Our team will contact you next.`}
        confirmLabel="Yes, accept"
        loading={accept.isPending}
        onConfirm={() => accept.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </Card>
  )
}