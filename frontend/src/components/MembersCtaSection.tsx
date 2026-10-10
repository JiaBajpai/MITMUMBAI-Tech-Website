import React from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowUpRight, FolderGit2 } from 'lucide-react'
import KernelLogo from './KernelLogo'

export const MembersCtaSection: React.FC = () => {
  const navigate = useNavigate()

  return (
    <section className="members-cta-section" id="members-corner">
      <div className="kernel-container">
        <div className="members-cta-box">
          {/* Official Emblem */}
          <KernelLogo alt="MIT Tech Kernel symbol" className="members-cta-emblem" />

          <div className="eyebrow-label lime" style={{ marginBottom: 14 }}>
            <span>MEMBERS CORNER // PRIVATE DESTINATION</span>
          </div>

          <h2 className="members-cta-title">
            Your next idea starts here.
          </h2>

          <p className="members-cta-desc">
            An internal platform engineered for active MIT Tech Kernel members.
            Access project workspaces, track verified task completions, submit GitHub evidence,
            and monitor your XP progression across all five domains.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <button
              onClick={() => navigate('/login')}
              className="btn-editorial-primary"
              style={{ padding: '14px 28px', fontSize: 13 }}
            >
              <span>Enter Members Corner</span>
              <ArrowUpRight size={16} />
            </button>

            <button
              onClick={() => navigate('/projects')}
              className="btn-editorial-secondary"
              style={{ padding: '14px 24px', fontSize: 13 }}
            >
              <FolderGit2 size={16} />
              <span>Explore Public Projects</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
export default MembersCtaSection
