import { useNavigate } from 'react-router-dom'
import { createPkcePair, isLoggedIn, saveTokens } from '../auth/tokenStorage'
import { exchangeGoogleCode } from '../api/client'
import { useEffect, useState } from 'react'

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''
const authMode = import.meta.env.VITE_AUTH_MODE ?? 'dev'

export default function LoginPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (isLoggedIn()) {
      navigate('/lookup', { replace: true })
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
    const redirectUri = `${window.location.origin}/auth/callback`
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
        redirectUri: `${window.location.origin}/auth/callback`,
      })
      saveTokens(tokens)
      navigate('/lookup')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Dev login failed')
    } finally {
      setBusy(false)
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
      </div>
    </main>
  )
}
