import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { resolveViteBase } from './src/viteBase'

export default defineConfig({
  // Web Railway SPA needs absolute `/` so /analytics (and other deep links) load
  // /assets/* instead of /analytics/assets/* (which SPA-fallback serves as HTML).
  // Capacitor builds set CAPACITOR_BUILD=1 for relative `./` WebView paths.
  base: resolveViteBase(),
  plugins: [react()],
  server: {
    port: 5173,
    // Allow the public reverse-proxy hostname (and the compose service name) to
    // reach the dev server; Vite blocks unknown Host headers by default.
    allowedHosts: process.env.VITE_ALLOWED_HOSTS
      ? process.env.VITE_ALLOWED_HOSTS.split(',')
      : ['localhost', '127.0.0.1', 'calorietracker-frontend-1'],
    // Proxy target configurable for containerised dev (VITE_DEV_PROXY_TARGET),
    // defaulting to a local gateway for plain `npm run dev`.
    proxy: {
      '/api': process.env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080',
      '/.well-known': process.env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
