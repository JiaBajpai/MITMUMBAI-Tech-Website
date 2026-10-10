import React from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, ArrowUpRight } from 'lucide-react'
import KernelLogo from './KernelLogo'

export const Hero: React.FC = () => {
  const navigate = useNavigate()

  const handleExploreClick = () => {
    const target = document.querySelector('#about')
    if (target) {
      target.scrollIntoView({ behavior: 'smooth' })
    }
  }

  return (
    <section className="hero-section" id="hero">
      <div className="kernel-container hero-shell">
        <div className="hero-brand-block">
          <KernelLogo alt="MIT Tech Kernel symbol" className="hero-logo" />
          <span className="hero-club-name">MIT TECH KERNEL</span>
        </div>

        <div className="hero-grid">
          <div className="hero-copy">
            <p className="hero-kicker">Student engineering community • MIT Mumbai</p>
            <h1 className="hero-headline">
              Curiosity builds <span>what comes next.</span>
            </h1>
            <p className="hero-description">
              MIT Tech Kernel brings students together to explore technology, learn the foundations of engineering,
              and build meaningful projects with curiosity, collaboration, and purpose.
            </p>

            <div className="hero-actions">
              <button onClick={handleExploreClick} className="btn-editorial-primary">
                <span>Explore the Club</span>
                <ArrowRight size={15} />
              </button>

              <button onClick={() => navigate('/login')} className="btn-editorial-secondary">
                <span>Members Corner</span>
                <ArrowUpRight size={15} />
              </button>
            </div>
          </div>

          <div className="hero-aside" aria-label="Core principles of MIT Tech Kernel">
            <div className="hero-aside-card">
              <span className="hero-aside-label">01</span>
              <strong>Explore</strong>
              <p>Understand how systems work and why they matter.</p>
            </div>
            <div className="hero-aside-card">
              <span className="hero-aside-label">02</span>
              <strong>Build</strong>
              <p>Turn ideas into useful projects with real momentum.</p>
            </div>
            <div className="hero-aside-card">
              <span className="hero-aside-label">03</span>
              <strong>Evolve</strong>
              <p>Improve through mentorship, feedback, and teamwork.</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
export default Hero
