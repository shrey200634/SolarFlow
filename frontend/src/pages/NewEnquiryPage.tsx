import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'
import { ArrowLeft } from 'lucide-react'

import { leadsApi } from '../api/leads.api'
import { pincodesApi } from '../api/pincodes.api'
import { parseError } from '../api/client'
import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Button } from '../components/common/Button'
import { SelectField, TextField } from '../components/forms/fields'
import { Alert } from '../components/feedback/Alert'
import { useToast } from '../components/feedback/toast-context'
import { ownershipLabel, roofTypeLabel } from '../features/leads/labels'
import type { Ownership, RoofType } from '../types/api'

const ownerships = Object.keys(ownershipLabel) as Ownership[]
const roofTypes = Object.keys(roofTypeLabel) as RoofType[]

const schema = z.object({
  ownership: z.enum(['OWNED', 'RENTED', 'LEASED'], { error: 'Choose one' }),
  roofType: z.enum(['RCC_FLAT', 'TIN_SHEET', 'TILED', 'ASBESTOS', 'OTHER'], { error: 'Choose one' }),
  roofAreaSqft: z
    .string()
    .min(1, 'Enter your roof area')
    .refine((v) => Number(v) >= 50, 'Roof area must be at least 50 sq ft')
    .refine((v) => Number(v) <= 100000, 'Roof area looks too large'),
  declaredMonthlyBill: z
    .string()
    .min(1, 'Enter your monthly bill')
    .refine((v) => Number(v) >= 1, 'Enter an amount above ₹0')
    .refine((v) => Number(v) <= 10000000, 'Amount looks too large'),
  pincode: z.string().regex(/^[1-9][0-9]{5}$/, 'Enter a 6-digit pincode'),
  phone: z
    .string()
    .regex(/^(\+?91[\s-]?|0)?[6-9][0-9]{4}[\s-]?[0-9]{5}$/, 'Enter a valid Indian mobile number'),
})
type FormValues = z.infer<typeof schema>

export default function NewEnquiryPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { roofAreaSqft: '', declaredMonthlyBill: '', pincode: '', phone: '' },
  })

  // Friendly early signal only; the backend still accepts a pincode we don't serve yet.
  const pincode = useWatch({ control, name: 'pincode' })
  const pincodeValid = /^[1-9][0-9]{5}$/.test(pincode ?? '')
  const availability = useQuery({
    queryKey: ['pincode', pincode],
    queryFn: () => pincodesApi.check(pincode),
    enabled: pincodeValid,
    staleTime: 5 * 60_000,
  })

  const create = useMutation({
    mutationFn: (v: FormValues) =>
      leadsApi.create({
        ownership: v.ownership,
        roofType: v.roofType,
        roofAreaSqft: Number(v.roofAreaSqft),
        declaredMonthlyBill: Number(v.declaredMonthlyBill),
        pincode: v.pincode,
        phone: v.phone.trim(),
      }),
    onSuccess: async (lead) => {
      await queryClient.invalidateQueries({ queryKey: ['leads'] })
      toast.success('Enquiry created. Now upload your electricity bill.')
      navigate(`/app/${lead.id}/bill`, { replace: true })
    },
    onError: (e) => {
      const p = parseError(e)
      let mapped = false
      for (const key of Object.keys(schema.shape) as (keyof FormValues)[]) {
        if (p.fieldErrors[key]) {
          setError(key, { message: p.fieldErrors[key] })
          mapped = true
        }
      }
      if (!mapped) setFormError(p.message)
    },
  })

  return (
    <>
      <Link to="/app" className="mb-5 inline-flex items-center gap-2 text-sm text-muted hover:text-brand">
        <ArrowLeft size={16} /> Back to applications
      </Link>

      <PageHeader title="New solar enquiry" description="Tell us about your roof and electricity use. It takes about a minute." />

      <Card className="max-w-2xl p-5">
        <form
          onSubmit={handleSubmit((v) => {
            setFormError(null)
            create.mutate(v)
          })}
          className="space-y-4"
          noValidate
        >
          {formError && <Alert tone="error">{formError}</Alert>}

          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField label="Property ownership" defaultValue="" error={errors.ownership?.message} {...register('ownership')}>
              <option value="" disabled>Select…</option>
              {ownerships.map((o) => <option key={o} value={o}>{ownershipLabel[o]}</option>)}
            </SelectField>

            <SelectField label="Roof type" defaultValue="" error={errors.roofType?.message} {...register('roofType')}>
              <option value="" disabled>Select…</option>
              {roofTypes.map((r) => <option key={r} value={r}>{roofTypeLabel[r]}</option>)}
            </SelectField>

            <TextField label="Usable roof area (sq ft)" inputMode="decimal"
              hint="Shadow-free area. At least 50 sq ft."
              error={errors.roofAreaSqft?.message} {...register('roofAreaSqft')} />

            <TextField label="Average monthly bill (₹)" inputMode="decimal"
              error={errors.declaredMonthlyBill?.message} {...register('declaredMonthlyBill')} />

            <TextField label="Pincode" inputMode="numeric" maxLength={6} autoComplete="postal-code"
              error={errors.pincode?.message} {...register('pincode')} />

            <TextField label="Mobile number" type="tel" autoComplete="tel"
              hint="Our team may call you on this number."
              error={errors.phone?.message} {...register('phone')} />
          </div>

          {pincodeValid && availability.data && !availability.data.serviceable && (
            <Alert tone="warning" title="We may not install in this area yet">
              You can still submit. Our team will confirm availability with you.
            </Alert>
          )}
          {pincodeValid && availability.data?.serviceable && (
            <Alert tone="success">Good news, we install in {pincode}.</Alert>
          )}

          <div className="flex justify-end gap-2 pt-2">
            <Link to="/app"
              className="inline-flex h-9 items-center rounded-md border border-line bg-surface px-3.5 text-sm font-medium hover:bg-canvas">
              Cancel
            </Link>
            <Button type="submit" loading={create.isPending}>Create enquiry</Button>
          </div>
        </form>
      </Card>
    </>
  )
}