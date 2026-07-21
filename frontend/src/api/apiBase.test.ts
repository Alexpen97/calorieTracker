import { afterEach, describe, expect, it, vi } from 'vitest'
import { resolveApiBase, formatHttpError } from './apiBase'

afterEach(() => {
  vi.unstubAllEnvs()
})

describe('resolveApiBase', () => {
  it('returns empty string for unset or blank values (same-origin /api)', () => {
    expect(resolveApiBase(undefined)).toBe('')
    expect(resolveApiBase('')).toBe('')
    expect(resolveApiBase('   ')).toBe('')
  })

  it('preserves absolute https URLs and strips trailing slashes', () => {
    expect(resolveApiBase('https://gateway.example.com')).toBe('https://gateway.example.com')
    expect(resolveApiBase('https://gateway.example.com/')).toBe('https://gateway.example.com')
    expect(resolveApiBase('https://gateway.example.com/api')).toBe('https://gateway.example.com/api')
  })

  it('preserves absolute http URLs', () => {
    expect(resolveApiBase('http://localhost:8080')).toBe('http://localhost:8080')
  })

  it('adds https in production builds when the scheme is omitted', () => {
    vi.stubEnv('PROD', true)
    expect(resolveApiBase('gateway-production.up.railway.app')).toBe(
      'https://gateway-production.up.railway.app',
    )
  })

  it('adds http in non-production builds when the scheme is omitted', () => {
    vi.stubEnv('PROD', false)
    expect(resolveApiBase('localhost:8080')).toBe('http://localhost:8080')
  })
})

describe('formatHttpError', () => {
  it('explains nginx 405 responses from misconfigured API base URLs', () => {
    const nginxBody = '<html><center><h1>405 Not Allowed</h1></center><hr><center>nginx/1.27.5</center>'
    expect(formatHttpError(405, nginxBody)).toContain('VITE_API_BASE_URL')
  })

  it('falls back to the body or status for other errors', () => {
    expect(formatHttpError(500, 'server error')).toBe('server error')
    expect(formatHttpError(404, '')).toBe('Request failed (404)')
  })
})
