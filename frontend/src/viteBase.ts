/**
 * Public asset base for Vite builds. Absolute `/` for the web SPA so deep
 * links (`/analytics`, `/diary`, …) resolve `/assets/…` correctly. Relative
 * `./` is only for Capacitor WebView packaging.
 *
 * Optional: set VITE_BASE (e.g. `/app`) to serve the SPA under a URL subpath —
 * used for the dev-mode deploy behind a reverse proxy (Caddy) at `/app`.
 */
export function resolveViteBase(env: NodeJS.ProcessEnv = process.env): string {
  if (env.CAPACITOR_BUILD === '1') {
    return './'
  }
  const base = env.VITE_BASE?.trim()
  if (base && base !== '/') {
    const withSlash = base.startsWith('/') ? base : `/${base}`
    return withSlash.endsWith('/') ? withSlash : `${withSlash}/`
  }
  return '/'
}

/**
 * Router basename that must mirror the Vite `base` so client-side routing and
 * deep links resolve under the same prefix.
 *
 * - Absolute root (`/`) and Capacitor's relative base (`./`) both map to ''
 *   (root), so routes like `/today` match the WebView URL directly.
 * - A URL subpath (e.g. `/app`) maps to `/app`.
 */
export function resolveRouterBasename(viteBase: string | undefined): string {
  const trimmed = (viteBase ?? '').replace(/\/+$/, '')
  if (!trimmed || trimmed === '/' || trimmed === '.' || trimmed === './') {
    return ''
  }
  return trimmed
}
