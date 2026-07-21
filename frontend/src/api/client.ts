import { getAccessToken, type TokenBundle } from '../auth/tokenStorage'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''

export type UserProfile = {
  id: string
  email: string
  displayName: string
  avatarUrl: string | null
  role: string
  sex: string | null
  heightCm: number | null
  activityLevel: string | null
  objective: string
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
    throw new Error(text || `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

async function parseNoContent(response: Response): Promise<void> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed (${response.status})`)
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

export async function fetchDiaryEntries(date: string): Promise<DiaryEntry[]> {
  const params = new URLSearchParams({ date })
  const response = await fetch(`${apiBase}/api/diary/entries?${params}`, {
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
  const params = new URLSearchParams({ date })
  const response = await fetch(`${apiBase}/api/diary/water?${params}`, {
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
  const params = new URLSearchParams({ date })
  const response = await fetch(`${apiBase}/api/diary/summary?${params}`, {
    headers: authHeaders(),
  })
  return parseJson<DaySummary>(response)
}
