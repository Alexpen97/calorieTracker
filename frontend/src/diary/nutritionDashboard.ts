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

export type MicronutrientTrendPoint = {
  date: string
  amount: number
  percent: number
}

export type MicronutrientTrend = {
  code: string
  label: string
  unit: string
  target: number | null
  points: MicronutrientTrendPoint[]
  latestPercent: number
  latestAmountLabel: string
}

/** Daily micronutrient intake series across day summaries (amount vs shared target). */
export function buildMicronutrientTrendSeries(
  summaries: Array<{ date: string; totals: NutrientTotalForDisplay[] }>,
  kind: MicronutrientKind,
): MicronutrientTrend[] {
  const labels = kind === 'vitamin' ? vitaminCodes : mineralCodes
  const ordered = [...summaries].sort((left, right) => left.date.localeCompare(right.date))

  return [...labels.entries()].map(([code, label]) => {
    let target: number | null = null
    let unit = ''
    for (const summary of ordered) {
      const total = summary.totals.find((item) => item.code === code)
      if (!total) continue
      if (total.target != null) target = total.target
      if (total.unit) unit = total.unit
    }

    const points = ordered.map((summary) => {
      const total = summary.totals.find((item) => item.code === code)
      const amount = total?.amount ?? 0
      const dayTarget = total?.target ?? target
      return {
        date: summary.date,
        amount,
        percent: dayTarget ? Math.min(100, Math.round((amount / dayTarget) * 100)) : 0,
      }
    })

    const latest = points.at(-1)
    const latestAmount = latest?.amount ?? 0
    const latestPercent = latest?.percent ?? 0
    const latestAmountLabel =
      target != null
        ? `${formatNumber(latestAmount)} / ${formatNumber(target)} ${unit}`.trim()
        : unit
          ? `${formatNumber(latestAmount)} ${unit}`
          : formatNumber(latestAmount)

    return {
      code,
      label,
      unit,
      target,
      points,
      latestPercent,
      latestAmountLabel,
    }
  })
}

/** Average micronutrient intake across day summaries (amount mean vs shared target). */
export function averageMicronutrientRows(
  summaries: Array<{ totals: NutrientTotalForDisplay[] }>,
  kind: MicronutrientKind,
): NutritionProgressRow[] {
  if (summaries.length === 0) {
    return buildMicronutrientRows([], kind)
  }
  const labels = kind === 'vitamin' ? vitaminCodes : mineralCodes
  return [...labels.entries()].map(([code, label]) => {
    let amountSum = 0
    let target: number | null = null
    let unit = ''
    for (const summary of summaries) {
      const total = summary.totals.find((item) => item.code === code)
      if (!total) continue
      amountSum += total.amount
      if (total.target != null) target = total.target
      if (total.unit) unit = total.unit
    }
    const amount = amountSum / summaries.length
    if (amount === 0 && target == null && !unit) {
      return { code, label, percent: 0, amountLabel: '0' }
    }
    return {
      code,
      label,
      percent: target ? Math.min(100, Math.round((amount / target) * 100)) : 0,
      amountLabel: target
        ? `${formatNumber(amount)} / ${formatNumber(target)} ${unit}`
        : `${formatNumber(amount)} ${unit}`.trim(),
    }
  })
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
