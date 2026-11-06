import { Link } from 'react-router-dom'
import { EmptyState } from '../components/common/EmptyState'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-full items-center justify-center">
      <EmptyState
        title="Page not found"
        description="The address may be wrong or the page has moved."
        action={<Link to="/" className="text-sm font-medium text-brand hover:underline">Go to home</Link>}
      />
    </div>
  )
}