import { useCallback, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { domainsApi } from '../../api/domains'
import { projectsApi } from '../../api/projects'
import { useAuth } from '../../app/useAuth'
import ProjectForm from '../../components/app/ProjectForm'
import { AppButton, EmptyState, ErrorState, LoadingState, PageHeader, StatusBadge } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

export default function ProjectDetailPage() {
  const { id: idParam } = useParams()
  const id = Number(idParam)
  const { user } = useAuth()
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [reviewComment, setReviewComment] = useState('')
  const [repoId, setRepoId] = useState('')
  const loadProject = useCallback((signal: AbortSignal) => projectsApi.get(id, signal), [id])
  const loadMembers = useCallback((signal: AbortSignal) => projectsApi.members(id, signal), [id])
  const loadDomains = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const loadRepo = useCallback((signal: AbortSignal) => projectsApi.linkedRepository(id, signal), [id])
  const loadContributions = useCallback((signal: AbortSignal) => projectsApi.contributions(id, signal), [id])
  const project = useRemoteData(`project-${id}`, loadProject)
  const members = useRemoteData(`project-members-${id}`, loadMembers)
  const domains = useRemoteData('domains-for-project-detail', loadDomains)
  const repository = useRemoteData(`project-repository-${id}`, loadRepo)
  const contributions = useRemoteData(`project-contributions-${id}`, loadContributions)
  const projectData = project.data
  const membership = useMemo(() => projectData?.members.find((item) => item.userId === user?.id), [projectData, user?.id])
  const reviewerRole = Boolean(user?.roles.some((role) => ['SUPER_ADMIN', 'CORE_MEMBER', 'DOMAIN_LEAD'].includes(role)))
  const canManage = Boolean(projectData && (projectData.createdBy === user?.id || reviewerRole))
  const canReview = reviewerRole
  const hasRepo = Boolean(repository.data)
  const repoMissing = repository.error instanceof ApiError && repository.error.status === 404

  async function action(work: () => Promise<unknown>, message: string) {
    setNotice(null)
    try { await work(); setNotice(message); project.reload(); members.reload(); repository.reload(); contributions.reload() }
    catch (error) { setNotice(error instanceof Error ? error.message : 'The action could not be completed.') }
  }
  async function saveProject(request: Parameters<typeof projectsApi.update>[1]) {
    setSaving(true)
    try { await projectsApi.update(id, request); setEditing(false); setNotice('Project updated.'); project.reload() }
    finally { setSaving(false) }
  }
  async function linkRepo() {
    if (!repoId) return
    await action(() => projectsApi.linkRepository(id, Number(repoId)), 'GitHub repository linked.')
  }

  return <div className="app-page">
    <PageHeader eyebrow="BUILD / PROJECT DETAIL" title={projectData?.name || 'Project'} description="Project details and membership actions available from the backend." actions={<Link className="app-button secondary" to="/app/projects">All projects</Link>} />
    {notice && <p className="app-notice" role="status">{notice}</p>}
    {project.loading && <LoadingState label="Loading project" />}{project.error && <ErrorState error={project.error} onRetry={project.reload} />}
    {projectData && <>
      {editing && domains.data ? <section className="app-content-section"><h2 className="app-section-heading">Edit project</h2><ProjectForm project={projectData} domains={domains.data} defaultProgram={projectData.program} saving={saving} onCancel={() => setEditing(false)} onSubmit={saveProject} /></section> : <section className="app-detail-heading app-content-section"><div className="app-project-meta"><span>PROJECT {projectData.id}</span><StatusBadge value={projectData.status} /><span>{projectData.program}</span></div><h2>{projectData.name}</h2><div className="app-project-tags">{projectData.technologies.map((tag) => <span className="app-tag" key={tag}>{tag}</span>)}</div><p><strong>Problem:</strong> {projectData.problem}</p><p><strong>Solution:</strong> {projectData.solution}</p>{projectData.outcome && <p><strong>Outcome:</strong> {projectData.outcome}</p>}<p><strong>Required skills:</strong> {projectData.requiredSkills.length ? projectData.requiredSkills.join(', ') : 'Not specified'}</p><p><strong>Expected team size:</strong> {projectData.expectedMembers}</p>{projectData.reviewComment && <p><strong>Review note:</strong> {projectData.reviewComment}</p>}
        <div className="app-project-actions">{canManage && <AppButton variant="secondary" onClick={() => setEditing(true)}>Edit project</AppButton>}{!membership && <AppButton disabled={saving} onClick={() => void action(() => projectsApi.join(id), 'Membership request submitted.')}>Request to join</AppButton>}{membership && membership.role !== 'OWNER' && projectData.createdBy !== user?.id && <AppButton variant="secondary" onClick={() => { if (window.confirm('Leave this project?')) void action(() => projectsApi.leave(id), 'You left the project.') }}>Leave project</AppButton>}</div>
      </section>}
      <div className="app-grid-2 app-content-section">
        <section className="app-detail-section"><h3>Project members</h3>{members.loading && <LoadingState label="Loading members" />}{members.error && <ErrorState error={members.error} onRetry={members.reload} />}{members.data && (members.data.length ? members.data.map((member) => <div className="app-member-line" key={member.id}><span>User {member.userId} · {member.role}</span><StatusBadge value={member.status} />{canManage && member.status === 'REQUESTED' && <span className="app-table-actions"><button className="app-button secondary" onClick={() => void action(() => projectsApi.approve(id, member.userId), 'Membership approved.')}>Approve</button><button className="app-button secondary" onClick={() => void action(() => projectsApi.reject(id, member.userId), 'Membership rejected.')}>Reject</button></span>}{canManage && member.status === 'ACTIVE' && member.role !== 'OWNER' && member.userId !== user?.id && <button className="app-button app-danger-button" onClick={() => { if (window.confirm(`Remove member ${member.userId} from this project?`)) void action(() => projectsApi.removeMember(id, member.userId), 'Member removed.') }}>Remove</button>}</div>) : <EmptyState title="No members">Member records will appear here when present.</EmptyState>)}</section>
        <section className="app-detail-section"><h3>GitHub repository</h3>{repository.loading && <LoadingState label="Loading linked repository" />}{repository.error && !repoMissing && <ErrorState error={repository.error} onRetry={repository.reload} />}{repository.data && <><p><a className="app-inline-link" href={repository.data.htmlUrl} target="_blank" rel="noreferrer">{repository.data.fullName} ↗</a></p><p>{repository.data.visibility}{repository.data.linkedAt ? ` · linked ${new Date(repository.data.linkedAt).toLocaleDateString()}` : ''}</p>{canManage && <AppButton variant="secondary" onClick={() => { if (window.confirm('Unlink this repository?')) void action(() => projectsApi.unlinkRepository(id), 'GitHub repository unlinked.') }}>Unlink repository</AppButton>}</>}{repoMissing && <><p>No repository is linked to this project.</p>{canManage && <RepositoryLinker repoId={repoId} setRepoId={setRepoId} onLink={() => void linkRepo()} />}</>}</section>
      </div>
      {hasRepo && <section className="app-detail-section app-content-section"><div className="app-section-title-row"><h3>GitHub contributions</h3><button className="app-button secondary" onClick={contributions.reload}>Refresh contributions</button></div>{contributions.loading && <LoadingState label="Checking GitHub contributions" />}{contributions.error && <ErrorState error={contributions.error} onRetry={contributions.reload} />}{contributions.data && (contributions.data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Commit</th><th>Author</th><th>Date</th><th>Kernel account</th><th>Verification</th><th /></tr></thead><tbody>{contributions.data.map((item) => <tr key={item.id}><td><a className="app-inline-link" href={item.url} target="_blank" rel="noreferrer">{item.message || item.sha.slice(0, 8)}</a></td><td>{item.authorLogin}</td><td>{new Date(item.committedAt).toLocaleDateString()}</td><td>{item.kernelUserId ?? 'Unlinked'}</td><td><StatusBadge value={item.verified ? 'VERIFIED' : 'UNVERIFIED'} /></td><td>{canReview && !item.verified && <button className="app-button secondary" onClick={() => void action(() => projectsApi.verifyContribution(id, item.id), 'Contribution verified.')}>Verify</button>}</td></tr>)}</tbody></table></div> : <EmptyState title="No contributions returned">The connected repository has no persisted contributions to display.</EmptyState>)}</section>}
      {canReview && <section className="app-detail-section app-content-section"><h3>Project review</h3><p>Review actions are checked by the backend against your role and domain assignment.</p><label className="app-field"><span>Review comment</span><textarea value={reviewComment} onChange={(event) => setReviewComment(event.target.value)} /></label><div className="app-project-actions app-review-actions">{['UNDER_REVIEW', 'APPROVE', 'CHANGES_REQUESTED', 'REJECT'].map((decision) => <button key={decision} className="app-button secondary" disabled={saving} onClick={() => void action(() => projectsApi.review(id, decision, reviewComment), `Review saved: ${decision.replaceAll('_', ' ').toLowerCase()}.`)}>{decision.replaceAll('_', ' ')}</button>)}</div></section>}
    </>}
  </div>
}

function RepositoryLinker({ repoId, setRepoId, onLink }: { repoId: string; setRepoId: (value: string) => void; onLink: () => void }) {
  const load = useCallback((signal: AbortSignal) => projectsApi.accessibleRepositories(signal), [])
  const { data, loading, error, reload } = useRemoteData('accessible-github-repositories', load)
  return <div className="repo-linker">{loading && <LoadingState label="Loading accessible repositories" />}{error && <ErrorState error={error} onRetry={reload} />}{data && (data.length ? <><label className="app-field"><span>Choose a repository</span><select value={repoId} onChange={(event) => setRepoId(event.target.value)}><option value="">Select repository</option>{data.map((repo) => <option key={repo.githubRepoId} value={repo.githubRepoId}>{repo.fullName} · {repo.visibility}</option>)}</select></label><button className="app-button primary" disabled={!repoId} onClick={onLink}>Link repository</button></> : <EmptyState title="No accessible repositories">Connect GitHub and grant repository access before linking a project repository.</EmptyState>)}</div>
}
