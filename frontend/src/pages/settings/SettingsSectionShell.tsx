import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'
import type { Objective } from '../../api/client'

export const sexOptions = [
  { value: 'MALE' as const, label: 'Male' },
  { value: 'FEMALE' as const, label: 'Female' },
]

export const activityOptions = [
  { value: 'SEDENTARY' as const, label: 'Sedentary' },
  { value: 'LIGHT' as const, label: 'Light' },
  { value: 'MODERATE' as const, label: 'Moderate' },
  { value: 'ACTIVE' as const, label: 'Active' },
  { value: 'VERY_ACTIVE' as const, label: 'Very active' },
]

export const objectiveOptions: Array<{ value: Objective; label: string }> = [
  { value: 'CUT', label: 'Cut' },
  { value: 'LOSE', label: 'Lose weight' },
  { value: 'MAINTAIN', label: 'Maintain' },
  { value: 'GAIN', label: 'Gentle gain' },
  { value: 'MUSCLE_GAIN', label: 'Lean muscle' },
  { value: 'BULK', label: 'Bulk' },
]

export function objectiveLabel(objective: Objective | null | undefined): string {
  if (!objective) {
    return 'No objective set'
  }
  return objectiveOptions.find((option) => option.value === objective)?.label ?? objective
}

export function SettingsSectionShell({
  title,
  description,
  children,
}: {
  title: string
  description: string
  children: ReactNode
}) {
  return (
    <main className="mobile-page settings-page">
      <div className="diary-header settings-section-header">
        <div>
          <Link className="settings-back-link" to="/settings">
            Back to settings
          </Link>
          <p className="sheet-kicker">Settings</p>
          <h2>{title}</h2>
          <p className="product-meta">{description}</p>
        </div>
      </div>
      {children}
    </main>
  )
}
