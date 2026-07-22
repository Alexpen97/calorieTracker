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

export type WeightTrendPoint = {
  weightKg: number
  /** Position on the window: 0 = window start, 1 = window end (clock). */
  t: number
  measuredAt: string
}

export type WeightTrendOptions = {
  days?: number
  clock?: Date
}

export function buildWeightTrendSeries(
  weights: WeightLog[],
  options: WeightTrendOptions = {},
): WeightTrendPoint[] {
  const days = options.days ?? 30
  const clock = options.clock ?? new Date()
  const dayStart = startOfLocalDay(clock)
  const startMs = dayStart.getTime() - (days - 1) * 24 * 60 * 60 * 1000
  const endMs = endOfLocalDay(clock).getTime()
  const spanMs = Math.max(1, endMs - startMs)

  return [...weights]
    .filter((weight) => {
      const measuredMs = new Date(weight.measuredAt).getTime()
      return measuredMs >= startMs && measuredMs <= endMs
    })
    .sort((left, right) => new Date(left.measuredAt).getTime() - new Date(right.measuredAt).getTime())
    .map((weight) => {
      const measuredMs = new Date(weight.measuredAt).getTime()
      return {
        weightKg: weight.weightKg,
        t: (measuredMs - startMs) / spanMs,
        measuredAt: weight.measuredAt,
      }
    })
}

export function buildWeightTrend(weights: WeightLog[], options: WeightTrendOptions = {}): number[] {
  return buildWeightTrendSeries(weights, options).map((point) => point.weightKg)
}

/** Short date labels for the weight chart X axis: window start, midpoint, end. */
export function buildWeightTrendAxisLabels(options: WeightTrendOptions = {}): [string, string, string] {
  const days = options.days ?? 30
  const clock = options.clock ?? new Date()
  const dayStart = startOfLocalDay(clock)
  const start = new Date(dayStart.getTime() - (days - 1) * 24 * 60 * 60 * 1000)
  const end = dayStart
  const mid = new Date(start.getTime() + (end.getTime() - start.getTime()) / 2)
  return [formatAxisDate(start), formatAxisDate(mid), formatAxisDate(end)]
}

function formatAxisDate(date: Date): string {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(date)
}

export function dateDaysAgo(days: number, clock = new Date()): string {
  const date = new Date(clock)
  date.setDate(date.getDate() - days)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function startOfLocalDay(clock: Date): Date {
  return new Date(clock.getFullYear(), clock.getMonth(), clock.getDate())
}

function endOfLocalDay(clock: Date): Date {
  return new Date(clock.getFullYear(), clock.getMonth(), clock.getDate(), 23, 59, 59, 999)
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
