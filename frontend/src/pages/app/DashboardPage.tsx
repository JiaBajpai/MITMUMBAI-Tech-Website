import { useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getDashboard } from '../../api/dashboard'
import { useAuth } from '../../app/useAuth'
import { EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

export default function DashboardPage() {
  const { user } = useAuth()
  const load = useCallback((signal: AbortSignal) => getDashboard(signal), [])
  const { data, loading, error, reload } = useRemoteData('my-dashboard', load)

  return <div className="app-page">
    <PageHeader eyebrow="MEMBERS CORNER / OVERVIEW" title={`Welcome, ${data?.profile.name ?? user?.name ?? 'member'}.`} description="Your current learning and project activity from the Kernel workspace." />
    {loading && <LoadingState label="Loading your overview" />}
    {error && <ErrorState error={error} onRetry={reload} />}
    {data && <>
      <div className="app-stat-grid">
        <div className="app-stat"><span>Total XP</span><strong>{data.xp.totalXp.toLocaleString()}</strong></div>
        <div className="app-stat"><span>Tasks open</span><strong>{data.tasks.assignedOpen}</strong></div>
        <div className="app-stat"><span>Active projects</span><strong>{data.projects.activeMemberships}</strong></div>
        <div className="app-stat"><span>Leaderboard rank</span><strong>{data.leaderboardRank ? `#${data.leaderboardRank}` : '—'}</strong></div>
      </div>
      <div className="app-two-col">
        <section className="app-card">
          <h2>Your activity</h2>
          <ul className="app-list">
            <li><span>Tasks completed / verified</span><strong>{data.tasks.assignedCompleted} / {data.tasks.assignedVerified}</strong></li>
            <li><span>Attendance recorded</span><strong>{data.attendance.total} · {data.attendance.present} present</strong></li>
            <li><span>GitHub contributions</span><strong>{data.githubContributions.verified} verified · {data.githubContributions.unverified} pending</strong></li>
            <li><span>Project memberships</span><strong>{data.projects.activeMemberships} active · {data.projects.requestedMemberships} requested</strong></li>
          </ul>
        </section>
        <section className="app-card">
          <h2>XP by activity</h2>
          {Object.keys(data.xp.breakdown).length ? <ul className="app-list">{Object.entries(data.xp.breakdown).map(([label, amount]) => <li key={label}><span>{label.replaceAll('_', ' ')}</span><strong>{amount} XP</strong></li>)}</ul> : <EmptyState title="No XP activity yet">Verified contributions will be reflected here when recorded by the backend.</EmptyState>}
        </section>
      </div>
      <section className="app-content-section">
        <h2 className="app-section-heading">Recent project memberships</h2>
        {data.projects.recentMemberships.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Project</th><th>Domain</th><th>Program</th><th>Membership</th><th>Project status</th></tr></thead><tbody>{data.projects.recentMemberships.map((item) => <tr key={`${item.projectId}-${item.membershipRole}`}><td><Link className="app-inline-link" to={`/app/projects/${item.projectId}`}>{item.projectName || `Project ${item.projectId}`}</Link></td><td>{item.domainName || '—'}</td><td>{item.program || '—'}</td><td><StatusBadge value={item.membershipStatus || 'UNKNOWN'} /></td><td><StatusBadge value={item.projectStatus || 'UNKNOWN'} /></td></tr>)}</tbody></table></div> : <EmptyState title="No project memberships yet" action={<Link className="app-button secondary" to="/app/projects">Explore projects</Link>}>Project activity will appear here after you join or create a project.</EmptyState>}
      </section>
    </>}
  </div>
}
