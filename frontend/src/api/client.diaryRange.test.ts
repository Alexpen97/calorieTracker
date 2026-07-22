import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearTokens, saveTokens } from '../auth/tokenStorage'

describe('diary range API client', () => {
  afterEach(() => {
    clearTokens()
    vi.unstubAllGlobals()
    vi.resetModules()
    vi.restoreAllMocks()
  })

  it('calls the range summary endpoint with from, to, and browser zone', async () => {
    saveTokens({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({
      timeZone: 'Europe/Amsterdam',
    } as Intl.ResolvedDateTimeFormatOptions)
    const fetchSpy = vi.fn().mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }))
    vi.stubGlobal('fetch', fetchSpy)
    vi.stubEnv('VITE_API_BASE_URL', '')

    const { fetchDiarySummaryRange } = await import('./client')
    await fetchDiarySummaryRange('2026-07-16', '2026-07-22')

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/diary/summary/range?from=2026-07-16&to=2026-07-22&zone=Europe%2FAmsterdam',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer access' }),
      }),
    )
  })
})
