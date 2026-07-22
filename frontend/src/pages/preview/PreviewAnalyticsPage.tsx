import type { DaySummary, WeightLog } from '../../api/client'
import AnalyticsView from '../../screens/AnalyticsView'

export default function PreviewAnalyticsPage() {
  const summaries: DaySummary[] = [
    {
      date: '2026-07-16',
      totals: [
        { code: 'protein', amount: 95, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 210, unit: 'g', target: 250 },
        { code: 'fat', amount: 55, unit: 'g', target: 70 },
        { code: 'vitamin_d', amount: 5, unit: 'ug', target: 15 },
        { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
      ],
      water: { amountMl: 1800, targetMl: 2500 },
    },
  ]

  const weights: WeightLog[] = [
    { id: 'w1', weightKg: 72.3, measuredAt: '2026-07-16T08:00:00Z' },
    { id: 'w2', weightKg: 71.9, measuredAt: '2026-07-22T08:00:00Z' },
  ]

  return <AnalyticsView from="2026-07-16" to="2026-07-22" summaries={summaries} weightHistory={weights} />
}

