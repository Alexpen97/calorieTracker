import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import App from './App'
import { initTokenStorage } from './auth/tokenStorage'
import './index.css'

const queryClient = new QueryClient()

// Routing base: matches Vite `base`. Defaults to '' (root, production/dev at `/`),
// but when served under a subpath (e.g. VITE_BASE=/app) the router must share it
// so client-side navigation and deep links resolve under that prefix.
const basename = import.meta.env.BASE_URL && import.meta.env.BASE_URL !== '/'
  ? import.meta.env.BASE_URL.replace(/\/$/, '')
  : ''

async function bootstrap() {
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
}

void bootstrap()
