import { Navigate } from 'react-router-dom'

// Legacy page retained for compatibility with older imports. Public event
// listings remain empty until real, publishable session data is available.
export default function Sessions() {
  return <Navigate to="/events" replace />
}
