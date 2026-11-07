import { useState } from 'react'
import { Copy, ExternalLink } from 'lucide-react'
import { Button } from '../../components/common/Button'
import { useToast } from '../../components/feedback/toast-context'
import { publicStatusUrl } from './labels'

/** Shows the shareable customer status URL with copy / open buttons. */
export function StatusLinkBox({ statusLink }: { statusLink: string | null }) {
  const toast = useToast()
  const [copied, setCopied] = useState(false)
  const url = publicStatusUrl(statusLink)

  if (!url) return <p className="text-sm text-muted">The public status link is turned off.</p>

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      toast.error('Could not copy. Select the link and copy it manually.')
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <input
        readOnly
        value={url}
        onFocus={(e) => e.currentTarget.select()}
        className="h-9 min-w-0 flex-1 rounded-md border border-line bg-canvas px-3 font-mono text-xs"
        aria-label="Public status link"
      />
      <Button variant="secondary" onClick={copy}><Copy size={14} /> {copied ? 'Copied' : 'Copy'}</Button>
      <a href={url} target="_blank" rel="noreferrer" className="inline-flex h-9 items-center gap-1 px-2 text-sm text-brand hover:underline">
        <ExternalLink size={14} /> Open
      </a>
    </div>
  )
}