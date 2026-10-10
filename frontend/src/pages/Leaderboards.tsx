import { Navigate } from 'react-router-dom'

/** Legacy route retained for older imports; the authenticated API-backed page is canonical. */
export default function Leaderboards() {
  return <Navigate to="/app/leaderboard" replace />
}
