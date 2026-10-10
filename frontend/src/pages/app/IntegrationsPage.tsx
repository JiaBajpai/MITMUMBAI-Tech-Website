import { useCallback, useState } from 'react'
import { ExternalLink, GitBranch, Link2Off } from 'lucide-react'
import { githubApi } from '../../api/github'
import { projectsApi } from '../../api/projects'
import { AppButton, EmptyState, ErrorState, LoadingState, PageHeader } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

export default function IntegrationsPage() {
  const loadStatus = useCallback((signal: AbortSignal) => githubApi.status(signal), [])
  const status = useRemoteData('github-connection-status', loadStatus)
  const connected = status.data?.connected === true
  const loadRepositories = useCallback((signal: AbortSignal) => connected
    ? projectsApi.accessibleRepositories(signal)
    : Promise.resolve([]), [connected])
  const repositories = useRemoteData(`github-accessible-repositories-${connected}`, loadRepositories)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function connect() {
    const popup = window.open('about:blank', '_blank')
    if (!popup) { setError('Allow popups for this site, then try connecting again.'); return }
    setBusy(true); setMessage(null); setError(null)
    try {
      const result = await githubApi.connect()
      const authorizationUrl = new URL(result.authorizationUrl)
      if (authorizationUrl.protocol !== 'https:' || authorizationUrl.hostname !== 'github.com') throw new Error('The server returned an unexpected GitHub authorization URL.')
      popup.opener = null
      popup.location.replace(authorizationUrl.toString())
      setMessage('Complete authorization in the new tab, then refresh connection status here.')
    } catch (cause) { popup.close(); setError(cause instanceof Error ? cause.message : 'GitHub connection could not be started.') }
    finally { setBusy(false) }
  }

  async function disconnect() {
    if (!window.confirm('Disconnect the GitHub account linked to your profile?')) return
    setBusy(true); setError(null); setMessage(null)
    try { await githubApi.disconnect(); setMessage('GitHub was disconnected.'); status.reload(); repositories.reload() }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'GitHub could not be disconnected.') }
    finally { setBusy(false) }
  }

  return <div className="app-page">
    <PageHeader eyebrow="ACCOUNT / INTEGRATIONS" title="GitHub connection" description="Connect your GitHub account to make repositories available to eligible project workflows and contribution verification." />
    {message && <p className="app-notice success" role="status">{message}</p>}{error && <p className="app-notice error" role="alert">{error}</p>}
    {status.loading && <LoadingState label="Checking GitHub connection" />}{status.error && <ErrorState error={status.error} onRetry={status.reload} />}
    {status.data && <section className="app-card integration-card"><div className="integration-mark"><GitBranch size={25} /></div><div className="integration-copy"><h2>{status.data.connected ? 'Connected' : 'Not connected'}</h2><p>{status.data.connected ? `GitHub account: ${status.data.username || 'connected'}` : 'Connect the GitHub identity you use for project work.'}</p></div>{status.data.connected ? <AppButton variant="secondary" disabled={busy} onClick={() => void disconnect()}><Link2Off size={15} /> Disconnect</AppButton> : <AppButton disabled={busy} onClick={() => void connect()}>{busy ? 'Connecting…' : 'Connect GitHub'} <ExternalLink size={14} /></AppButton>}<AppButton variant="quiet" onClick={() => { status.reload(); repositories.reload() }}>Refresh status</AppButton></section>}
    <section className="app-content-section"><h2 className="app-section-heading">Accessible repositories</h2>{status.data && !connected && <EmptyState title="Connect GitHub to browse repositories">Your accessible repositories will appear here after you connect your GitHub account.</EmptyState>}{connected && repositories.loading && <LoadingState label="Loading accessible repositories" />}{connected && repositories.error && <ErrorState error={repositories.error} onRetry={repositories.reload} />}{connected && !repositories.loading && !repositories.error && repositories.data && (repositories.data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Repository</th><th>Visibility</th><th>Project</th><th>Linked</th></tr></thead><tbody>{repositories.data.map((repo) => <tr key={`${repo.githubRepoId}-${repo.fullName}`}><td><a className="app-inline-link" href={repo.htmlUrl} target="_blank" rel="noreferrer">{repo.fullName} ↗</a></td><td>{repo.visibility}</td><td>{repo.projectId ?? 'Not linked'}</td><td>{repo.linkedAt ? new Date(repo.linkedAt).toLocaleDateString() : '—'}</td></tr>)}</tbody></table></div> : <EmptyState title="No repositories available">No repositories are available to this GitHub account. Check its repository access and organization policies.</EmptyState>)}</section>
  </div>
}
