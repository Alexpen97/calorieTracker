import { describe, expect, it } from 'vitest'
import { isValidBarcode, sanitizeBarcodeInput } from './barcode'

describe('barcode helpers', () => {
  it('strips non-digits and validates length', () => {
    expect(sanitizeBarcodeInput(' 3017-6204 22003 ')).toBe('3017620422003')
    expect(isValidBarcode('3017620422003')).toBe(true)
    expect(isValidBarcode('123')).toBe(false)
    expect(isValidBarcode('abcdef')).toBe(false)
  })
})
