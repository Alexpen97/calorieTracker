export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

export type DiaryEntryForDisplay = {
  id: string
  productId: string | null
  submissionId?: string | null
  productName: string
  brand: string | null
  weightG: number
  mealType: MealType
  consumedAt: string
  createdAt: string
  nutrients: unknown[]
}

export type NutrientTotalForDisplay = {
  code: string
  amount: number
  unit: string
  target: number | null
}

export type WaterSummaryForDisplay = {
  amountMl: number
  targetMl: number | null
}

export const mealOrder: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK']

export function parseMealTypeParam(value: string | null | undefined): MealType | null {
  if (value === 'BREAKFAST' || value === 'LUNCH' || value === 'DINNER' || value === 'SNACK') {
    return value
  }
  return null
}

export function mealLookupPath(mealType: MealType): string {
  return `/lookup?meal=${mealType}`
}

export function productPathWithMeal(productId: string, mealType: MealType | null): string {
  if (!mealType) {
    return `/products/${productId}`
  }
  return `/products/${productId}?meal=${mealType}`
}

export type MealGroup<T extends DiaryEntryForDisplay = DiaryEntryForDisplay> = {
  mealType: MealType
  entries: T[]
}

export type ProgressValue = {
  percent: number
}

export type NutrientProgress = NutrientTotalForDisplay & ProgressValue

export type WaterProgress = WaterSummaryForDisplay & ProgressValue

export function formatLocalDate(date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function parseLocalDate(dateStr: string): Date {
  const [year, month, day] = dateStr.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function shiftLocalDate(dateStr: string, days: number): string {
  const date = parseLocalDate(dateStr)
  date.setDate(date.getDate() + days)
  return formatLocalDate(date)
}

export function formatDiaryDayLabel(dateStr: string, today = formatLocalDate()): string {
  const day = new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
  }).format(parseLocalDate(dateStr))

  if (dateStr === today) return `Today, ${day}`
  if (dateStr === shiftLocalDate(today, -1)) return `Yesterday, ${day}`
  if (dateStr === shiftLocalDate(today, 1)) return `Tomorrow, ${day}`
  return new Intl.DateTimeFormat(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  }).format(parseLocalDate(dateStr))
}

/** Inclusive 30-day window ending on `rangeEnd` (`from` = end − 29 days). */
export const ANALYTICS_RANGE_DAYS = 30

export function analyticsRangeFromEnd(rangeEnd: string): { from: string; to: string } {
  return {
    from: shiftLocalDate(rangeEnd, -(ANALYTICS_RANGE_DAYS - 1)),
    to: rangeEnd,
  }
}

export function shiftAnalyticsRangeEnd(rangeEnd: string, direction: -1 | 1, today = formatLocalDate()): string {
  const next = shiftLocalDate(rangeEnd, direction * ANALYTICS_RANGE_DAYS)
  if (direction > 0 && next > today) return today
  return next
}

export function formatAnalyticsRangeLabel(from: string, to: string): string {
  const start = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(parseLocalDate(from))
  const end = new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(parseLocalDate(to))
  return `${start} – ${end}`
}

export function groupEntriesByMeal<T extends DiaryEntryForDisplay>(entries: T[]): MealGroup<T>[] {
  return mealOrder.map((mealType) => ({
    mealType,
    entries: entries.filter((entry) => entry.mealType === mealType),
  }))
}

export function getMacroProgress(
  totals: NutrientTotalForDisplay[],
  code: string,
): NutrientProgress | null {
  const total = totals.find((item) => item.code === code)
  if (!total) {
    return null
  }
  return {
    ...total,
    percent: progressPercent(total.amount, total.target),
  }
}

export function waterProgress(water: WaterSummaryForDisplay): WaterProgress {
  return {
    ...water,
    percent: progressPercent(water.amountMl, water.targetMl),
  }
}

function progressPercent(amount: number, target: number | null): number {
  if (!target || target <= 0) {
    return 0
  }
  return Math.min(100, Math.round((amount / target) * 100))
}
