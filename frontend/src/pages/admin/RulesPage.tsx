import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'

import { adminApi } from '../../api/admin.api'
import { parseError } from '../../api/client'
import { PageHeader } from '../../components/common/PageHeader'
import { Card } from '../../components/common/Card'
import { Badge } from '../../components/common/Badge'
import { Button } from '../../components/common/Button'
import { EmptyState } from '../../components/common/EmptyState'
import { Spinner } from '../../components/common/Loading'
import { CheckboxField, TextField } from '../../components/forms/fields'
import { Alert } from '../../components/feedback/Alert'
import { useToast } from '../../components/feedback/toast-context'
import { formatINR } from '../../features/leads/labels'
import { formatDay, inForceToday, todayISO } from '../../features/projects/labels'

type Tab = 'pricing' | 'subsidy'
const money = (s: string) => s.replace(/[^\d.]/g, '')
const isMoney = (s: string, min = 0.01) => /^\d+(\.\d{1,2})?$/.test(s) && Number(s) >= min

function RuleBadges({ active, from, to }: { active: boolean; from: string; to: string | null }) {
  if (!active) return <Badge>Inactive</Badge>
  if (inForceToday(from, to)) return <Badge tone="success">In force today</Badge>
  return from > todayISO() ? <Badge tone="info">Scheduled</Badge> : <Badge>Expired</Badge>
}

function DateFields({ from, to, setFrom, setTo, errors }: {
  from: string; to: string; setFrom: (v: string) => void; setTo: (v: string) => void; errors: Record<string, string>
}) {
  return (
    <>
      <TextField label="Effective from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} error={errors.effectiveFrom} />
      <TextField label="Effective to" type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} error={errors.effectiveTo} hint="Leave empty for open-ended" />
    </>
  )
}

/** Shared mutation handling: show field errors inline, overlap (409) as a form error. */
function useRuleMutation<T>(fn: () => Promise<T>, key: string, onDone: () => void) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const m = useMutation({
    mutationFn: fn,
    onSuccess: async () => {
      toast.success('Rule added. Existing quotations are unaffected.')
      setErrors({}); setFormError(null); onDone()
      await queryClient.invalidateQueries({ queryKey: ['admin', key] })
    },
    onError: (e) => {
      const p = parseError(e)
      setErrors(p.fieldErrors)
      setFormError(Object.keys(p.fieldErrors).length ? null : p.message)
    },
  })
  return { m, errors, setErrors, formError }
}

function useToggle(key: string, fn: (id: string, active: boolean) => Promise<unknown>) {
  const queryClient = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => fn(id, active),
    onSuccess: async () => {
      toast.success('Rule updated.')
      await queryClient.invalidateQueries({ queryKey: ['admin', key] })
    },
    onError: (e) => toast.error(parseError(e).message), // e.g. 409 overlap on re-activate
  })
}

function PricingRules() {
  const [price, setPrice] = useState('')
  const [tariff, setTariff] = useState('')
  const [from, setFrom] = useState(todayISO())
  const [to, setTo] = useState('')
  const list = useQuery({ queryKey: ['admin', 'pricing'], queryFn: adminApi.pricingRules })
  const toggle = useToggle('pricing', adminApi.setPricingRuleActive)
  const { m, errors, setErrors, formError } = useRuleMutation(
    () => adminApi.addPricingRule({ pricePerKw: Number(price), electricityTariffPerUnit: Number(tariff), effectiveFrom: from, effectiveTo: to || null }),
    'pricing',
    () => { setPrice(''); setTariff(''); setTo('') },
  )

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!isMoney(price)) errs.pricePerKw = 'Enter a price above 0'
    if (!isMoney(tariff)) errs.electricityTariffPerUnit = 'Enter a tariff above 0'
    if (!from) errs.effectiveFrom = 'Choose a start date'
    if (to && to < from) errs.effectiveTo = 'End date cannot be before start date'
    if (Object.keys(errs).length) return setErrors(errs)
    m.mutate()
  }

  return (
    <>
      <Card className="mb-5 p-4">
        <h2 className="mb-3 font-semibold">New pricing rule</h2>
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <TextField label="System price per kW (₹)" inputMode="decimal" value={price} onChange={(e) => setPrice(money(e.target.value))} error={errors.pricePerKw} placeholder="63333" />
          <TextField label="Electricity tariff (₹/unit)" inputMode="decimal" value={tariff} onChange={(e) => setTariff(money(e.target.value))} error={errors.electricityTariffPerUnit} hint="Used for savings and payback" placeholder="8" />
          <DateFields from={from} to={to} setFrom={setFrom} setTo={setTo} errors={errors} />
          <div className="flex items-center justify-between gap-3 lg:col-span-4">
            {formError ? <Alert tone="error">{formError}</Alert> : <span className="text-xs text-muted">Active rules may not overlap in dates.</span>}
            <Button type="submit" loading={m.isPending}><Plus size={14} /> Add rule</Button>
          </div>
        </form>
      </Card>

      {list.isPending && <Spinner />}
      {list.isError && <Alert tone="error">{parseError(list.error).message}</Alert>}
      {list.data && (
        <Card className="overflow-x-auto">
          {list.data.length === 0 ? <EmptyState title="No pricing rules" description="Quotations fail until a rule is in force." /> : (
            <table className="w-full min-w-[640px] text-left text-sm">
              <thead className="border-b border-line text-muted">
                <tr>
                  <th className="px-4 py-3 font-medium">Price / kW</th>
                  <th className="px-4 py-3 font-medium">Tariff / unit</th>
                  <th className="px-4 py-3 font-medium">From</th>
                  <th className="px-4 py-3 font-medium">To</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {list.data.map((r) => (
                  <tr key={r.id} className={r.active ? '' : 'text-muted'}>
                    <td className="px-4 py-3 font-medium">{formatINR(r.pricePerKw)}</td>
                    <td className="px-4 py-3">₹{r.electricityTariffPerUnit}</td>
                    <td className="px-4 py-3">{formatDay(r.effectiveFrom)}</td>
                    <td className="px-4 py-3">{formatDay(r.effectiveTo)}</td>
                    <td className="px-4 py-3"><RuleBadges active={r.active} from={r.effectiveFrom} to={r.effectiveTo} /></td>
                    <td className="px-4 py-3 text-right">
                      <Button variant="ghost" loading={toggle.isPending && toggle.variables?.id === r.id} onClick={() => toggle.mutate({ id: r.id, active: !r.active })}>
                        {r.active ? 'Deactivate' : 'Activate'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}
    </>
  )
}

function SubsidyRules() {
  const [first, setFirst] = useState('30000')
  const [third, setThird] = useState('18000')
  const [max, setMax] = useState('78000')
  const [from, setFrom] = useState(todayISO())
  const [to, setTo] = useState('')
  const [estimated, setEstimated] = useState(true)
  const list = useQuery({ queryKey: ['admin', 'subsidy'], queryFn: adminApi.subsidyRules })
  const toggle = useToggle('subsidy', adminApi.setSubsidyRuleActive)
  const { m, errors, setErrors, formError } = useRuleMutation(
    () => adminApi.addSubsidyRule({
      subsidyPerKwFirstTwoKw: Number(first), subsidyForThirdKw: Number(third), maximumSubsidy: Number(max),
      effectiveFrom: from, effectiveTo: to || null, estimated,
    }),
    'subsidy',
    () => setTo(''),
  )

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!isMoney(first, 0)) errs.subsidyPerKwFirstTwoKw = 'Enter 0 or more'
    if (!isMoney(third, 0)) errs.subsidyForThirdKw = 'Enter 0 or more'
    if (!isMoney(max)) errs.maximumSubsidy = 'Enter a cap above 0'
    if (!from) errs.effectiveFrom = 'Choose a start date'
    if (to && to < from) errs.effectiveTo = 'End date cannot be before start date'
    if (Object.keys(errs).length) return setErrors(errs)
    m.mutate()
  }

  // Same formula as PricingCalculationService.calculateSubsidy (prorated above 2 kW, capped).
  const preview = (kw: number) => {
    const f = Number(first) || 0, t = Number(third) || 0, c = Number(max) || 0
    const s = Math.min(kw, 2) * f + Math.max(Math.min(kw, 3) - 2, 0) * t
    return Math.min(s, c)
  }

  return (
    <>
      <Card className="mb-5 p-4">
        <h2 className="mb-3 font-semibold">New subsidy rule</h2>
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <TextField label="Per kW, first 2 kW (₹)" inputMode="decimal" value={first} onChange={(e) => setFirst(money(e.target.value))} error={errors.subsidyPerKwFirstTwoKw} />
          <TextField label="For the 3rd kW (₹)" inputMode="decimal" value={third} onChange={(e) => setThird(money(e.target.value))} error={errors.subsidyForThirdKw} />
          <TextField label="Maximum subsidy (₹)" inputMode="decimal" value={max} onChange={(e) => setMax(money(e.target.value))} error={errors.maximumSubsidy} />
          <DateFields from={from} to={to} setFrom={setFrom} setTo={setTo} errors={errors} />
          <div className="sm:pt-7">
            <CheckboxField
              label="Estimate (not officially verified)"
              checked={estimated}
              onChange={(e) => setEstimated(e.target.checked)}
            />
          </div>
          <div className="rounded-md border border-line bg-canvas p-3 text-xs text-muted lg:col-span-3">
            Preview — 1 kW: <b className="text-ink">{formatINR(preview(1))}</b> · 2 kW: <b className="text-ink">{formatINR(preview(2))}</b> ·
            2.5 kW: <b className="text-ink">{formatINR(preview(2.5))}</b> · 3 kW+: <b className="text-ink">{formatINR(preview(3))}</b>
            <span className="ml-1">(indicative; the server calculation is final)</span>
          </div>
          <div className="flex items-center justify-between gap-3 lg:col-span-3">
            {formError ? <Alert tone="error">{formError}</Alert> : <span className="text-xs text-muted">Estimated rules are labelled as estimates on every quotation and PDF.</span>}
            <Button type="submit" loading={m.isPending}><Plus size={14} /> Add rule</Button>
          </div>
        </form>
      </Card>

      {list.isPending && <Spinner />}
      {list.isError && <Alert tone="error">{parseError(list.error).message}</Alert>}
      {list.data && (
        <Card className="overflow-x-auto">
          {list.data.length === 0 ? <EmptyState title="No subsidy rules" /> : (
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-line text-muted">
                <tr>
                  <th className="px-4 py-3 font-medium">First 2 kW</th>
                  <th className="px-4 py-3 font-medium">3rd kW</th>
                  <th className="px-4 py-3 font-medium">Cap</th>
                  <th className="px-4 py-3 font-medium">From</th>
                  <th className="px-4 py-3 font-medium">To</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {list.data.map((r) => (
                  <tr key={r.id} className={r.active ? '' : 'text-muted'}>
                    <td className="px-4 py-3">{formatINR(r.subsidyPerKwFirstTwoKw)}/kW</td>
                    <td className="px-4 py-3">{formatINR(r.subsidyForThirdKw)}</td>
                    <td className="px-4 py-3 font-medium">{formatINR(r.maximumSubsidy)}</td>
                    <td className="px-4 py-3">{formatDay(r.effectiveFrom)}</td>
                    <td className="px-4 py-3">{formatDay(r.effectiveTo)}</td>
                    <td className="space-x-1 px-4 py-3">
                      <RuleBadges active={r.active} from={r.effectiveFrom} to={r.effectiveTo} />
                      {r.estimated && <Badge tone="warning">Unverified</Badge>}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button variant="ghost" loading={toggle.isPending && toggle.variables?.id === r.id} onClick={() => toggle.mutate({ id: r.id, active: !r.active })}>
                        {r.active ? 'Deactivate' : 'Activate'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}
    </>
  )
}

export default function RulesPage() {
  const [tab, setTab] = useState<Tab>('pricing')
  return (
    <>
      <PageHeader
        title="Pricing & subsidy rules"
        description="Effective-dated. Each quotation snapshots the rule in force on its date, so changes here never alter an issued quote. Example values are hypothetical."
      />
      <div className="mb-5 inline-flex rounded-md border border-line bg-surface p-0.5" role="tablist">
        {(['pricing', 'subsidy'] as Tab[]).map((t) => (
          <button
            key={t}
            role="tab"
            aria-selected={tab === t}
            onClick={() => setTab(t)}
            className={`rounded px-4 py-1.5 text-sm ${tab === t ? 'bg-brand text-white' : 'text-muted hover:text-ink'}`}
          >
            {t === 'pricing' ? 'Pricing' : 'Subsidy'}
          </button>
        ))}
      </div>
      {tab === 'pricing' ? <PricingRules /> : <SubsidyRules />}
    </>
  )
}