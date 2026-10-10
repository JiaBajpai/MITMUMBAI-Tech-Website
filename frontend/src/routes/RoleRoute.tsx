import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../app/useAuth'

export default function RoleRoute({ allowed, children }: { allowed: string[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!allowed.some((role) => user.roles.includes(role))) return <section className="app-page"><p className="app-eyebrow">ACCESS RESTRICTED</p><h1>Not available for this account</h1><p>Your account role does not have access to this area.</p></section>
  return children
}
