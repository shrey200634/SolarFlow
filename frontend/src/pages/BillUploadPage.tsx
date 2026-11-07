import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, FileText, UploadCloud } from 'lucide-react'

import { leadsApi } from '../api/leads.api'
import { documentsApi, type LeadDocument } from '../api/documents.api'
import { parseError } from '../api/client'

import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Button } from '../components/common/Button'
import { Spinner } from '../components/common/Loading'
import { Alert } from '../components/feedback/Alert'
import { useToast } from '../components/feedback/toast-context'

const MAX_FILE_SIZE = 8 * 1024 * 1024

const ALLOWED_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
]

function formatFileSize(bytes: number) {
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

function statusLabel(status: LeadDocument['status']) {
  const labels: Record<LeadDocument['status'], string> = {
    UPLOADED: 'Uploaded',
    EXTRACTING: 'Processing',
    EXTRACTED: 'Details extracted',
    LOW_QUALITY: 'Low quality',
    FAILED: 'Processing failed',
    CONFIRMED: 'Confirmed',
  }

  return labels[status]
}

export default function BillUploadPage() {
  const { leadId } = useParams<{ leadId: string }>()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [validationError, setValidationError] = useState('')

  const queryClient = useQueryClient()
  const toast = useToast()

  const leadQuery = useQuery({
    queryKey: ['lead', leadId],
    queryFn: () => leadsApi.get(leadId!),
    enabled: Boolean(leadId),
  })

  const documentsQuery = useQuery({
    queryKey: ['documents', leadId],
    queryFn: () => documentsApi.list(leadId!),
    enabled: Boolean(leadId),
  })

  const uploadMutation = useMutation({
    mutationFn: () => {
      if (!leadId || !selectedFile) {
        throw new Error('Please select a bill before uploading.')
      }

      return documentsApi.upload(leadId, selectedFile)
    },
    onSuccess: async () => {
      toast.success('Electricity bill uploaded successfully.')
      setSelectedFile(null)
      setValidationError('')

      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }

      await queryClient.invalidateQueries({
        queryKey: ['documents', leadId],
      })

      await queryClient.invalidateQueries({
        queryKey: ['leads'],
      })
    },
    onError: (error) => {
      toast.error(parseError(error).message)
    },
  })

  function handleFileChange(file?: File) {
    setValidationError('')
    setSelectedFile(null)

    if (!file) return

    if (!ALLOWED_TYPES.includes(file.type)) {
      setValidationError('Only PDF, JPG and PNG files are supported.')
      return
    }

    if (file.size > MAX_FILE_SIZE) {
      setValidationError('The file must be smaller than 8 MB.')
      return
    }

    if (file.size === 0) {
      setValidationError('The selected file is empty.')
      return
    }

    setSelectedFile(file)
  }

  if (leadQuery.isPending) {
    return <Spinner label="Loading application..." />
  }

  if (leadQuery.isError || !leadQuery.data) {
    return (
      <Card className="p-4">
        <Alert tone="error" title="Could not load this application">
          {leadQuery.isError
            ? parseError(leadQuery.error).message
            : 'Application not found.'}
        </Alert>
        <Link to="/app" className="mt-4 inline-flex text-sm text-brand hover:underline">
          Back to applications
        </Link>
      </Card>
    )
  }

  const lead = leadQuery.data

  return (
    <>
      <Link
        to="/app"
        className="mb-5 inline-flex items-center gap-2 text-sm text-muted hover:text-brand"
      >
        <ArrowLeft size={16} />
        Back to applications
      </Link>

      <PageHeader
        title="Upload electricity bill"
        description="Upload a recent electricity bill so we can review your solar requirements."
      />

      <Card className="mb-5 p-4 sm:p-5">
        <p className="text-sm font-medium">Application details</p>

        <div className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <p className="text-muted">Roof type</p>
            <p className="font-medium">{lead.roofType.replaceAll('_', ' ')}</p>
          </div>

          <div>
            <p className="text-muted">Monthly electricity bill</p>
            <p className="font-medium">
              ₹{Number(lead.declaredMonthlyBill).toLocaleString('en-IN')}
            </p>
          </div>

          <div>
            <p className="text-muted">Pincode</p>
            <p className="font-medium">{lead.pincode}</p>
          </div>

          <div>
            <p className="text-muted">Roof area</p>
            <p className="font-medium">{lead.roofAreaSqft} sq ft</p>
          </div>
        </div>
      </Card>

      <Card className="p-4 sm:p-6">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-soft text-brand">
            <FileText size={20} />
          </span>
          <div>
            <h2 className="font-semibold">Your electricity bill</h2>
            <p className="text-sm text-muted">
              PDF, JPG or PNG · Maximum 8 MB
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="mt-5 flex w-full flex-col items-center justify-center rounded-lg border-2 border-dashed border-line bg-canvas px-4 py-9 text-center transition-colors hover:border-brand"
        >
          <UploadCloud size={28} className="text-brand" />

          <span className="mt-3 text-sm font-medium">
            Choose an electricity bill
          </span>

          <span className="mt-1 text-xs text-muted">
            Select a file from your device
          </span>
        </button>

        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
          className="hidden"
          onChange={(event) => handleFileChange(event.target.files?.[0])}
        />

        {selectedFile && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-md border border-line p-3">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium">{selectedFile.name}</p>
              <p className="text-xs text-muted">
                {formatFileSize(selectedFile.size)}
              </p>
            </div>

            <button
              type="button"
              onClick={() => {
                setSelectedFile(null)
                if (fileInputRef.current) fileInputRef.current.value = ''
              }}
              className="text-sm text-danger hover:underline"
            >
              Remove
            </button>
          </div>
        )}

        {validationError && (
          <div className="mt-3">
            <Alert tone="error" title="Invalid file">
              {validationError}
            </Alert>
          </div>
        )}

       <div className="mt-5">
  <button
    type="button"
    disabled={!selectedFile || uploadMutation.isPending}
    onClick={() => uploadMutation.mutate()}
    className="flex w-full items-center justify-center rounded-md bg-brand px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-brand-hover disabled:cursor-not-allowed disabled:opacity-50"
  >
    {uploadMutation.isPending ? 'Uploading...' : 'Upload bill'}
  </button>
</div>
      </Card>

      <Card className="mt-5 p-4 sm:p-5">
        <h2 className="font-semibold">Uploaded documents</h2>

        {documentsQuery.isPending && <Spinner label="Loading documents..." />}

        {documentsQuery.isError && (
          <div className="mt-3">
            <Alert tone="error" title="Could not load uploaded documents">
              {parseError(documentsQuery.error).message}
            </Alert>
            <Button
              variant="secondary"
              className="mt-3"
              onClick={() => documentsQuery.refetch()}
            >
              Retry
            </Button>
          </div>
        )}

        {documentsQuery.data && documentsQuery.data.length === 0 && (
          <p className="mt-3 text-sm text-muted">
            No bill has been uploaded for this application yet.
          </p>
        )}

        {documentsQuery.data && documentsQuery.data.length > 0 && (
          <div className="mt-3 divide-y divide-line">
            {documentsQuery.data.map((document) => (
              <div
                key={document.id}
                className="flex flex-wrap items-center justify-between gap-3 py-3"
              >
                <div>
                  <p className="text-sm font-medium">
                    Electricity bill
                  </p>
                  <p className="text-xs text-muted">
                    {formatFileSize(document.sizeBytes)} ·{' '}
                    {new Date(document.createdAt).toLocaleDateString('en-IN')}
                  </p>
                </div>

                <span className="rounded-full bg-brand-soft px-2.5 py-1 text-xs font-medium text-brand">
                  {statusLabel(document.status)}
                </span>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  )
}