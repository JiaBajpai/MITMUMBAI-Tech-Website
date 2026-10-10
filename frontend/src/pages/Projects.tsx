import { Navigate } from 'react-router-dom'

// Legacy page retained for compatibility with older imports. Public projects
// are intentionally an honest empty showcase until real public records exist.
export default function Projects() {
  return <Navigate to="/projects" replace />
}
