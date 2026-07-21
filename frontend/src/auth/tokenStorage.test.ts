import { describe, expect, it, beforeEach } from 'vitest'
import { clearTokens, isLoggedIn, saveTokens, getAccessToken } from '../auth/tokenStorage'

describe('tokenStorage', () => {
  beforeEach(() => {
    clearTokens()
  })

  it('persists and clears tokens', () => {
    expect(isLoggedIn()).toBe(false)
    saveTokens({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    expect(isLoggedIn()).toBe(true)
    expect(getAccessToken()).toBe('access')
    clearTokens()
    expect(isLoggedIn()).toBe(false)
  })
})
