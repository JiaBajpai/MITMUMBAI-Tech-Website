import React, { useState } from 'react'
import { AlertTriangle, CheckCircle } from 'lucide-react'

export const AiIotDemo: React.FC = () => {
  const [sensorValue, setSensorValue] = useState<number>(42)
  const threshold = 65

  const isAnomaly = sensorValue >= threshold

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--lime-accent)' }}>
          SIMULATED IOT SENSOR & CLASSIFIER
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--kernel-text-dark-muted)' }}>
          [LOCAL CLIENT-SIDE SIMULATION]
        </span>
      </div>

      {/* Sensor Input Slider */}
      <div
        style={{
          background: '#121212',
          border: '1px solid var(--kernel-border-dark)',
          borderRadius: 4,
          padding: 16,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontFamily: 'var(--font-mono)', fontSize: 11 }}>
          <span style={{ color: 'var(--kernel-text-dark-muted)' }}>TELEMETRY STREAM: CORE TEMP</span>
          <span style={{ color: isAnomaly ? '#EF4444' : 'var(--lime-accent)', fontWeight: 700 }}>
            {sensorValue}°C / CRITICAL: {threshold}°C
          </span>
        </div>

        <input
          type="range"
          min="20"
          max="95"
          value={sensorValue}
          onChange={(e) => setSensorValue(Number(e.target.value))}
          style={{
            width: '100%',
            accentColor: isAnomaly ? '#EF4444' : 'var(--lime-accent)',
          }}
          aria-label="Sensor temperature slider"
        />

        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, fontFamily: 'var(--font-mono)', fontSize: 9, color: '#555555' }}>
          <span>20°C (AMBIENT)</span>
          <span>65°C (ANOMALY THRESHOLD)</span>
          <span>95°C (MAX)</span>
        </div>
      </div>

      {/* Inference & Classification Result */}
      <div
        style={{
          background: isAnomaly ? 'rgba(239, 68, 68, 0.08)' : 'rgba(163, 230, 53, 0.05)',
          border: `1px solid ${isAnomaly ? 'rgba(239, 68, 68, 0.4)' : 'var(--lime-accent-border)'}`,
          borderRadius: 4,
          padding: 18,
          display: 'flex',
          alignItems: 'center',
          gap: 16,
        }}
      >
        <div
          style={{
            width: 40,
            height: 40,
            borderRadius: '50%',
            background: isAnomaly ? '#EF4444' : 'var(--lime-accent)',
            color: '#000000',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          {isAnomaly ? <AlertTriangle size={20} /> : <CheckCircle size={20} />}
        </div>

        <div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: isAnomaly ? '#F87171' : 'var(--lime-accent)' }}>
            DECISION BOUNDARY: {isAnomaly ? 'CLASS 1 // CRITICAL TRIGGER' : 'CLASS 0 // STABLE NOMINAL'}
          </div>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#FFFFFF', marginTop: 2 }}>
            {isAnomaly
              ? 'Edge node flagged automated failover event.'
              : 'Sensor readings operating within safe thermal equilibrium.'}
          </div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)', marginTop: 4 }}>
            Inference Latency: &lt;1.2ms on embedded ARM Cortex simulation.
          </div>
        </div>
      </div>
    </div>
  )
}
export default AiIotDemo
