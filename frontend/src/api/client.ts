import { getAccessToken, type TokenBundle } from '../auth/tokenStorage'
import { formatHttpError, resolveApiBase } from './apiBase'

const apiBase = resolveApiBase(import.meta.env.VITE_API_BASE_URL)

export type Sex = 'MALE' | 'FEMALE'
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE'
export type Objective = 'LOSE' | 'MAINTAIN' | 'GAIN'
export type GoalOrigin = 'COMPUTED' | 'USER_OVERRIDE'

export type UserProfile = {
  id: string
  email: string
  displayName: string
  avatarUrl: string | null
  role: string
  sex: Sex | null
  birthDate: string | null
  heightCm: number | null
  activityLevel: ActivityLevel | null
  objective: Objective
}

export type UpdateMeInput = {
  displayName?: string
  sex?: Sex
  birthDate?: string
  heightCm?: number
  activityLevel?: ActivityLevel
  objective?: Objective
}

export type WeightLog = {
  id: string
  weightKg: number
  measuredAt: string
}

export type Goal = {
  nutrientCode: string
  dailyTarget: number
  unit: string
  origin: GoalOrigin
  computedAt: string | null
}

export type GoalOverride = {
  nutrientCode: string
  dailyTarget: number
  unit: string
}

export type RecalculateGoalsResult = {
  needsProfile: boolean
  suggested: Goal[]
  current: Goal[]
}

export type ProductNutrient = {
  code: string
  amountPer100g: number
  unit: string
}

export type Product = {
  id: string
  barcode: string | null
  source: string
  name: string
  brand: string | null
  quantityLabel: string | null
  servingSizeG: number | null
  imageUrl: string | null
  nutriScore: string | null
  ingredientsText: string | null
  allergenTags: string[]
  offLastSyncedAt: string | null
  nutrients: ProductNutrient[]
}

export type Nutrient = {
  code: string
  displayName: string
  category: string
  defaultUnit: string
  description: string | null
  bodyEffects: string | null
  deficiencyEffects: string | null
  excessEffects: string | null
  commonSources: string | null
  contentSource: string | null
}

export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

export type DiaryNutrient = {
  code: string
  amount: number
  amountPer100g: number
  unit: string
}

export type DiaryEntry = {
  id: string
  productId: string
  productName: string
  brand: string | null
  weightG: number
  mealType: MealType
  consumedAt: string
  createdAt: string
  nutrients: DiaryNutrient[]
}

export type WaterLog = {
  id: string
  amountMl: number
  loggedAt: string
}

export type NutrientTotal = {
  code: string
  amount: number
  unit: string
  target: number | null
}

export type DaySummary = {
  date: string
  totals: NutrientTotal[]
  water: {
    amountMl: number
    targetMl: number | null
  }
}


async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(formatHttpError(response.status, text))
  }
  return response.json() as Promise<T>
}

async function parseNoContent(response: Response): Promise<void> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(formatHttpError(response.status, text))
  }
}

function authHeaders(): Record<string, string> {
  const token = getAccessToken()
  if (!token) {
    throw new Error('Not authenticated')
  }
  return { Authorization: `Bearer ${token}` }
}

export async function exchangeGoogleCode(input: {
  code: string
  codeVerifier?: string
  redirectUri: string
}): Promise<TokenBundle> {
  const response = await fetch(`${apiBase}/api/auth/google/callback`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<TokenBundle>(response)
}

export async function fetchMe(): Promise<UserProfile> {
  const response = await fetch(`${apiBase}/api/users/me`, {
    headers: authHeaders(),
  })
  return parseJson<UserProfile>(response)
}

export async function updateMe(input: UpdateMeInput): Promise<UserProfile> {
  const response = await fetch(`${apiBase}/api/users/me`, {
    method: 'PUT',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<UserProfile>(response)
}

export async function logWeight(input: { weightKg: number; measuredAt?: string }): Promise<WeightLog> {
  const response = await fetch(`${apiBase}/api/users/me/weight`, {
    method: 'POST',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<WeightLog>(response)
}

export async function fetchWeightHistory(input: { from?: string; to?: string } = {}): Promise<WeightLog[]> {
  const params = new URLSearchParams()
  if (input.from) {
    params.set('from', input.from)
  }
  if (input.to) {
    params.set('to', input.to)
  }
  const query = params.toString()
  const response = await fetch(`${apiBase}/api/users/me/weight${query ? `?${query}` : ''}`, {
    headers: authHeaders(),
  })
  return parseJson<WeightLog[]>(response)
}

export async function fetchGoals(): Promise<Goal[]> {
  const response = await fetch(`${apiBase}/api/users/me/goals`, {
    headers: authHeaders(),
  })
  return parseJson<Goal[]>(response)
}

export async function overrideGoals(input: { goals: GoalOverride[] }): Promise<Goal[]> {
  const response = await fetch(`${apiBase}/api/users/me/goals`, {
    method: 'PUT',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<Goal[]>(response)
}

export async function recalculateGoals(apply: boolean): Promise<RecalculateGoalsResult> {
  const params = new URLSearchParams({ apply: String(apply) })
  const response = await fetch(`${apiBase}/api/users/me/goals/recalculate?${params}`, {
    method: 'POST',
    headers: authHeaders(),
  })
  return parseJson<RecalculateGoalsResult>(response)
}

export async function fetchProductByBarcode(ean: string): Promise<Product> {
  const response = await fetch(`${apiBase}/api/products/barcode/${encodeURIComponent(ean)}`, {
    headers: authHeaders(),
  })
  return parseJson<Product>(response)
}

export async function fetchProductById(id: string): Promise<Product> {
  const response = await fetch(`${apiBase}/api/products/${encodeURIComponent(id)}`, {
    headers: authHeaders(),
  })
  return parseJson<Product>(response)
}

export async function fetchNutrient(code: string): Promise<Nutrient> {
  const response = await fetch(`${apiBase}/api/nutrients/${encodeURIComponent(code)}`, {
    headers: authHeaders(),
  })
  return parseJson<Nutrient>(response)
}

export async function fetchNutrients(): Promise<Nutrient[]> {
  const response = await fetch(`${apiBase}/api/nutrients`, {
    headers: authHeaders(),
  })
  return parseJson<Nutrient[]>(response)
}

export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}

function withDateAndZone(date: string): URLSearchParams {
  return new URLSearchParams({ date, zone: browserTimeZone() })
}

export async function fetchDiaryEntries(date: string): Promise<DiaryEntry[]> {
  const response = await fetch(`${apiBase}/api/diary/entries?${withDateAndZone(date)}`, {
    headers: authHeaders(),
  })
  return parseJson<DiaryEntry[]>(response)
}

export async function createDiaryEntry(input: {
  productId: string
  weightG: number
  mealType: MealType
  consumedAt?: string
}): Promise<DiaryEntry> {
  const response = await fetch(`${apiBase}/api/diary/entries`, {
    method: 'POST',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<DiaryEntry>(response)
}

export async function deleteDiaryEntry(id: string): Promise<void> {
  const response = await fetch(`${apiBase}/api/diary/entries/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  return parseNoContent(response)
}

export async function fetchWater(date: string): Promise<WaterLog[]> {
  const response = await fetch(`${apiBase}/api/diary/water?${withDateAndZone(date)}`, {
    headers: authHeaders(),
  })
  return parseJson<WaterLog[]>(response)
}

export async function logWater(input: { amountMl: number; loggedAt?: string }): Promise<WaterLog> {
  const response = await fetch(`${apiBase}/api/diary/water`, {
    method: 'POST',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<WaterLog>(response)
}

export async function deleteWater(id: string): Promise<void> {
  const response = await fetch(`${apiBase}/api/diary/water/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  return parseNoContent(response)
}

export async function fetchDiarySummary(date: string): Promise<DaySummary> {
  const response = await fetch(`${apiBase}/api/diary/summary?${withDateAndZone(date)}`, {
    headers: authHeaders(),
  })
  return parseJson<DaySummary>(response)
}
