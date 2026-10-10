import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { domainsApi } from '../../api/domains'
import { sessionsApi, type SessionFilters } from '../../api/sessions'
import { EmptyState, ErrorState, LoadingState, PageHeader } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.valueOf()) ? value : date.toLocaleDateString(undefined, { dateStyle: 'medium' })
}

export default function SessionsPage() {
  const [filters, setFilters] = useState<SessionFilters>({})
  const key = useMemo(() => JSON.stringify(filters), [filters])
  const load = useCallback((signal: AbortSignal) => sessionsApi.list(filters, signal), [filters])
  const domainsLoad = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const sessions = useRemoteData(`sessions-${key}`, load)
  const domains = useRemoteData('domains-for-sessions', domainsLoad)
  const change = (field: keyof SessionFilters, value: string) => setFilters((current) => ({ ...current, [field]: value ? field === 'domainId' ? Number(value) : value : undefined }))
  return <div className="app-page">
    <PageHeader eyebrow="LEARNING / SESSIONS" title="Sessions" description="Browse sessions returned for your account. Use the filters to narrow the list." />
    <div className="app-toolbar app-filter-grid">
      <label className="app-field"><span>Domain</span><select value={filters.domainId || ''} onChange={(event) => change('domainId', event.target.value)}><option value="">All domains</option>{domains.data?.map((domain) => <option value={domain.id} key={domain.id}>{domain.name}</option>)}</select></label>
      <label className="app-field"><span>Program</span><select value={filters.program || ''} onChange={(event) => change('program', event.target.value)}><option value="">All programs</option><option value="TECHNICAL">Technical</option><option value="FOUNDATION">Foundation</option></select></label>
      <label className="app-field"><span>Type</span><input value={filters.type || ''} onChange={(event) => change('type', event.target.value)} placeholder="Filter by type" /></label>
      <label className="app-field"><span>Date</span><input type="date" value={filters.date || ''} onChange={(event) => change('date', event.target.value)} /></label>
    </div>
    {sessions.loading && <LoadingState label="Loading sessions" />}{sessions.error && <ErrorState error={sessions.error} onRetry={sessions.reload} />}
    {sessions.data && (sessions.data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Session</th><th>Domain</th><th>Date & time</th><th>Program</th><th>Type</th><th>Instructor</th></tr></thead><tbody>{sessions.data.map((session) => <tr key={session.id}><td><Link className="app-inline-link" to={`/app/sessions/${session.id}`}>{session.topic}</Link><div className="app-table-subtext">{session.description}</div></td><td>{domains.data?.find((item) => item.id === session.domainId)?.name || session.domainId}</td><td>{formatDate(session.date)}{session.time ? ` · ${session.time}` : ''}</td><td>{session.program}</td><td>{session.type}</td><td>{session.instructor || '—'}</td></tr>)}</tbody></table></div> : <EmptyState title="No sessions found">There are no sessions matching the selected filters. Change or clear a filter to try again.</EmptyState>)}
  </div>
}
