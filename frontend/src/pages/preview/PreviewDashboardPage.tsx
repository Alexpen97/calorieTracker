import type { DaySummary, WeightLog } from '../../api/client'
import DashboardView from '../../screens/DashboardView'

export default function PreviewDashboardPage() {
  const summary: DaySummary = {
    date: '2026-07-22',
    totals: [
      { code: 'energy_kcal', amount: 1280, unit: 'kcal', target: 1840 },
      { code: 'protein', amount: 102, unit: 'g', target: 120 },
      { code: 'carbohydrates', amount: 143, unit: 'g', target: 180 },
      { code: 'fat', amount: 49, unit: 'g', target: 60 },
      { code: 'vitamin_a', amount: 820, unit: 'ug', target: 1000 },
      { code: 'vitamin_c', amount: 92, unit: 'mg', target: 80 },
      { code: 'vitamin_d', amount: 10, unit: 'ug', target: 15 },
      { code: 'vitamin_b12', amount: 2.1, unit: 'ug', target: 2.4 },
      { code: 'iron', amount: 7.8, unit: 'mg', target: 10 },
      { code: 'calcium', amount: 900, unit: 'mg', target: 1000 },
      { code: 'magnesium', amount: 240, unit: 'mg', target: 400 },
      { code: 'potassium', amount: 2800, unit: 'mg', target: 3400 },
    ],
    water: { amountMl: 1600, targetMl: 2500 },
  }

  const weights: WeightLog[] = [
    { id: 'w1', weightKg: 73.2, measuredAt: '2026-07-10T08:00:00Z' },
    { id: 'w2', weightKg: 72.9, measuredAt: '2026-07-12T08:00:00Z' },
    { id: 'w3', weightKg: 72.7, measuredAt: '2026-07-14T08:00:00Z' },
    { id: 'w4', weightKg: 72.4, measuredAt: '2026-07-16T08:00:00Z' },
    { id: 'w5', weightKg: 72.2, measuredAt: '2026-07-18T08:00:00Z' },
    { id: 'w6', weightKg: 72.0, measuredAt: '2026-07-20T08:00:00Z' },
    { id: 'w7', weightKg: 71.8, measuredAt: '2026-07-22T08:00:00Z' },
  ]

  return (
    <DashboardView
      me={{ displayName: 'Alex', avatarUrl: null }}
      summary={summary}
      weightHistory={weights}
      selectedDate={summary.date}
      onSelectDate={() => undefined}
    />
  )
}
