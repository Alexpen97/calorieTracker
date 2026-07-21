/** Path Google must redirect to after consent (SPA route, not the API). */
export const GOOGLE_OAUTH_CALLBACK_PATH = '/auth/callback'

function normalizeOrigin(origin: string): string {
  return origin.replace(/\/+$/, '')
}

/** Value for Google Cloud "Authorized JavaScript origins" (no path). */
export function getGoogleJavascriptOrigin(origin: string = window.location.origin): string {
  return normalizeOrigin(origin)
}

/**
 * Exact redirect_uri the SPA sends to Google.
 * Must match an "Authorized redirect URI" on the Web OAuth client — character for character.
 */
export function getGoogleRedirectUri(origin: string = window.location.origin): string {
  return `${normalizeOrigin(origin)}${GOOGLE_OAUTH_CALLBACK_PATH}`
}
