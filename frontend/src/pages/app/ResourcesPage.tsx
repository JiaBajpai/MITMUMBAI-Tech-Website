import { useCallback, useMemo, useState } from 'react'
import { ExternalLink } from 'lucide-react'
import { domainsApi } from '../../api/domains'
import { resourcesApi, type ResourceFilters } from '../../api/resources'
import { EmptyState, ErrorState, LoadingState, PageHeader } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

const types = ['DOCUMENTATION', 'COURSE', 'TUTORIAL', 'VIDEO', 'ARTICLE', 'PRACTICE_PLATFORM', 'GITHUB_REPO']
const difficulties = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED']

export default function ResourcesPage() {
  const [filters, setFilters] = useState<ResourceFilters>({})
  const key = useMemo(() => JSON.stringify(filters), [filters])
  const load = useCallback((signal: AbortSignal) => resourcesApi.list(filters, signal), [filters])
  const domainLoad = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const resources = useRemoteData(`resources-${key}`, load)
  const domains = useRemoteData('domains-for-resources', domainLoad)
  const change = (field: keyof ResourceFilters, value: string) => setFilters((current) => ({ ...current, [field]: value ? field === 'domainId' ? Number(value) : value : undefined }))
  return <div className="app-page">
    <PageHeader eyebrow="LEARNING / LIBRARY" title="Resources" description="Search and filter the resources available to your account." />
    <div className="app-toolbar app-filter-grid">
      <label className="app-field"><span>Domain</span><select value={filters.domainId || ''} onChange={(event) => change('domainId', event.target.value)}><option value="">All domains</option>{domains.data?.map((domain) => <option key={domain.id} value={domain.id}>{domain.name}</option>)}</select></label>
      <label className="app-field"><span>Resource type</span><select value={filters.type || ''} onChange={(event) => change('type', event.target.value)}><option value="">All types</option>{types.map((type) => <option key={type}>{type}</option>)}</select></label>
      <label className="app-field"><span>Difficulty</span><select value={filters.difficulty || ''} onChange={(event) => change('difficulty', event.target.value)}><option value="">All levels</option>{difficulties.map((level) => <option key={level}>{level}</option>)}</select></label>
      <label className="app-field"><span>Topic</span><input value={filters.topic || ''} onChange={(event) => change('topic', event.target.value)} placeholder="Filter by topic" /></label>
    </div>
    {resources.loading && <LoadingState label="Loading resources" />}{resources.error && <ErrorState error={resources.error} onRetry={resources.reload} />}
    {resources.data && (resources.data.length ? <div className="app-table-wrap"><table className="app-table"><thead><tr><th>Resource</th><th>Domain</th><th>Type</th><th>Difficulty</th><th>Topics</th><th>Quality</th><th /></tr></thead><tbody>{resources.data.map((resource) => <tr key={resource.id}><td><strong>{resource.title}</strong></td><td>{domains.data?.find((domain) => domain.id === resource.domainId)?.name || resource.domainId}</td><td>{resource.type.replaceAll('_', ' ')}</td><td>{resource.difficulty}</td><td>{resource.topics?.length ? resource.topics.join(', ') : '—'}</td><td>{resource.quality ?? '—'}</td><td><a className="app-inline-link app-external-link" href={resource.url} target="_blank" rel="noreferrer">Open <ExternalLink size={13} /></a></td></tr>)}</tbody></table></div> : <EmptyState title="No resources match these filters">The backend returned no resource records for the selected filters. Try a different topic or clear a filter.</EmptyState>)}
  </div>
}
