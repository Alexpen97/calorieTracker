export function sanitizeBarcodeInput(raw: string): string {
  return raw.replace(/\D/g, '')
}

export function isValidBarcode(raw: string): boolean {
  return /^\d{8,14}$/.test(sanitizeBarcodeInput(raw))
}
