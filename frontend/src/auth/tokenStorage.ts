import { SecureStorage } from '@aparajita/capacitor-secure-storage'
import { isNativePlatform } from '../platform/native'

const ACCESS_TOKEN_KEY = 'nutritrack.accessToken'
const REFRESH_TOKEN_KEY = 'nutritrack.refreshToken'

export type TokenBundle = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

let memoryAccessToken: string | null = null
let memoryRefreshToken: string | null = null
let hydrated = false

async function persist(key: string, value: string): Promise<void> {
  if (isNativePlatform()) {
    await SecureStorage.set(key, value)
    return
  }
  localStorage.setItem(key, value)
}

async function read(key: string): Promise<string | null> {
  if (isNativePlatform()) {
    const value = await SecureStorage.get(key)
    return typeof value === 'string' ? value : null
  }
  return localStorage.getItem(key)
}

async function remove(key: string): Promise<void> {
  if (isNativePlatform()) {
    await SecureStorage.remove(key)
    return
  }
  localStorage.removeItem(key)
}

/** Load tokens into memory before the React tree mounts. */
export async function initTokenStorage(): Promise<void> {
  if (hydrated) {
    return
  }
  memoryAccessToken = await read(ACCESS_TOKEN_KEY)
  memoryRefreshToken = await read(REFRESH_TOKEN_KEY)
  hydrated = true
}

export async function saveTokens(tokens: TokenBundle): Promise<void> {
  memoryAccessToken = tokens.accessToken
  memoryRefreshToken = tokens.refreshToken
  await persist(ACCESS_TOKEN_KEY, tokens.accessToken)
  await persist(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export async function clearTokens(): Promise<void> {
  memoryAccessToken = null
  memoryRefreshToken = null
  await remove(ACCESS_TOKEN_KEY)
  await remove(REFRESH_TOKEN_KEY)
}

export function getAccessToken(): string | null {
  // Web: prefer live localStorage so duplicate module graphs (e.g. Vitest
  // resetModules) still see the same tokens. Native: memory after init.
  if (!isNativePlatform() && typeof localStorage !== 'undefined') {
    memoryAccessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
  }
  return memoryAccessToken
}

export function getRefreshToken(): string | null {
  if (!isNativePlatform() && typeof localStorage !== 'undefined') {
    memoryRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  }
  return memoryRefreshToken
}

export function isLoggedIn(): boolean {
  return Boolean(getAccessToken())
}

/** PKCE helpers for Google Authorization Code flow (web). */
export async function createPkcePair(): Promise<{ verifier: string; challenge: string }> {
  const verifier = base64Url(crypto.getRandomValues(new Uint8Array(32)))
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  const challenge = base64Url(new Uint8Array(digest))
  return { verifier, challenge }
}

function base64Url(bytes: Uint8Array): string {
  let binary = ''
  bytes.forEach((b) => {
    binary += String.fromCharCode(b)
  })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/** Test helper — clears in-memory + web storage without async SecureStorage. */
export function resetTokenStorageForTests(): void {
  memoryAccessToken = null
  memoryRefreshToken = null
  hydrated = false
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

/** Simulate app restart: drop memory but keep persisted web storage. */
export function unloadTokenStorageForTests(): void {
  memoryAccessToken = null
  memoryRefreshToken = null
  hydrated = false
}
