import { describe, expect, it } from 'vitest'
import { needsOnboarding } from './needsOnboarding'
import type { UserProfile, WeightLog } from '../api/client'

function profile(overrides: Partial<UserProfile> = {}): UserProfile {
  return {
    id: 'u1',
    email: 'alex@example.com',
    displayName: 'Alex',
    avatarUrl: null,
    role: 'USER',
    sex: null,
    birthDate: null,
    heightCm: null,
    activityLevel: null,
    objective: 'MAINTAIN',
    ...overrides,
  }
}

describe('needsOnboarding', () => {
  it('requires onboarding when height, weight, or diet profile fields are missing', () => {
    expect(needsOnboarding(profile(), [])).toBe(true)
    expect(
      needsOnboarding(
        profile({
          sex: 'MALE',
          birthDate: '1996-07-21',
          heightCm: 180,
          activityLevel: 'MODERATE',
          objective: 'LOSE',
        }),
        [],
      ),
    ).toBe(true)
    expect(
      needsOnboarding(profile({ heightCm: 180, sex: 'MALE' }), [
        { id: 'w1', weightKg: 80, measuredAt: '2026-07-21T10:00:00Z' },
      ]),
    ).toBe(true)
  })

  it('skips onboarding when profile and weight are complete enough for goals', () => {
    const weights: WeightLog[] = [{ id: 'w1', weightKg: 80, measuredAt: '2026-07-21T10:00:00Z' }]
    expect(
      needsOnboarding(
        profile({
          sex: 'FEMALE',
          birthDate: '1990-01-01',
          heightCm: 165,
          activityLevel: 'LIGHT',
          objective: 'GAIN',
        }),
        weights,
      ),
    ).toBe(false)
  })
})
