import React, { useState } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import FrontendDemo from './domain-demos/FrontendDemo'
import BackendDemo from './domain-demos/BackendDemo'
import AiIotDemo from './domain-demos/AiIotDemo'
import GeneralDemo from './domain-demos/GeneralDemo'
import CpDemo from './domain-demos/CpDemo'

export const DomainsSection: React.FC = () => {
  const [activeTab, setActiveTab] = useState<number>(0)
  const [showDeepDive, setShowDeepDive] = useState<boolean>(false)

  const domains = [
    {
      id: 'frontend',
      num: '01',
      name: 'Frontend',
      subtitle: 'Interfaces people actually enjoy using',
      description:
        'We design interfaces that are clear, thoughtful, and responsive — balancing accessibility, performance, and user attention with a refined product experience.',
      pills: ['React', 'UI Systems', 'Accessibility', 'Performance', 'Design Thinking'],
      deepDiveText:
        'Frontend work at MIT Tech Kernel focuses on readable interfaces, component clarity, responsive layouts, and thoughtful interaction design grounded in user needs.',
      component: FrontendDemo,
    },
    {
      id: 'backend',
      num: '02',
      name: 'Backend',
      subtitle: 'The systems behind the experience',
      description:
        'Backend work turns ideas into reliable systems. We build APIs, connect services, improve data flows, and focus on maintainable architecture and clear contracts.',
      pills: ['APIs', 'Databases', 'Architecture', 'Security', 'System Design'],
      deepDiveText:
        'Members learn to structure service logic, validate requests, reason about data integrity, and build resilient backend flows that scale responsibly.',
      component: BackendDemo,
    },
    {
      id: 'machine-learning',
      num: '03',
      name: 'Machine Learning',
      subtitle: 'Patterns, data, and prediction',
      description:
        'Machine learning introduces students to data-driven thinking: understanding patterns, training models, and evaluating how systems make decisions from evidence.',
      pills: ['Data', 'Models', 'Evaluation', 'Prediction', 'Experimentation'],
      deepDiveText:
        'We explore how models learn from data, how to reason about metrics, and how to apply machine learning principles responsibly and practically.',
      component: AiIotDemo,
    },
    {
      id: 'iot',
      num: '04',
      name: 'Internet of Things',
      subtitle: 'Connected systems in the physical world',
      description:
        'From sensors to embedded devices, IoT introduces the interplay between hardware, software, and real-world signals. It is about building connected systems that respond intelligently.',
      pills: ['Sensors', 'Embedded Systems', 'Signals', 'Connectivity', 'Automation'],
      deepDiveText:
        'Members look at sensor-driven systems, data acquisition, automation workflows, and how software interacts with the physical environment through devices.',
      component: GeneralDemo,
    },
    {
      id: 'cp',
      num: '05',
      name: 'Competitive Programming',
      subtitle: 'Problem solving under constraint',
      description:
        'Competitive programming strengthens algorithmic thinking, teaches efficient patterns, and helps students reason clearly under pressure while building confidence in optimization.',
      pills: ['Algorithms', 'Data Structures', 'Optimization', 'Logic', 'Complexity'],
      deepDiveText:
        'This domain builds speed, rigor, and clarity in problem solving through structured analysis, efficient implementations, and careful reasoning about complexity.',
      component: CpDemo,
    },
  ]

  const currentDomain = domains[activeTab]
  const ActiveDemoComponent = currentDomain.component

  return (
    <section className="domains-section" id="domains">
      <div className="kernel-container">
        <div className="section-header-editorial">
          <div className="eyebrow-label dark-muted">
            <span className="eyebrow-pip" />
            <span>03 // FIND YOUR WAY TO BUILD</span>
          </div>

          <h2 className="section-heading-dark">Find your way to build.</h2>

          <p className="section-description-dark">
            MIT Tech Kernel brings together five core technical paths, each designed to help students learn by doing and discover where they enjoy creating.
          </p>
        </div>

        <div className="domains-tab-bar" role="tablist" aria-label="Engineering Domains">
          {domains.map((dom, index) => (
            <button
              key={dom.id}
              role="tab"
              aria-selected={activeTab === index}
              onClick={() => {
                setActiveTab(index)
                setShowDeepDive(false)
              }}
              className={`domain-tab-btn ${activeTab === index ? 'active' : ''}`}
            >
              <span>{dom.num}</span>
              <span>{dom.name}</span>
            </button>
          ))}
        </div>

        <div className="domain-content-panel">
          <div className="domain-info-col">
            <div className="eyebrow-label dark-muted">
              <span>{currentDomain.num} // {currentDomain.subtitle.toUpperCase()}</span>
            </div>

            <h3>{currentDomain.name}</h3>
            <p>{currentDomain.description}</p>

            <div className="domain-tech-pills">
              {currentDomain.pills.map((pill) => (
                <span key={pill} className="domain-pill">
                  {pill}
                </span>
              ))}
            </div>

            <div className="deep-dive-toggle-wrap">
              <button
                onClick={() => setShowDeepDive(!showDeepDive)}
                className="deep-dive-toggle"
              >
                <span>{showDeepDive ? 'Hide technical context' : 'Expand technical context'}</span>
                {showDeepDive ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
              </button>

              {showDeepDive && (
                <div className="deep-dive-panel">
                  {currentDomain.deepDiveText}
                </div>
              )}
            </div>
          </div>

          <div className="domain-interactive-box">
            <div className="domain-interactive-header">
              <span>{currentDomain.name.toUpperCase()} // LAB</span>
              <span className="domain-live-tag">Illustrative demo</span>
            </div>

            <ActiveDemoComponent />
          </div>
        </div>
      </div>
    </section>
  )
}
export default DomainsSection
