import type { WeightLog } from '../api/client'
import type { NutrientTotalForDisplay } from './formatDay'

export type MicronutrientKind = 'vitamin' | 'mineral'

export type NutritionProgressRow = {
  code: string
  label: string
  percent: number
  amountLabel: string
}

const macroDisplay = [
  ['protein', 'Protein'],
  ['carbohydrates', 'Carbs'],
  ['fat', 'Fat'],
] as const

const vitaminCodes = new Map([
  ['vitamin_a', 'Vitamin A'],
  ['vitamin_c', 'Vitamin C'],
  ['vitamin_d', 'Vitamin D'],
  ['vitamin_b12', 'B12'],
])

const mineralCodes = new Map([
  ['iron', 'Iron'],
  ['calcium', 'Calcium'],
  ['magnesium', 'Magnesium'],
  ['potassium', 'Potassium'],
])

export function buildMacroSummaries(totals: NutrientTotalForDisplay[]): NutritionProgressRow[] {
  return macroDisplay
    .map(([code, label]) => progressRow(totals, code, label))
    .filter((row): row is NutritionProgressRow => row !== null)
}

export function buildMicronutrientRows(
  totals: NutrientTotalForDisplay[],
  kind: MicronutrientKind,
): NutritionProgressRow[] {
  const labels = kind === 'vitamin' ? vitaminCodes : mineralCodes
  return [...labels.entries()]
    .map(([code, label]) => progressRow(totals, code, label))
    .filter((row): row is NutritionProgressRow => row !== null)
}

export function buildWeightTrend(weights: WeightLog[]): number[] {
  return [...weights]
    .sort((left, right) => new Date(left.measuredAt).getTime() - new Date(right.measuredAt).getTime())
    .slice(-14)
    .map((weight) => weight.weightKg)
}

export function dateDaysAgo(days: number, clock = new Date()): string {
  const date = new Date(clock)
  date.setDate(date.getDate() - days)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function progressRow(
  totals: NutrientTotalForDisplay[],
  code: string,
  label: string,
): NutritionProgressRow | null {
  const total = totals.find((item) => item.code === code)
  if (!total) return null
  return {
    code,
    label,
    percent: total.target ? Math.min(100, Math.round((total.amount / total.target) * 100)) : 0,
    amountLabel: total.target
      ? `${formatNumber(total.amount)} / ${formatNumber(total.target)} ${total.unit}`
      : `${formatNumber(total.amount)} ${total.unit}`,
  }
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value)
}
