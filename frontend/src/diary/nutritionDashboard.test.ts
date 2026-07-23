import { describe, expect, it } from 'vitest'
import {
  averageMicronutrientRows,
  buildMacroSummaries,
  buildMicronutrientRows,
  buildWeightTrend,
  buildWeightTrendAxisLabels,
  buildWeightTrendSeries,
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

  it('always returns protein, carbs, and fat rows even when totals are empty', () => {
    expect(buildMacroSummaries([])).toEqual([
      { code: 'protein', label: 'Protein', percent: 0, amountLabel: '0 g' },
      { code: 'carbohydrates', label: 'Carbs', percent: 0, amountLabel: '0 g' },
      { code: 'fat', label: 'Fat', percent: 0, amountLabel: '0 g' },
    ])
  })

  it('shows zero consumed against goals when macros are present at amount 0', () => {
    const emptyWithGoals: NutrientTotalForDisplay[] = [
      { code: 'protein', amount: 0, unit: 'g', target: 100 },
      { code: 'carbohydrates', amount: 0, unit: 'g', target: 250 },
      { code: 'fat', amount: 0, unit: 'g', target: 70 },
    ]
    expect(buildMacroSummaries(emptyWithGoals)).toEqual([
      { code: 'protein', label: 'Protein', percent: 0, amountLabel: '0 / 100 g' },
      { code: 'carbohydrates', label: 'Carbs', percent: 0, amountLabel: '0 / 250 g' },
      { code: 'fat', label: 'Fat', percent: 0, amountLabel: '0 / 70 g' },
    ])
  })

  it('builds vitamin and mineral progress rows for the full checklist', () => {
    const vitamins = buildMicronutrientRows(totals, 'vitamin')
    const minerals = buildMicronutrientRows(totals, 'mineral')

    expect(vitamins).toHaveLength(13)
    expect(minerals).toHaveLength(13)
    expect(vitamins.map((item) => item.code)).toEqual([
      'vitamin_a',
      'vitamin_b1',
      'vitamin_b2',
      'vitamin_b3',
      'vitamin_b5',
      'vitamin_b6',
      'vitamin_b7',
      'vitamin_b9',
      'vitamin_b12',
      'vitamin_c',
      'vitamin_d',
      'vitamin_e',
      'vitamin_k',
    ])
    expect(vitamins.find((item) => item.code === 'vitamin_d')).toEqual({
      code: 'vitamin_d',
      label: 'Vitamin D',
      percent: 40,
      amountLabel: '6 / 15 ug',
    })
    expect(minerals.find((item) => item.code === 'calcium')).toMatchObject({
      label: 'Calcium',
      percent: 65,
    })
    expect(minerals.find((item) => item.code === 'zinc')).toMatchObject({
      label: 'Zinc',
      percent: 0,
      amountLabel: '0',
    })
  })

  it('averages micronutrient progress across multiple day summaries', () => {
    const days: NutrientTotalForDisplay[][] = [
      [
        { code: 'vitamin_d', amount: 6, unit: 'ug', target: 15 },
        { code: 'calcium', amount: 500, unit: 'mg', target: 1000 },
      ],
      [
        { code: 'vitamin_d', amount: 9, unit: 'ug', target: 15 },
        { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
      ],
    ]

    const vitamins = averageMicronutrientRows(
      days.map((dayTotals) => ({ totals: dayTotals })),
      'vitamin',
    )
    const minerals = averageMicronutrientRows(
      days.map((dayTotals) => ({ totals: dayTotals })),
      'mineral',
    )

    expect(vitamins.find((item) => item.code === 'vitamin_d')).toMatchObject({
      percent: 50,
      amountLabel: '7.5 / 15 ug',
    })
    expect(minerals.find((item) => item.code === 'calcium')).toMatchObject({
      percent: 60,
      amountLabel: '600 / 1,000 mg',
    })
    expect(vitamins).toHaveLength(13)
    expect(minerals).toHaveLength(13)
  })

  it('builds oldest-to-newest weight trend points for the last 30 days', () => {
    const clock = new Date(2026, 6, 22)
    const weights: WeightLog[] = [
      { id: 'old', weightKg: 80, measuredAt: '2026-06-01T08:00:00Z' },
      { id: '2', weightKg: 71.8, measuredAt: '2026-07-22T08:00:00Z' },
      { id: '1', weightKg: 72.3, measuredAt: '2026-07-20T08:00:00Z' },
    ]

    expect(buildWeightTrend(weights, { clock })).toEqual([72.3, 71.8])
  })

  it('places weight trend points on a 30-day timeline by measuredAt', () => {
    const clock = new Date(2026, 6, 22, 12, 0, 0)
    const weights: WeightLog[] = [
      { id: 'old', weightKg: 80, measuredAt: '2026-06-01T08:00:00Z' },
      { id: 'mid', weightKg: 72.3, measuredAt: '2026-07-07T08:00:00Z' },
      { id: 'latest', weightKg: 71.8, measuredAt: '2026-07-22T08:00:00Z' },
    ]

    const series = buildWeightTrendSeries(weights, { days: 30, clock })
    expect(series).toHaveLength(2)
    expect(series[0]).toMatchObject({ weightKg: 72.3 })
    expect(series[1]).toMatchObject({ weightKg: 71.8 })
    expect(series[0].t).toBeGreaterThan(0)
    expect(series[0].t).toBeLessThan(series[1].t)
    expect(series[1].t).toBeGreaterThan(0.9)
  })

  it('formats dates relative to a provided clock', () => {
    expect(dateDaysAgo(6, new Date(2026, 6, 22))).toBe('2026-07-16')
  })

  it('builds start/mid/end axis labels for the weight window', () => {
    const labels = buildWeightTrendAxisLabels({ days: 30, clock: new Date(2026, 6, 22, 12, 0, 0) })
    expect(labels).toHaveLength(3)
    expect(labels[0]).toMatch(/23/)
    expect(labels[0]).toMatch(/Jun/i)
    expect(labels[2]).toMatch(/22/)
    expect(labels[2]).toMatch(/Jul/i)
  })
})
