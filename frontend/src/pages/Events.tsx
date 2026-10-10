import { ArrowRight, CalendarDays } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function Events() {
  return <section className="empty-showcase-page events-empty-page">
    <div className="empty-showcase-copy"><p className="public-eyebrow">MIT TECH KERNEL · GATHERINGS</p><h1>Events</h1><p className="empty-showcase-lead">A space for shared discovery.</p><p className="empty-showcase-body">Event listings will appear here when they are ready to share. In the meantime, explore the technical areas that bring our community together.</p><Link className="public-button dark" to="/domains">Explore domains <ArrowRight size={16} /></Link></div>
    <div className="empty-showcase-visual" aria-hidden="true"><CalendarDays size={54} strokeWidth={1} /><div className="empty-visual-grid" /><span>EVENTS<br />WILL APPEAR HERE</span></div>
  </section>
}
