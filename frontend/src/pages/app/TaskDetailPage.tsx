import { useCallback, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { tasksApi } from '../../api/tasks'
import type { Task, TaskRequest } from '../../api/types'
import { useAuth } from '../../app/useAuth'
import { AppButton, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

function TaskEditForm({ task, saving, onCancel, onSave }: { task: Task; saving: boolean; onCancel: () => void; onSave: (request: Partial<TaskRequest>) => Promise<void> }) {
  const [title, setTitle] = useState(task.title)
  const [requirements, setRequirements] = useState(task.requirements.join('\n'))
  const [deadline, setDeadline] = useState(task.deadline ? new Date(task.deadline).toISOString().slice(0, 16) : '')
  const [assignee, setAssignee] = useState(task.assigneeId?.toString() || '')
  const [verificationRequired, setVerificationRequired] = useState(task.verificationRequired)
  const [error, setError] = useState<string | null>(null)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null)
    const requirementList = requirements.split('\n').map((value) => value.trim()).filter(Boolean)
    if (!title.trim() || !requirementList.length) { setError('A task title and at least one requirement are required.'); return }
    try { await onSave({ sessionId: task.sessionId, projectId: task.projectId ?? undefined, title: title.trim(), requirements: requirementList, deadline: deadline ? new Date(deadline).toISOString() : undefined, assigneeId: assignee ? Number(assignee) : undefined, verificationRequired }) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Task could not be updated.') }
  }
  return <form className="app-form app-card" onSubmit={(event) => void submit(event)}><label className="app-field span-2"><span>Title</span><input required value={title} onChange={(event) => setTitle(event.target.value)} /></label><label className="app-field span-2"><span>Requirements · one per line</span><textarea required value={requirements} onChange={(event) => setRequirements(event.target.value)} /></label><label className="app-field"><span>Deadline</span><input type="datetime-local" value={deadline} onChange={(event) => setDeadline(event.target.value)} /></label><label className="app-field"><span>Assignee user ID</span><input type="number" min="1" value={assignee} onChange={(event) => setAssignee(event.target.value)} /></label><label className="app-checkbox span-2"><input type="checkbox" checked={verificationRequired} onChange={(event) => setVerificationRequired(event.target.checked)} /> Require GitHub contribution verification</label>{error && <p className="app-notice error span-2" role="alert">{error}</p>}<div className="app-form-actions"><AppButton type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</AppButton><AppButton type="button" variant="secondary" onClick={onCancel}>Cancel</AppButton></div></form>
}

export default function TaskDetailPage() {
  const { id: rawId } = useParams()
  const id = Number(rawId)
  const { user } = useAuth()
  const load = useCallback((signal: AbortSignal) => tasksApi.get(id, signal), [id])
  const task = useRemoteData(`task-${id}`, load)
  const [repoUrl, setRepoUrl] = useState('')
  const [commitSha, setCommitSha] = useState('')
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [updating, setUpdating] = useState(false)
  const canComplete = Boolean(task.data && user && (task.data.assigneeId === user.id || user.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER'].includes(role))))
  const canVerify = Boolean(user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER', 'DOMAIN_LEAD'].includes(role)))
  const canEdit = Boolean(user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER', 'DOMAIN_LEAD'].includes(role)))

  async function complete(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(null); setNotice(null)
    if (task.data?.verificationRequired && !/^[0-9a-f]{40}$/i.test(commitSha.trim())) { setError('Enter the full 40-character commit SHA required for GitHub verification.'); setBusy(false); return }
    try { await tasksApi.complete(id, { repoUrl: repoUrl.trim(), commitSha: commitSha.trim() || undefined, notes: notes.trim() || undefined }); setNotice('Completion evidence submitted.'); task.reload() }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Completion evidence was not accepted.') }
    finally { setBusy(false) }
  }

  async function verify() {
    setBusy(true); setError(null); setNotice(null)
    try { await tasksApi.verify(id); setNotice('Task verification completed.'); task.reload() }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Task could not be verified.') }
    finally { setBusy(false) }
  }

  async function saveTask(request: Partial<TaskRequest>) {
    setUpdating(true)
    try { await tasksApi.update(id, request); setNotice('Task updated.'); setEditing(false); task.reload() }
    finally { setUpdating(false) }
  }

  return <div className="app-page">
    <PageHeader eyebrow="BUILD / TASK DETAIL" title={task.data?.title || 'Task'} description="Task requirements, status, and evidence actions." actions={<><Link className="app-button secondary" to="/app/tasks">All tasks</Link>{canEdit && task.data && <button className="app-button secondary" onClick={() => setEditing(!editing)}>{editing ? 'Close edit' : 'Edit task'}</button>}</>} />
    {task.loading && <LoadingState label="Loading task" />}{task.error && <ErrorState error={task.error} onRetry={task.reload} />}
    {notice && <p className="app-notice success" role="status">{notice}</p>}{error && <p className="app-notice error" role="alert">{error}</p>}
    {task.data && <>
      {editing && <section className="app-content-section"><h2 className="app-section-heading">Edit task</h2><TaskEditForm task={task.data} saving={updating} onCancel={() => setEditing(false)} onSave={saveTask} /></section>}
      <div className="app-task-summary app-content-section"><div><span>SESSION {task.data.sessionId}{task.data.projectId ? ` · PROJECT ${task.data.projectId}` : ''}</span><StatusBadge value={task.data.status} /></div><dl className="app-kv"><dt>Assignee</dt><dd>{task.data.assigneeId ?? 'Unassigned'}</dd><dt>Deadline</dt><dd>{task.data.deadline ? new Date(task.data.deadline).toLocaleString() : 'No deadline set'}</dd><dt>Verification</dt><dd>{task.data.verificationRequired ? 'GitHub contribution required' : 'Standard verification'}</dd></dl></div>
      <section className="app-detail-section app-content-section"><h2 className="app-section-heading">Requirements</h2>{task.data.requirements.length ? <ol className="app-requirements">{task.data.requirements.map((requirement, index) => <li key={`${index}-${requirement}`}>{requirement}</li>)}</ol> : <p>No requirements were returned for this task.</p>}</section>
      {canComplete && task.data.status === 'OPEN' && <section className="app-detail-section app-content-section"><h2 className="app-section-heading">Submit completion evidence</h2><form className="app-form evidence-form" onSubmit={(event) => void complete(event)}><label className="app-field span-2"><span>Repository URL *</span><input type="url" required value={repoUrl} onChange={(event) => setRepoUrl(event.target.value)} placeholder="https://github.com/owner/repository" /></label>{task.data.verificationRequired && <label className="app-field span-2"><span>Commit SHA · 40 characters *</span><input required minLength={40} maxLength={40} pattern="[0-9a-fA-F]{40}" value={commitSha} onChange={(event) => setCommitSha(event.target.value)} /></label>}<label className="app-field span-2"><span>Notes</span><textarea value={notes} onChange={(event) => setNotes(event.target.value)} /></label><div className="app-form-actions"><AppButton type="submit" disabled={busy}>{busy ? 'Submitting…' : 'Submit evidence'}</AppButton></div></form></section>}
      {canVerify && task.data.status === 'COMPLETED' && <section className="app-detail-section app-content-section"><h2 className="app-section-heading">Verify completion</h2><p>The backend checks your role, project alignment and any required GitHub contribution before verifying this task.</p><AppButton onClick={() => void verify()} disabled={busy}>{busy ? 'Verifying…' : 'Verify task'}</AppButton></section>}
      {!canComplete && task.data.status === 'OPEN' && <p className="app-notice app-content-section">This task can only be completed by its assigned member or a privileged project role.</p>}
    </>}
  </div>
}
