import { useCallback, useState } from 'react'
import { domainsApi } from '../../api/domains'
import { gamificationApi } from '../../api/gamification'
import { PageHeader, EmptyState, ErrorState, LoadingState } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

export default function LeaderboardPage() {
  const [program, setProgram] = useState('TECHNICAL')
  const [domainId, setDomainId] = useState('')
  const [page, setPage] = useState(0)
  const domainLoad = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const domainData = useRemoteData('domains-for-leaderboard', domainLoad)
  const load = useCallback((signal: AbortSignal) => domainId ? gamificationApi.domainLeaderboard(Number(domainId), page, 10, signal) : gamificationApi.leaderboard(program, page, 10, signal), [domainId, page, program])
  const leaderboard = useRemoteData(`leaderboard-${domainId}-${program}-${page}`, load)
  return <div className="app-page">
    <PageHeader eyebrow="PROGRESS / LEADERBOARD" title="Leaderboard" description="Scores and rankings returned by the Kernel leaderboard service." />
    <div className="app-toolbar app-filter-grid"><label className="app-field"><span>Program</span><select value={program} disabled={Boolean(domainId)} onChange={(event) => { setProgram(event.target.value); setPage(0) }}><option value="TECHNICAL">Technical</option><option value="FOUNDATION">Foundation</option></select></label><label className="app-field"><span>Domain</span><select value={domainId} onChange={(event) => { setDomainId(event.target.value); setPage(0) }}><option value="">Overall</option>{domainData.data?.map((domain) => <option value={domain.id} key={domain.id}>{domain.name}</option>)}</select></label></div>
    {leaderboard.loading && <LoadingState label="Loading leaderboard" />}{leaderboard.error && <ErrorState error={leaderboard.error} onRetry={leaderboard.reload} />}
    {leaderboard.data && (leaderboard.data.entries.length ? <><div className="app-table-wrap"><table className="app-table"><thead><tr><th>Rank</th><th>Member</th><th>Program</th><th>Total XP</th></tr></thead><tbody>{leaderboard.data.entries.map((entry) => <tr key={`${entry.userId}-${entry.rank}`}><td><strong>#{entry.rank}</strong></td><td>{entry.name}</td><td>{entry.program}</td><td>{entry.totalXp.toLocaleString()}</td></tr>)}</tbody></table></div><div className="app-pagination"><span>{leaderboard.data.totalElements.toLocaleString()} members · page {leaderboard.data.totalPages ? page + 1 : 0} of {leaderboard.data.totalPages}</span><span><button className="app-button secondary" disabled={page <= 0} onClick={() => setPage((current) => current - 1)}>Previous</button><button className="app-button secondary" disabled={page + 1 >= leaderboard.data!.totalPages} onClick={() => setPage((current) => current + 1)}>Next</button></span></div></> : <EmptyState title="No leaderboard entries">No ranking data is available for this selection.</EmptyState>)}
  </div>
}
