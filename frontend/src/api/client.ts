import { getAccessToken, type TokenBundle } from '../auth/tokenStorage'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

export type UserProfile = {
  id: string
  email: string
  displayName: string
  avatarUrl: string | null
  role: string
  sex: string | null
  heightCm: number | null
  activityLevel: string | null
  objective: string
}

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

export async function exchangeGoogleCode(input: {
  code: string
  codeVerifier?: string
  redirectUri: string
}): Promise<TokenBundle> {
  const response = await fetch(`${apiBase}/api/auth/google/callback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<TokenBundle>(response)
}

export async function fetchMe(): Promise<UserProfile> {
  const token = getAccessToken()
  if (!token) {
    throw new Error('Not authenticated')
  }
  const response = await fetch(`${apiBase}/api/users/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  return parseJson<UserProfile>(response)
}
