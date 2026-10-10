import { useCallback, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { projectsApi } from '../../api/projects'
import { sessionsApi } from '../../api/sessions'
import { tasksApi } from '../../api/tasks'
import { useAuth } from '../../app/useAuth'
import { AppButton, EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

function TaskCreateForm({ onCreate, onCancel, saving }: { onCreate: (data: Parameters<typeof tasksApi.create>[0]) => Promise<void>; onCancel: () => void; saving: boolean }) {
  const [sessionId, setSessionId] = useState('')
  const [projectId, setProjectId] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [title, setTitle] = useState('')
  const [requirements, setRequirements] = useState('')
  const [deadline, setDeadline] = useState('')
  const [verificationRequired, setVerificationRequired] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const sessionsLoad = useCallback((signal: AbortSignal) => sessionsApi.list({}, signal), [])
  const projectsLoad = useCallback((signal: AbortSignal) => projectsApi.list({}, signal), [])
  const sessions = useRemoteData('sessions-for-task-form', sessionsLoad)
  const projects = useRemoteData('projects-for-task-form', projectsLoad)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null)
    const requirementsList = requirements.split('\n').map((item) => item.trim()).filter(Boolean)
    if (!sessionId || !title.trim() || !requirementsList.length) { setError('Choose a session and provide a title and at least one requirement.'); return }
    try { await onCreate({ sessionId: Number(sessionId), projectId: projectId ? Number(projectId) : undefined, assigneeId: assigneeId ? Number(assigneeId) : undefined, title: title.trim(), requirements: requirementsList, deadline: deadline ? new Date(deadline).toISOString() : undefined, verificationRequired }) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Task could not be created.') }
  }
  return <form className="app-form app-card" onSubmit={(event) => void submit(event)}>
    {sessions.loading && <LoadingState label="Loading sessions" />}{sessions.error && <ErrorState error={sessions.error} onRetry={sessions.reload} />}
    <label className="app-field"><span>Session *</span><select required value={sessionId} onChange={(event) => setSessionId(event.target.value)}><option value="">Choose session</option>{sessions.data?.map((session) => <option value={session.id} key={session.id}>{session.topic} · {session.program}</option>)}</select></label>
    <label className="app-field"><span>Related project</span><select value={projectId} onChange={(event) => setProjectId(event.target.value)}><option value="">No project</option>{projects.data?.map((project) => <option value={project.id} key={project.id}>{project.name}</option>)}</select></label>
    <label className="app-field span-2"><span>Task title *</span><input required value={title} onChange={(event) => setTitle(event.target.value)} /></label>
    <label className="app-field span-2"><span>Requirements · one per line *</span><textarea required value={requirements} onChange={(event) => setRequirements(event.target.value)} /></label>
    <label className="app-field"><span>Deadline</span><input type="datetime-local" value={deadline} onChange={(event) => setDeadline(event.target.value)} /></label>
    <label className="app-field"><span>Assignee user ID</span><input type="number" min="1" value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)} /></label>
    <label className="app-checkbox span-2"><input type="checkbox" checked={verificationRequired} onChange={(event) => setVerificationRequired(event.target.checked)} /> Require GitHub contribution verification</label>
    {error && <p className="app-notice error span-2" role="alert">{error}</p>}
    <div className="app-form-actions"><AppButton type="submit" disabled={saving}>{saving ? 'Creating…' : 'Create task'}</AppButton><AppButton type="button" variant="secondary" onClick={onCancel}>Cancel</AppButton></div>
  </form>
}

export default function TasksPage({ managementOnly = false }: { managementOnly?: boolean }) {
  const { user } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [sessionFilter, setSessionFilter] = useState('')
  const [notice, setNotice] = useState<string | null>(null)
  const load = useCallback((signal: AbortSignal) => tasksApi.list(sessionFilter ? Number(sessionFilter) : undefined, signal), [sessionFilter])
  const sessionsLoad = useCallback((signal: AbortSignal) => sessionsApi.list({}, signal), [])
  const tasks = useRemoteData(`tasks-${sessionFilter}`, load)
  const sessions = useRemoteData('sessions-for-tasks', sessionsLoad)
  const canCreate = Boolean(user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER', 'DOMAIN_LEAD'].includes(role)))
  const title = managementOnly ? 'Task management' : 'Tasks'
  async function create(request: Parameters<typeof tasksApi.create>[0]) {
    setSaving(true)
    try { await tasksApi.create(request); setCreateOpen(false); setNotice('Task created.'); tasks.reload() }
    finally { setSaving(false) }
  }
  return <div className="app-page">
    <PageHeader eyebrow="BUILD / TASKS" title={title} description={managementOnly ? 'Create and review tasks within the permissions assigned to your account.' : 'Tasks returned by the backend for your account. Completion evidence is submitted to the real task service.'} actions={canCreate ? <button className="app-button primary" onClick={() => setCreateOpen(!createOpen)}>{createOpen ? 'Close form' : 'Create task'}</button> : undefined} />
    {notice && <p className="app-notice success" role="status">{notice}</p>}
    {createOpen && canCreate && <section className="app-content-section"><h2 className="app-section-heading">New task</h2><TaskCreateForm onCreate={create} onCancel={() => setCreateOpen(false)} saving={saving} /></section>}
    <div className="app-toolbar"><label className="app-field app-session-filter"><span>Filter by session</span><select value={sessionFilter} onChange={(event) => setSessionFilter(event.target.value)}><option value="">All sessions</option>{sessions.data?.map((session) => <option value={session.id} key={session.id}>{session.topic}</option>)}</select></label></div>
    {tasks.loading && <LoadingState label="Loading tasks" />}{tasks.error && <ErrorState error={tasks.error} onRetry={tasks.reload} />}
    {tasks.data && (tasks.data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Task</th><th>Session</th><th>Assignee</th><th>Deadline</th><th>Verification</th><th>Status</th></tr></thead><tbody>{tasks.data.map((task) => <tr key={task.id}><td><Link className="app-inline-link" to={`/app/tasks/${task.id}`}>{task.title}</Link></td><td>{sessions.data?.find((session) => session.id === task.sessionId)?.topic || task.sessionId}</td><td>{task.assigneeId ?? 'Unassigned'}</td><td>{task.deadline ? new Date(task.deadline).toLocaleString() : '—'}</td><td>{task.verificationRequired ? 'GitHub required' : 'Standard'}</td><td><StatusBadge value={task.status} /></td></tr>)}</tbody></table></div> : <EmptyState title="No tasks found">The backend returned no tasks for this view.</EmptyState>)}
  </div>
}
