import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../app/useAuth'
import { LoadingState } from '../components/app/AppUI'

export default function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth()
  const location = useLocation()
  if (isInitializing) return <LoadingState label="Restoring your session" />
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />
}
