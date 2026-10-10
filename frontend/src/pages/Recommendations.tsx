import { Navigate } from 'react-router-dom'

/** Legacy route retained for older imports; recommendations are not currently available. */
export default function Recommendations() {
  return <Navigate to="/app/recommendations" replace />
}
