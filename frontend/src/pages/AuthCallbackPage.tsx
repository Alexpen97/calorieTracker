import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { exchangeGoogleCode } from '../api/client'
import { saveTokens } from '../auth/tokenStorage'

export default function AuthCallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    if (!code) {
      setError('Missing authorization code from Google.')
      return
    }
    const verifier = sessionStorage.getItem('pkce_verifier') ?? undefined
    const redirectUri = `${window.location.origin}/auth/callback`
    exchangeGoogleCode({ code, codeVerifier: verifier, redirectUri })
      .then((tokens) => {
        saveTokens(tokens)
        sessionStorage.removeItem('pkce_verifier')
        navigate('/lookup', { replace: true })
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : 'Login failed')
      })
  }, [navigate])

  return (
    <main className="hero">
      <div className="hero-inner">
        <h1>NutriTrack</h1>
        <p>{error ?? 'Finishing sign-in…'}</p>
      </div>
    </main>
  )
}
