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
    proxy: {
      '/api': 'http://localhost:8080',
      '/.well-known': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
