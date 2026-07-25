import type { DaySummary, WeightLog } from '../../api/client'
import AnalyticsView from '../../screens/AnalyticsView'

export default function PreviewAnalyticsPage() {
  const summaries: DaySummary[] = Array.from({ length: 30 }, (_, index) => {
    const date = new Date(2026, 5, 23)
    date.setDate(date.getDate() + index)
    const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    const wave = Math.round(40 + 35 * Math.sin(index / 4))
    return {
      date: iso,
      totals: [
        { code: 'protein', amount: 95, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 210, unit: 'g', target: 250 },
        { code: 'fat', amount: 55, unit: 'g', target: 70 },
        { code: 'vitamin_d', amount: Math.max(0, (wave / 100) * 15), unit: 'ug', target: 15 },
        { code: 'vitamin_c', amount: Math.max(0, ((wave + 20) / 100) * 80), unit: 'mg', target: 80 },
        { code: 'calcium', amount: Math.max(0, ((wave + 10) / 100) * 1000), unit: 'mg', target: 1000 },
        { code: 'iron', amount: Math.max(0, ((wave - 10) / 100) * 14), unit: 'mg', target: 14 },
      ],
      water: { amountMl: 1800, targetMl: 2500 },
    }
  })

  const weights: WeightLog[] = [
    { id: 'w1', weightKg: 72.3, measuredAt: '2026-06-23T08:00:00Z' },
    { id: 'w2', weightKg: 71.9, measuredAt: '2026-07-22T08:00:00Z' },
  ]

  return <AnalyticsView from="2026-06-23" to="2026-07-22" summaries={summaries} weightHistory={weights} />
}
