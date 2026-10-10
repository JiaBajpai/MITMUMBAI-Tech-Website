import { useState } from 'react'
import { ArrowRight, ArrowUpRight, BookOpen, FolderKanban, ListChecks } from 'lucide-react'
import { Link } from 'react-router-dom'
import KernelLogo from '../components/KernelLogo'

const parts = [
  { label: 'Learn', icon: BookOpen, copy: 'See club sessions and continue learning with the member community.' },
  { label: 'Contribute', icon: ListChecks, copy: 'Follow project work and the tasks connected to it.' },
  { label: 'Keep track', icon: FolderKanban, copy: 'Use the member workspace to find projects and relevant recommendations.' },
]

export default function Members() {
  const [active, setActive] = useState(0)
  const part = parts[active]
  const Icon = part.icon
  return <section className="members-experience">
    <div className="members-hero">
      <div className="members-hero-copy"><p className="public-eyebrow">COMMUNITY · MEMBERS CORNER</p><h1>Keep building together.</h1><p>The Members Corner is the private entry point to the club’s authenticated workspace. Sign in to pick up where you left off.</p><div className="members-actions"><Link className="public-button dark" to="/login">Go to login <ArrowUpRight size={16} /></Link><Link className="public-text-link" to="/domains">Explore public domains <ArrowRight size={15} /></Link></div></div>
      <div className="members-mark-panel"><span>MIT TECH KERNEL</span><KernelLogo className="members-mark" /><strong>One community.<br />Many ways to learn.</strong></div>
    </div>
    <div className="members-journey"><div className="members-journey-heading"><p className="public-eyebrow">INSIDE MEMBERS CORNER</p><h2>A shared place to continue the work.</h2></div><div className="members-journey-content"><div className="members-journey-tabs" aria-label="Member workspace overview">{parts.map((item, index) => { const StepIcon = item.icon; return <button type="button" key={item.label} aria-pressed={active === index} onClick={() => setActive(index)}><StepIcon size={18} /><span>0{index + 1}</span><strong>{item.label}</strong><ArrowRight size={15} /></button> })}</div><div className="members-journey-detail" aria-live="polite"><Icon size={24} strokeWidth={1.5} /><span>0{active + 1} / 03</span><h3>{part.label}</h3><p>{part.copy}</p></div></div></div>
  </section>
}
