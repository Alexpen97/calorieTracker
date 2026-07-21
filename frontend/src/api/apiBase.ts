/**
 * Resolves VITE_API_BASE_URL to an absolute API origin prefix, or '' for same-origin.
 *
 * Without a scheme, values like "gateway.example.com" are treated as relative paths by
 * fetch() (e.g. from /auth/callback → POST /auth/gateway.example.com/api/...), which
 * nginx serves from the SPA location and rejects with 405 Method Not Allowed.
 */
export function resolveApiBase(raw: string | undefined): string {
  const trimmed = (raw ?? '').trim()
  if (!trimmed) {
    return ''
  }

  const withoutTrailingSlash = trimmed.replace(/\/+$/, '')
  if (/^https?:\/\//i.test(withoutTrailingSlash)) {
    return withoutTrailingSlash
  }

  const scheme = import.meta.env.PROD ? 'https' : 'http'
  return `${scheme}://${withoutTrailingSlash}`
}

export function formatHttpError(status: number, body: string): string {
  if (status === 405 && body.includes('nginx')) {
    return 'API request was blocked (405). Check VITE_API_BASE_URL includes https:// and points at the gateway, not the frontend.'
  }
  return body || `Request failed (${status})`
}
