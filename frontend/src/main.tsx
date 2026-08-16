import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import { initTokenStorage } from './auth/tokenStorage'
import { resolveRouterBasename } from './viteBase'
import './index.css'

const queryClient = new QueryClient()

// Routing base: mirrors Vite `base` so client-side navigation and deep links
// resolve under the same prefix. Capacitor's relative base (`./`) maps to root.
const basename = resolveRouterBasename(import.meta.env.BASE_URL)

function showBootError(error: unknown): void {
  // Render a visible message instead of a silent white screen so native
  // (Capacitor WebView) boot failures are diagnosable.
  console.error('[bootstrap] failed', error)
  const root = document.getElementById('root')
  if (!root) return
  root.innerHTML = `<main style="padding:24px;font-family:sans-serif;color:#1f2937">
    <h1>Something went wrong starting NutriTrack</h1>
    <pre style="white-space:pre-wrap;background:#f3f4f6;padding:12px;border-radius:8px">${
      error instanceof Error ? error.message : String(error)
    }</pre>
  </main>`
}

async function bootstrap() {
  try {
    await initTokenStorage()
    createRoot(document.getElementById('root')!).render(
      <StrictMode>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter basename={basename}>
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </StrictMode>,
    )
  } catch (err) {
    showBootError(err)
  }
}

void bootstrap()
