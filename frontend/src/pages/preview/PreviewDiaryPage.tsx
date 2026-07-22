import type { DaySummary, DiaryEntry, WaterLog, WeightLog } from '../../api/client'
import DiaryView from '../../screens/DiaryView'

export default function PreviewDiaryPage() {
  const summary: DaySummary = {
    date: '2026-07-22',
    totals: [
      { code: 'energy_kcal', amount: 1650, unit: 'kcal', target: 2200 },
      { code: 'protein', amount: 92, unit: 'g', target: 120 },
      { code: 'carbohydrates', amount: 165, unit: 'g', target: 275 },
      { code: 'fat', amount: 55, unit: 'g', target: 73 },
      { code: 'vitamin_a', amount: 900, unit: 'ug', target: 1000 },
      { code: 'vitamin_c', amount: 65, unit: 'mg', target: 80 },
      { code: 'vitamin_d', amount: 9, unit: 'ug', target: 15 },
      { code: 'vitamin_b12', amount: 2.0, unit: 'ug', target: 2.4 },
      { code: 'iron', amount: 10, unit: 'mg', target: 10 },
      { code: 'calcium', amount: 750, unit: 'mg', target: 1000 },
      { code: 'magnesium', amount: 240, unit: 'mg', target: 400 },
      { code: 'zinc', amount: 6, unit: 'mg', target: 10 },
    ],
    water: { amountMl: 1250, targetMl: 2500 },
  }

  const entries: DiaryEntry[] = [
    {
      id: 'e1',
      productId: 'p1',
      submissionId: null,
      productName: 'Greek Yogurt with Berries',
      brand: 'Kitchen',
      weightG: 220,
      mealType: 'BREAKFAST',
      consumedAt: '2026-07-22T08:00:00Z',
      createdAt: '2026-07-22T08:00:00Z',
      nutrients: [
        { code: 'energy_kcal', amount: 425, amountPer100g: 193, unit: 'kcal' },
        { code: 'protein', amount: 28, amountPer100g: 12.7, unit: 'g' },
        { code: 'carbohydrates', amount: 45, amountPer100g: 20.4, unit: 'g' },
        { code: 'fat', amount: 12, amountPer100g: 5.4, unit: 'g' },
      ],
    },
    {
      id: 'e2',
      productId: 'p2',
      submissionId: null,
      productName: 'Grilled Chicken Bowl',
      brand: 'Kitchen',
      weightG: 360,
      mealType: 'LUNCH',
      consumedAt: '2026-07-22T12:00:00Z',
      createdAt: '2026-07-22T12:00:00Z',
      nutrients: [
        { code: 'energy_kcal', amount: 585, amountPer100g: 163, unit: 'kcal' },
        { code: 'protein', amount: 38, amountPer100g: 10.6, unit: 'g' },
        { code: 'carbohydrates', amount: 65, amountPer100g: 18.1, unit: 'g' },
        { code: 'fat', amount: 18, amountPer100g: 5.0, unit: 'g' },
      ],
    },
  ]

  const waterLogs: WaterLog[] = [
    { id: 'w1', amountMl: 250, loggedAt: '2026-07-22T09:00:00Z' },
    { id: 'w2', amountMl: 500, loggedAt: '2026-07-22T11:00:00Z' },
    { id: 'w3', amountMl: 500, loggedAt: '2026-07-22T14:00:00Z' },
  ]

  const weights: WeightLog[] = [
    { id: 'ww1', weightKg: 75.0, measuredAt: '2026-07-14T08:00:00Z' },
    { id: 'ww2', weightKg: 74.2, measuredAt: '2026-07-22T08:00:00Z' },
  ]

  return (
    <DiaryView
      dateLabel="Today, May 24"
      summary={summary}
      entries={entries}
      waterLogs={waterLogs}
      weightHistory={weights}
      onDeleteEntry={() => {}}
    />
  )
}

