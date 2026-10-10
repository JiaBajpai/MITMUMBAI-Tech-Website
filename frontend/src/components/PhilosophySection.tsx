import React from 'react'

export const PhilosophySection: React.FC = () => {
  return (
    <section className="philosophy-section" id="about">
      <div className="kernel-container">
        {/* Section Header */}
        <div className="section-header-editorial">
          <div className="eyebrow-label light-muted">
            <span className="eyebrow-pip" style={{ backgroundColor: '#111111' }} />
            <span>02 // CLUB PHILOSOPHY</span>
          </div>

          <h2 className="section-heading-light">
            More than a technical club.
          </h2>

          <p className="section-description-light">
            MIT TECH KERNEL brings together curious minds to dissect technology,
            experiment fearlessly with fundamental concepts, and engineer resilient
            systems with purpose.
          </p>
        </div>

        {/* Three Editorial Stages */}
        <div className="philosophy-stages-grid">
          {/* Stage 01 */}
          <div className="philosophy-stage-item">
            <div className="stage-number">01</div>
            <div className="stage-action-label">EXPLORE</div>
            <h3 className="stage-title">Understand how technology works.</h3>
            <p className="stage-copy">
              Look beyond high-level libraries. We inspect protocols, analyze source code,
              dissect operating system kernels, and question default assumptions. Curiosity is our first tool.
            </p>
            <div className="stage-tags">
              <span className="stage-tag">SYSTEMS INQUIRY</span>
              <span className="stage-tag">FIRST PRINCIPLES</span>
              <span className="stage-tag">DEEP DIVES</span>
            </div>
          </div>

          {/* Stage 02 */}
          <div className="philosophy-stage-item">
            <div className="stage-number">02</div>
            <div className="stage-action-label">BUILD</div>
            <h3 className="stage-title">Turn ideas into working projects.</h3>
            <p className="stage-copy">
              Theory finds meaning in implementation. Members form cross-domain teams to architect
              backend services, craft accessible user interfaces, train lightweight models, and solve hard problems.
            </p>
            <div className="stage-tags">
              <span className="stage-tag">HANDS-ON SHIP</span>
              <span className="stage-tag">MODULAR ARCHITECTURE</span>
              <span className="stage-tag">CODE REVIEWS</span>
            </div>
          </div>

          {/* Stage 03 */}
          <div className="philosophy-stage-item">
            <div className="stage-number">03</div>
            <div className="stage-action-label">EVOLVE</div>
            <h3 className="stage-title">Improve through collaboration.</h3>
            <p className="stage-copy">
              Excellence is an iterative process. Through mentorship, peer feedback, hackathons,
              and contest debriefs, our engineers continuously refine their technical depth and leadership.
            </p>
            <div className="stage-tags">
              <span className="stage-tag">PEER CRITIQUE</span>
              <span className="stage-tag">REFACTORING</span>
              <span className="stage-tag">LIFELONG GROWTH</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
export default PhilosophySection
