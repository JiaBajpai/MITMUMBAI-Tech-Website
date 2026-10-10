import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { sessionsApi } from '../../api/sessions'
import { EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

export default function AttendancePage() {
  const load = useCallback((signal: AbortSignal) => sessionsApi.mine(signal), [])
  const { data, loading, error, reload } = useRemoteData('my-attendance', load)
  return <div className="app-page">
    <PageHeader eyebrow="LEARNING / MY RECORDS" title="My attendance" description="Attendance records returned for your account." />
    {loading && <LoadingState label="Loading attendance" />}{error && <ErrorState error={error} onRetry={reload} />}
    {data && (data.length ? <div className="app-table-wrap app-content-section"><table className="app-table"><thead><tr><th>Session</th><th>Status</th><th>Recorded</th><th>Marked by</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td><Link className="app-inline-link" to={`/app/sessions/${item.sessionId}`}>Session {item.sessionId}</Link></td><td><StatusBadge value={item.status} /></td><td>{new Date(item.createdAt).toLocaleString()}</td><td>{item.markedBy}</td></tr>)}</tbody></table></div> : <EmptyState title="No attendance records yet">When attendance is recorded for sessions, it will appear here.</EmptyState>)}
  </div>
}
