import { describe, expect, it } from 'vitest'
import { resolveRouterBasename, resolveViteBase } from './viteBase'

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

describe('resolveRouterBasename', () => {
  it('maps absolute root to an empty basename', () => {
    expect(resolveRouterBasename('/')).toBe('')
    expect(resolveRouterBasename(undefined)).toBe('')
  })

  it('maps Capacitor relative base to an empty basename', () => {
    // Capacitor builds use Vite `base: './'`; must not produce a broken basename.
    expect(resolveRouterBasename('./')).toBe('')
  })

  it('keeps a URL subpath without a trailing slash', () => {
    expect(resolveRouterBasename('/app')).toBe('/app')
    expect(resolveRouterBasename('/app/')).toBe('/app')
  })
})
