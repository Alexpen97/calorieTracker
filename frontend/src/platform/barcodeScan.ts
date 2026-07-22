import { BarcodeFormat, BarcodeScanner } from '@capacitor-mlkit/barcode-scanning'
import { isNativePlatform } from './native'

const FOOD_BARCODE_FORMATS = [
  BarcodeFormat.Ean13,
  BarcodeFormat.Ean8,
  BarcodeFormat.UpcA,
  BarcodeFormat.UpcE,
  BarcodeFormat.Code128,
]

/**
 * Opens the native ML Kit barcode UI when available.
 * Returns the raw barcode string, or null if the user cancelled / none found.
 * On web this always returns null — use the Camera + BarcodeDetector path instead.
 */
export async function scanBarcodeNative(): Promise<string | null> {
  if (!isNativePlatform()) {
    return null
  }

  const { available } = await BarcodeScanner.isGoogleBarcodeScannerModuleAvailable()
  if (!available) {
    await BarcodeScanner.installGoogleBarcodeScannerModule()
  }

  const { barcodes } = await BarcodeScanner.scan({
    formats: FOOD_BARCODE_FORMATS,
  })
  const raw = barcodes[0]?.rawValue
  return raw && raw.length > 0 ? raw : null
}

export async function isNativeBarcodeScanAvailable(): Promise<boolean> {
  return isNativePlatform()
}
