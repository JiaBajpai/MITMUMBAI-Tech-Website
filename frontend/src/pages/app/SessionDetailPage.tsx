import { useCallback, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { sessionsApi } from '../../api/sessions'
import { tasksApi } from '../../api/tasks'
import { useAuth } from '../../app/useAuth'
import { AppButton, EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'
import type { Attendance } from '../../api/types'

function AttendanceManager({ sessionId }: { sessionId: number }) {
  const load = useCallback((signal: AbortSignal) => sessionsApi.attendance(sessionId, signal), [sessionId])
  const { data, loading, error, reload } = useRemoteData(`session-attendance-${sessionId}`, load)
  const [userId, setUserId] = useState('')
  const [status, setStatus] = useState<Attendance['status']>('PRESENT')
  const [message, setMessage] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  async function mark(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setMessage(null)
    try { await sessionsApi.markAttendance(sessionId, Number(userId), status); setMessage('Attendance recorded.'); setUserId(''); reload() }
    catch (cause) { setMessage(cause instanceof Error ? cause.message : 'Attendance could not be recorded.') }
    finally { setSaving(false) }
  }
  return <section className="app-detail-section"><h3>Session attendance</h3>{loading && <LoadingState label="Loading session attendance" />}{error && <ErrorState error={error} onRetry={reload} />}
    {data && (data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>User ID</th><th>Status</th><th>Marked</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td>{item.userId}</td><td><StatusBadge value={item.status} /></td><td>{new Date(item.createdAt).toLocaleString()}</td></tr>)}</tbody></table></div> : <EmptyState title="No attendance recorded">There are no attendance entries for this session yet.</EmptyState>)}
    <form className="app-inline-form" onSubmit={(event) => void mark(event)}><label className="app-field"><span>Member user ID</span><input type="number" min="1" required value={userId} onChange={(event) => setUserId(event.target.value)} /></label><label className="app-field"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value as Attendance['status'])}><option value="PRESENT">Present</option><option value="ABSENT">Absent</option><option value="LATE">Late</option><option value="EXCUSED">Excused</option></select></label><AppButton type="submit" disabled={saving}>{saving ? 'Saving…' : 'Record attendance'}</AppButton></form>
    {message && <p className="app-notice" role="status">{message}</p>}
  </section>
}

export default function SessionDetailPage() {
  const { id: rawId } = useParams()
  const id = Number(rawId)
  const { user } = useAuth()
  const load = useCallback((signal: AbortSignal) => sessionsApi.get(id, signal), [id])
  const tasksLoad = useCallback((signal: AbortSignal) => tasksApi.list(id, signal), [id])
  const session = useRemoteData(`session-${id}`, load)
  const tasks = useRemoteData(`session-tasks-${id}`, tasksLoad)
  const mayManageAttendance = Boolean(user && session.data && (user.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER'].includes(role)) || user.id === session.data.leadId || user.roles.includes('DOMAIN_LEAD')))
  return <div className="app-page">
    <PageHeader eyebrow="LEARNING / SESSION DETAIL" title={session.data?.topic || 'Session'} description="Session information and related tasks from the Kernel backend." actions={<Link className="app-button secondary" to="/app/sessions">All sessions</Link>} />
    {session.loading && <LoadingState label="Loading session" />}{session.error && <ErrorState error={session.error} onRetry={session.reload} />}
    {session.data && <div className="app-detail-grid app-content-section"><section className="app-card"><h2>Session details</h2><dl className="app-kv"><dt>Date</dt><dd>{session.data.date} {session.data.time}</dd><dt>Program</dt><dd>{session.data.program}</dd><dt>Type</dt><dd>{session.data.type}</dd><dt>Instructor</dt><dd>{session.data.instructor || '—'}</dd><dt>Domain ID</dt><dd>{session.data.domainId}</dd></dl><p>{session.data.description}</p></section><section className="app-card"><h2>Objectives</h2>{session.data.objectives.length ? <ul>{session.data.objectives.map((objective, index) => <li key={`${index}-${objective}`}>{objective}</li>)}</ul> : <p>No objectives are listed for this session.</p>}</section></div>}
    <section className="app-content-section"><h2 className="app-section-heading">Related tasks</h2>{tasks.loading && <LoadingState label="Loading session tasks" />}{tasks.error && <ErrorState error={tasks.error} onRetry={tasks.reload} />}{tasks.data && (tasks.data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Task</th><th>Assignee ID</th><th>Deadline</th><th>Status</th></tr></thead><tbody>{tasks.data.map((task) => <tr key={task.id}><td><Link className="app-inline-link" to={`/app/tasks/${task.id}`}>{task.title}</Link></td><td>{task.assigneeId ?? 'Unassigned'}</td><td>{task.deadline ? new Date(task.deadline).toLocaleString() : '—'}</td><td><StatusBadge value={task.status} /></td></tr>)}</tbody></table></div> : <EmptyState title="No tasks for this session">There are no task records associated with this session.</EmptyState>)}</section>
    {mayManageAttendance && session.data && <AttendanceManager sessionId={id} />}
  </div>
}
