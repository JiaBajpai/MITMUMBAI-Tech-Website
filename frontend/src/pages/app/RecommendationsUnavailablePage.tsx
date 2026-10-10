import { Link } from 'react-router-dom'
import { EmptyState, PageHeader } from '../../components/app/AppUI'

export default function RecommendationsUnavailablePage() {
  return <div className="app-page"><PageHeader eyebrow="LEARNING / RECOMMENDATIONS" title="Recommendations" description="This area is reserved for personalized learning recommendations." /><EmptyState title="Recommendations are not available yet">The current backend has no recommendations endpoint, so this page does not display sample or generated suggestions.</EmptyState><Link className="app-button secondary unavailable-link" to="/app/domains">Explore available domains</Link></div>
}
