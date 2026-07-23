/** Public asset base for Vite builds. Absolute `/` for the web SPA so deep
 * links (`/analytics`, `/diary`, …) resolve `/assets/…` correctly. Relative
 * `./` is only for Capacitor WebView packaging. */
export function resolveViteBase(env: NodeJS.ProcessEnv = process.env): '/' | './' {
  return env.CAPACITOR_BUILD === '1' ? './' : '/'
}
