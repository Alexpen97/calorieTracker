import { describe, expect, it } from 'vitest'
import { resolveViteBase } from './viteBase'

describe('resolveViteBase', () => {
  it('defaults to absolute root for web SPA deep links', () => {
    expect(resolveViteBase({})).toBe('/')
    expect(resolveViteBase({ CAPACITOR_BUILD: '' })).toBe('/')
    expect(resolveViteBase({ CAPACITOR_BUILD: '0' })).toBe('/')
  })

  it('uses relative base only for Capacitor packaging', () => {
    expect(resolveViteBase({ CAPACITOR_BUILD: '1' })).toBe('./')
  })
})
