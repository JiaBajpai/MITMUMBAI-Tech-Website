import { useCallback, useMemo, useState, type FormEvent } from 'react'
import { accountsApi } from '../../api/accounts'
import { domainsApi } from '../../api/domains'
import type { AdminAccount, CreateAccountRequest, Domain } from '../../api/types'
import { useAuth } from '../../app/useAuth'
import { AppButton, EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

const allRoles = ['SUPER_ADMIN', 'CORE_MEMBER', 'FACULTY', 'DOMAIN_LEAD', 'STUDENT']

export default function AccountsPage() {
  const { user } = useAuth()
  const isSuperAdmin = Boolean(user?.roles.includes('SUPER_ADMIN'))
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<{ text: string; error?: boolean } | null>(null)
  const loadAccounts = useCallback((signal: AbortSignal) => accountsApi.list(search.trim(), signal), [search])
  const loadDomains = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const accounts = useRemoteData(`admin-accounts-${search}`, loadAccounts)
  const domains = useRemoteData('admin-accounts-domains', loadDomains)

  async function runAction(action: () => Promise<unknown>, message: string) {
    setBusy(true); setNotice(null)
    try { await action(); setNotice({ text: message }); accounts.reload() }
    catch (cause) { setNotice({ text: cause instanceof Error ? cause.message : 'The account action could not be completed.', error: true }) }
    finally { setBusy(false) }
  }

  async function create(request: CreateAccountRequest) {
    await runAction(() => accountsApi.create(request), 'Account created. A password setup link was sent to the verified account email.')
    setCreateOpen(false)
  }

  return <div className="app-page">
    <PageHeader eyebrow="ADMINISTRATION / ACCOUNTS" title="Account management" description={isSuperAdmin ? 'Manage member accounts, roles, domain-lead assignments, and password setup.' : 'Manage student accounts only. Privileged roles and domain-lead assignments are restricted to Super Admins.'} actions={<AppButton onClick={() => setCreateOpen((open) => !open)}>{createOpen ? 'Close form' : 'Create account'}</AppButton>} />
    {notice && <p className={`app-notice ${notice.error ? 'error' : 'success'}`} role={notice.error ? 'alert' : 'status'}>{notice.text}</p>}
    {!domains.data && domains.loading && <LoadingState label="Loading domains" />}
    {domains.error && <ErrorState error={domains.error} onRetry={domains.reload} />}
    {createOpen && domains.data && <CreateAccountForm isSuperAdmin={isSuperAdmin} domains={domains.data} busy={busy} onCancel={() => setCreateOpen(false)} onCreate={create} />}
    <section className="app-content-section">
      <div className="app-toolbar"><label className="app-field app-search-field"><span>Search accounts</span><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Name, email, or system ID" /></label></div>
      {accounts.loading && <LoadingState label="Loading accounts" />}
      {accounts.error && <ErrorState error={accounts.error} onRetry={accounts.reload} />}
      {accounts.data && (accounts.data.length ? <div className="app-grid-2">{accounts.data.map((account) => <AccountCard key={account.id} account={account} domains={domains.data || []} isSuperAdmin={isSuperAdmin} busy={busy} onAction={runAction} />)}</div> : <EmptyState title="No accounts found">No accounts match this search.</EmptyState>)}
    </section>
  </div>
}

function CreateAccountForm({ isSuperAdmin, domains, busy, onCancel, onCreate }: { isSuperAdmin: boolean; domains: Domain[]; busy: boolean; onCancel: () => void; onCreate: (request: CreateAccountRequest) => Promise<void> }) {
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [program, setProgram] = useState('TECHNICAL')
  const [roles, setRoles] = useState<string[]>(['STUDENT'])
  const [domainIds, setDomainIds] = useState<number[]>([])
  const [error, setError] = useState<string | null>(null)
  const requiresDomains = roles.includes('DOMAIN_LEAD')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null)
    if (requiresDomains && !domainIds.length) { setError('Choose at least one domain for a Domain Lead.'); return }
    try { await onCreate({ email: email.trim(), name: name.trim(), program, roles: isSuperAdmin ? roles : ['STUDENT'], domainIds: requiresDomains ? domainIds : [] }) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'The account could not be created.') }
  }

  return <section className="app-card app-content-section">
    <h2>Create account</h2><p className="app-muted-copy">The system assigns a unique account ID. The account receives a single-use password setup link by email; no temporary password is shown here.</p>
    <form className="app-form" onSubmit={(event) => void submit(event)}>
      <div className="app-field"><label htmlFor="account-name">Full name</label><input id="account-name" required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} /></div>
      <div className="app-field"><label htmlFor="account-email">Email address</label><input id="account-email" type="email" required maxLength={255} value={email} onChange={(event) => setEmail(event.target.value)} /></div>
      <div className="app-field"><label htmlFor="account-program">Program</label><select id="account-program" value={program} onChange={(event) => setProgram(event.target.value)}><option value="TECHNICAL">Technical</option><option value="FOUNDATION">Foundation</option></select></div>
      <div className="app-field"><label>Roles</label>{isSuperAdmin ? <div className="account-role-options">{allRoles.map((role) => <label key={role}><input type="checkbox" checked={roles.includes(role)} onChange={(event) => setRoles((current) => event.target.checked ? [...current, role] : current.filter((item) => item !== role))} />{role.replaceAll('_', ' ')}</label>)}</div> : <p className="app-muted-copy">STUDENT (Core Members can create student accounts only)</p>}</div>
      {requiresDomains && <div className="app-field span-2"><label htmlFor="account-domains">Domain Lead domains</label><select id="account-domains" multiple value={domainIds.map(String)} onChange={(event) => setDomainIds(Array.from(event.target.selectedOptions, (option) => Number(option.value)))}>{domains.map((domain) => <option key={domain.id} value={domain.id}>{domain.name}</option>)}</select><small>Use Ctrl/Command to select multiple domains.</small></div>}
      {error && <p className="app-notice error span-2" role="alert">{error}</p>}
      <div className="app-form-actions"><AppButton type="submit" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</AppButton><AppButton type="button" variant="secondary" onClick={onCancel}>Cancel</AppButton></div>
    </form>
  </section>
}

function AccountCard({ account, domains, isSuperAdmin, busy, onAction }: { account: AdminAccount; domains: Domain[]; isSuperAdmin: boolean; busy: boolean; onAction: (action: () => Promise<unknown>, message: string) => Promise<void> }) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(account.name)
  const [email, setEmail] = useState(account.email)
  const [program, setProgram] = useState(account.program)
  const [roles, setRoles] = useState(account.roles)
  const [domainId, setDomainId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const domainNames = useMemo(() => account.domainIds.map((id) => domains.find((domain) => domain.id === id)?.name || `Domain ${id}`), [account.domainIds, domains])
  const isStudentOnly = account.roles.length === 1 && account.roles[0] === 'STUDENT'

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null)
    try {
      await onAction(() => accountsApi.update(account.id, { name: name.trim(), email: email.trim(), program }), 'Account details updated.')
      setEditing(false)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Account details could not be saved.') }
  }

  async function saveRoles() {
    setError(null)
    try { await onAction(() => accountsApi.setRoles(account.id, roles), 'Account roles updated.') }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Account roles could not be updated.') }
  }

  return <article className="app-card account-card">
    <div className="account-card-heading"><div><p className="app-eyebrow">SYSTEM ACCOUNT ID {account.id}</p><h2>{account.name}</h2></div><StatusBadge value={account.active ? 'ACTIVE' : 'INACTIVE'} /></div>
    <p className="account-email">{account.email}</p>
    <dl className="app-kv"><dt>Program</dt><dd>{account.program}</dd><dt>Roles</dt><dd>{account.roles.map((role) => role.replaceAll('_', ' ')).join(', ')}</dd>{account.roles.includes('DOMAIN_LEAD') && <><dt>Domains</dt><dd>{domainNames.join(', ') || 'No active domains'}</dd></>}{account.passwordSetupRequired && <><dt>Password setup</dt><dd>Pending</dd></>}</dl>
    {editing && <form className="app-form account-edit-form" onSubmit={(event) => void save(event)}>
      <div className="app-field"><label htmlFor={`edit-name-${account.id}`}>Name</label><input id={`edit-name-${account.id}`} required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} /></div>
      <div className="app-field"><label htmlFor={`edit-email-${account.id}`}>Email</label><input id={`edit-email-${account.id}`} type="email" required maxLength={255} value={email} onChange={(event) => setEmail(event.target.value)} /></div>
      <div className="app-field"><label htmlFor={`edit-program-${account.id}`}>Program</label><select id={`edit-program-${account.id}`} value={program} onChange={(event) => setProgram(event.target.value)}><option value="TECHNICAL">Technical</option><option value="FOUNDATION">Foundation</option></select></div>
      <div className="app-form-actions"><AppButton type="submit" disabled={busy}>Save details</AppButton><AppButton type="button" variant="secondary" onClick={() => setEditing(false)}>Cancel</AppButton></div>
    </form>}
    {isSuperAdmin && <div className="account-role-options account-role-edit">{allRoles.map((role) => <label key={role}><input type="checkbox" checked={roles.includes(role)} onChange={(event) => setRoles((current) => event.target.checked ? [...current, role] : current.filter((item) => item !== role))} />{role.replaceAll('_', ' ')}</label>)}<AppButton variant="secondary" disabled={busy || roles.join() === account.roles.join()} onClick={() => void saveRoles()}>Save roles</AppButton></div>}
    {isSuperAdmin && <div className="app-inline-form"><label htmlFor={`domain-${account.id}`}>Domain lead assignment</label><select id={`domain-${account.id}`} value={domainId} onChange={(event) => setDomainId(event.target.value)}><option value="">Choose domain</option>{domains.map((domain) => <option key={domain.id} value={domain.id}>{domain.name}</option>)}</select><AppButton variant="secondary" disabled={busy || !domainId} onClick={() => void onAction(() => accountsApi.assignDomain(account.id, Number(domainId)), 'Domain lead assignment saved.')}>Assign</AppButton>{account.domainIds.map((id) => <AppButton key={id} variant="secondary" disabled={busy} onClick={() => { const label = domains.find((domain) => domain.id === id)?.name || `domain ${id}`; if (window.confirm(`Remove ${account.name} from ${label} lead duties?`)) void onAction(() => accountsApi.removeDomain(account.id, id), 'Domain lead assignment removed.') }}>Remove {domains.find((domain) => domain.id === id)?.name || id}</AppButton>)}</div>}
    {error && <p className="app-notice error" role="alert">{error}</p>}
    <div className="app-form-actions account-actions">
      <AppButton variant="secondary" onClick={() => setEditing((value) => !value)}>{editing ? 'Close editor' : 'Edit details'}</AppButton>
      {(isSuperAdmin || isStudentOnly) && <AppButton variant="secondary" disabled={busy} onClick={() => { if (window.confirm(`Send a password ${account.passwordSetupRequired ? 'setup' : 'reset'} link to ${account.email}?`)) void onAction(() => accountsApi.issuePasswordReset(account.id), 'A password link was sent to the account email.') }}>Email password link</AppButton>}
      {(isSuperAdmin || isStudentOnly) && <AppButton variant="secondary" disabled={busy} onClick={() => { const action = account.active ? 'deactivate' : 'activate'; if (window.confirm(`${action[0].toUpperCase()}${action.slice(1)} ${account.name}'s account?`)) void onAction(() => accountsApi.update(account.id, { active: !account.active }), `Account ${action}d.`) }}>{account.active ? 'Deactivate' : 'Activate'}</AppButton>}
    </div>
  </article>
}
