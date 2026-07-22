import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  saveTokens,
  type TokenBundle,
} from '../auth/tokenStorage'
import { formatHttpError, resolveApiBase } from './apiBase'

const apiBase = resolveApiBase(import.meta.env.VITE_API_BASE_URL)

export type Sex = 'MALE' | 'FEMALE'
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE'
export type Objective = 'LOSE' | 'MAINTAIN' | 'GAIN' | 'CUT' | 'MUSCLE_GAIN' | 'BULK'
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

export type OnboardingInput = {
  sex: Sex
  birthDate: string
  heightCm: number
  weightKg: number
  activityLevel: ActivityLevel
  objective: Objective
}

export type OnboardingResult = {
  profile: UserProfile
  weight: WeightLog
  needsProfile: boolean
  goals: Goal[]
}

export type ProductNutrient = {
  code: string
  amountPer100g: number
  unit: string
}

export type Product = {
  id: string
  submissionId: string | null
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

export type ProductSearchResult = {
  query: string
  page: number
  pageSize: number
  items: Product[]
}

export type SubmissionStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type ProductSubmission = {
  id: string
  submitterUserId: string
  status: SubmissionStatus
  barcode: string | null
  name: string
  brand: string | null
  servingSizeG: number | null
  nutrients: ProductNutrient[]
  submittedAt: string
  reviewedBy: string | null
  reviewedAt: string | null
  reviewNote: string | null
  publishedProductId: string | null
  duplicateWarnings: string[]
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
  productId: string | null
  submissionId: string | null
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

let refreshInFlight: Promise<boolean> | null = null

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return false
  }
  const response = await fetch(`${apiBase}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) {
    clearTokens()
    return false
  }
  const tokens = (await response.json()) as TokenBundle
  saveTokens(tokens)
  return true
}

/** Single-flight refresh so parallel 401s only hit /api/auth/refresh once. */
function refreshAccessTokenShared(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = refreshAccessToken().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function authenticatedFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const withAuth = {
    ...init,
    headers: { ...authHeaders(), ...(init.headers as Record<string, string> | undefined) },
  }
  const response = await fetch(input, withAuth)
  if (response.status !== 401) {
    return response
  }
  const refreshed = await refreshAccessTokenShared()
  if (!refreshed) {
    return response
  }
  return fetch(input, {
    ...init,
    headers: { ...authHeaders(), ...(init.headers as Record<string, string> | undefined) },
  })
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
  const response = await authenticatedFetch(`${apiBase}/api/users/me`)
  return parseJson<UserProfile>(response)
}

export async function updateMe(input: UpdateMeInput): Promise<UserProfile> {
  const response = await authenticatedFetch(`${apiBase}/api/users/me`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<UserProfile>(response)
}

export async function logWeight(input: { weightKg: number; measuredAt?: string }): Promise<WeightLog> {
  const response = await authenticatedFetch(`${apiBase}/api/users/me/weight`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
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
  const response = await authenticatedFetch(
    `${apiBase}/api/users/me/weight${query ? `?${query}` : ''}`,
  )
  return parseJson<WeightLog[]>(response)
}

export async function fetchGoals(): Promise<Goal[]> {
  const response = await authenticatedFetch(`${apiBase}/api/users/me/goals`)
  return parseJson<Goal[]>(response)
}

export async function overrideGoals(input: { goals: GoalOverride[] }): Promise<Goal[]> {
  const response = await authenticatedFetch(`${apiBase}/api/users/me/goals`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<Goal[]>(response)
}

export async function recalculateGoals(apply: boolean): Promise<RecalculateGoalsResult> {
  const params = new URLSearchParams({ apply: String(apply) })
  const response = await authenticatedFetch(
    `${apiBase}/api/users/me/goals/recalculate?${params}`,
    { method: 'POST' },
  )
  return parseJson<RecalculateGoalsResult>(response)
}

export async function completeOnboarding(input: OnboardingInput): Promise<OnboardingResult> {
  const response = await authenticatedFetch(`${apiBase}/api/users/me/onboarding`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<OnboardingResult>(response)
}

export async function fetchProductByBarcode(ean: string): Promise<Product> {
  const response = await authenticatedFetch(
    `${apiBase}/api/products/barcode/${encodeURIComponent(ean)}`,
  )
  return parseJson<Product>(response)
}

export async function fetchProductById(id: string): Promise<Product> {
  const response = await authenticatedFetch(`${apiBase}/api/products/${encodeURIComponent(id)}`)
  return parseJson<Product>(response)
}

export async function searchProducts(q: string, page = 1): Promise<ProductSearchResult> {
  const params = new URLSearchParams({ q, page: String(page) })
  const response = await authenticatedFetch(`${apiBase}/api/products/search?${params}`)
  return parseJson<ProductSearchResult>(response)
}

export async function createProductSubmission(input: {
  name: string
  brand?: string
  barcode?: string
  servingSizeG?: number
  nutrients: ProductNutrient[]
  force?: boolean
}): Promise<ProductSubmission> {
  const response = await authenticatedFetch(`${apiBase}/api/products/submissions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<ProductSubmission>(response)
}

export async function fetchMySubmissions(): Promise<ProductSubmission[]> {
  const response = await authenticatedFetch(`${apiBase}/api/products/submissions/mine`)
  return parseJson<ProductSubmission[]>(response)
}

export async function fetchSubmissionQueue(
  status: SubmissionStatus = 'PENDING',
): Promise<ProductSubmission[]> {
  const params = new URLSearchParams({ status })
  const response = await authenticatedFetch(`${apiBase}/api/products/submissions?${params}`)
  return parseJson<ProductSubmission[]>(response)
}

export async function approveSubmission(id: string): Promise<ProductSubmission> {
  const response = await authenticatedFetch(
    `${apiBase}/api/products/submissions/${encodeURIComponent(id)}/approve`,
    { method: 'POST' },
  )
  return parseJson<ProductSubmission>(response)
}

export async function rejectSubmission(id: string, note?: string): Promise<ProductSubmission> {
  const response = await authenticatedFetch(
    `${apiBase}/api/products/submissions/${encodeURIComponent(id)}/reject`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ note: note ?? null }),
    },
  )
  return parseJson<ProductSubmission>(response)
}

export async function fetchNutrient(code: string): Promise<Nutrient> {
  const response = await authenticatedFetch(
    `${apiBase}/api/nutrients/${encodeURIComponent(code)}`,
  )
  return parseJson<Nutrient>(response)
}

export async function fetchNutrients(): Promise<Nutrient[]> {
  const response = await authenticatedFetch(`${apiBase}/api/nutrients`)
  return parseJson<Nutrient[]>(response)
}

export function browserTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}

function withDateAndZone(date: string): URLSearchParams {
  return new URLSearchParams({ date, zone: browserTimeZone() })
}

export async function fetchDiaryEntries(date: string): Promise<DiaryEntry[]> {
  const response = await authenticatedFetch(
    `${apiBase}/api/diary/entries?${withDateAndZone(date)}`,
  )
  return parseJson<DiaryEntry[]>(response)
}

export async function createDiaryEntry(input: {
  productId?: string
  submissionId?: string
  weightG: number
  mealType: MealType
  consumedAt?: string
}): Promise<DiaryEntry> {
  const response = await authenticatedFetch(`${apiBase}/api/diary/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<DiaryEntry>(response)
}

export async function deleteDiaryEntry(id: string): Promise<void> {
  const response = await authenticatedFetch(
    `${apiBase}/api/diary/entries/${encodeURIComponent(id)}`,
    { method: 'DELETE' },
  )
  return parseNoContent(response)
}

export async function fetchWater(date: string): Promise<WaterLog[]> {
  const response = await authenticatedFetch(
    `${apiBase}/api/diary/water?${withDateAndZone(date)}`,
  )
  return parseJson<WaterLog[]>(response)
}

export async function logWater(input: { amountMl: number; loggedAt?: string }): Promise<WaterLog> {
  const response = await authenticatedFetch(`${apiBase}/api/diary/water`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  return parseJson<WaterLog>(response)
}

export async function deleteWater(id: string): Promise<void> {
  const response = await authenticatedFetch(
    `${apiBase}/api/diary/water/${encodeURIComponent(id)}`,
    { method: 'DELETE' },
  )
  return parseNoContent(response)
}

export async function fetchDiarySummary(date: string): Promise<DaySummary> {
  const response = await authenticatedFetch(
    `${apiBase}/api/diary/summary?${withDateAndZone(date)}`,
  )
  return parseJson<DaySummary>(response)
}

export async function fetchDiarySummaryRange(from: string, to: string): Promise<DaySummary[]> {
  const params = new URLSearchParams({ from, to, zone: browserTimeZone() })
  const response = await authenticatedFetch(`${apiBase}/api/diary/summary/range?${params}`)
  return parseJson<DaySummary[]>(response)
}
