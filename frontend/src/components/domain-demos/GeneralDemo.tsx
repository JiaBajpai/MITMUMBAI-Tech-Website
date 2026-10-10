import React, { useState } from 'react'
import { Users, Layers, Award, Terminal } from 'lucide-react'

export const GeneralDemo: React.FC = () => {
  const [selectedTrack, setSelectedTrack] = useState<number>(0)

  const tracks = [
    {
      title: 'Foundation',
      level: 'Stage 01',
      icon: Terminal,
      focus: 'Git & Linux fundamentals, open source etiquette, shell proficiency.',
      deliverable: 'First verified GitHub pull request and environment setup.',
    },
    {
      title: 'Specialization',
      level: 'Stage 02',
      icon: Layers,
      focus: 'Immersion in a primary domain (Frontend, Backend, AI/ML, or CP).',
      deliverable: 'Completing domain tasks and participating in hands-on workshops.',
    },
    {
      title: 'Collaborative Build',
      level: 'Stage 03',
      icon: Users,
      focus: 'Forming inter-disciplinary teams for official club projects.',
      deliverable: 'Deploying an MVP project with CI/CD and architecture review.',
    },
    {
      title: 'Mentorship & Lead',
      level: 'Stage 04',
      icon: Award,
      focus: 'Guiding new members, authoring workshop material, organizing events.',
      deliverable: 'Domain leadership and technical roadmap stewardship.',
    },
  ]

  const ActiveIcon = tracks[selectedTrack].icon

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--lime-accent)' }}>
          INTERACTIVE LEARNER JOURNEY
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)' }}>
          SELECT MILESTONE TO INSPECT
        </span>
      </div>

      {/* Track Selection Buttons */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
        {tracks.map((t, idx) => (
          <button
            key={t.title}
            onClick={() => setSelectedTrack(idx)}
            style={{
              padding: '10px 8px',
              background: selectedTrack === idx ? '#1F1F1F' : '#111111',
              border: `1px solid ${selectedTrack === idx ? 'var(--lime-accent)' : 'var(--kernel-border-dark)'}`,
              borderRadius: 4,
              color: selectedTrack === idx ? '#FFFFFF' : 'var(--kernel-text-dark-muted)',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              cursor: 'pointer',
              textAlign: 'center',
              transition: 'all 0.2s ease',
            }}
          >
            <div>{t.level}</div>
            <div style={{ fontWeight: 600, marginTop: 4, color: selectedTrack === idx ? 'var(--lime-accent)' : 'inherit' }}>
              {t.title}
            </div>
          </button>
        ))}
      </div>

      {/* Selected Stage Detail Display */}
      <div
        style={{
          background: '#131313',
          border: '1px solid var(--kernel-border-dark)',
          borderRadius: 4,
          padding: 20,
          display: 'flex',
          flexDirection: 'column',
          gap: 12,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 4,
              background: 'var(--lime-accent-dim)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--lime-accent)',
            }}
          >
            <ActiveIcon size={18} />
          </div>
          <div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--lime-accent)' }}>
              {tracks[selectedTrack].level} // {tracks[selectedTrack].title.toUpperCase()}
            </div>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#FFFFFF' }}>
              Path Objectives & Core Competencies
            </div>
          </div>
        </div>

        <p style={{ margin: 0, fontSize: 13, color: 'var(--kernel-text-dark-muted)', lineHeight: 1.6 }}>
          {tracks[selectedTrack].focus}
        </p>

        <div
          style={{
            padding: '8px 12px',
            background: '#0B0B0B',
            borderRadius: 4,
            borderLeft: '2px solid var(--lime-accent)',
            fontFamily: 'var(--font-mono)',
            fontSize: 11,
            color: '#D4D4D4',
          }}
        >
          <strong style={{ color: 'var(--lime-accent)' }}>TARGET DELIVERABLE:</strong> {tracks[selectedTrack].deliverable}
        </div>
      </div>
    </div>
  )
}
export default GeneralDemo
