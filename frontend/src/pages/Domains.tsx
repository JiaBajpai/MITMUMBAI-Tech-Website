import { useState } from 'react'
import { ArrowDown, ArrowRight, ArrowUpRight, Check, ChevronRight, RotateCcw } from 'lucide-react'

const domains = [
  { name: 'Frontend', short: 'FE', description: 'The interface layer: the visible parts of digital products and how people interact with them.' },
  { name: 'Backend', short: 'BE', description: 'The application logic and data exchange that make a digital experience work.' },
  { name: 'Machine Learning (ML)', short: 'ML', description: 'A way to use examples and patterns in data to make a simple prediction.' },
  { name: 'Internet of Things (IoT)', short: 'IoT', description: 'Connected devices translate physical measurements into useful signals.' },
  { name: 'Competitive Programming (CP)', short: 'CP', description: 'Algorithmic techniques help solve clearly defined problems efficiently.' },
]

function FrontendDemo() {
  const [dark, setDark] = useState(false)
  const [compact, setCompact] = useState(false)
  return <div className="demo-frame frontend-demo">
    <div className="demo-frame-heading"><span>LIVE INTERFACE PREVIEW</span><span>INTERACTIVE</span></div>
    <div className={`ui-preview${dark ? ' ui-preview-dark' : ''}${compact ? ' ui-preview-compact' : ''}`}>
      <div className="ui-preview-nav"><span className="ui-preview-logo">fieldnotes</span><span>Journal</span><span>About</span></div>
      <p className="ui-preview-kicker">A SMALL COLLECTION OF IDEAS</p><h3>Make room<br />for discovery.</h3><p className="ui-preview-body">A clear layout helps the important things find their place.</p><button type="button" onClick={() => setCompact(!compact)}>{compact ? 'Show full layout' : 'Explore the layout'} <ArrowRight size={13} /></button>
    </div>
    <div className="demo-controls"><button type="button" className="demo-control" aria-pressed={dark} onClick={() => setDark(!dark)}><span className={`toggle-dot${dark ? ' on' : ''}`} />Dark preview</button><span className="demo-explanation">Change the preview theme and layout.</span></div>
  </div>
}

function BackendDemo() {
  const requests = [
    { method: 'GET', path: '/api/profile', response: '{ "name": "Asha", "domain": "Frontend" }' },
    { method: 'GET', path: '/api/domains', response: '{ "domains": ["Frontend", "Backend", "ML", "IoT", "CP"] }' },
    { method: 'POST', path: '/api/ideas', response: '{ "saved": true, "id": 24 }' },
  ]
  const [request, setRequest] = useState(0)
  const [stage, setStage] = useState(-1)
  const selected = requests[request]
  const stageLabels = ['Request', 'Application', 'Response']
  return <div className="demo-frame backend-demo">
    <div className="demo-frame-heading"><span>REQUEST → RESPONSE</span><span>EDUCATIONAL SIMULATION</span></div>
    <label className="demo-select-label" htmlFor="request-choice">Choose a sample request</label>
    <select id="request-choice" value={request} onChange={(event) => { setRequest(Number(event.target.value)); setStage(-1) }}>
      {requests.map((item, index) => <option value={index} key={item.path}>{item.method} {item.path}</option>)}
    </select>
    <div className="request-flow">{stageLabels.map((label, index) => <div className={`request-step${stage >= index ? ' reached' : ''}`} key={label}><span>{stage > index ? <Check size={15} /> : `0${index + 1}`}</span><strong>{label}</strong></div>)}</div>
    <div className="request-detail" aria-live="polite"><span>{stage < 0 ? 'READY TO SEND' : stageLabels[stage].toUpperCase()}</span><code>{stage === 0 ? `${selected.method} ${selected.path}` : stage === 1 ? 'The application checks the route and prepares a result.' : stage === 2 ? selected.response : 'Select “Run next step” to follow the request.'}</code></div>
    <button type="button" className="demo-action" onClick={() => setStage((value) => value >= 2 ? -1 : value + 1)}>{stage < 0 || stage >= 2 ? 'Run simulation' : 'Run next step'} <ArrowRight size={14} /></button>
    <p className="demo-explanation">A local illustration of the client/server exchange. No live service is called.</p>
  </div>
}

function MachineLearningDemo() {
  const [signal, setSignal] = useState(5)
  const classification = signal >= 6 ? 'Likely to bloom' : 'Not yet blooming'
  return <div className="demo-frame ml-demo">
    <div className="demo-frame-heading"><span>A SIMPLE CLASSIFIER</span><span>RULE-BASED EXAMPLE</span></div>
    <div className="ml-visual" aria-hidden="true"><span className={signal >= 6 ? 'ml-sun high' : 'ml-sun'} /><div className="ml-ground" /><span className="ml-sprout">{signal >= 6 ? '✳' : '·'}</span></div>
    <label htmlFor="ml-signal">Warm sunny days this week <strong>{signal}</strong></label>
    <input id="ml-signal" type="range" min="0" max="10" value={signal} onChange={(event) => setSignal(Number(event.target.value))} />
    <div className="ml-result" aria-live="polite"><span>RULE OUTPUT</span><strong>{classification}</strong></div>
    <p className="demo-explanation">This toy example uses one rule: at six or more warm days, predict “Likely to bloom.” It is not a trained model.</p>
  </div>
}

function IotDemo() {
  const [temperature, setTemperature] = useState(22)
  const state = temperature >= 28 ? 'Fan on' : temperature <= 16 ? 'Fan off · cool' : 'Fan off'
  return <div className="demo-frame iot-demo">
    <div className="demo-frame-heading"><span>ROOM SENSOR</span><span>SIMULATED READING</span></div>
    <div className="sensor-scene"><div className="sensor-device"><span className="sensor-device-dot" /><span>ROOM<br />SENSOR</span></div><div className="sensor-reading"><strong>{temperature}°</strong><span>simulated °C</span></div></div>
    <label htmlFor="sensor-input">Adjust simulated temperature</label>
    <input id="sensor-input" type="range" min="10" max="35" value={temperature} onChange={(event) => setTemperature(Number(event.target.value))} />
    <div className="sensor-state" aria-live="polite"><span>DEVICE RESPONSE</span><strong>{state}</strong></div>
    <p className="demo-explanation">Illustration rule: the fan turns on at 28°C and above. This is not a live sensor.</p>
  </div>
}

function CpDemo() {
  const values = [3, 8, 12, 17, 23, 31, 42]
  const target = 23
  const [low, setLow] = useState(0)
  const [high, setHigh] = useState(values.length - 1)
  const [found, setFound] = useState(false)
  const [done, setDone] = useState(false)
  const mid = Math.floor((low + high) / 2)
  const step = () => {
    if (values[mid] === target) { setFound(true); setDone(true); return }
    if (values[mid] < target) setLow(mid + 1)
    else setHigh(mid - 1)
    if ((values[mid] < target && mid + 1 > high) || (values[mid] > target && mid - 1 < low)) setDone(true)
  }
  const reset = () => { setLow(0); setHigh(values.length - 1); setFound(false); setDone(false) }
  const active = done ? (found ? values.indexOf(target) : -1) : mid
  return <div className="demo-frame cp-demo">
    <div className="demo-frame-heading"><span>BINARY SEARCH</span><span>STEP THROUGH</span></div>
    <p className="cp-target">Find <strong>{target}</strong> in this sorted list.</p>
    <div className="cp-array" aria-label="Sorted values">{values.map((value, index) => <div key={value} className={`${index === active ? 'current' : ''}${found && value === target ? ' found' : ''}${!done && (index < low || index > high) ? ' excluded' : ''}`}><span>{value}</span><small>{index === active ? 'mid' : ''}</small></div>)}</div>
    <p className="cp-explanation" aria-live="polite">{done ? found ? `Found ${target} at index ${values.indexOf(target)}.` : `${target} is not in this range.` : `Check the middle value, ${values[mid]}. ${values[mid] === target ? 'It matches.' : values[mid] < target ? 'It is lower than the target, so discard the left half.' : 'It is higher than the target, so discard the right half.'}`}</p>
    <div className="cp-controls"><button type="button" className="demo-action" onClick={done ? reset : step}>{done ? 'Start again' : 'Next step'} {done ? <RotateCcw size={14} /> : <ChevronRight size={15} />}</button><span>Stepwise binary search · O(log n)</span></div>
    <p className="demo-explanation">Binary search repeatedly halves a sorted range until the target is found.</p>
  </div>
}

const demos = [FrontendDemo, BackendDemo, MachineLearningDemo, IotDemo, CpDemo]

export default function Domains() {
  const [active, setActive] = useState(0)
  const ActiveDemo = demos[active]
  return <section className="domains-page">
    <div className="domains-intro"><div><p className="public-eyebrow">EXPLORE · LEARN · BUILD</p><h1>Five paths into technology.</h1></div><p>Each domain offers a different way to ask questions, learn the fundamentals, and make something work.</p></div>
    <div className="domain-explorer">
      <nav className="domain-picker" aria-label="Choose a technology domain">{domains.map((domain, index) => <button type="button" key={domain.short} aria-pressed={active === index} onClick={() => setActive(index)}><span>{domain.short}</span><strong>{domain.name}</strong><ArrowUpRight size={16} /></button>)}</nav>
      <section className="domain-detail" aria-live="polite">
        <div className="domain-detail-heading"><span>DOMAIN 0{active + 1} / 05</span><span>SELECT A DOMAIN TO EXPLORE</span></div>
        <div className="domain-detail-intro"><h2>{domains[active].name}</h2><p>{domains[active].description}</p></div>
        <ActiveDemo key={active} />
      </section>
    </div>
    <div className="domain-page-note"><ArrowDown size={16} /><p>Explore at your own pace. Each preview is an educational illustration.</p></div>
  </section>
}
