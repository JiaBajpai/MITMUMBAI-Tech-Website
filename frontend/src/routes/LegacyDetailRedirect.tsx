import { Navigate, useParams } from 'react-router-dom'

export function LegacyTaskRedirect() {
  const { id } = useParams()
  return <Navigate to={`/app/tasks/${id}`} replace />
}

export function LegacySessionRedirect() {
  const { id } = useParams()
  return <Navigate to={`/app/sessions/${id}`} replace />
}

export function LegacyProjectRedirect() {
  const { id } = useParams()
  return <Navigate to={`/app/projects/${id}`} replace />
}
