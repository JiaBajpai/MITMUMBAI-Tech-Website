import { useState } from 'react'
import { ArrowRight } from 'lucide-react'

const learningSteps = [
  { title: 'Understand the fundamentals', label: 'Start with the why', body: 'Break a topic into its essential ideas. A clear foundation makes new tools easier to understand and use.' },
  { title: 'Experiment with ideas', label: 'Try a small change', body: 'Make a focused experiment, observe what happens, and use the result to ask a better question.' },
  { title: 'Build projects', label: 'Bring ideas together', body: 'Apply what you have learned in a practical project, working through the details one step at a time.' },
  { title: 'Learn from feedback', label: 'Improve together', body: 'Share your work, listen to different perspectives, and refine your approach with others.' },
]

export default function About() {
  const [active, setActive] = useState(0)
  const step = learningSteps[active]
  return <article className="about-page">
    <section className="about-intro">
      <div className="about-intro-copy"><p className="public-eyebrow">MIT TECH KERNEL · ABOUT US</p><h1>Curiosity is where engineering begins.</h1></div>
      <div className="about-intro-aside"><span className="about-aside-index">01 — 04</span><p>MIT Tech Kernel is a student community at MIT Mumbai for people who want to understand technology by exploring it and building with it.</p></div>
      <div className="about-graphic" aria-hidden="true"><span className="about-graphic-line" /><span className="about-graphic-dot" /><span className="about-graphic-label">QUESTION → POSSIBILITY</span></div>
    </section>
    <section className="about-principles">
      <div className="about-principles-lead"><p className="public-eyebrow">WHAT WE VALUE</p><h2>Learn deeply.<br />Make thoughtfully.</h2></div>
      <div className="about-principles-copy"><p>We value questions, careful experiments, and learning the ideas behind the tools we use. Progress comes from trying, reflecting, and improving together.</p><p>Students with different levels of experience can learn alongside one another, collaborate across interests, and build at a pace that makes room for understanding.</p></div>
    </section>
    <section className="learning-explorer" aria-labelledby="learning-heading">
      <div className="learning-explorer-heading"><p className="public-eyebrow">HOW WE LEARN</p><h2 id="learning-heading">A process you can grow into.</h2><p>Explore each part of the learning cycle.</p></div>
      <div className="learning-explorer-body">
        <div className="learning-step-list" aria-label="Learning stages">{learningSteps.map((item, index) => <button key={item.title} type="button" aria-pressed={active === index} onClick={() => setActive(index)}><span>0{index + 1}</span><strong>{item.title}</strong><ArrowRight size={16} /></button>)}</div>
        <div className="learning-step-detail" aria-live="polite"><span>0{active + 1} / 04</span><p>{step.label}</p><h3>{step.title}</h3><div className="learning-progress" aria-hidden="true"><span style={{ width: `${((active + 1) / learningSteps.length) * 100}%` }} /></div><p className="learning-step-copy">{step.body}</p></div>
      </div>
    </section>
  </article>
}
