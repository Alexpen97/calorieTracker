import { registerPlugin } from '@capacitor/core'
import {
  browserTimeZone,
  syncSamsungHealth,
  type SamsungHealthSyncBody,
  type SamsungHealthSyncDay,
  type SamsungHealthSyncResponse,
} from '../api/client'
import { isNativePlatform, nativePlatform } from './native'

export type SamsungHealthPermissionState = string

export type SamsungHealthPluginStatus = {
  /** Legacy/web mock field. Prefer supported + sdkLinked from the Android plugin. */
  available?: boolean
  supported?: boolean
  sdkLinked?: boolean
  permissionState: SamsungHealthPermissionState
  message?: string
}

export type SamsungHealthPluginDay = {
  localDate: string
  activeEnergyKcal?: number
  totalEnergyKcal?: number
  selectedBurnKcal?: number
  sourceRecordCount?: number
}

export interface SamsungHealthPlugin {
  getStatus(): Promise<SamsungHealthPluginStatus>
  requestPermissions(): Promise<{ permissionState: SamsungHealthPermissionState }>
  readDailyBurns(options: {
    fromDate: string
    toDate: string
    zone: string
  }): Promise<{ days: SamsungHealthPluginDay[] }>
}

const SamsungHealth = registerPlugin<SamsungHealthPlugin>('SamsungHealth')

export type SamsungHealthConnectionState =
  | { status: 'unsupported' }
  | { status: 'unavailable'; reason?: string }
  | { status: 'permission_denied'; permissionState: string }
  | { status: 'ready'; permissionState: string }
  | { status: 'connected'; permissionState: string }

export function isSamsungHealthFeatureEnabled(): boolean {
  return import.meta.env.VITE_SAMSUNG_HEALTH_ENABLED !== 'false'
}

export function isSamsungHealthSupported(): boolean {
  return isNativePlatform() && nativePlatform() === 'android'
}

export async function getConnectionState(): Promise<SamsungHealthConnectionState> {
  if (!isSamsungHealthSupported()) {
    return { status: 'unsupported' }
  }

  try {
    const status = await SamsungHealth.getStatus()
    const permissionState = status?.permissionState ?? 'UNKNOWN'
    if (!isPluginReady(status)) {
      return {
        status: 'unavailable',
        reason:
          status?.message ||
          (permissionState === 'SDK_NOT_LINKED'
            ? 'Samsung Health SDK is not linked in this build'
            : 'Samsung Health is not available on this device'),
      }
    }
    if (isDenied(permissionState)) {
      return { status: 'permission_denied', permissionState }
    }
    if (permissionState === 'GRANTED') {
      return { status: 'connected', permissionState }
    }
    return { status: 'ready', permissionState }
  } catch (error) {
    return mapPluginError(error)
  }
}

export async function requestSamsungHealthPermissions(): Promise<{
  permissionState: SamsungHealthPermissionState
}> {
  if (!isSamsungHealthSupported()) {
    return { permissionState: 'UNSUPPORTED' }
  }
  try {
    return await SamsungHealth.requestPermissions()
  } catch (error) {
    const mapped = mapPluginError(error)
    if (mapped.status === 'permission_denied') {
      return { permissionState: mapped.permissionState }
    }
    return { permissionState: 'DENIED' }
  }
}

export async function readSamsungHealthDailyBurns(
  fromDate: string,
  toDate: string,
  zone: string,
): Promise<SamsungHealthSyncDay[]> {
  if (!isSamsungHealthSupported()) {
    return []
  }
  try {
    const result = await SamsungHealth.readDailyBurns({ fromDate, toDate, zone })
    return result.days ?? []
  } catch {
    return []
  }
}

export type CollectAndSyncOptions = {
  zone?: string
  daysBack?: number
  clock?: Date
  syncFn?: (body: SamsungHealthSyncBody) => Promise<SamsungHealthSyncResponse>
}

export async function collectAndSyncSamsungHealth(
  options: CollectAndSyncOptions = {},
): Promise<SamsungHealthSyncResponse> {
  if (!isSamsungHealthSupported()) {
    throw new Error('Samsung Health sync requires the Android app')
  }

  const permission = await requestSamsungHealthPermissions()
  if (isDenied(permission.permissionState) || permission.permissionState === 'UNSUPPORTED') {
    throw new Error('Samsung Health permission is required')
  }

  const zone = options.zone ?? browserTimeZone()
  const daysBack = options.daysBack ?? 7
  const clock = options.clock ?? new Date()
  const toDate = formatLocalDate(clock)
  const from = new Date(clock)
  from.setDate(from.getDate() - (daysBack - 1))
  const fromDate = formatLocalDate(from)

  const days = await readSamsungHealthDailyBurns(fromDate, toDate, zone)
  if (days.length === 0) {
    throw new Error('No Samsung Health activity found for the selected dates')
  }

  const syncFn = options.syncFn ?? syncSamsungHealth
  return syncFn({
    zone,
    permissionState: permission.permissionState || 'GRANTED',
    days,
  })
}

function isPluginReady(status: SamsungHealthPluginStatus | null | undefined): boolean {
  if (!status) return false
  if (typeof status.available === 'boolean') {
    return status.available
  }
  if (status.supported === false) return false
  if (status.sdkLinked === false) return false
  if ((status.permissionState ?? '').toUpperCase() === 'SDK_NOT_LINKED') return false
  return status.supported !== false
}

function isDenied(permissionState: string): boolean {
  const normalized = permissionState.toUpperCase()
  return normalized === 'DENIED' || normalized === 'PERMISSION_DENIED'
}

function mapPluginError(error: unknown): SamsungHealthConnectionState {
  const message = error instanceof Error ? error.message : String(error ?? '')
  const lower = message.toLowerCase()
  if (
    lower.includes('permission') ||
    lower.includes('denied') ||
    lower.includes('not implemented') ||
    lower.includes('unimplemented')
  ) {
    return { status: 'permission_denied', permissionState: 'DENIED' }
  }
  return {
    status: 'unavailable',
    reason: message || 'Samsung Health plugin is unavailable',
  }
}

function formatLocalDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
