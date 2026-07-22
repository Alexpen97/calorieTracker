import { beforeEach, describe, expect, it, vi } from 'vitest'

const scan = vi.fn()
const isGoogleBarcodeScannerModuleAvailable = vi.fn()
const installGoogleBarcodeScannerModule = vi.fn()

vi.mock('@capacitor-mlkit/barcode-scanning', () => ({
  BarcodeFormat: {
    Ean13: 'EAN_13',
    Ean8: 'EAN_8',
    UpcA: 'UPC_A',
    UpcE: 'UPC_E',
    Code128: 'CODE_128',
  },
  BarcodeScanner: {
    isGoogleBarcodeScannerModuleAvailable: (...args: unknown[]) =>
      isGoogleBarcodeScannerModuleAvailable(...args),
    installGoogleBarcodeScannerModule: (...args: unknown[]) =>
      installGoogleBarcodeScannerModule(...args),
    scan: (...args: unknown[]) => scan(...args),
  },
}))

describe('barcodeScan', () => {
  beforeEach(() => {
    vi.resetModules()
    scan.mockReset()
    isGoogleBarcodeScannerModuleAvailable.mockReset()
    installGoogleBarcodeScannerModule.mockReset()
  })

  it('returns null on web without calling ML Kit', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => false,
      nativePlatform: () => 'web',
    }))
    const { scanBarcodeNative, isNativeBarcodeScanAvailable } = await import('./barcodeScan')
    await expect(scanBarcodeNative()).resolves.toBeNull()
    await expect(isNativeBarcodeScanAvailable()).resolves.toBe(false)
    expect(scan).not.toHaveBeenCalled()
  })

  it('returns the first ML Kit barcode value on native', async () => {
    vi.doMock('./native', () => ({
      isNativePlatform: () => true,
      nativePlatform: () => 'android',
    }))
    isGoogleBarcodeScannerModuleAvailable.mockResolvedValue({ available: true })
    scan.mockResolvedValue({ barcodes: [{ rawValue: '3017620422003' }] })

    const { scanBarcodeNative, isNativeBarcodeScanAvailable } = await import('./barcodeScan')
    await expect(isNativeBarcodeScanAvailable()).resolves.toBe(true)
    await expect(scanBarcodeNative()).resolves.toBe('3017620422003')
    expect(scan).toHaveBeenCalled()
  })
})
