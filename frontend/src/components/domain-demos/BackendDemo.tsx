import React, { useState } from 'react'
import { Send, Database, Shield, Server, Laptop } from 'lucide-react'

export const BackendDemo: React.FC = () => {
  const [activeStep, setActiveStep] = useState<number>(0)
  const [isProcessing, setIsProcessing] = useState<boolean>(false)

  const steps = [
    { name: 'Browser Client', icon: Laptop, latency: '0ms', role: 'Issues GET /api/v1/sessions HTTP/1.1 request' },
    { name: 'JWT Security Filter', icon: Shield, latency: '3ms', role: 'Validates Authorization Bearer token signature' },
    { name: 'Spring Controller', icon: Server, latency: '7ms', role: 'Binds request parameters and routes to SessionService' },
    { name: 'PostgreSQL 16 DB', icon: Database, latency: '12ms', role: 'Executes indexed query with Flyway schema constraints' },
  ]

  const runSimulation = () => {
    if (isProcessing) return
    setIsProcessing(true)
    setActiveStep(1)

    setTimeout(() => {
      setActiveStep(2)
      setTimeout(() => {
        setActiveStep(3)
        setTimeout(() => {
          setActiveStep(4)
          setIsProcessing(false)
        }, 600)
      }, 600)
    }, 600)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--lime-accent)' }}>
          REQUEST-RESPONSE LIFECYCLE
        </span>
        <button
          onClick={runSimulation}
          disabled={isProcessing}
          className="btn-editorial-primary"
          style={{ padding: '6px 12px', fontSize: 10 }}
        >
          <Send size={11} />
          <span>{isProcessing ? 'TRANSITING...' : 'DISPATCH REQUEST'}</span>
        </button>
      </div>

      {/* Visual Pipeline Tiers */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
        {steps.map((st, idx) => {
          const Icon = st.icon
          const isPassed = activeStep > idx
          const isCurrent = activeStep === idx + 1
          return (
            <div
              key={st.name}
              style={{
                padding: '12px 8px',
                background: isCurrent ? '#1E1E1E' : isPassed ? '#141414' : '#0E0E0E',
                border: `1px solid ${isCurrent ? 'var(--lime-accent)' : isPassed ? 'rgba(163, 230, 53, 0.4)' : 'var(--kernel-border-dark)'}`,
                borderRadius: 4,
                textAlign: 'center',
                transition: 'all 0.3s ease',
              }}
            >
              <div
                style={{
                  width: 28,
                  height: 28,
                  margin: '0 auto 6px auto',
                  borderRadius: '50%',
                  background: isCurrent ? 'var(--lime-accent)' : 'rgba(255,255,255,0.05)',
                  color: isCurrent ? '#000000' : isPassed ? 'var(--lime-accent)' : 'var(--kernel-text-dark-muted)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Icon size={14} />
              </div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, fontWeight: 600, color: isCurrent ? 'var(--lime-accent)' : '#FFFFFF' }}>
                {st.name}
              </div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--kernel-text-dark-muted)', marginTop: 2 }}>
                +{st.latency}
              </div>
            </div>
          )
        })}
      </div>

      {/* Response Payload & Status Console */}
      <div
        style={{
          background: '#0A0A0A',
          border: '1px solid var(--kernel-border-dark)',
          borderRadius: 4,
          padding: 14,
          fontFamily: 'var(--font-mono)',
          fontSize: 11,
          lineHeight: 1.5,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #1A1A1A', paddingBottom: 6, marginBottom: 8 }}>
          <span style={{ color: activeStep === 4 ? 'var(--lime-accent)' : 'var(--kernel-text-dark-muted)' }}>
            HTTP/1.1 {activeStep === 4 ? '200 OK' : 'PENDING DISPATCH'}
          </span>
          <span style={{ color: 'var(--kernel-text-dark-muted)' }}>
            TIME: {activeStep === 4 ? '22ms' : '--'} // CONTENT-TYPE: APPLICATION/JSON
          </span>
        </div>

        <div style={{ color: activeStep === 4 ? '#CE9178' : '#666666' }}>
          {activeStep === 4 ? (
            `{
  "status": 200,
  "success": true,
  "data": [
    { "id": 1, "topic": "REST APIs with Spring Boot", "domain": "Backend", "status": "UPCOMING" },
    { "id": 2, "topic": "PostgreSQL Fundamentals", "domain": "Backend", "status": "UPCOMING" }
  ]
}`
          ) : (
            'Click "Dispatch Request" above to simulate an authenticated Spring Boot request cycle.'
          )}
        </div>
      </div>
    </div>
  )
}
export default BackendDemo
