import { beforeEach, describe, expect, it, vi } from 'vitest'

const getStatus = vi.fn()
const requestPermissions = vi.fn()
const readDailyBurns = vi.fn()
const syncSamsungHealth = vi.fn()

vi.mock('@capacitor/core', () => ({
  registerPlugin: () => ({
    getStatus: (...args: unknown[]) => getStatus(...args),
    requestPermissions: (...args: unknown[]) => requestPermissions(...args),
    readDailyBurns: (...args: unknown[]) => readDailyBurns(...args),
  }),
}))

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return {
    ...actual,
    syncSamsungHealth: (...args: unknown[]) => syncSamsungHealth(...args),
  }
})

describe('samsungHealth platform adapter', () => {
  beforeEach(() => {
    vi.resetModules()
    getStatus.mockReset()
    requestPermissions.mockReset()
    readDailyBurns.mockReset()
    syncSamsungHealth.mockReset()
    vi.unstubAllEnvs()
  })

  it('is feature-enabled unless VITE_SAMSUNG_HEALTH_ENABLED is false', async () => {
    vi.stubEnv('VITE_SAMSUNG_HEALTH_ENABLED', undefined)
    vi.doMock('./native', () => ({
      isNativePlatform: () => false,
      nativePlatform: () => 'web',
    }))
    const { isSamsungHealthFeatureEnabled } = await import('./samsungHealth')
    expect(isSamsungHealthFeatureEnabled()).toBe(true)

    vi.resetModules()
    vi.stubEnv('VITE_SAMSUNG_HEALTH_ENABLED', 'false')
    vi.doMock('./native', () => ({
      isNativePlatform: () => false,
      nativePlatform: () => 'web',
    }))
    const disabled = await import('./samsungHealth')
    expect(disabled.isSamsungHealthFeatureEnabled()).toBe(false)
  })

  it('reports unsupported on web without calling the plugin', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => false,
      nativePlatform: () => 'web',
    }))
    const { getConnectionState, isSamsungHealthSupported } = await import('./samsungHealth')
    expect(isSamsungHealthSupported()).toBe(false)
    await expect(getConnectionState()).resolves.toEqual({ status: 'unsupported' })
    expect(getStatus).not.toHaveBeenCalled()
  })

  it('maps plugin permission errors to permission_denied on native', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => true,
      nativePlatform: () => 'android',
    }))
    getStatus.mockRejectedValue(new Error('permission denied by user'))

    const { getConnectionState } = await import('./samsungHealth')
    await expect(getConnectionState()).resolves.toEqual({
      status: 'permission_denied',
      permissionState: 'DENIED',
    })
  })

  it('maps missing plugin errors to unavailable on native', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => true,
      nativePlatform: () => 'android',
    }))
    getStatus.mockRejectedValue(new Error('SamsungHealth plugin is not available'))

    const { getConnectionState } = await import('./samsungHealth')
    await expect(getConnectionState()).resolves.toEqual({
      status: 'unavailable',
      reason: 'SamsungHealth plugin is not available',
    })
  })

  it('maps Android SDK_NOT_LINKED status to unavailable', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => true,
      nativePlatform: () => 'android',
    }))
    getStatus.mockResolvedValue({
      supported: true,
      sdkLinked: false,
      permissionState: 'SDK_NOT_LINKED',
      message: 'Add the Samsung Health SDK AAR to enable live reads',
    })

    const { getConnectionState } = await import('./samsungHealth')
    await expect(getConnectionState()).resolves.toEqual({
      status: 'unavailable',
      reason: 'Add the Samsung Health SDK AAR to enable live reads',
    })
  })

  it('collects daily burns and posts them through syncSamsungHealth', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => true,
      nativePlatform: () => 'android',
    }))
    requestPermissions.mockResolvedValue({ permissionState: 'GRANTED' })
    readDailyBurns.mockResolvedValue({
      days: [
        {
          localDate: '2026-07-21',
          activeEnergyKcal: 320,
          selectedBurnKcal: 320,
          sourceRecordCount: 2,
        },
      ],
    })
    syncSamsungHealth.mockResolvedValue({
      provider: 'SAMSUNG_HEALTH',
      syncedAt: '2026-07-25T16:30:00Z',
      days: [{ localDate: '2026-07-21', selectedBurnKcal: 320 }],
    })

    const { collectAndSyncSamsungHealth } = await import('./samsungHealth')
    const clock = new Date(2026, 6, 25)
    const result = await collectAndSyncSamsungHealth({
      zone: 'Europe/Amsterdam',
      daysBack: 7,
      clock,
    })

    expect(requestPermissions).toHaveBeenCalled()
    expect(readDailyBurns).toHaveBeenCalledWith({
      fromDate: '2026-07-19',
      toDate: '2026-07-25',
      zone: 'Europe/Amsterdam',
    })
    expect(syncSamsungHealth).toHaveBeenCalledWith({
      zone: 'Europe/Amsterdam',
      permissionState: 'GRANTED',
      days: [
        {
          localDate: '2026-07-21',
          activeEnergyKcal: 320,
          selectedBurnKcal: 320,
          sourceRecordCount: 2,
        },
      ],
    })
    expect(result.provider).toBe('SAMSUNG_HEALTH')
  })
})
