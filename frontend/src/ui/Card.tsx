import type { ReactNode } from 'react'

type CardTone = 'green' | 'amber' | 'purple' | 'blue'
export type CardDensity = 'default' | 'hero' | 'metric' | 'list' | 'insight'
export type MetricTone = 'protein' | 'carbs' | 'fat' | 'calories' | 'neutral'

export function DashboardCard({
  icon,
  eyebrow,
  title,
  action,
  children,
  className = '',
  density = 'default',
}: {
  icon?: ReactNode
  eyebrow?: string
  title: string
  action?: ReactNode
  children: ReactNode
  className?: string
  density?: CardDensity
}) {
  const densityClass = density === 'default' ? '' : `dashboard-card-${density}`
  return (
    <section className={`dashboard-card ${densityClass} ${className}`.trim()}>
      <div className="card-heading">
        <div className="card-heading-main">
          {icon && <span className="card-icon" aria-hidden>{icon}</span>}
          <div>
          {eyebrow && <p className="card-eyebrow">{eyebrow}</p>}
          <h2>{title}</h2>
          </div>
        </div>
        {action}
      </div>
      {children}
    </section>
  )
}

export function MetricCard({
  label,
  value,
  tone = 'neutral',
  progress,
  icon,
  className = '',
}: {
  label: string
  value: string
  tone?: MetricTone
  progress?: number
  icon?: ReactNode
  className?: string
}) {
  const bounded =
    progress == null || !Number.isFinite(progress)
      ? null
      : Math.max(0, Math.min(100, Math.round(progress)))

  return (
    <section className={`metric-card metric-card-${tone} ${className}`.trim()}>
      <div className="metric-card-copy">
        <strong className="metric-card-value">{value}</strong>
        <span className="metric-card-label">{label}</span>
      </div>
      {icon ? <span className="metric-card-icon" aria-hidden>{icon}</span> : null}
      {bounded != null ? (
        <div
          className="metric-card-track"
          role="progressbar"
          aria-label={label}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={bounded}
        >
          <div className="metric-card-fill" style={{ width: `${bounded}%` }} />
        </div>
      ) : null}
    </section>
  )
}

export function MetricPill({
  label,
  value,
  tone,
}: {
  label: string
  value: string
  tone: CardTone
}) {
  return (
    <div className={`metric-pill metric-pill-${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

export function EmptyCard({ title, copy }: { title: string; copy: string }) {
  return (
    <section className="dashboard-card empty-card">
      <h2>{title}</h2>
      <p>{copy}</p>
    </section>
  )
}
