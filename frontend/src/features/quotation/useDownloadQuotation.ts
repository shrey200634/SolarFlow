import { quotationsApi } from '../../api/quotations.api'
import { parseError } from '../../api/client'
import { useToast } from '../../components/feedback/toast-context'
import { formatDate } from '../leads/labels'
import type { Quotation } from '../../types/api'

/** Returns a function that fetches the quotation PDF (needs the auth header) and saves it. */
export function useDownloadQuotation() {
  const toast = useToast()
  return async (q: Quotation) => {
    try {
      const url = URL.createObjectURL(await quotationsApi.pdf(q.id))
      const a = document.createElement('a')
      a.href = url
      a.download = `solarflow-quotation-${formatDate(q.createdAt).replaceAll(' ', '-')}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      toast.error(parseError(e).message)
    }
  }
}