import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Save } from 'lucide-react'

import { leadsApi } from '../../api/leads.api'
import { parseError } from '../../api/client'
import { Card } from '../../components/common/Card'
import { Button } from '../../components/common/Button'
import { SelectField, TextField } from '../../components/forms/fields'
import { ConfirmDialog } from '../../components/feedback/ConfirmDialog'
import { useToast } from '../../components/feedback/toast-context'
import { ownershipLabel, roofTypeLabel } from '../leads/labels'
import type { Lead, Ownership, RoofType, UpdateLeadRequest } from '../../types/api'

/**
 * Staff corrections after the offline roof survey (blueprint R6), assignment, and lost/reopen.
 * Sizing reads lead roof data, so options refresh after a save. Re-run qualification to rescore.
 */
export function LeadEditPanel({ lead }: { lead: Lead }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [ownership, setOwnership] = useState<Ownership>(lead.ownership)
  const [roofType, setRoofType] = useState<RoofType>(lead.roofType)
  const [roofArea, setRoofArea] = useState(String(lead.roofAreaSqft))
  const [assignedTo, setAssignedTo] = useState(lead.assignedToId ?? '')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [confirmLost, setConfirmLost] = useState(false)

  const staff = useQuery({ queryKey: ['staff-users'], queryFn: leadsApi.staffUsers, staleTime: 5 * 60_000 })

  const refresh = () =>
    Promise.all(
      [['lead', lead.id], ['leads'], ['lead-stats'], ['recommendations', lead.id]].map((queryKey) =>
        queryClient.invalidateQueries({ queryKey }),
      ),
    )

  const save = useMutation({
    mutationFn: (body: UpdateLeadRequest) => leadsApi.update(lead.id, body),
    onSuccess: async (_d, body) => {
      setConfirmLost(false)
      setFieldErrors({})
      toast.success(body.status === 'LOST' ? 'Lead marked as lost.' : body.status ? 'Lead reopened.' : 'Lead updated.')
      await refresh()
    },
    onError: (e) => {
      setConfirmLost(false)
      const p = parseError(e)
      setFieldErrors(p.fieldErrors)
      if (Object.keys(p.fieldErrors).length === 0) toast.error(p.message)
    },
  })

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    const area = Number(roofArea)
    if (!roofArea || Number.isNaN(area) || area < 50) return setFieldErrors({ roofAreaSqft: 'Roof area must be at least 50 sq ft' })
    const body: UpdateLeadRequest = {}
    if (ownership !== lead.ownership) body.ownership = ownership
    if (roofType !== lead.roofType) body.roofType = roofType
    if (area !== Number(lead.roofAreaSqft)) body.roofAreaSqft = area
    if (assignedTo && assignedTo !== lead.assignedToId) body.assignedTo = assignedTo
    if (Object.keys(body).length === 0) return toast.info('Nothing changed.')
    save.mutate(body)
  }

  const closed = lead.status === 'ACCEPTED'

  return (
    <Card className="p-4 sm:p-5">
      <h2 className="font-semibold">Survey corrections & assignment</h2>
      <p className="mt-1 text-sm text-muted">Update roof details after the site visit, then re-run qualification.</p>

      <form onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2">
        <SelectField label="Property" value={ownership} onChange={(e) => setOwnership(e.target.value as Ownership)} disabled={closed}>
          {(Object.keys(ownershipLabel) as Ownership[]).map((o) => <option key={o} value={o}>{ownershipLabel[o]}</option>)}
        </SelectField>
        <SelectField label="Roof type" value={roofType} onChange={(e) => setRoofType(e.target.value as RoofType)} disabled={closed}>
          {(Object.keys(roofTypeLabel) as RoofType[]).map((r) => <option key={r} value={r}>{roofTypeLabel[r]}</option>)}
        </SelectField>
        <TextField
          label="Usable roof area (sq ft)"
          inputMode="decimal"
          value={roofArea}
          onChange={(e) => setRoofArea(e.target.value.replace(/[^\d.]/g, ''))}
          error={fieldErrors.roofAreaSqft}
          disabled={closed}
        />
        <SelectField label="Assigned to" value={assignedTo} onChange={(e) => setAssignedTo(e.target.value)} error={fieldErrors.assignedTo}>
          <option value="" disabled>{staff.isPending ? 'Loading...' : 'Choose a person'}</option>
          {staff.data?.map((u) => <option key={u.id} value={u.id}>{u.fullName} ({u.role === 'ADMIN' ? 'Admin' : 'Sales'})</option>)}
        </SelectField>

        <div className="flex flex-wrap items-center justify-between gap-2 sm:col-span-2">
          {lead.status === 'LOST' ? (
            <Button variant="secondary" loading={save.isPending} onClick={() => save.mutate({ status: 'SALES_CALL_PENDING' })}>
              Reopen lead
            </Button>
          ) : !closed ? (
            <Button variant="ghost" className="text-danger" onClick={() => setConfirmLost(true)}>Mark as lost</Button>
          ) : <span />}
          <Button type="submit" loading={save.isPending && !confirmLost}><Save size={14} /> Save changes</Button>
        </div>
      </form>

      <ConfirmDialog
        open={confirmLost}
        danger
        title="Mark this lead as lost?"
        message="The customer will see the enquiry as closed. You can reopen it later."
        confirmLabel="Mark as lost"
        loading={save.isPending}
        onConfirm={() => save.mutate({ status: 'LOST' })}
        onCancel={() => setConfirmLost(false)}
      />
    </Card>
  )
}