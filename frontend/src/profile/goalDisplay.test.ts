import { describe, expect, it } from 'vitest'
import { formatGoalLabel, needsProfileMessage } from './goalDisplay'

describe('profile goal display helpers', () => {
  it('formats nutrient targets with human labels and origin copy', () => {
    expect(
      formatGoalLabel({
        nutrientCode: 'energy_kcal',
        dailyTarget: 2150,
        unit: 'kcal',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T12:00:00Z',
      }),
    ).toBe('Energy kcal: 2,150 kcal (computed)')

    expect(
      formatGoalLabel({
        nutrientCode: 'vitamin_c',
        dailyTarget: 90.5,
        unit: 'mg',
        origin: 'USER_OVERRIDE',
        computedAt: null,
      }),
    ).toBe('Vitamin c: 90.5 mg (custom)')
  })

  it('only shows the profile completion message when recalculation needs profile data', () => {
    expect(needsProfileMessage(true)).toBe(
      'Complete sex, birth date, height, activity, objective, and weight to calculate goals.',
    )
    expect(needsProfileMessage(false)).toBeNull()
  })
})
