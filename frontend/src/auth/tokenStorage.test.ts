import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearTokens,
  getAccessToken,
  initTokenStorage,
  isLoggedIn,
  resetTokenStorageForTests,
  saveTokens,
  unloadTokenStorageForTests,
} from '../auth/tokenStorage'

describe('tokenStorage', () => {
  beforeEach(() => {
    resetTokenStorageForTests()
  })

  it('persists and clears tokens', async () => {
    expect(isLoggedIn()).toBe(false)
    await saveTokens({
      accessToken: 'access',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    expect(isLoggedIn()).toBe(true)
    expect(getAccessToken()).toBe('access')

    unloadTokenStorageForTests()
    await initTokenStorage()
    expect(getAccessToken()).toBe('access')

    await clearTokens()
    expect(isLoggedIn()).toBe(false)
  })
})
