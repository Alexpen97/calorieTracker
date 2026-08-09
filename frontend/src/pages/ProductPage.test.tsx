import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProductPage from './ProductPage'
import * as client from '../api/client'
import type { DiaryEntry, Product } from '../api/client'

const baseProduct = {
  id: 'prod-1',
  submissionId: null,
  barcode: '3017620422003',
  source: 'OFF' as const,
  name: 'Nutella',
  brand: 'Ferrero',
  quantityLabel: '400 g',
  servingSizeG: 15 as number | null,
  densityGPerMl: null,
  imageUrl: null,
  nutriScore: 'E' as string | null,
  ingredientsText: null,
  allergenTags: [] as string[],
  offLastSyncedAt: null,
  nutrients: [
    { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
  ],
}

function renderProduct(id = 'prod-1') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/products/${id}`]}>
        <Routes>
          <Route path="/products/:id" element={<ProductPage />} />
          <Route path="/today" element={<p>Today</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function renderProductWithSwitch(initialId = 'oil') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/products/${initialId}`]}>
        <Routes>
          <Route
            path="/products/:id"
            element={
              <>
                <ProductPage />
                <Link to="/products/flour">Switch to flour</Link>
              </>
            }
          />
          <Route path="/today" element={<p>Today</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function product(overrides: Partial<Product> = {}): Product {
  return {
    id: 'prod-1',
    submissionId: null,
    barcode: '3017620422003',
    source: 'OFF',
    name: 'Nutella',
    brand: 'Ferrero',
    quantityLabel: '400 g',
    servingSizeG: 15,
    densityGPerMl: null,
    imageUrl: null,
    nutriScore: 'E',
    ingredientsText: null,
    allergenTags: [],
    offLastSyncedAt: null,
    nutrients: [
      { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
    ],
    ...overrides,
  }
}

function diaryEntry(overrides: Partial<DiaryEntry> = {}): DiaryEntry {
  return {
    id: 'entry-1',
    productId: 'prod-1',
    submissionId: null,
    productName: 'Olive oil',
    brand: 'Farm',
    weightG: 100,
    mealType: 'BREAKFAST',
    consumedAt: '2026-08-09T12:00:00.000Z',
    createdAt: '2026-08-09T12:00:00.000Z',
    nutrients: [],
    ...overrides,
  }
}

describe('ProductPage estimated nutrients', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows approx marker and footnote when a nutrient is estimated', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product({
      nutrients: [
        { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
        { code: 'vitamin_b3', amountPer100g: 1.2, unit: 'mg', estimated: true },
      ],
    }))

    renderProduct()

    expect(await screen.findByText(/≈ vitamin b3/i)).toBeInTheDocument()
    expect(
      screen.getByText(/≈ estimated from USDA FoodData Central generic data/i),
    ).toBeInTheDocument()
  })

  it('hides footnote when no nutrients are estimated', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product({
      id: 'prod-2',
      barcode: '12345678',
      name: 'Plain yogurt',
      brand: 'Farm',
      quantityLabel: null,
      servingSizeG: null,
      nutriScore: null,
      nutrients: [
        { code: 'protein', amountPer100g: 4, unit: 'g', estimated: false },
      ],
    }))

    renderProduct('prod-2')

    expect(await screen.findByText(/protein/i)).toBeInTheDocument()
    expect(
      screen.queryByText(/≈ estimated from USDA FoodData Central generic data/i),
    ).not.toBeInTheDocument()
  })
})

describe('ProductPage diary amount units', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('hides volume unit toggle for gram-only products without serving size', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product({ servingSizeG: null }))

    renderProduct()

    const amount = await screen.findByLabelText(/amount \(g\)/i)
    expect(amount).toHaveValue(100)
    expect(screen.queryByLabelText(/unit/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Pieces$/i })).not.toBeInTheDocument()
  })

  it('shows g and ml unit toggle and helper for volume-capable products', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product({
      id: 'oil',
      name: 'Olive oil',
      quantityLabel: '500 ml',
      servingSizeG: null,
      densityGPerMl: 0.92,
    }))

    renderProduct('oil')

    expect(await screen.findByLabelText(/^amount$/i)).toHaveValue(100)
    expect(screen.getByLabelText(/unit/i)).toHaveValue('ml')
    expect(screen.getByText(/≈\s*92\s*g at 0\.92 g\/ml/i)).toBeInTheDocument()
  })

  it('validates amount without referencing grams', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product({
      id: 'oil',
      name: 'Olive oil',
      quantityLabel: '500 ml',
      servingSizeG: null,
      densityGPerMl: 0.92,
    }))

    renderProduct('oil')

    fireEvent.change(await screen.findByLabelText(/^amount$/i), { target: { value: '-1' } })
    fireEvent.click(screen.getByRole('button', { name: /add to diary/i }))

    expect(await screen.findByText('Enter a positive amount.')).toBeInTheDocument()
  })

  it('submits converted weightG when ml selected', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product({
      id: 'oil',
      name: 'Olive oil',
      quantityLabel: '500 ml',
      servingSizeG: null,
      densityGPerMl: 0.92,
    }))
    const create = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue(diaryEntry({
      productId: 'oil',
      weightG: 230,
    }))

    renderProduct('oil?meal=BREAKFAST')

    fireEvent.change(await screen.findByLabelText(/^amount$/i), { target: { value: '250' } })
    fireEvent.click(screen.getByRole('button', { name: /add to diary/i }))

    await waitFor(() => {
      expect(create).toHaveBeenCalledWith(
        expect.objectContaining({ productId: 'oil', weightG: 230, mealType: 'BREAKFAST' }),
      )
    })
  })

  it('resets amount and unit defaults when navigating between products', async () => {
    vi.spyOn(client, 'fetchProductById').mockImplementation(async (id) => {
      if (id === 'oil') {
        return product({
          id: 'oil',
          name: 'Olive oil',
          quantityLabel: '500 ml',
          servingSizeG: null,
          densityGPerMl: 0.92,
        })
      }
      return product({
        id: 'flour',
        name: 'Plain flour',
        quantityLabel: '1 kg',
        servingSizeG: null,
        densityGPerMl: null,
      })
    })

    renderProductWithSwitch()

    fireEvent.change(await screen.findByLabelText(/^amount$/i), { target: { value: '42' } })
    fireEvent.change(screen.getByLabelText(/unit/i), { target: { value: 'g' } })
    fireEvent.click(screen.getByRole('link', { name: /switch to flour/i }))

    const amount = await screen.findByLabelText(/amount \(g\)/i)
    expect(amount).toHaveValue(100)
    expect(screen.queryByLabelText(/unit/i)).not.toBeInTheDocument()
  })
})

describe('ProductPage piece entry', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('offers Grams/Pieces when servingSizeG is positive', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)

    renderProduct()

    expect(await screen.findByRole('button', { name: /^Grams$/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Pieces$/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/Amount \(g\)/i)).toBeInTheDocument()
  })

  it('hides piece toggle when servingSizeG is missing', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue({
      ...baseProduct,
      id: 'prod-2',
      servingSizeG: null,
    })

    renderProduct('prod-2')

    expect(await screen.findByLabelText(/Amount \(g\)/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Pieces$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Grams$/i })).not.toBeInTheDocument()
  })

  it('submits weightG = pieces × servingSizeG', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)
    const createSpy = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'entry-1',
      productId: 'prod-1',
      submissionId: null,
      productName: 'Nutella',
      brand: 'Ferrero',
      mealType: 'BREAKFAST',
      weightG: 30,
      consumedAt: '2026-08-09T08:00:00Z',
      createdAt: '2026-08-09T08:00:00Z',
      nutrients: [],
    })

    renderProduct('prod-1?meal=BREAKFAST')

    await screen.findByRole('button', { name: /^Pieces$/i })
    fireEvent.click(screen.getByRole('button', { name: /^Pieces$/i }))

    expect(screen.getByLabelText(/Amount \(pieces\)/i)).toHaveValue(1)
    expect(screen.getByText(/1 piece ≈ 15 g/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Amount \(pieces\)/i), { target: { value: '2' } })
    expect(screen.getByText(/2 pieces ≈ 30 g/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Add to diary/i }))

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          productId: 'prod-1',
          weightG: 30,
          mealType: 'BREAKFAST',
        }),
      )
    })
  })

  it('rejects non-integer piece amounts', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)
    const createSpy = vi.spyOn(client, 'createDiaryEntry')

    renderProduct()

    await screen.findByRole('button', { name: /^Pieces$/i })
    fireEvent.click(screen.getByRole('button', { name: /^Pieces$/i }))
    fireEvent.change(screen.getByLabelText(/Amount \(pieces\)/i), { target: { value: '1.5' } })
    fireEvent.click(screen.getByRole('button', { name: /Add to diary/i }))

    expect(await screen.findByText(/positive whole number of pieces/i)).toBeInTheDocument()
    expect(createSpy).not.toHaveBeenCalled()
  })

  it('resets amount when switching between grams and pieces', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)

    renderProduct()

    const amount = await screen.findByLabelText(/Amount \(g\)/i)
    fireEvent.change(amount, { target: { value: '250' } })

    fireEvent.click(screen.getByRole('button', { name: /^Pieces$/i }))
    expect(screen.getByLabelText(/Amount \(pieces\)/i)).toHaveValue(1)

    fireEvent.click(screen.getByRole('button', { name: /^Grams$/i }))
    expect(screen.getByLabelText(/Amount \(g\)/i)).toHaveValue(100)
  })
})

describe('ProductPage meal selection', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('defaults the meal select from local browser time when no meal param exists', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product())

    renderProduct()

    expect(await screen.findByLabelText('Meal')).toHaveValue('LUNCH')
  })

  it('uses an explicit valid meal param instead of local time inference', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product())

    renderProduct('prod-1?meal=DINNER')

    expect(await screen.findByLabelText('Meal')).toHaveValue('DINNER')
  })

  it('falls back to local time when the meal param is invalid', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 23, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product())

    renderProduct('prod-1?meal=brunch')

    expect(await screen.findByLabelText('Meal')).toHaveValue('SNACK')
  })

  it('submits the user-selected meal override', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(product())
    const createSpy = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue(diaryEntry({
      mealType: 'SNACK',
    }))

    renderProduct()

    fireEvent.change(await screen.findByLabelText('Meal'), { target: { value: 'SNACK' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add to diary' }))

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        productId: 'prod-1',
        weightG: 100,
        mealType: 'SNACK',
      })
    })
  })
})
