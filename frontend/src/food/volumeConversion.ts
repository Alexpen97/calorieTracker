export type AmountUnit = 'g' | 'ml'

export function isVolumeCapable(densityGPerMl: number | null | undefined): boolean {
  return typeof densityGPerMl === 'number' && Number.isFinite(densityGPerMl) && densityGPerMl > 0
}

/** Round HALF_UP to 2 decimal places. */
export function convertVolumeToGrams(amountMl: number, densityGPerMl: number): number {
  const raw = amountMl * densityGPerMl
  return Math.round((raw + Number.EPSILON) * 100) / 100
}

export function resolveWeightG(amount: number, unit: AmountUnit, densityGPerMl: number | null): number {
  if (unit === 'ml') {
    if (!isVolumeCapable(densityGPerMl)) {
      throw new Error('Product does not support ml entry')
    }
    return convertVolumeToGrams(amount, densityGPerMl!)
  }
  return Math.round((amount + Number.EPSILON) * 100) / 100
}
