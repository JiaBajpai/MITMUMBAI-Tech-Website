import React, { useState } from 'react'
import { Server, Database, Shield, Laptop, GitCommit, Network } from 'lucide-react'

export const ArchitectureLab: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'topology' | 'inspector' | 'schema'>('topology')
  const [selectedNodeId, setSelectedNodeId] = useState<string>('core')

  const nodes = [
    {
      id: 'edge',
      name: 'Client Edge (Browser SPA)',
      type: 'PRESENTATION TIER',
      icon: Laptop,
      status: 'ACTIVE',
      protocol: 'HTTPS / TLS 1.3',
      description: 'Single-page application compiled via Vite with React 19. Renders stateful UI and dispatches authenticated REST requests.',
      inputs: 'User pointer & keyboard events, LocalStorage JWT credentials',
      outputs: 'HTTP REST calls to /api/v1/*, WebSocket telemetry',
      invariants: 'Zero client-side secrets, strict CORS origin isolation',
    },
    {
      id: 'gateway',
      name: 'API Gateway & Reverse Proxy',
      type: 'INGRESS TIER',
      icon: Network,
      status: 'ROUTING',
      protocol: 'TCP :8080',
      description: 'Terminates client traffic, enforces rate limiting, normalizes headers, and routes requests to backend services.',
      inputs: 'Raw public TCP streams',
      outputs: 'Internal routed requests, unified error responses',
      invariants: 'Enforces strict CORS origins (http://localhost:5173, http://localhost:3000)',
    },
    {
      id: 'security',
      name: 'Security Filter & JWT Chain',
      type: 'AUTH BOUNDARY',
      icon: Shield,
      status: 'VERIFIED',
      protocol: 'JJWT 0.12.6',
      description: 'Stateless authentication filter. Validates HMAC-SHA256 signatures, extracts user roles and domain permissions.',
      inputs: 'Authorization: Bearer <accessToken>',
      outputs: 'SecurityContext with authenticated UserPrincipal',
      invariants: '15-minute token TTL, 7-day refresh rotation, BCrypt-12 passwords',
    },
    {
      id: 'core',
      name: 'Domain Core Monolith',
      type: 'APPLICATION LOGIC',
      icon: Server,
      status: 'HEALTHY',
      protocol: 'Spring Boot 4.1.1 / Java 25',
      description: 'Modular application core encapsulating domain business rules, transaction boundaries (@Transactional), and DTO projections.',
      inputs: 'Validated Command/Query DTOs',
      outputs: 'Standard API envelope { timestamp, status, success, data }',
      invariants: 'Controller -> Service -> Repository strict layering, zero cyclic dependencies',
    },
    {
      id: 'database',
      name: 'PostgreSQL Relational Engine',
      type: 'PERSISTENCE TIER',
      icon: Database,
      status: 'INDEXED',
      protocol: 'PostgreSQL 16 (Port 5433:5432)',
      description: 'ACID transactional store with Flyway migration history (V1 through V14). Powers XP ledgers and domain indexes.',
      inputs: 'Prepared SQL statements via Hibernate / Spring Data JPA',
      outputs: 'Normalized entity records, aggregated leaderboard counts',
      invariants: 'Foreign key integrity, unique attendance pairs, idempotent XP events',
    },
    {
      id: 'verifier',
      name: 'Task & Evidence Verifier',
      type: 'AUDIT & VERIFICATION',
      icon: GitCommit,
      status: 'STANDBY',
      protocol: 'GitHub OAuth & SHA Hash',
      description: 'Validates submitted task commit SHAs and repository links against member accounts before awarding XP.',
      inputs: 'Repository URLs, commit SHAs, author identity',
      outputs: 'Task completion verification flag, +15 or +40 XP award event',
      invariants: 'Verified task must link to active project member commit',
    },
  ]

  const selectedNode = nodes.find((n) => n.id === selectedNodeId) || nodes[0]
  const NodeIcon = selectedNode.icon

  return (
    <section className="arch-lab-section" id="architecture">
      <div className="kernel-container">
        {/* Section Header */}
        <div className="section-header-editorial">
          <div className="eyebrow-label lime">
            <span className="eyebrow-pip" />
            <span>05 // SYSTEM VISUALIZER</span>
          </div>

          <h2 className="section-heading-dark">
            Explore the Kernel Architecture.
          </h2>

          <p className="section-description-dark">
            Inspect the modular architecture underpinning MIT Tech Kernel.
            Understand how client edges, security boundaries, and relational stores interact.
          </p>
        </div>

        {/* Laboratory Container */}
        <div className="arch-lab-container">
          {/* Top Tabs Bar */}
          <div className="arch-lab-tabs-bar">
            <div className="arch-tab-btns" role="tablist">
              <button
                role="tab"
                aria-selected={activeTab === 'topology'}
                onClick={() => setActiveTab('topology')}
                className={`arch-tab-btn ${activeTab === 'topology' ? 'active' : ''}`}
              >
                01 // SYSTEM TOPOLOGY
              </button>
              <button
                role="tab"
                aria-selected={activeTab === 'inspector'}
                onClick={() => setActiveTab('inspector')}
                className={`arch-tab-btn ${activeTab === 'inspector' ? 'active' : ''}`}
              >
                02 // NODE INSPECTOR
              </button>
              <button
                role="tab"
                aria-selected={activeTab === 'schema'}
                onClick={() => setActiveTab('schema')}
                className={`arch-tab-btn ${activeTab === 'schema' ? 'active' : ''}`}
              >
                03 // SCHEMA CONTRACTS
              </button>
            </div>

            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--lime-accent)' }}>
              [DEMO TOPOLOGY // EDUCATIONAL SIMULATION // NON-PRODUCTION]
            </div>
          </div>

          {/* Interactive Lab Content Area */}
          <div className="arch-lab-content">
            {/* Left Column: Interactive Topology Node Selector */}
            <div className="arch-graph-canvas" role="region" aria-label="Interactive Architecture Nodes">
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)', marginBottom: 8 }}>
                SELECT A SUBSYSTEM TO INSPECT TELEMETRY & INVARIANTS:
              </div>

              {nodes.map((node) => {
                const Icon = node.icon
                const isSelected = node.id === selectedNodeId
                return (
                  <div
                    key={node.id}
                    onClick={() => setSelectedNodeId(node.id)}
                    className={`arch-node-item ${isSelected ? 'selected' : ''}`}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setSelectedNodeId(node.id) }}
                    aria-pressed={isSelected}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div
                        style={{
                          width: 28,
                          height: 28,
                          borderRadius: 3,
                          background: isSelected ? 'var(--lime-accent)' : '#222222',
                          color: isSelected ? '#000000' : 'var(--kernel-text-dark)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <Icon size={14} />
                      </div>
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 600, color: isSelected ? 'var(--lime-accent)' : '#FFFFFF' }}>
                          {node.name}
                        </div>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--kernel-text-dark-muted)' }}>
                          {node.type} // {node.protocol}
                        </div>
                      </div>
                    </div>

                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: 9,
                        padding: '2px 6px',
                        borderRadius: 2,
                        background: isSelected ? 'var(--lime-accent-dim)' : 'rgba(255,255,255,0.05)',
                        color: isSelected ? 'var(--lime-accent)' : 'var(--kernel-text-dark-muted)',
                      }}
                    >
                      {node.status}
                    </span>
                  </div>
                )
              })}
            </div>

            {/* Right Column: Node Details / Schema Inspector */}
            <div className="arch-node-detail-panel">
              {activeTab === 'schema' ? (
                <div>
                  <div style={{ color: 'var(--lime-accent)', marginBottom: 12 }}>
                    // FLYWAY SCHEMA DEFINITIONS (MIGRATIONS V1–V14)
                  </div>
                  <pre
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 11,
                      lineHeight: 1.6,
                      color: '#E0E0E0',
                      margin: 0,
                      overflowX: 'auto',
                    }}
                  >
{`CREATE TABLE sessions (
  id BIGSERIAL PRIMARY KEY,
  domain_id BIGINT REFERENCES domains(id),
  topic VARCHAR(255) NOT NULL,
  type VARCHAR(50) NOT NULL,
  date DATE NOT NULL,
  time TIME NOT NULL,
  description TEXT,
  instructor VARCHAR(255),
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tasks (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT REFERENCES sessions(id),
  title VARCHAR(255) NOT NULL,
  verification_required BOOLEAN DEFAULT FALSE,
  status VARCHAR(50) DEFAULT 'OPEN'
);`}
                  </pre>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingBottom: 12, borderBottom: '1px solid #1E1E1E' }}>
                    <div style={{ width: 32, height: 32, borderRadius: 3, background: 'var(--lime-accent)', color: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <NodeIcon size={16} />
                    </div>
                    <div>
                      <div style={{ color: 'var(--lime-accent)', fontSize: 13, fontWeight: 700 }}>
                        {selectedNode.name}
                      </div>
                      <div style={{ color: '#888888', fontSize: 10 }}>
                        {selectedNode.type} // PROTOCOL: {selectedNode.protocol}
                      </div>
                    </div>
                  </div>

                  <div>
                    <span style={{ color: 'var(--kernel-text-dark-muted)' }}>ROLE & FUNCTION:</span>
                    <p style={{ margin: '4px 0 0 0', color: '#DDDDDD', lineHeight: 1.5 }}>
                      {selectedNode.description}
                    </p>
                  </div>

                  <div>
                    <span style={{ color: 'var(--kernel-text-dark-muted)' }}>INPUT VECTORS:</span>
                    <div style={{ color: '#DDDDDD', marginTop: 2 }}>{selectedNode.inputs}</div>
                  </div>

                  <div>
                    <span style={{ color: 'var(--kernel-text-dark-muted)' }}>OUTPUT VECTORS:</span>
                    <div style={{ color: '#DDDDDD', marginTop: 2 }}>{selectedNode.outputs}</div>
                  </div>

                  <div style={{ padding: 10, background: '#121212', borderLeft: '2px solid var(--lime-accent)', borderRadius: 2 }}>
                    <span style={{ color: 'var(--lime-accent)', fontWeight: 600 }}>SECURITY INVARIANT:</span>
                    <div style={{ color: '#CCCCCC', marginTop: 2 }}>{selectedNode.invariants}</div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
export default ArchitectureLab
