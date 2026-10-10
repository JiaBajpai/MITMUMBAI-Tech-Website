import { useState } from 'react'
import { ArrowRight, ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import KernelLogo from '../components/KernelLogo'

const stages = [
  { number: '01', title: 'Explore', summary: 'Discover how technology works.', detail: 'Start with questions. Look beneath the surface of the tools and systems around you.', motif: 'curiosity' },
  { number: '02', title: 'Build', summary: 'Turn ideas into working projects.', detail: 'Combine what you learn, test an idea, and shape it into something people can use.', motif: 'making' },
  { number: '03', title: 'Evolve', summary: 'Experiment, collaborate, and improve.', detail: 'Share your work, learn from feedback, and make the next version stronger.', motif: 'growth' },
]

export default function Home() {
  const [activeStage, setActiveStage] = useState(0)
  const stage = stages[activeStage]

  return <>
    <section className="home-hero">
      <div className="home-hero-copy">
        <p className="public-eyebrow hero-reveal">MIT Mumbai · Student engineering community</p>
        <h1 className="hero-reveal hero-reveal-delay">CORE OF CAMPUS INNOVATION</h1>
        <p className="home-intro hero-reveal hero-reveal-delay-2">MIT Tech Kernel brings students together to explore technology, learn engineering fundamentals, and build meaningful things through curiosity and collaboration.</p>
        <div className="home-actions hero-reveal hero-reveal-delay-2">
          <Link className="public-button dark" to="/about">Explore the club <ArrowRight size={17} /></Link>
          <Link className="public-button light" to="/members">Members Corner <ArrowUpRight size={16} /></Link>
        </div>
      </div>
      <div className="home-hero-visual" aria-label="Official MIT Tech Kernel symbol">
        <div className="hero-mark-stage"><span className="hero-mark-caption">MIT TECH KERNEL <span>·</span> CORE OF INNOVATION</span><KernelLogo className="hero-mark" /></div>
      </div>
      <div className="hero-scroll-cue" aria-hidden="true"><span /> Scroll to explore</div>
    </section>

    <section className="home-experience" aria-labelledby="home-experience-heading">
      <div className="home-experience-inner">
        <div className="experience-heading">
          <p className="public-eyebrow">A PRACTICE OF CURIOSITY</p>
          <h2 id="home-experience-heading">Curiosity becomes something real.</h2>
          <p>Choose a step to see how questions turn into shared work.</p>
          <div className="stage-selector" aria-label="How we learn">
            {stages.map((item, index) => <button type="button" key={item.number} aria-pressed={activeStage === index} onClick={() => setActiveStage(index)}>
              <span>{item.number}</span>{item.title}<ArrowRight size={15} />
            </button>)}
          </div>
        </div>
        <div className={`stage-feature stage-feature-${stage.motif}`} aria-live="polite">
          <div className="stage-feature-top"><span>{stage.number} / 03</span><span>MIT TECH KERNEL</span></div>
          <div className="stage-feature-art" aria-hidden="true"><div className="stage-art-ring ring-one" /><div className="stage-art-ring ring-two" /><span className="stage-art-point" /></div>
          <div className="stage-feature-copy"><h3>{stage.title}</h3><strong>{stage.summary}</strong><p>{stage.detail}</p></div>
        </div>
      </div>
    </section>

    <section className="home-pathways">
      <div className="home-pathways-heading"><p className="public-eyebrow">FIND YOUR WAY IN</p><h2>There’s more than one way to begin.</h2></div>
      <div className="pathway-links">
        <Link to="/domains"><span>01</span><strong>Explore five technical domains</strong><ArrowUpRight size={19} /></Link>
        <Link to="/about"><span>02</span><strong>Get to know the club</strong><ArrowUpRight size={19} /></Link>
        <Link to="/events"><span>03</span><strong>See club events</strong><ArrowUpRight size={19} /></Link>
        <Link to="/projects"><span>04</span><strong>Browse project showcases</strong><ArrowUpRight size={19} /></Link>
        <Link to="/members"><span>05</span><strong>Visit Members Corner</strong><ArrowUpRight size={19} /></Link>
      </div>
    </section>
  </>
}
