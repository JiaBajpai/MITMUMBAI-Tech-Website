import { useState, type FormEvent } from 'react'
import type { Domain, Project, ProjectRequest } from '../../api/types'
import { AppButton } from './AppUI'

type Props = { domains: Domain[]; project?: Project; saving: boolean; onCancel: () => void; onSubmit: (request: ProjectRequest) => Promise<void>; defaultProgram: string }

export default function ProjectForm({ domains, project, saving, onCancel, onSubmit, defaultProgram }: Props) {
  const [name, setName] = useState(project?.name || '')
  const [domainId, setDomainId] = useState(project ? String(project.domainId) : '')
  const [program, setProgram] = useState<'TECHNICAL' | 'FOUNDATION'>(project?.program === 'FOUNDATION' || defaultProgram === 'FOUNDATION' ? 'FOUNDATION' : 'TECHNICAL')
  const [problem, setProblem] = useState(project?.problem || '')
  const [solution, setSolution] = useState(project?.solution || '')
  const [technologies, setTechnologies] = useState(project?.technologies.join(', ') || '')
  const [requiredSkills, setRequiredSkills] = useState(project?.requiredSkills.join(', ') || '')
  const [expectedMembers, setExpectedMembers] = useState(String(project?.expectedMembers || 2))
  const [outcome, setOutcome] = useState(project?.outcome || '')
  const [error, setError] = useState<string | null>(null)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(null)
    if (!name.trim() || !problem.trim() || !solution.trim() || !domainId || Number(expectedMembers) < 1) { setError('Complete the required fields and choose a positive team size.'); return }
    const split = (value: string) => value.split(',').map((part) => part.trim()).filter(Boolean)
    try { await onSubmit({ name: name.trim(), domainId: Number(domainId), program, problem: problem.trim(), solution: solution.trim(), technologies: split(technologies), requiredSkills: split(requiredSkills), expectedMembers: Number(expectedMembers), outcome: outcome.trim() || undefined }) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Project could not be saved.') }
  }

  return <form className="app-form app-card" onSubmit={(event) => void submit(event)}>
    <div className="app-field"><label htmlFor="project-name">Project name *</label><input id="project-name" required maxLength={160} value={name} onChange={(event) => setName(event.target.value)} /></div>
    <div className="app-field"><label htmlFor="project-domain">Domain *</label><select id="project-domain" required value={domainId} onChange={(event) => setDomainId(event.target.value)}><option value="">Choose domain</option>{domains.map((domain) => <option key={domain.id} value={domain.id}>{domain.name}</option>)}</select></div>
    <div className="app-field"><label htmlFor="project-program">Program *</label><select id="project-program" value={program} onChange={(event) => setProgram(event.target.value as 'TECHNICAL' | 'FOUNDATION')}><option value="TECHNICAL">Technical</option><option value="FOUNDATION">Foundation</option></select></div>
    <div className="app-field"><label htmlFor="project-team-size">Expected members *</label><input id="project-team-size" type="number" min="1" required value={expectedMembers} onChange={(event) => setExpectedMembers(event.target.value)} /></div>
    <div className="app-field span-2"><label htmlFor="project-problem">Problem *</label><textarea id="project-problem" required value={problem} onChange={(event) => setProblem(event.target.value)} /></div>
    <div className="app-field span-2"><label htmlFor="project-solution">Solution *</label><textarea id="project-solution" required value={solution} onChange={(event) => setSolution(event.target.value)} /></div>
    <div className="app-field"><label htmlFor="project-tech">Technologies, comma separated</label><input id="project-tech" value={technologies} onChange={(event) => setTechnologies(event.target.value)} /></div>
    <div className="app-field"><label htmlFor="project-skills">Required skills, comma separated</label><input id="project-skills" value={requiredSkills} onChange={(event) => setRequiredSkills(event.target.value)} /></div>
    <div className="app-field span-2"><label htmlFor="project-outcome">Expected outcome</label><textarea id="project-outcome" value={outcome} onChange={(event) => setOutcome(event.target.value)} /></div>
    {error && <p className="app-notice error span-2" role="alert">{error}</p>}
    <div className="app-form-actions"><AppButton type="submit" disabled={saving}>{saving ? 'Saving…' : project ? 'Save changes' : 'Create project'}</AppButton><AppButton type="button" variant="secondary" onClick={onCancel}>Cancel</AppButton></div>
  </form>
}
