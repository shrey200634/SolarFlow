import { PageHeader } from '../components/common/PageHeader'
import { Card } from '../components/common/Card'
import { Alert } from '../components/feedback/Alert'

export default function HomeownerHome() {
  return (
    <>
      <PageHeader title="My applications" />
      <Card className="p-4">
        <Alert tone="info" title="Under development">
          Enquiries, bill upload and quotations are added in the next phase. This page lists your applications once they are connected.
        </Alert>
      </Card>
    </>
  )
}