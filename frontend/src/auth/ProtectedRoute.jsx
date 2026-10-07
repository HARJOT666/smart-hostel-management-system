import { Navigate, Outlet } from 'react-router-dom'
import { homeFor, useAuth } from './AuthContext'

/** Only lets logged-in users with one of the allowed roles see the nested routes. */
export default function ProtectedRoute({ roles }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />
  return <Outlet />
}
