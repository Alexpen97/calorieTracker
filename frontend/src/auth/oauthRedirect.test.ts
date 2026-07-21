import { describe, expect, it } from 'vitest'
import {
  getGoogleJavascriptOrigin,
  getGoogleRedirectUri,
} from './oauthRedirect'

describe('oauthRedirect', () => {
  it('builds the Google redirect URI as origin + /auth/callback', () => {
    expect(getGoogleRedirectUri('https://front-end-production-4a95.up.railway.app')).toBe(
      'https://front-end-production-4a95.up.railway.app/auth/callback',
    )
  })

  it('strips a trailing slash from the origin before appending the callback path', () => {
    expect(getGoogleRedirectUri('https://front-end-production-4a95.up.railway.app/')).toBe(
      'https://front-end-production-4a95.up.railway.app/auth/callback',
    )
  })

  it('uses the origin alone for Authorized JavaScript origins', () => {
    expect(getGoogleJavascriptOrigin('https://front-end-production-4a95.up.railway.app/')).toBe(
      'https://front-end-production-4a95.up.railway.app',
    )
  })

  it('never uses the gateway API callback path as the Google redirect URI', () => {
    const uri = getGoogleRedirectUri('https://front-end-production-4a95.up.railway.app')
    expect(uri).not.toContain('/api/auth')
    expect(uri.endsWith('/auth/callback')).toBe(true)
  })
})
