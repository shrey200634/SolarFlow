import { Route, Routes } from 'react-router-dom'
import { GuestOnly, RequireAuth, RoleRedirect } from './routes/guards'
import { AppShell } from './layouts/AppShell'
import { homeownerNav, staffNav } from './layouts/nav'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomeownerHome from './pages/HomeownerHome'
import NewEnquiryPage from './pages/NewEnquiryPage'
import BillUploadPage from './pages/BillUploadPage'
import StaffHome from './pages/StaffHome'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route element={<RequireAuth roles={['HOMEOWNER']} />}>
        <Route path="/app" element={<AppShell nav={homeownerNav} />}>
          <Route index element={<HomeownerHome />} />
          <Route path="new" element={<NewEnquiryPage />} />
          <Route path=":leadId/bill" element={<BillUploadPage />} />
        </Route>
      </Route>

      {/* ADMIN is allowed here too: every sales endpoint accepts ADMIN. */}
      <Route element={<RequireAuth roles={['SALES', 'ADMIN']} />}>
        <Route path="/staff" element={<AppShell nav={staffNav} />}>
          <Route index element={<StaffHome />} />
        </Route>
      </Route>

      <Route path="/" element={<RoleRedirect />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}