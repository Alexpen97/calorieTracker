import { describe, expect, it } from 'vitest'
import {
  formatLocalDate,
  getMacroProgress,
  groupEntriesByMeal,
  waterProgress,
  type DiaryEntryForDisplay,
  type NutrientTotalForDisplay,
} from './formatDay'

describe('diary day formatting helpers', () => {
  it('formats a Date as a local YYYY-MM-DD value', () => {
    expect(formatLocalDate(new Date(2026, 6, 21, 23, 30))).toBe('2026-07-21')
  })

  it('groups entries by the fixed meal order', () => {
    const entries: DiaryEntryForDisplay[] = [
      entry('2', 'Dinner soup', 'DINNER'),
      entry('1', 'Morning oats', 'BREAKFAST'),
      entry('3', 'Afternoon pear', 'SNACK'),
    ]

    expect(groupEntriesByMeal(entries)).toEqual([
      { mealType: 'BREAKFAST', entries: [entries[1]] },
      { mealType: 'LUNCH', entries: [] },
      { mealType: 'DINNER', entries: [entries[0]] },
      { mealType: 'SNACK', entries: [entries[2]] },
    ])
  })

  it('builds bounded nutrient and water progress values', () => {
    const totals: NutrientTotalForDisplay[] = [
      { code: 'protein', amount: 75, unit: 'g', target: 100 },
      { code: 'energy_kcal', amount: 2500, unit: 'kcal', target: 2000 },
    ]

    expect(getMacroProgress(totals, 'protein')).toEqual({
      code: 'protein',
      amount: 75,
      unit: 'g',
      target: 100,
      percent: 75,
    })
    expect(getMacroProgress(totals, 'energy_kcal')?.percent).toBe(100)
    expect(getMacroProgress(totals, 'fat')).toBeNull()
    expect(waterProgress({ amountMl: 1250, targetMl: 2500 })).toEqual({
      amountMl: 1250,
      targetMl: 2500,
      percent: 50,
    })
  })
})

function entry(id: string, productName: string, mealType: DiaryEntryForDisplay['mealType']) {
  return {
    id,
    productId: id,
    productName,
    brand: null,
    weightG: 100,
    mealType,
    consumedAt: '2026-07-21T12:00:00Z',
    createdAt: '2026-07-21T12:00:00Z',
    nutrients: [],
  }
}
