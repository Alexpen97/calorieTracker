import type { CSSProperties } from 'react'

type Segment = { label: string; percent: number }
type ChartPoint = number

function clampPercent(value: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

export function ProgressRing({
  label,
  percent,
  value,
}: {
  label: string
  percent: number
  value: string
}) {
  const bounded = clampPercent(percent)
  return (
    <div
      className="progress-ring"
      aria-label={`${label}: ${bounded}%`}
      style={{ '--progress': `${bounded}%` } as CSSProperties}
    >
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  )
}

export function ProgressRow({
  label,
  percent,
  amountLabel,
}: {
  label: string
  percent: number
  amountLabel: string
}) {
  const bounded = clampPercent(percent)
  return (
    <div className="progress-row">
      <span>{label}</span>
      <div className="mini-track" aria-hidden>
        <div className="mini-fill" style={{ width: `${bounded}%` }} />
      </div>
      <strong>{amountLabel}</strong>
    </div>
  )
}

export function Sparkline({ label, points }: { label: string; points: ChartPoint[] }) {
  const path = sparklinePath(points)
  return (
    <svg className="sparkline" viewBox="0 0 100 36" role="img" aria-label={label}>
      <path d={path} />
    </svg>
  )
}

export function StackedBar({ label, segments }: { label: string; segments: Segment[] }) {
  return (
    <div className="stacked-bar">
      <p>{label}</p>
      <div className="stacked-track" aria-hidden>
        {segments.map((segment) => (
          <span key={segment.label} style={{ width: `${clampPercent(segment.percent)}%` }} />
        ))}
      </div>
      <div className="stacked-legend">
        {segments.map((segment) => (
          <span key={segment.label}>{segment.label}</span>
        ))}
      </div>
    </div>
  )
}

export function GroupedBars({ label, groups }: { label: string; groups: Segment[] }) {
  return (
    <div className="grouped-bars" aria-label={label}>
      {groups.map((group) => (
        <ProgressRow
          key={group.label}
          label={group.label}
          percent={group.percent}
          amountLabel={`${clampPercent(group.percent)}%`}
        />
      ))}
    </div>
  )
}

function sparklinePath(points: ChartPoint[]): string {
  if (points.length === 0) return ''
  const min = Math.min(...points)
  const max = Math.max(...points)
  const spread = max - min || 1
  return points
    .map((point, index) => {
      const x = points.length === 1 ? 50 : (index / (points.length - 1)) * 100
      const y = 32 - ((point - min) / spread) * 28
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}
