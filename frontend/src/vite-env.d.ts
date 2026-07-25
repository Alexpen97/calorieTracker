/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_GOOGLE_CLIENT_ID?: string
  readonly VITE_AUTH_MODE?: string
  readonly VITE_SAMSUNG_HEALTH_ENABLED?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
