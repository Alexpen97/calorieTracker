import { useNavigate } from 'react-router-dom'
import { createPkcePair, isLoggedIn, saveTokens } from '../auth/tokenStorage'
import {
  getGoogleJavascriptOrigin,
  getGoogleRedirectUri,
} from '../auth/oauthRedirect'
import { exchangeGoogleCode } from '../api/client'
import { useEffect, useState } from 'react'

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''
const authMode = import.meta.env.VITE_AUTH_MODE ?? 'dev'

export default function LoginPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const javascriptOrigin = getGoogleJavascriptOrigin()
  const redirectUri = getGoogleRedirectUri()

  useEffect(() => {
    if (isLoggedIn()) {
      navigate('/today', { replace: true })
    }
  }, [navigate])

  async function startGoogleLogin() {
    setError(null)
    if (!googleClientId) {
      setError('VITE_GOOGLE_CLIENT_ID is not configured.')
      return
    }
    const { verifier, challenge } = await createPkcePair()
    sessionStorage.setItem('pkce_verifier', verifier)
    const params = new URLSearchParams({
      client_id: googleClientId,
      redirect_uri: redirectUri,
      response_type: 'code',
      scope: 'openid email profile',
      code_challenge: challenge,
      code_challenge_method: 'S256',
      access_type: 'online',
      prompt: 'select_account',
    })
    window.location.assign(`https://accounts.google.com/o/oauth2/v2/auth?${params}`)
  }

  async function devLogin() {
    setBusy(true)
    setError(null)
    try {
      const tokens = await exchangeGoogleCode({
        code: 'dev',
        redirectUri,
      })
      saveTokens(tokens)
      navigate('/onboarding')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Dev login failed')
    } finally {
      setBusy(false)
    }
  }

  async function copyRedirectUri() {
    try {
      await navigator.clipboard.writeText(redirectUri)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      setError(`Copy failed — paste this into Google: ${redirectUri}`)
    }
  }

  return (
    <main className="hero">
      <div className="hero-inner">
        <h1>NutriTrack</h1>
        <p>See what you eat — calories, macros, and micronutrients in one calm place.</p>
        <div className="cta-row">
          <button className="btn btn-primary" type="button" onClick={startGoogleLogin}>
            Continue with Google
          </button>
          {authMode === 'dev' && (
            <button className="btn btn-secondary" type="button" disabled={busy} onClick={devLogin}>
              {busy ? 'Signing in…' : 'Dev login'}
            </button>
          )}
        </div>
        {error && <p className="error">{error}</p>}
        {googleClientId && (
          <section className="oauth-setup" aria-label="Google OAuth setup">
            <p>
              If Google shows <code>redirect_uri_mismatch</code>, register these exact values on
              your <strong>Web application</strong> OAuth client (not the domain alone, and not
              another Railway app):
            </p>
            <dl>
              <dt>Authorized JavaScript origins</dt>
              <dd>
                <code>{javascriptOrigin}</code>
              </dd>
              <dt>Authorized redirect URIs</dt>
              <dd>
                <code>{redirectUri}</code>
                <button className="btn btn-secondary btn-compact" type="button" onClick={copyRedirectUri}>
                  {copied ? 'Copied' : 'Copy'}
                </button>
              </dd>
            </dl>
          </section>
        )}
      </div>
    </main>
  )
}
