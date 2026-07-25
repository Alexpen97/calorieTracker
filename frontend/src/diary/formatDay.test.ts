import { describe, expect, it } from 'vitest'
import {
  analyticsRangeFromEnd,
  formatAnalyticsRangeLabel,
  formatDiaryDayLabel,
  formatLocalDate,
  getMacroProgress,
  groupEntriesByMeal,
  mealLookupPath,
  parseMealTypeParam,
  productPathWithMeal,
  shiftAnalyticsRangeEnd,
  shiftLocalDate,
  waterProgress,
  type DiaryEntryForDisplay,
  type NutrientTotalForDisplay,
} from './formatDay'

describe('diary day formatting helpers', () => {
  it('formats a Date as a local YYYY-MM-DD value', () => {
    expect(formatLocalDate(new Date(2026, 6, 21, 23, 30))).toBe('2026-07-21')
  })

  it('shifts a local YYYY-MM-DD date by whole days', () => {
    expect(shiftLocalDate('2026-07-22', -1)).toBe('2026-07-21')
    expect(shiftLocalDate('2026-07-22', 1)).toBe('2026-07-23')
    expect(shiftLocalDate('2026-07-01', -1)).toBe('2026-06-30')
  })

  it('labels today, yesterday, tomorrow, and other days', () => {
    expect(formatDiaryDayLabel('2026-07-22', '2026-07-22')).toMatch(/^Today,/)
    expect(formatDiaryDayLabel('2026-07-21', '2026-07-22')).toMatch(/^Yesterday,/)
    expect(formatDiaryDayLabel('2026-07-23', '2026-07-22')).toMatch(/^Tomorrow,/)
    expect(formatDiaryDayLabel('2026-07-20', '2026-07-22')).toMatch(/Jul/)
    expect(formatDiaryDayLabel('2026-07-20', '2026-07-22')).not.toMatch(/Today|Yesterday|Tomorrow/)
  })

  it('builds inclusive 30-day analytics windows and clamps forward navigation', () => {
    expect(analyticsRangeFromEnd('2026-07-25')).toEqual({
      from: '2026-06-26',
      to: '2026-07-25',
    })
    expect(shiftAnalyticsRangeEnd('2026-07-25', -1, '2026-07-25')).toBe('2026-06-25')
    expect(shiftAnalyticsRangeEnd('2026-06-25', 1, '2026-07-25')).toBe('2026-07-25')
    expect(shiftAnalyticsRangeEnd('2026-07-25', 1, '2026-07-25')).toBe('2026-07-25')
    expect(formatAnalyticsRangeLabel('2026-06-26', '2026-07-25')).toMatch(/Jun 26/)
    expect(formatAnalyticsRangeLabel('2026-06-26', '2026-07-25')).toMatch(/Jul 25/)
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

  it('parses meal query params and builds meal-aware paths', () => {
    expect(parseMealTypeParam('LUNCH')).toBe('LUNCH')
    expect(parseMealTypeParam('snack')).toBeNull()
    expect(parseMealTypeParam(null)).toBeNull()
    expect(mealLookupPath('DINNER')).toBe('/lookup?meal=DINNER')
    expect(productPathWithMeal('abc', 'SNACK')).toBe('/products/abc?meal=SNACK')
    expect(productPathWithMeal('abc', null)).toBe('/products/abc')
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
