import type { ReactNode } from 'react'

type CardTone = 'green' | 'amber' | 'purple' | 'blue'

export function DashboardCard({
  eyebrow,
  title,
  action,
  children,
  className = '',
}: {
  eyebrow?: string
  title: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`dashboard-card ${className}`.trim()}>
      <div className="card-heading">
        <div>
          {eyebrow && <p className="card-eyebrow">{eyebrow}</p>}
          <h2>{title}</h2>
        </div>
        {action}
      </div>
      {children}
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
