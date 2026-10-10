import React, { useState, useRef, useEffect } from 'react'
import { Terminal as TerminalIcon, Play, RotateCcw } from 'lucide-react'

interface HistoryEntry {
  type: 'command' | 'response' | 'system'
  text: string
}

const INITIAL_OUTPUT: HistoryEntry[] = [
  {
    type: 'system',
    text: 'MIT TECH KERNEL // SHELL DEMONSTRATION ENVIRONMENT v1.0.4\nType "help" to view safe interactive commands or select a pill below.\n[CLIENT-SIDE SANDBOX // NO ARBITRARY SYSTEM EXECUTION]',
  },
]

export const TerminalSection: React.FC = () => {
  const [history, setHistory] = useState<HistoryEntry[]>(INITIAL_OUTPUT)
  const [inputVal, setInputVal] = useState('')
  const terminalBodyRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (terminalBodyRef.current) {
      terminalBodyRef.current.scrollTop = terminalBodyRef.current.scrollHeight
    }
  }, [history])

  const executeCommand = (rawCmd: string) => {
    const cmd = rawCmd.trim().toLowerCase()
    if (!cmd) return

    const newEntries: HistoryEntry[] = [
      ...history,
      { type: 'command', text: `kernel@guest:~$ ${rawCmd}` },
    ]

    switch (cmd) {
      case 'help':
        newEntries.push({
          type: 'response',
          text: `AVAILABLE SAFE COMMANDS:
  help      - Display this list of supported demonstration commands
  about     - Overview of the MIT Tech Kernel engineering community
  domains   - List the 5 core technical disciplines
  events    - Display recent and upcoming club workshops & sessions
  status    - View the operational status of this demo interface
  whoami    - Reveal current visitor context
  clear     - Reset the terminal output buffer`,
        })
        break

      case 'about':
        newEntries.push({
          type: 'response',
          text: `MIT TECH KERNEL:
The premier student-led technical community at MIT Mumbai.
We focus on deep computer systems, web platforms, machine learning, and algorithmic
excellence. Our members learn by dissecting primitives and shipping production software.`,
        })
        break

      case 'domains':
        newEntries.push({
          type: 'response',
          text: `OFFICIAL KERNEL DISCIPLINES:
  01. General                  - Club operations, systems literacy, and open discussions
  02. Frontend & UX            - Modern browser platforms, design systems, and responsive UI
  03. Backend & Systems        - Typed REST APIs, relational persistence, and cloud services
  04. AI / ML / IoT            - Machine learning pipelines, dataset curation, and embedded sensors
  05. Competitive Programming  - Algorithmic problem solving, complexity analysis, and contests`,
        })
        break

      case 'events':
        newEntries.push({
          type: 'response',
          text: `No events are currently published.
Visit the Events page for updates when club events are ready to share.`,
        })
        break

      case 'status':
        newEntries.push({
          type: 'response',
          text: `This is a client-side terminal demonstration.
It does not report live application, backend, database, or network status.`,
        })
        break

      case 'whoami':
        newEntries.push({
          type: 'response',
          text: `VISITOR CONTEXT:
You are exploring the public MIT Tech Kernel website as an unauthenticated guest.
To access authenticated club repositories, tasks, and project boards, select "Enter Members Corner".`,
        })
        break

      case 'clear':
        setHistory([])
        setInputVal('')
        return

      default:
        newEntries.push({
          type: 'response',
          text: `zsh: command not found: "${rawCmd}". Type "help" to view safe interactive commands.`,
        })
        break
    }

    setHistory(newEntries)
    setInputVal('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      executeCommand(inputVal)
    }
  }

  const handleReset = () => {
    setHistory(INITIAL_OUTPUT)
    setInputVal('')
  }

  const quickCommands = ['help', 'about', 'domains', 'events', 'status', 'whoami', 'clear']

  return (
    <section className="terminal-section" id="terminal">
      <div className="kernel-container">
        {/* Section Header */}
        <div className="section-header-editorial">
          <div className="eyebrow-label lime">
            <span className="eyebrow-pip" />
            <span>01 // ENGINEERING PRIMITIVES</span>
          </div>

          <h2 className="section-heading-dark">
            From Kernel space to user space.
          </h2>

          <p className="section-description-dark">
            Engineering begins below the surface. We bridge low-level systems thinking,
            rigorous open-source practices, and user-facing applications through principled,
            hands-on software development.
          </p>
        </div>

        {/* Interactive Terminal Window */}
        <div className="terminal-window">
          {/* Title Bar */}
          <div className="terminal-titlebar">
            <div className="terminal-window-buttons">
              <span className="terminal-dot close" />
              <span className="terminal-dot min" />
              <span className="terminal-dot max" />
            </div>

            <div className="terminal-title-text" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <TerminalIcon size={13} color="var(--lime-accent)" />
              <span>guest@kernel-os: ~/public</span>
            </div>

            <div className="terminal-title-text" style={{ fontSize: 10 }}>
              UTF-8 // SANDBOX
            </div>
          </div>

          {/* Terminal Body */}
          <div className="terminal-body" ref={terminalBodyRef} role="region" aria-label="Interactive Terminal Output">
            {history.map((entry, idx) => (
              <div key={idx} className={`terminal-output-line ${entry.type}`}>
                {entry.text}
              </div>
            ))}
          </div>

          {/* Command Input Bar */}
          <div className="terminal-input-bar">
            <span className="terminal-prompt-prefix">kernel@guest:~$</span>
            <input
              type="text"
              className="terminal-input-field"
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Type command here (e.g. 'help', 'domains')..."
              aria-label="Terminal command input"
            />
            <button
              onClick={() => executeCommand(inputVal)}
              className="btn-editorial-primary"
              style={{ padding: '6px 14px', fontSize: 11 }}
            >
              <Play size={12} />
              <span>Run</span>
            </button>
            <button
              onClick={handleReset}
              className="btn-editorial-secondary"
              style={{ padding: '6px 12px', fontSize: 11 }}
              title="Reset terminal buffer"
            >
              <RotateCcw size={12} />
            </button>
          </div>

          {/* Quick Command Pills */}
          <div className="terminal-quick-commands">
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)', marginRight: 6 }}>
              QUICK COMMANDS:
            </span>
            {quickCommands.map((cmd) => (
              <button
                key={cmd}
                onClick={() => executeCommand(cmd)}
                className="quick-cmd-btn"
              >
                {cmd}
              </button>
            ))}
          </div>
        </div>

        {/* Three Editorial Engineering Feature Blocks */}
        <div className="engineering-grid">
          <div className="engineering-card">
            <div className="engineering-card-num">01 / FOUNDATIONS</div>
            <h3>Deterministic Primitives</h3>
            <p>
              We prioritize understanding foundational data structures, algorithmic complexity,
              and low-level runtime behavior before adopting high-level abstractions or ephemeral frameworks.
            </p>
          </div>

          <div className="engineering-card">
            <div className="engineering-card-num">02 / COLLABORATION</div>
            <h3>Open Source Governance</h3>
            <p>
              Every club project follows transparent repository workflows: structured pull requests,
              peer code reviews, reproducible development containers, and disciplined version control.
            </p>
          </div>

          <div className="engineering-card">
            <div className="engineering-card-num">03 / REALIZATION</div>
            <h3>Disciplined Execution</h3>
            <p>
              Ideas transition into real deliverables. From design documents to tested, deployable code,
              members build technology that actually solves problems and serves our campus community.
            </p>
          </div>
        </div>
      </div>
    </section>
  )
}
export default TerminalSection
