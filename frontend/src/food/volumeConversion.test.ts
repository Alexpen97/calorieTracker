import { describe, expect, it } from 'vitest'
import {
  convertVolumeToGrams,
  isVolumeCapable,
  resolveWeightG,
} from './volumeConversion'

describe('volumeConversion', () => {
  it('detects volume capability from density', () => {
    expect(isVolumeCapable(1)).toBe(true)
    expect(isVolumeCapable(null)).toBe(false)
    expect(isVolumeCapable(0)).toBe(false)
  })

  it('converts ml to grams with density', () => {
    expect(convertVolumeToGrams(250, 1)).toBe(250)
    expect(convertVolumeToGrams(100, 0.92)).toBe(92)
  })

  it('resolveWeightG passes grams through and converts ml', () => {
    expect(resolveWeightG(100, 'g', null)).toBe(100)
    expect(resolveWeightG(100, 'ml', 0.92)).toBe(92)
  })
})
