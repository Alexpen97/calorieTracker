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
  ['vitamin_b1', 'B1'],
  ['vitamin_b2', 'B2'],
  ['vitamin_b3', 'B3'],
  ['vitamin_b5', 'B5'],
  ['vitamin_b6', 'B6'],
  ['vitamin_b7', 'B7'],
  ['vitamin_b9', 'B9'],
  ['vitamin_b12', 'B12'],
  ['vitamin_c', 'Vitamin C'],
  ['vitamin_d', 'Vitamin D'],
  ['vitamin_e', 'Vitamin E'],
  ['vitamin_k', 'Vitamin K'],
])

const mineralCodes = new Map([
  ['calcium', 'Calcium'],
  ['iron', 'Iron'],
  ['magnesium', 'Magnesium'],
  ['potassium', 'Potassium'],
  ['sodium', 'Sodium'],
  ['zinc', 'Zinc'],
  ['iodine', 'Iodine'],
  ['selenium', 'Selenium'],
  ['copper', 'Copper'],
  ['manganese', 'Manganese'],
  ['phosphorus', 'Phosphorus'],
  ['chromium', 'Chromium'],
  ['molybdenum', 'Molybdenum'],
])

export function buildMacroSummaries(totals: NutrientTotalForDisplay[]): NutritionProgressRow[] {
  return macroDisplay.map(([code, label]) => {
    const total = totals.find((item) => item.code === code)
    if (!total) {
      return {
        code,
        label,
        percent: 0,
        amountLabel: '0 g',
      }
    }
    return {
      code,
      label,
      percent: total.target ? Math.min(100, Math.round((total.amount / total.target) * 100)) : 0,
      amountLabel: total.target
        ? `${formatNumber(total.amount)} / ${formatNumber(total.target)} ${total.unit}`
        : `${formatNumber(total.amount)} ${total.unit}`,
    }
  })
}

export function buildMicronutrientRows(
  totals: NutrientTotalForDisplay[],
  kind: MicronutrientKind,
): NutritionProgressRow[] {
  const labels = kind === 'vitamin' ? vitaminCodes : mineralCodes
  return [...labels.entries()].map(([code, label]) => progressRow(totals, code, label))
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
): NutritionProgressRow {
  const total = totals.find((item) => item.code === code)
  if (!total) {
    return {
      code,
      label,
      percent: 0,
      amountLabel: '0',
    }
  }
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
