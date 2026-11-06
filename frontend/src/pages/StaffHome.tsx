import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Alert } from '../components/feedback/Alert'

export default function StaffHome() {
  return (
    <>
      <PageHeader title="Overview" />
      <Card className="p-4">
        <Alert tone="info" title="Under development">
          The lead table and lead details are added in the sales phase.
        </Alert>
      </Card>
    </>
  )
}