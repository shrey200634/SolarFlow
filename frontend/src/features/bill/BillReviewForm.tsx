import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'
import { CheckCircle2, ExternalLink } from 'lucide-react'

import { documentsApi, type BillFieldKey, type Extraction, type LeadDocument } from '../../api/documents.api'
import { parseError } from '../../api/client'
import { Card } from '../../components/common/Card'
import { Button } from '../../components/common/Button'
import { Spinner } from '../../components/common/Loading'
import { TextField } from '../../components/forms/fields'
import { Alert } from '../../components/feedback/Alert'
import { useToast } from '../../components/feedback/toast-context'
import { parseBillIssues } from './issues'

const schema = z.object({
  consumerNumber: z.string().trim().min(1, 'Enter the consumer number').max(50, 'Use 50 characters or fewer'),
  tariff: z.string().trim().min(1, 'Enter the tariff category').max(50, 'Use 50 characters or fewer'),
  sanctionedLoadKw: z
    .string()
    .trim()
    .min(1, 'Enter the sanctioned load')
    .refine((v) => Number(v) > 0, 'Enter a number above 0'),
  monthlyUnits: z
    .string()
    .trim()
    .min(1, 'Enter the units used')
    .refine((v) => Number(v) >= 0, 'Enter a number, 0 or more'),
})
type FormValues = z.infer<typeof schema>

const FIELDS: { key: BillFieldKey; label: string; hint: string; numeric: boolean }[] = [
  { key: 'consumerNumber', label: 'Consumer number', hint: 'Printed near the top of your bill.', numeric: false },
  { key: 'tariff', label: 'Tariff category', hint: 'For example LT-I Residential.', numeric: false },
  { key: 'sanctionedLoadKw', label: 'Sanctioned load (kW)', hint: 'Also called contract load.', numeric: true },
  { key: 'monthlyUnits', label: 'Units used this month (kWh)', hint: 'Electricity consumed in the billing period.', numeric: true },
]

const CONFIDENT = 0.8

function toText(v: string | number | null | undefined): string {
  return v === null || v === undefined ? '' : String(v)
}

async function openBill(documentId: string) {
  // Open the tab first so the browser does not treat it as a blocked popup.
  const tab = window.open('', '_blank')
  try {
    const url = URL.createObjectURL(await documentsApi.file(documentId))
    if (tab) tab.location.href = url
    else window.location.href = url
  } catch {
    tab?.close()
    throw new Error('Could not open the bill file.')
  }
}

function ReviewForm({ document, extraction }: { document: LeadDocument; extraction: Extraction }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [formError, setFormError] = useState<string | null>(null)

  const initial: FormValues = {
    consumerNumber: toText(extraction.extractedFields.consumerNumber?.value),
    tariff: toText(extraction.extractedFields.tariff?.value),
    sanctionedLoadKw: toText(extraction.extractedFields.sanctionedLoadKw?.value),
    monthlyUnits: toText(extraction.extractedFields.monthlyUnits?.value),
  }

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: initial })

  const confirm = useMutation({
    mutationFn: (v: FormValues) => {
      // Only send what the person changed, so unchanged values stay marked as read from the bill.
      const body: Record<string, string | number> = {}
      for (const f of FIELDS) {
        const next = v[f.key]
        const same = f.numeric ? Number(next) === Number(initial[f.key]) && initial[f.key] !== '' : next === initial[f.key]
        if (!same) body[f.key] = f.numeric ? Number(next) : next
      }
      return documentsApi.confirm(document.id, body)
    },
    onSuccess: async () => {
      toast.success('Bill details confirmed.')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['documents', document.leadId] }),
        queryClient.invalidateQueries({ queryKey: ['extraction', document.id] }),
        queryClient.invalidateQueries({ queryKey: ['lead', document.leadId] }),
        queryClient.invalidateQueries({ queryKey: ['leads'] }),
      ])
    },
    onError: (e) => {
      const p = parseError(e)
      const parsed = parseBillIssues(p.message)
      const keys = Object.keys(parsed.fields) as BillFieldKey[]
      for (const key of keys) setError(key, { message: parsed.fields[key] })
      if (keys.length === 0) setFormError(p.status === 409 ? 'This bill was already confirmed. Refresh the page.' : p.message)
    },
  })

  const failed = document.status === 'FAILED'
  const lowQuality = document.status === 'LOW_QUALITY'
  const lowConfidence = FIELDS.filter((f) => {
    const c = extraction.extractedFields[f.key]?.confidence
    return typeof c === 'number' && c < CONFIDENT
  }).map((f) => f.key)

  return (
    <Card className="p-4 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-semibold">{failed ? 'Enter your bill details' : 'Check your bill details'}</h2>
          <p className="mt-1 text-sm text-muted">
            {failed
              ? 'We could not read your bill automatically. Enter the details from it below, or upload a clearer copy.'
              : 'We read these from your bill. Fix anything that looks wrong, then confirm.'}
          </p>
        </div>
        <Button variant="secondary" onClick={() => openBill(document.id).catch((e: Error) => toast.error(e.message))}>
          <ExternalLink size={14} /> Open my bill
        </Button>
      </div>

      {lowQuality && (
        <div className="mt-4">
          <Alert tone="warning" title="The photo was hard to read">
            Please check every value carefully, or upload a clearer copy below.
          </Alert>
        </div>
      )}
      {formError && (
        <div className="mt-4">
          <Alert tone="error">{formError}</Alert>
        </div>
      )}

      <form
        onSubmit={handleSubmit((v) => {
          setFormError(null)
          confirm.mutate(v)
        })}
        className="mt-5 grid gap-4 sm:grid-cols-2"
        noValidate
      >
        {FIELDS.map((f) => (
          <TextField
            key={f.key}
            label={f.label}
            inputMode={f.numeric ? 'decimal' : undefined}
            hint={lowConfidence.includes(f.key) && !errors[f.key] ? 'Please double-check this one against your bill.' : f.hint}
            error={errors[f.key]?.message}
            {...register(f.key)}
          />
        ))}

        <div className="flex justify-end sm:col-span-2">
          <Button type="submit" loading={confirm.isPending}>
            Confirm details
          </Button>
        </div>
      </form>
    </Card>
  )
}

function ConfirmedSummary({ document, extraction }: { document: LeadDocument; extraction: Extraction }) {
  const toast = useToast()
  return (
    <Card className="p-4 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <CheckCircle2 size={18} className="text-success" />
          <h2 className="font-semibold">Bill details confirmed</h2>
        </div>
        <Button variant="secondary" onClick={() => openBill(document.id).catch((e: Error) => toast.error(e.message))}>
          <ExternalLink size={14} /> Open my bill
        </Button>
      </div>
      <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        {FIELDS.map((f) => (
          <div key={f.key}>
            <dt className="text-muted">{f.label}</dt>
            <dd className="font-medium">{toText(extraction.confirmedFields[f.key]?.value) || '—'}</dd>
          </div>
        ))}
      </dl>
      <p className="mt-4 text-sm text-muted">Our team is reviewing your enquiry and will be in touch.</p>
    </Card>
  )
}

/** Review step for one uploaded bill. Shows the form, or the confirmed summary once done. */
export function BillReview({ document }: { document: LeadDocument }) {
  const query = useQuery({
    queryKey: ['extraction', document.id],
    queryFn: () => documentsApi.extraction(document.id),
  })

  if (query.isPending) return <Spinner label="Loading bill details..." />
  if (query.isError) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="Could not load the bill details">
          {parseError(query.error).message}
        </Alert>
        <Button variant="secondary" className="mt-3" onClick={() => query.refetch()}>
          Try again
        </Button>
      </Card>
    )
  }

  return document.status === 'CONFIRMED' ? (
    <ConfirmedSummary document={document} extraction={query.data} />
  ) : (
    <ReviewForm key={document.id} document={document} extraction={query.data} />
  )
}