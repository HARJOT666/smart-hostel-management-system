import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, homeFor, useAuth } from './auth/AuthContext'
import ProtectedRoute from './auth/ProtectedRoute'
import Layout from './components/Layout'
import { ToastProvider } from './components/Toast'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import StudentDashboard from './pages/student/StudentDashboard'
import MyRoom from './pages/student/MyRoom'
import MyComplaints from './pages/student/MyComplaints'
import MyLeaves from './pages/student/MyLeaves'
import LeaveLetter from './pages/student/LeaveLetter'
import MyVisitors from './pages/student/MyVisitors'
import MyFees from './pages/student/MyFees'
import NoticesPage from './pages/shared/NoticesPage'
import MessMenuPage from './pages/shared/MessMenuPage'
import ContactsPage from './pages/shared/ContactsPage'
import MarketplacePage from './pages/shared/MarketplacePage'
import LostFoundPage from './pages/shared/LostFoundPage'
import InventoryPage from './pages/shared/InventoryPage'
import AdminDashboard from './pages/admin/AdminDashboard'
import StudentsPage from './pages/admin/StudentsPage'
import StaffPage from './pages/admin/StaffPage'
import HostelsPage from './pages/admin/HostelsPage'
import RoomsPage from './pages/admin/RoomsPage'
import AllocationsPage from './pages/admin/AllocationsPage'
import ComplaintsAdmin from './pages/admin/ComplaintsAdmin'
import LeavesAdmin from './pages/admin/LeavesAdmin'
import VisitorsAdmin from './pages/admin/VisitorsAdmin'
import FeesAdmin from './pages/admin/FeesAdmin'
import ReportsPage from './pages/admin/ReportsPage'
import MaintDashboard from './pages/maintenance/MaintDashboard'
import MyJobs from './pages/maintenance/MyJobs'

function Home() {
  const { user } = useAuth()
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route element={<ProtectedRoute roles={['STUDENT']} />}>
              <Route path="/student" element={<Layout />}>
                <Route index element={<StudentDashboard />} />
                <Route path="room" element={<MyRoom />} />
                <Route path="complaints" element={<MyComplaints />} />
                <Route path="leaves" element={<MyLeaves />} />
                <Route path="leaves/:id/letter" element={<LeaveLetter />} />
                <Route path="visitors" element={<MyVisitors />} />
                <Route path="fees" element={<MyFees />} />
                <Route path="notices" element={<NoticesPage />} />
                <Route path="mess" element={<MessMenuPage />} />
                <Route path="marketplace" element={<MarketplacePage />} />
                <Route path="lost-found" element={<LostFoundPage />} />
                <Route path="contacts" element={<ContactsPage />} />
              </Route>
            </Route>

            <Route element={<ProtectedRoute roles={['ADMIN', 'WARDEN']} />}>
              <Route path="/admin" element={<Layout />}>
                <Route index element={<AdminDashboard />} />
                <Route path="students" element={<StudentsPage />} />
                <Route path="staff" element={<StaffPage />} />
                <Route path="hostels" element={<HostelsPage />} />
                <Route path="rooms" element={<RoomsPage />} />
                <Route path="allocations" element={<AllocationsPage />} />
                <Route path="complaints" element={<ComplaintsAdmin />} />
                <Route path="leaves" element={<LeavesAdmin />} />
                <Route path="visitors" element={<VisitorsAdmin />} />
                <Route path="fees" element={<FeesAdmin />} />
                <Route path="notices" element={<NoticesPage />} />
                <Route path="mess" element={<MessMenuPage />} />
                <Route path="inventory" element={<InventoryPage />} />
                <Route path="marketplace" element={<MarketplacePage />} />
                <Route path="lost-found" element={<LostFoundPage />} />
                <Route path="contacts" element={<ContactsPage />} />
                <Route path="reports" element={<ReportsPage />} />
              </Route>
            </Route>

            <Route element={<ProtectedRoute roles={['MAINTENANCE']} />}>
              <Route path="/maintenance" element={<Layout />}>
                <Route index element={<MaintDashboard />} />
                <Route path="jobs" element={<MyJobs />} />
                <Route path="inventory" element={<InventoryPage />} />
                <Route path="notices" element={<NoticesPage />} />
                <Route path="contacts" element={<ContactsPage />} />
              </Route>
            </Route>

            <Route path="*" element={<Home />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
