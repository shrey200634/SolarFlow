import { Route, Routes } from 'react-router-dom'
import { GuestOnly, RequireAuth, RoleRedirect } from './routes/guards'
import { AppShell } from './layouts/AppShell'
import { adminNav, homeownerNav, staffNav } from './layouts/nav'
import { useAuth } from './features/auth/auth-context'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomeownerHome from './pages/HomeownerHome'
import NewEnquiryPage from './pages/NewEnquiryPage'
import BillUploadPage from './pages/BillUploadPage'
import LeadDetailPage from './pages/LeadDetailPage'
import StaffHome from './pages/StaffHome'
import StaffLeadDetailPage from './pages/StaffLeadDetailPage'
import StaffProjectsPage from './pages/StaffProjectsPage'
import StaffProjectDetailPage from './pages/StaffProjectDetailPage'
import PincodesPage from './pages/admin/PincodesPage'
import RulesPage from './pages/admin/RulesPage'
import PublicStatusPage from './pages/PublicStatusPage'
import NotFoundPage from './pages/NotFoundPage'

/** Same shell for SALES and ADMIN; admins get the extra config links. */
function StaffShell() {
  const { user } = useAuth()
  return <AppShell nav={user?.role === 'ADMIN' ? adminNav : staffNav} />
}

export default function App() {
  return (
    <Routes>
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Public, token-secured customer status page. No login. */}
      <Route path="/status/:token" element={<PublicStatusPage />} />

      <Route element={<RequireAuth roles={['HOMEOWNER']} />}>
        <Route path="/app" element={<AppShell nav={homeownerNav} />}>
          <Route index element={<HomeownerHome />} />
          <Route path="new" element={<NewEnquiryPage />} />
          <Route path=":leadId" element={<LeadDetailPage />} />
          <Route path=":leadId/bill" element={<BillUploadPage />} />
        </Route>
      </Route>

      <Route element={<RequireAuth roles={['SALES', 'ADMIN']} />}>
        <Route path="/staff" element={<StaffShell />}>
          <Route index element={<StaffHome />} />
          <Route path="leads/:leadId" element={<StaffLeadDetailPage />} />
          <Route path="projects" element={<StaffProjectsPage />} />
          <Route path="projects/:projectId" element={<StaffProjectDetailPage />} />
        </Route>
      </Route>

      <Route element={<RequireAuth roles={['ADMIN']} />}>
        <Route path="/admin" element={<StaffShell />}>
          <Route path="pincodes" element={<PincodesPage />} />
          <Route path="rules" element={<RulesPage />} />
        </Route>
      </Route>

      <Route path="/" element={<RoleRedirect />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}