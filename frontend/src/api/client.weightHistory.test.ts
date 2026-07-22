import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearTokens, saveTokens } from '../auth/tokenStorage'

describe('weight history API client', () => {
  afterEach(() => {
    clearTokens()
    vi.unstubAllGlobals()
    vi.resetModules()
    vi.restoreAllMocks()
  })

  it('converts LocalDate from/to into Instant query params', async () => {
    saveTokens({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    const fetchSpy = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }))
    vi.stubGlobal('fetch', fetchSpy)
    vi.stubEnv('VITE_API_BASE_URL', '')

    const { fetchWeightHistory } = await import('./client')
    await fetchWeightHistory({ from: '2026-07-16', to: '2026-07-22' })

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/users/me/weight?from=2026-07-16T00%3A00%3A00Z&to=2026-07-22T23%3A59%3A59.999Z',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer access' }),
      }),
    )
  })

  it('passes Instant from/to through unchanged', async () => {
    saveTokens({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    const fetchSpy = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }))
    vi.stubGlobal('fetch', fetchSpy)
    vi.stubEnv('VITE_API_BASE_URL', '')

    const { fetchWeightHistory } = await import('./client')
    await fetchWeightHistory({
      from: '2026-07-21T00:00:00Z',
      to: '2026-07-21T23:59:59Z',
    })

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/users/me/weight?from=2026-07-21T00%3A00%3A00Z&to=2026-07-21T23%3A59%3A59Z',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer access' }),
      }),
    )
  })

  it('omits query string when no range is provided', async () => {
    saveTokens({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    const fetchSpy = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }))
    vi.stubGlobal('fetch', fetchSpy)
    vi.stubEnv('VITE_API_BASE_URL', '')

    const { fetchWeightHistory } = await import('./client')
    await fetchWeightHistory()

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/users/me/weight',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer access' }),
      }),
    )
  })
})
