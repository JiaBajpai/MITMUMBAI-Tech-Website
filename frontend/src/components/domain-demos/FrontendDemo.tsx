import React, { useState } from 'react'

export const FrontendDemo: React.FC = () => {
  const [accentColor, setAccentColor] = useState<string>('#A3E635')
  const [borderRadius, setBorderRadius] = useState<number>(6)
  const padding = 16
  const [mode, setMode] = useState<'dark' | 'light'>('dark')

  const isDark = mode === 'dark'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--lime-accent)' }}>
          LIVE COMPONENT CUSTOMIZER
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)' }}>
          CLIENT-SIDE STATE DEMO
        </span>
      </div>

      {/* Control Panel */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 12,
          padding: 12,
          background: '#121212',
          border: '1px solid var(--kernel-border-dark)',
          borderRadius: 4,
        }}
      >
        {/* Accent Selector */}
        <div>
          <label style={{ display: 'block', fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)', marginBottom: 6 }}>
            ACCENT THEME
          </label>
          <div style={{ display: 'flex', gap: 6 }}>
            {['#A3E635', '#38BDF8', '#F472B6', '#FBBF24'].map((color) => (
              <button
                key={color}
                onClick={() => setAccentColor(color)}
                style={{
                  width: 22,
                  height: 22,
                  borderRadius: 3,
                  backgroundColor: color,
                  border: accentColor === color ? '2px solid #FFFFFF' : '1px solid rgba(255,255,255,0.2)',
                  cursor: 'pointer',
                }}
                aria-label={`Select accent ${color}`}
              />
            ))}
          </div>
        </div>

        {/* Radius Slider */}
        <div>
          <label style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)', marginBottom: 6 }}>
            <span>RADIUS</span>
            <span>{borderRadius}px</span>
          </label>
          <input
            type="range"
            min="0"
            max="20"
            value={borderRadius}
            onChange={(e) => setBorderRadius(Number(e.target.value))}
            style={{ width: '100%', accentColor }}
          />
        </div>

        {/* Surface Theme Toggle */}
        <div>
          <label style={{ display: 'block', fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--kernel-text-dark-muted)', marginBottom: 6 }}>
            SURFACE
          </label>
          <button
            onClick={() => setMode(isDark ? 'light' : 'dark')}
            style={{
              padding: '4px 10px',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              background: '#1F1F1F',
              border: '1px solid var(--kernel-border-dark)',
              color: '#FFFFFF',
              borderRadius: 3,
              cursor: 'pointer',
              width: '100%',
            }}
          >
            {isDark ? '🌙 DARK SURFACE' : '☀️ LIGHT SURFACE'}
          </button>
        </div>
      </div>

      {/* Live Preview Card */}
      <div
        style={{
          backgroundColor: isDark ? '#141414' : '#FAFAFA',
          color: isDark ? '#F5F5F3' : '#111111',
          border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'}`,
          borderRadius: `${borderRadius}px`,
          padding: `${padding}px`,
          transition: 'all 0.2s ease',
          boxShadow: isDark ? '0 10px 30px rgba(0,0,0,0.5)' : '0 10px 30px rgba(0,0,0,0.06)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span
              style={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                backgroundColor: accentColor,
                display: 'inline-block',
              }}
            />
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, fontWeight: 600 }}>
              KERNEL UI PRIMITIVE
            </span>
          </div>
          <span
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              padding: '2px 6px',
              borderRadius: 2,
              background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)',
            }}
          >
            LIVE PREVIEW
          </span>
        </div>

        <p style={{ margin: '0 0 14px 0', fontSize: 13, lineHeight: 1.5, opacity: 0.85 }}>
          Components adhere to strict design token specifications with accessible contrast ratios
          and fluid responsiveness across mobile and desktop displays.
        </p>

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            style={{
              backgroundColor: accentColor,
              color: '#000000',
              border: 'none',
              borderRadius: `${borderRadius}px`,
              padding: '6px 14px',
              fontFamily: 'var(--font-mono)',
              fontSize: 11,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Action Button
          </button>
          <button
            style={{
              backgroundColor: 'transparent',
              color: 'inherit',
              border: `1px solid ${isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)'}`,
              borderRadius: `${borderRadius}px`,
              padding: '6px 14px',
              fontFamily: 'var(--font-mono)',
              fontSize: 11,
              cursor: 'pointer',
            }}
          >
            Secondary
          </button>
        </div>
      </div>
    </div>
  )
}
export default FrontendDemo
