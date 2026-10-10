import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { domainsApi } from '../../api/domains'
import { projectsApi, type ProjectFilters } from '../../api/projects'
import { useAuth } from '../../app/useAuth'
import ProjectForm from '../../components/app/ProjectForm'
import { EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

const statuses = ['PROPOSED', 'UNDER_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED', 'REJECTED']

export default function ProjectsPage() {
  const { user } = useAuth()
  const [filters, setFilters] = useState<ProjectFilters>({})
  const [showCreate, setShowCreate] = useState(false)
  const [createBusy, setCreateBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const filterKey = useMemo(() => JSON.stringify(filters), [filters])
  const load = useCallback((signal: AbortSignal) => projectsApi.list(filters, signal), [filters])
  const domainLoad = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const projects = useRemoteData(`projects-${filterKey}`, load)
  const domains = useRemoteData('domains-for-projects', domainLoad)
  const canReview = Boolean(user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER', 'DOMAIN_LEAD'].includes(role)))
  const change = (field: keyof ProjectFilters, value: string) => setFilters((current) => ({ ...current, [field]: value ? field === 'domainId' ? Number(value) : value : undefined }))

  async function create(request: Parameters<typeof projectsApi.create>[0]) {
    setCreateBusy(true)
    try { await projectsApi.create(request); setShowCreate(false); setNotice('Project created and submitted in proposed status.'); projects.reload() }
    finally { setCreateBusy(false) }
  }

  return <div className="app-page">
    <PageHeader eyebrow="BUILD / PROJECTS" title="Projects" description="View project records and request membership. Project review and management actions follow your backend permissions." actions={<button className="app-button primary" type="button" onClick={() => { setNotice(null); setShowCreate(!showCreate) }}>{showCreate ? 'Close form' : 'Create project'}</button>} />
    {notice && <p className="app-notice success" role="status">{notice}</p>}
    {showCreate && domains.data && <section className="app-content-section"><h2 className="app-section-heading">Create a project</h2><ProjectForm domains={domains.data} defaultProgram={user?.program || 'TECHNICAL'} saving={createBusy} onCancel={() => setShowCreate(false)} onSubmit={create} /></section>}
    <div className="app-toolbar app-filter-grid">
      <label className="app-field"><span>Search projects</span><input value={filters.q || ''} onChange={(event) => change('q', event.target.value)} placeholder="Name or description" /></label>
      <label className="app-field"><span>Domain</span><select value={filters.domainId || ''} onChange={(event) => change('domainId', event.target.value)}><option value="">All domains</option>{domains.data?.map((domain) => <option key={domain.id} value={domain.id}>{domain.name}</option>)}</select></label>
      <label className="app-field"><span>Status</span><select value={filters.status || ''} onChange={(event) => change('status', event.target.value)}><option value="">All statuses</option>{statuses.map((status) => <option key={status}>{status}</option>)}</select></label>
      <label className="app-field"><span>Program</span><select value={filters.program || ''} onChange={(event) => change('program', event.target.value)}><option value="">All programs</option><option value="TECHNICAL">Technical</option><option value="FOUNDATION">Foundation</option></select></label>
    </div>
    {projects.loading && <LoadingState label="Loading projects" />}{projects.error && <ErrorState error={projects.error} onRetry={projects.reload} />}
    {projects.data && (projects.data.length ? <div className="app-project-list">{projects.data.map((project) => {
      const membership = project.members.find((item) => item.userId === user?.id)
      const memberCount = project.members.filter((item) => item.status === 'ACTIVE').length
      const isManager = project.createdBy === user?.id || canReview
      return <article className="app-project-row" key={project.id}>
        <div className="app-project-main"><div className="app-project-meta"><span>PROJECT {String(project.id).padStart(3, '0')}</span><StatusBadge value={project.status} /><span>{project.program}</span></div><h2><Link to={`/app/projects/${project.id}`}>{project.name}</Link></h2><p>{project.problem}</p><div className="app-project-tags">{project.technologies.slice(0, 4).map((tag) => <span key={tag}>{tag}</span>)}</div></div>
        <div className="app-project-side"><span>{memberCount} active members · {project.expectedMembers} expected</span>{membership ? <StatusBadge value={membership.status} /> : <button className="app-button secondary" type="button" onClick={async () => { setNotice(null); try { await projectsApi.join(project.id); setNotice('Membership request submitted.'); projects.reload() } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not request membership.') } }}>Request to join</button>}{canReview && <small>{isManager ? 'Project controls in details' : 'Review available where authorized'}</small>}</div>
      </article>
    })}</div> : <EmptyState title="No projects found">The backend returned no projects for your account or selected filters.</EmptyState>)}
  </div>
}
