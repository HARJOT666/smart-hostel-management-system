import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const NAV = {
  STUDENT: [
    ['Overview', [['/student', 'Dashboard'], ['/student/room', 'My Room'], ['/student/notices', 'Notice Board']]],
    ['Requests', [['/student/complaints', 'Complaints'], ['/student/leaves', 'Leave'], ['/student/visitors', 'Visitor Passes'], ['/student/fees', 'Fees']]],
    ['Hostel Life', [['/student/mess', 'Mess Menu'], ['/student/marketplace', 'Marketplace'], ['/student/lost-found', 'Lost & Found'], ['/student/contacts', 'Emergency Contacts']]],
  ],
  ADMIN: [
    ['Overview', [['/admin', 'Dashboard'], ['/admin/reports', 'Reports']]],
    ['Residents', [['/admin/students', 'Students'], ['/admin/allocations', 'Room Allocation'], ['/admin/rooms', 'Rooms & Vacancy'], ['/admin/hostels', 'Hostels']]],
    ['Requests', [['/admin/complaints', 'Complaints'], ['/admin/leaves', 'Leave Approvals'], ['/admin/visitors', 'Visitors'], ['/admin/fees', 'Fees']]],
    ['Operations', [['/admin/staff', 'Staff'], ['/admin/notices', 'Notice Board'], ['/admin/mess', 'Mess Menu'], ['/admin/inventory', 'Inventory'], ['/admin/marketplace', 'Marketplace'], ['/admin/lost-found', 'Lost & Found'], ['/admin/contacts', 'Emergency Contacts']]],
  ],
  MAINTENANCE: [
    ['Work', [['/maintenance', 'Dashboard'], ['/maintenance/jobs', 'My Jobs'], ['/maintenance/inventory', 'Inventory']]],
    ['Hostel', [['/maintenance/notices', 'Notice Board'], ['/maintenance/contacts', 'Contacts']]],
  ],
}
NAV.WARDEN = NAV.ADMIN

const ROLE_LABEL = { STUDENT: 'Student', ADMIN: 'Administrator', WARDEN: 'Warden', MAINTENANCE: 'Maintenance' }

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const sections = NAV[user.role] || []

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">SH</div>
          <div>
            <div className="brand-name">Smart Hostel</div>
            <div className="brand-sub">Management System</div>
          </div>
        </div>
        <nav>
          {sections.map(([title, links]) => (
            <div key={title}>
              <div className="nav-section">{title}</div>
              {links.map(([to, label]) => (
                <NavLink key={to} to={to} end={to.split('/').length === 2} className="nav-link">{label}</NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="who">{user.name}</div>
          <div className="role">{ROLE_LABEL[user.role]} · {user.username}</div>
          <button className="btn btn-sm" onClick={() => { logout(); navigate('/login') }}>Sign out</button>
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
