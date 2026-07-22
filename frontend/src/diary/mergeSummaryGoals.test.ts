import { describe, expect, it } from 'vitest'
import type { DaySummary, Goal } from '../api/client'
import { mergeSummaryWithGoals } from './mergeSummaryGoals'

const goals: Goal[] = [
  {
    nutrientCode: 'energy_kcal',
    dailyTarget: 2100,
    unit: 'kcal',
    origin: 'COMPUTED',
    computedAt: '2026-07-21T00:00:00Z',
  },
  {
    nutrientCode: 'protein',
    dailyTarget: 100,
    unit: 'g',
    origin: 'COMPUTED',
    computedAt: '2026-07-21T00:00:00Z',
  },
  {
    nutrientCode: 'carbohydrates',
    dailyTarget: 250,
    unit: 'g',
    origin: 'COMPUTED',
    computedAt: '2026-07-21T00:00:00Z',
  },
  {
    nutrientCode: 'fat',
    dailyTarget: 70,
    unit: 'g',
    origin: 'COMPUTED',
    computedAt: '2026-07-21T00:00:00Z',
  },
  {
    nutrientCode: 'water_ml',
    dailyTarget: 2600,
    unit: 'ml',
    origin: 'COMPUTED',
    computedAt: '2026-07-21T00:00:00Z',
  },
]

describe('mergeSummaryWithGoals', () => {
  it('fills null targets from goals and seeds missing nutrient rows', () => {
    const summary: DaySummary = {
      date: '2026-07-22',
      totals: [
        { code: 'energy_kcal', amount: 2333, unit: 'kcal', target: null },
        { code: 'protein', amount: 233, unit: 'g', target: null },
        { code: 'carbohydrates', amount: 23, unit: 'g', target: null },
        { code: 'fat', amount: 223, unit: 'g', target: null },
      ],
      water: { amountMl: 0, targetMl: null },
    }

    expect(mergeSummaryWithGoals(summary, goals)).toEqual({
      date: '2026-07-22',
      totals: [
        { code: 'carbohydrates', amount: 23, unit: 'g', target: 250 },
        { code: 'energy_kcal', amount: 2333, unit: 'kcal', target: 2100 },
        { code: 'fat', amount: 223, unit: 'g', target: 70 },
        { code: 'protein', amount: 233, unit: 'g', target: 100 },
      ],
      water: { amountMl: 0, targetMl: 2600 },
    })
  })

  it('keeps summary targets when goals are missing', () => {
    const summary: DaySummary = {
      date: '2026-07-22',
      totals: [{ code: 'energy_kcal', amount: 100, unit: 'kcal', target: 1800 }],
      water: { amountMl: 500, targetMl: 2000 },
    }

    expect(mergeSummaryWithGoals(summary, undefined)).toEqual(summary)
    expect(mergeSummaryWithGoals(summary, [])).toEqual(summary)
  })
})
