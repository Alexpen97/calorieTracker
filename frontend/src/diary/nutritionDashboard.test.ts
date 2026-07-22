import { describe, expect, it } from 'vitest'
import {
  buildMacroSummaries,
  buildMicronutrientRows,
  buildWeightTrend,
  dateDaysAgo,
} from './nutritionDashboard'
import type { NutrientTotalForDisplay } from './formatDay'
import type { WeightLog } from '../api/client'

describe('nutrition dashboard helpers', () => {
  const totals: NutrientTotalForDisplay[] = [
    { code: 'protein', amount: 80, unit: 'g', target: 100 },
    { code: 'carbohydrates', amount: 220, unit: 'g', target: 250 },
    { code: 'fat', amount: 55, unit: 'g', target: 70 },
    { code: 'vitamin_d', amount: 6, unit: 'ug', target: 15 },
    { code: 'calcium', amount: 650, unit: 'mg', target: 1000 },
  ]

  it('builds macro summaries in display order', () => {
    expect(buildMacroSummaries(totals).map((item) => item.label)).toEqual(['Protein', 'Carbs', 'Fat'])
    expect(buildMacroSummaries(totals)[0]).toMatchObject({
      code: 'protein',
      percent: 80,
      amountLabel: '80 / 100 g',
    })
  })

  it('builds vitamin and mineral progress rows', () => {
    expect(buildMicronutrientRows(totals, 'vitamin')).toEqual([
      { code: 'vitamin_d', label: 'Vitamin D', percent: 40, amountLabel: '6 / 15 ug' },
    ])
    expect(buildMicronutrientRows(totals, 'mineral')[0]).toMatchObject({
      label: 'Calcium',
      percent: 65,
    })
  })

  it('builds oldest-to-newest weight trend points', () => {
    const weights: WeightLog[] = [
      { id: '2', weightKg: 71.8, measuredAt: '2026-07-22T08:00:00Z' },
      { id: '1', weightKg: 72.3, measuredAt: '2026-07-20T08:00:00Z' },
    ]

    expect(buildWeightTrend(weights)).toEqual([72.3, 71.8])
  })

  it('formats dates relative to a provided clock', () => {
    expect(dateDaysAgo(6, new Date(2026, 6, 22))).toBe('2026-07-16')
  })
})
