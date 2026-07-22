import { SocialLogin } from '@capgo/capacitor-social-login'
import { isNativePlatform } from './native'

let initialized = false

async function ensureGoogleInitialized(webClientId: string): Promise<void> {
  if (initialized) {
    return
  }
  await SocialLogin.initialize({
    google: {
      webClientId,
      mode: 'offline',
    },
  })
  initialized = true
}

/**
 * Native Google Sign-In (account picker) returning a server auth code for
 * exchange via auth-service. Web callers should use the PKCE redirect flow.
 */
export async function loginWithGoogleNative(webClientId: string): Promise<string> {
  if (!isNativePlatform()) {
    throw new Error('Native Google Sign-In is only available in the Android app')
  }
  if (!webClientId) {
    throw new Error('VITE_GOOGLE_CLIENT_ID is not configured.')
  }

  await ensureGoogleInitialized(webClientId)
  const response = await SocialLogin.login({
    provider: 'google',
    options: {
      scopes: ['email', 'profile', 'openid'],
      forceRefreshToken: true,
    },
  })

  if (response.provider !== 'google') {
    throw new Error('Unexpected Google Sign-In provider response')
  }

  const result = response.result as { serverAuthCode?: string; responseType?: string }
  const code = result.serverAuthCode
  if (!code) {
    throw new Error('Google Sign-In did not return a server auth code')
  }
  return code
}

export function canUseNativeGoogleSignIn(): boolean {
  return isNativePlatform()
}

/** Test helper — resets the initialize-once guard. */
export function resetGoogleNativeAuthForTests(): void {
  initialized = false
}
