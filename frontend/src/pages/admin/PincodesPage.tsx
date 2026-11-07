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
import { TextField } from '../../components/forms/fields'
import { Alert } from '../../components/feedback/Alert'
import { useToast } from '../../components/feedback/toast-context'
import { formatDate } from '../../features/leads/labels'

const PIN = /^[1-9][0-9]{5}$/

export default function PincodesPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [pincode, setPincode] = useState('')
  const [areaName, setAreaName] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [filter, setFilter] = useState('')

  const list = useQuery({ queryKey: ['admin', 'pincodes'], queryFn: adminApi.pincodes })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin', 'pincodes'] })

  const add = useMutation({
    mutationFn: () => adminApi.addPincode({ pincode, areaName: areaName.trim() || undefined }),
    onSuccess: async (p) => {
      toast.success(`${p.pincode} added.`)
      setPincode(''); setAreaName(''); setErrors({})
      await refresh()
    },
    onError: (e) => {
      const p = parseError(e)
      setErrors(p.status === 409 ? { pincode: 'This pincode is already in the list' } : p.fieldErrors)
      if (p.status !== 409 && !Object.keys(p.fieldErrors).length) toast.error(p.message)
    },
  })

  const toggle = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => adminApi.setPincodeActive(id, active),
    onSuccess: async (p) => { toast.success(`${p.pincode} ${p.active ? 'activated' : 'deactivated'}.`); await refresh() },
    onError: (e) => toast.error(parseError(e).message),
  })

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!PIN.test(pincode)) return setErrors({ pincode: 'Enter a 6-digit pincode' })
    add.mutate()
  }

  const rows = (list.data ?? []).filter(
    (p) => !filter || p.pincode.includes(filter) || p.areaName?.toLowerCase().includes(filter.toLowerCase()),
  )
  const activeCount = list.data?.filter((p) => p.active).length ?? 0

  return (
    <>
      <PageHeader
        title="Serviceable pincodes"
        description="Leads outside an active pincode lose the pincode points and cannot be quoted. Rows are never deleted, only deactivated."
      />

      <Card className="mb-5 p-4">
        <form onSubmit={submit} className="grid items-start gap-3 sm:grid-cols-[160px_1fr_auto]">
          <TextField
            label="Pincode"
            inputMode="numeric"
            maxLength={6}
            value={pincode}
            onChange={(e) => setPincode(e.target.value.replace(/\D/g, ''))}
            error={errors.pincode}
            placeholder="440010"
          />
          <TextField label="Area name (optional)" maxLength={100} value={areaName} onChange={(e) => setAreaName(e.target.value)} placeholder="Dharampeth, Nagpur" />
          <Button type="submit" loading={add.isPending} className="sm:mt-6"><Plus size={14} /> Add</Button>
        </form>
      </Card>

      {list.isPending && <Spinner />}
      {list.isError && <Alert tone="error" title="Could not load pincodes">{parseError(list.error).message}</Alert>}

      {list.data && (
        <Card>
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line p-4">
            <p className="text-sm text-muted">{activeCount} active of {list.data.length}</p>
            <input
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Filter by pincode or area"
              className="h-9 w-56 rounded-md border border-line px-3 text-sm"
              aria-label="Filter pincodes"
            />
          </div>
          {rows.length === 0 ? (
            <EmptyState title="No pincodes" description={filter ? 'Nothing matches the filter.' : 'Add the first serviceable pincode above.'} />
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line text-muted">
                <tr>
                  <th className="px-4 py-3 font-medium">Pincode</th>
                  <th className="px-4 py-3 font-medium">Area</th>
                  <th className="px-4 py-3 font-medium">Added</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {rows.map((p) => (
                  <tr key={p.id} className={p.active ? '' : 'text-muted'}>
                    <td className="px-4 py-3 font-mono font-medium">{p.pincode}</td>
                    <td className="px-4 py-3">{p.areaName ?? '—'}</td>
                    <td className="px-4 py-3">{formatDate(p.createdAt)}</td>
                    <td className="px-4 py-3"><Badge tone={p.active ? 'success' : 'neutral'}>{p.active ? 'Active' : 'Inactive'}</Badge></td>
                    <td className="px-4 py-3 text-right">
                      <Button
                        variant="ghost"
                        loading={toggle.isPending && toggle.variables?.id === p.id}
                        onClick={() => toggle.mutate({ id: p.id, active: !p.active })}
                      >
                        {p.active ? 'Deactivate' : 'Activate'}
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