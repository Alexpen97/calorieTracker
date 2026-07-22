import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearTokens, getAccessToken, saveTokens } from '../auth/tokenStorage'

const apiBase = 'https://gateway.example.com'

describe('authenticatedFetch refresh on 401', () => {
  beforeEach(async () => {
    await clearTokens()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(async () => {
    await clearTokens()
    vi.unstubAllGlobals()
    vi.resetModules()
  })

  it('refreshes the access token and retries once on 401', async () => {
    await saveTokens({
      accessToken: 'expired',
      refreshToken: 'refresh-1',
      tokenType: 'Bearer',
      expiresIn: 0,
    })

    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(new Response('', { status: 401 }))
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            accessToken: 'fresh',
            refreshToken: 'refresh-2',
            tokenType: 'Bearer',
            expiresIn: 900,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify([{ id: 'w1' }]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

    vi.stubEnv('VITE_API_BASE_URL', apiBase)
    const { fetchWater } = await import('./client')
    const water = await fetchWater('2026-07-22')

    expect(water).toEqual([{ id: 'w1' }])
    expect(getAccessToken()).toBe('fresh')
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(String(fetchMock.mock.calls[1]?.[0])).toContain('/api/auth/refresh')
    expect(fetchMock.mock.calls[2]?.[1]).toMatchObject({
      headers: expect.objectContaining({ Authorization: 'Bearer fresh' }),
    })
  })
})
