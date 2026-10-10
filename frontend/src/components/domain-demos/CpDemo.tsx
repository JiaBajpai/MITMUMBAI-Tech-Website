import React, { useState } from 'react'
import { StepForward, RotateCcw } from 'lucide-react'

export const CpDemo: React.FC = () => {
  const array = [2, 7, 14, 23, 31, 42, 59, 78, 91]
  const target = 42

  // Pre-calculated steps for Binary Search of 42
  const steps = [
    {
      stepNum: 1,
      low: 0,
      high: 8,
      mid: 4,
      desc: 'Initial range [0..8]. Mid is index 4 (value 31). Since 31 < 42, target lies to the right. Discard left half.',
      found: false,
    },
    {
      stepNum: 2,
      low: 5,
      high: 8,
      mid: 6,
      desc: 'Range narrowed to [5..8]. Mid is index 6 (value 59). Since 59 > 42, target lies to the left. Discard right half.',
      found: false,
    },
    {
      stepNum: 3,
      low: 5,
      high: 5,
      mid: 5,
      desc: 'Range narrowed to [5..5]. Mid is index 5 (value 42). Target found in 3 comparisons! O(log N) efficiency.',
      found: true,
    },
  ]

  const [currentStepIdx, setCurrentStepIdx] = useState<number>(0)
  const currentStep = steps[currentStepIdx]

  const handleNext = () => {
    if (currentStepIdx < steps.length - 1) {
      setCurrentStepIdx(currentStepIdx + 1)
    }
  }

  const handleReset = () => {
    setCurrentStepIdx(0)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--lime-accent)' }}>
          BINARY SEARCH VISUALIZER (TARGET: {target})
        </span>
        <div style={{ display: 'flex', gap: 6 }}>
          <button
            onClick={handleNext}
            disabled={currentStep.found}
            className="btn-editorial-primary"
            style={{ padding: '4px 10px', fontSize: 10 }}
          >
            <StepForward size={11} />
            <span>{currentStep.found ? 'MATCH FOUND' : 'NEXT STEP'}</span>
          </button>
          <button
            onClick={handleReset}
            className="btn-editorial-secondary"
            style={{ padding: '4px 8px', fontSize: 10 }}
            title="Reset simulation"
          >
            <RotateCcw size={11} />
          </button>
        </div>
      </div>

      {/* Array Elements Display */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: `repeat(${array.length}, 1fr)`,
          gap: 6,
          padding: '16px 8px',
          background: '#121212',
          border: '1px solid var(--kernel-border-dark)',
          borderRadius: 4,
        }}
      >
        {array.map((val, idx) => {
          const isMid = idx === currentStep.mid
          const inRange = idx >= currentStep.low && idx <= currentStep.high
          const isFound = isMid && currentStep.found

          return (
            <div
              key={idx}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <div
                style={{
                  width: '100%',
                  aspectRatio: '1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: isFound
                    ? 'var(--lime-accent)'
                    : isMid
                    ? '#2A2A2A'
                    : inRange
                    ? '#1A1A1A'
                    : '#0A0A0A',
                  color: isFound ? '#000000' : inRange ? '#FFFFFF' : '#444444',
                  border: `1px solid ${
                    isFound
                      ? 'var(--lime-accent)'
                      : isMid
                      ? 'var(--lime-accent)'
                      : inRange
                      ? 'var(--kernel-border-dark)'
                      : '#1F1F1F'
                  }`,
                  borderRadius: 3,
                  fontFamily: 'var(--font-mono)',
                  fontSize: 12,
                  fontWeight: isMid || isFound ? 700 : 500,
                  transition: 'all 0.2s ease',
                }}
              >
                {val}
              </div>
              <div
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 8,
                  color: isMid ? 'var(--lime-accent)' : '#555555',
                }}
              >
                {isMid ? 'MID' : idx === currentStep.low ? 'L' : idx === currentStep.high ? 'R' : `[${idx}]`}
              </div>
            </div>
          )
        })}
      </div>

      {/* Step Explanation */}
      <div
        style={{
          background: '#0D0D0D',
          border: '1px solid var(--kernel-border-dark)',
          borderRadius: 4,
          padding: 14,
          fontFamily: 'var(--font-mono)',
          fontSize: 11,
          lineHeight: 1.5,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
          <span style={{ color: 'var(--lime-accent)' }}>
            ITERATION {currentStep.stepNum} OF 3
          </span>
          <span style={{ color: 'var(--kernel-text-dark-muted)' }}>
            TIME COMPLEXITY: O(log N)
          </span>
        </div>
        <p style={{ margin: 0, color: currentStep.found ? 'var(--lime-accent)' : '#C4C4C4' }}>
          {currentStep.desc}
        </p>
      </div>
    </div>
  )
}
export default CpDemo
