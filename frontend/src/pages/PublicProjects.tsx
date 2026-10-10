import { ArrowRight, Layers3 } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function PublicProjects() {
  return <section className="empty-showcase-page projects-empty-page">
    <div className="empty-showcase-copy"><p className="public-eyebrow">MIT TECH KERNEL · WORK IN PROGRESS</p><h1>Projects</h1><p className="empty-showcase-lead">Ideas take shape here.</p><p className="empty-showcase-body">Project showcases will appear here when they are ready to share. Get to know the club and the ways students explore technology.</p><Link className="public-button dark" to="/about">About the club <ArrowRight size={16} /></Link></div>
    <div className="empty-showcase-visual" aria-hidden="true"><Layers3 size={54} strokeWidth={1} /><div className="empty-visual-grid" /><span>PROJECTS<br />WILL APPEAR HERE</span></div>
  </section>
}
