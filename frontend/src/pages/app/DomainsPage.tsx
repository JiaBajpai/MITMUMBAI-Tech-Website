import { useCallback } from 'react'
import { BookOpen } from 'lucide-react'
import { domainsApi } from '../../api/domains'
import { EmptyState, ErrorState, LoadingState, PageHeader } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

export default function DomainsPage() {
  const load = useCallback((signal: AbortSignal) => domainsApi.list(signal), [])
  const { data, loading, error, reload } = useRemoteData('domains', load)
  return <div className="app-page">
    <PageHeader eyebrow="LEARNING / DOMAINS" title="Technical domains" description="The available domains provided by the Kernel backend." />
    {loading && <LoadingState label="Loading domains" />}{error && <ErrorState error={error} onRetry={reload} />}
    {data && (data.length ? <div className="app-content-section app-domain-grid">{data.map((domain) => <article className="app-domain-item" key={domain.id}><span>DOMAIN {String(domain.displayOrder).padStart(2, '0')} · ID {domain.id}</span><h2><BookOpen size={17} /> {domain.name}</h2><p>{domain.description || 'No description has been provided.'}</p></article>)}</div> : <EmptyState title="No domains are available">The service returned no domain records.</EmptyState>)}
  </div>
}
