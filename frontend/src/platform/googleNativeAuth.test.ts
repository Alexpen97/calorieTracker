import { beforeEach, describe, expect, it, vi } from 'vitest'

const login = vi.fn()
const initialize = vi.fn()

vi.mock('@capgo/capacitor-social-login', () => ({
  SocialLogin: {
    initialize: (...args: unknown[]) => initialize(...args),
    login: (...args: unknown[]) => login(...args),
  },
}))

describe('googleNativeAuth', () => {
  beforeEach(() => {
    vi.resetModules()
    initialize.mockReset()
    login.mockReset()
  })

  it('is disabled on web', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => false,
      nativePlatform: () => 'web',
    }))
    const { canUseNativeGoogleSignIn, loginWithGoogleNative, resetGoogleNativeAuthForTests } =
      await import('./googleNativeAuth')
    resetGoogleNativeAuthForTests()
    expect(canUseNativeGoogleSignIn()).toBe(false)
    await expect(loginWithGoogleNative('client-id')).rejects.toThrow(/only available/)
  })

  it('returns serverAuthCode from offline Google Sign-In on native', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => true,
      nativePlatform: () => 'android',
    }))
    initialize.mockResolvedValue(undefined)
    login.mockResolvedValue({
      provider: 'google',
      result: { serverAuthCode: 'server-code-1', responseType: 'offline' },
    })

    const mod = await import('./googleNativeAuth')
    mod.resetGoogleNativeAuthForTests()
    expect(mod.canUseNativeGoogleSignIn()).toBe(true)
    await expect(mod.loginWithGoogleNative('web-client.apps.googleusercontent.com')).resolves.toBe(
      'server-code-1',
    )
    expect(initialize).toHaveBeenCalledWith({
      google: {
        webClientId: 'web-client.apps.googleusercontent.com',
        mode: 'offline',
      },
    })
  })
})
